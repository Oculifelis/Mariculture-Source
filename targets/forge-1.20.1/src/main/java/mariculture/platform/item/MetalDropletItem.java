package mariculture.platform.item;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;

import mariculture.domain.fishery.Droplets;
import mariculture.domain.machine.Stack;
import mariculture.platform.Stacks;

/** One droplet for any metal fluid (Droplets.METAL): named after the fluid in its item data and tinted its colour. */
public class MetalDropletItem extends Item {
    public MetalDropletItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        Droplets.Droplet d = Droplets.of(Stacks.toDomain(stack));
        ResourceLocation key = d == null ? null : ResourceLocation.tryParse(d.fluid());
        if (key == null || !BuiltInRegistries.FLUID.containsKey(key)) return super.getName(stack);
        Fluid f = BuiltInRegistries.FLUID.get(key);
        if (f == Fluids.EMPTY) return super.getName(stack);
        return Component.translatable("item.mariculture.metal_droplet.named", f.getFluidType().getDescription());
    }

    /** Tint for layer 0 from the droplet's data (registered in ModClient). */
    public static int tint(ItemStack stack, int layer) {
        return layer == 0 ? 0xFF000000 | Droplets.color(Stacks.toDomain(stack)) : -1;
    }
}
