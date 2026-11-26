package mariculture.platform.compat.crafttweaker;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.item.IItemStack;

import mariculture.domain.metal.Casting.Kind;
import mariculture.domain.plugin.Tweaks;

/** mods.mariculture.Casting (plugins/minetweaker/Casting.java): the fluid stack's amount is what one cast uses. */
@ZenRegister
@ZenCodeType.Name("mods.mariculture.Casting")
public final class Casting {
    private Casting() {}

    @ZenCodeType.Method
    public static void addNuggetRecipe(IFluidStack input, IItemStack output) {
        add(Kind.NUGGET, input, output);
    }

    @ZenCodeType.Method
    public static void addIngotRecipe(IFluidStack input, IItemStack output) {
        add(Kind.INGOT, input, output);
    }

    @ZenCodeType.Method
    public static void addBlockRecipe(IFluidStack input, IItemStack output) {
        add(Kind.BLOCK, input, output);
    }

    @ZenCodeType.Method
    public static void removeNuggetRecipe(IItemStack output) {
        remove(Kind.NUGGET, output);
    }

    @ZenCodeType.Method
    public static void removeIngotRecipe(IItemStack output) {
        remove(Kind.INGOT, output);
    }

    @ZenCodeType.Method
    public static void removeBlockRecipe(IItemStack output) {
        remove(Kind.BLOCK, output);
    }

    private static void add(Kind kind, IFluidStack input, IItemStack output) {
        Scripts.apply(new Tweaks.CastAdd(kind, Scripts.id(input), Scripts.id(output), Math.max(1, Scripts.mb(input))),
                "Adding " + kind.name().toLowerCase(java.util.Locale.ROOT) + " casting " + Scripts.id(input) + " -> " + Scripts.id(output));
    }

    private static void remove(Kind kind, IItemStack output) {
        Scripts.apply(new Tweaks.CastRemove(kind, Scripts.id(output)),
                "Removing " + kind.name().toLowerCase(java.util.Locale.ROOT) + " casting of " + Scripts.id(output));
    }
}
