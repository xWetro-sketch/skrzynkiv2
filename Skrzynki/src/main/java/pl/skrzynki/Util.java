package pl.skrzynki;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Arrays;
import java.util.Locale;
import java.util.stream.Collectors;

public final class Util {
    public static final MiniMessage MM = MiniMessage.miniMessage();
    public static final String PREFIX = "<gradient:#ffd700:#ff8c00><bold>ꜱᴋʀᴢʏɴᴋɪ</bold></gradient> <dark_gray>» <gray>";
    /** Napis "TWOJA NAGRODA" - ładna czcionka (small caps), pogrubiona, BEZ pochylenia. */
    public static final String TITLE = "<gradient:#ffd700:#ff8c00><bold>ᴛᴡᴏᴊᴀ ɴᴀɢʀᴏᴅᴀ</bold></gradient>";

    public static Component c(String s) {
        return MM.deserialize("<!italic>" + s);
    }

    public static void msg(CommandSender s, String m) {
        s.sendMessage(MM.deserialize(PREFIX + m));
    }

    public static String pct(double d) {
        return String.format(Locale.US, "%.2f", d) + "%";
    }

    public static ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(c(name));
        if (lore.length > 0) meta.lore(Arrays.stream(lore).map(Util::c).collect(Collectors.toList()));
        it.setItemMeta(meta);
        return it;
    }

    public static ItemStack filler(Material m) {
        return item(m, " ");
    }

    public static Component nameOf(ItemStack it) {
        ItemMeta m = it.getItemMeta();
        Component base = (m != null && m.hasDisplayName()) ? m.displayName() : Component.translatable(it.translationKey());
        Component out = Component.empty().decoration(TextDecoration.ITALIC, false).append(base);
        if (it.getAmount() > 1) out = out.append(c(" <gray>x" + it.getAmount()));
        return out;
    }
}
