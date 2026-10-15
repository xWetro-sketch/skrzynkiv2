package pl.skrzynki;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.block.Block;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.*;
import java.util.stream.Collectors;

public class Skrzynki extends JavaPlugin implements TabExecutor {
    public CrateManager manager;
    public final Random random = new Random();

    private static final List<String> SUBS = List.of("animacja", "podglad", "stworz", "usunskrzynke", "nazwa", "lista",
            "edytuj", "dodaj", "szansa", "usun", "klucz", "dajdrop", "wymus", "ustaw", "zdejmij", "typklucza", "reload");

    @Override
    public void onEnable() {
        Keys.init(this);
        manager = new CrateManager(this);
        manager.load();
        Bukkit.getPluginManager().registerEvents(new Listeners(this), this);
        getCommand("skrzynka").setExecutor(this);
        getCommand("skrzynka").setTabCompleter(this);

        // ładne cząsteczki nad skrzynkami
        Bukkit.getScheduler().runTaskTimer(this, () -> {
            for (String k : manager.locations.keySet()) {
                Location l = manager.parse(k);
                if (l == null || !l.isChunkLoaded()) continue;
                l.getWorld().spawnParticle(Particle.END_ROD, l.clone().add(0.5, 1.1, 0.5), 2, 0.25, 0.1, 0.25, 0.01);
                l.getWorld().spawnParticle(Particle.ENCHANT, l.clone().add(0.5, 1.0, 0.5), 6, 0.4, 0.4, 0.4, 0.5);
            }
        }, 20L, 10L);
    }

    @Override
    public void onDisable() {
        for (Player pl : Bukkit.getOnlinePlayers()) {
            if (pl.getOpenInventory().getTopInventory().getHolder() instanceof Animation) pl.closeInventory();
        }
        manager.save();
    }

    public void tryOpen(Player p, Crate c, Location loc) {
        if (manager.animating.contains(p.getUniqueId())) return;
        if (c.rewards.isEmpty()) {
            Util.msg(p, "<red>Ta skrzynka jest pusta.");
            return;
        }
        if (!c.key.equals("brak") && !Keys.take(p, c.key)) {
            Util.msg(p, "<red>Potrzebujesz klucza: " + Keys.display(c.key));
            p.playSound(p.getLocation(), Sound.ENTITY_VILLAGER_NO, 1f, 1f);
            return;
        }
        Reward win = manager.pollForced(p.getUniqueId(), c);
        if (win == null) win = c.roll(random);
        if (manager.useAnimation(p.getUniqueId())) new Animation(this, p, c, win, loc);
        else Gui.giveReward(p, c, win, loc);
    }

    private void help(CommandSender s) {
        Util.msg(s, "<gold>Komendy <gray>(gracz):");
        s.sendMessage(Util.MM.deserialize("<yellow>/skrzynka animacja <gray>- włącz/wyłącz animację"));
        s.sendMessage(Util.MM.deserialize("<yellow>/skrzynka podglad <id> <gray>- podgląd nagród"));
        if (!s.hasPermission("skrzynki.admin")) return;
        s.sendMessage(Util.MM.deserialize("<gold>Admin:"));
        for (String l : new String[]{
                "stworz <id> [nazwa] - nowa skrzynka (kreator)",
                "edytuj <id> - GUI kreatora (szanse, dodawanie, usuwanie)",
                "dodaj <id> <szansa%> - dodaj item z ręki jako drop",
                "szansa <id> <nr> <%> - ustaw szansę dropu",
                "usun <id> <nr> - usuń drop",
                "nazwa <id> <nazwa> - zmień nazwę (MiniMessage)",
                "typklucza <id> <rzadki|epicki|brak>",
                "ustaw <id> - patrzony blok staje się skrzynką",
                "zdejmij - patrzony blok przestaje być skrzynką",
                "klucz <gracz> <rzadki|epicki> [ilość]",
                "dajdrop <id> <nr> - dostajesz wybrany drop",
                "wymus <id> <nr|off> - Twoje następne otwarcie da ten drop",
                "usunskrzynke <id>, lista, reload"}) {
            s.sendMessage(Util.MM.deserialize("<yellow>/skrzynka <gray>" + l.replace("<", "\\<")));
        }
    }

    private Crate crate(CommandSender s, String id) {
        Crate c = manager.crates.get(id.toLowerCase());
        if (c == null) Util.msg(s, "<red>Nie ma skrzynki o ID: <yellow>" + id);
        return c;
    }

