package org.windy.guildshelter.adapter.bukkit.command;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.windy.guildshelter.GuildShelterPlugin;
import org.windy.guildshelter.adapter.bukkit.BlockDisplayNames;
import org.windy.guildshelter.adapter.bukkit.ManorEntityCensus;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.Permissions;
import org.windy.guildshelter.adapter.bukkit.gui.Menus;
import org.windy.guildshelter.adapter.bukkit.listener.TerritoryGreetingListener;
import org.windy.guildshelter.domain.flag.Flag;
import org.windy.guildshelter.domain.flag.FlagType;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.port.CampSpawnStore;
import org.windy.guildshelter.domain.port.ManorRepository;
import org.windy.guildshelter.domain.port.ui.UiActionRouter;
import org.windy.guildshelter.domain.port.ui.UiBackend;
import org.windy.guildshelter.domain.port.ui.UiView;
import org.windy.guildshelter.domain.port.ui.UiViewer;
import org.windy.guildshelter.domain.rule.OptimizationLimit;
import org.windy.guildshelter.domain.rule.quota.MachineKey;
import org.windy.guildshelter.domain.rule.quota.ManorQuotaKey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * UI 动作注册器：从 GsCommand.registerUiActions 提取。
 *
 * <p>负责将庄园控制器、营地管理、Flag 编辑等 UI 按钮动作注册到 {@link UiActionRouter}。
 * 由 Plugin 启动流程调用，与命令框架解耦。
 */
public final class UiActionRegistrar {

    private final CommandContext ctx;

    public UiActionRegistrar(CommandContext ctx) {
        this.ctx = ctx;
    }

