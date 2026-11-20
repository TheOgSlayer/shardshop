package dev.shardshop;

import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

final class ItemStore {
    static final class Entry {
        final int id;
        final long price;
        final int expireHours; // 0 = never
        final ItemStack item;

        Entry(int id, long price, int expireHours, ItemStack item) {
            this.id = id;
            this.price = price;
            this.expireHours = expireHours;
            this.item = item;
        }
    }

    private final ShardShopPlugin pl;
    private final File file;
    private final TreeMap<Integer, Entry> entries = new TreeMap<>();

    ItemStore(ShardShopPlugin pl) {
        this.pl = pl;
        this.file = new File(pl.getDataFolder(), "items.yml");
    }

    void load() {
        entries.clear();
        if (!file.exists()) {
            seedDefaults();
            save();
            return;
        }
        YamlConfiguration y = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection sec = y.getConfigurationSection("items");
        if (sec == null) return;
        for (String k : sec.getKeys(false)) {
            try {
                int id = Integer.parseInt(k);
                ConfigurationSection s = sec.getConfigurationSection(k);
                if (s == null) continue;
                ItemStack item = s.getItemStack("item");
                if (item == null) continue;
                entries.put(id, new Entry(id, s.getLong("price"), s.getInt("expire-hours"), item));
            } catch (NumberFormatException ignored) {}
        }
    }

    void save() {
        YamlConfiguration y = new YamlConfiguration();
        for (Entry e : entries.values()) {
            String p = "items." + e.id;
            y.set(p + ".price", e.price);
            y.set(p + ".expire-hours", e.expireHours);
            y.set(p + ".item", e.item);
        }
        try {
            y.save(file);
        } catch (IOException ex) {
            pl.getLogger().warning("Could not save items.yml: " + ex.getMessage());
        }
    }

    List<Entry> all() {
        return new ArrayList<>(entries.values());
    }

    int add(long price, int hours, ItemStack item) {
        int id = entries.isEmpty() ? 1 : entries.lastKey() + 1;
        entries.put(id, new Entry(id, price, hours, item));
        save();
        return id;
    }

    boolean remove(int id) {
        boolean r = entries.remove(id) != null;
        if (r) save();
        return r;
    }

    private void seedDefaults() {
        String pick = T.BLUE + "Shard Pickaxe";
        String axe = T.BLUE + "Shard Axe";
        String shovel = T.BLUE + "Shard Shovel";

        // 1) Silk Touch pickaxe
        entries.put(1, new Entry(1, 1000, 24, Items.tool(Material.NETHERITE_PICKAXE, pick, Items.PICKAXE,
                Enchantment.SILK_TOUCH, 1, Enchantment.MENDING, 1, Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3)));
        // 2) Fortune pickaxe
        entries.put(2, new Entry(2, 1000, 24, Items.tool(Material.NETHERITE_PICKAXE, pick, Items.PICKAXE,
                Enchantment.FORTUNE, 3, Enchantment.MENDING, 1, Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3)));
        // 3) Axe
        entries.put(3, new Entry(3, 1000, 24, Items.tool(Material.NETHERITE_AXE, axe, Items.AXE,
                Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1)));
        // 4) Silk Touch shovel
        entries.put(4, new Entry(4, 1000, 24, Items.tool(Material.NETHERITE_SHOVEL, shovel, Items.SHOVEL,
                Enchantment.SILK_TOUCH, 1, Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1)));
        // 5) Fortune shovel
        entries.put(5, new Entry(5, 1000, 24, Items.tool(Material.NETHERITE_SHOVEL, shovel, Items.SHOVEL,
                Enchantment.FORTUNE, 3, Enchantment.EFFICIENCY, 5, Enchantment.UNBREAKING, 3, Enchantment.MENDING, 1)));
    }
}
