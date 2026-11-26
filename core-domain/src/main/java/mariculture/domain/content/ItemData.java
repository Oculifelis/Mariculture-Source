package mariculture.domain.content;

import java.util.List;

/**
 * Which data component holds an item's Mariculture data on 1.20.5+ editions (Forge 1.20.1 keeps the stack's NBT
 * tag). One component per item family, so other mods, {@code /give} and data packs see what an item carries
 * ({@code mariculture:fish}, {@code mariculture:jewelry}, ...) instead of an anonymous custom-data blob. The payload
 * is the same key map the domain reads ({@code Stack.data()}).
 */
public final class ItemData {
    public static final String FISH = "fish";
    public static final String EGGS = "eggs";
    public static final String JEWELRY = "jewelry";
    public static final String WORKED = "worked";
    public static final String ENERGY = "energy";
    public static final String ITEM = "item_data";
    public static final List<String> COMPONENTS = List.of(FISH, EGGS, JEWELRY, WORKED, ENERGY, ITEM);

    private ItemData() {}

    /** The component (path under {@code mariculture:}) for an item id; ids of other mods get {@link #ITEM}. */
    public static String component(String itemId) {
        String id = itemId.startsWith("mariculture:") ? itemId.substring("mariculture:".length()) : itemId;
        if (id.equals("fish_eggs")) return EGGS;
        if (id.equals("ring") || id.equals("bracelet") || id.equals("necklace")) return JEWELRY;
        if (id.equals("worked_item")) return WORKED;
        if (id.endsWith("_battery") || id.equals("flux_rod")) return ENERGY;
        if (id.equals("live_fish") || liveFish().contains(id)) return FISH;
        return ITEM;
    }

    private static java.util.Set<String> liveFish;

    private static synchronized java.util.Set<String> liveFish() {
        if (liveFish == null) {
            liveFish = java.util.Set.copyOf(Catalog.items().stream().filter(i -> i.kind() instanceof ItemKind.LiveFish).map(ItemSpec::id).toList());
        }
        return liveFish;
    }
}