    public void register(UiActionRouter router) {
        if (router == null) return;

        router.onAction("close", (viewer, view) -> {
            UiBackend backend = GuildShelterPlugin.uiBackend();
            if (backend != null) backend.close(viewer);
        });
        router.onAction("menu.controller", (viewer, view) -> openController(viewer));
        router.onAction("menu.camp", (viewer, view) -> openCampPage(viewer, view, "main"));
        router.onAction("menu.camp.spawn", (viewer, view) -> openCampPage(viewer, view, "spawn"));
        router.onAction("menu.camp.city", (viewer, view) -> openCampPage(viewer, view, "city"));
        router.onAction("menu.camp.social", (viewer, view) -> openCampPage(viewer, view, "social"));
        router.onAction("menu.controller.info", (viewer, view) -> openControllerPage(viewer, view, "info"));
        router.onAction("menu.controller.upgrade", (viewer, view) -> openControllerPage(viewer, view, "upgrade"));
        router.onAction("menu.controller.quick", (viewer, view) -> openControllerPage(viewer, view, "quick"));
        router.onAction("menu.controller.care", (viewer, view) -> openControllerPage(viewer, view, "care"));
        router.onAction("menu.controller.maintenance", (viewer, view) -> openControllerPage(viewer, view, "maintenance"));
        router.onAction("menu.controller.limits", (viewer, view) -> openControllerPage(viewer, view, "limits"));
        router.onAction("menu.controller.security", (viewer, view) -> openControllerPage(viewer, view, "security"));
        router.onAction("menu.controller.flags", (viewer, view) -> openControllerPage(viewer, view, "flags"));
        router.onAction("menu.controller.members", (viewer, view) -> openControllerPage(viewer, view, "members"));
        router.onAction("menu.controller.members.ops", (viewer, view) -> openControllerPage(viewer, view, "member_ops"));
        router.onAction("menu.controller.activity", (viewer, view) -> openControllerPage(viewer, view, "activity"));
        router.onAction("menu.controller.activity.board", (viewer, view) -> openControllerPage(viewer, view, "activity_board"));
        router.onAction("menu.controller.activity.rate", (viewer, view) -> openControllerPage(viewer, view, "activity_rate"));
        router.onAction("menu.controller.activity.inbox", (viewer, view) -> { });

        router.onAction("menu.controller.flags.full", (viewer, view) -> {
            Player player = Bukkit.getPlayer(viewer.id());
            if (player == null) return;
            Manor manor = contextManor(view, player);
            if (manor == null) return;
            openUi(player, Menus.flagEditor(manor, 0));
        });

        for (int page = 0; page < 10; page++) {
            final int targetPage = page;
            router.onAction("menu.flags.page." + targetPage, (viewer, view) -> {
                Player player = Bukkit.getPlayer(viewer.id());
                if (player == null) return;
                Manor manor = contextManor(view, player);
                if (manor == null) return;
                openUi(player, Menus.flagEditor(manor, targetPage));
            });
        }

        router.onAction("menu.members", (viewer, view) -> {
            Player player = Bukkit.getPlayer(viewer.id());
            if (player == null) return;
            Manor manor = contextManor(view, player);
            if (manor == null) return;
            openUi(player, Menus.memberManager(manor));
        });

        router.onAction("menu.info", (viewer, view) -> openControllerPage(viewer, view, "info"));
        router.onAction("upgrade.pending", (viewer, view) -> runPlayerCommand(viewer, "upgrade"));

        for (int score = 1; score <= 10; score++) {
            final int targetScore = score;
            router.onAction("menu.controller.activity.rate." + targetScore, (viewer, view) -> {
                Player player = Bukkit.getPlayer(viewer.id());
                if (player == null) return;
                Manor manor = contextManor(view, player);
                if (manor == null) return;
                GuildWorld gw = contextGuildWorld(view, manor);
                if (gw == null) return;
                rateManor(player, manor, targetScore);
                openUi(player, controllerActivityView(player, manor, gw));
            });
        }

        for (Flag flag : Flag.values()) {
            if (flag.type() == FlagType.BOOLEAN) {
                router.onAction("flag.toggle." + flag.id(), (viewer, view) -> toggleBooleanFlag(viewer, view, flag));
            }
        }

        Set<String> legacyInfoOnlyActions = Set.of(
                "command.info", "command.manors", "command.template", "command.desc",
                "command.flag", "command.deny", "command.log", "command.board",
                "command.comment", "command.rate", "command.inbox");
        legacyInfoOnlyActions.forEach(action -> router.onAction(action, (viewer, view) -> { }));

        Map<String, String> commands = Map.ofEntries(
                Map.entry("command.upgrade", "upgrade"),
                Map.entry("command.home", "home"),
                Map.entry("command.sethome", "sethome"),
                Map.entry("command.spawn", "spawn"),
                Map.entry("command.setspawn.member", "setspawn member"),
                Map.entry("command.setspawn.visitor", "setspawn visitor"),
                Map.entry("command.cityunlock", "cityunlock"),
                Map.entry("command.cityplot.list", "cityplot list"),
                Map.entry("command.holo.list", "holo list"),
                Map.entry("command.log", "log"),
                Map.entry("command.unlock", "unlock"),
                Map.entry("command.clear", "clear"),
                Map.entry("command.move", "move"),
                Map.entry("command.middle", "middle"),
                Map.entry("command.open.60", "open 60"),
                Map.entry("command.close", "close"),
                Map.entry("command.flower", "flower"));
        for (Map.Entry<String, String> entry : commands.entrySet()) {
            router.onAction(entry.getKey(), (viewer, view) -> runPlayerCommand(viewer, entry.getValue()));
        }
    }

    // ── Controller 页面导航 ──

    private void openController(UiViewer viewer) {
        Player player = Bukkit.getPlayer(viewer.id());
        if (player != null) openController(player);
    }

    public void openController(Player player) {
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { player.sendMessage(Messages.get("error.no_manor")); return; }
        GuildWorld gw = ctx.guilds.find(manor.guild()).orElse(null);
        if (gw == null) { player.sendMessage(Messages.get("error.guild_not_exist", manor.guild().value())); return; }
        openUi(player, Menus.manorController(manor, gw, ctx.levels));
    }

    private void openControllerPage(UiViewer viewer, UiView source, String page) {
        Player player = Bukkit.getPlayer(viewer.id());
        if (player == null) return;
        Manor manor = contextManor(source, player);
        if (manor == null) return;
        GuildWorld gw = contextGuildWorld(source, manor);
        if (gw == null) { player.sendMessage(Messages.get("error.guild_not_exist", manor.guild().value())); return; }
        UiView next = switch (page) {
            case "info" -> Menus.controllerInfo(manor, gw, ctx.levels);
            case "upgrade" -> controllerUpgradeView(manor, gw);
            case "quick" -> Menus.controllerQuick(manor, gw);
            case "limits" -> controllerLimitsView(player, manor, gw);
            case "care" -> Menus.controllerCare(manor, gw);
            case "maintenance" -> Menus.controllerMaintenance(manor, gw);
            case "security" -> controllerSecurityView(manor, gw);
            case "flags" -> controllerFlagsView(manor, gw);
            case "activity" -> controllerActivityView(player, manor, gw);
            case "activity_board" -> controllerActivityBoardView(manor, gw);
            case "activity_rate" -> controllerActivityRateView(player, manor, gw);
            case "members" -> controllerMembersView(manor, gw);
            case "member_ops" -> controllerMemberOpsView(manor, gw);
            case "member_list" -> Menus.memberManager(manor);
            default -> Menus.manorController(manor, gw, ctx.levels);
        };
        openUi(player, next);
    }

