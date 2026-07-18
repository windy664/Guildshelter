package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;

/**
 * /gs confirm：确认待执行的危险操作。
 *
 * <p>实际逻辑由 {@link org.windy.guildshelter.adapter.bukkit.command.CommandRegistry} 处理，
 * 此类仅为注册占位。
 */
@GsSubCommand(name = "confirm")
public class ConfirmCommand extends SubCommand {
    @Override
    public void execute(CommandSender sender, String[] args) {
        // 不应到达此处——CommandRegistry 在路由阶段已处理 /gs confirm。
        // 若意外到达，静默忽略。
    }
}
