package pl.skrzynki;

import org.bukkit.inventory.ItemStack;

public class Reward {
    public ItemStack item;
    public double chance; // procent (suma w skrzynce = 100)

    public Reward(ItemStack item, double chance) {
        this.item = item;
        this.chance = chance;
    }
}
