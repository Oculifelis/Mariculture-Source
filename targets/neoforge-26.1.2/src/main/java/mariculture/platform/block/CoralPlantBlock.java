package mariculture.platform.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SimpleWaterloggedBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

import mariculture.domain.content.Ids;
import mariculture.domain.worldgen.Coral;
import mariculture.platform.ModContent;

/** Dried or living coral (domain Coral): a waterloggable cross plant on coral ground; living coral spreads on coral rock. */
public class CoralPlantBlock extends Block implements SimpleWaterloggedBlock {
    public static final BooleanProperty WATERLOGGED = BlockStateProperties.WATERLOGGED;
    public static final TagKey<Block> GROUND = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Ids.MOD_ID, "coral_ground"));
    /** Legacy bounds 0.125..0.875 across, full height. */
    private static final VoxelShape SHAPE = Block.box(2, 0, 2, 14, 16, 14);

    private final Coral.Colour colour;
    private final boolean living;

    public CoralPlantBlock(Properties props, String id) {
        super(props);
        this.living = id.startsWith("live_");
        this.colour = colourOf(id);
        registerDefaultState(stateDefinition.any().setValue(WATERLOGGED, true));
    }

    static Coral.Colour colourOf(String id) {
        for (Coral.Colour c : Coral.Colour.values()) if (id.equals(c.dried()) || id.equals(c.living())) return c;
        throw new IllegalArgumentException("not a coral: " + id);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(WATERLOGGED);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(WATERLOGGED, ctx.getLevel().getFluidState(ctx.getClickedPos()).getType() == Fluids.WATER);
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    protected FluidState getFluidState(BlockState state) {
        return state.getValue(WATERLOGGED) ? Fluids.WATER.getSource(false) : super.getFluidState(state);
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(GROUND);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction dir, BlockPos neighborPos, BlockState neighbor, RandomSource random) {
        if (state.getValue(WATERLOGGED)) ticks.scheduleTick(pos, Fluids.WATER, Fluids.WATER.getTickDelay(level));
        // BlockCoral.onNeighborBlockChange: dropped as an item when the ground goes; break next tick so it drops.
        if (!state.canSurvive(level, pos)) ticks.scheduleTick(pos, this, 1);
        return state;
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!state.canSurvive(level, pos)) level.destroyBlock(pos, true);
    }

    @Override
    protected boolean isRandomlyTicking(BlockState state) {
        return living;
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        Coral.spreadTick(Coral.Settings.LEGACY, colour, new Coral.SpreadWorld() {
            @Override
            public boolean onCoralRock() {
                return level.getBlockState(pos.below()).is(ModContent.block("coral_rock"));
            }

            @Override
            public Coral.Colour livingAt(int dx, int dy, int dz) {
                return level.getBlockState(pos.offset(dx, dy, dz)).getBlock() instanceof CoralPlantBlock c && c.living ? c.colour : null;
            }

            @Override
            public boolean canGrowAt(int dx, int dy, int dz) {
                BlockPos at = pos.offset(dx, dy, dz);
                return level.getBlockState(at).is(net.minecraft.world.level.block.Blocks.WATER) && level.getFluidState(at.above()).is(FluidTags.WATER)
                        && level.getBlockState(at.below()).is(GROUND);
            }

            @Override
            public void grow(int dx, int dy, int dz, Coral.Colour c) {
                level.setBlock(pos.offset(dx, dy, dz), ModContent.block(c.living()).defaultBlockState(), 2);
            }
        }, OysterBlock.rng(random));
    }
}
