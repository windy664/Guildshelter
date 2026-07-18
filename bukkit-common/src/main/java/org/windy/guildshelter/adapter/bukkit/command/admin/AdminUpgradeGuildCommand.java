package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;

@GsSubCommand(name = "admin upgrade-guild", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminUpgradeGuildCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.admin_upgrade_guild")); return; }
        GuildId guild = new GuildId(args[1]);
        GuildWorld before = ctx.guilds.find(guild).orElse(null);
        if (before == null) { sender.sendMessage(Messages.get("error.guild_not_exist", guild.value())); return; }
        LayoutCalculator layout = new LayoutCalculator(before.layout());
        double oldBorder = layout.adaptiveBorderSizeBlocks(before.allocatedSlots(), 1);
        boolean ok = ctx.service.upgradeGuild(guild);
        if (!ok) { sender.sendMessage(Messages.get("error.already_max_level", ctx.levels.maxGuildLevel())); return; }
        GuildWorld after = ctx.guilds.find(guild).orElse(before);
        double newBorder = layout.adaptiveBorderSizeBlocks(after.allocatedSlots(), 1);
        sender.sendMessage(Messages.get("success.upgrade_guild", after.guildLevel(), ctx.levels.maxGuildLevel(),
                ctx.levels.maxMembers(before.guildLevel()), ctx.levels.maxMembers(after.guildLevel()), (int) oldBorder, (int) newBorder));
        World world = Bukkit.getWorld(after.worldName());
        if (world != null) {
            LayoutCalculator al = new LayoutCalculator(after.layout());
            int sx = al.spawnBlockX() + (after.originChunkX() << 4);
            int sz = al.spawnBlockZ() + (after.originChunkZ() << 4);
            int sy = world.getHighestBlockYAt(sx, sz) + 2;
            var loc = new org.bukkit.Location(world, sx + 0.5, sy, sz + 0.5);
            world.spawn(loc, org.bukkit.entity.Firework.class, fw -> {
                var meta = fw.getFireworkMeta();
                meta.addEffect(org.bukkit.FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BALL_LARGE).withColor(org.bukkit.Color.YELLOW, org.bukkit.Color.ORANGE).withFade(org.bukkit.Color.RED).trail(true).flicker(true).build());
                meta.setPower(1); fw.setFireworkMeta(meta);
            });
            for (Player p : world.getPlayers()) p.sendTitle("§6§l⬆ 公会升级！", "§e" + guild.value() + " §7→ Lv" + after.guildLevel(), 10, 60, 20);
        }
        ctx.logMap(guild);
    }
}
