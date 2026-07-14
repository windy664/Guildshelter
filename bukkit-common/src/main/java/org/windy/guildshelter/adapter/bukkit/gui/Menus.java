package org.windy.guildshelter.adapter.bukkit.gui;

import org.windy.guildshelter.GuildShelterPlugin;
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
                    List.of("§8" + f.description(), "§7类型: " + f.type(), "", "§e左键: true  §c右键: false  §7中键: 重置"),
                    "flag.toggle." + f.id()));
        }

        // 翻页按钮
        if (page > 0) items.put(27, UiItem.of(UiIcon.of("arrow"), "§e上一页", "menu.flags.page." + (page - 1)));
        if (page < maxPage) items.put(35, UiItem.of(UiIcon.of("arrow"), "§e下一页", "menu.flags.page." + (page + 1)));

        // 返回按钮
        items.put(31, UiItem.of(UiIcon.of("barrier"), "§c返回", "menu.info"));

        return new UiView("flag_editor", "§8[§6Flag 编辑器§8] §7页 " + (page + 1) + "/" + (maxPage + 1), 4, items,
                Map.of("manor", manor, "page", page));
    }

    /** 成员管理面板。 */
    public static UiView memberManager(Manor manor) {
        Map<Integer, UiItem> items = new HashMap<>();
        int slot = 0;

        // Owner
        items.put(slot++, UiItem.of(UiIcon.of("golden_helmet"), "§6庄主", List.of("§f" + manor.owner()), ""));

        // Trusted
        items.put(slot++, UiItem.of(UiIcon.of("diamond_helmet"), "§b共建人 (" + manor.coBuilders().size() + ")",
                manor.coBuilders().stream().map(r -> "§7- §f" + r).toList(), "menu.members.trusted"));

        // Members
        items.put(slot++, UiItem.of(UiIcon.of("iron_helmet"), "§a成员 (" + manor.members().size() + ")",
                manor.members().stream().map(r -> "§7- §f" + r).toList(), "menu.members.members"));

        // Denied
        items.put(slot++, UiItem.of(UiIcon.of("redstone_block"), "§c黑名单 (" + manor.denied().size() + ")",
                manor.denied().stream().map(r -> "§7- §f" + r).toList(), "menu.members.denied"));

        // 返回
        items.put(8, UiItem.of(UiIcon.of("barrier"), "§c返回", "menu.info"));

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
        items.put(40, UiItem.of(UiIcon.of("wheat"),
                "§d活动",
                List.of("§7花、留言、评分等庄园互动。"),
                "menu.controller.activity"));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭", "close"));

        return new UiView("manor_controller", "§8[§6庄园控制器§8]", 6, items,
                ctx);
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
                List.of("§8概览当前庄园", "§7展示等级、额度、成员与公会信息。", "", "§e点击查看详情"),
                "command.info"));
        items.put(21, UiItem.of(UiIcon.of("chest"), "§a我的庄园",
                List.of("§8多庄园管理", "§7列出你拥有的全部地皮与默认 Home。", "", "§e点击打开列表"),
                "command.manors"));
        items.put(22, UiItem.of(UiIcon.of("ender_pearl"), "§a回家",
                List.of("§8快速返回建设点", "§7优先使用 Home，未设置时回到庄园入口。", "", "§e点击传送"),
                "command.home"));
        items.put(23, UiItem.of(UiIcon.of("compass"), "§a设置 Home",
                List.of("§8保存当前落点", "§7把脚下位置设为以后回家的传送点。", "", "§e点击设置"),
                "command.sethome"));
        items.put(24, UiItem.of(UiIcon.of("player_head"), "§a成员管理",
                List.of("§8协作与访问关系", "§7查看共建人、普通成员和黑名单。", "", "§e点击查看"),
                "menu.members"));
        addBack(items);
        return page("controller_info", "资料", manor, gw, items);
    }

    public static UiView controllerUpgrade(Manor manor, GuildWorld gw, LevelRules levels) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw, "levels", levels);
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
                        maxed ? "§6已达到满级" : "§7下一级: §fLv" + nextLevel + " §8| §7额度 §f" + next),
                ""));
        items.put(20, UiItem.of(UiIcon.of("emerald"),
                maxed ? "§6已满级" : "§a确认升级",
                List.of("§8提交升级请求",
                        "§7会先检查 Vault 余额与背包材料。",
                        "§7全部满足后才会扣除并提升等级。",
                        "",
                        maxed ? "§6当前已达到最高等级" : "§e点击升级到 Lv" + nextLevel),
                "command.upgrade"));
        items.put(22, UiItem.of(UiIcon.of("gold_ingot"),
                "§eVault + 物品消耗",
                List.of("§7配置: §flevels.yml §8→ §fmanor.upgrade-costs",
                        "§7命令层扣钱/扣物，GUI 只触发命令。"),
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

    public static UiView controllerCare(Manor manor, GuildWorld gw) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw);
        UiView yaml = fromYaml("controller_care", ctx);
        if (yaml != null) return yaml;
        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("anvil"), "§b美化与护理",
                List.of("§7清理、模板、搬家与常用建设动作。"), ""));
        items.put(20, UiItem.of(UiIcon.of("iron_shovel"), "§a清理地表",
                List.of("§8清理当前庄园地表", "§7用于重新规划建筑或清除自然杂物。", "§c会改动地表，请确认站在目标庄园。", "", "§e点击开始清理流程"),
                "command.clear"));
        items.put(21, UiItem.of(UiIcon.of("writable_book"), "§a建筑模板",
                List.of("§8保存或套用建筑方案", "§7便于迁移、复原或复用成熟布局。", "", "§e点击打开模板命令"),
                "command.template"));
        items.put(22, UiItem.of(UiIcon.of("minecart"), "§a搬家",
                List.of("§8迁移庄园位置", "§7会遵守费用、冷却与确认流程。", "", "§e点击进入搬家流程"),
                "command.move"));
        items.put(23, UiItem.of(UiIcon.of("target"), "§a庄园中心",
                List.of("§8定位完整地皮中心", "§7用于勘察、对齐建筑或快速回到中心。", "", "§e点击传送到中心"),
                "command.middle"));
        items.put(24, UiItem.of(UiIcon.of("oak_sign"), "§a描述/展示",
                List.of("§8编辑庄园展示文本", "§7给访客一个简短介绍，也方便排行展示。", "", "§e点击查看用法"),
                "command.desc"));
        addBack(items);
        return page("controller_care", "美化与护理", manor, gw, items);
    }

    public static UiView controllerSecurity(Manor manor, GuildWorld gw) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw);
        UiView yaml = fromYaml("controller_security", ctx);
        if (yaml != null) return yaml;
        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("shield"), "§c金库与安保",
                List.of("§7先接现有访客、黑名单、日志和 Flag。"), ""));
        items.put(20, UiItem.of(UiIcon.of("oak_door"), "§a临时开放访客",
                List.of("§8临时允许访客进入", "§7默认开放 60 分钟，适合展示、评分或活动。", "", "§e点击开放 60 分钟"),
                "command.open.60"));
        items.put(21, UiItem.of(UiIcon.of("iron_door"), "§c关闭访客",
                List.of("§8恢复私有访问", "§7立即关闭临时开放状态。", "§7已信任成员仍按权限进入。", "", "§e点击关闭访客"),
                "command.close"));
        items.put(22, UiItem.of(UiIcon.of("redstone_torch"), "§eFlag 设置",
                List.of("§8细分行为规则", "§7控制容器、交互、PVP、进入提示等策略。", "", "§e点击查看 Flag 用法"),
                "command.flag"));
        items.put(23, UiItem.of(UiIcon.of("redstone_block"), "§c黑名单",
                List.of("§8拒绝特定玩家", "§7添加对象仍需在聊天命令中指定。", "", "§e点击查看黑名单命令"),
                "command.deny"));
        items.put(24, UiItem.of(UiIcon.of("paper"), "§e审计日志",
                List.of("§8追踪关键变更", "§7查看升级、解锁、权限调整等审计记录。", "", "§e点击查看日志"),
                "command.log"));
        addBack(items);
        return page("controller_security", "金库与安保", manor, gw, items);
    }

    public static UiView controllerActivity(Manor manor, GuildWorld gw) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw);
        UiView yaml = fromYaml("controller_activity", ctx);
        if (yaml != null) return yaml;
        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("wheat"), "§d活动",
                List.of("§7庄园互动、留言与排行入口。"), ""));
        items.put(20, UiItem.of(UiIcon.of("flower_pot"), "§d送花",
                List.of("§8给喜欢的庄园一点反馈", "§7用于访客互动和人气展示。", "", "§e点击送花"),
                "command.flower"));
        items.put(21, UiItem.of(UiIcon.of("oak_sign"), "§a留言墙",
                List.of("§8查看脚下庄园留言", "§7适合访客评价、合作提醒和活动记录。", "", "§e点击打开留言墙"),
                "command.board"));
        items.put(22, UiItem.of(UiIcon.of("name_tag"), "§a留言",
                List.of("§8留下文本留言", "§7具体内容在聊天命令中输入。", "", "§e点击查看留言用法"),
                "command.comment"));
        items.put(23, UiItem.of(UiIcon.of("nether_star"), "§e评分",
                List.of("§8为庄园打分", "§7面向开放参观和排行榜统计。", "", "§e点击查看评分用法"),
                "command.rate"));
        items.put(24, UiItem.of(UiIcon.of("chest"), "§a收件箱",
                List.of("§8查看收到的互动", "§7集中处理留言、反馈和系统通知。", "", "§e点击打开收件箱"),
                "command.inbox"));
        addBack(items);
        return page("controller_activity", "活动", manor, gw, items);
    }

    private static UiView page(String id, String title, Manor manor, GuildWorld gw, Map<Integer, UiItem> items) {
        return new UiView(id, "§8[§6庄园控制器§8] §7" + title, 6, items,
                Map.of("manor", manor, "guildWorld", gw));
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
        items.put(45, UiItem.of(UiIcon.of("arrow"), "§e返回控制器", "menu.controller"));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭", "close"));
    }
}
