package org.windy.guildshelter.horde;

import org.bukkit.ChatColor;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.Map;

final class HordeMessages {

    private final FileConfiguration config;
    private final Map<String, String> defaults = new HashMap<>();

    HordeMessages(FileConfiguration config) {
        this.config = config;
        putDefaults();
    }

    String get(String key, Object... args) {
        String raw = config.getString("messages." + key, defaults.getOrDefault(key, key));
        raw = ChatColor.translateAlternateColorCodes('&', raw);
        for (int i = 0; i + 1 < args.length; i += 2) {
            raw = raw.replace("{" + args[i] + "}", String.valueOf(args[i + 1]));
        }
        return raw;
    }

    private void putDefaults() {
        defaults.put("command.no-permission", "&c你没有权限使用尸潮管理命令。");
        defaults.put("command.usage", "&e用法：/gshorde <start|stop|status> [worldName]");
        defaults.put("command.console-start-usage", "&e控制台用法：/gshorde start <worldName>");
        defaults.put("command.console-stop-usage", "&e控制台用法：/gshorde stop <worldName>");
        defaults.put("command.console-status-usage", "&e控制台用法：/gshorde status <worldName>");

        defaults.put("error.not-in-camp", "&c你当前不在 GuildShelter 公会营地内。");
        defaults.put("error.world-not-loaded", "&c营地世界未加载：&f{world}");
        defaults.put("error.world-not-camp", "&c该世界不是 GuildShelter 公会营地：&f{world}");
        defaults.put("error.already-running-camp", "&c营地 &f{guild} &c的尸潮已经在进行中。");
        defaults.put("error.already-running-global", "&c当前已有其它营地尸潮在进行中，同一时间只能开一个营地。");
        defaults.put("error.peaceful-disabled", "&c无法启动尸潮：营地世界是和平难度，且配置未允许临时改难度。");

        defaults.put("command.started", "&a已为营地 &f{guild} &a启动尸潮。");
        defaults.put("command.stopped", "&a已停止世界 &f{world} &a的尸潮。");
        defaults.put("command.not-running", "&e世界 &f{world} &e当前没有尸潮。");

        defaults.put("session.started", "&6尸潮来袭！&e守住公会营地。");
        defaults.put("session.wave-start", "&c第 &f{wave}&c/&f{max_waves} &c波尸潮开始。");
        defaults.put("session.wave-end", "&e第 &f{wave} &e波攻势结束，残留敌人不会被清理。");
        defaults.put("session.world-unloaded", "&e尸潮已结束：营地世界已卸载。");
        defaults.put("session.world-peaceful", "&e尸潮已结束：营地世界被切回和平难度。");
        defaults.put("session.no-spawn", "&e尸潮已结束：当前世界拒绝生成敌对生物。");
        defaults.put("session.no-defenders", "&e尸潮已退去：营地无人驻守。");
        defaults.put("session.success", "&a尸潮防守成功。");
        defaults.put("session.failed", "&c尸潮防守失败。");
        defaults.put("session.manual-stop", "&e管理员已停止尸潮。");
        defaults.put("session.plugin-disabled", "&e插件卸载，尸潮已停止。");
        defaults.put("session.camp-dissolved", "&e营地已解散，尸潮已停止。");
        defaults.put("session.reward", "&a尸潮奖励：&f{exp} &a经验。");
        defaults.put("session.difficulty-forced", "&e尸潮期间已临时将营地世界难度调整为 &c{difficulty}&e。");
        defaults.put("session.difficulty-restored", "&7尸潮结束，营地世界难度已恢复为 &f{difficulty}&7。");

        defaults.put("status.running", "&e尸潮进行中：第 &f{wave}&e/&f{max_waves} &e波，已生成 &f{spawned}&e/&f{target} &e只，存活 &f{alive} &e只。");
        defaults.put("status.intermission", "&e尸潮整备中：第 &f{wave}&e/&f{max_waves} &e波，剩余 &f{seconds}s&e。");

        defaults.put("boss.preparing", "尸潮整备中");
        defaults.put("boss.intermission", "尸潮整备 | 第 {wave} 波攻势结束");
        defaults.put("boss.wave", "尸潮第 {wave}/{max_waves} 波 | 已生成 {spawned}/{target} | 存活 {alive}");

        defaults.put("reminder.running", "&6尸潮提醒：&c本营地尸潮正在进行中。");
        defaults.put("reminder.queued", "&6尸潮提醒：&e本营地尸潮已排队，等待当前尸潮结束后触发。");
        defaults.put("reminder.scheduled", "&6尸潮提醒：&e本营地约 &f{remaining} &e后迎来尸潮。");
        defaults.put("reminder.less-than-day", "不到1天");
        defaults.put("reminder.days", "{days}天");
    }
}
