package dev.shardshop;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

final class ShopGui implements Listener {
    enum Type { SHOP, RANKS, HISTORY }

    static final class Holder implements InventoryHolder {
        final Type type;
        final Map<Integer, ItemStore.Entry> bySlot = new HashMap<>();
        Inventory inv;

        Holder(Type type) { this.type = type; }

        @Override
        public Inventory getInventory() { return inv; }
    }

    static final int SLOT_RANKS = 47;
    static final int SLOT_SHARDS = 49;
    static final int SLOT_HISTORY = 51;
    static final int SLOT_BACK = 49;

    private final ShardShopPlugin pl;

    ShopGui(ShardShopPlugin pl) { this.pl = pl; }

    static String num(long n) {
        return NumberFormat.getIntegerInstance(Locale.US).format(n);
    }

    private static ItemStack named(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(T.c(name));
        if (lore.length > 0) meta.lore(T.lore(lore));
        it.setItemMeta(meta);
        return it;
    }

    private static String plainName(ItemStack it) {
        ItemMeta m = it.getItemMeta();
        if (m != null && m.hasDisplayName()) {
            return PlainTextComponentSerializer.plainText().serialize(m.displayName());
        }
        return it.getType().name().toLowerCase(Locale.ROOT).replace('_', ' ');
    }

    private void fillBottom(Inventory inv) {
        ItemStack glass = named(Material.GRAY_STAINED_GLASS_PANE, " ");
        for (int i = 45; i < 54; i++) inv.setItem(i, glass);
    }

    // ---------------------------------------------------------------- main shop

    void openShop(Player p) {
        Holder h = new Holder(Type.SHOP);
        Inventory inv = Bukkit.createInventory(h, 54, T.c("&fShard " + T.BLUE + "Shop"));
        h.inv = inv;
        fillBottom(inv);

        int slot = 0;
        for (ItemStore.Entry e : pl.store.all()) {
            if (slot >= 45) break;
            inv.setItem(slot, display(e));
            h.bySlot.put(slot, e);
            slot++;
        }

        inv.setItem(SLOT_RANKS, ranksButton(p));
        inv.setItem(SLOT_SHARDS, shardsButton(p));
        inv.setItem(SLOT_HISTORY, named(Material.CLOCK, T.BLUE + "Buy History",
                "&fCheck your transaction history in shard shop"));
        p.openInventory(inv);
    }

    private ItemStack display(ItemStore.Entry e) {
        List<String> extra = new ArrayList<>();
        extra.add("");
        extra.add(T.BLUE + "Price: &f" + num(e.price) + " shards");
        if (e.expireHours > 0) extra.add("&7Self destructs in " + e.expireHours + "h");
        extra.add("");
        extra.add("&fClick to buy");
        return Items.addLore(e.item, extra);
    }

    private ItemStack shardsButton(Player p) {
        return named(Material.AMETHYST_SHARD, T.BLUE + "Shards",
                T.BLUE + "Shards: &f" + num(pl.shards.get(p.getUniqueId())));
    }

    private int rankOf(UUID id) {
        long mine = pl.shards.get(id);
        int better = 0;
        for (Map.Entry<UUID, Long> en : pl.shards.all().entrySet()) {
            if (!en.getKey().equals(id) && en.getValue() > mine) better++;
        }
        return better + 1;
    }

