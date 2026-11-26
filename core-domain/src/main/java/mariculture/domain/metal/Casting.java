package mariculture.domain.metal;

import java.util.Map;
import java.util.function.Function;
import java.util.function.Predicate;

import mariculture.domain.fluid.Fluids;

/**
 * Casting (core/handlers/CastingHandler, RecipeHelper.addMetalCasting, RecipesSmelting.java:289, 313): a molten
 * metal casts a nugget (16 mB), ingot (144 mB) or block (1296 mB). Water casts salt in the nugget caster and molten
 * glass casts glass in the block caster.
 *
 * <p>Outputs: Mariculture and vanilla metals name their item; other mods' metals cast the first item of
 * {@code c:<kind>s/<metal>} (legacy took the first ore-dictionary entry), and nothing when the tag is empty.
 */
public final class Casting {
    public enum Kind {
        NUGGET("nuggets", MetalRates.NUGGET), INGOT("ingots", MetalRates.INGOT), BLOCK("storage_blocks", MetalRates.BLOCK);

        final String tagFolder;
        final int amount;

        Kind(String tagFolder, int amount) {
            this.tagFolder = tagFolder;
            this.amount = amount;
        }

        public int amount() {
            return amount;
        }
    }

    /** What a caster makes from its tank: the item and the fluid it uses. */
    public record Result(String item, int amount) {}

    private static final Map<String, String[]> KNOWN = Map.of(
            "iron", new String[] {"minecraft:iron_nugget", "minecraft:iron_ingot", "minecraft:iron_block"},
            "gold", new String[] {"minecraft:gold_nugget", "minecraft:gold_ingot", "minecraft:gold_block"},
            "copper", new String[] {"mariculture:copper_nugget", "minecraft:copper_ingot", "minecraft:copper_block"},
            "aluminum", new String[] {"mariculture:aluminum_nugget", "mariculture:aluminum_ingot", "mariculture:aluminum_block"},
            "magnesium", new String[] {"mariculture:magnesium_nugget", "mariculture:magnesium_ingot", "mariculture:magnesium_block"},
            "titanium", new String[] {"mariculture:titanium_nugget", "mariculture:titanium_ingot", "mariculture:titanium_block"},
            "rutile", new String[] {"mariculture:rutile_nugget", "mariculture:rutile_ingot", "mariculture:rutile_block"},
            // Thaumaturge's metals (its fluids exist only with the plugin; void metal's tags are c:*/void_metal)
            "thaumium", new String[] {"thaumaturge:nugget_thaumium", "thaumaturge:ingot_thaumium", "thaumaturge:metal_thaumium"},
            "void", new String[] {"thaumaturge:nugget_void", "thaumaturge:ingot_void", "thaumaturge:metal_void"});

    private Casting() {}

    /**
     * The cast for a tank holding {@code amount} of {@code fluid}, or null when there is none or not enough
     * (CastingHandler.get*Result). {@code firstInTag} returns a tag's first item id or null.
     */
    public static Result result(Kind kind, String fluid, int amount, Function<String, String> firstInTag) {
        if (fluid == null) return null;
        Result r = recipe(kind, fluid, firstInTag);
        return r == null || amount < r.amount() ? null : r;
    }

    static Result recipe(Kind kind, String fluid, Function<String, String> firstInTag) {
        return mariculture.domain.plugin.Tweaks.cast(kind, fluid, () -> builtIn(kind, fluid, firstInTag));
    }

    private static Result builtIn(Kind kind, String fluid, Function<String, String> firstInTag) {
        if (kind == Kind.NUGGET && fluid.equals(Fluids.WATER)) return new Result("mariculture:salt", 250);
        if (kind == Kind.BLOCK && fluid.equals("mariculture:molten_glass")) return new Result("minecraft:glass", 1000);
        if (!fluid.startsWith("mariculture:molten_")) return null;
        String metal = fluid.substring("mariculture:molten_".length());
        if (metal.equals("salt") || metal.equals("glass")) return null;
        String[] known = KNOWN.get(metal);
        String item = known != null ? known[kind.ordinal()] : firstInTag.apply("c:" + kind.tagFolder + "/" + metal);
        return item == null ? null : new Result(item, kind.amount);
    }

    /** The cast for Mariculture's own and vanilla fluids, ignoring other mods' tags (recipe viewers). */
    public static Result recipeFor(Kind kind, String fluid) {
        return recipe(kind, fluid, t -> null);
    }

    /** Whether the fluid can ever be cast by this kind of caster (fill checks). */
    public static boolean accepts(Kind kind, String fluid, Predicate<String> tagHasItems) {
        return recipe(kind, fluid, t -> tagHasItems.test(t) ? "present" : null) != null;
    }
}
