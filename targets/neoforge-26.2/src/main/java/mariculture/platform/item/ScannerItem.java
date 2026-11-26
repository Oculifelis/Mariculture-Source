package mariculture.platform.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import mariculture.domain.fish.FishText;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.machine.Stack;
import mariculture.platform.Stacks;
import mariculture.platform.Texts;

/** Fish DNA scanner: used with a live fish in the other hand, prints its genes and marks it scanned (domain FishText). */
public class ScannerItem extends Item {
    public ScannerItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        InteractionHand other = hand == InteractionHand.MAIN_HAND ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND;
        Stack fish = Stacks.toDomain(player.getItemInHand(other));
        if (!LiveFish.isLiveFish(fish)) {
            if (!level.isClientSide()) player.sendSystemMessage(Component.translatable("item.mariculture.scanner.hint").withStyle(ChatFormatting.GRAY));
            return InteractionResult.PASS;
        }
        if (!level.isClientSide()) {
            for (FishText.Line l : FishText.scanReport(fish.data())) player.sendSystemMessage(Texts.line(l));
            player.setItemInHand(other, Stacks.toLoader(FishText.scanned(fish)));
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        lines.accept(Component.translatable("item.mariculture.scanner.hint").withStyle(ChatFormatting.GRAY));
    }
}
