package mariculture.platform.compat.crafttweaker;

import java.util.concurrent.atomic.AtomicInteger;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.item.IItemStack;

import mariculture.domain.machine.FluidAmount;
import mariculture.domain.metal.VatRecipes;
import mariculture.domain.plugin.Tweaks;

/** mods.mariculture.Vat: the nine addRecipe and three removeRecipe forms of plugins/minetweaker/Vat.java. Time is in vat steps. */
@ZenRegister
@ZenCodeType.Name("mods.mariculture.Vat")
public final class Vat {
    private static final AtomicInteger IDS = new AtomicInteger();

    private Vat() {}

    @ZenCodeType.Method
    public static void addRecipe(IFluidStack fluid1, IFluidStack fluid2, IFluidStack outputFluid, int time) {
        add(fluid1, fluid2, null, outputFluid, null, time);
    }

    @ZenCodeType.Method
    public static void addRecipe(IFluidStack fluid1, IFluidStack fluid2, IItemStack outputItem, int time) {
        add(fluid1, fluid2, null, null, outputItem, time);
    }

    @ZenCodeType.Method
    public static void addRecipe(IFluidStack fluid1, IFluidStack fluid2, IFluidStack outputFluid, IItemStack outputItem, int time) {
        add(fluid1, fluid2, null, outputFluid, outputItem, time);
    }

    @ZenCodeType.Method
    public static void addRecipe(IFluidStack fluid1, IItemStack input, IFluidStack outputFluid, int time) {
        add(fluid1, null, input, outputFluid, null, time);
    }

    @ZenCodeType.Method
    public static void addRecipe(IFluidStack fluid1, IItemStack input, IItemStack outputItem, int time) {
        add(fluid1, null, input, null, outputItem, time);
    }

    @ZenCodeType.Method
    public static void addRecipe(IFluidStack fluid1, IItemStack input, IFluidStack outputFluid, IItemStack outputItem, int time) {
        add(fluid1, null, input, outputFluid, outputItem, time);
    }

    @ZenCodeType.Method
    public static void addRecipe(IFluidStack fluid1, IFluidStack fluid2, IItemStack input, IFluidStack outputFluid, int time) {
        add(fluid1, fluid2, input, outputFluid, null, time);
    }

    @ZenCodeType.Method
    public static void addRecipe(IFluidStack fluid1, IFluidStack fluid2, IItemStack input, IItemStack outputItem, int time) {
        add(fluid1, fluid2, input, null, outputItem, time);
    }

    @ZenCodeType.Method
    public static void addRecipe(IFluidStack fluid1, IFluidStack fluid2, IItemStack input, IFluidStack outputFluid, IItemStack outputItem, int time) {
        add(fluid1, fluid2, input, outputFluid, outputItem, time);
    }

    @ZenCodeType.Method
    public static void removeRecipe(IItemStack outputItem) {
        Scripts.apply(new Tweaks.VatRemove(Scripts.id(outputItem), null), "Removing vat recipes making " + Scripts.id(outputItem));
    }

    @ZenCodeType.Method
    public static void removeRecipe(IFluidStack outputFluid) {
        Scripts.apply(new Tweaks.VatRemove(null, Scripts.id(outputFluid)), "Removing vat recipes making " + Scripts.id(outputFluid));
    }

    @ZenCodeType.Method
    public static void removeRecipe(IItemStack outputItem, IFluidStack outputFluid) {
        Scripts.apply(new Tweaks.VatRemove(Scripts.id(outputItem), Scripts.id(outputFluid)),
                "Removing vat recipes making " + Scripts.id(outputItem) + " and " + Scripts.id(outputFluid));
    }

    private static void add(IFluidStack f1, IFluidStack f2, IItemStack input, IFluidStack outFluid, IItemStack outItem, int time) {
        VatRecipes.Recipe r = new VatRecipes.Recipe("crafttweaker_" + IDS.getAndIncrement(), amount(f1), f2 == null ? null : amount(f2),
                input == null ? null : Scripts.id(input), input == null ? 0 : input.getInternal().getCount(),
                outFluid == null ? null : amount(outFluid), outItem == null ? null : Scripts.stack(outItem), Math.max(1, time), "CraftTweaker");
        Scripts.apply(new Tweaks.VatAdd(r), "Adding vat recipe " + r.id());
    }

    private static FluidAmount amount(IFluidStack f) {
        return new FluidAmount(Scripts.id(f), Scripts.mb(f));
    }
}
