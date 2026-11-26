package mariculture.platform.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import mariculture.domain.machine.Stack;
import mariculture.domain.machine.TemperatureControl;
import mariculture.platform.Stacks;

/** Temperature control: takes a heating/cooling upgrade from the other hand, sneak-use returns the last (domain TemperatureControl). */
public class TemperatureControlItem extends Item {
    public TemperatureControlItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        Stack control = Stacks.toDomain(stack);
        if (player.isShiftKeyDown()) {
            String last = TemperatureControl.last(control);
            if (last == null) return InteractionResultHolder.pass(stack);
            if (!level.isClientSide) {
                player.setItemInHand(hand, Stacks.toLoader(TemperatureControl.withoutLast(control)));
                ItemStack back = Stacks.toLoader(Stack.of(last));
                if (!player.getInventory().add(back)) player.drop(back, false);
            }
            return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
        }
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack upgrade = player.getItemInHand(other);
        Stack filled = TemperatureControl.add(control, Stacks.toDomain(upgrade).withCount(1));
        if (filled == null) return InteractionResultHolder.pass(stack);
        if (!level.isClientSide) {
            player.setItemInHand(hand, Stacks.toLoader(filled));
            upgrade.shrink(1);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    @Override
    public void appendHoverText(ItemStack stack, Level level, List<Component> lines, TooltipFlag flag) {
        Stack control = Stacks.toDomain(stack);
        int t = TemperatureControl.temperature(control);
        lines.add(Component.translatable("item.mariculture.temperature_control.contents", TemperatureControl.contents(control).size(), TemperatureControl.SLOTS)
                .withStyle(ChatFormatting.DARK_GREEN));
        if (t != 0) lines.add(Component.literal((t > 0 ? "+" : "") + t + "\u00b0C").withStyle(t > 0 ? ChatFormatting.GOLD : ChatFormatting.BLUE));
        lines.add(Component.translatable("item.mariculture.temperature_control.hint").withStyle(ChatFormatting.GRAY));
    }
}
