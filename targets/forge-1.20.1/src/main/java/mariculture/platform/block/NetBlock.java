package mariculture.platform.block;


import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.WaterlilyBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import mariculture.domain.fish.LiveFish;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.machine.Stack;
import mariculture.platform.Env;
import mariculture.platform.Rngs;
import mariculture.platform.Stacks;

/** A fishing net on water (legacy BlockTicking): random ticks now and then land a fish on top of it. */
public class NetBlock extends WaterlilyBlock {
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 1, 16);

    public NetBlock(Properties props) {
        super(props);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public boolean isRandomlyTicking(BlockState state) {
        return true;
    }

    @Override
    public void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Stack caught = FishingRules.net(Env.dimensionKind(level), pos.getY(), level.isDay(), Env.salinity(level, pos), Env.temperature(level, pos),
                LiveFish.registry(), Rngs.of(random));
        if (caught == null) return;
        ItemEntity item = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, Stacks.toLoader(caught));
        level.addFreshEntity(item);
    }
}
