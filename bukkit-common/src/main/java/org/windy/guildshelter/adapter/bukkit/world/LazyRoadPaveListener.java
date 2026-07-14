package org.windy.guildshelter.adapter.bukkit.world;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.world.ChunkLoadEvent;

/**
 * 纯 Bukkit 端的惰性路面铺设监听：区块<b>新生成</b>（{@link ChunkLoadEvent#isNewChunk()}）时把它那一格路铺好，
 * 委托 {@link LazyRoadPaver}。只在 Iris 等惰性世界生效（{@code LazyRoadPaver} 内部判定）。
 *
 * <p>仅监听 {@code isNewChunk()=true}：只在区块"首次生成"那一刻铺，避免每次加载已存在区块都重铺（浪费）。
 * 混合端走 NeoForge 原生 {@code ChunkEvent.Load}（见 NeoForge 侧），不注册本监听以免双触发。
 */
public final class LazyRoadPaveListener implements Listener {

    private final LazyRoadPaver paver;

    public LazyRoadPaveListener(LazyRoadPaver paver) {
        this.paver = paver;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onChunkLoad(ChunkLoadEvent event) {
        if (!event.isNewChunk()) {
            return; // 只在首次生成时铺；已存在区块的重复加载不处理
        }
        paver.onChunkGenerated(event.getWorld().getName(), event.getChunk().getX(), event.getChunk().getZ());
    }
}