    // ── Camp 页面 ──

    private void openCampPage(UiViewer viewer, UiView source, String page) {
        Player player = Bukkit.getPlayer(viewer.id());
        if (player == null) return;
        GuildWorld gw = source.context().get("guildWorld") instanceof GuildWorld g ? g : currentCampWorld(player);
        if (gw == null) { player.sendMessage(Messages.get("error.not_in_guild_world")); return; }
        Map<String, Object> values = campValues(gw);
        UiView next = switch (page) {
            case "spawn" -> Menus.campSpawn(gw, ctx.levels, values);
            case "city" -> Menus.campCity(gw, ctx.levels, values);
            case "social" -> Menus.campSocial(gw, ctx.levels, values);
            default -> Menus.campManager(gw, ctx.levels, values);
        };
        openUi(player, next);
    }

    // ── UI View Builders ──

    private UiView controllerMembersView(Manor manor, GuildWorld gw) {
        Map<String, Object> values = new HashMap<>();
        values.put("owner_name", nameOf(manor.owner().uuid().toString()));
        values.put("owner_status", online(manor.owner()) ? "在线" : "离线");
        putRoleSummary(values, "trusted", manor.coBuilders());
        putRoleSummary(values, "member", manor.members());
        putRoleSummary(values, "deny", manor.denied());
        int totalCount = 1 + manor.coBuilders().size() + manor.members().size() + manor.denied().size();
        int totalOnline = (online(manor.owner()) ? 1 : 0) + onlineCount(manor.coBuilders()) + onlineCount(manor.members()) + onlineCount(manor.denied());
        values.put("total_count", totalCount);
        values.put("total_online", totalOnline);
        return Menus.controllerMembers(manor, gw, values);
    }

    private UiView controllerMemberOpsView(Manor manor, GuildWorld gw) {
        Map<String, Object> values = new HashMap<>();
        values.put("owner_name", nameOf(manor.owner().uuid().toString()));
        values.put("owner_status", online(manor.owner()) ? "在线" : "离线");
        putRoleSummary(values, "trusted", manor.coBuilders());
        putRoleSummary(values, "member", manor.members());
        putRoleSummary(values, "deny", manor.denied());
        int totalCount = 1 + manor.coBuilders().size() + manor.members().size() + manor.denied().size();
        int totalOnline = (online(manor.owner()) ? 1 : 0) + onlineCount(manor.coBuilders()) + onlineCount(manor.members()) + onlineCount(manor.denied());
        values.put("total_count", totalCount);
        values.put("total_online", totalOnline);
        return Menus.controllerMemberOps(manor, gw, values);
    }

    private UiView controllerActivityView(Player player, Manor manor, GuildWorld gw) {
        Map<String, Object> values = new HashMap<>();
        List<ManorRepository.CommentEntry> comments = ctx.manors.getComments(manor.guild(), manor.slot(), 5);
        values.put("flower_today", ctx.manors.getTodayFlowerCount(manor.guild(), manor.slot()));
        values.put("popularity", formatOne(ctx.manors.getPopularity(manor.guild(), manor.slot())));
        values.put("rating_avg", formatOne(ctx.manors.getAverageRating(manor.guild(), manor.slot())));
        values.put("rating_count", ctx.manors.getRatingCount(manor.guild(), manor.slot()));
        values.put("my_rating", player == null ? 0 : ctx.manors.getRating(manor.guild(), manor.slot(), PlayerRef.of(player.getUniqueId())));
        values.put("comment_count", comments.size());
        fillCommentPreviews(values, comments, "comment_preview");
        values.put("comment_hint", "使用 /gs comment <留言> 留下文字反馈");
        values.put("board_hint", "点击留言墙查看完整列表");
        values.put("inbox_hint", "收件箱会显示你拥有的庄园收到的留言");
        return Menus.controllerActivity(manor, gw, values);
    }

