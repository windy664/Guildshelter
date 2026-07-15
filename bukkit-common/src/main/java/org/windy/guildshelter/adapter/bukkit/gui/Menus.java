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
                        "§7路径: §fmanor.upgrade-costs.levels",
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

    public static UiView controllerSecurity(Manor manor, GuildWorld gw) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw);
        UiView yaml = fromYaml("controller_security", ctx);
        if (yaml != null) return yaml;
        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("shield"), "§c金库与安保",
                List.of("§7控制访客开放、访问策略和安全记录。",
                        "§7涉及输入对象的功能先展示规则说明。",
                        "",
                        "§8避免点击后把用法刷到聊天框。"), ""));
        items.put(20, UiItem.of(UiIcon.of("oak_door"), "§a临时开放访客",
                List.of("§8临时允许访客进入", "§7默认开放 60 分钟，适合展示、评分或活动。", "", "§e点击开放 60 分钟"),
                "command.open.60"));
        items.put(21, UiItem.of(UiIcon.of("iron_door"), "§c关闭访客",
                List.of("§8恢复私有访问", "§7立即关闭临时开放状态。", "§7已信任成员仍按权限进入。", "", "§e点击关闭访客"),
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

    public static UiView controllerActivity(Manor manor, GuildWorld gw) {
        Map<String, Object> ctx = Map.of("manor", manor, "guildWorld", gw);
        UiView yaml = fromYaml("controller_activity", ctx);
        if (yaml != null) return yaml;
        Map<Integer, UiItem> items = framed();
        items.put(4, UiItem.of(UiIcon.of("wheat"), "§d活动",
                List.of("§7庄园互动、留言与排行入口。",
                        "§7需要文本输入或翻页的功能先做成说明卡。",
                        "",
                        "§8后续可扩展成独立活动面板。"), ""));
        items.put(20, UiItem.of(UiIcon.of("flower_pot"), "§d送花",
                List.of("§8给喜欢的庄园一点反馈", "§7用于访客互动和人气展示。", "", "§e点击送花"),
                "command.flower"));
        items.put(21, UiItem.of(UiIcon.of("oak_sign"), "§a留言墙",
                List.of("§8查看脚下庄园留言",
                        "§7适合访客评价、合作提醒和活动记录。",
                        "§7留言墙需要翻页列表承载，避免刷屏。",
                        "",
                        "§8当前先作为功能预览。"),
                ""));
        items.put(22, UiItem.of(UiIcon.of("name_tag"), "§a留言",
                List.of("§8留下文本留言",
                        "§7留言需要输入内容，不能用单次点击直接完成。",
                        "§7后续可接入文本输入/确认页。",
                        "",
                        "§8当前先作为说明卡，不刷聊天。"),
                ""));
        items.put(23, UiItem.of(UiIcon.of("nether_star"), "§e评分",
                List.of("§8为庄园打分",
                        "§7面向开放参观和排行榜统计。",
                        "§7评分需要选择分数，适合做成 1-5 星按钮页。",
                        "",
                        "§8当前先展示玩法说明。"),
                ""));
        items.put(24, UiItem.of(UiIcon.of("chest"), "§a收件箱",
                List.of("§8查看收到的互动",
                        "§7集中处理留言、反馈和系统通知。",
                        "§7收件箱需要独立分页和已读状态。",
                        "",
                        "§8当前先作为后续入口说明。"),
                ""));
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
        items.put(45, UiItem.of(UiIcon.of("arrow"), "§e返回控制器",
                List.of("§7回到庄园控制器首页。", "§8不会执行任何庄园操作。"),
                "menu.controller"));
        items.put(49, UiItem.of(UiIcon.of("barrier"), "§c关闭",
                List.of("§7关闭当前控制器界面。", "§8已确认的命令不会被撤销。"),
                "close"));
    }
}