    @Override
    public boolean onCommand(CommandSender s, Command cmd, String label, String[] a) {
        if (a.length == 0) { help(s); return true; }
        String sub = a[0].toLowerCase();
        try {
            if (sub.equals("animacja")) {
                if (!(s instanceof Player p)) return true;
                boolean on = manager.toggleAnim(p.getUniqueId());
                Util.msg(p, "Animacja otwierania: " + (on ? "<green>włączona" : "<red>wyłączona"));
                return true;
            }
            if (sub.equals("podglad")) {
                if (!(s instanceof Player p) || a.length < 2) return true;
                Crate c = crate(s, a[1]);
                if (c != null) Gui.preview(p, c);
                return true;
            }
            if (!s.hasPermission("skrzynki.admin")) {
                Util.msg(s, "<red>Brak uprawnień.");
                return true;
            }
            switch (sub) {
                case "stworz" -> {
                    if (a.length < 2) { Util.msg(s, "<red>/skrzynka stworz <id> [nazwa]"); return true; }
                    String id = a[1].toLowerCase();
                    if (manager.crates.containsKey(id)) { Util.msg(s, "<red>Takie ID już istnieje."); return true; }
                    String name = a.length > 2 ? String.join(" ", Arrays.copyOfRange(a, 2, a.length)) : "<gold><bold>" + id;
                    manager.crates.put(id, new Crate(id, name));
                    manager.save();
                    Util.msg(s, "<green>Utworzono skrzynkę <yellow>" + id + "<green>. Użyj <yellow>/skrzynka edytuj " + id);
                    if (s instanceof Player p) Gui.editor(p, manager.crates.get(id));
                }
                case "usunskrzynke" -> {
                    Crate c = crate(s, a.length > 1 ? a[1] : "");
                    if (c == null) return true;
                    manager.crates.remove(c.id);
                    manager.locations.values().removeIf(v -> v.equals(c.id));
                    manager.save();
                    Util.msg(s, "<green>Usunięto skrzynkę.");
                }
                case "nazwa" -> {
                    Crate c = crate(s, a.length > 1 ? a[1] : "");
                    if (c == null || a.length < 3) return true;
                    c.name = String.join(" ", Arrays.copyOfRange(a, 2, a.length));
                    manager.save();
                    Util.msg(s, "<green>Zmieniono nazwę na: <white>" + c.name);
                }
                case "lista" -> {
                    Util.msg(s, "<gold>Skrzynki:");
                    for (Crate c : manager.crates.values())
                        s.sendMessage(Util.MM.deserialize("<yellow>" + c.id + " <dark_gray>- <white>" + c.name
                                + " <dark_gray>(<gray>" + c.rewards.size() + " dropów, klucz: " + c.key + "<dark_gray>)"));
                }
                case "edytuj" -> {
                    if (!(s instanceof Player p)) return true;
                    Crate c = crate(s, a.length > 1 ? a[1] : "");
                    if (c != null) Gui.editor(p, c);
                }
                case "dodaj" -> {
                    if (!(s instanceof Player p) || a.length < 3) { Util.msg(s, "<red>/skrzynka dodaj <id> <szansa%>"); return true; }
                    Crate c = crate(s, a[1]);
                    if (c == null) return true;
                    ItemStack hand = p.getInventory().getItemInMainHand();
                    if (hand.getType().isAir()) { Util.msg(p, "<red>Weź przedmiot do ręki."); return true; }
                    c.addReward(hand.clone(), Double.parseDouble(a[2].replace(',', '.')));
                    manager.save();
                    Util.msg(p, "<green>Dodano drop. Szanse przeliczone do 100%.");
                }
                case "szansa" -> {
                    if (a.length < 4) { Util.msg(s, "<red>/skrzynka szansa <id> <nr> <%>"); return true; }
                    Crate c = crate(s, a[1]);
                    if (c == null) return true;
                    int i = Integer.parseInt(a[2]) - 1;
                    if (i < 0 || i >= c.rewards.size()) { Util.msg(s, "<red>Zły numer dropu."); return true; }
                    c.setChance(i, Double.parseDouble(a[3].replace(',', '.')));
                    manager.save();
                    Util.msg(s, "<green>Ustawiono szansę: <yellow>" + Util.pct(c.rewards.get(i).chance));
                }
                case "usun" -> {
                    if (a.length < 3) return true;
                    Crate c = crate(s, a[1]);
                    if (c == null) return true;
                    int i = Integer.parseInt(a[2]) - 1;
                    if (i < 0 || i >= c.rewards.size()) { Util.msg(s, "<red>Zły numer dropu."); return true; }
                    c.removeReward(i);
                    manager.save();
                    Util.msg(s, "<green>Usunięto drop.");
                }
                case "klucz" -> {
                    if (a.length < 3) { Util.msg(s, "<red>/skrzynka klucz <gracz> <rzadki|epicki> [ilość]"); return true; }
                    Player t = Bukkit.getPlayerExact(a[1]);
                    String type = a[2].toLowerCase();
                    if (t == null) { Util.msg(s, "<red>Gracz offline."); return true; }
                    if (!Keys.valid(type)) { Util.msg(s, "<red>Typ: rzadki lub epicki."); return true; }
                    int n = a.length > 3 ? Integer.parseInt(a[3]) : 1;
                    t.getInventory().addItem(Keys.create(type, n)).values()
                            .forEach(i -> t.getWorld().dropItemNaturally(t.getLocation(), i));
                    Util.msg(s, "<green>Dano <yellow>" + n + "x <green>" + type + " graczowi " + t.getName());
                    Util.msg(t, "<green>Otrzymałeś <yellow>" + n + "x " + Keys.display(type));
                }
                case "dajdrop" -> {
                    if (!(s instanceof Player p) || a.length < 3) return true;
                    Crate c = crate(s, a[1]);
                    if (c == null) return true;
                    int i = Integer.parseInt(a[2]) - 1;
                    if (i < 0 || i >= c.rewards.size()) { Util.msg(s, "<red>Zły numer dropu."); return true; }
                    p.getInventory().addItem(c.rewards.get(i).item.clone());
                    Util.msg(p, "<green>Dostałeś drop nr " + (i + 1) + ".");
                }
                case "wymus" -> {
                    if (!(s instanceof Player p) || a.length < 3) return true;
                    Crate c = crate(s, a[1]);
                    if (c == null) return true;
                    if (a[2].equalsIgnoreCase("off")) {
                        manager.forced.remove(p.getUniqueId());
                        Util.msg(p, "<yellow>Wyłączono wymuszanie dropu.");
                        return true;
                    }
                    int i = Integer.parseInt(a[2]) - 1;
                    if (i < 0 || i >= c.rewards.size()) { Util.msg(s, "<red>Zły numer dropu."); return true; }
                    manager.forced.put(p.getUniqueId(), c.id + ":" + i);
                    Util.msg(p, "<green>Następne otwarcie skrzynki <yellow>" + c.id + "<green> da drop nr " + (i + 1) + ".");
                }
                case "ustaw" -> {
                    if (!(s instanceof Player p) || a.length < 2) return true;
                    Crate c = crate(s, a[1]);
                    if (c == null) return true;
                    Block b = p.getTargetBlockExact(6);
                    if (b == null || b.getType().isAir()) { Util.msg(p, "<red>Patrz na blok (max 6 kratek)."); return true; }
                    manager.locations.put(CrateManager.keyOf(b), c.id);
                    manager.save();
                    Util.msg(p, "<green>Ten blok (<yellow>" + b.getType() + "<green>) to teraz skrzynka <yellow>" + c.id);
                }
                case "zdejmij" -> {
                    if (!(s instanceof Player p)) return true;
                    Block b = p.getTargetBlockExact(6);
                    if (b == null || manager.locations.remove(CrateManager.keyOf(b)) == null) {
                        Util.msg(p, "<red>To nie jest skrzynka.");
                        return true;
                    }
                    manager.save();
                    Util.msg(p, "<green>Blok nie jest już skrzynką.");
                }
                case "typklucza" -> {
                    if (a.length < 3) return true;
                    Crate c = crate(s, a[1]);
                    if (c == null) return true;
                    String t = a[2].toLowerCase();
                    if (!Keys.valid(t) && !t.equals("brak")) { Util.msg(s, "<red>rzadki / epicki / brak"); return true; }
                    c.key = t;
                    manager.save();
                    Util.msg(s, "<green>Ustawiono klucz: " + Keys.display(t));
                }
                case "reload" -> {
                    manager.load();
                    Util.msg(s, "<green>Przeładowano.");
                }
                default -> help(s);
            }
        } catch (NumberFormatException ex) {
            Util.msg(s, "<red>Podaj poprawną liczbę.");
        }
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender s, Command cmd, String label, String[] a) {
        List<String> out = new ArrayList<>();
        if (a.length == 1) {
            out = SUBS.stream().filter(x -> s.hasPermission("skrzynki.admin") || x.equals("animacja") || x.equals("podglad")).collect(Collectors.toList());
        } else if (a.length == 2) {
            String sub = a[0].toLowerCase();
            if (sub.equals("klucz")) Bukkit.getOnlinePlayers().forEach(p -> out.add(p.getName()));
            else if (Set.of("podglad", "usunskrzynke", "nazwa", "edytuj", "dodaj", "szansa", "usun", "dajdrop", "wymus", "ustaw", "typklucza").contains(sub))
                out.addAll(manager.crates.keySet());
        } else if (a.length == 3) {
            String sub = a[0].toLowerCase();
            if (sub.equals("klucz")) out.addAll(List.of("rzadki", "epicki"));
            if (sub.equals("typklucza")) out.addAll(List.of("rzadki", "epicki", "brak"));
        }
        String last = a[a.length - 1].toLowerCase();
        return out.stream().filter(x -> x.toLowerCase().startsWith(last)).collect(Collectors.toList());
    }
}
