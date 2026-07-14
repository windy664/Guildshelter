package org.windy.guildshelter.service;

import org.windy.guildshelter.domain.model.GuildId;
import org.windy.guildshelter.domain.port.AuditStore;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * 审计日志<b>写入服务</b>：包装 {@link AuditStore}，把记录写放到<b>单后台线程</b>异步执行，不堵主线程
 * （领地写操作在主线程触发，DB 写不应卡 tick）。读取（{@link #recent}）走调用线程（命令场景，可接受）。
 *
 * <p>关闭功能（config {@code audit.enabled=false}）时用 {@link #disabled()} 取得一个<b>空操作实例</b>，
 * 所有 record/recent 静默无副作用——调用方无需各处判空。
 */
public final class AuditLog {

    /** 空操作实例：审计关闭时用，record/recent 全无副作用。 */
    private static final AuditLog DISABLED = new AuditLog(null, null);

    private final AuditStore store;          // null = 关闭
    private final ExecutorService writer;    // null = 关闭
    private final Logger logger;

    private AuditLog(AuditStore store, Logger logger) {
        this.store = store;
        this.logger = logger;
        this.writer = store == null ? null
                : Executors.newSingleThreadExecutor(r -> {
                    Thread t = new Thread(r, "GuildShelter-Audit");
                    t.setDaemon(true); // 不阻止 JVM 退出；关闭时 shutdown 会排空
                    return t;
                });
    }

    /** 启用：绑定后端 + 后台写线程。 */
    public static AuditLog enabled(AuditStore store, Logger logger) {
        return new AuditLog(store, logger);
    }

    /** 关闭：空操作。 */
    public static AuditLog disabled() {
        return DISABLED;
    }

    public boolean isEnabled() {
        return store != null;
    }

    /** 异步追加一条审计记录（关闭时静默）。actor 可为 null（系统动作）。 */
    public void record(GuildId guild, String actorUuid, String action, String target, String detail) {
        if (store == null || guild == null) {
            return;
        }
        AuditStore.Entry e = new AuditStore.Entry(0, System.currentTimeMillis(),
                guild.value(), actorUuid, action, target, detail);
        try {
            writer.execute(() -> {
                try {
                    store.record(e);
                } catch (Exception ex) {
                    if (logger != null) {
                        logger.log(Level.WARNING, "[GuildShelter] 审计写入失败: " + action, ex);
                    }
                }
            });
        } catch (java.util.concurrent.RejectedExecutionException ignored) {
            // 已 shutdown（停服中）→ 丢弃这条审计，不影响业务
        }
    }

    /** 读取某公会最近记录（同步，命令用）；关闭时返回空。 */
    public List<AuditStore.Entry> recent(GuildId guild, int limit, int offset) {
        return store == null ? List.of() : store.recent(guild, limit, offset);
    }

    /** 清理保留期外的旧记录（关闭时空操作）。同步执行（启动/定时任务调用，量小）。 */
    public void purgeOld(long beforeMillis) {
        if (store != null) {
            store.purgeOld(beforeMillis);
        }
    }

    /** 停服时排空后台写队列（最多等 5 秒）。 */
    public void shutdown() {
        if (writer == null) {
            return;
        }
        writer.shutdown();
        try {
            if (!writer.awaitTermination(5, TimeUnit.SECONDS)) {
                writer.shutdownNow();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            writer.shutdownNow();
        }
    }
}
