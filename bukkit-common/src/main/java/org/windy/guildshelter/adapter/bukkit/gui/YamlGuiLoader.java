package org.windy.guildshelter.adapter.bukkit.gui;

import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.port.ui.UiIcon;
import org.windy.guildshelter.domain.port.ui.UiItem;
import org.windy.guildshelter.domain.port.ui.UiView;
import org.windy.guildshelter.domain.rule.LevelRules;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * 从 gui.yml 加载菜单定义，转换为平台中立的 {@link UiView}。
 * 缺失的菜单/按钮用硬编码默认值兜底（{@link Menus}）。
 *
 * <p>材质名只当字符串存进 {@link UiIcon}，不在此解析为 Bukkit {@code Material}——交给具体后端
 * （{@code BukkitInventoryUi} 用 {@code matchMaterial}，模组后端按命名空间 id），保持加载器平台中立。
 */
public final class YamlGuiLoader {

    private static final String[] DEFAULT_MENU_FILES = {
            "controller/manor_controller",
            "controller/controller_info",
            "controller/controller_upgrade",
            "controller/controller_care",
            "controller/controller_security",
            "controller/controller_activity"
    };

    private final YamlConfiguration config;
    private final Logger logger;
    private final File guiDir;

    public YamlGuiLoader(File dataFolder, Logger logger) {
        this.logger = logger;
        this.guiDir = new File(dataFolder, "gui");
        if (!guiDir.exists() && !guiDir.mkdirs()) {
            logger.warning("[GuildShelter] 创建 GUI 目录失败: " + guiDir.getAbsolutePath());
        }
        copyDefaultMenuFiles();
        // 按语言优先级加载：gui_{lang}.yml → gui.yml
        String lang = org.windy.guildshelter.adapter.bukkit.Messages.lang();
        File file = new File(dataFolder, "gui_" + lang + ".yml");
        if (!file.exists()) {
            file = new File(dataFolder, "gui.yml");
        }
        if (!file.exists()) {
            // 从 resources 复制默认文件
            String resName = "/gui_" + lang + ".yml";
            InputStream res = getClass().getResourceAsStream(resName);
            if (res == null) res = getClass().getResourceAsStream("/gui.yml");
            if (res != null) {
                try (InputStreamReader reader = new InputStreamReader(res, StandardCharsets.UTF_8)) {
                    YamlConfiguration defaults = YamlConfiguration.loadConfiguration(reader);
                    defaults.save(file);
                } catch (IOException e) {
                    logger.warning("[GuildShelter] 保存默认 gui.yml 失败: " + e.getMessage());
                }
            }
        }
        this.config = YamlConfiguration.loadConfiguration(file);
        logger.info("[GuildShelter] GUI 配置已加载: " + file.getName());
    }

    /** 加载指定菜单的 {@link UiView}，返回 null 表示未定义（应用硬编码默认）。 */
    public UiView loadMenu(String menuId, Map<String, Object> context) {
        UiView fileMenu = loadMenuFile(menuId, context);
        if (fileMenu != null) {
            return fileMenu;
        }
        ConfigurationSection section = config.getConfigurationSection("menus." + menuId);
        if (section == null) {
            return null;
        }
        String title = section.getString("title", "§8[§6GuildShelter§8]");
        int rows = Math.max(1, Math.min(6, section.getInt("rows", 3)));

        Map<Integer, UiItem> items = new HashMap<>();
        ConfigurationSection itemsSection = section.getConfigurationSection("items");
        if (itemsSection != null) {
            for (String key : itemsSection.getKeys(false)) {
                try {
                    int slot = Integer.parseInt(key);
                    ConfigurationSection itemSec = itemsSection.getConfigurationSection(key);
                    if (itemSec == null) continue;
                    UiItem item = parseItem(itemSec, context);
                    if (item != null) {
                        items.put(slot, item);
                    }
                } catch (NumberFormatException e) {
                    logger.warning("[GuildShelter] gui.yml 菜单 " + menuId + " 的 slot 不是数字: " + key);
                }
            }
        }

        // 填充分隔线（空 slot 用玻璃板）
        String paneMat = config.getString("settings.pane-material", "GRAY_STAINED_GLASS_PANE");
        UiIcon pane = UiIcon.of(paneMat);
        for (int i = 0; i < rows * 9; i++) {
            if (!items.containsKey(i)) {
                items.put(i, UiItem.separator(pane));
            }
        }

        return new UiView(menuId, title, rows, items, context);
    }

