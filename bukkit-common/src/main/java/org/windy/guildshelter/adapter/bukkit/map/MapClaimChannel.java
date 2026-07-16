package org.windy.guildshelter.adapter.bukkit.map;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerChangedWorldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.windy.guildshelter.api.MapClaimStatus;
import org.windy.guildshelter.adapter.bukkit.GuildWorldRegistry;
import org.windy.guildshelter.domain.layout.Classification;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.port.GuildRepository;
import org.windy.guildshelter.domain.port.ManorRepository;
import org.windy.guildshelter.service.GuildService;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * 主城/庄园地皮在 <b>Xaero 世界地图</b>上的可视化 + 点击圈地的<b>服务端半</b>（协议见 PLAN_XAERO.md）。
 *
 * <p>通道 {@code guildshelter:map}（Bukkit 插件消息 = 自定义载荷，NeoForge 客户端 mod 注册同名 payload 即互通）。
 * 玩家进公会世界时下发 PLOTS（主城 + 已分配庄园的已解锁 chunk，含颜色 + 标签）；客户端 mod 在地图上
 * 高亮并支持点击 → 回 CLAIM 包，本端按既有规则（归属/相邻/额度/会长权限）裁决后调 {@link GuildService} 解锁。
 *
 * <p><b>无客户端 mod 的玩家完全不受影响</b>：出站包没有监听端即被丢弃，入站永不发生，服务端逻辑照常。
 */
public final class MapClaimChannel implements PluginMessageListener, Listener {

    public static final String CHANNEL = "guildshelter:map";

    // S→C
    private static final byte PLOTS = 0x01;
    private static final byte CLEAR = 0x02;
    private static final byte CLAIM_RESULT = 0x20;
    // C→S
    private static final byte CLAIM_REQUEST = 0x10;
    private static final byte ACTION_CLAIM = 0;
    private static final int MAX_CLAIM_REQUEST_BYTES = 64;

    // kind（与 PLAN_XAERO.md §3 对齐）
    private static final byte KIND_CITY_UNLOCKED = 0;
    private static final byte KIND_OWN_UNLOCKED = 1;
    private static final byte KIND_OTHER_UNLOCKED = 2;

    // ARGB 颜色（半透明，便于叠在地图上）
    private static final int C_CITY = 0x80FFD24D;       // 金黄
    private static final int C_OWN = 0x8055FF55;        // 亮绿
    private static final int C_OTHER = 0x8055D7FF;      // 青蓝

    private final GuildWorldRegistry registry;
    private final GuildRepository guilds;
    private final ManorRepository manors;
    private final GuildService service;
    private final Plugin plugin;
    private final Logger logger;

    public MapClaimChannel(GuildWorldRegistry registry, GuildRepository guilds, ManorRepository manors,
                           GuildService service, Plugin plugin, Logger logger) {
        this.registry = registry;
        this.guilds = guilds;
        this.manors = manors;
        this.service = service;
        this.plugin = plugin;
        this.logger = logger;
    }

    /** 注册收/发通道 + 事件监听。 */
    public void register() {
        plugin.getServer().getMessenger().registerOutgoingPluginChannel(plugin, CHANNEL);
        plugin.getServer().getMessenger().registerIncomingPluginChannel(plugin, CHANNEL, this);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        logger.info("[GuildShelter] 通道已注册: " + CHANNEL);
    }

    // ===== 触发：进公会世界发 PLOTS =====

