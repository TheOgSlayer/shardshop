package dev.shardshop;

import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/** Bridge to the shard system inside ShopPlugin (uses reflection, no compile dependency). */
final class Shards {
    private final Plugin shop;
    private final Method getM;
    private final Method setM;
    private final Method saveM;
    private final Field dataF;

    Shards() throws Exception {
        shop = Bukkit.getPluginManager().getPlugin("ShopPlugin");
        if (shop == null) throw new IllegalStateException("ShopPlugin not found");
        Class<?> c = shop.getClass();
        getM = c.getMethod("getShards", UUID.class);
        setM = c.getMethod("setShards", UUID.class, long.class);
        Method s = null;
        try {
            s = c.getDeclaredMethod("saveData");
            s.setAccessible(true);
        } catch (Exception ignored) {}
        saveM = s;
        Field f = null;
        try {
            f = c.getDeclaredField("data");
            f.setAccessible(true);
        } catch (Exception ignored) {}
        dataF = f;
    }

    long get(UUID id) {
        try {
            return (Long) getM.invoke(shop, id);
        } catch (Exception e) {
            return 0L;
        }
    }

    void set(UUID id, long amount) {
        try {
            setM.invoke(shop, id, Math.max(0L, amount));
            if (saveM != null) saveM.invoke(shop);
        } catch (Exception e) {
            Bukkit.getLogger().warning("[ShardShop] Could not set shards: " + e.getMessage());
        }
    }

    /** Takes shards if the player can afford it. */
    boolean take(UUID id, long amount) {
        long cur = get(id);
        if (cur < amount) return false;
        set(id, cur - amount);
        return true;
    }

    /** All known shard balances (offline players included). */
    Map<UUID, Long> all() {
        Map<UUID, Long> out = new HashMap<>();
        try {
            YamlConfiguration y = null;
            if (dataF != null) y = (YamlConfiguration) dataF.get(shop);
            if (y == null) y = YamlConfiguration.loadConfiguration(new File(shop.getDataFolder(), "data.yml"));
            ConfigurationSection sec = y.getConfigurationSection("shards");
            if (sec != null) {
                for (String k : sec.getKeys(false)) {
                    try {
                        out.put(UUID.fromString(k), sec.getLong(k));
                    } catch (IllegalArgumentException ignored) {}
                }
            }
        } catch (Exception ignored) {}
        for (Player p : Bukkit.getOnlinePlayers()) out.put(p.getUniqueId(), get(p.getUniqueId()));
        return out;
    }
}
