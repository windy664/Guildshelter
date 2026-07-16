package org.windy.guildshelter.adapter.bukkit;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.java.JavaPlugin;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.port.ManorUpgradeHook;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * {@link ManorUpgradeHook} 的 config 驱动实现。
 *
 * <p>读取 {@code levels.yml} 的 {@code manor.levels.<等级>.upgrade.commands}。
 */
public final class ManorUpgradeCommandHook implements ManorUpgradeHook {

    private final JavaPlugin plugin;
    /** 目标等级 -> 命令模板列表。 */
    private final Map<Integer, List<String>> byLevel;

    private ManorUpgradeCommandHook(JavaPlugin plugin, Map<Integer, List<String>> byLevel) {
        this.plugin = plugin;
        this.byLevel = byLevel;
    }

    /**
     * 从 levels.yml 构建；找不到任何升级命令时返回 {@code null}。
     */
    public static ManorUpgradeCommandHook fromConfig(JavaPlugin plugin, org.bukkit.configuration.file.FileConfiguration levelsConfig) {
        Map<Integer, List<String>> byLevel = new HashMap<>();

        ConfigurationSection manorLevels =
                levelsConfig == null ? null : levelsConfig.getConfigurationSection("manor.levels");
        if (manorLevels != null) {
            for (String key : manorLevels.getKeys(false)) {
                int level;
                try {
                    level = Integer.parseInt(key.trim());
                } catch (NumberFormatException e) {
                    plugin.getLogger().warning("manor.levels 含非法等级键 '" + key + "'，已跳过。");
                    continue;
                }
                ConfigurationSection row = manorLevels.getConfigurationSection(key);
                if (row == null) {
                    continue;
                }
                ConfigurationSection upgrade = row.getConfigurationSection("upgrade");
                List<String> cmds = upgrade != null ? upgrade.getStringList("commands") : List.of();
                if (cmds != null && !cmds.isEmpty()) {
                    byLevel.put(level, List.copyOf(cmds));
                }
            }
        }

        if (byLevel.isEmpty()) {
            return null;
        }
        return new ManorUpgradeCommandHook(plugin, byLevel);
    }

    @Override
    public void onUpgrade(Manor manor, int oldLevel) {
        List<String> templates = byLevel.get(manor.level());
        if (templates == null || templates.isEmpty()) {
            return;
        }
        UUID uuid = manor.owner().uuid();
        OfflinePlayer off = Bukkit.getOfflinePlayer(uuid);
        String name = off.getName() != null ? off.getName() : uuid.toString();
        String guild = manor.guild().value();
        int newLevel = manor.level();
        int slot = manor.slot();

        plugin.getServer().getScheduler().runTask(plugin, () -> {
            for (String template : templates) {
                String cmd = template
                        .replace("%player%", name)
                        .replace("%uuid%", uuid.toString())
                        .replace("%guild%", guild)
                        .replace("%level%", Integer.toString(newLevel))
                        .replace("%old_level%", Integer.toString(oldLevel))
                        .replace("%slot%", Integer.toString(slot))
                        .trim();
                if (cmd.isEmpty()) {
                    continue;
                }
                if (cmd.startsWith("/")) {
                    cmd = cmd.substring(1);
                }
                try {
                    Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd);
                } catch (Exception e) {
                    plugin.getLogger().warning("庄园升级命令执行失败 [lv" + newLevel + "] '" + cmd + "': " + e.getMessage());
                }
            }
        });
    }
}
