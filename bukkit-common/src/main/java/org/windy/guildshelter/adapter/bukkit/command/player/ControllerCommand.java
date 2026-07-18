package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.adapter.bukkit.gui.Menus;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.port.ui.UiBackend;
import org.windy.guildshelter.domain.port.ui.UiViewer;

/**
 * /gs controller：打开庄园控制面板 UI。
 */
@GsSubCommand(name = "controller", permission = "guildshelter.command.controller")
public class ControllerCommand extends SubCommand {

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Messages.get("error.player_only"));
            return;
        }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) {
            sender.sendMessage(Messages.get("error.no_manor"));
            return;
        }
        GuildWorld gw = ctx.guilds.find(manor.guild()).orElse(null);
        if (gw == null) {
            sender.sendMessage(Messages.get("error.guild_not_exist", manor.guild().value()));
            return;
        }
        openUi(player, Menus.manorController(manor, gw, ctx.levels));
    }

    static void openUi(Player player, org.windy.guildshelter.domain.port.ui.UiView view) {
        UiBackend backend = org.windy.guildshelter.GuildShelterPlugin.uiBackend();
        if (backend == null || view == null) {
            player.sendMessage(Messages.get("error.gui_not_ready"));
            return;
        }
        backend.open(new UiViewer(player.getUniqueId(), player.getName()), view);
    }
}
