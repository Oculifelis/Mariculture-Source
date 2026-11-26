package mariculture.platform.compat.crafttweaker;

import org.openzen.zencode.java.ZenCodeType;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.item.IItemStack;

import mariculture.domain.plugin.Tweaks;

/** mods.mariculture.Anvil (plugins/minetweaker/Anvil.java): hammer {@code input} {@code hits} times into {@code output}. */
@ZenRegister
@ZenCodeType.Name("mods.mariculture.Anvil")
public final class Anvil {
    private Anvil() {}

    @ZenCodeType.Method
    public static void addRecipe(IItemStack input, IItemStack output, int hits) {
        Scripts.apply(new Tweaks.AnvilAdd(new mariculture.domain.metal.Anvil.Recipe(Scripts.id(input), Scripts.id(output),
                output.getInternal().getCount(), Math.max(1, hits))), "Adding anvil recipe " + Scripts.id(input) + " -> " + Scripts.id(output));
    }

    @ZenCodeType.Method
    public static void removeRecipe(IItemStack input) {
        Scripts.apply(new Tweaks.AnvilRemove(Scripts.id(input)), "Removing anvil recipe for " + Scripts.id(input));
    }
}
