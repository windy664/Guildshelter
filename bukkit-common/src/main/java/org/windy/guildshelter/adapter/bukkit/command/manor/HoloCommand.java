package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.port.CityHologramStore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@GsSubCommand(name = "holo", permission = "guildshelter.command.holo")
public class HoloCommand extends SubCommand {
    private static final Pattern PAPI_TOKEN = Pattern.compile("%([^%]+)%");

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        if (!ctx.holoEnabled || ctx.holoStore == null || ctx.holoBackend == null || !ctx.holoBackend.available()) {
            player.sendMessage(Messages.get("error.holo_unavailable")); return;
        }
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null || !standingInMainCity(player, gw)) { player.sendMessage(Messages.get("error.holo_not_in_city")); return; }
        boolean leader = ctx.service.isGuildAdmin(PlayerRef.of(player.getUniqueId()), gw.guild()) || player.isOp();
        String action = args.length >= 2 ? args[1].toLowerCase() : "list";
        switch (action) {
            case "add" -> {
                if (!leader) { player.sendMessage(Messages.get("error.holo_leader_only")); return; }
                if (args.length < 3) { player.sendMessage(Messages.get("usage.holo")); return; }
                List<CityHologramStore.HoloRecord> cur = ctx.holoStore.list(gw.guild());
                if (cur.size() >= ctx.holoMaxPerGuild) { player.sendMessage(Messages.get("error.holo_limit", ctx.holoMaxPerGuild)); return; }
                String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                String bad = firstDisallowedPlaceholder(text);
                if (bad != null) { player.sendMessage(Messages.get("error.holo_papi_blocked", "%" + bad + "%")); return; }
                List<String> lines = new ArrayList<>();
                for (String part : text.split("\\|", -1)) lines.add(part.trim());
                String name = uniqueHoloName(gw.guild());
                Location loc = player.getLocation().clone().add(0, 2.3, 0);
                if (!ctx.holoBackend.create(name, loc, lines)) { player.sendMessage(Messages.get("error.holo_create_failed")); return; }
                ctx.holoStore.add(gw.guild(), name, String.join("|", lines));
                player.sendMessage(Messages.get("success.holo_added", cur.size() + 1, ctx.holoMaxPerGuild));
            }
            case "remove", "delete" -> {
                if (!leader) { player.sendMessage(Messages.get("error.holo_leader_only")); return; }
                var rec = holoByIndex(player, gw, args);
                if (rec == null) return;
                ctx.holoBackend.remove(rec.name());
                ctx.holoStore.remove(gw.guild(), rec.name());
                player.sendMessage(Messages.get("success.holo_removed"));
            }
            case "move" -> {
                if (!leader) { player.sendMessage(Messages.get("error.holo_leader_only")); return; }
                var rec = holoByIndex(player, gw, args);
                if (rec == null) return;
                Location loc = player.getLocation().clone().add(0, 2.3, 0);
                if (!ctx.holoBackend.move(rec.name(), loc)) { player.sendMessage(Messages.get("error.holo_create_failed")); return; }
                player.sendMessage(Messages.get("success.holo_moved"));
            }
            default -> {
                List<CityHologramStore.HoloRecord> list = ctx.holoStore.list(gw.guild());
                if (list.isEmpty()) { player.sendMessage(Messages.get("info.holo_empty")); return; }
                player.sendMessage(Messages.get("info.holo_header", list.size(), ctx.holoMaxPerGuild));
                for (int i = 0; i < list.size(); i++) player.sendMessage(Messages.get("info.holo_entry", i + 1, list.get(i).label()));
            }
        }
    }

    private CityHologramStore.HoloRecord holoByIndex(Player player, GuildWorld gw, String[] args) {
        List<CityHologramStore.HoloRecord> list = ctx.holoStore.list(gw.guild());
        int idx;
        try { idx = Integer.parseInt(args[2]); } catch (Exception e) { player.sendMessage(Messages.get("error.holo_index")); return null; }
        if (idx < 1 || idx > list.size()) { player.sendMessage(Messages.get("error.holo_index")); return null; }
        return list.get(idx - 1);
    }

    private String uniqueHoloName(GuildId guild) {
        String safe = guild.value().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_]", "_");
        String name;
        do { name = "gs_" + safe + "_" + Long.toHexString((System.nanoTime() & 0xFFFFFFFFFFL)); } while (ctx.holoBackend.exists(name));
        return name;
    }

    private boolean standingInMainCity(Player player, GuildWorld gw) {
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        int lx = (player.getLocation().getBlockX() >> 4) - gw.originChunkX();
        int lz = (player.getLocation().getBlockZ() >> 4) - gw.originChunkZ();
        return layout.classify(lx, lz).isMainCity();
    }

    private String firstDisallowedPlaceholder(String text) {
        if (ctx.holoPapiWhitelist == null) return null;
        Matcher m = PAPI_TOKEN.matcher(text);
        while (m.find()) { String token = m.group(1); if (!ctx.holoPapiWhitelist.matches(token)) return token; }
        return null;
    }
}
