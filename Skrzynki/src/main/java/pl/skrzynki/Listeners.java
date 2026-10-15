package pl.skrzynki;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.Listener;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;

public class Listeners implements Listener {
    private final Skrzynki plugin;

    public Listeners(Skrzynki plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        if (e.getHand() != EquipmentSlot.HAND) return;
        Block b = e.getClickedBlock();
        if (b == null) return;
        Action a = e.getAction();
        if (a != Action.RIGHT_CLICK_BLOCK && a != Action.LEFT_CLICK_BLOCK) return;
        Crate c = plugin.manager.at(b);
        if (c == null) return;
        e.setCancelled(true);
        Player p = e.getPlayer();
        if (a == Action.LEFT_CLICK_BLOCK) Gui.preview(p, c);
        else plugin.tryOpen(p, c, b.getLocation());
    }

    @EventHandler
    public void onBreak(BlockBreakEvent e) {
        if (plugin.manager.at(e.getBlock()) != null) {
            e.setCancelled(true);
            Util.msg(e.getPlayer(), "<red>To jest skrzynka! Usuń ją komendą <yellow>/skrzynka zdejmij</yellow>.");
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        Inventory top = e.getView().getTopInventory();
        InventoryHolder h = top.getHolder();
        if (h instanceof Animation || h instanceof Gui.PreviewHolder) {
            e.setCancelled(true);
            return;
        }
        if (!(h instanceof Gui.EditorHolder eh)) return;
        e.setCancelled(true);
        Player p = (Player) e.getWhoClicked();
        if (!p.hasPermission("skrzynki.admin")) return;
        Inventory clicked = e.getClickedInventory();
        if (clicked == null) return;
        Crate c = eh.crate;

        if (clicked.equals(top)) {
            int slot = e.getRawSlot();
            if (slot >= 45 || slot >= c.rewards.size()) return;
            Reward r = c.rewards.get(slot);
            switch (e.getClick()) {
                case LEFT -> c.setChance(slot, r.chance + 1);
                case RIGHT -> c.setChance(slot, r.chance - 1);
                case SHIFT_LEFT -> c.setChance(slot, r.chance + 10);
                case SHIFT_RIGHT -> c.setChance(slot, r.chance - 10);
                case DROP, CONTROL_DROP -> c.removeReward(slot);
                case SWAP_OFFHAND -> {
                    p.getInventory().addItem(r.item.clone());
                    Util.msg(p, "<green>Dodano drop do Twojego ekwipunku.");
                }
                default -> { return; }
            }
        } else {
            ItemStack it = e.getCurrentItem();
            if (it == null || it.getType().isAir()) return;
            if (c.rewards.size() >= 45) {
                Util.msg(p, "<red>Maksymalnie 45 nagród w skrzynce.");
                return;
            }
            c.addReward(it.clone(), 10);
        }
        plugin.manager.save();
        Gui.refreshEditor(eh);
    }

    @EventHandler
    public void onDrag(InventoryDragEvent e) {
        InventoryHolder h = e.getView().getTopInventory().getHolder();
        if (h instanceof Animation || h instanceof Gui.PreviewHolder || h instanceof Gui.EditorHolder) e.setCancelled(true);
    }

    @EventHandler
    public void onClose(InventoryCloseEvent e) {
        if (e.getInventory().getHolder() instanceof Animation a) a.finishNow();
    }
}