    private UiView controllerActivityBoardView(Manor manor, GuildWorld gw) {
        Map<String, Object> values = new HashMap<>();
        List<ManorRepository.CommentEntry> comments = ctx.manors.getComments(manor.guild(), manor.slot(), 5);
        values.put("comment_count", comments.size());
        fillCommentPreviews(values, comments, "comment_preview");
        values.put("board_hint", "留言按时间倒序显示，最多展示最近 5 条");
        return Menus.controllerActivityBoard(manor, gw, values);
    }

    private UiView controllerActivityRateView(Player player, Manor manor, GuildWorld gw) {
        Map<String, Object> values = new HashMap<>();
        values.put("rating_avg", formatOne(ctx.manors.getAverageRating(manor.guild(), manor.slot())));
        values.put("rating_count", ctx.manors.getRatingCount(manor.guild(), manor.slot()));
        values.put("my_rating", player == null ? 0 : ctx.manors.getRating(manor.guild(), manor.slot(), PlayerRef.of(player.getUniqueId())));
        values.put("rate_hint", "点击数字即可保存评分");
        return Menus.controllerActivityRate(manor, gw, values);
    }

    private UiView controllerLimitsView(Player player, Manor manor, GuildWorld gw) {
        Map<String, Object> values = new HashMap<>();
        World world = Bukkit.getWorld(gw.worldName());
        ManorEntityCensus.Census counts = world != null && ctx.census != null
                ? ctx.census.countAtCached(world, manor) : ManorEntityCensus.Census.EMPTY;
        putLimit(values, "drops", counts.droppedItems(), cap(manor, OptimizationLimit.DROPS));
        putLimit(values, "tiles", counts.tileEntities(), cap(manor, OptimizationLimit.TILES));
        putLimit(values, "animal", counts.animals(), cap(manor, OptimizationLimit.ANIMAL));
        putLimit(values, "hostile", counts.hostiles(), cap(manor, OptimizationLimit.HOSTILE));
        putLimit(values, "mob", counts.livingTotal(), cap(manor, OptimizationLimit.MOB));
        putLimit(values, "vehicle", counts.vehicles(), cap(manor, OptimizationLimit.VEHICLE));
        List<String> machines = ctx.census == null || ctx.census.quotas() == null ? List.of()
                : ctx.census.quotas().machineIds().stream().sorted().toList();
        values.put("machine_count", machines.size());
        values.put("machine_summary", machineSummary(machines, counts));
        for (int i = 0; i < 3; i++) {
            String prefix = "machine_" + (i + 1);
            if (i < machines.size()) {
                String id = machines.get(i);
                int c = ctx.census.quotas().effectiveCap(manor, new MachineKey(id));
                int used = counts.machineCount(id);
                values.put(prefix + "_name", BlockDisplayNames.display(id));
                putLimit(values, prefix, used, c);
            } else {
                values.put(prefix + "_name", "未配置");
                values.put(prefix + "_used", "-");
                values.put(prefix + "_cap", "-");
                values.put(prefix + "_remain", "-");
                values.put(prefix + "_status", "未配置");
            }
        }
        values.put("limits_world_status", world == null ? "营地世界未加载，当前计数显示为 0" : "统计最近 3 秒缓存内的已加载区块");
        return Menus.controllerLimits(manor, gw, ctx.levels, values);
    }

