package pl.skrzynki;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.inventory.ItemStack;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class CrateManager {
    private final Skrzynki plugin;
    private final File file;

    public final Map<String, Crate> crates = new LinkedHashMap<>();
    public final Map<String, String> locations = new HashMap<>();
    public final Set<UUID> noAnim = new HashSet<>();
    public final Set<UUID> animating = new HashSet<>();
    public final Map<UUID, String> forced = new HashMap<>(); // "id:index"

    public CrateManager(Skrzynki plugin) {
        this.plugin = plugin;
        this.file = new File(plugin.getDataFolder(), "crates.yml");
    }

    public void load() {
        crates.clear();
        locations.clear();
        noAnim.clear();
        if (!file.exists()) {
            defaults();
            save();
            return;
        }
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(file);
        ConfigurationSection cs = cfg.getConfigurationSection("crates");
        if (cs != null) {
            for (String id : cs.getKeys(false)) {
                ConfigurationSection s = cs.getConfigurationSection(id);
                Crate c = new Crate(id, s.getString("name", id));
                c.key = s.getString("key", "brak");
                ConfigurationSection rs = s.getConfigurationSection("rewards");
                if (rs != null) {
                    List<String> keys = new ArrayList<>(rs.getKeys(false));
                    keys.sort(Comparator.comparingInt(Integer::parseInt));
                    for (String k : keys) {
                        ItemStack it = rs.getItemStack(k + ".item");
                        if (it == null) continue;
                        c.rewards.add(new Reward(it, rs.getDouble(k + ".chance", 1)));
                    }
                }
                crates.put(id, c);
            }
        }
        for (String e : cfg.getStringList("locations")) {
            String[] p = e.split(";");
            if (p.length == 5) locations.put(p[0] + ";" + p[1] + ";" + p[2] + ";" + p[3], p[4]);
        }
        for (String u : cfg.getStringList("noanim")) {
            try { noAnim.add(UUID.fromString(u)); } catch (IllegalArgumentException ignored) {}
        }
    }

    public void save() {
        YamlConfiguration cfg = new YamlConfiguration();
        for (Crate c : crates.values()) {
            String base = "crates." + c.id;
            cfg.set(base + ".name", c.name);
            cfg.set(base + ".key", c.key);
            for (int i = 0; i < c.rewards.size(); i++) {
                cfg.set(base + ".rewards." + i + ".item", c.rewards.get(i).item);
                cfg.set(base + ".rewards." + i + ".chance", c.rewards.get(i).chance);
            }
        }
        List<String> locs = new ArrayList<>();
        for (Map.Entry<String, String> e : locations.entrySet()) locs.add(e.getKey() + ";" + e.getValue());
        cfg.set("locations", locs);
        List<String> na = new ArrayList<>();
        for (UUID u : noAnim) na.add(u.toString());
        cfg.set("noanim", na);
        try {
            plugin.getDataFolder().mkdirs();
            cfg.save(file);
        } catch (IOException ex) {
            plugin.getLogger().severe("Nie udalo sie zapisac crates.yml: " + ex.getMessage());
        }
    }

    private void defaults() {
        Crate r = new Crate("rzadka", "<gradient:#4aa3ff:#7b61ff><bold>Skrzynka Rzadka</bold></gradient>");
        r.key = "rzadki";
        r.rewards.add(new Reward(new ItemStack(Material.IRON_INGOT, 16), 40));
        r.rewards.add(new Reward(new ItemStack(Material.GOLD_INGOT, 8), 30));
        r.rewards.add(new Reward(new ItemStack(Material.DIAMOND, 2), 20));
        r.rewards.add(new Reward(new ItemStack(Material.EMERALD, 4), 8));
        r.rewards.add(new Reward(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 1), 2));
        crates.put(r.id, r);

        Crate e = new Crate("epicka", "<gradient:#ff5fd2:#ffb347><bold>Skrzynka Epicka</bold></gradient>");
        e.key = "epicki";
        e.rewards.add(new Reward(new ItemStack(Material.DIAMOND, 8), 40));
        e.rewards.add(new Reward(new ItemStack(Material.NETHERITE_INGOT, 1), 25));
        e.rewards.add(new Reward(new ItemStack(Material.ENCHANTED_GOLDEN_APPLE, 2), 20));
        e.rewards.add(new Reward(new ItemStack(Material.TOTEM_OF_UNDYING, 1), 10));
        e.rewards.add(new Reward(new ItemStack(Material.NETHER_STAR, 1), 5));
        crates.put(e.id, e);
    }

    public static String keyOf(Block b) {
        return b.getWorld().getName() + ";" + b.getX() + ";" + b.getY() + ";" + b.getZ();
    }

    public Crate at(Block b) {
        String id = locations.get(keyOf(b));
        return id == null ? null : crates.get(id);
    }

    public Location parse(String key) {
        String[] p = key.split(";");
        if (p.length != 4) return null;
        var w = Bukkit.getWorld(p[0]);
        if (w == null) return null;
        try {
            return new Location(w, Integer.parseInt(p[1]), Integer.parseInt(p[2]), Integer.parseInt(p[3]));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public boolean useAnimation(UUID u) {
        return !noAnim.contains(u);
    }

    public boolean toggleAnim(UUID u) {
        if (noAnim.remove(u)) { save(); return true; }
        noAnim.add(u);
        save();
        return false;
    }

    public Reward pollForced(UUID u, Crate c) {
        String f = forced.get(u);
        if (f == null) return null;
        String[] p = f.split(":");
        if (!p[0].equals(c.id)) return null;
        forced.remove(u);
        try {
            int i = Integer.parseInt(p[1]);
            return i >= 0 && i < c.rewards.size() ? c.rewards.get(i) : null;
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
