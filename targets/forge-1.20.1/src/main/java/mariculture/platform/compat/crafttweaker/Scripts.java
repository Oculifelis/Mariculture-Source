package mariculture.platform.compat.crafttweaker;

import com.blamejared.crafttweaker.api.CraftTweakerAPI;
import com.blamejared.crafttweaker.api.action.base.IUndoableAction;
import com.blamejared.crafttweaker.api.fluid.IFluidStack;
import com.blamejared.crafttweaker.api.item.IItemStack;

import mariculture.domain.machine.Stack;
import mariculture.domain.plugin.Tweaks;
import net.minecraft.core.registries.BuiltInRegistries;

/** Shared helpers for the CraftTweaker classes (PLUG-022): each script call is one undoable {@link Tweaks.Change}. */
final class Scripts {
    private Scripts() {}

    static void apply(Tweaks.Change change, String what) {
        CraftTweakerAPI.apply(new Action(change, what));
    }

    static String id(IItemStack stack) {
        return BuiltInRegistries.ITEM.getKey(stack.getInternal().getItem()).toString();
    }

    static Stack stack(IItemStack stack) {
        return Stack.of(id(stack), stack.getInternal().getCount());
    }

    static String id(IFluidStack fluid) {
        return BuiltInRegistries.FLUID.getKey(fluid.getFluid()).toString();
    }

    static int mb(IFluidStack fluid) {
        return (int) Math.min(Integer.MAX_VALUE, fluid.getAmount());
    }

    /** The tag key from "#c:ingots/iron" or "c:ingots/iron". */
    static String tag(String tag) {
        return "#" + (tag.startsWith("#") ? tag.substring(1) : tag);
    }

    private record Action(Tweaks.Change change, String what) implements IUndoableAction {
        @Override
        public void apply() {
            Tweaks.apply(change);
        }

        @Override
        public void undo() {
            Tweaks.undo(change);
        }

        @Override
        public String describe() {
            return what;
        }

        @Override
        public String describeUndo() {
            return "Undoing: " + what;
        }

        @Override
        public String systemName() {
            return "Mariculture";
        }
    }
}
