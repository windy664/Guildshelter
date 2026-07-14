package org.windy.guildshelter.farm;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.windy.guildshelter.api.GuildShelterAPI;

/**
 * GuildShelterFarm：官方农场附属（A1 共享农场 + A2 生长加速 + 偷菜）。
 *
 * <p><b>不碰 GuildShelter 内部</b>——从 ServicesManager 取只读 {@link GuildShelterAPI}：
 * <ul>
 *   <li>注册 {@link FarmCheckProvider}(BuildCheckProvider)：放行成员农事 / 非成员偷成熟作物。</li>
 *   <li>{@link FarmGrowthListener}：作物/幼崽随公会等级加速。</li>
 *   <li>{@link FarmStealListener}：偷菜计数 + 通知。</li>
 * </ul>
 * 是"核心留 API、玩法做附属"的完整范例（含决策参与 dogfooding）。见 guildshelter-api/README.md。
 */
public final class FarmPlugin extends JavaPlugin {

    private GuildShelterAPI api;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        RegisteredServiceProvider<GuildShelterAPI> rsp =
                getServer().getServicesManager().getRegistration(GuildShelterAPI.class);
        if (rsp == null) {
            getLogger().severe("未找到 GuildShelterAPI（GuildShelter 未启用或版本过旧），农场附属停用。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.api = rsp.getProvider();
        var cfg = getConfig();

        FarmBlocks farmBlocks = new FarmBlocks(cfg.getStringList("farm-blocks"));
        StealQuota stealQuota = new StealQuota(cfg.getInt("steal.daily-limit", 32));

        boolean shareEnabled = cfg.getBoolean("share.enabled", true);
        boolean allowContainers = cfg.getBoolean("share.allow-containers", true);
        boolean stealEnabled = cfg.getBoolean("steal.enabled", true);
        boolean stealOnlyMature = cfg.getBoolean("steal.only-mature", true);

        // 决策参与：放行成员农事 + 非成员偷成熟作物。priority 100（默认靠后即可）。
        if (shareEnabled || stealEnabled) {
            api.registerBuildCheck(this, new FarmCheckProvider(api, farmBlocks, shareEnabled, allowContainers,
                    stealEnabled, stealOnlyMature, stealQuota), 100);
        }

        // 偷菜计数/通知
        if (stealEnabled) {
            getServer().getPluginManager().registerEvents(
                    new FarmStealListener(api, farmBlocks, stealQuota, cfg.getBoolean("steal.notify-owner", true)), this);
        }

        // 生长加速
        if (cfg.getBoolean("growth.enabled", true)) {
            getServer().getPluginManager().registerEvents(new FarmGrowthListener(api,
                    cfg.getDouble("growth.crop-multiplier-per-level", 0.08),
                    cfg.getDouble("growth.crop-max-extra-chance", 0.6),
                    cfg.getBoolean("growth.breed-enabled", true),
                    cfg.getDouble("growth.breed-multiplier-per-level", 0.08),
                    cfg.getDouble("growth.breed-max-extra-chance", 0.6),
                    cfg.getInt("growth.breed-bump-ticks", 2400)), this);
        }

        getLogger().info("GuildShelterFarm 已启用：共享农场 " + shareEnabled + " / 加速 "
                + cfg.getBoolean("growth.enabled", true) + " / 偷菜 " + stealEnabled + "。");
    }

    @Override
    public void onDisable() {
        if (api != null) {
            api.unregisterBuildChecks(this); // 主动注销（主插件也会在本插件 disable 时自动清理，双保险）
        }
    }
}
