package org.windy.guildshelter.xaero;

import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.windy.guildshelter.api.GuildShelterAPI;

/** Bukkit-side entry point for the optional Xaero map bridge. */
public final class XaeroMapAddonPlugin extends JavaPlugin {

    private GuildShelterAPI api;
    private XaeroMapChannel channel;

    @Override
    public void onEnable() {
        this.api = resolveApi();
        if (api == null) {
            getLogger().severe("GuildShelterAPI is not registered. Disabling Xaero map bridge.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.channel = new XaeroMapChannel(this, api, getLogger());
        this.channel.register();
        getLogger().info("GuildShelter Xaero map bridge is enabled.");
    }

    @Override
    public void onDisable() {
        this.api = null;
        this.channel = null;
    }

    public GuildShelterAPI api() {
        return api;
    }

    private GuildShelterAPI resolveApi() {
        RegisteredServiceProvider<GuildShelterAPI> provider =
                getServer().getServicesManager().getRegistration(GuildShelterAPI.class);
        return provider == null ? null : provider.getProvider();
    }
}
