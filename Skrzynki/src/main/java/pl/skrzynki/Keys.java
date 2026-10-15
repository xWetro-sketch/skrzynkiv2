package pl.skrzynki;

import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class Keys {
    private static NamespacedKey KEY;

    public static void init(JavaPlugin plugin) {
        KEY = new NamespacedKey(plugin, "klucz");
    }

    public static boolean valid(String type) {
        return type.equals("rzadki") || type.equals("epicki");
    }

    public static String display(String type) {
        return switch (type) {
            case "rzadki" -> "<gradient:#4aa3ff:#7b61ff><bold>Klucz Rzadki</bold></gradient>";
            case "epicki" -> "<gradient:#ff5fd2:#ffb347><bold>Klucz Epicki</bold></gradient>";
            default -> "<gray>Brak klucza";
        };
    }

    public static ItemStack create(String type, int amount) {
        Material m = type.equals("epicki") ? Material.NETHER_STAR : Material.TRIPWIRE_HOOK;
        ItemStack it = new ItemStack(m, amount);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Util.c(display(type)));
        meta.lore(List.of(
                Util.c("<gray>Użyj na skrzynce, aby ją otworzyć."),
                Util.c(""),
                Util.c("<dark_gray>▪ <gray>Typ: " + (type.equals("epicki") ? "<light_purple>Epicki" : "<aqua>Rzadki"))));
        meta.setEnchantmentGlintOverride(true);
        meta.getPersistentDataContainer().set(KEY, PersistentDataType.STRING, type);
        it.setItemMeta(meta);
        return it;
    }

    public static String typeOf(ItemStack it) {
        if (it == null || !it.hasItemMeta()) return null;
        return it.getItemMeta().getPersistentDataContainer().get(KEY, PersistentDataType.STRING);
    }

    public static boolean take(Player p, String type) {
        ItemStack[] contents = p.getInventory().getContents();
        for (int i = 0; i < contents.length; i++) {
            ItemStack it = contents[i];
            if (it != null && type.equals(typeOf(it))) {
                if (it.getAmount() <= 1) p.getInventory().setItem(i, null);
                else it.setAmount(it.getAmount() - 1);
                return true;
            }
        }
        return false;
    }
}
