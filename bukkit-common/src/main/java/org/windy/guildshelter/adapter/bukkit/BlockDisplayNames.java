package org.windy.guildshelter.adapter.bukkit;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Locale;

/** Bukkit 侧的方块/机器显示名解析。配置只写 id，展示名运行时从服务端 API 取。 */
public final class BlockDisplayNames {

    private BlockDisplayNames() {
    }

    public static String display(String blockId) {
        if (blockId == null || blockId.isBlank()) {
            return "";
        }
        Material material = material(blockId);
        if (material == null) {
            return blockId;
        }
        String itemName = itemName(material);
        if (!itemName.isBlank()) {
            return itemName;
        }
        return prettify(material.getKey().getKey());
    }

    private static Material material(String blockId) {
        String id = blockId.trim().toLowerCase(Locale.ROOT);
        Material material = Material.matchMaterial(id, false);
        if (material != null) {
            return material;
        }
        int colon = id.indexOf(':');
        String key = colon >= 0 ? id.substring(colon + 1) : id;
        return Material.matchMaterial(key.toUpperCase(Locale.ROOT), false);
    }

    private static String itemName(Material material) {
        if (!material.isItem()) {
            return "";
        }
        ItemMeta meta = new ItemStack(material).getItemMeta();
        if (meta == null) {
            return "";
        }
        if (meta.hasItemName()) {
            return meta.getItemName();
        }
        if (meta.hasDisplayName()) {
            return meta.getDisplayName();
        }
        return "";
    }

    private static String prettify(String key) {
        String[] parts = key.split("_");
        StringBuilder out = new StringBuilder();
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            if (!out.isEmpty()) {
                out.append(' ');
            }
            out.append(Character.toUpperCase(part.charAt(0)));
            if (part.length() > 1) {
                out.append(part.substring(1));
            }
        }
        return out.isEmpty() ? key : out.toString();
    }
}
