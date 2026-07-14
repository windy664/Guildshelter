package org.windy.guildshelter.adapter.bungee;

import org.bukkit.entity.Player;

/**
 * 代理跨服传送抽象。
 * 通过 PluginMessage 通道让代理把玩家送到指定服务器。
 */
public interface ProxyChannel {

    /** 把玩家送到指定服务器。 */
    void sendToServer(Player player, String serverName);

    /** 是否可用（通道已注册）。 */
    boolean isAvailable();

    /**
     * 按跨服开关创建实例。
     * @param crossServer 是否启用代理跨服
     * @param plugin 插件实例（注册通道用）
     */
    static ProxyChannel create(boolean crossServer, org.bukkit.plugin.Plugin plugin) {
        return crossServer ? new BungeeCordChannel(plugin) : new NoOpChannel();
    }
}
