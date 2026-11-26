package mariculture.platform.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import mariculture.domain.metal.Anvil;
import mariculture.platform.Stacks;

/** A half-worked anvil item (legacy ItemWorked): named after its result, the bar shows the hits done. */
public class WorkedItem extends Item {
    public WorkedItem(Properties props) {
        super(props);
    }

    private static int value(ItemStack stack, String key, int fallback) {
        return Stacks.data(stack).get(key) instanceof Number n ? n.intValue() : fallback;
    }

    @Override
    public Component getName(ItemStack stack) {
        Object result = Stacks.data(stack).get(Anvil.K_RESULT);
        if (!(result instanceof String id) || Stacks.item(id) == null) return super.getName(stack);
        return Component.translatable("item.mariculture.worked_item.of", new ItemStack(Stacks.item(id)).getHoverName());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Math.round(13f * value(stack, Anvil.K_WORKED, 0) / Math.max(1, value(stack, Anvil.K_REQUIRED, 1)));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFFAA33;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        lines.accept(Component.translatable("item.mariculture.worked_item.hits", value(stack, Anvil.K_WORKED, 0), value(stack, Anvil.K_REQUIRED, 1))
                .withStyle(ChatFormatting.GRAY));
    }
}
