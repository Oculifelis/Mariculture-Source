package mariculture.platform.compat.crafttweaker;

import java.util.List;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.item.IItemStack;

import mariculture.domain.metal.CrucibleFuels;
import mariculture.domain.metal.Melting;
import mariculture.domain.plugin.Tweaks;

/**
 * mods.mariculture.Crucible (plugins/minetweaker/Crucible.java): melting recipes (temperature in degrees, optional
 * by-product at 1 in {@code chance}) and fuels ({@code max} temperature, degrees per heat and ticks it lasts). A string
 * fuel was an ore-dictionary name; it is an item tag now.
 */
@ZenRegister
@ZenCodeType.Name("mods.mariculture.Crucible")
public final class Crucible {
    private Crucible() {}

    @ZenCodeType.Method
    public static void addRecipe(int temp, IItemStack input, IFluidStack fluid) {
        add(temp, input, fluid, null, 0);
    }

    @ZenCodeType.Method
    public static void addRecipe(int temp, IItemStack input, IFluidStack fluid, IItemStack output, int chance) {
        add(temp, input, fluid, output, chance);
    }

    @ZenCodeType.Method
    public static void removeRecipe(IItemStack input) {
        Scripts.apply(new Tweaks.MeltRemove(Scripts.id(input)), "Removing crucible melting of " + Scripts.id(input));
    }

    @ZenCodeType.Method
    public static void addFuel(IItemStack input, int max, int per, int time) {
        fuel(Scripts.id(input), false, max, per, time);
    }

    @ZenCodeType.Method
    public static void addFuel(IFluidStack input, int max, int per, int time) {
        fuel(Scripts.id(input), true, max, per, time);
    }

    @ZenCodeType.Method
    public static void addFuel(String tag, int max, int per, int time) {
        fuel(Scripts.tag(tag), false, max, per, time);
    }

    @ZenCodeType.Method
    public static void removeFuel(IItemStack fuel) {
        Scripts.apply(new Tweaks.FuelRemove(Scripts.id(fuel), false), "Removing crucible fuel " + Scripts.id(fuel));
    }

    @ZenCodeType.Method
    public static void removeFuel(IFluidStack fuel) {
        Scripts.apply(new Tweaks.FuelRemove(Scripts.id(fuel), true), "Removing crucible fuel " + Scripts.id(fuel));
    }

    @ZenCodeType.Method
    public static void removeFuel(String tag) {
        Scripts.apply(new Tweaks.FuelRemove(Scripts.tag(tag), false), "Removing crucible fuel " + Scripts.tag(tag));
    }

    private static void add(int temp, IItemStack input, IFluidStack fluid, IItemStack output, int chance) {
        Melting.Recipe r = new Melting.Recipe(Scripts.id(input), temp, Scripts.id(fluid), Scripts.mb(fluid),
                output == null ? null : Scripts.id(output), output == null ? 0 : Math.max(1, chance), List.of(), List.of());
        Scripts.apply(new Tweaks.MeltAdd(r), "Adding crucible melting " + r.input() + " -> " + r.fluid());
    }

    private static void fuel(String key, boolean fluid, int max, int per, int time) {
        Scripts.apply(new Tweaks.FuelAdd(key, fluid, new CrucibleFuels.Fuel(max, per, Math.max(1, time), 0, 0)), "Adding crucible fuel " + key);
    }
}