    private UiView controllerUpgradeView(Manor manor, GuildWorld gw) {
        Map<String, Object> values = new HashMap<>();
        boolean maxed = manor.level() >= ctx.levels.manorMaxLevel();
        int targetLevel = Math.min(ctx.levels.manorMaxLevel(), manor.level() + 1);
        int currentQuota = manor.quotaCap(gw.layout(), ctx.levels);
        int nextQuota = maxed ? currentQuota : ctx.levels.manorQuotaCap(gw.layout(), targetLevel);
        values.put("upgrade_status", maxed ? "已满级" : "可升级");
        values.put("quota_delta", maxed ? "0" : signed(nextQuota - currentQuota));

        // 从 levels.yml 读取升级费用
        if (!maxed) {
            var file = new java.io.File(ctx.plugin.getDataFolder(), "levels.yml");
            var cfg = org.bukkit.configuration.file.YamlConfiguration.loadConfiguration(file);
            var section = cfg.getConfigurationSection("manor.levels." + targetLevel + ".upgrade");
            if (section != null) {
                double money = Math.max(0, section.getDouble("money", 0));
                var items = section.getStringList("items");
                values.put("upgrade_money", money > 0 ? String.format(java.util.Locale.ROOT, "%.0f", money) : "免费");
                values.put("upgrade_items_count", items.size());
                values.put("upgrade_items_more", items.size() > 5 ? "+" + (items.size() - 5) + " 项" : "无");
                for (int i = 0; i < Math.min(5, items.size()); i++) {
                    values.put("upgrade_item_" + (i + 1), formatUpgradeItem(items.get(i)));
                }
            } else {
                values.put("upgrade_money", "免费");
                values.put("upgrade_items_count", 0);
                values.put("upgrade_items_more", "无");
            }
        } else {
            values.put("upgrade_money", "—");
            values.put("upgrade_items_count", 0);
            values.put("upgrade_items_more", "—");
        }

        Manor nextManor = manor.withLevel(targetLevel);
        putUpgradeDelta(values, "tiles", manor, nextManor, OptimizationLimit.TILES);
        putUpgradeDelta(values, "drops", manor, nextManor, OptimizationLimit.DROPS);
        putUpgradeDelta(values, "animal", manor, nextManor, OptimizationLimit.ANIMAL);
        putUpgradeDelta(values, "hostile", manor, nextManor, OptimizationLimit.HOSTILE);
        putUpgradeDelta(values, "mob", manor, nextManor, OptimizationLimit.MOB);
        putUpgradeDelta(values, "vehicle", manor, nextManor, OptimizationLimit.VEHICLE);
        return Menus.controllerUpgrade(manor, gw, ctx.levels, values);
    }

    private UiView controllerSecurityView(Manor manor, GuildWorld gw) {
        Map<String, Object> values = new HashMap<>();
        boolean open = plotOpen(manor);
        boolean denyEntry = Flag.DENY_ENTRY.resolveBool(manor.flags());
        values.put("visitor_status", visitorStatus(manor));
        values.put("visitor_action_hint", open ? "如需结束参观，点击关闭访客" : "点击可开放 60 分钟");
        values.put("entry_policy", open ? "临时开放中" : (denyEntry ? "谢客，非成员禁止进入" : "允许访客进入"));
        values.put("exit_policy", Flag.DENY_EXIT.resolveBool(manor.flags()) ? "限制非成员离开" : "允许自由离开");
        values.put("price_text", moneyText(Flag.PRICE.resolveDouble(manor.flags())));
        values.put("deny_count", manor.denied().size());
        values.put("trusted_count", manor.coBuilders().size());
        values.put("member_count", manor.members().size());
        values.put("pvp_status", onOff(Flag.PVP.resolveBool(manor.flags()), "允许", "禁止"));
        values.put("use_status", onOff(Flag.USE.resolveBool(manor.flags()), "允许", "禁止"));
        values.put("container_status", onOff(Flag.CONTAINER.resolveBool(manor.flags()), "允许", "禁止"));
        values.put("vehicle_status", onOff(Flag.VEHICLE_USE.resolveBool(manor.flags()), "允许", "禁止"));
        values.put("greeting_status", blankFlag(Flag.GREETING.resolveString(manor.flags())));
        values.put("farewell_status", blankFlag(Flag.FAREWELL.resolveString(manor.flags())));
        values.put("titles_status", onOff(Flag.TITLES.resolveBool(manor.flags()), "标题", "聊天"));
        values.put("notify_status", "进入 " + onOff(Flag.NOTIFY_ENTER.resolveBool(manor.flags()), "开", "关")
                + " / 离开 " + onOff(Flag.NOTIFY_LEAVE.resolveBool(manor.flags()), "开", "关"));
        values.put("flag_custom_count", manor.flags().size());
        values.put("audit_status", ctx.auditLog != null && ctx.auditLog.isEnabled() ? "已启用" : "未启用");
        return Menus.controllerSecurity(manor, gw, values);
    }

