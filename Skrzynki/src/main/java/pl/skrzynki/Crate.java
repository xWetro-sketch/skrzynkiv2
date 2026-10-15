package pl.skrzynki;

import org.bukkit.inventory.ItemStack;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Crate {
    public final String id;
    public String name;
    public String key = "brak"; // rzadki / epicki / brak
    public final List<Reward> rewards = new ArrayList<>();

    public Crate(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public Reward roll(Random random) {
        double total = 0;
        for (Reward r : rewards) total += r.chance;
        double x = random.nextDouble() * total;
        for (Reward r : rewards) {
            x -= r.chance;
            if (x <= 0) return r;
        }
        return rewards.get(rewards.size() - 1);
    }

    /** Ustawia szanse nagrody (w %), reszta jest automatycznie przeliczana do 100%. */
    public void setChance(int idx, double p) {
        if (rewards.size() == 1) {
            rewards.get(0).chance = 100;
            return;
        }
        p = Math.max(0.01, Math.min(99.0, p));
        Reward r = rewards.get(idx);
        double others = 0;
        for (Reward o : rewards) if (o != r) others += o.chance;
        for (Reward o : rewards) {
            if (o == r) continue;
            o.chance = others <= 0 ? (100 - p) / (rewards.size() - 1) : o.chance * ((100 - p) / others);
        }
        r.chance = p;
    }

    public void addReward(ItemStack item, double p) {
        if (rewards.isEmpty()) {
            rewards.add(new Reward(item, 100));
            return;
        }
        rewards.add(new Reward(item, 1));
        setChance(rewards.size() - 1, p);
    }

    public void removeReward(int idx) {
        rewards.remove(idx);
        double sum = 0;
        for (Reward r : rewards) sum += r.chance;
        if (sum > 0) for (Reward r : rewards) r.chance = r.chance * 100 / sum;
    }
}
