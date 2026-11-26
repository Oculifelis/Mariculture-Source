package mariculture.domain.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import mariculture.domain.config.Settings;

/**
 * Which items count as the same thing (core/handlers/OreDicHandler.java, core/config/AutoDictionary.java, 1.3.0).
 * Legacy compared ore dictionary names; here two items are the same when they share a material tag:
 * {@code c:<group>/<material>} (Forge 1.20.1: {@code forge:}) with the group in config
 * {@code factory.dictionary_tag_groups}, and neither is in {@code factory.dictionary_blacklist}.
 */
public final class Dictionary {
    private Dictionary() {}

    /** The material tags among an item's tags, in the order given. */
    public static List<String> materialTags(Set<String> tags) {
        List<String> out = new ArrayList<>();
        for (String t : tags) {
            int colon = t.indexOf(':');
            String ns = colon < 0 ? "" : t.substring(0, colon);
            if (!ns.equals("c") && !ns.equals("forge")) continue;
            String path = t.substring(colon + 1);
            int slash = path.indexOf('/');
            if (slash > 0 && Settings.DICTIONARY_GROUPS.get().contains(path.substring(0, slash))) out.add(t);
        }
        out.sort(null);
        return out;
    }

    public static boolean blacklisted(String item) {
        return Settings.DICTIONARY_BLACKLIST.get().contains(item);
    }

    /** OreDicHandler.areEqual: a shared material tag. */
    public static boolean same(String a, Set<String> tagsA, String b, Set<String> tagsB) {
        if (blacklisted(a) || blacklisted(b)) return false;
        List<String> mb = materialTags(tagsB);
        for (String t : materialTags(tagsA)) if (mb.contains(t)) return true;
        return false;
    }

    /** OreDicHandler.getNextValidEntry: the item after {@code item} in its first material tag, wrapping round. */
    public static String next(String item, List<String> inTag) {
        List<String> ok = new ArrayList<>();
        for (String i : inTag) if (!blacklisted(i)) ok.add(i);
        if (ok.isEmpty()) return item;
        int at = ok.indexOf(item);
        return ok.get((at + 1) % ok.size());
    }
}
