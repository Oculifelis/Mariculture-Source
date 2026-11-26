package mariculture.domain.content;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The order of the Mariculture creative tab. Legacy 1.7 had several tabs (core, fishery, magic, factory, world);
 * the modern single tab keeps that grouping instead of registration order: fishing first, then fish, food,
 * pearls and jewelry, diving gear, machines and upgrades, materials, building blocks and buckets last.
 * Inside a group the catalog order is kept (sorting is stable).
 */
public final class CreativeOrder {
    public enum Group { FISHING, FISH, FOOD, MAGIC, DIVING, MACHINES, MATERIALS, BUILDING, BUCKETS }

    private static Map<String, Group> byId;

    private CreativeOrder() {}

    /** The group of an item id (path only, or {@code mariculture:path}). Unknown ids (compat items) go to MATERIALS. */
    public static Group group(String id) {
        String path = id.startsWith("mariculture:") ? id.substring("mariculture:".length()) : id;
        return index().getOrDefault(path, Group.MATERIALS);
    }

    /** Sorts item ids into tab order, keeping the given order inside each group. */
    public static <T> void sort(List<T> items, java.util.function.Function<T, String> id) {
        items.sort(Comparator.comparingInt(t -> group(id.apply(t)).ordinal()));
    }

    private static synchronized Map<String, Group> index() {
        if (byId != null) return byId;
        Map<String, Group> map = new HashMap<>();
        for (BlockSpec b : Catalog.blocks()) {
            if (b.hasItem()) map.put(b.id(), ofBlock(b));
        }
        for (ItemSpec i : Catalog.items()) map.put(i.id(), ofItem(i));
        byId = map;
        return map;
    }

    static Group ofBlock(BlockSpec b) {
        return switch (b.shape()) {
            case MACHINE, MACHINE_GLASS, MACHINE_MODEL -> Group.MACHINES;
            case NET -> Group.FISHING;
            case OYSTER -> Group.MAGIC;
            default -> Group.BUILDING;
        };
    }

    static Group ofItem(ItemSpec i) {
        String id = i.id();
        ItemKind k = i.kind();
        if (k instanceof ItemKind.Rod || id.startsWith("bait_")) return Group.FISHING;
        if (k instanceof ItemKind.LiveFish || k instanceof ItemKind.RawFish || k instanceof ItemKind.FishEggs
                || id.equals("live_fish") || id.equals("fish_dna") || id.equals("fish_meal")) return Group.FISH;
        if (k instanceof ItemKind.Food) return Group.FOOD;
        if (k instanceof ItemKind.FluidBucket || id.endsWith("_bucket")) return Group.BUCKETS;
        if (k instanceof ItemKind.Armor || id.contains("bucket_hat") || id.equals("speed_boat") || id.equals("fludd")
                || id.equals("lifejacket")) return Group.DIVING;
        if (id.endsWith("_pearl") || id.equals("ring") || id.equals("bracelet") || id.equals("necklace")
                || id.endsWith("mirror") || id.startsWith("droplet_") || id.startsWith("golden_")
                || id.equals("worked_item") || id.equals("life_core")) return Group.MAGIC;
        if (id.endsWith("_upgrade") || id.startsWith("plan_") || id.equals("blank_plan") || id.equals("paintbrush")
                || id.equals("hammer") || id.equals("ladle") || id.equals("scanner") || id.equals("thermometer")
                || id.equals("item_filter") || id.equals("creative_battery") || id.endsWith("_battery")
                || id.endsWith("_component") || id.equals("temperature_control")) return Group.MACHINES;
        return Group.MATERIALS;
    }
}
