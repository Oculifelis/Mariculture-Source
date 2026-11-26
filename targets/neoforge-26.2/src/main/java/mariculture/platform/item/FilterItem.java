package mariculture.platform.item;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import mariculture.domain.factory.ItemFilter;
import mariculture.domain.machine.Stack;
import mariculture.platform.Stacks;

/** Item filter (domain ItemFilter): use with an item in the other hand to add it, sneak-use to empty it. */
public class FilterItem extends Item {
    public FilterItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide()) apply(player, hand, stack);
        return InteractionResult.SUCCESS;
    }

    private static void apply(Player player, InteractionHand hand, ItemStack stack) {
        Stack filter = Stacks.toDomain(stack);
        if (player.isShiftKeyDown()) {
            Stacks.setData(stack, ItemFilter.clear(filter).data());
            return;
        }
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        ItemStack held = player.getItemInHand(other);
        Stack added = held.isEmpty() ? null : ItemFilter.add(filter, Stacks.id(held.getItem()));
        if (added == null) {
            player.sendSystemMessage(Component.translatable("item.mariculture.item_filter.hint").withStyle(ChatFormatting.GRAY));
            return;
        }
        Stacks.setData(stack, added.data());
    }

    private static List<Component> tooltip(ItemStack stack) {
        List<Component> out = new ArrayList<>();
        for (String id : ItemFilter.entries(Stacks.toDomain(stack))) {
            out.add(Component.literal("+ ").append(Component.translatable(BuiltInRegistries.ITEM.getValue(net.minecraft.resources.Identifier.parse(id)).getDescriptionId())).withStyle(ChatFormatting.GOLD));
        }
        out.add(Component.translatable("item.mariculture.item_filter.hint").withStyle(ChatFormatting.GRAY));
        return out;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        for (Component c : tooltip(stack)) lines.accept(c);
    }
}