    private ItemStack ranksButton(Player p) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta sm = (SkullMeta) head.getItemMeta();
        sm.setOwningPlayer(p);
        sm.displayName(T.c(T.BLUE + "Ranks"));
        sm.lore(T.lore(T.BLUE + "Your rank: &f#" + rankOf(p.getUniqueId()), "", "&fClick to view the leaderboard"));
        head.setItemMeta(sm);
        return head;
    }

    // ------------------------------------------------------------------- ranks

    void openRanks(Player p) {
        Holder h = new Holder(Type.RANKS);
        Inventory inv = Bukkit.createInventory(h, 54, T.c(T.BLUE + "Shard " + "&fRanks"));
        h.inv = inv;
        fillBottom(inv);

        List<Map.Entry<UUID, Long>> list = new ArrayList<>();
        for (Map.Entry<UUID, Long> en : pl.shards.all().entrySet()) {
            if (en.getValue() > 0) list.add(en);
        }
        list.sort((a, b) -> Long.compare(b.getValue(), a.getValue()));

        for (int i = 0; i < Math.min(45, list.size()); i++) {
            UUID id = list.get(i).getKey();
            org.bukkit.OfflinePlayer op = Bukkit.getOfflinePlayer(id);
            String name = op.getName() == null ? "Unknown" : op.getName();
            ItemStack head = new ItemStack(Material.PLAYER_HEAD);
            SkullMeta sm = (SkullMeta) head.getItemMeta();
            sm.setOwningPlayer(op);
            sm.displayName(T.c(T.BLUE + "#" + (i + 1) + " &f" + name));
            sm.lore(T.lore(T.BLUE + "Shards: &f" + num(list.get(i).getValue())));
            head.setItemMeta(sm);
            inv.setItem(i, head);
        }
        if (list.isEmpty()) {
            inv.setItem(22, named(Material.BARRIER, "&fNo one has any shards yet"));
        }
        inv.setItem(SLOT_BACK, named(Material.ARROW, "&fBack"));
        p.openInventory(inv);
    }

    // ----------------------------------------------------------------- history

    void openHistory(Player p) {
        Holder h = new Holder(Type.HISTORY);
        Inventory inv = Bukkit.createInventory(h, 54, T.c("&fBuy " + T.BLUE + "History"));
        h.inv = inv;
        fillBottom(inv);

        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm");
        List<History.Rec> recs = pl.history.get(p.getUniqueId());
        for (int i = 0; i < Math.min(45, recs.size()); i++) {
            History.Rec r = recs.get(i);
            inv.setItem(i, named(Material.PAPER, T.BLUE + r.item(),
                    "&fPrice: " + T.BLUE + num(r.price()) + " shards",
                    "&f" + df.format(new Date(r.time()))));
        }
        if (recs.isEmpty()) {
            inv.setItem(22, named(Material.BARRIER, "&fNo purchases yet"));
        }
        inv.setItem(SLOT_BACK, named(Material.ARROW, "&fBack"));
        p.openInventory(inv);
    }

    // ------------------------------------------------------------------ events

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getView().getTopInventory().getHolder() instanceof Holder h)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (e.getClickedInventory() == null || e.getClickedInventory() != e.getView().getTopInventory()) return;

        int slot = e.getRawSlot();
        switch (h.type) {
            case SHOP -> {
                if (slot == SLOT_RANKS) openRanks(p);
                else if (slot == SLOT_HISTORY) openHistory(p);
                else if (slot == SLOT_SHARDS) { /* not clickable */ }
                else if (slot >= 0 && slot < 45) {
                    ItemStore.Entry en = h.bySlot.get(slot);
                    if (en != null) buy(p, h, en);
                }
            }
            case RANKS, HISTORY -> {
                if (slot == SLOT_BACK) openShop(p);
            }
        }
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        if (e.getView().getTopInventory().getHolder() instanceof Holder) e.setCancelled(true);
    }

    private void buy(Player p, Holder h, ItemStore.Entry en) {
        if (p.getInventory().firstEmpty() == -1) {
            p.sendMessage(T.c("&cYour inventory is full."));
            return;
        }
        UUID id = p.getUniqueId();
        long have = pl.shards.get(id);
        if (have < en.price) {
            p.sendMessage(T.c("&cYou don't have enough shards. " + T.BLUE + "Need: &f" + num(en.price)
                    + " " + T.BLUE + "You have: &f" + num(have)));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        if (!pl.shards.take(id, en.price)) return;

        ItemStack give = en.item.clone();
        if (en.expireHours > 0) {
            long at = System.currentTimeMillis() + en.expireHours * 3_600_000L;
            give = Items.setExpires(give, at);
            give = Items.addLore(give, List.of("", "&cSelf destructs in " + en.expireHours + "h",
                    "&7Expires: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date(at))));
        }
        p.getInventory().addItem(give);
        String name = plainName(en.item);
        pl.history.add(id, name, en.price);
        p.playSound(p.getLocation(), Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1f, 1f);
        p.sendMessage(T.c("&fBought " + T.BLUE + name + " &ffor " + T.BLUE + num(en.price) + " &fshards."));

        // refresh the shards + ranks buttons
        h.inv.setItem(SLOT_SHARDS, shardsButton(p));
        h.inv.setItem(SLOT_RANKS, ranksButton(p));
    }
}
