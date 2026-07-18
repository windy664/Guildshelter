package org.windy.guildshelter.adapter.bukkit.command.admin;

import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.model.GuildWorld;

@GsSubCommand(name = "admin fund", permission = "guildshelter.admin", requiresPlayer = false)
public class AdminFundCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) { sender.sendMessage(Messages.get("usage.admin_fund")); return; }
        GuildId guild = new GuildId(args[1]);
        GuildWorld gw = ctx.guilds.find(guild).orElse(null);
        if (gw == null) { sender.sendMessage(Messages.get("error.guild_not_exist", guild.value())); return; }
        String action = args.length >= 3 ? args[2].toLowerCase() : "check";
        switch (action) {
            case "check" -> sender.sendMessage(Messages.get("info.fund_check", guild.value(), String.format("%.0f", gw.funds())));
            case "add" -> {
                if (args.length < 4) { sender.sendMessage(Messages.get("usage.admin_fund")); return; }
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { sender.sendMessage(Messages.get("error.number_must_be_int")); return; }
                if (amount <= 0) { sender.sendMessage(Messages.get("error.positive_required")); return; }
                ctx.guilds.save(gw.withFunds(gw.funds() + amount));
                sender.sendMessage(Messages.get("success.fund_added", guild.value(), String.format("%.0f", amount), String.format("%.0f", gw.funds() + amount)));
            }
            case "set" -> {
                if (args.length < 4) { sender.sendMessage(Messages.get("usage.admin_fund")); return; }
                double amount;
                try { amount = Double.parseDouble(args[3]); } catch (NumberFormatException e) { sender.sendMessage(Messages.get("error.number_must_be_int")); return; }
                ctx.guilds.save(gw.withFunds(amount));
                sender.sendMessage(Messages.get("success.fund_set", guild.value(), String.format("%.0f", amount)));
            }
            default -> sender.sendMessage(Messages.get("usage.admin_fund"));
        }
    }
}
