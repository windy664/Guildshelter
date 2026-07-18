package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.command.CommandSender;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.Permissions;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * /gs help [子命令]：显示帮助信息。
 */
@GsSubCommand(name = "help")
public class HelpCommand extends SubCommand {

    private static final Map<String, String> COMMAND_HELP = new LinkedHashMap<>();
    static {
        COMMAND_HELP.put("home", "传送到自己庄园（/gs home <slot> 指定，/gs home set 设默认，/gs manors 查看）");
        COMMAND_HELP.put("spawn", "传送到公会主城");
        COMMAND_HELP.put("middle", "传送到庄园正中心（无视 sethome）");
        COMMAND_HELP.put("sethome", "把当前位置设为 home 传送点");
        COMMAND_HELP.put("visit <公会>", "到访某公会的主城");
        COMMAND_HELP.put("info", "查看自己庄园+公会信息");
        COMMAND_HELP.put("card [玩家]", "查看庄园档案卡（实体/成员/评分）");
        COMMAND_HELP.put("near", "列出附近庄园（按距离排序）");
        COMMAND_HELP.put("list [mine]", "列出所有公会营地（mine=只看自己的）");
        COMMAND_HELP.put("board", "查看脚下庄园的留言墙");
        COMMAND_HELP.put("top [公会] [排序]", "排行榜（rating/level/members/entities/visits）");
        COMMAND_HELP.put("trust <玩家|*>", "加共建人（可建造/交互，*=批量）");
        COMMAND_HELP.put("untrust <玩家>", "移除共建人");
        COMMAND_HELP.put("citytrust <玩家>", "会长信任会内成员建造公会主城");
        COMMAND_HELP.put("cityuntrust <玩家>", "撤销会内成员的主城建造信任");
        COMMAND_HELP.put("setspawn <member|visitor>", "会长设置营地成员/访客传送点（站在目标位置执行）");
        COMMAND_HELP.put("holo <add|list|remove|move>", "主城悬浮字（需 DecentHolograms；会长/副会长，站主城内）");
        COMMAND_HELP.put("greeting <set <文本>|clear>", "会长设公会领地欢迎语（进入公会世界时弹；%guild%/%player%）");
        COMMAND_HELP.put("farewell <set <文本>|clear>", "会长设公会领地告别语（离开公会世界时弹）");
        COMMAND_HELP.put("log [页]", "会长查看本会领地操作审计日志（站公会世界内）");
        COMMAND_HELP.put("cityplot <define|assign|unassign|remove|list>", "主城子地块：会长把已解锁主城地划给成员开店");
        COMMAND_HELP.put("createcamp [地形]", Messages.get("help.createcamp"));
        COMMAND_HELP.put("member <add|remove> <玩家>", "管理受限成员（上级在线时才有权）");
        COMMAND_HELP.put("deny <玩家|*>", "拉黑（禁止进入，*=批量，需确认）");
        COMMAND_HELP.put("undeny <玩家>", "移出黑名单");
        COMMAND_HELP.put("kick <玩家>", "把非成员踢出你的庄园");
        COMMAND_HELP.put("flag [set|unset] [flag] [值]", "查看/设置庄园 flag");
        COMMAND_HELP.put("alias <名称>", "设置庄园别名（空参清除）");
        COMMAND_HELP.put("desc <描述>", "设置庄园描述（空参清除）");
        COMMAND_HELP.put("done", "切换完工标记");
        COMMAND_HELP.put("toggle titles", "个人开关进出标题消息");
        COMMAND_HELP.put("open [分钟]", "临时开放庄园给访客（0=永久，默认60分钟）");
        COMMAND_HELP.put("close", "关闭庄园访客模式");
        COMMAND_HELP.put("comment <留言>", "给当前所在庄园留言");
        COMMAND_HELP.put("inbox", "查看自己庄园收到的留言");
        COMMAND_HELP.put("rate <1-10> [公会 slot]", "给庄园打分");
        COMMAND_HELP.put("flower [公会 slot]", "给庄园送花（每天每块限一次）");
        COMMAND_HELP.put("gift <玩家>", "把手持物品送给同世界的玩家");
        COMMAND_HELP.put("bulletin <set|show|clear>", "公会公告板管理");
        COMMAND_HELP.put("unlock", "用额度解锁脚下区块（须与已有领地相邻）");
        COMMAND_HELP.put("cityunlock", "会长用公会额度解锁脚下主城区块");
        COMMAND_HELP.put("claim", "站在空闲地皮上认领额外一块庄园（多庄园模式，数量受权限封顶）");
        COMMAND_HELP.put("manors", "列出你的全部庄园（slot/等级/坐标）");
        COMMAND_HELP.put("clear", "清空自己庄园的地表建筑（需确认）");
        COMMAND_HELP.put("swap <玩家>", "与对方互换庄园 slot（需确认）");
        COMMAND_HELP.put("merge <slot>", "合并相邻庄园到自己的庄园（需确认）");
        COMMAND_HELP.put("unmerge [slot]", "取消合并（不填=全部）（需确认）");
        COMMAND_HELP.put("move <公会>", "搬家到另一个公会（保留建筑，需确认）");
        COMMAND_HELP.put("template <子命令>", "权限模板管理（create/delete/apply/setflag/list）");
        COMMAND_HELP.put("sub <子命令>", "子领地管理（create/delete/setflag/list）");
        COMMAND_HELP.put("confirm", "确认待执行的危险操作");
        COMMAND_HELP.put("admin create <公会> [地形]", "创建公会营地");
        COMMAND_HELP.put("admin tp <公会>", "传送到公会主城");
        COMMAND_HELP.put("admin claim <公会>", "给自己分配一块庄园");
        COMMAND_HELP.put("admin fill <公会> <数量>", "批量填充测试庄园");
        COMMAND_HELP.put("admin map <公会>", "输出网格图到控制台");
        COMMAND_HELP.put("admin upgrade-manor <公会> <玩家> [slot]", "给指定玩家庄园升一级");
        COMMAND_HELP.put("admin roadpermit <玩家> <公会> <时长>", "给玩家限时路权");
        COMMAND_HELP.put("admin quota <公会> <玩家> <set|add> <数量> [slot]", "设/加玩家庄园解锁额度");
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length >= 2) {
            String cmd = args[1].toLowerCase();
            String desc = COMMAND_HELP.get(cmd);
            if (desc == null) {
                for (var e : COMMAND_HELP.entrySet()) {
                    if (e.getKey().startsWith(cmd) || e.getKey().split(" ")[0].equals(cmd)) {
                        desc = e.getValue();
                        break;
                    }
                }
            }
            if (desc != null) sender.sendMessage(Messages.get("info.help_cmd", cmd, desc));
            else sender.sendMessage(Messages.get("info.help_unknown", cmd));
            return;
        }
        boolean isAdmin = sender.isOp() || Permissions.hasAdminPerm(sender, Permissions.ADMIN);
        sender.sendMessage(Messages.get("info.help_header"));
        sender.sendMessage(Messages.get("info.help_role_common"));
        sender.sendMessage(Messages.get("info.help_cat_teleport"));
        helpLine(sender, "home"); helpLine(sender, "spawn"); helpLine(sender, "middle");
        helpLine(sender, "sethome"); helpLine(sender, "visit");
        sender.sendMessage(Messages.get("info.help_cat_info"));
        helpLine(sender, "info"); helpLine(sender, "card"); helpLine(sender, "near");
        helpLine(sender, "list"); helpLine(sender, "manors"); helpLine(sender, "board"); helpLine(sender, "top");
        sender.sendMessage(Messages.get("info.help_cat_social"));
        helpLine(sender, "comment"); helpLine(sender, "inbox"); helpLine(sender, "rate");
        helpLine(sender, "flower"); helpLine(sender, "gift"); helpLine(sender, "bulletin");
        sender.sendMessage(Messages.get("info.help_role_owner"));
        sender.sendMessage(Messages.get("info.help_cat_people"));
        helpLine(sender, "trust"); helpLine(sender, "untrust"); helpLine(sender, "member");
        helpLine(sender, "deny"); helpLine(sender, "undeny"); helpLine(sender, "kick");
        sender.sendMessage(Messages.get("info.help_cat_settings"));
        helpLine(sender, "flag"); helpLine(sender, "flag_set"); helpLine(sender, "alias");
        helpLine(sender, "desc"); helpLine(sender, "done"); helpLine(sender, "toggle");
        helpLine(sender, "open"); helpLine(sender, "close");
        sender.sendMessage(Messages.get("info.help_cat_advanced"));
        helpLine(sender, "unlock"); helpLine(sender, "claim"); helpLine(sender, "clear"); helpLine(sender, "swap");
        helpLine(sender, "merge"); helpLine(sender, "unmerge"); helpLine(sender, "move");
        helpLine(sender, "template"); helpLine(sender, "sub"); helpLine(sender, "confirm");
        sender.sendMessage(Messages.get("info.help_role_leader"));
        helpLine(sender, "citytrust"); helpLine(sender, "cityuntrust");
        helpLine(sender, "cityunlock"); helpLine(sender, "setspawn"); helpLine(sender, "cityflag");
        helpLine(sender, "holo");
        if (isAdmin) {
            sender.sendMessage(Messages.get("info.help_cat_admin"));
            helpLine(sender, "admin_create"); helpLine(sender, "admin_tp"); helpLine(sender, "admin_claim");
            helpLine(sender, "admin_fill"); helpLine(sender, "admin_map");
            helpLine(sender, "admin_upgrade_manor"); helpLine(sender, "admin_upgrade_guild");
            helpLine(sender, "admin_delete"); helpLine(sender, "admin_worlds");
            helpLine(sender, "admin_whereami"); helpLine(sender, "admin_reload");
            helpLine(sender, "admin_setowner"); helpLine(sender, "admin_purge");
            helpLine(sender, "admin_regen"); helpLine(sender, "admin_export");
            helpLine(sender, "admin_fund"); helpLine(sender, "admin_citywall");
        }
        sender.sendMessage(Messages.get("info.help_footer"));
    }

    private void helpLine(CommandSender sender, String key) {
        String msg = Messages.get("info.help_ln_" + key);
        if (msg.startsWith("info.help_ln_")) return;
        sender.sendMessage(msg);
    }
}