    @EventHandler
    public void onWorldChange(PlayerChangedWorldEvent e) {
        Player p = e.getPlayer();
        GuildWorld gw = registry.get(p.getWorld().getName());
        if (gw != null) {
            refreshPlayer(p, gw);
        } else if (registry.get(e.getFrom().getName()) != null) {
            // 从公会世界离开到非公会世界：清空高亮，避免站回主世界还残留（同名 overworld 地图）。
            sendClear(p, e.getFrom().getName());
        }
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        Player p = e.getPlayer();
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            GuildWorld gw = registry.get(p.getWorld().getName());
            if (gw != null) {
                sendPlots(p, gw);
            }
        }, 20L);
    }

    /** 给该公会在线成员重发（解锁/认领/升级后由调用方触发）。 */
    public void refreshGuild(GuildWorld gw) {
        for (Player p : plugin.getServer().getOnlinePlayers()) {
            if (gw.worldName().equals(p.getWorld().getName())) {
                sendPlots(p, gw);
            }
        }
    }

    /** Re-send one player's active guild-world highlights after teleport/client dimension state settles. */
    public void refreshPlayer(Player player, GuildWorld gw) {
        sendPlotsIfStillInWorld(player, gw, 5L);
        sendPlotsIfStillInWorld(player, gw, 40L);
    }

    private void sendPlotsIfStillInWorld(Player player, GuildWorld gw, long delayTicks) {
        plugin.getServer().getScheduler().runTaskLater(plugin, () -> {
            if (player.isOnline() && gw.worldName().equals(player.getWorld().getName())) {
                sendPlots(player, gw);
            }
        }, delayTicks);
    }

    // ===== 出站：PLOTS（已占领 chunk）=====

    public void sendPlots(Player player, GuildWorld gw) {
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeByte(PLOTS);
            out.writeUTF(gw.worldName());
            out.writeInt(gw.originChunkX());
            out.writeInt(gw.originChunkZ());

            ByteArrayOutputStream entries = new ByteArrayOutputStream();
            DataOutputStream eo = new DataOutputStream(entries);
            int count = 0;

            // 主城：只发已解锁 chunk。
            ChunkRegion city = layout.mainCityRegion();
            for (int dx = city.minChunkX(); dx <= city.maxChunkX(); dx++) {
                for (int dz = city.minChunkZ(); dz <= city.maxChunkZ(); dz++) {
                    if (gw.isCityUnlocked(dx, dz)) {
                        writeEntry(eo, gw.originChunkX() + dx, gw.originChunkZ() + dz,
                                C_CITY, KIND_CITY_UNLOCKED, "城");
                        count++;
                    }
                }
            }

            // 已分配庄园：只发已解锁 chunk。自己的庄园用绿色，其它成员庄园用青蓝。
            for (Manor manor : manors.findAll(gw.guild())) {
                ChunkRegion plot = layout.plotRegion(manor.slot());
                boolean own = manor.owner().equals(ref);
                for (int packed : manor.unlockedChunks()) {
                    int wx = plot.minChunkX() + Manor.unpackDx(packed);
                    int wz = plot.minChunkZ() + Manor.unpackDz(packed);
                    writeEntry(eo, gw.originChunkX() + wx, gw.originChunkZ() + wz,
                            own ? C_OWN : C_OTHER,
                            own ? KIND_OWN_UNLOCKED : KIND_OTHER_UNLOCKED,
                            "#" + manor.slot());
                    count++;
                }
            }

            out.writeInt(count);
            eo.flush();
            out.write(entries.toByteArray());
            out.flush();
            byte[] payload = bos.toByteArray();
            player.sendPluginMessage(plugin, CHANNEL, payload);
            // 诊断：确认服务端确实发了 PLOTS（客户端侧对应 "[gsmap] received guildshelter:map payload"）。
            // 若此行有、客户端无，则混合端 Bukkit 插件消息→NeoForge 客户端 payload 的桥没通（PLAN_XAERO §8）。
            logger.info("[GuildShelter] 已发送 PLOTS 给 " + player.getName() + " world=" + gw.worldName()
                    + " chunks=" + count + " bytes=" + payload.length);
        } catch (IOException ex) {
            logger.warning("[GuildShelter] 发送地图地皮失败: " + ex.getMessage());
        }
    }

    /** 离开公会世界时清空客户端高亮（混合端维度塌缩成 overworld，靠 enter/leave 推送界定，不靠客户端 dim 判定）。 */
    private void sendClear(Player player, String worldName) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            out.writeByte(CLEAR);
            out.writeUTF(worldName);
            player.sendPluginMessage(plugin, CHANNEL, bos.toByteArray());
        } catch (IOException ex) {
            logger.warning("[GuildShelter] 发送地图清空失败: " + ex.getMessage());
        }
    }

    private static void writeEntry(DataOutputStream eo, int chunkX, int chunkZ, int argb, byte kind, String label)
            throws IOException {
        eo.writeInt(chunkX);
        eo.writeInt(chunkZ);
        eo.writeInt(argb);
        eo.writeByte(kind);
        eo.writeUTF(label);
    }

    // ===== 入站：CLAIM（地图点击圈地）=====

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
                sendResult(player, action, left, top, right, bottom, MapClaimStatus.NOT_CLAIMABLE);
                return;
            }
            handleClaim(player, action, left, top);
        } catch (IOException ex) {
            logger.warning("[GuildShelter] 解析地图圈地请求失败: " + ex.getMessage());
        }
    }

    /** 裁决一次地图圈地点击：按 chunk 落区分流到庄园解锁 / 主城解锁，复用既有服务规则。 */
    private void handleClaim(Player player, byte action, int worldChunkX, int worldChunkZ) {
        GuildWorld gw = registry.get(player.getWorld().getName());
        if (gw == null) {
            return; // 不在公会世界
        }
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        int lx = worldChunkX - gw.originChunkX();
        int lz = worldChunkZ - gw.originChunkZ();
        Classification c = layout.classify(lx, lz);
        PlayerRef ref = PlayerRef.of(player.getUniqueId());

        if (c.isPlot()) {
            GuildService.UnlockResult r = service.unlockChunk(gw.guild(), ref, worldChunkX, worldChunkZ);
            sendResult(player, action, worldChunkX, worldChunkZ, worldChunkX, worldChunkZ, mapStatus(r));
            if (r == GuildService.UnlockResult.SUCCESS) {
                refreshGuild(gw);
            }
        } else if (c.isMainCity()) {
            if (!service.isGuildAdmin(ref, gw.guild()) && !player.isOp()) {
                sendResult(player, action, worldChunkX, worldChunkZ, worldChunkX, worldChunkZ, MapClaimStatus.CITY_LEADER_ONLY);
                return;
            }
            GuildWorld updated = service.unlockCityChunk(gw.guild(), worldChunkX, worldChunkZ);
            boolean ok = updated != null;
            if (ok) {
                registry.register(updated);
            }
            sendResult(player, action, worldChunkX, worldChunkZ, worldChunkX, worldChunkZ, mapStatus(service.lastCityUnlockResult()));
            if (ok) {
                refreshGuild(updated);
            }
        } else {
            sendResult(player, action, worldChunkX, worldChunkZ, worldChunkX, worldChunkZ, MapClaimStatus.NOT_CLAIMABLE);
        }
    }

    private void sendResult(Player player, byte action, int left, int top, int right, int bottom, MapClaimStatus status) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (DataOutputStream out = new DataOutputStream(bos)) {
            int statusOrdinal = status.ordinal();
            if (statusOrdinal > 255) {
                throw new IOException("Map claim status ordinal is too large: " + statusOrdinal);
            }
            out.writeByte(CLAIM_RESULT);
            out.writeByte(action);
            out.writeInt(left);
            out.writeInt(top);
            out.writeInt(right);
            out.writeInt(bottom);
            out.writeByte(statusOrdinal);
            player.sendPluginMessage(plugin, CHANNEL, bos.toByteArray());
        } catch (IOException ex) {
            logger.warning("[GuildShelter] 发送圈地结果失败: " + ex.getMessage());
        }
    }

    private static MapClaimStatus mapStatus(GuildService.UnlockResult result) {
        return switch (result) {
            case SUCCESS -> MapClaimStatus.SUCCESS;
            case NO_MANOR -> MapClaimStatus.NO_MANOR;
            case NOT_YOUR_PLOT -> MapClaimStatus.NOT_YOUR_PLOT;
            case ALREADY_UNLOCKED -> MapClaimStatus.ALREADY_UNLOCKED;
            case NO_QUOTA -> MapClaimStatus.NO_QUOTA;
            case NOT_ADJACENT -> MapClaimStatus.NOT_ADJACENT;
        };
    }
}
