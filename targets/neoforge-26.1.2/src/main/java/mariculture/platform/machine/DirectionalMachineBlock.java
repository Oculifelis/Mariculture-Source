package mariculture.platform.machine;

import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import mariculture.domain.content.BlockSpec;

/** A machine that faces any of six ways, like a piston (legacy geyser: BlockPistonBase.determineOrientation). */
public class DirectionalMachineBlock extends MachineBlock {
    public static final EnumProperty<Direction> FACING_ALL = BlockStateProperties.FACING;

    public DirectionalMachineBlock(Properties props, BlockSpec spec) {
        super(props, spec);
        registerDefaultState(stateDefinition.any().setValue(FACING_ALL, Direction.UP));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING_ALL);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING_ALL, ctx.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected Direction modelFacing(BlockState state) {
        return state.getValue(FACING_ALL);
    }
}
