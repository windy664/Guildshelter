package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.ChunkRegion;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@GsSubCommand(name = "template", permission = "guildshelter.command.template")
public class TemplateCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.player_only")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        if (!manor.owner().equals(PlayerRef.of(player.getUniqueId()))) { sender.sendMessage(Messages.get("error.only_owner_template")); return; }
        GuildId guild = manor.guild();
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.template")); return; }
        String action = args[1].toLowerCase();
        switch (action) {
            case "save" -> {
                if (ctx.schematicStore == null) { sender.sendMessage(Messages.get("error.no_worldedit")); return; }
                if (args.length < 3) { sender.sendMessage(Messages.get("usage.template_save")); return; }
                String name = args[2].toLowerCase();
                if (!name.matches("[a-zA-Z0-9_\\-]+")) { sender.sendMessage(Messages.get("error.invalid_name")); return; }
                GuildWorld gw = ctx.guilds.find(guild).orElse(null);
                if (gw == null) { sender.sendMessage(Messages.get("error.world_not_exist")); return; }
                LayoutCalculator layout = new LayoutCalculator(gw.layout());
                ChunkRegion active = layout.activeRegion(manor.slot(), manor.level()).shift(gw.originChunkX(), gw.originChunkZ());
                int minY = player.getWorld().getMinHeight(); int maxY = player.getWorld().getMaxHeight();
                var path = ctx.schematicStore.save(gw.worldName(), name, active.minBlockX(), minY, active.minBlockZ(), active.maxBlockX() + 15, maxY, active.maxBlockZ() + 15);
                sender.sendMessage(path != null ? Messages.get("success.template_saved", name) : Messages.get("error.template_save_failed"));
            }
            case "paste" -> {
                if (ctx.schematicStore == null) { sender.sendMessage(Messages.get("error.no_worldedit")); return; }
                if (args.length < 3) { sender.sendMessage(Messages.get("usage.template_paste")); return; }
                String name = args[2].toLowerCase();
                GuildWorld gw = ctx.guilds.find(guild).orElse(null);
                if (gw == null) { sender.sendMessage(Messages.get("error.world_not_exist")); return; }
                LayoutCalculator layout = new LayoutCalculator(gw.layout());
                ChunkRegion active = layout.activeRegion(manor.slot(), manor.level()).shift(gw.originChunkX(), gw.originChunkZ());
                int x = active.minBlockX(), z = active.minBlockZ();
                int y = player.getWorld().getHighestBlockYAt(x, z);
                boolean async = org.bukkit.Bukkit.getPluginManager().getPlugin("FastAsyncWorldEdit") != null;
                ctx.schematicStore.paste(gw.worldName(), name, x, y, z, async);
                sender.sendMessage(Messages.get("success.template_pasted", name));
            }
            case "list-schematics" -> {
                if (ctx.schematicStore == null) { sender.sendMessage(Messages.get("error.no_worldedit")); return; }
                var schematics = ctx.schematicStore.list();
                if (schematics.isEmpty()) { sender.sendMessage(Messages.get("info.no_schematics")); return; }
                sender.sendMessage(Messages.get("info.schematics_header"));
                for (String n : schematics) sender.sendMessage(Messages.get("info.schematics_entry", n));
            }
            case "create" -> {
                if (args.length < 3) { sender.sendMessage(Messages.get("usage.template_create")); return; }
                String name = args[2].toLowerCase();
                if (!name.matches("[a-zA-Z0-9_\\-]+")) { sender.sendMessage(Messages.get("error.invalid_name")); return; }
                if (ctx.manors.getTemplate(guild, name).isPresent()) { sender.sendMessage(Messages.get("error.template_already_exist", name)); return; }
                ctx.manors.saveTemplate(guild, name, Map.of());
                sender.sendMessage(Messages.get("success.template_created", name, name));
            }
            case "delete" -> {
                if (args.length < 3) { sender.sendMessage(Messages.get("usage.template_delete")); return; }
                ctx.manors.deleteTemplate(guild, args[2].toLowerCase());
                sender.sendMessage(Messages.get("success.template_deleted", args[2]));
            }
            case "setflag" -> {
                if (args.length < 5) { sender.sendMessage(Messages.get("usage.template_setflag")); return; }
                String name = args[2].toLowerCase();
                Map<String, String> tpl = ctx.manors.getTemplate(guild, name).orElse(null);
                if (tpl == null) { sender.sendMessage(Messages.get("error.template_not_exist", name)); return; }
                Flag f = Flag.byId(args[3]).orElse(null);
                if (f == null) { sender.sendMessage(Messages.get("error.unknown_flag", args[3])); return; }
                String value = f.normalize(args[4]).orElse(null);
                if (value == null) { sender.sendMessage(Messages.get("error.invalid_value")); return; }
                Map<String, String> updated = new HashMap<>(tpl);
                updated.put(f.id(), value);
                ctx.manors.saveTemplate(guild, name, updated);
                sender.sendMessage(Messages.get("success.template_flag_set", name, f.id(), value));
            }
            case "apply" -> {
                if (args.length < 3) { sender.sendMessage(Messages.get("usage.template_apply")); return; }
                String name = args[2].toLowerCase();
                Map<String, String> tpl = ctx.manors.getTemplate(guild, name).orElse(null);
                if (tpl == null) { sender.sendMessage(Messages.get("error.template_not_exist", name)); return; }
                Map<String, String> flags = new HashMap<>(manor.flags());
                flags.putAll(tpl);
                ctx.manors.save(manor.withFlags(flags));
                sender.sendMessage(Messages.get("success.template_applied", name, tpl.size(), manor.slot()));
            }
            case "list" -> {
                List<String> names = ctx.manors.listTemplates(guild);
                if (names.isEmpty()) { sender.sendMessage(Messages.get("error.no_template_yet")); return; }
                sender.sendMessage(Messages.get("info.template_header"));
                for (String n : names) { Map<String, String> tpl = ctx.manors.getTemplate(guild, n).orElse(Map.of()); sender.sendMessage(Messages.get("info.template_entry", n, tpl.size())); }
            }
            default -> sender.sendMessage(Messages.get("usage.template"));
        }
    }
}
