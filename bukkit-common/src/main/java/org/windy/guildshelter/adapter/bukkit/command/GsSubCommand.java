package org.windy.guildshelter.adapter.bukkit.command;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 标记一个子命令类。{@link CommandRegistry} 启动时扫描此注解自动注册路由。
 *
 * <p>示例：{@code @GsSubCommand(name = "home", permission = "guildshelter.command.home")}
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GsSubCommand {

    /** 子命令名，多词用空格分隔（如 {@code "admin create"}）。 */
    String name();

    /** 权限节点。空串 = 默认放行（仅 requiresPlayer 约束）。 */
    String permission() default "";

    /** 是否只允许玩家执行（控制台自动拒绝并发 {@code error.player_only}）。 */
    boolean requiresPlayer() default true;

    /** 是否需要二次确认（30 秒内连打两次或 {@code /gs confirm}）。 */
    boolean requiresConfirm() default false;

    /** 别名列表（如 {@code "cityuntrust"} 可注册为 {@code "citytrust"} 的别名）。 */
    String[] aliases() default {};
}
