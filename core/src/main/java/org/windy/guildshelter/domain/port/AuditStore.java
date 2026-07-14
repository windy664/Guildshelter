package org.windy.guildshelter.domain.port;

import org.windy.guildshelter.domain.model.GuildId;

import java.util.List;

/**
 * 领地操作<b>审计日志</b>持久化（借鉴 HuskTowns town logs，见 PLAN_AUDIT.md）：记录"谁在何时对哪个公会做了什么"，
 * 供会长/管理员追责防坑。<b>append-only</b>（只插入、按 guild 倒序读、按时间清旧），三后端各自实现。
 *
 * <p>写入应<b>异步</b>（见 {@code service/AuditLog}），不堵主线程；读取（{@link #recent}）在命令线程同步即可。
 */
public interface AuditStore {

    /**
     * 一条审计记录。
     *
     * @param id        自增主键（写入时忽略，读取时填）
     * @param ts        毫秒时间戳
     * @param guildId   关联公会（领地维度）
     * @param actorUuid 操作者 UUID 字符串；{@code null}/空 = 系统/管理动作（无具体玩家）
     * @param action    动作标识（如 {@code manor_assign}/{@code chunk_unlock}/{@code guild_dissolve}），用于本地化展示
     * @param target    动作对象（如 {@code guild#slot}、chunk 坐标）；可空
     * @param detail    补充说明；可空
     */
    record Entry(long id, long ts, String guildId, String actorUuid, String action, String target, String detail) {}

    /** 追加一条记录（实现侧仅 INSERT；id 由库生成）。 */
    void record(Entry entry);

    /** 某公会最近的记录，按时间<b>倒序</b>，分页（limit 条，跳过 offset 条）。 */
    List<Entry> recent(GuildId guild, int limit, int offset);

    /** 删除时间早于 {@code beforeMillis} 的记录（保留期清理）。 */
    void purgeOld(long beforeMillis);

    /** 清除某公会的全部审计记录（解散时可选调用）。 */
    void clear(GuildId guild);
}
