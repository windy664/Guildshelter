package org.windy.guildshelter.adapter.bukkit.command.player;

import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.windy.guildshelter.adapter.bukkit.BlockDisplayNames;
import org.windy.guildshelter.adapter.bukkit.Messages;
import org.windy.guildshelter.adapter.bukkit.command.GsSubCommand;
import org.windy.guildshelter.adapter.bukkit.command.SubCommand;
import org.windy.guildshelter.domain.model.GuildWorld;
import org.windy.guildshelter.domain.model.Manor;
import org.windy.guildshelter.domain.model.PlayerRef;
import org.windy.guildshelter.domain.port.EconomyPort;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * /gs upgrade：按 levels.yml 的 Vault 金额与背包物品成本升级当前庄园。
 */
@GsSubCommand(name = "upgrade", permission = "guildshelter.command.upgrade")
public class UpgradeCommand extends SubCommand {

    private record UpgradeCost(double money, List<ItemCost> items) {}
    private record ItemCost(String id, Material material, int amount) {}

    private EconomyPort economyCache;
    private boolean economyResolved;

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof Player player)) { sender.sendMessage(Messages.get("error.only_player_upgrade")); return; }
        Manor manor = ctx.currentOwnManor(player).orElse(null);
        if (manor == null) { sender.sendMessage(Messages.get("error.no_manor")); return; }
        GuildWorld gw = ctx.guilds.find(manor.guild()).orElse(null);
        if (gw == null) { sender.sendMessage(Messages.get("error.guild_not_exist", manor.guild().value())); return; }
        int cap = ctx.levels.manorMaxLevel();
        if (!ctx.levels.canUpgradeManor(manor.level())) { sender.sendMessage(Messages.get("error.already_max_level", cap)); return; }
        int targetLevel = manor.level() + 1;
        UpgradeCost cost;
        try { cost = loadUpgradeCost(targetLevel); } catch (IllegalArgumentException ex) { sender.sendMessage(Messages.get("error.upgrade_config_error", ex.getMessage())); return; }
        PlayerRef ref = PlayerRef.of(player.getUniqueId());
        EconomyPort eco = null;
        if (cost.money() > 0) {
            eco = economy();
            if (eco == null) { sender.sendMessage(Messages.get("error.upgrade_no_economy", cost.money())); return; }
            if (!eco.has(ref, cost.money())) { sender.sendMessage(Messages.get("error.upgrade_insufficient_funds", eco.format(cost.money()))); return; }
        }
        List<String> missing = missingItems(player, cost.items());
        if (!missing.isEmpty()) {
            sender.sendMessage(Messages.get("error.upgrade_insufficient_items"));
            for (String line : missing) sender.sendMessage("§7- " + line);
            return;
        }
        if (eco != null && !eco.withdraw(ref, cost.money())) { sender.sendMessage(Messages.get("error.upgrade_payment_failed")); return; }
        removeItems(player, cost.items());
        boolean ok = ctx.service.upgradeManor(manor.guild(), manor.slot());
        if (!ok) {
            if (eco != null) eco.deposit(ref, cost.money());
            refundItems(player, cost.items());
            sender.sendMessage(Messages.get("error.already_max_level", cap));
            return;
        }
        Manor upgraded = ctx.manors.findBySlot(manor.guild(), manor.slot()).orElseThrow();
        String moneyText = cost.money() > 0 && eco != null ? " §7| §e-" + eco.format(cost.money()) : "";
        sender.sendMessage(Messages.get("success.upgraded", upgraded.level(), cap) + moneyText);
    }

    private EconomyPort economy() {
        if (!economyResolved) { economyCache = org.windy.guildshelter.adapter.bukkit.VaultEconomy.tryCreate(ctx.logger); economyResolved = true; }
        return economyCache;
    }

    private UpgradeCost loadUpgradeCost(int targetLevel) {
        File file = new File(ctx.plugin.getDataFolder(), "levels.yml");
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        String path = "manor.levels." + targetLevel + ".upgrade";
        ConfigurationSection section = cfg.getConfigurationSection(path);
        if (section == null) return new UpgradeCost(0.0, List.of());
        double money = Math.max(0.0, section.getDouble("money", 0.0));
        List<ItemCost> items = new ArrayList<>();
        for (String raw : section.getStringList("items")) {
            if (raw == null || raw.isBlank()) continue;
            items.add(parseItemCost(raw));
        }
        return new UpgradeCost(money, List.copyOf(items));
    }

    private ItemCost parseItemCost(String raw) {
        String text = raw.trim();
        int split = text.lastIndexOf(':');
        if (split <= 0 || split == text.length() - 1) throw new IllegalArgumentException("物品成本必须写成 material:amount，例如 minecraft:stone:64");
        String id = text.substring(0, split).trim();
        int amount;
        try { amount = Integer.parseInt(text.substring(split + 1).trim()); } catch (NumberFormatException ex) { throw new IllegalArgumentException("物品数量不是数字: " + raw); }
        if (amount <= 0) throw new IllegalArgumentException("物品数量必须大于 0: " + raw);
        Material material = material(id);
        if (material == null || !material.isItem()) throw new IllegalArgumentException("未知或不可作为物品的材料: " + id);
        return new ItemCost(id, material, amount);
    }

    private static Material material(String id) {
        String normalized = id.trim().toLowerCase(Locale.ROOT);
        Material material = Material.matchMaterial(normalized, false);
        if (material != null) return material;
        int colon = normalized.indexOf(':');
        String key = colon >= 0 ? normalized.substring(colon + 1) : normalized;
        return Material.matchMaterial(key.toUpperCase(Locale.ROOT), false);
    }

    private List<String> missingItems(Player player, List<ItemCost> costs) {
        List<String> missing = new ArrayList<>();
        for (ItemCost cost : costs) {
            int have = countItems(player, cost.material());
            if (have < cost.amount()) missing.add(itemName(cost) + " §7需要 §f" + cost.amount() + " §7当前 §c" + have);
        }
        return missing;
    }

    private int countItems(Player player, Material material) {
        int count = 0;
        for (ItemStack stack : player.getInventory().getStorageContents()) {
            if (stack != null && stack.getType() == material) count += stack.getAmount();
        }
        return count;
    }

    private void removeItems(Player player, List<ItemCost> costs) {
        for (ItemCost cost : costs) {
            int remaining = cost.amount();
            ItemStack[] contents = player.getInventory().getStorageContents();
            for (int i = 0; i < contents.length && remaining > 0; i++) {
                ItemStack stack = contents[i];
                if (stack == null || stack.getType() != cost.material()) continue;
                int take = Math.min(stack.getAmount(), remaining);
                stack.setAmount(stack.getAmount() - take);
                remaining -= take;
                if (stack.getAmount() <= 0) contents[i] = null;
            }
            player.getInventory().setStorageContents(contents);
        }
    }

    private void refundItems(Player player, List<ItemCost> costs) {
        for (ItemCost cost : costs) {
            var leftovers = player.getInventory().addItem(new ItemStack(cost.material(), cost.amount()));
            for (ItemStack stack : leftovers.values()) player.getWorld().dropItemNaturally(player.getLocation(), stack);
        }
    }

    private String itemName(ItemCost cost) { return "§f" + BlockDisplayNames.display(cost.id()); }
}
