package org.windy.guildshelter.adapter.bukkit.command;

import org.bukkit.command.CommandSender;

import java.util.List;

/**
 * 子命令抽象基类。每个子命令一个文件，通过 {@link GsSubCommand} 注解声明元数据。
 *
 * <p>子类必须实现 {@link #execute}；可选覆写 {@link #tabComplete}。
 */
public abstract class SubCommand {

    protected CommandContext ctx;

    /**
     * 执行子命令。
     *
     * @param sender 命令发送者
     * @param args   完整参数数组（args[0] = 子命令名，args[1..] = 用户参数）
     */
    public abstract void execute(CommandSender sender, String[] args);

    /**
     * Tab 补全。返回当前位置可用的补全候选。
     *
     * @param sender 命令发送者
     * @param args   完整参数数组
     * @return 候选列表；空列表 = 不补全
     */
    public List<String> tabComplete(CommandSender sender, String[] args) {
        return List.of();
    }

    public void setContext(CommandContext ctx) {
        this.ctx = ctx;
    }
}
