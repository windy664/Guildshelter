package org.windy.guildshelter.adapter.bukkit.command.manor;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.layout.Classification;
import org.windy.guildshelter.domain.layout.LayoutCalculator;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.PlayerRef;

@GsSubCommand(name = "claim", permission = "guildshelter.command.claim")
public class ClaimSelfCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.claim_not_in_world")); return; }
        GuildWorld gw = ctx.registry.get(player.getWorld().getName());
        if (gw == null) { sender.sendMessage(Messages.get("error.claim_not_in_world")); return; }
        LayoutCalculator layout = new LayoutCalculator(gw.layout());
        int lx = (player.getLocation().getBlockX() >> 4) - gw.originChunkX();
        int lz = (player.getLocation().getBlockZ() >> 4) - gw.originChunkZ();
        Classification c = layout.classify(lx, lz);
        if (!c.isPlot()) { sender.sendMessage(Messages.get("error.claim_not_plot")); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        int limit = ctx.multiManor.limitFor(player);
        int owned = ctx.manors.countByOwner(gw.guild(), ref);
        if (owned >= limit) {
            String cap = limit == Integer.MAX_VALUE ? "∞" : String.valueOf(limit);
            sender.sendMessage(Messages.get("error.claim_limit_reached", owned, cap)); return;
        }
        double cost = ctx.multiManor.claimCost();
        org.windy.guildshelter.domain.port.EconomyPort eco = cost > 0 ? economy() : null;
        if (cost > 0 && (eco == null || !eco.has(ref, cost))) {
            sender.sendMessage(Messages.get("error.claim_insufficient_funds", eco != null ? eco.format(cost) : cost)); return;
        }
        switch (ctx.service.claimManorAt(gw.guild(), ref, c.slot())) {
            case SUCCESS -> {
                if (cost > 0 && eco != null) eco.withdraw(ref, cost);
                ctx.registry.register(ctx.guilds.find(gw.guild()).orElse(gw));
                if (cost > 0 && eco != null) sender.sendMessage(Messages.get("success.claim_self_success_paid", c.slot(), eco.format(cost)));
                else sender.sendMessage(Messages.get("success.claim_self_success", c.slot()));
                ctx.logMap(gw.guild());
            }
            case ALREADY_OWNED -> sender.sendMessage(Messages.get("success.claim_already_owned"));
            case GUILD_FULL -> sender.sendMessage(Messages.get("success.claim_guild_full"));
            case NOT_PLOT -> sender.sendMessage(Messages.get("success.claim_not_plot"));
        }
    }

    private org.windy.guildshelter.domain.port.EconomyPort economy() {
        return org.windy.guildshelter.adapter.bukkit.VaultEconomy.tryCreate(ctx.logger);
    }
}
