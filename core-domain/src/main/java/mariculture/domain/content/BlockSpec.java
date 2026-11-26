package mariculture.domain.content;

import java.util.Map;
import java.util.Objects;

/**
 * A simple Mariculture block: everything a target needs to register it and to generate its
 * blockstate, models, loot table, mining tags and name.
 *
 * <p>{@code resistance} is the modern explosion resistance. Legacy values convert as follows:
 * a 1.7 {@code getExplosionResistance} override is used unchanged (same formula), while a 1.7
 * {@code setResistance(r)} becomes {@code r * 3 / 5}.
 *
 * @param legacy legacy block name and metadata, for docs/id_mapping.csv
 * @param textures model texture slot to texture name (under textures/block/)
 */
public record BlockSpec(String id, String englishName, String legacy, Shape shape, Sound sound,
                        float hardness, float resistance, boolean pickaxe, HarvestLevel harvestLevel,
                        Map<String, String> textures, boolean hasItem) {
    public BlockSpec {
        Ids.requireValid(id);
        Objects.requireNonNull(englishName);
        if (hardness < 0 || resistance < 0) throw new IllegalArgumentException(id + ": negative strength");
        textures = Map.copyOf(textures);
        switch (shape) {
            case CUBE -> requireSlots(id, textures, textures.containsKey("all") ? new String[] {"all"} : new String[] {"end", "side"});
            case PILLAR -> requireSlots(id, textures, "end", "side");
            case PEDESTAL -> requireSlots(id, textures, "top", "bottom", "side");
            case OYSTER -> requireSlots(id, textures, "shell", "lid");
            case NET -> requireSlots(id, textures, "all");
            case LAMP -> requireSlots(id, textures, "on", "off");
            case GLASS, ANCIENT_SAND, BUBBLE_SAND, CORAL, LIVING_CORAL, GAS, CUSTOM -> requireSlots(id, textures, "all");
            case MACHINE, MACHINE_GLASS -> requireSlots(id, textures, textures.containsKey("all") ? new String[] {"all"}
                    : textures.containsKey("down") ? new String[] {"down", "up", "north", "south", "west", "east"}
                    : new String[] {"front", "side", "top", "bottom"});
            case MACHINE_MODEL -> {
                if (!textures.containsKey("particle")) throw new IllegalArgumentException(id + " is missing texture slot particle");
            }
        }
    }

    private static void requireSlots(String id, Map<String, String> textures, String... slots) {
        for (String slot : slots) {
            if (!textures.containsKey(slot)) throw new IllegalArgumentException(id + " is missing texture slot " + slot);
        }
        if (textures.size() != slots.length) throw new IllegalArgumentException(id + " has unexpected texture slots " + textures.keySet());
    }

    /**
     * Whether the item, used on nothing, places the block above still water as well as normally (legacy autofisher,
     * ItemBlockMachine.onItemRightClick:48).
     */
    public boolean alsoPlacesOnWater() {
        return id.equals("autofisher");
    }

    public boolean isMachine() {
        return shape == Shape.MACHINE || shape == Shape.MACHINE_GLASS || shape == Shape.MACHINE_MODEL;
    }

    /** Machines with front/side/top/bottom textures face the player who placed them. */
    public boolean facing() {
        return isMachine() && textures.containsKey("front");
    }

    /** 1.7 {@code setResistance(r)} to modern explosion resistance. */
    public static float fromLegacySetResistance(float r) {
        return r * 3f / 5f;
    }
}
