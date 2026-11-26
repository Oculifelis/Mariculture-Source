package mariculture.domain.content;

import java.util.Objects;

import mariculture.domain.fish.Species;

/**
 * What kind of item a catalog entry is, so each target can pick the matching item class. Plain
 * items need nothing more; the others carry the numbers their legacy item class set.
 */
public sealed interface ItemKind permits ItemKind.Plain, ItemKind.Rod, ItemKind.Armor, ItemKind.Food, ItemKind.LiveFish,
        ItemKind.RawFish, ItemKind.FluidBucket, ItemKind.FishEggs, ItemKind.Special, ItemKind.FluidContainer {
    ItemKind PLAIN = new Plain();

    record Plain() implements ItemKind {}

    /**
     * A fishing rod (fishery/items/ItemRod). {@code repairItem} is the anvil repair material,
     * legacy {@code setRepairMaterial}. Durability 0 is a rod that runs on energy instead (the flux rod), with no
     * repair material.
     */
    record Rod(Species.Rod tier, int durability, int enchantability, String repairItem) implements ItemKind {
        public Rod {
            Objects.requireNonNull(tier);
            if (durability < 0 || enchantability < 0) throw new IllegalArgumentException("bad rod numbers");
            repairItem = repairItem == null ? null : Ids.full(repairItem);
        }

        public boolean usesEnergy() {
            return durability == 0;
        }
    }

    /** A wearable armour piece made of one of {@link Catalog#armorMaterials()}. */
    record Armor(String material, ArmorSlot slot) implements ItemKind {
        public Armor {
            Ids.requireValid(material);
            Objects.requireNonNull(slot);
        }
    }

    /**
     * Food (core/items/ItemFood). {@code remainder} is the item left after eating (a bowl), or null.
     * {@code effect} names a domain eating effect ({@link mariculture.domain.fish.FishEffects}) or null.
     */
    record Food(int hunger, float saturation, boolean alwaysEdible, String remainder, String effect) implements ItemKind {}

    /** A live fish carrying a genome (fishery/items/ItemFishy); one item per shipped species. */
    record LiveFish(String species) implements ItemKind {}

    /** The raw (dead) form of a species, edible with its food values and eating effect (ItemVanillaFish). */
    record RawFish(String species, int hunger, float saturation) implements ItemKind {}

    /** A bucket of a Mariculture fluid. */
    record FluidBucket(String fluid) implements ItemKind {}

    /** Fish eggs carrying an egg genome (fishery/items/ItemEgg). */
    record FishEggs() implements ItemKind {}

    /**
     * An item whose behaviour is a named adapter class (scanner, temperature control, net placer, ...).
     * {@code durability} 0 means none.
     */
    record Special(String behaviour, int durability) implements ItemKind {}

    /**
     * A container holding one fluid (core/items/ItemFluidStorage: ladle, titanium bucket; ItemBottle heat bottles).
     * {@code heatBottle} caps each fluid at its legacy heat-bottle volume ({@link mariculture.domain.fluid.HeatBottle}).
     */
    record FluidContainer(int capacity, boolean heatBottle) implements ItemKind {}
}
