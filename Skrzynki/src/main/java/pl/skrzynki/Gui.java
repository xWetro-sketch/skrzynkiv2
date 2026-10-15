package pl.skrzynki;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public final class Gui {

    public static class PreviewHolder implements InventoryHolder {
        public final Crate crate;
        public Inventory inv;
        public PreviewHolder(Crate c) { this.crate = c; }
        @Override public Inventory getInventory() { return inv; }
    }

    public static class EditorHolder implements InventoryHolder {
        public final Crate crate;
        public Inventory inv;
        public EditorHolder(Crate c) { this.crate = c; }
        @Override public Inventory getInventory() { return inv; }
    }

    private static String colorFor(double chance) {
        if (chance <= 2) return "<light_purple>";
        if (chance <= 10) return "<gold>";
        if (chance <= 25) return "<aqua>";
        return "<green>";
    }

    private static String tierFor(double chance) {
        if (chance <= 2) return "<light_purple><bold>LEGENDARNY</bold>";
        if (chance <= 10) return "<gold><bold>EPICKI</bold>";
        if (chance <= 25) return "<aqua><bold>RZADKI</bold>";
        return "<green><bold>ZWYKŁY</bold>";
    }

    /** Przedmiot nagrody z widoczną szansą na drop w opisie. */
    public static ItemStack display(Reward r, String... extra) {
        ItemStack it = r.item.clone();
        ItemMeta m = it.getItemMeta();
        List<Component> lore = m.hasLore() ? new ArrayList<>(m.lore()) : new ArrayList<>();
        lore.add(Component.empty());
        lore.add(Util.c("<dark_gray>▪ <gray>Szansa na drop: " + colorFor(r.chance) + "<bold>" + Util.pct(r.chance)));
        lore.add(Util.c("<dark_gray>▪ <gray>Ranga: " + tierFor(r.chance)));
        if (extra.length > 0) {
            lore.add(Component.empty());
            for (String e : extra) lore.add(Util.c(e));
        }
        m.lore(lore);
        it.setItemMeta(m);
        return it;
    }

    public static void preview(Player p, Crate c) {
        int n = c.rewards.size();
        int rows = Math.min(6, Math.max(2, (n + 8) / 9 + 1));
        PreviewHolder h = new PreviewHolder(c);
        Inventory inv = Bukkit.createInventory(h, rows * 9, Util.c("<gold><bold>Podgląd</bold> <dark_gray>» <white>" + c.name));
        h.inv = inv;
        int size = rows * 9;
        for (int i = size - 9; i < size; i++) inv.setItem(i, Util.filler(Material.GRAY_STAINED_GLASS_PANE));
        for (int i = 0; i < n && i < size - 9; i++) inv.setItem(i, display(c.rewards.get(i)));
        inv.setItem(size - 5, Util.item(Material.TRIPWIRE_HOOK, "<gold><bold>Informacje</bold>",
                "<gray>Wymagany klucz: " + Keys.display(c.key),
                "<gray>Liczba nagród: <yellow>" + n,
                "",
                "<dark_gray>Kliknij PPM na skrzynkę, aby otworzyć."));
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 0.6f, 1.2f);
    }

    public static void editor(Player p, Crate c) {
        EditorHolder h = new EditorHolder(c);
        Inventory inv = Bukkit.createInventory(h, 54, Util.c("<dark_red><bold>Kreator</bold> <dark_gray>» <white>" + c.id));
        h.inv = inv;
        refreshEditor(h);
        p.openInventory(inv);
    }

    public static void refreshEditor(EditorHolder h) {
        Inventory inv = h.inv;
        inv.clear();
        Crate c = h.crate;
        for (int i = 45; i < 54; i++) inv.setItem(i, Util.filler(Material.GRAY_STAINED_GLASS_PANE));
        for (int i = 0; i < c.rewards.size() && i < 45; i++) {
            inv.setItem(i, display(c.rewards.get(i),
                    "<yellow>LPM <gray>+1%  <dark_gray>| <yellow>PPM <gray>-1%",
                    "<yellow>Shift+LPM <gray>+10%  <dark_gray>| <yellow>Shift+PPM <gray>-10%",
                    "<yellow>Q <gray>usuń  <dark_gray>| <yellow>F <gray>weź ten drop do ekwipunku"));
        }
        inv.setItem(49, Util.item(Material.WRITABLE_BOOK, "<gold><bold>Kreator skrzynki</bold>",
                "<gray>Kliknij dowolny przedmiot ze swojego",
                "<gray>ekwipunku, aby dodać go jako nagrodę.",
                "",
                "<gray>Klucz: " + Keys.display(c.key),
                "<gray>Szanse zawsze sumują się do <yellow>100%<gray>."));
    }

    /** Wydaje nagrodę graczowi + efekty + napis TWOJA NAGRODA. */
    public static void giveReward(Player p, Crate c, Reward r, Location crateLoc) {
        p.getInventory().addItem(r.item.clone()).values()
                .forEach(i -> p.getWorld().dropItemNaturally(p.getLocation(), i));

        Component sub = Component.empty().append(Util.nameOf(r.item))
                .append(Util.c(" <dark_gray>• <gray>Szansa: <yellow>" + Util.pct(r.chance)));
        p.showTitle(Title.title(Util.c(Util.TITLE), sub,
                Title.Times.times(Duration.ofMillis(200), Duration.ofMillis(2600), Duration.ofMillis(600))));

        p.sendMessage(Util.MM.deserialize(Util.PREFIX + "Twoja nagroda: ")
                .append(Util.nameOf(r.item))
                .append(Util.c(" <dark_gray>(<yellow>" + Util.pct(r.chance) + "<dark_gray>)")));

        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 0.8f, 1f);
        if (crateLoc != null && crateLoc.getWorld() != null) {
            Location l = crateLoc.clone().add(0.5, 1.2, 0.5);
            l.getWorld().spawnParticle(Particle.END_ROD, l, 50, 0.4, 0.4, 0.4, 0.12);
            l.getWorld().spawnParticle(Particle.FLAME, l, 25, 0.3, 0.3, 0.3, 0.05);
        }
    }
}
