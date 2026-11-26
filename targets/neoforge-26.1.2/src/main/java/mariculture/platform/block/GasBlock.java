package mariculture.platform.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.redstone.Orientation;

import mariculture.domain.config.Settings;
import mariculture.domain.fluid.NaturalGas;
import mariculture.platform.Rngs;

/**
 * Natural gas in the world (domain NaturalGas, legacy core/blocks/BlockAir meta 0): a see-through block players walk
 * through and choke in, that burns fast and catches fire from fire, torches or lava next to it.
 */
public class GasBlock extends Block {
    public GasBlock(Properties props) {
        super(props);
    }

    @Override
    protected boolean skipRendering(BlockState state, BlockState neighbor, Direction dir) {
        return neighbor.is(this) || super.skipRendering(state, neighbor, dir);
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects, boolean precise) {
        if (!(entity instanceof Player player) || level.isClientSide()) return;
        for (NaturalGas.Effect e : NaturalGas.effects(Rngs.of(level.getRandom()))) {
            Holder<MobEffect> effect = switch (e.id()) {
                case "mining_fatigue" -> MobEffects.MINING_FATIGUE;
                case "nausea" -> MobEffects.NAUSEA;
                case "poison" -> MobEffects.POISON;
                default -> MobEffects.WEAKNESS;
            };
            player.addEffect(new MobEffectInstance(effect, e.ticks(), e.amplifier(), true, true));
        }
    }

    @Override
    protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block block, Orientation from, boolean moving) {
        super.neighborChanged(state, level, pos, block, from, moving);
        if (level.isClientSide() || !Settings.GAS_CATCHES_FIRE.get()) return;
        for (BlockPos p : BlockPos.betweenClosed(pos.offset(-1, -1, -1), pos.offset(1, 1, 1))) {
            if (!NaturalGas.IGNITERS.contains(BuiltInRegistries.BLOCK.getKey(level.getBlockState(p).getBlock()).toString())) continue;
            for (int[] o : NaturalGas.FIRE_SIDES) {
                BlockPos f = pos.offset(o[0], o[1], o[2]);
                if (level.isEmptyBlock(f)) {
                    level.setBlockAndUpdate(f, Blocks.FIRE.defaultBlockState());
                    return;
                }
            }
            return;
        }
    }

    /** BlockAir: flammability 300, fire spread speed 1000. */
    @Override
    public boolean isFlammable(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return true;
    }

    @Override
    public int getFlammability(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return NaturalGas.FLAMMABILITY;
    }

    @Override
    public int getFireSpreadSpeed(BlockState state, BlockGetter level, BlockPos pos, Direction face) {
        return NaturalGas.FIRE_SPREAD_SPEED;
    }
}
