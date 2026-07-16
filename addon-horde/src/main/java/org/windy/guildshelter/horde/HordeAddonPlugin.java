package org.windy.guildshelter.horde;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.windy.guildshelter.api.GuildShelterAPI;
import org.windy.guildshelter.api.event.GuildDissolvedEvent;

public final class HordeAddonPlugin extends JavaPlugin implements Listener {

    private GuildShelterAPI api;
    private HordeManager manager;
    private HordeMessages messages;
    private HordeEntityMarker marker;
    private WeeklyHordeScheduler weeklyScheduler;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        this.api = resolveApi();
        if (api == null) {
            getLogger().severe("未找到 GuildShelterAPI 服务，正在禁用尸潮附属。");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.messages = new HordeMessages(getConfig());
        this.marker = new HordeEntityMarker(this);
        this.manager = new HordeManager(this, api, getConfig(), messages, marker);
        this.weeklyScheduler = WeeklyHordeScheduler.create(this, api, manager, getConfig());

        getServer().getPluginManager().registerEvents(this, this);
        getServer().getPluginManager().registerEvents(new HordeCombatListener(api, manager.settings(), marker), this);
        getServer().getPluginManager().registerEvents(new HordeReminderListener(this, api, manager, weeklyScheduler,
                messages), this);
        if (getCommand("gshorde") != null) {
            getCommand("gshorde").setExecutor(this);
        }
        weeklyScheduler.start();
        getLogger().info("GuildShelter 尸潮附属已启用。");
    }

    @Override
    public void onDisable() {
        if (weeklyScheduler != null) {
            weeklyScheduler.stop();
            weeklyScheduler = null;
        }
        if (manager != null) {
            manager.stopAll(messages == null ? "插件卸载，尸潮已停止。" : messages.get("session.plugin-disabled"));
            manager = null;
        }
        marker = null;
        messages = null;
        api = null;
    }

    @EventHandler
    public void onGuildDissolved(GuildDissolvedEvent event) {
        if (manager != null) {
            manager.stopAtWorldName(event.guild().worldName(), messages.get("session.camp-dissolved"));
        }
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!"gshorde".equalsIgnoreCase(command.getName())) {
            return false;
        }
        if (!sender.hasPermission("guildshelter.horde.manage")) {
            sender.sendMessage(messages.get("command.no-permission"));
            return true;
        }
        if (args.length == 0) {
            sender.sendMessage(messages.get("command.usage"));
            return true;
        }
        String sub = args[0].toLowerCase();
        String worldName = args.length >= 2 ? args[1] : null;
        String message = switch (sub) {
            case "start" -> worldName == null ? startAtSender(sender) : manager.startAtWorldName(worldName);
            case "stop" -> worldName == null ? stopAtSender(sender) : manager.stopAtWorldName(worldName,
                    messages.get("session.manual-stop"));
            case "status" -> worldName == null ? statusAtSender(sender) : manager.statusAtWorldName(worldName);
            default -> messages.get("command.usage");
        };
        sender.sendMessage(message);
        return true;
    }

    private String startAtSender(CommandSender sender) {
        if (sender instanceof Player player) {
            return manager.startAt(player.getLocation());
        }
        return messages.get("command.console-start-usage");
    }

    private String stopAtSender(CommandSender sender) {
        if (sender instanceof Player player) {
            return manager.stopAt(player.getLocation(), messages.get("session.manual-stop"));
        }
        return messages.get("command.console-stop-usage");
    }

    private String statusAtSender(CommandSender sender) {
        if (sender instanceof Player player) {
            return manager.statusAt(player.getLocation());
        }
        return messages.get("command.console-status-usage");
    }

    private GuildShelterAPI resolveApi() {
        RegisteredServiceProvider<GuildShelterAPI> provider =
                getServer().getServicesManager().getRegistration(GuildShelterAPI.class);
        return provider == null ? null : provider.getProvider();
    }
}