    private UiView loadMenuFile(String menuId, Map<String, Object> context) {
        File file = menuFile(menuId);
        if (!file.exists()) {
            return null;
        }
        YamlConfiguration yaml = YamlConfiguration.loadConfiguration(file);
        int rows = rows(yaml);
        Map<Integer, UiItem> items = new HashMap<>();

        parseIndexedItems(yaml.getConfigurationSection("items"), items, context);
        parseIndexedItems(yaml.getConfigurationSection("custom"), items, context);

        String paneMat = yaml.getString("pane-material",
                config.getString("settings.pane-material", "GRAY_STAINED_GLASS_PANE"));
        UiIcon pane = UiIcon.of(paneMat);
        for (int i = 0; i < rows * 9; i++) {
            if (!items.containsKey(i)) {
                items.put(i, UiItem.separator(pane));
            }
        }

        String title = replace(yaml.getString("title", "§8[§6GuildShelter§8]"), context);
        return new UiView(menuId, title, rows, items, context);
    }

    private void parseIndexedItems(ConfigurationSection section, Map<Integer, UiItem> items, Map<String, Object> context) {
        if (section == null) {
            return;
        }
        for (String key : section.getKeys(false)) {
            ConfigurationSection itemSec = section.getConfigurationSection(key);
            if (itemSec == null || !itemSec.getBoolean("enable", true)) {
                continue;
            }
            UiItem item = parseItem(itemSec, context);
            for (int slot : indexes(itemSec.getString("index", key))) {
                items.put(slot, item);
            }
        }
    }

    private UiItem parseItem(ConfigurationSection sec, Map<String, Object> context) {
        String matName = sec.getString("material", "PAPER");
        int customModelData = sec.getInt("custom-model-data", 0);
        String name = replace(sec.getString("name", ""), context);
        List<String> lore = replace(sec.getStringList("lore"), context);
        String action = sec.getString("action", "").trim();
        if (action.isEmpty()) {
            action = normalizeCommand(sec.getString("command", ""));
        }
        return UiItem.of(UiIcon.of(matName, customModelData), name, lore, action);
    }

    private static String normalizeCommand(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = raw.trim();
        if ("close".equals(text) || text.startsWith("menu.") || text.startsWith("command.")) {
            return text;
        }
        if (text.startsWith("/")) {
            text = text.substring(1).trim();
        }
        if (text.regionMatches(true, 0, "gs ", 0, 3)) {
            text = text.substring(3).trim();
        }
        if (text.isEmpty()) {
            return "";
        }
        return "command." + text.toLowerCase(java.util.Locale.ROOT).replace(' ', '.');
    }

    private static int rows(ConfigurationSection section) {
        if (section.contains("rows")) {
            return Math.max(1, Math.min(6, section.getInt("rows", 3)));
        }
        int size = section.getInt("size", 27);
        return Math.max(1, Math.min(6, (int) Math.ceil(size / 9.0)));
    }