    private UiView controllerFlagsView(Manor manor, GuildWorld gw) {
        Map<String, Object> values = new HashMap<>();
        putFlag(values, "deny_entry", manor, Flag.DENY_ENTRY);
        putFlag(values, "deny_exit", manor, Flag.DENY_EXIT);
        putFlag(values, "pvp", manor, Flag.PVP);
        putFlag(values, "pve", manor, Flag.PVE);
        putFlag(values, "invincible", manor, Flag.INVINCIBLE);
        putFlag(values, "use", manor, Flag.USE);
        putFlag(values, "container", manor, Flag.CONTAINER);
        putFlag(values, "item_frame", manor, Flag.ITEM_FRAME);
        putFlag(values, "vehicle", manor, Flag.VEHICLE_USE);
        putFlag(values, "titles", manor, Flag.TITLES);
        putFlag(values, "notify_enter", manor, Flag.NOTIFY_ENTER);
        putFlag(values, "notify_leave", manor, Flag.NOTIFY_LEAVE);
        values.put("flag_custom_count", manor.flags().size());
        values.put("more_flags_text", "完整列表包含环境、农场和数值 Flag");
        return Menus.controllerFlags(manor, gw, values);
    }

    // ── 交互操作 ──

    private void toggleBooleanFlag(UiViewer viewer, UiView view, Flag flag) {
        Player player = Bukkit.getPlayer(viewer.id());
        if (player == null) return;
        Manor manor = contextManor(view, player);
        if (manor == null) return;
        if (flag.type() != FlagType.BOOLEAN) { player.sendMessage(Messages.get("error.invalid_value")); return; }
        if (!canSetFlag(player, manor, flag)) { player.sendMessage(Messages.get("error.flag_set_perm", Permissions.flagSet(flag.id()))); return; }
        Map<String, String> flags = new HashMap<>(manor.flags());
        boolean next = !flag.resolveBool(flags);
        flags.put(flag.id(), Boolean.toString(next));
        Manor updated = manor.withFlags(flags);
        ctx.manors.save(updated);
        player.sendMessage(Messages.get("success.flag_set", flag.id(), Boolean.toString(next), updated.slot()));
        GuildWorld gw = contextGuildWorld(view, updated);
        if (gw != null) {
            UiView refreshed = "flag_editor".equals(view.id()) ? Menus.flagEditor(updated, view.page()) : controllerFlagsView(updated, gw);
            openUi(player, refreshed);
        }
    }

    private void rateManor(Player player, Manor targetManor, int score) {
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        ctx.manors.rate(targetManor.guild(), targetManor.slot(), ref, score);
        player.sendMessage(Messages.get("success.rate_saved", score));
    }

    private void runPlayerCommand(UiViewer viewer, String command) {
        Player player = Bukkit.getPlayer(viewer.id());
        if (player == null) return;
        UiBackend backend = GuildShelterPlugin.uiBackend();
        if (backend != null) backend.close(viewer);
        player.performCommand("gs " + command);
    }

    // ── 辅助方法 ──

    private void openUi(Player player, UiView view) {
        UiBackend backend = GuildShelterPlugin.uiBackend();
        if (backend == null || view == null) { player.sendMessage(Messages.get("error.gui_not_ready")); return; }
        backend.open(new UiViewer(player.getUniqueId(), player.getName()), view);
    }

    private Manor contextManor(UiView view, Player player) {
        Object val = view.context().get("manor");
        if (val instanceof Manor m) return m;
        return ctx.currentOwnManor(player).orElse(null);
    }

    private GuildWorld contextGuildWorld(UiView view, Manor manor) {
        Object val = view.context().get("guildWorld");
        if (val instanceof GuildWorld gw) return gw;
        return ctx.guilds.find(manor.guild()).orElse(null);
    }

    private GuildWorld currentCampWorld(Player player) {
        GuildWorld current = ctx.registry.get(player.getWorld().getName());
        if (current != null) return current;
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        return manor == null ? null : ctx.guilds.find(manor.guild()).orElse(null);
    }

