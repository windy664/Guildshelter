package org.windy.guildshelter.xaero;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.windy.guildshelter.api.GuildShelterAPI;
import org.windy.guildshelter.api.MapClaimResult;
import org.windy.guildshelter.api.MapClaimStatus;
import org.windy.guildshelter.api.TerritoryMapChunk;
import org.windy.guildshelter.api.TerritoryMapKind;
import org.windy.guildshelter.api.TerritoryMapSnapshot;
import org.windy.guildshelter.api.event.ChunkUnlockedEvent;
import org.windy.guildshelter.api.event.CityChunkUnlockedEvent;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Optional;
import java.util.logging.Logger;

final class XaeroMapChannel implements PluginMessageListener, Listener {

    static final String CHANNEL = "guildshelter:map";

    private static final byte PLOTS = 0x01;
    private static final byte CLEAR = 0x02;
    private static final byte CLAIM_RESULT = 0x20;
    private static final byte CLAIM_REQUEST = 0x10;
    private static final byte ACTION_CLAIM = 0;
    private static final int MAX_CLAIM_REQUEST_BYTES = 64;

    private static final byte KIND_CITY_UNLOCKED = 0;
    private static final byte KIND_OWN_UNLOCKED = 1;
    private static final byte KIND_OTHER_UNLOCKED = 2;

    private static final int C_CITY = 0x80FFD24D;
    private static final int C_OWN = 0x8055FF55;
    private static final int C_OTHER = 0x8055D7FF;

    private final Plugin plugin;
    private final GuildShelterAPI api;
    private final Logger logger;

    XaeroMapChannel(Plugin plugin, GuildShelterAPI api, Logger logger) {
        this.plugin = plugin;
        this.api = api;
        this.logger = logger;
    }

    void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        logger.info("Registered " + CHANNEL + " map bridge channel.");
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> refreshPlayer(player), 20L);
    }

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent event) {
        Player player = event.getPlayer();
        Optional<TerritoryMapSnapshot> snapshot = api.mapSnapshot(player);
        if (snapshot.isPresent()) {
            sendPlots(player, snapshot.get());
        } else {
            sendClear(player, event.getFrom().getName());
        }
    }

    @EventHandler
    public void onChunkUnlocked(ChunkUnlockedEvent event) {
        refreshWorld(event.guild().worldName());
    }

    @EventHandler
    public void onCityChunkUnlocked(CityChunkUnlockedEvent event) {
        refreshWorld(event.guild().worldName());
    }

    private void refreshPlayer(Player player) {
        if (player == null || !player.isOnline()) {
            return;
        }
        api.mapSnapshot(player).ifPresent(snapshot -> sendPlots(player, snapshot));
    }

    private void refreshWorld(String worldName) {
        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getWorld().getName().equals(worldName)) {
                refreshPlayer(player);
            }
        }
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!CHANNEL.equals(channel)) {
            return;
        }
        if (message == null || message.length > MAX_CLAIM_REQUEST_BYTES) {
            return;
        }
        try (DataInputStream in = new DataInputStream(new ByteArrayInputStream(message))) {
            byte type = in.readByte();
            if (type != CLAIM_REQUEST) {
                return;
            }
            byte action = in.readByte();
            if (action != ACTION_CLAIM) {
                return;
            }
            int left = in.readInt();
            int top = in.readInt();
            int right = in.readInt();
            int bottom = in.readInt();
            if (left > right || top > bottom) {
                return;
            }
            if (left != right || top != bottom) {
                sendResult(player, action, left, top, right, bottom, MapClaimResult.of(MapClaimStatus.NOT_CLAIMABLE));
                return;
            }

            MapClaimResult result = api.tryClaimMapChunk(player, left, top);
            sendResult(player, action, left, top, right, bottom, result);
            if (result.success()) {
                refreshWorld(player.getWorld().getName());
            }
        } catch (IOException ex) {
            logger.warning("Failed to parse map claim request: " + ex.getMessage());
        }
    }

    private void sendPlots(Player player, TerritoryMapSnapshot snapshot) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeByte(PLOTS);
            out.writeUTF(snapshot.worldName());
            out.writeInt(snapshot.originChunkX());
            out.writeInt(snapshot.originChunkZ());
            out.writeInt(snapshot.chunks().size());
            for (TerritoryMapChunk chunk : snapshot.chunks()) {
                out.writeInt(chunk.chunkX());
                out.writeInt(chunk.chunkZ());
                out.writeInt(color(chunk.kind()));
                out.writeByte(kind(chunk.kind()));
                out.writeUTF(chunk.label());
            }
            player.sendPluginMessage(plugin, CHANNEL, bos.toByteArray());
        } catch (IOException ex) {
            logger.warning("Failed to send map plots: " + ex.getMessage());
        }
    }

    private void sendClear(Player player, String worldName) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeByte(CLEAR);
            out.writeUTF(worldName);
            player.sendPluginMessage(plugin, CHANNEL, bos.toByteArray());
        } catch (IOException ex) {
            logger.warning("Failed to send map clear: " + ex.getMessage());
        }
    }

    private void sendResult(Player player, byte action, int left, int top, int right, int bottom, MapClaimResult result) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            int status = result.status().ordinal();
            if (status > 255) {
                throw new IOException("Map claim status ordinal is too large: " + status);
            }
            out.writeByte(CLAIM_RESULT);
            out.writeByte(action);
            out.writeInt(left);
            out.writeInt(top);
            out.writeInt(right);
            out.writeInt(bottom);
            out.writeByte(status);
            player.sendPluginMessage(plugin, CHANNEL, bos.toByteArray());
        } catch (IOException ex) {
            logger.warning("Failed to send map claim result: " + ex.getMessage());
        }
    }

    private static int color(TerritoryMapKind kind) {
        return switch (kind) {
            case CITY_UNLOCKED -> C_CITY;
            case OWN_UNLOCKED -> C_OWN;
            case OTHER_UNLOCKED -> C_OTHER;
        };
    }

    private static byte kind(TerritoryMapKind kind) {
        return switch (kind) {
            case CITY_UNLOCKED -> KIND_CITY_UNLOCKED;
            case OWN_UNLOCKED -> KIND_OWN_UNLOCKED;
            case OTHER_UNLOCKED -> KIND_OTHER_UNLOCKED;
        };
    }
}
