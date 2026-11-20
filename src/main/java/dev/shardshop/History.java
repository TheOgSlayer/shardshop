package dev.shardshop;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

final class History {
    record Rec(long time, String item, long price) {}

    private final ShardShopPlugin pl;
    private final File file;
    private final YamlConfiguration y;

    History(ShardShopPlugin pl) {
        this.pl = pl;
        this.file = new File(pl.getDataFolder(), "history.yml");
        this.y = YamlConfiguration.loadConfiguration(file);
    }

    void add(UUID id, String itemName, long price) {
        List<Map<?, ?>> list = new ArrayList<>(y.getMapList(id.toString()));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("time", System.currentTimeMillis());
        m.put("item", itemName);
        m.put("price", price);
        list.add(m);
        int limit = Math.max(1, pl.getConfig().getInt("history-limit", 45));
        while (list.size() > limit) list.remove(0);
        y.set(id.toString(), list);
        try {
            y.save(file);
        } catch (IOException e) {
            pl.getLogger().warning("Could not save history.yml: " + e.getMessage());
        }
    }

    /** Newest first. */
    List<Rec> get(UUID id) {
        List<Rec> out = new ArrayList<>();
        for (Map<?, ?> m : y.getMapList(id.toString())) {
            try {
                out.add(new Rec(((Number) m.get("time")).longValue(), String.valueOf(m.get("item")),
                        ((Number) m.get("price")).longValue()));
            } catch (Exception ignored) {}
        }
        java.util.Collections.reverse(out);
        return out;
    }
}
