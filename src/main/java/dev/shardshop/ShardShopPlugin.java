package dev.shardshop;

import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class ShardShopPlugin extends JavaPlugin implements CommandExecutor, TabCompleter {
    Shards shards;
    ItemStore store;
    History history;
    ShopGui gui;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        try {
            shards = new Shards();
        } catch (Exception e) {
            getLogger().severe("Could not hook into ShopPlugin's shard system: " + e);
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        Items.init(this);
        store = new ItemStore(this);
        store.load();
        history = new History(this);
        gui = new ShopGui(this);

        getServer().getPluginManager().registerEvents(gui, this);
        getServer().getPluginManager().registerEvents(new ToolListener(this), this);
        getCommand("shardshop").setExecutor(this);
        getCommand("shardshop").setTabCompleter(this);
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) {
            if (!(s instanceof Player p)) {
                s.sendMessage("Players only.");
                return true;
            }
            if (!p.hasPermission("shardshop.use")) {
                p.sendMessage(T.c("&cYou don't have permission."));
                return true;
            }
            gui.openShop(p);
            return true;
        }

        if (!s.hasPermission("shardshop.admin")) {
            s.sendMessage(T.c("&cYou don't have permission."));
            return true;
        }

        switch (a[0].toLowerCase(Locale.ROOT)) {
            case "additem" -> {
                if (!(s instanceof Player p)) {
                    s.sendMessage("Players only.");
                    return true;
                }
                if (a.length < 2) {
                    s.sendMessage(T.c("&cUsage: /shardshop additem <price> [expireHours] [pickaxe|axe|shovel]"));
                    return true;
                }
                ItemStack hand = p.getInventory().getItemInMainHand();
                if (hand.getType() == Material.AIR) {
                    s.sendMessage(T.c("&cHold the item you want to add in your main hand."));
                    return true;
                }
                long price;
                int hours = 0;
                try {
                    price = Long.parseLong(a[1]);
                    if (a.length >= 3) hours = Integer.parseInt(a[2]);
                } catch (NumberFormatException ex) {
                    s.sendMessage(T.c("&cInvalid number."));
                    return true;
                }
                if (price < 0 || hours < 0) {
                    s.sendMessage(T.c("&cValues must be 0 or more."));
                    return true;
                }
                ItemStack item = hand.clone();
                if (a.length >= 4) {
                    String mode = a[3].toLowerCase(Locale.ROOT);
                    if (!List.of(Items.PICKAXE, Items.AXE, Items.SHOVEL).contains(mode)) {
                        s.sendMessage(T.c("&cMode must be pickaxe, axe or shovel."));
                        return true;
                    }
                    item = Items.setMode(item, mode);
                    item = Items.addLore(item, Items.modeLore(mode));
                }
                int id = store.add(price, hours, item);
                s.sendMessage(T.c("&fAdded item " + T.BLUE + "#" + id + " &ffor " + T.BLUE
                        + ShopGui.num(price) + " &fshards."));
            }
            case "removeitem" -> {
                if (a.length < 2) {
                    s.sendMessage(T.c("&cUsage: /shardshop removeitem <id>"));
                    return true;
                }
                try {
                    boolean ok = store.remove(Integer.parseInt(a[1]));
                    s.sendMessage(T.c(ok ? "&fItem removed." : "&cNo item with that id."));
                } catch (NumberFormatException ex) {
                    s.sendMessage(T.c("&cInvalid id."));
                }
            }
            case "list" -> {
                s.sendMessage(T.c(T.BLUE + "Shard Shop items:"));
                for (ItemStore.Entry e : store.all()) {
                    s.sendMessage(T.c("&f#" + e.id + " " + e.item.getType() + " - " + T.BLUE
                            + ShopGui.num(e.price) + " &fshards" + (e.expireHours > 0 ? " (" + e.expireHours + "h)" : "")));
                }
            }
            case "reload" -> {
                reloadConfig();
                store.load();
                s.sendMessage(T.c("&fShardShop reloaded."));
            }
            default -> s.sendMessage(T.c("&cUsage: /shardshop [additem|removeitem|list|reload]"));
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (!s.hasPermission("shardshop.admin")) return out;
        if (a.length == 1) {
            for (String o : List.of("additem", "removeitem", "list", "reload"))
                if (o.startsWith(a[0].toLowerCase(Locale.ROOT))) out.add(o);
        } else if (a.length == 4 && a[0].equalsIgnoreCase("additem")) {
            for (String o : List.of("pickaxe", "axe", "shovel"))
                if (o.startsWith(a[3].toLowerCase(Locale.ROOT))) out.add(o);
        } else if (a.length == 2 && a[0].equalsIgnoreCase("removeitem")) {
            for (ItemStore.Entry e : store.all()) out.add(String.valueOf(e.id));
        }
        return out;
    }
}
