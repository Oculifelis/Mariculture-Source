package mariculture.domain.machine;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Predicate;

import mariculture.domain.fish.LiveFish;
import mariculture.domain.fish.ShippedSpecies;
import mariculture.domain.maritech.EnergyItem;

/**
 * What a machine slot is for, shown in its menu: the role (border colour and shift-click order), a lang key suffix
 * naming what goes in ({@code gui.mariculture.slot.<what>}) and the items an empty slot shows faintly, cycling. Ghost
 * entries are item ids or "#tag" and come from the same tables the machine checks, so they follow data changes; an id
 * the game does not have is skipped by the screen. {@code fits} (null for any item) says which stacks the slot is
 * meant for, so a shift-click picks the slot made for the item before a slot that merely accepts it.
 *
 * <p>New in the port: legacy menus had plain slots and no hints.
 */
public record SlotInfo(Role role, String what, List<String> ghosts, Predicate<Stack> fits) {

    public enum Role {
        INPUT(0xFF3F7FD8), OUTPUT(0xFFE08A2E), FUEL(0xFFE0C830), CATALYST(0xFFE0C830), UPGRADE(0), FILTER(0), STORAGE(0);

        /** ARGB border colour, 0 for a plain slot. */
        public final int colour;

        Role(int colour) {
            this.colour = colour;
        }

        /** Lang key of the role name. */
        public String key() {
            return "gui.mariculture.slot.role." + name().toLowerCase(java.util.Locale.ROOT);
        }

        /** Shift-click priority: lower is tried first. Outputs and filters never take shift-clicks. */
        public int order() {
            return switch (this) {
                case FUEL, CATALYST, UPGRADE -> 0;
                case INPUT -> 1;
                case STORAGE -> 2;
                case OUTPUT, FILTER -> -1;
            };
        }
    }

    public SlotInfo {
        ghosts = List.copyOf(ghosts);
    }

    /** Lang key naming what goes in. */
    public String key() {
        return "gui.mariculture.slot." + what;
    }

    public static SlotInfo of(Role role, String what, Collection<String> ghosts, Predicate<Stack> fits) {
        return new SlotInfo(role, what, new ArrayList<>(ghosts), fits);
    }

    public static SlotInfo input(String what, Collection<String> ghosts, Predicate<Stack> fits) {
        return of(Role.INPUT, what, ghosts, fits);
    }

    public static final SlotInfo OUTPUT = new SlotInfo(Role.OUTPUT, "output", List.of(), null);
    public static final SlotInfo ANY = new SlotInfo(Role.INPUT, "input", List.of(), null);
    public static final SlotInfo FILTER = new SlotInfo(Role.FILTER, "filter", List.of(), null);
    public static final SlotInfo STORAGE = new SlotInfo(Role.STORAGE, "storage", List.of(), null);

    // Ghost lists taken from the machines' own tables.

    public static SlotInfo upgrade() {
        List<String> g = new ArrayList<>();
        for (Upgrades.Spec s : Upgrades.all()) g.add("mariculture:" + s.id());
        g.add(TemperatureControl.ITEM);
        return of(Role.UPGRADE, "upgrade", g, Upgrades::isUpgrade);
    }

    public static SlotInfo battery() {
        return of(Role.FUEL, "battery", List.of("mariculture:" + EnergyItem.COPPER, "mariculture:" + EnergyItem.TITANIUM),
                s -> EnergyItem.spec(s) != null);
    }

    public static SlotInfo liveFish() {
        return input("live_fish", liveFishItems(), LiveFish::isLiveFish);
    }

    /** Fluid containers for a machine's fluid input slot. */
    public static SlotInfo fluidIn() {
        return input("fluid_container", FLUID_CONTAINERS, SlotInfo::isFluidContainer);
    }

    /** Live fish items of the shipped species (the item a species is drawn as). */
    public static List<String> liveFishItems() {
        LinkedHashSet<String> g = new LinkedHashSet<>();
        for (ShippedSpecies.Entry e : ShippedSpecies.ALL) {
            String item = LiveFish.itemFor("mariculture:" + e.id());
            if (!item.equals(LiveFish.GENERIC)) g.add(item);
            if (g.size() >= 12) break;
        }
        return new ArrayList<>(g);
    }

    /** Buckets and bottles: what a fluid input slot is meant for. */
    public static boolean isFluidContainer(Stack s) {
        if (s.isEmpty()) return false;
        String path = s.item().substring(s.item().indexOf(':') + 1);
        return path.endsWith("bucket") || path.endsWith("bottle") || path.contains("bucket_") || path.contains("_cell");
    }

    public static final List<String> FLUID_CONTAINERS = List.of("minecraft:water_bucket", "mariculture:fish_oil_bucket", "minecraft:lava_bucket");
}
