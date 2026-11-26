package mariculture.platform.item;

import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
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
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != BlockHitResult.Type.BLOCK || !level.getFluidState(hit.getBlockPos()).isSource()) return InteractionResult.PASS;
        BlockHitResult above = hit.withPosition(hit.getBlockPos().above());
        return super.useOn(new UseOnContext(player, hand, above));
    }
}
