package mariculture.platform.compat.crafttweaker;

import java.util.Locale;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.item.IItemStack;

import mariculture.domain.fish.Species;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.plugin.Tweaks;

/**
 * mods.mariculture.Fishing (plugins/minetweaker/Fishing.java): extra loot with a percent chance, the weakest rod
 * that can catch it ({@code type}, default "dire" = any rod; {@code exact} = that rod only) and the dimensions. Legacy
 * dimension ids map to kinds: -1 nether, 1 end, anything else the overworld kind; no dimensions means everywhere.
 */
@ZenRegister
@ZenCodeType.Name("mods.mariculture.Fishing")
public final class Fishing {
    private Fishing() {}

    @ZenCodeType.Method
    public static void addJunk(IItemStack loot, double chance) {
        add(loot, chance, null, false, null, FishingRules.Rarity.JUNK);
    }

    @ZenCodeType.Method
    public static void addJunk(IItemStack loot, double chance, String type) {
        add(loot, chance, type, false, null, FishingRules.Rarity.JUNK);
    }

    @ZenCodeType.Method
    public static void addJunk(IItemStack loot, double chance, String type, boolean exact) {
        add(loot, chance, type, exact, null, FishingRules.Rarity.JUNK);
    }

    @ZenCodeType.Method
    public static void addJunk(IItemStack loot, double chance, String type, boolean exact, int[] dimension) {
        add(loot, chance, type, exact, dimension, FishingRules.Rarity.JUNK);
    }

    @ZenCodeType.Method
    public static void addGood(IItemStack loot, double chance) {
        add(loot, chance, null, false, null, FishingRules.Rarity.GOOD);
    }

    @ZenCodeType.Method
    public static void addGood(IItemStack loot, double chance, String type) {
        add(loot, chance, type, false, null, FishingRules.Rarity.GOOD);
    }

    @ZenCodeType.Method
    public static void addGood(IItemStack loot, double chance, String type, boolean exact) {
        add(loot, chance, type, exact, null, FishingRules.Rarity.GOOD);
    }

    @ZenCodeType.Method
    public static void addGood(IItemStack loot, double chance, String type, boolean exact, int[] dimension) {
        add(loot, chance, type, exact, dimension, FishingRules.Rarity.GOOD);
    }

    @ZenCodeType.Method
    public static void addRare(IItemStack loot, double chance) {
        add(loot, chance, null, false, null, FishingRules.Rarity.RARE);
    }

    @ZenCodeType.Method
    public static void addRare(IItemStack loot, double chance, String type) {
        add(loot, chance, type, false, null, FishingRules.Rarity.RARE);
    }

    @ZenCodeType.Method
    public static void addRare(IItemStack loot, double chance, String type, boolean exact) {
        add(loot, chance, type, exact, null, FishingRules.Rarity.RARE);
    }

    @ZenCodeType.Method
    public static void addRare(IItemStack loot, double chance, String type, boolean exact, int[] dimension) {
        add(loot, chance, type, exact, dimension, FishingRules.Rarity.RARE);
    }

    @ZenCodeType.Method
    public static void removeLoot(IItemStack loot) {
        Scripts.apply(new Tweaks.LootRemove(Scripts.id(loot)), "Removing " + Scripts.id(loot) + " from Mariculture fishing loot");
    }

    private static void add(IItemStack loot, double chance, String type, boolean exact, int[] dimension, FishingRules.Rarity rarity) {
        Species.Rod rod = rod(type);
        String item = Scripts.id(loot);
        if (dimension == null || dimension.length == 0) dimension = new int[] {Short.MAX_VALUE};
        for (int dim : dimension) {
            String kind = dim == Short.MAX_VALUE ? null : dim == -1 ? "nether" : dim == 1 ? "end" : "overworld_like";
            Scripts.apply(new Tweaks.LootAdd(new FishingRules.Loot(item, chance, rarity, kind, rod), exact),
                    "Adding " + item + " to Mariculture fishing loot with rarity " + rarity.name().toLowerCase(Locale.ROOT));
        }
    }

    /** Fishing.getRodTypeFromString: unknown names are the dire rod. */
    private static Species.Rod rod(String name) {
        if (name == null) return Species.Rod.DIRE;
        for (Species.Rod r : Species.Rod.values()) if (r.name().equalsIgnoreCase(name)) return r;
        return Species.Rod.DIRE;
    }
}
