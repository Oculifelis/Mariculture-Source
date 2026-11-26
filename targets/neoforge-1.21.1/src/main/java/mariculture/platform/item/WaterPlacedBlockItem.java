package mariculture.platform.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.BlockHitResult;

/** A block item placed normally, or above still water when used in the air (legacy autofisher). */
public class WaterPlacedBlockItem extends BlockItem {
    public WaterPlacedBlockItem(Block block, Properties properties) {
        super(block, properties);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != BlockHitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).isSource()) return InteractionResultHolder.pass(stack);
        BlockHitResult above = hit.withPosition(hit.getBlockPos().above());
        return new InteractionResultHolder<>(super.useOn(new UseOnContext(player, hand, above)), stack);
    }
}
