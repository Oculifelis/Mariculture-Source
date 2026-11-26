package mariculture.platform.block;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

import mariculture.domain.fishery.NeonLamp;

/** Neon lamp: an inverted redstone lamp; rules in core-domain NeonLamp. */
public final class NeonLampBlock extends Block {
    public static final BooleanProperty LIT = BlockStateProperties.LIT;

    public NeonLampBlock(BlockBehaviour.Properties properties) {
        super(properties.lightLevel(s -> s.getValue(LIT) ? NeonLamp.LIGHT : 0));
        registerDefaultState(defaultBlockState().setValue(LIT, true));
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(LIT, !context.getLevel().hasNeighborSignal(context.getClickedPos()));
    }

    @Override
    public void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, BlockPos fromPos, boolean moving) {
        if (level.isClientSide) return;
        switch (NeonLamp.onChange(state.getValue(LIT), level.hasNeighborSignal(pos))) {
            case TURN_OFF -> level.setBlock(pos, state.setValue(LIT, false), 2);
            case SCHEDULE_ON -> level.scheduleTick(pos, this, NeonLamp.RELIGHT_DELAY);
            case NONE -> { }
        }
    }

    @Override
    public void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        boolean lit = state.getValue(LIT);
        if (NeonLamp.litAfterTick(lit, level.hasNeighborSignal(pos)) != lit) level.setBlock(pos, state.setValue(LIT, true), 2);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(LIT);
    }
}