    private Map<String, Object> campValues(GuildWorld gw) {
        Map<String, Object> values = new HashMap<>();
        int cityQuota = gw.cityQuotaCap(ctx.levels);
        int cityUnlocked = gw.cityUnlockedChunks().size();
        values.put("city_quota", cityQuota);
        values.put("city_unlocked", cityUnlocked);
        values.put("city_remaining", Math.max(0, cityQuota - cityUnlocked));
        values.put("member_spawn_status", ctx.campSpawn != null && ctx.campSpawn.get(gw.guild(), CampSpawnStore.Type.MEMBER).isPresent() ? "已设置" : "未设置");
        values.put("visitor_spawn_status", ctx.campSpawn != null && ctx.campSpawn.get(gw.guild(), CampSpawnStore.Type.VISITOR).isPresent() ? "已设置" : "未设置");
        values.put("cityplot_status", ctx.cityPlotsEnabled && ctx.cityPlotCache != null ? "已启用" : "未启用");
        values.put("cityplot_count", ctx.cityPlotsEnabled && ctx.cityPlotCache != null ? ctx.cityPlotCache.list(gw.guild()).size() : 0);
        values.put("cityplot_limit", ctx.cityPlotsMaxPerGuild);
        values.put("holo_status", ctx.holoEnabled && ctx.holoStore != null && ctx.holoBackend != null && ctx.holoBackend.available() ? "已启用" : "未启用");
        values.put("holo_count", ctx.holoStore != null ? ctx.holoStore.list(gw.guild()).size() : 0);
        values.put("holo_limit", ctx.holoMaxPerGuild);
        values.put("audit_status", ctx.auditLog != null && ctx.auditLog.isEnabled() ? "已启用" : "未启用");
        values.put("greeting_status", ctx.cityFlagCache != null && ctx.cityFlagCache.flags(gw.guild()).containsKey(TerritoryGreetingListener.KEY_GREETING) ? "已设置" : "未设置");
        values.put("farewell_status", ctx.cityFlagCache != null && ctx.cityFlagCache.flags(gw.guild()).containsKey(TerritoryGreetingListener.KEY_FAREWELL) ? "已设置" : "未设置");
        return values;
    }

    private boolean canSetFlag(Player player, Manor manor, Flag flag) {
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        boolean isOwner = manor.owner().equals(ref);
        boolean isTrusted = manor.coBuilders().contains(ref);
        return isOwner || (isTrusted && CommandContext.isTrustedFlag(flag))
                || player.hasPermission(Permissions.flagSet(flag.id()))
                || Permissions.hasAdminPerm(player, Permissions.ADMIN_FLAG_OTHER);
    }

    private boolean plotOpen(Manor manor) {
        String key = manor.guild().value() + ":" + manor.slot();
        Long expireAt = ctx.openPlots.get(key);
        if (expireAt == null) return false;
        if (expireAt == 0) return true;
        if (System.currentTimeMillis() >= expireAt) { ctx.openPlots.remove(key); return false; }
        return true;
    }

    private String visitorStatus(Manor manor) {
        String key = manor.guild().value() + ":" + manor.slot();
        Long expireAt = ctx.openPlots.get(key);
        if (expireAt == null) return "未开放";
        if (expireAt == 0) return "永久开放";
        long remaining = expireAt - System.currentTimeMillis();
        if (remaining <= 0) { ctx.openPlots.remove(key); return "未开放"; }
        return "开放中，剩余 " + minutesText(remaining);
    }

    private int cap(Manor manor, ManorQuotaKey key) {
        if (ctx.census == null || ctx.census.quotas() == null) return -1;
        return ctx.census.quotas().effectiveCap(manor, key);
    }

    // ── 格式化工具 ──

    private void putUpgradeDelta(Map<String, Object> values, String key, Manor current, Manor next, ManorQuotaKey quotaKey) {
        int before = cap(current, quotaKey);
        int after = cap(next, quotaKey);
        values.put(key + "_next_cap", formatCap(after));
        values.put(key + "_delta", deltaText(before, after));
    }

    private static String deltaText(int before, int after) {
        if (before < 0 && after < 0) return "不限";
        if (before < 0) return "不限 → " + after;
        if (after < 0) return before + " → 不限";
        int d = after - before;
        return d > 0 ? "+" + d : String.valueOf(d);
    }

    private static void putFlag(Map<String, Object> values, String key, Manor manor, Flag flag) {
        boolean enabled = flag.resolveBool(manor.flags());
        values.put(key + "_status", enabled ? "开启" : "关闭");
        values.put(key + "_next", enabled ? "点击关闭" : "点击开启");
    }

