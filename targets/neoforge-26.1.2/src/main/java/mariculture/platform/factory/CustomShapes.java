package mariculture.platform.factory;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.SlabType;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import mariculture.domain.factory.CustomFaces;
import mariculture.domain.factory.Plans;
import mariculture.platform.Stacks;

/**
 * The sawmill's blocks (factory/blocks/BlockCustom*.java): vanilla shapes that keep their faces in a {@link
 * CustomBlockEntity}. Each drops itself with its faces (a double slab two), none in creative; hardness and blast
 * resistance are the average of the faces; mobs never spawn on them.
 */
public final class CustomShapes {
    private CustomShapes() {}

    public static Block create(String id, BlockBehaviour.Properties props) {
        Plans.Type type = null;
        for (Plans.Type t : Plans.Type.values()) if (t.block.equals(id)) type = t;
        if (type == null) throw new IllegalArgumentException("not a sawmill block: " + id);
        props = props.noOcclusion().isValidSpawn((s, l, p, e) -> false);
        return switch (type) {
            case FLOOR -> new Floor(props);
            case BLOCK, RF -> new Cube(props);
            case LIGHT -> new Cube(props.lightLevel(s -> 15));
            case STAIRS -> new Stairs(props);
            case SLABS -> new Slab(props);
            case FENCE -> new Fence(props);
            case GATE -> new Gate(props);
            case WALL, RF_WALL -> new Wall(props);
        };
    }

    /** The shared behaviour; every shape forwards to these. */
    public interface Custom extends EntityBlock {
        @Override
        default BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new CustomBlockEntity(pos, state);
        }
    }

    static void placed(Level level, BlockPos pos, ItemStack stack) {
        if (level.getBlockEntity(pos) instanceof CustomBlockEntity be) be.setFaces(CustomFaces.fromData(Stacks.data(stack)));
    }

    static List<ItemStack> drops(BlockState state, LootParams.Builder params) {
        BlockEntity be = params.getOptionalParameter(LootContextParams.BLOCK_ENTITY);
        int count = state.hasProperty(SlabBlock.TYPE) && state.getValue(SlabBlock.TYPE) == SlabType.DOUBLE ? 2 : 1;
        return be instanceof CustomBlockEntity c ? List.of(c.asItem(count)) : List.of();
    }

    static float progress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        float hardness = level.getBlockEntity(pos) instanceof CustomBlockEntity be ? be.average(false) : 1.5f;
        return hardness <= 0 ? 1f : player.getDestroySpeed(state) / hardness / 30f;
    }

    static float resistance(BlockGetter level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof CustomBlockEntity be ? be.average(true) : 1.5f;
    }

    static ItemStack cloneStack(LevelReader level, BlockPos pos, BlockState state) {
        return level.getBlockEntity(pos) instanceof CustomBlockEntity be ? be.asItem(1) : new ItemStack(state.getBlock());
    }

    public static final class Cube extends Block implements Custom {
        Cube(Properties p) {
            super(p);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
            super.setPlacedBy(level, pos, state, by, stack);
            placed(level, pos, stack);
        }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return drops(state, params);
        }

        @Override
        protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
            return progress(state, player, level, pos);
        }

        @Override
        public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
            return resistance(level, pos);
        }

        @Override
        protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return cloneStack(level, pos, state);
        }
    }

    public static final class Floor extends CarpetBlock implements Custom {
        Floor(Properties p) {
            super(p);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
            super.setPlacedBy(level, pos, state, by, stack);
            placed(level, pos, stack);
        }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return drops(state, params);
        }

        @Override
        protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
            return progress(state, player, level, pos);
        }

        @Override
        public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
            return resistance(level, pos);
        }

        @Override
        protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return cloneStack(level, pos, state);
        }
    }

    public static final class Stairs extends StairBlock implements Custom {
        Stairs(Properties p) {
            super(Blocks.STONE.defaultBlockState(), p);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
            super.setPlacedBy(level, pos, state, by, stack);
            placed(level, pos, stack);
        }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return drops(state, params);
        }

        @Override
        protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
            return progress(state, player, level, pos);
        }

        @Override
        public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
            return resistance(level, pos);
        }

        @Override
        protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return cloneStack(level, pos, state);
        }
    }

    public static final class Slab extends SlabBlock implements Custom {
        Slab(Properties p) {
            super(p);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
            super.setPlacedBy(level, pos, state, by, stack);
            placed(level, pos, stack);
        }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return drops(state, params);
        }

        @Override
        protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
            return progress(state, player, level, pos);
        }

        @Override
        public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
            return resistance(level, pos);
        }

        @Override
        protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return cloneStack(level, pos, state);
        }
    }

    public static final class Fence extends FenceBlock implements Custom {
        Fence(Properties p) {
            super(p);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
            super.setPlacedBy(level, pos, state, by, stack);
            placed(level, pos, stack);
        }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return drops(state, params);
        }

        @Override
        protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
            return progress(state, player, level, pos);
        }

        @Override
        public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
            return resistance(level, pos);
        }

        @Override
        protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return cloneStack(level, pos, state);
        }
    }

    public static final class Gate extends FenceGateBlock implements Custom {
        Gate(Properties p) {
            super(WoodType.OAK, p);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
            super.setPlacedBy(level, pos, state, by, stack);
            placed(level, pos, stack);
        }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return drops(state, params);
        }

        @Override
        protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
            return progress(state, player, level, pos);
        }

        @Override
        public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
            return resistance(level, pos);
        }

        @Override
        protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return cloneStack(level, pos, state);
        }
    }

    public static final class Wall extends WallBlock implements Custom {
        Wall(Properties p) {
            super(p);
        }

        @Override
        public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack stack) {
            super.setPlacedBy(level, pos, state, by, stack);
            placed(level, pos, stack);
        }

        @Override
        protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
            return drops(state, params);
        }

        @Override
        protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
            return progress(state, player, level, pos);
        }

        @Override
        public float getExplosionResistance(BlockState state, BlockGetter level, BlockPos pos, Explosion explosion) {
            return resistance(level, pos);
        }

        @Override
        protected ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state, boolean includeData) {
            return cloneStack(level, pos, state);
        }
    }
}
