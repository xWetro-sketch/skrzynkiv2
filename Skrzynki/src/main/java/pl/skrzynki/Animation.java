package pl.skrzynki;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.ArrayList;
import java.util.List;

/** Animacja otwierania - przewijana "ruletka" z nagrodami. */
public class Animation extends BukkitRunnable implements InventoryHolder {
    private static final int N = 40;
    private static final Material[] PANES = {
            Material.PURPLE_STAINED_GLASS_PANE, Material.MAGENTA_STAINED_GLASS_PANE,
            Material.PINK_STAINED_GLASS_PANE, Material.ORANGE_STAINED_GLASS_PANE,
            Material.YELLOW_STAINED_GLASS_PANE, Material.LIGHT_BLUE_STAINED_GLASS_PANE
    };

    private final Skrzynki plugin;
    private final Player p;
    private final Crate crate;
    private final Reward win;
    private final Location loc;
    private final Inventory inv;
    private final List<Reward> strip = new ArrayList<>();
    private int offset = 0, tick = 0, next = 0, hold = 50;
    private boolean revealed = false, given = false;

    public Animation(Skrzynki plugin, Player p, Crate crate, Reward win, Location loc) {
        this.plugin = plugin;
        this.p = p;
        this.crate = crate;
        this.win = win;
        this.loc = loc;
        this.inv = Bukkit.createInventory(this, 27, Util.c("<gradient:#ffd700:#ff8c00><bold>ꜱᴋʀᴢʏɴᴋᴀ</bold></gradient>"));
        for (int i = 0; i < N; i++) strip.add(i == N - 5 ? win : crate.roll(plugin.random));
        inv.setItem(4, Util.item(Material.END_ROD, "<gold>▼"));
        inv.setItem(22, Util.item(Material.END_ROD, "<gold>▲"));
        draw();
        plugin.manager.animating.add(p.getUniqueId());
        p.openInventory(inv);
        p.playSound(p.getLocation(), Sound.BLOCK_CHEST_OPEN, 1f, 1f);
        runTaskTimer(plugin, 1L, 1L);
    }

    @Override
    public Inventory getInventory() {
        return inv;
    }

    private int delay(int off) {
        if (off < 12) return 1;
        if (off < 20) return 2;
        if (off < 26) return 3;
        if (off < 29) return 5;
        if (off < 31) return 7;
        return 10;
    }

    private void draw() {
        for (int i = 0; i < 9; i++) inv.setItem(9 + i, Gui.display(strip.get(offset + i)));
        for (int s = 0; s < 27; s++) {
            if ((s >= 9 && s <= 17) || s == 4 || s == 22) continue;
            inv.setItem(s, Util.filler(PANES[(s + tick / 2) % PANES.length]));
        }
    }

    @Override
    public void run() {
        if (!p.isOnline()) {
            finishNow();
            return;
        }
        tick++;
        if (revealed) {
            if (tick % 4 == 0) {
                Location l = p.getLocation().add(0, 1, 0);
                p.getWorld().spawnParticle(Particle.END_ROD, l, 6, 0.6, 0.6, 0.6, 0.02);
            }
            if (--hold <= 0) {
                cancel();
                p.closeInventory();
                plugin.manager.animating.remove(p.getUniqueId());
            }
            return;
        }
        if (tick < next) return;
        if (offset < N - 9) {
            offset++;
            draw();
            p.playSound(p.getLocation(), Sound.BLOCK_NOTE_BLOCK_HAT, 0.6f, 0.8f + offset * 0.03f);
            next = tick + delay(offset);
        } else {
            reveal();
        }
    }

    private void reveal() {
        revealed = true;
        for (int s = 0; s < 27; s++) {
            if (s == 13 || s == 22) continue;
            inv.setItem(s, Util.filler(s >= 9 && s <= 17 ? Material.BLACK_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS_PANE));
        }
        inv.setItem(13, Gui.display(win));
        inv.setItem(22, Util.item(Material.NAME_TAG, Util.TITLE,
                "",
                "<white>" + plain(),
                "<gray>Szansa na drop: <yellow>" + Util.pct(win.chance)));
        give();
    }

    private String plain() {
        var m = win.item.getItemMeta();
        String name = (m != null && m.hasDisplayName())
                ? net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer.plainText().serialize(m.displayName())
                : win.item.getType().name().toLowerCase().replace('_', ' ');
        return name + (win.item.getAmount() > 1 ? " x" + win.item.getAmount() : "");
    }

    private void give() {
        if (given) return;
        given = true;
        Gui.giveReward(p, crate, win, loc);
    }

    /** Wywoływane gdy gracz zamknie okno w trakcie animacji - nagroda i tak jest wydana. */
    public void finishNow() {
        give();
        try { cancel(); } catch (IllegalStateException ignored) {}
        plugin.manager.animating.remove(p.getUniqueId());
    }
}
