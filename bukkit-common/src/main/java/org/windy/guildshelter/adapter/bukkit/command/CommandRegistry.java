package org.windy.guildshelter.adapter.bukkit.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.Permissions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * 子命令路由注册中心。
 *
 * <p>通过 {@link #register(SubCommand)} 注册带 {@link GsSubCommand} 注解的子命令类，
 * 自动处理路由、权限校验、playerOnly 拦截、确认机制。
 */
public final class CommandRegistry implements CommandExecutor, TabCompleter {

    private static final String ADMIN_PERM = Permissions.ADMIN;

    /** name → handler（包含别名映射）。 */
    private final Map<String, SubCommand> commands = new HashMap<>();
    private final CommandContext ctx;

    public CommandRegistry(CommandContext ctx) {
        this.ctx = ctx;
    }

    /**
     * 注册一个子命令。根据 {@link GsSubCommand} 注解注册 name 和 aliases。
     */
    public void register(SubCommand cmd) {
        GsSubCommand ann = cmd.getClass().getAnnotation(GsSubCommand.class);
        if (ann == null) {
            throw new IllegalArgumentException(cmd.getClass().getName() + " 缺少 @GsSubCommand 注解");
        }
        cmd.setContext(ctx);
        commands.put(ann.name(), cmd);
        for (String alias : ann.aliases()) {
            commands.put(alias, cmd);
        }
    }

    /**
     * 获取所有已注册的主命令名（不含别名），用于 help 和 Tab 首位补全。
     */
    public java.util.Set<String> registeredNames() {
        java.util.Set<String> names = new java.util.LinkedHashSet<>();
        for (Map.Entry<String, SubCommand> e : commands.entrySet()) {
            GsSubCommand ann = e.getValue().getClass().getAnnotation(GsSubCommand.class);
            names.add(ann.name());
        }
        return names;
    }

    /**
     * 获取子命令的注解元数据（供 help 等使用）。
     */
    public GsSubCommand getAnnotation(String name) {
        SubCommand cmd = commands.get(name);
        return cmd != null ? cmd.getClass().getAnnotation(GsSubCommand.class) : null;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        String sub = args.length >= 1 ? args[0].toLowerCase() : "";
        if (sub.isEmpty()) {
            showHelp(sender);
            return true;
        }

        // ── 1. confirm 特殊命令优先处理 ──
        if ("confirm".equals(sub) && sender instanceof Player player) {
            UUID id = player.getUniqueId();
            PendingAction pending = ctx.pendingConfirm.remove(id);
            if (pending == null) {
                sender.sendMessage(Messages.get("error.no_pending"));
            } else if (pending.expired()) {
                sender.sendMessage(Messages.get("error.confirm_expired"));
            } else {
                SubCommand pendingHandler = commands.get(pending.sub());
                if (pendingHandler != null) {
                    pendingHandler.execute(sender, pending.args());
                }
            }
            return true;
        }

        // ── 2. 查找 handler ──
        SubCommand handler = commands.get(sub);
        if (handler == null) {
            // admin 子命令：尝试多词匹配 "admin xxx"
            if (args.length >= 2) {
                String twoWord = sub + " " + args[1].toLowerCase();
                handler = commands.get(twoWord);
                if (handler != null) {
                    String[] rewritten = new String[args.length - 1];
                    rewritten[0] = twoWord;
                    System.arraycopy(args, 2, rewritten, 1, args.length - 2);
                    args = rewritten;
                    sub = twoWord;
                }
            }
            if (handler == null) {
                showHelp(sender);
                return true;
            }
        }

        GsSubCommand ann = handler.getClass().getAnnotation(GsSubCommand.class);

        // ── 3. playerOnly 检查 ──
        if (ann.requiresPlayer() && !(sender instanceof Player)) {
            sender.sendMessage(Messages.get("error.player_only"));
            return true;
        }

        // ── 4. 权限检查 ──
        if (!ann.permission().isEmpty() && !sender.hasPermission(ann.permission())) {
            if (ann.permission().equals(ADMIN_PERM) && sender.isOp()) {
                // OP 放行
            } else {
                sender.sendMessage(Messages.get("error.no_permission_for", sub));
                return true;
            }
        }

        // ── 5. 确认机制 ──
        if (ann.requiresConfirm() && sender instanceof Player player) {
            UUID id = player.getUniqueId();
            PendingAction existing = ctx.pendingConfirm.get(id);
            if (existing != null && !existing.expired() && existing.sub().equals(sub)) {
                ctx.pendingConfirm.remove(id);
            } else {
                ctx.pendingConfirm.put(id, new PendingAction(sub, args, System.currentTimeMillis() + 30_000));
                sender.sendMessage(Messages.get("error.need_confirm"));
                return true;
            }
        }

        // ── 6. 执行 ──
        handler.execute(sender, args);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> out = new ArrayList<>();

        if (args.length == 1) {
            String prefix = args[0].toLowerCase();
            // 补全单层子命令（排除多词命令如 "admin create"）
            for (Map.Entry<String, SubCommand> e : commands.entrySet()) {
                String name = e.getKey();
                if (name.contains(" ")) continue; // 多词命令不显示在首位
                // admin 只对有权限的人显示
                if ("admin".equals(name) && !sender.hasPermission(ADMIN_PERM) && !sender.isOp()) continue;
                if (name.startsWith(prefix)) out.add(name);
            }
            // admin 入口（如果有 admin 子命令）
            if ("admin".startsWith(prefix) && commands.keySet().stream().anyMatch(k -> k.startsWith("admin "))) {
                if (!out.contains("admin") && (sender.hasPermission(ADMIN_PERM) || sender.isOp())) out.add("admin");
            }
            return out;
        }

        // 多词子命令匹配
        SubCommand handler = commands.get(args[0].toLowerCase());
        String[] tabArgs = args;

        if (handler == null && args.length >= 2) {
            String twoWord = args[0].toLowerCase() + " " + args[1].toLowerCase();
            handler = commands.get(twoWord);
            if (handler != null) {
                // 重写 tabArgs
                String[] rewritten = new String[args.length - 1];
                rewritten[0] = twoWord;
                System.arraycopy(args, 2, rewritten, 1, args.length - 2);
                tabArgs = rewritten;
            }
        }

        if (handler == null) {
            // admin 第二位：补全 admin 子命令（需权限）
            if (args[0].equalsIgnoreCase("admin") && args.length == 2
                    && (sender.hasPermission(ADMIN_PERM) || sender.isOp())) {
                String prefix = args[1].toLowerCase();
                for (String name : commands.keySet()) {
                    if (name.startsWith("admin ") && name.substring(6).startsWith(prefix)) {
                        out.add(name.substring(6));
                    }
                }
            }
            return out;
        }

        return handler.tabComplete(sender, tabArgs);
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage(Messages.get("help.header", "help"));
    }
}
