package dev.shardshop;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

final class ToolListener implements Listener {
    private final ShardShopPlugin pl;
    private final Set<UUID> busy = new HashSet<>();

    ToolListener(ShardShopPlugin pl) {
        this.pl = pl;
        // Self-destruct sweep
        Bukkit.getScheduler().runTaskTimer(pl, () -> {
            for (Player p : Bukkit.getOnlinePlayers()) sweep(p);
        }, 100L, 100L);
    }

    private void sweep(Player p) {
        PlayerInventory inv = p.getInventory();
        ItemStack[] contents = inv.getContents();
        boolean removed = false;
        for (int i = 0; i < contents.length; i++) {
            if (Items.expired(contents[i])) {
                inv.setItem(i, null);
                removed = true;
            }
        }
        if (removed) p.sendMessage(T.c("&cYour shard tool has self destructed."));
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent e) {
        sweep(e.getPlayer());
    }

    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onBreak(BlockBreakEvent e) {
        Player p = e.getPlayer();
        if (busy.contains(p.getUniqueId())) return;

        ItemStack tool = p.getInventory().getItemInMainHand();
        String mode = Items.mode(tool);
        if (mode == null) return;

        if (Items.expired(tool)) {
            e.setCancelled(true);
            p.getInventory().setItemInMainHand(null);
            p.sendMessage(T.c("&cYour shard tool has self destructed."));
            return;
        }

        Block origin = e.getBlock();
        List<Block> targets = new ArrayList<>();

        switch (mode) {
            case Items.PICKAXE -> {
                int up = pl.getConfig().getInt("pickaxe.up", 4);
                int down = pl.getConfig().getInt("pickaxe.down", 4);
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++)
                        for (int dy = -down; dy <= up; dy++)
                            targets.add(origin.getRelative(dx, dy, dz));
            }
            case Items.SHOVEL -> {
                for (int dx = -1; dx <= 1; dx++)
                    for (int dz = -1; dz <= 1; dz++)
                        targets.add(origin.getRelative(dx, 0, dz));
            }
            case Items.AXE -> {
                if (!Tag.LOGS.isTagged(origin.getType())) return;
                targets = connectedLogs(origin, pl.getConfig().getInt("axe.max-logs", 100));
            }
            default -> { return; }
        }

        busy.add(p.getUniqueId());
        try {
            for (Block b : targets) {
                if (b.equals(origin)) continue;
                if (!canBreak(b, tool)) continue;
                BlockBreakEvent ev = new BlockBreakEvent(b, p);
                Bukkit.getPluginManager().callEvent(ev);
                if (ev.isCancelled()) continue;
                if (p.getGameMode() == GameMode.CREATIVE || !ev.isDropItems()) {
                    b.setType(Material.AIR);
                } else {
                    b.breakNaturally(tool);
                }
            }
        } finally {
            busy.remove(p.getUniqueId());
        }
    }

    private boolean canBreak(Block b, ItemStack tool) {
        Material m = b.getType();
        if (m.isAir() || b.isLiquid()) return false;
        if (m.getHardness() < 0) return false;           // bedrock, barriers, etc.
        if (b.getState() instanceof Container) return false; // never wipe chests
        if (Tag.LOGS.isTagged(m) && Items.AXE.equals(Items.mode(tool))) return true;
        return b.isPreferredTool(tool);
    }

    private List<Block> connectedLogs(Block start, int max) {
        List<Block> found = new ArrayList<>();
        Set<Block> seen = new HashSet<>();
        ArrayDeque<Block> queue = new ArrayDeque<>();
        queue.add(start);
        seen.add(start);
        while (!queue.isEmpty() && found.size() < max) {
            Block cur = queue.poll();
            found.add(cur);
            for (int dx = -1; dx <= 1; dx++)
                for (int dy = -1; dy <= 1; dy++)
                    for (int dz = -1; dz <= 1; dz++) {
                        if (dx == 0 && dy == 0 && dz == 0) continue;
                        Block n = cur.getRelative(dx, dy, dz);
                        if (seen.contains(n) || !Tag.LOGS.isTagged(n.getType())) continue;
                        seen.add(n);
                        queue.add(n);
                    }
        }
        return found;
    }
}