    private static void putRoleSummary(Map<String, Object> values, String key, Set<PlayerRef> refs) {
        values.put(key + "_count", refs.size());
        values.put(key + "_online", onlineCount(refs));
        values.put(key + "_preview", previewRefs(refs, 3));
    }

    private static String previewRefs(Set<PlayerRef> refs, int limit) {
        if (refs.isEmpty()) return "无";
        List<String> names = new ArrayList<>();
        int extra = 0;
        for (PlayerRef ref : refs) {
            if (names.size() < limit) names.add(nameOf(ref.uuid().toString()));
            else extra++;
        }
        if (extra > 0) names.add("+" + extra);
        return String.join(" / ", names);
    }

    private static void putLimit(Map<String, Object> values, String key, int used, int cap) {
        values.put(key + "_used", used);
        values.put(key + "_cap", formatCap(cap));
        values.put(key + "_remain", cap < 0 ? "不限" : Math.max(0, cap - used));
        values.put(key + "_status", status(used, cap));
    }

    private static void fillCommentPreviews(Map<String, Object> values, List<ManorRepository.CommentEntry> comments, String prefix) {
        for (int i = 0; i < 5; i++) {
            String key = prefix + "_" + (i + 1);
            if (i < comments.size()) values.put(key, formatComment(comments.get(i)));
            else values.put(key, "无");
        }
    }

    /** 解析 levels.yml 的物品字符串（如 "minecraft:stone:64"）为 "石头 ×64" 格式。 */
    private static String formatUpgradeItem(String raw) {
        if (raw == null || raw.isBlank()) return "";
        String text = raw.trim();
        int split = text.lastIndexOf(':');
        if (split <= 0) return BlockDisplayNames.display(text);
        String id = text.substring(0, split).trim();
        String amount = text.substring(split + 1).trim();
        String name = BlockDisplayNames.display(id);
        return name + " §7×" + amount;
    }

    private static String formatComment(ManorRepository.CommentEntry c) {
        return "§e" + nameOf(c.author().uuid().toString()) + " §7: " + c.message();
    }

    private static String nameOf(String uuid) {
        try {
            String n = Bukkit.getOfflinePlayer(java.util.UUID.fromString(uuid)).getName();
            return n != null ? n : uuid.substring(0, 8);
        } catch (IllegalArgumentException e) { return uuid; }
    }

    private static boolean online(PlayerRef ref) {
        Player p = Bukkit.getPlayer(ref.uuid());
        return p != null && p.isOnline();
    }

    private static int onlineCount(Set<PlayerRef> refs) {
        int count = 0;
        for (PlayerRef ref : refs) { if (online(ref)) count++; }
        return count;
    }

    private static String moneyText(double money) {
        if (money <= 0) return "免费";
        if (money == Math.rint(money)) return String.valueOf((long) money);
        return String.format(java.util.Locale.ROOT, "%.2f", money);
    }

    private static String signed(int value) { return value > 0 ? "+" + value : String.valueOf(value); }

    private static String formatCap(int cap) { return cap < 0 ? "不限" : String.valueOf(cap); }

    private static String status(int used, int cap) {
        if (cap < 0) return "不限";
        if (used >= cap) return "已满";
        if (used >= cap * 0.8) return "即将满";
        return "正常";
    }

    private static String formatOne(double value) { return String.format(java.util.Locale.ROOT, "%.1f", value); }

    private static String onOff(boolean enabled, String onText, String offText) { return enabled ? onText : offText; }

    private static String blankFlag(String value) { return value == null || value.isBlank() ? "未设置" : "已设置"; }

    private static String minutesText(long millis) {
        long minutes = Math.max(1, (long) Math.ceil(millis / 60_000.0));
        if (minutes < 60) return minutes + " 分钟";
        long hours = minutes / 60;
        long rest = minutes % 60;
        return rest == 0 ? hours + " 小时" : hours + " 小时 " + rest + " 分钟";
    }

    private String machineSummary(List<String> machineIds, ManorEntityCensus.Census counts) {
        if (machineIds.isEmpty()) return "无机器限制";
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < Math.min(3, machineIds.size()); i++) {
            if (i > 0) sb.append(" / ");
            String id = machineIds.get(i);
            sb.append(BlockDisplayNames.display(id)).append(": ").append(counts.machineCount(id));
        }
        if (machineIds.size() > 3) sb.append(" +").append(machineIds.size() - 3);
        return sb.toString();
    }
}
