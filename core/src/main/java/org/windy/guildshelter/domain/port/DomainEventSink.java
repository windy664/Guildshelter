package org.windy.guildshelter.domain.port;

import org.windy.guildshelter.domain.model.GuildId;

import java.util.UUID;

/**
 * <b>领域事件出口</b>（core 端口）：{@link org.windy.guildshelter.service.GuildService} 在关键领地动作处发布,
 * 平台层（bukkit-common）实现它、转成对外 Bukkit 事件给附属插件监听（见 PLAN_API.md）。
 *
 * <p>专收那些<b>没有现成回调</b>的生命周期点（建会/解锁/升级）；成员入会/退会/解散已走
 * {@link MembershipChangeListener}，不在此重复。全部 default 空实现 → {@link #NONE} 即空操作,
 * 未注入时 service 静默。core 保持平台无关（只用领域类型/UUID,不碰 Bukkit）。
 */
public interface DomainEventSink {

    /** 空操作（未注入时用）。 */
    DomainEventSink NONE = new DomainEventSink() {};

    /** 公会营地建成（finishCreate 末尾）。 */
    default void onGuildCreated(GuildId guild) {}

    /** 玩家解锁了一个 chunk（unlockChunk 成功）。坐标为世界 chunk 坐标。 */
    default void onChunkUnlocked(GuildId guild, int slot, int worldChunkX, int worldChunkZ, UUID player) {}

    /** 庄园升级（upgradeManor 成功）。 */
    default void onManorUpgraded(GuildId guild, int slot, int oldLevel, int newLevel, UUID owner) {}

    /** 公会升级（upgradeGuild 成功）。 */
    default void onGuildUpgraded(GuildId guild, int newLevel) {}
}
