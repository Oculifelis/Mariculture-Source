package mariculture.platform.block;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

import mariculture.domain.content.Face;
import mariculture.domain.content.Pedestal;

/** Limestone pedestal: its bordered top faces the clicked face, or up/down near a side's edges. */
public class PedestalBlock extends DirectionalBlock {
    public PedestalBlock(Properties props) {
        super(props);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.UP));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction clicked = ctx.getClickedFace();
        double hitY = ctx.getClickLocation().y - ctx.getClickedPos().getY();
        // getClickedPos is the new block's position; for side clicks it shares the clicked block's Y.
        Face facing = Pedestal.facing(Face.values()[clicked.get3DDataValue()], (float) hitY);
        return defaultBlockState().setValue(FACING, Direction.from3DDataValue(facing.ordinal()));
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }
}