    private static List<Integer> indexes(String raw) {
        List<Integer> out = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return out;
        }
        for (String part : raw.split(",")) {
            try {
                int slot = Integer.parseInt(part.trim());
                if (slot >= 0 && slot < 54) {
                    out.add(slot);
                }
            } catch (NumberFormatException ignored) {
                // 非数字 index 忽略。
            }
        }
        return out;
    }

    private static List<String> replace(List<String> lines, Map<String, Object> context) {
        List<String> out = new ArrayList<>();
        for (String line : lines) {
            out.add(replace(line, context));
        }
        return out;
    }

    private static String replace(String text, Map<String, Object> context) {
        if (text == null || text.isEmpty()) {
            return "";
        }
        Manor manor = context.get("manor") instanceof Manor m ? m : null;
        GuildWorld gw = context.get("guildWorld") instanceof GuildWorld g ? g : null;
        LevelRules levels = context.get("levels") instanceof LevelRules l ? l : null;

        Map<String, String> values = new HashMap<>();
        if (manor != null) {
            values.put("slot", String.valueOf(manor.slot()));
            values.put("level", String.valueOf(manor.level()));
            values.put("owner", manor.owner().uuid().toString());
            values.put("unlocked", String.valueOf(manor.unlockedChunks().size()));
        }
        if (gw != null) {
            values.put("guild", gw.guild().value());
            values.put("guild_level", String.valueOf(gw.guildLevel()));
        }
        if (manor != null && gw != null && levels != null) {
            int quota = manor.quotaCap(gw.layout(), levels);
            int nextLevel = Math.min(levels.manorMaxLevel(), manor.level() + 1);
            values.put("max_level", String.valueOf(levels.manorMaxLevel()));
            values.put("quota", String.valueOf(quota));
            values.put("next_level", String.valueOf(nextLevel));
            values.put("next_quota", String.valueOf(levels.manorQuotaCap(gw.layout(), nextLevel)));
        }
        if (gw != null && levels != null) {
            values.put("guild_level_name", levels.guildLevelName(gw.guildLevel()));
        }

        String out = text.replace('&', '§');
        for (Map.Entry<String, String> entry : values.entrySet()) {
            out = out.replace("{" + entry.getKey() + "}", entry.getValue());
        }
        return out;
    }

    private void copyDefaultMenuFiles() {
        for (String menuPath : DEFAULT_MENU_FILES) {
            File target = new File(guiDir, menuPath + ".yml");
            if (target.exists()) {
                continue;
            }
            File parent = target.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                logger.warning("[GuildShelter] 创建 GUI 子目录失败: " + parent.getAbsolutePath());
                continue;
            }
            File legacy = legacyMenuFile(menuPath);
            if (legacy != null && legacy.exists()) {
                try {
                    Files.copy(legacy.toPath(), target.toPath());
                    logger.info("[GuildShelter] 已迁移旧 GUI 菜单到 " + target.getPath());
                    continue;
                } catch (IOException e) {
                    logger.warning("[GuildShelter] 迁移旧 GUI 菜单失败 " + legacy.getName() + ": " + e.getMessage());
                }
            }
            try (InputStream in = getClass().getResourceAsStream("/gui/" + menuPath + ".yml")) {
                if (in != null) {
                    Files.copy(in, target.toPath());
                }
            } catch (IOException e) {
                logger.warning("[GuildShelter] 保存默认 GUI 菜单失败 " + menuPath + ": " + e.getMessage());
            }
        }
    }

    private File menuFile(String menuId) {
        File nested = controllerMenu(menuId)
                ? new File(new File(guiDir, "controller"), menuId + ".yml")
                : new File(guiDir, menuId + ".yml");
        if (nested.exists()) {
            return nested;
        }
        // Backward compatibility for servers that already generated the first flat layout.
        return new File(guiDir, menuId + ".yml");
    }

    private File legacyMenuFile(String menuPath) {
        int slash = menuPath.lastIndexOf('/');
        if (slash < 0) {
            return null;
        }
        return new File(guiDir, menuPath.substring(slash + 1) + ".yml");
    }

    private static boolean controllerMenu(String menuId) {
        return "manor_controller".equals(menuId) || menuId.startsWith("controller_");
    }

    /** 检查某菜单是否在 gui.yml 中定义。 */
    public boolean hasMenu(String menuId) {
        return menuFile(menuId).exists() || config.contains("menus." + menuId);
    }
}
