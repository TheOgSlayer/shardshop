package dev.shardshop;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;

final class Items {
    static NamespacedKey MODE;
    static NamespacedKey EXPIRES;

    static final String PICKAXE = "pickaxe";
    static final String AXE = "axe";
    static final String SHOVEL = "shovel";

    private Items() {}

    static void init(Plugin p) {
        MODE = new NamespacedKey(p, "tool_mode");
        EXPIRES = new NamespacedKey(p, "expires_at");
    }

    static String mode(ItemStack s) {
        if (s == null || !s.hasItemMeta()) return null;
        return s.getItemMeta().getPersistentDataContainer().get(MODE, PersistentDataType.STRING);
    }

    /** Epoch millis when the item self-destructs, or 0 if it never does. */
    static long expires(ItemStack s) {
        if (s == null || !s.hasItemMeta()) return 0L;
        Long v = s.getItemMeta().getPersistentDataContainer().get(EXPIRES, PersistentDataType.LONG);
        return v == null ? 0L : v;
    }

    static boolean expired(ItemStack s) {
        long e = expires(s);
        return e > 0 && System.currentTimeMillis() >= e;
    }

    static ItemStack setMode(ItemStack item, String mode) {
        ItemStack out = item.clone();
        ItemMeta m = out.getItemMeta();
        m.getPersistentDataContainer().set(MODE, PersistentDataType.STRING, mode);
        out.setItemMeta(m);
        return out;
    }

    static ItemStack setExpires(ItemStack item, long at) {
        ItemStack out = item.clone();
        ItemMeta m = out.getItemMeta();
        m.getPersistentDataContainer().set(EXPIRES, PersistentDataType.LONG, at);
        out.setItemMeta(m);
        return out;
    }

    static List<String> modeLore(String mode) {
        List<String> l = new ArrayList<>();
        if (PICKAXE.equals(mode)) {
            l.add("&fMines 9 blocks vertically");
            l.add("&f3 width x 3 length");
        } else if (AXE.equals(mode)) {
            l.add("&fBreaks all connected logs");
            l.add("&f(max 100 logs per mine)");
        } else if (SHOVEL.equals(mode)) {
            l.add("&fBreaks 9 blocks at once");
            l.add("&f3 width x 3 length");
        }
        return l;
    }

    /** Adds extra lore lines (legacy/hex strings) to an item. */
    static ItemStack addLore(ItemStack item, List<String> lines) {
        ItemStack out = item.clone();
        ItemMeta m = out.getItemMeta();
        List<Component> lore = m.hasLore() ? new ArrayList<>(m.lore()) : new ArrayList<>();
        for (String s : lines) lore.add(T.c(s));
        m.lore(lore);
        out.setItemMeta(m);
        return out;
    }

    /** Builds a default Shard tool. enchants = Enchantment, Integer, Enchantment, Integer ... */
    static ItemStack tool(Material mat, String name, String mode, Object... enchants) {
        ItemStack it = new ItemStack(mat);
        ItemMeta m = it.getItemMeta();
        m.displayName(T.c(name));
        for (int i = 0; i + 1 < enchants.length; i += 2) {
            m.addEnchant((Enchantment) enchants[i], (Integer) enchants[i + 1], true);
        }
        List<Component> lore = new ArrayList<>();
        for (String s : modeLore(mode)) lore.add(T.c(s));
        m.lore(lore);
        m.getPersistentDataContainer().set(MODE, PersistentDataType.STRING, mode);
        it.setItemMeta(m);
        return it;
    }
}
