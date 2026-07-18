package org.windy.guildshelter.adapter.bukkit.gui;

import org.windy.guildshelter.GuildShelterPlugin;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.port.ui.UiIcon;
import org.windy.guildshelter.domain.port.ui.UiItem;
import org.windy.guildshelter.domain.port.ui.UiView;
import org.windy.guildshelter.domain.rule.LevelRules;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 预置菜单构造器：创建各种管理菜单的 {@link UiView}。
 * 优先从 gui.yml 加载（服主可自定义），缺失时用硬编码默认值。
 *
 * <p>平台中立——图标用 {@link UiIcon}（物品名字符串），不引用 {@code org.bukkit.Material}，
 * 由具体后端翻译。<b>注：尚无命令调用这些构造器，GUI 入口待接</b>（见 {@code UiActionRouter}）。
 */
public final class Menus {

    private Menus() {}

    /**
     * 尝试从 YAML 加载菜单，未定义则返回 null（调用方 fallback 到硬编码）。
     */
    public static UiView fromYaml(String menuId, Map<String, Object> context) {
        YamlGuiLoader loader = GuildShelterPlugin.guiLoader();
        return loader != null ? loader.loadMenu(menuId, context) : null;
    }

    /** 庄园信息面板（YAML 优先，硬编码兜底）。 */
    public static UiView manorInfoYaml(Manor manor, GuildWorld gw, LevelRules levels) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw);
        UiView yaml = fromYaml("manor_info", ctx);
        if (yaml != null) return yaml;
        return manorInfo(manor, gw, levels);
    }

    /** 成员管理面板（YAML 优先，硬编码兜底）。 */
    public static UiView memberManagerYaml(Manor manor) {
        Map<String, Object> ctx = Map.of("manor", manor);
        UiView yaml = fromYaml("member_manager", ctx);
        if (yaml != null) return yaml;
        return memberManager(manor);
    }

    /** 庄园信息面板。 */
    public static UiView manorInfo(Manor manor, GuildWorld gw, LevelRules levels) {
        Map<Integer, UiItem> items = new HashMap<>();
        int slot = 0;

        // 基本信息
        items.put(slot++, UiItem.of(UiIcon.of("book"),
                "§6庄园 #" + manor.slot(),
                List.of("§7公会: §f" + manor.guild().value(),
                        "§7等级: §f" + manor.level() + "/" + levels.manorMaxLevel(),
                        "§7庄主: §f" + manor.owner()),
                ""));

        // Flag 编辑按钮
        items.put(slot++, UiItem.of(UiIcon.of("redstone_torch"),
                "§e编辑 Flag",
                List.of("§7点击打开 flag 编辑器"),
                "menu.flags"));

        // 成员管理按钮
        items.put(slot++, UiItem.of(UiIcon.of("player_head"),
                "§e成员管理",
                List.of("§7trusted: §f" + manor.coBuilders().size(),
                        "§7member: §f" + manor.members().size(),
                        "§7denied: §c" + manor.denied().size()),
                "menu.members"));

        // 模板按钮
        items.put(slot++, UiItem.of(UiIcon.of("writable_book"),
                "§e权限模板",
                List.of("§7点击管理权限模板"),
                "menu.templates"));

        // 子领地按钮
        items.put(slot++, UiItem.of(UiIcon.of("map"),
                "§e子领地",
                List.of("§7点击管理子领地"),
                "menu.subs"));

        // 分隔线
        for (int i = slot; i < 9; i++) {
            items.put(i, UiItem.separator(UiIcon.of("gray_stained_glass_pane")));
        }

        // 描述
        String desc = Flag.DESCRIPTION.resolveString(manor.flags());
        if (!desc.isBlank()) {
            items.put(9, UiItem.of(UiIcon.of("paper"), "§7描述: §f" + desc, List.of(), ""));
        }

        // 入场费
        double price = Flag.PRICE.resolveDouble(manor.flags());
        if (price > 0) {
            items.put(10, UiItem.of(UiIcon.of("gold_ingot"), "§7入场费: §e" + price, List.of(), ""));
        }

        return new UiView("manor_info", "§8[§6庄园管理§8] §7#" + manor.slot(), 3, items,
                Map.of("manor", manor, "guildWorld", gw));
    }

    /** Flag 编辑器（按类别分页）。 */
    public static UiView flagEditor(Manor manor, int page) {
        Flag[] flags = Flag.values();
        int perPage = 27; // 3 行
        int start = page * perPage;
        int end = Math.min(start + perPage, flags.length);
        int maxPage = (flags.length + perPage - 1) / perPage - 1;

        Map<Integer, UiItem> items = new HashMap<>();
        int slot = 0;
        for (int i = start; i < end; i++) {
            Flag f = flags[i];
            String current = manor.flags().getOrDefault(f.id(), f.defaultValue() + " §8(默认)");
            boolean isDefault = !manor.flags().containsKey(f.id());
            items.put(slot++, UiItem.of(
                    UiIcon.of(f.type() == org.windy.guildshelter.domain.flag.FlagType.BOOLEAN ? "lever" : "paper"),
                    "§f" + f.id() + " §7= " + (isDefault ? "§8" + current : "§a" + current),
                    List.of("§8" + Messages.get(f.description()), "§7类型: " + f.type(), "", 
                            f.type() == org.windy.guildshelter.domain.flag.FlagType.BOOLEAN ? "§e点击切换" : "§8文本/数值 Flag 请使用 /gs flag set"),
                    f.type() == org.windy.guildshelter.domain.flag.FlagType.BOOLEAN ? "flag.toggle." + f.id() : ""));
        }

        // 翻页按钮
        if (page > 0) items.put(27, UiItem.of(UiIcon.of("arrow"), "§e上一页", "menu.flags.page." + (page - 1)));
        if (page < maxPage) items.put(35, UiItem.of(UiIcon.of("arrow"), "§e下一页", "menu.flags.page." + (page + 1)));

        // 返回按钮
        items.put(31, UiItem.of(UiIcon.of("barrier"), "§c返回", "menu.info"));

        return new UiView("flag_editor", "§8[§6Flag 编辑器§8] §7页 " + (page + 1) + "/" + (maxPage + 1), 4, items,
                Map.of("manor", manor, "page", page), page);
    }

    /** 成员管理面板。 */
    public static UiView memberManager(Manor manor) {
        Map<Integer, UiItem> items = new HashMap<>();
        int slot = 0;

        // Owner
        items.put(slot++, UiItem.of(UiIcon.of("golden_helmet"), "§6庄主", List.of("§f" + manor.owner()), ""));

        // Trusted
        items.put(slot++, UiItem.of(UiIcon.of("diamond_helmet"), "§b共建人 (" + manor.coBuilders().size() + ")",
                manor.coBuilders().stream().map(r -> "§7- §f" + r).toList(), ""));

        // Members
        items.put(slot++, UiItem.of(UiIcon.of("iron_helmet"), "§a成员 (" + manor.members().size() + ")",
                manor.members().stream().map(r -> "§7- §f" + r).toList(), ""));

        // Denied
        items.put(slot++, UiItem.of(UiIcon.of("redstone_block"), "§c黑名单 (" + manor.denied().size() + ")",
                manor.denied().stream().map(r -> "§7- §f" + r).toList(), ""));

        // 返回
        items.put(8, UiItem.of(UiIcon.of("barrier"), "§c返回资料页", "menu.controller.info"));

        return new UiView("member_manager", "§8[§6成员管理§8]", 3, items, Map.of("manor", manor));
    }

    /** 庄园控制器首页：只做导航与核心状态展示。 */
    public static UiView manorController(Manor manor, GuildWorld gw, LevelRules levels) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw, "levels", levels);
        UiView yaml = fromYaml("manor_controller", ctx);
        if (yaml != null) return yaml;
        Map<Integer, UiItem> items = framed();
        int quota = manor.quotaCap(gw.layout(), levels);
        int used = manor.unlockedChunks().size();
        int nextQuota = manor.level() < levels.manorMaxLevel()
                ? levels.manorQuotaCap(gw.layout(), manor.level() + 1)
                : quota;

        items.put(4, UiItem.of(UiIcon.of("lectern"),
                "§6§l庄园控制器",
                List.of("§7公会: §f" + manor.guild().value(),
                        "§7庄园: §f#" + manor.slot() + " §8| §7Lv§f" + manor.level() + "/" + levels.manorMaxLevel(),
                        "§7已解锁: §f" + used + "§7/§f" + quota + " chunk",
                        "§7下级额度: §f" + nextQuota + " chunk"),
                ""));

        items.put(19, UiItem.of(UiIcon.of("book"),
                "§e资料",
                List.of("§7庄园信息、成员、权限与传送点。"),
                "menu.controller.info"));
        items.put(21, UiItem.of(UiIcon.of("experience_bottle"),
                "§a升级",
                List.of("§7查看当前等级、下级额度与升级入口。"),
                "menu.controller.upgrade"));
        items.put(23, UiItem.of(UiIcon.of("anvil"),
                "§b美化与护理",
                List.of("§7清理、模板、搬家与建造辅助。"),
                "menu.controller.care"));
        items.put(25, UiItem.of(UiIcon.of("shield"),
                "§c金库与安保",
                List.of("§7访客开放、黑名单、审计与安全设置。"),
                "menu.controller.security"));
        items.put(29, UiItem.of(UiIcon.of("player_head"),
                "§a成员",
                List.of("§7庄主、共建人、成员与黑名单总览。"),
                "menu.controller.members"));
        items.put(31, UiItem.of(UiIcon.of("comparator"),
                "§b限额监控",
                List.of("§7查看实体、掉落物、方块实体与机器配额。"),
                "menu.controller.limits"));
        items.put(40, UiItem.of(UiIcon.of("wheat"),
                "§d活动",
                List.of("§7花、留言、评分等庄园互动。"),
                "menu.controller.activity"));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭", "close"));

        return new UiView("manor_controller", "§8[§6庄园控制器§8]", 6, items,
                ctx);
    }


    public static UiView controllerQuick(Manor manor, GuildWorld gw) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw);
        UiView yaml = fromYaml("controller_quick", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("compass"), "\u00a7e快捷操作",
                List.of("\u00a77只放不需要文字输入的现有功能。"), ""));
        items.put(10, UiItem.of(UiIcon.of("ender_pearl"), "\u00a7a回家",
                List.of("\u00a77执行 /gs home"), "command.home"));
        items.put(11, UiItem.of(UiIcon.of("compass"), "\u00a7a设置 Home",
                List.of("\u00a77执行 /gs sethome"), "command.sethome"));
        items.put(12, UiItem.of(UiIcon.of("lodestone"), "\u00a7a营地出生点",
                List.of("\u00a77执行 /gs spawn"), "command.spawn"));
        items.put(13, UiItem.of(UiIcon.of("target"), "\u00a7a庄园中心",
                List.of("\u00a77执行 /gs middle"), "command.middle"));
        items.put(14, UiItem.of(UiIcon.of("diamond_pickaxe"), "\u00a7a解锁脚下区块",
                List.of("\u00a77执行 /gs unlock"), "command.unlock"));
        items.put(19, UiItem.of(UiIcon.of("oak_door"), "\u00a7a开放访客 60 分钟",
                List.of("\u00a77执行 /gs open 60"), "command.open.60"));
        items.put(20, UiItem.of(UiIcon.of("iron_door"), "\u00a7c关闭访客",
                List.of("\u00a77执行 /gs close"), "command.close"));
        items.put(21, UiItem.of(UiIcon.of("iron_shovel"), "\u00a7e清理地表",
                List.of("\u00a77执行 /gs clear"), "command.clear"));
        items.put(22, UiItem.of(UiIcon.of("minecart"), "\u00a7e搬家",
                List.of("\u00a78用法: \u00a7f/gs move <公会名>"), ""));
        items.put(24, UiItem.of(UiIcon.of("writable_book"), "\u00a7f文本类功能",
                List.of("\u00a77描述、公告、留言、问候语、告别语。",
                        "\u00a78这些需要输入内容，继续使用聊天命令。"), ""));
        items.put(27, UiItem.of(UiIcon.of("arrow"), "\u00a7e返回控制器", "menu.controller"));
        items.put(31, UiItem.of(UiIcon.of("barrier"), "\u00a7c关闭", "close"));
        return new UiView("controller_quick", "\u00a70庄园控制器 - 快捷操作", 4, items, ctx);
    }
    public static UiView controllerInfo(Manor manor, GuildWorld gw, LevelRules levels) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw, "levels", levels);
        UiView yaml = fromYaml("controller_info", ctx);
        if (yaml != null) return yaml;
        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("book"),
                "§e资料",
                List.of("§7庄园 #" + manor.slot(),
                        "§7公会: §f" + manor.guild().value(),
                        "§7庄主: §f" + manor.owner().uuid(),
                        "§7公会等级: §f" + gw.guildLevel() + " §8(" + levels.guildLevelName(gw.guildLevel()) + "§8)"),
                ""));
        items.put(20, UiItem.of(UiIcon.of("map"), "§a庄园信息",
                List.of("§8当前庄园总览",
                        "§7这里集中展示庄园等级、地皮额度、",
                        "§7公会归属和基础管理状态。",
                        "",
                        "§8这是资料页内的信息卡，不会刷聊天文字。"),
                ""));
        items.put(21, UiItem.of(UiIcon.of("chest"), "§a我的庄园",
                List.of("§8多庄园管理说明",
                        "§7用于整理你名下的地皮、默认 Home",
                        "§7以及后续别院/多地皮切换入口。",
                        "",
                        "§8独立列表页完成前，这里先作为说明卡展示。"),
                ""));
        items.put(22, UiItem.of(UiIcon.of("ender_pearl"), "§a回家",
                List.of("§8快速返回建设点",
                        "§7优先使用你设置过的 Home。",
                        "§7没有 Home 时，会回到庄园入口。",
                        "",
                        "§e点击立即传送"),
                "command.home"));
        items.put(23, UiItem.of(UiIcon.of("compass"), "§a设置 Home",
                List.of("§8保存当前落点",
                        "§7把脚下位置设为以后回家的传送点。",
                        "§7适合放在大门、仓库或主楼入口。",
                        "",
                        "§e点击设置当前位置"),
                "command.sethome"));
        items.put(24, UiItem.of(UiIcon.of("player_head"), "§a成员管理",
                List.of("§8协作与访问关系",
                        "§7查看共建人、普通成员和黑名单。",
                        "§7成员页会展示当前名单，不会刷聊天。",
                        "",
                        "§e点击进入成员页"),
                "menu.controller.members"));
        addBack(items);
        return page("controller_info", "资料", manor, gw, items);
    }

    public static UiView controllerMembers(Manor manor, GuildWorld gw, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        if (values != null) {
            ctx.putAll(values);
        }
        UiView yaml = fromYaml("controller_members", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("player_head"), "§a成员总览",
                List.of("§7庄主: §f" + ctx.getOrDefault("owner_name", manor.owner().uuid()),
                        "§7状态: §f" + ctx.getOrDefault("owner_status", "离线"),
                        "§7在线: §f" + ctx.getOrDefault("total_online", "0") + "§7/§f" + ctx.getOrDefault("total_count", "1")), ""));
        items.put(20, UiItem.of(UiIcon.of("diamond_helmet"), "§b共建人",
                List.of("§7总数: §f" + ctx.getOrDefault("trusted_count", manor.coBuilders().size()),
                        "§7在线: §f" + ctx.getOrDefault("trusted_online", "0"),
                        "§7预览: §f" + ctx.getOrDefault("trusted_preview", "无")), ""));
        items.put(21, UiItem.of(UiIcon.of("iron_helmet"), "§a普通成员",
                List.of("§7总数: §f" + ctx.getOrDefault("member_count", manor.members().size()),
                        "§7在线: §f" + ctx.getOrDefault("member_online", "0"),
                        "§7预览: §f" + ctx.getOrDefault("member_preview", "无")), ""));
        items.put(22, UiItem.of(UiIcon.of("redstone_block"), "§c黑名单",
                List.of("§7总数: §f" + ctx.getOrDefault("deny_count", manor.denied().size()),
                        "§7在线: §f" + ctx.getOrDefault("deny_online", "0"),
                        "§7预览: §f" + ctx.getOrDefault("deny_preview", "无")), ""));
        items.put(24, UiItem.of(UiIcon.of("book"), "§e完整名单",
                List.of("§7打开完整成员列表。", "§8包含庄主、共建人、普通成员和黑名单。"),
                "menu.members"));
        addBack(items);
        return page("controller_members", "成员", manor, gw, items);
    }

    public static UiView controllerUpgrade(Manor manor, GuildWorld gw, LevelRules levels) {
        return controllerUpgrade(manor, gw, levels, Map.of());
    }


    public static UiView controllerMemberOps(Manor manor, GuildWorld gw) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        UiView yaml = fromYaml("controller_member_ops", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("writable_book"), "§e成员操作",
                List.of("§7庄园: §f#" + manor.slot() + " §8| §7公会: §f" + gw.guild().value(),
                        "§7在线: §f0 §7/ §f0",
                        "§7共建: §f" + manor.coBuilders().size() + " §8| §7成员: §f" + manor.members().size() + " §8| §7黑名单: §c" + manor.denied().size(),
                        "",
                        "§8这里不做铁砧输入，玩家名继续走聊天命令。"), ""));
        items.put(19, UiItem.of(UiIcon.of("diamond_helmet"), "§b添加共建人",
                List.of("§7用法: §f/gs trust <玩家>", "§7共建: §f" + manor.coBuilders().size()), ""));
        items.put(20, UiItem.of(UiIcon.of("chainmail_helmet"), "§b移除共建人",
                List.of("§7用法: §f/gs untrust <玩家>"), ""));
        items.put(21, UiItem.of(UiIcon.of("iron_helmet"), "§a添加普通成员",
                List.of("§7用法: §f/gs member add <玩家>"), ""));
        items.put(22, UiItem.of(UiIcon.of("leather_helmet"), "§a移除普通成员",
                List.of("§7用法: §f/gs member remove <玩家>"), ""));
        items.put(23, UiItem.of(UiIcon.of("redstone_block"), "§c加入黑名单",
                List.of("§7用法: §f/gs deny <玩家>"), ""));
        items.put(24, UiItem.of(UiIcon.of("redstone_torch"), "§c移出黑名单",
                List.of("§7用法: §f/gs undeny <玩家>"), ""));
        items.put(30, UiItem.of(UiIcon.of("iron_boots"), "§e踢出访客",
                List.of("§7用法: §f/gs kick <玩家>"), ""));
        items.put(31, UiItem.of(UiIcon.of("map"), "§e庄园名片",
                List.of("§7用法: §f/gs card [玩家]"), ""));
        items.put(32, UiItem.of(UiIcon.of("name_tag"), "§e庄园别名",
                List.of("§7用法: §f/gs alias <名称>", "§7清空: §f/gs alias"), ""));
        items.put(40, UiItem.of(UiIcon.of("book"), "§e完整名单",
                List.of("§7查看庄主、共建人、成员和黑名单。"), "menu.members"));
        addBack(items);
        return page("controller_member_ops", "成员操作", manor, gw, items, ctx);
    }

    public static UiView controllerMemberOps(Manor manor, GuildWorld gw, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        if (values != null) {
            ctx.putAll(values);
        }
        UiView yaml = fromYaml("controller_member_ops", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("writable_book"), "§e成员操作",
                List.of("§7庄园: §f#" + manor.slot() + " §8| §7公会: §f" + gw.guild().value(),
                        "§7在线: §f" + ctx.getOrDefault("total_online", 0) + " §7/ §f" + ctx.getOrDefault("total_count", 0),
                        "§7共建: §f" + ctx.getOrDefault("trusted_count", manor.coBuilders().size()) + " §8| §7成员: §f" + ctx.getOrDefault("member_count", manor.members().size()) + " §8| §7黑名单: §c" + ctx.getOrDefault("deny_count", manor.denied().size()),
                        "",
                        "§8这里不做铁砧输入，玩家名继续走聊天命令。"), ""));
        items.put(19, UiItem.of(UiIcon.of("diamond_helmet"), "§b添加共建人",
                List.of("§7用法: §f/gs trust <玩家>", "§7共建: §f" + ctx.getOrDefault("trusted_preview", "无")), ""));
        items.put(20, UiItem.of(UiIcon.of("chainmail_helmet"), "§b移除共建人",
                List.of("§7用法: §f/gs untrust <玩家>", "§7共建: §f" + ctx.getOrDefault("trusted_preview", "无")), ""));
        items.put(21, UiItem.of(UiIcon.of("iron_helmet"), "§a添加普通成员",
                List.of("§7用法: §f/gs member add <玩家>", "§7成员: §f" + ctx.getOrDefault("member_preview", "无")), ""));
        items.put(22, UiItem.of(UiIcon.of("leather_helmet"), "§a移除普通成员",
                List.of("§7用法: §f/gs member remove <玩家>", "§7成员: §f" + ctx.getOrDefault("member_preview", "无")), ""));
        items.put(23, UiItem.of(UiIcon.of("redstone_block"), "§c加入黑名单",
                List.of("§7用法: §f/gs deny <玩家>", "§7黑名单: §f" + ctx.getOrDefault("deny_preview", "无")), ""));
        items.put(24, UiItem.of(UiIcon.of("redstone_torch"), "§c移出黑名单",
                List.of("§7用法: §f/gs undeny <玩家>", "§7黑名单: §f" + ctx.getOrDefault("deny_preview", "无")), ""));
        items.put(30, UiItem.of(UiIcon.of("iron_boots"), "§e踢出访客",
                List.of("§7用法: §f/gs kick <玩家>", "§8目标必须在线且在庄园世界内。"), ""));
        items.put(31, UiItem.of(UiIcon.of("map"), "§e庄园名片",
                List.of("§7用法: §f/gs card [玩家]", "§8可展示等级、别名、描述和评分摘要。"), ""));
        items.put(32, UiItem.of(UiIcon.of("name_tag"), "§e庄园别名",
                List.of("§7用法: §f/gs alias <名称>", "§7清空: §f/gs alias"), ""));
        items.put(40, UiItem.of(UiIcon.of("book"), "§e完整名单",
                List.of("§7查看庄主、共建人、成员和黑名单。"), "menu.members"));
        addBack(items);
        return page("controller_member_ops", "成员操作", manor, gw, items, ctx);
    }    public static UiView controllerUpgrade(Manor manor, GuildWorld gw, LevelRules levels, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        ctx.put("levels", levels);
        if (values != null) {
            ctx.putAll(values);
        }
        UiView yaml = fromYaml("controller_upgrade", ctx);
        if (yaml != null) return yaml;
        Map<Integer, UiItem> items = framed();
        int current = manor.quotaCap(gw.layout(), levels);
        int used = manor.unlockedChunks().size();
        boolean maxed = manor.level() >= levels.manorMaxLevel();
        int nextLevel = Math.min(levels.manorMaxLevel(), manor.level() + 1);
        int next = maxed ? current : levels.manorQuotaCap(gw.layout(), nextLevel);

        items.put(4, UiItem.of(UiIcon.of("experience_bottle"),
                "§a升级",
                List.of("§7当前等级: §f" + manor.level() + "/" + levels.manorMaxLevel(),
                        "§7当前额度: §f" + used + "§7/§f" + current + " chunk",
                        maxed ? "§6已达到满级" : "§7下一级: §fLv" + nextLevel + " §8| §7额度 §f" + next,
                        "",
                        "§8升级会提升庄园可建设范围。",
                        "§8脚下区块解锁和庄园升级是两套消耗。"),
                ""));
        items.put(20, UiItem.of(UiIcon.of("emerald"),
                maxed ? "§6已满级" : "§a确认升级",
                List.of("§8提交庄园升级",
                        "§7目标等级: §fLv" + nextLevel,
                        "§7升级后额度: §f" + next + " chunk",
                        "",
                        "§7会先完整检查两件事:",
                        "§8▪ §7Vault 余额是否足够",
                        "§8▪ §7背包材料是否全部满足",
                        "",
                        "§a全部满足后才扣钱扣物。",
                        "§c任一条件不足不会扣除任何东西。",
                        "",
                        maxed ? "§6当前已达到最高等级" : "§e点击升级到 Lv" + nextLevel),
                "command.upgrade"));
        items.put(22, UiItem.of(UiIcon.of("gold_ingot"),
                "§eVault + 物品消耗",
                List.of("§8升级消耗说明",
                        "§7金币和材料由服主在 levels.yml 配置。",
                        "§7路径: §fmanor.levels.<等级>.upgrade",
                        "",
                        "§7物品只从玩家背包扣除。",
                        "§7如果背包数量不足，升级不会开始。",
                        "",
                        "§8GUI 只负责提交升级请求，",
                        "§8真正校验和扣除都在命令层完成。"),
                ""));
        items.put(24, UiItem.of(UiIcon.of("diamond_pickaxe"),
                "§a解锁脚下区块",
                List.of("§8扩展可建造范围",
                        "§7站在相邻的未解锁区块上使用。",
                        "§7消耗庄园额度，不消耗升级材料。",
                        "",
                        "§e点击解锁脚下区块"),
                "command.unlock"));
        addBack(items);
        return page("controller_upgrade", "升级", manor, gw, items);
    }

    public static UiView controllerLimits(Manor manor, GuildWorld gw, LevelRules levels, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        ctx.put("levels", levels);
        if (values != null) {
            ctx.putAll(values);
        }
        UiView yaml = fromYaml("controller_limits", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("comparator"), "§b限额监控",
                List.of("§7查看当前庄园实体、掉落物、方块实体与机器配额。"), ""));
        items.put(20, UiItem.of(UiIcon.of("hopper"), "§e方块实体",
                List.of("§7当前: §f" + ctx.getOrDefault("tiles_used", "?"),
                        "§7上限: §f" + ctx.getOrDefault("tiles_cap", "?")), ""));
        items.put(21, UiItem.of(UiIcon.of("bone"), "§e掉落物",
                List.of("§7当前: §f" + ctx.getOrDefault("drops_used", "?"),
                        "§7上限: §f" + ctx.getOrDefault("drops_cap", "?")), ""));
        items.put(22, UiItem.of(UiIcon.of("zombie_head"), "§e生物总数",
                List.of("§7当前: §f" + ctx.getOrDefault("mob_used", "?"),
                        "§7上限: §f" + ctx.getOrDefault("mob_cap", "?")), ""));
        items.put(23, UiItem.of(UiIcon.of("minecart"), "§e载具",
                List.of("§7当前: §f" + ctx.getOrDefault("vehicle_used", "?"),
                        "§7上限: §f" + ctx.getOrDefault("vehicle_cap", "?")), ""));
        items.put(24, UiItem.of(UiIcon.of("furnace"), "§e机器配额",
                List.of("§7已配置: §f" + ctx.getOrDefault("machine_count", "0") + " 项",
                        "§7摘要: §f" + ctx.getOrDefault("machine_summary", "无")), ""));
        addBack(items);
        return page("controller_limits", "限额", manor, gw, items);
    }

    public static UiView controllerFlags(Manor manor, GuildWorld gw, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        if (values != null) {
            ctx.putAll(values);
        }
        UiView yaml = fromYaml("controller_flags", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("redstone_torch"), "§e安保开关",
                List.of("§7自定义 Flag: §f" + ctx.getOrDefault("flag_custom_count", "0") + " 项",
                        "§8点击开关会立即保存并刷新当前页。"), ""));
        items.put(20, flagToggle("oak_door", "§e谢客进入", "deny_entry", ctx, "flag.toggle.deny-entry"));
        items.put(21, flagToggle("iron_door", "§e限制离开", "deny_exit", ctx, "flag.toggle.deny-exit"));
        items.put(22, flagToggle("diamond_sword", "§cPVP", "pvp", ctx, "flag.toggle.pvp"));
        items.put(23, flagToggle("iron_sword", "§cPVE", "pve", ctx, "flag.toggle.pve"));
        items.put(24, flagToggle("totem_of_undying", "§d无敌", "invincible", ctx, "flag.toggle.invincible"));
        items.put(29, flagToggle("lever", "§a普通交互", "use", ctx, "flag.toggle.use"));
        items.put(30, flagToggle("chest", "§a容器访问", "container", ctx, "flag.toggle.container"));
        items.put(31, flagToggle("item_frame", "§a展示框/盔甲架", "item_frame", ctx, "flag.toggle.item-frame"));
        items.put(32, flagToggle("minecart", "§a载具交互", "vehicle", ctx, "flag.toggle.vehicle-use"));
        items.put(33, flagToggle("name_tag", "§b标题提示", "titles", ctx, "flag.toggle.titles"));
        items.put(38, flagToggle("bell", "§b进入通知", "notify_enter", ctx, "flag.toggle.notify-enter"));
        items.put(39, flagToggle("bell", "§b离开通知", "notify_leave", ctx, "flag.toggle.notify-leave"));
        items.put(41, UiItem.of(UiIcon.of("book"), "§e完整 Flag 列表",
                List.of("§7" + ctx.getOrDefault("more_flags_text", "查看所有 Flag"), "§e点击打开完整编辑器"),
                "menu.controller.flags.full"));
        addBack(items);
        items.put(45, UiItem.of(UiIcon.of("arrow"), "§e返回安保页",
                List.of("§7回到金库与安保状态页。"), "menu.controller.security"));
        return page("controller_flags", "安保开关", manor, gw, items);
    }

    private static UiItem flagToggle(String icon, String name, String key, Map<String, Object> ctx, String action) {
        return UiItem.of(UiIcon.of(icon), name,
                List.of("§7当前: §f" + ctx.getOrDefault(key + "_status", "?"),
                        "§e" + ctx.getOrDefault(key + "_next", "点击切换")),
                action);
    }

    public static UiView controllerCare(Manor manor, GuildWorld gw) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw);
        UiView yaml = fromYaml("controller_care", ctx);
        if (yaml != null) return yaml;
        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("anvil"), "§b美化与护理",
                List.of("§7面向建设前后的整理动作。",
                        "§7会改动世界的项目保留明确提示。",
                        "",
                        "§8不能在 GUI 内输入文字的项目，",
                        "§8这里先做成说明卡，不刷聊天。"), ""));
        items.put(20, UiItem.of(UiIcon.of("iron_shovel"), "§a清理地表",
                List.of("§8清理当前庄园地表",
                        "§7用于重新规划建筑或清除自然杂物。",
                        "§7适合刚领地皮后快速整理入口区域。",
                        "",
                        "§8建议先把重要方块、箱子和机器挪走。",
                        "§c会改动地表，请确认站在目标庄园。",
                        "",
                        "§e点击开始清理流程"),
                "command.clear"));
        items.put(21, UiItem.of(UiIcon.of("writable_book"), "§a建筑模板",
                List.of("§8建筑方案说明",
                        "§7模板适合保存成熟建筑布局，",
                        "§7以后可以迁移、复原或给其他庄园套用。",
                        "",
                        "§7常见用途:",
                        "§8▪ §7保存一套别墅结构",
                        "§8▪ §7给新地皮快速复原布局",
                        "§8▪ §7活动建筑统一发放",
                        "",
                        "§8当前 GUI 暂不承载模板命名输入。"),
                ""));
        items.put(22, UiItem.of(UiIcon.of("minecart"), "§a搬家",
                List.of("§8迁移庄园位置",
                        "§7适合调整邻居、道路或景观布局。",
                        "§7会遵守费用、冷却与确认流程。",
                        "",
                        "§7搬家会保留庄园结构和数据，",
                        "§7但仍建议在低峰期操作。",
                        "",
                        "§e点击进入搬家流程"),
                "command.move"));
        items.put(23, UiItem.of(UiIcon.of("target"), "§a庄园中心",
                List.of("§8定位完整地皮中心",
                        "§7无视 Home 设置，用于勘察或对齐建筑。",
                        "§7适合确认庄园边界、中心轴和主建筑方向。",
                        "",
                        "§e点击传送到中心"),
                "command.middle"));
        items.put(24, UiItem.of(UiIcon.of("oak_sign"), "§a描述/展示",
                List.of("§8展示文本说明",
                        "§7庄园描述会展示给访客，",
                        "§7也适合写主题、规则或建筑介绍。",
                        "",
                        "§7建议内容:",
                        "§8▪ §7庄园主题",
                        "§8▪ §7参观提醒",
                        "§8▪ §7活动说明",
                        "",
                        "§8当前 GUI 暂不承载文本输入。"),
                ""));
        addBack(items);
        return page("controller_care", "美化与护理", manor, gw, items);
    }


    public static UiView controllerMaintenance(Manor manor, GuildWorld gw) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw);
        UiView yaml = fromYaml("controller_maintenance", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("crafting_table"), "§e维护与迁移",
                List.of("§7庄园: §f#" + manor.slot() + " §8| §7公会: §f" + gw.guild().value(),
                        "",
                        "§8需要参数的项目只展示用法，不执行空命令。"), ""));
        items.put(19, UiItem.of(UiIcon.of("iron_shovel"), "§a清理地表",
                List.of("§7用法: §f/gs clear", "§7命令层会要求二次确认。", "", "§e点击执行清理流程"), "command.clear"));
        items.put(20, UiItem.of(UiIcon.of("writable_book"), "§a建筑模板",
                List.of("§7查看: §f/gs template list", "§7创建: §f/gs template create <名称>", "§7应用: §f/gs template apply <名称>"), ""));
        items.put(21, UiItem.of(UiIcon.of("structure_block"), "§a结构文件",
                List.of("§7列出: §f/gs template list-schematics", "§7保存: §f/gs template save <名称>", "§7粘贴: §f/gs template paste <名称>"), ""));
        items.put(22, UiItem.of(UiIcon.of("map"), "§b子领地",
                List.of("§7创建: §f/gs sub create <名称>", "§7列表: §f/gs sub list", "§7Flag: §f/gs sub setflag <名称> <flag> <值>"), ""));
        items.put(23, UiItem.of(UiIcon.of("minecart"), "§e搬家",
                List.of("§7用法: §f/gs move <公会名>", "§7会检查费用、冷却、容量和风险。"), ""));
        items.put(24, UiItem.of(UiIcon.of("ender_pearl"), "§e交换庄园",
                List.of("§7用法: §f/gs swap <玩家>", "§7多庄园时以脚下庄园为目标。"), ""));
        items.put(29, UiItem.of(UiIcon.of("anvil"), "§6合并庄园",
                List.of("§7合并: §f/gs merge <slot>", "§7取消: §f/gs unmerge [slot]"), ""));
        items.put(30, UiItem.of(UiIcon.of("oak_sign"), "§f展示文本",
                List.of("§7描述: §f/gs desc <内容>", "§7公告: §f/gs bulletin set <内容>", "§7查看: §f/gs bulletin show"), ""));
        items.put(31, UiItem.of(UiIcon.of("redstone_torch"), "§c确认机制",
                List.of("§7clear / move / swap / merge / unmerge", "§7确认: §f/gs confirm"), ""));
        items.put(45, UiItem.of(UiIcon.of("arrow"), "§e返回护理页",
                List.of("§7回到美化与护理。"), "menu.controller.care"));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭", "close"));
        return page("controller_maintenance", "维护与迁移", manor, gw, items, ctx);
    }
    public static UiView controllerActivity(Manor manor, GuildWorld gw) {
        return controllerActivity(manor, gw, Map.of());
    }

    public static UiView controllerActivity(Manor manor, GuildWorld gw, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        if (values != null) {
            ctx.putAll(values);
        }
        UiView yaml = fromYaml("controller_activity", ctx);
        if (yaml != null) return yaml;
        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("wheat"), "§d活动",
                List.of("§7庄园互动、留言与排行入口。",
                        "§7送花、评分和留言墙都从这里进入。",
                        "",
                        "§8这里先展示现成数据和快捷入口。"), ""));
        items.put(20, UiItem.of(UiIcon.of("flower_pot"), "§d送花",
                List.of("§8给喜欢的庄园一点反馈",
                        "§7用于访客互动和人气展示。",
                        "§7当前庄园今天收到: §f" + ctx.getOrDefault("flower_today", "0"),
                        "",
                        "§e点击送花"),
                "command.flower"));
        items.put(21, UiItem.of(UiIcon.of("oak_sign"), "§a留言墙",
                List.of("§8查看脚下庄园留言",
                        "§7最近留言: §f" + ctx.getOrDefault("comment_count", "0"),
                        "§7最新: §f" + ctx.getOrDefault("comment_preview_1", "无"),
                        "",
                        "§e点击打开留言墙"),
                "menu.controller.activity.board"));
        items.put(22, UiItem.of(UiIcon.of("name_tag"), "§a留言",
                List.of("§8留下文本留言",
                        "§7留言需要输入内容，不能用单次点击直接完成。",
                        "§7请用命令输入留言内容。",
                        "",
                        "§8当前先作为说明卡。"),
                ""));
        items.put(23, UiItem.of(UiIcon.of("nether_star"), "§e评分",
                List.of("§8为庄园打分",
                        "§7平均分: §f" + ctx.getOrDefault("rating_avg", "0.0") + "§7 / §f" + ctx.getOrDefault("rating_count", "0") + " 票",
                        "§7你的评分: §f" + ctx.getOrDefault("my_rating", "0"),
                        "",
                        "§e点击打开评分页"),
                "menu.controller.activity.rate"));
        items.put(24, UiItem.of(UiIcon.of("chest"), "§a收件箱",
                List.of("§8查看收到的互动",
                        "§7用于查看留言、反馈和系统通知。",
                        "§7如果你是庄主，也可以用来回看访客留言。",
                        "",
                        "§e点击打开收件箱"),
                "menu.controller.activity.inbox"));
        items.put(29, UiItem.of(UiIcon.of("paper"), "§7最近留言 1",
                List.of("§f" + ctx.getOrDefault("comment_preview_1", "无")), ""));
        items.put(30, UiItem.of(UiIcon.of("paper"), "§7最近留言 2",
                List.of("§f" + ctx.getOrDefault("comment_preview_2", "无")), ""));
        items.put(31, UiItem.of(UiIcon.of("paper"), "§7最近留言 3",
                List.of("§f" + ctx.getOrDefault("comment_preview_3", "无")), ""));
        addBack(items);
        return page("controller_activity", "活动", manor, gw, items, ctx);
    }

    public static UiView controllerActivityBoard(Manor manor, GuildWorld gw, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        if (values != null) {
            ctx.putAll(values);
        }
        UiView yaml = fromYaml("controller_activity_board", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("oak_sign"), "§a留言墙",
                List.of("§7最近留言: §f" + ctx.getOrDefault("comment_count", "0"),
                        "§7最新: §f" + ctx.getOrDefault("comment_preview_1", "无"),
                        "",
                        "§8这里展示当前庄园最近的访客留言。"), ""));
        items.put(20, UiItem.of(UiIcon.of("paper"), "§f1",
                List.of("§7" + ctx.getOrDefault("comment_preview_1", "无")), ""));
        items.put(21, UiItem.of(UiIcon.of("paper"), "§f2",
                List.of("§7" + ctx.getOrDefault("comment_preview_2", "无")), ""));
        items.put(22, UiItem.of(UiIcon.of("paper"), "§f3",
                List.of("§7" + ctx.getOrDefault("comment_preview_3", "无")), ""));
        items.put(23, UiItem.of(UiIcon.of("paper"), "§f4",
                List.of("§7" + ctx.getOrDefault("comment_preview_4", "无")), ""));
        items.put(24, UiItem.of(UiIcon.of("paper"), "§f5",
                List.of("§7" + ctx.getOrDefault("comment_preview_5", "无")), ""));
        items.put(31, UiItem.of(UiIcon.of("book"), "§e去留言",
                List.of("§7留言仍需文本输入。", "§8请用 /gs comment <留言>。"), ""));
        items.put(33, UiItem.of(UiIcon.of("book"), "§e返回活动页",
                List.of("§7回到活动总览。"), "menu.controller.activity"));
        addBack(items);
        return page("controller_activity_board", "留言墙", manor, gw, items, ctx);
    }

    public static UiView controllerActivityRate(Manor manor, GuildWorld gw, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        if (values != null) {
            ctx.putAll(values);
        }
        UiView yaml = fromYaml("controller_activity_rate", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("nether_star"), "§e评分",
                List.of("§7平均分: §f" + ctx.getOrDefault("rating_avg", "0.0"),
                        "§7评分数: §f" + ctx.getOrDefault("rating_count", "0"),
                        "§7你的评分: §f" + ctx.getOrDefault("my_rating", "0"),
                        "",
                        "§8点击数字即可保存评分并返回活动页。"), ""));
        int slot = 20;
        for (int score = 1; score <= 10; score++) {
            items.put(slot++, UiItem.of(UiIcon.of("paper"),
                    "§f" + score + " 分",
                    List.of("§7点击提交 " + score + " 分评分"), "menu.controller.activity.rate." + score));
            if (slot == 25) {
                slot = 29;
            }
        }
        items.put(31, UiItem.of(UiIcon.of("book"), "§e返回活动页",
                List.of("§7回到活动总览。"), "menu.controller.activity"));
        addBack(items);
        return page("controller_activity_rate", "评分", manor, gw, items, ctx);
    }

    public static UiView controllerSecurity(Manor manor, GuildWorld gw) {
        return controllerSecurity(manor, gw, Map.of());
    }

    public static UiView controllerSecurity(Manor manor, GuildWorld gw, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        if (values != null) {
            ctx.putAll(values);
        }
        UiView yaml = fromYaml("controller_security", ctx);
        if (yaml != null) return yaml;
        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("shield"), "§c金库与安保",
                List.of("§7访客状态: §f" + ctx.getOrDefault("visitor_status", "?"),
                        "§7进入规则: §f" + ctx.getOrDefault("entry_policy", "?"),
                        "§7入场费: §e" + ctx.getOrDefault("price_text", "免费")), ""));
        items.put(20, UiItem.of(UiIcon.of("oak_door"), "§a临时开放访客",
                List.of("§8临时允许访客进入", "§7当前: §f" + ctx.getOrDefault("visitor_status", "?"), "", "§e点击开放 60 分钟"),
                "command.open.60"));
        items.put(21, UiItem.of(UiIcon.of("iron_door"), "§c关闭访客",
                List.of("§8恢复私有访问", "§7当前: §f" + ctx.getOrDefault("visitor_status", "?"), "§7已信任成员仍按权限进入。", "", "§e点击关闭访客"),
                "command.close"));
        items.put(22, UiItem.of(UiIcon.of("redstone_torch"), "§eFlag 设置",
                List.of("§8细分行为规则",
                        "§7用于控制容器、交互、PVP、进入提示等策略。",
                        "",
                        "§7后续可以拆成独立开关页:",
                        "§8▪ §7访客能否开容器",
                        "§8▪ §7是否允许交互机器/动物",
                        "§8▪ §7是否显示进出提示",
                        "",
                        "§8当前先作为规则说明卡。"),
                ""));
        items.put(23, UiItem.of(UiIcon.of("redstone_block"), "§c黑名单",
                List.of("§8拒绝特定玩家",
                        "§7适合处理骚扰、恶意参观或活动黑名单。",
                        "§7添加对象需要选择玩家，不能只靠一个按钮完成。",
                        "",
                        "§8独立玩家选择页完成前，这里不刷聊天。"),
                ""));
        items.put(24, UiItem.of(UiIcon.of("paper"), "§e审计日志",
                List.of("§8追踪关键变更",
                        "§7用于查看升级、解锁、权限调整等记录。",
                        "§7日志适合做成独立翻页列表。",
                        "",
                        "§8当前先展示入口说明，不刷聊天日志。"),
                ""));
        addBack(items);
        return page("controller_security", "金库与安保", manor, gw, items);
    }

    private static UiView page(String id, String title, Manor manor, GuildWorld gw, Map<Integer, UiItem> items) {
        return new UiView(id, "§8[§6庄园控制器§8] §7" + title, 6, items,
                Map.of("manor", manor, "guildWorld", gw));
    }

    private static UiView page(String id, String title, Manor manor, GuildWorld gw, Map<Integer, UiItem> items,
                               Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>();
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        if (values != null) {
            ctx.putAll(values);
        }
        return new UiView(id, "§8[§6庄园控制器§8] §7" + title, 6, items, ctx);
    }

    private static Map<Integer, UiItem> framed() {
        Map<Integer, UiItem> items = new HashMap<>();
        UiItem pane = UiItem.separator(UiIcon.of("gray_stained_glass_pane"));
        for (int i = 0; i < 54; i++) {
            if (i < 9 || i >= 45 || i % 9 == 0 || i % 9 == 8) {
                items.put(i, pane);
            }
        }
        return items;
    }

    private static void addBack(Map<Integer, UiItem> items) {
        items.put(45, UiItem.of(UiIcon.of("arrow"), "§e返回控制器",
                List.of("§7回到庄园控制器首页。", "§8不会执行任何庄园操作。"),
                "menu.controller"));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭",
                List.of("§7关闭当前控制器界面。", "§8已确认的命令不会被撤销。"),
                "close"));
    }

    public static UiView campManager(GuildWorld gw, LevelRules levels, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>(values);
        ctx.put("guildWorld", gw);
        ctx.put("levels", levels);
        UiView yaml = fromYaml("camp_manager", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("lectern"), "§6§l营地服务台", List.of("§7公会: §f" + gw.guild().value()), ""));
        items.put(20, UiItem.of(UiIcon.of("compass"), "§b传送点设置", List.of("§7管理营地成员和访客的传送坐标。"), "menu.camp.spawn"));
        items.put(22, UiItem.of(UiIcon.of("map"), "§a主城与地块", List.of("§7管理主城解锁与子地块规划。"), "menu.camp.city"));
        items.put(24, UiItem.of(UiIcon.of("book"), "§e展示与日志", List.of("§7悬浮字、迎送语及审计日志管理。"), "menu.camp.social"));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭", "close"));
        return new UiView("camp_manager", "§8[§6营地服务台§8] §7总览", 6, items, ctx);
    }

    public static UiView campSpawn(GuildWorld gw, LevelRules levels, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>(values);
        ctx.put("guildWorld", gw);
        ctx.put("levels", levels);
        UiView yaml = fromYaml("camp_spawn", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(21, UiItem.of(UiIcon.of("ender_pearl"), "§b成员传送点", List.of("§e点击将当前脚下设为成员点"), "command.setspawn.member"));
        items.put(23, UiItem.of(UiIcon.of("magma_cream"), "§6访客传送点", List.of("§e点击将当前脚下设为访客点"), "command.setspawn.visitor"));
        items.put(45, UiItem.of(UiIcon.of("arrow"), "§e返回上一页", "menu.camp"));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭", "close"));
        return new UiView("camp_spawn", "§8[§6营地服务台§8] §7传送点设置", 6, items, ctx);
    }

    public static UiView campCity(GuildWorld gw, LevelRules levels, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>(values);
        ctx.put("guildWorld", gw);
        ctx.put("levels", levels);
        UiView yaml = fromYaml("camp_city", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(21, UiItem.of(UiIcon.of("golden_shovel"), "§e解锁主城区块", List.of("§e点击解锁脚下区块"), "command.cityunlock"));
        items.put(23, UiItem.of(UiIcon.of("oak_fence"), "§6主城子地块列表", List.of("§e点击列出当前地块列表"), "command.cityplot.list"));
        items.put(45, UiItem.of(UiIcon.of("arrow"), "§e返回上一页", "menu.camp"));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭", "close"));
        return new UiView("camp_city", "§8[§6营地服务台§8] §7主城与地块", 6, items, ctx);
    }

    public static UiView campSocial(GuildWorld gw, LevelRules levels, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>(values);
        ctx.put("guildWorld", gw);
        ctx.put("levels", levels);
        UiView yaml = fromYaml("camp_social", ctx);
        if (yaml != null) return yaml;

        Map<Integer, UiItem> items = framed();
        items.put(20, UiItem.of(UiIcon.of("name_tag"), "§b主城悬浮字列表", List.of("§e点击查看已有悬浮字列表"), "command.holo.list"));
        items.put(24, UiItem.of(UiIcon.of("book"), "§6打印审计日志", List.of("§e点击在聊天框打印最新日志"), "command.log"));
        items.put(45, UiItem.of(UiIcon.of("arrow"), "§e返回上一页", "menu.camp"));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭", "close"));
        return new UiView("camp_social", "§8[§6营地服务台§8] §7展示与日志", 6, items, ctx);
    }

    /** 庄园门牌 GUI（/gs card 打开）。YAML 优先，硬编码兜底。 */
    public static UiView manorCard(Manor manor, GuildWorld gw, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>(values);
        ctx.put("manor", manor);
        ctx.put("guildWorld", gw);
        UiView yaml = fromYaml("manor_card", ctx);
        if (yaml != null) return yaml;

        // 硬编码兜底
        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("lectern"), "§6§l庄园门牌",
                List.of("§7公会: §f" + manor.guild().value(),
                        "§7庄园: §f#" + manor.slot() + " §8| §7Lv§f" + manor.level() + "/" + ctx.getOrDefault("max_level", "?"),
                        "§7庄主: §f" + ctx.getOrDefault("owner_name", "?"),
                        "§7状态: §f" + ctx.getOrDefault("done_status", "?")),
                ""));
        items.put(11, UiItem.of(UiIcon.of("map"), "§e基本资料",
                List.of("§7地块: §f" + ctx.getOrDefault("side", "?") + "x" + ctx.getOrDefault("side", "?") + " chunk",
                        "§7已解锁: §f" + manor.unlockedChunks().size() + " chunk"),
                ""));
        items.put(13, UiItem.of(UiIcon.of("armor_stand"), "§b实体统计",
                List.of("§f" + ctx.getOrDefault("entity_line", "§8(世界未加载)")),
                ""));
        items.put(15, UiItem.of(UiIcon.of("player_head"), "§a成员",
                List.of("§7共建人: §f" + manor.coBuilders().size(),
                        "§7黑名单: §c" + manor.denied().size()),
                ""));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭", "close"));
        return new UiView("manor_card", "§8[§6庄园门牌§8]", 6, items, ctx);
    }

    /** 留言板 GUI（/gs board 打开）。YAML 优先，硬编码兜底。 */
    public static UiView boardView(GuildWorld gw, Map<String, Object> values) {
        Map<String, Object> ctx = new HashMap<>(values);
        ctx.put("guildWorld", gw);
        UiView yaml = fromYaml("board", ctx);
        if (yaml != null) return yaml;

        // 硬编码兜底
        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("oak_sign"), "§a留言墙",
                List.of("§7最近留言: §f" + ctx.getOrDefault("comment_count", "0")),
                ""));
        // 动态填充留言到 slot 10-16, 19-21
        int[] slots = {10, 11, 12, 13, 14, 15, 16, 19, 20, 21};
        for (int i = 0; i < slots.length; i++) {
            String preview = (String) ctx.getOrDefault("comment_preview_" + (i + 1), null);
            if (preview != null) {
                items.put(slots[i], UiItem.of(UiIcon.of("paper"), "§f留言 " + (i + 1),
                        List.of("§7" + preview), ""));
            }
        }
        items.put(31, UiItem.of(UiIcon.of("book"), "§e去留言",
                List.of("§7留言需要文本输入。", "§8请用 §f/gs comment <留言>§8。"), ""));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭", "close"));
        return new UiView("board", "§8[§6留言板§8]", 6, items, ctx);
    }
}
