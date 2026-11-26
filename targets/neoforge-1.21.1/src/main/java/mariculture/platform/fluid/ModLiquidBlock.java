package mariculture.platform.fluid;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.phys.Vec3;

import mariculture.domain.fluid.FluidEffect;
import mariculture.domain.fluid.FluidSpec;

/** A Mariculture fluid in the world, applying its legacy effect ({@link FluidEffect}). */
public class ModLiquidBlock extends LiquidBlock {
    private final FluidSpec spec;

    public ModLiquidBlock(FlowingFluid fluid, FluidSpec spec, Properties props) {
        super(fluid, props);
        this.spec = spec;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity) {
        super.entityInside(state, level, pos, entity);
        Vec3 m = entity.getDeltaMovement();
        switch (spec.effect()) {
            case HOT -> {
                if (!entity.fireImmune()) entity.lavaHurt();
            }
            case BLOOD -> entity.setDeltaMovement(m.x / 2, Math.max(-0.05, m.y / 2), m.z / 2);
            case CUSTARD -> {
                double y = m.x == 0 && m.z == 0 ? m.y / 5 : m.y / 2;
                entity.setDeltaMovement(m.x / 5, Math.max(-0.05, y), m.z / 5);
            }
            case WIND -> {
                RandomSource r = level.random;
                entity.setDeltaMovement(m.add(0.5 - r.nextDouble(), 0.5 - r.nextDouble(), 0.5 - r.nextDouble()));
            }
            case FISH_OIL -> effect(entity, MobEffects.REGENERATION, 0);
            case ICE -> effect(entity, MobEffects.MOVEMENT_SLOWDOWN, 2);
            case POISON -> effect(entity, MobEffects.POISON, 1);
            case ENDER -> {
                if (level instanceof ServerLevel server) enderTeleport(server, entity, FluidEffect.ENDER_RANGE);
            }
            case MANA -> {
                if (entity instanceof ServerPlayer player && level instanceof ServerLevel server) {
                    BlockPos bed = player.getRespawnPosition();
                    if (bed != null && player.getRespawnDimension() == level.dimension()
                            && player.distanceToSqr(Vec3.atCenterOf(bed)) >= FluidEffect.MANA_MIN_BED_DISTANCE * FluidEffect.MANA_MIN_BED_DISTANCE) {
                        server.playSound(null, pos, SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1f, 1f);
                        player.teleportTo(bed.getX() + 0.5, bed.getY(), bed.getZ() + 0.5);
                        server.playSound(null, bed, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1f, 1f);
                    }
                }
            }
            default -> { }
        }
    }

    private static void effect(Entity entity, Holder<MobEffect> effect, int amplifier) {
        if (entity instanceof LivingEntity living && !living.hasEffect(effect)) {
            living.addEffect(new MobEffectInstance(effect, FluidEffect.EFFECT_TICKS, amplifier, true, true));
        }
    }

    /** BlockEnder / FishEnder: a random spot within +-range at the top solid or liquid block, never into lava. */
    public static void enderTeleport(ServerLevel level, Entity entity, int range) {
        int x = (int) (entity.getX() + level.random.nextInt(range * 2) - range);
        int z = (int) (entity.getZ() + level.random.nextInt(range * 2) - range);
        if (!level.hasChunkAt(new BlockPos(x, 0, z))) return;
        int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
        BlockPos target = new BlockPos(x, y, z);
        if (level.getBlockState(target).is(Blocks.LAVA)) return;
        level.playSound(null, entity.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1f, 1f);
        entity.teleportTo(x + 0.5, y, z + 0.5);
        level.playSound(null, target, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 1f, 1f);
    }

    @Override
    protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        super.randomTick(state, level, pos, random);
        if (random.nextInt(FluidEffect.TICK_EFFECT_ONE_IN) != 0) return;
        switch (spec.effect()) {
            case FLUX -> {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(Vec3.atBottomCenterOf(pos.offset(random.nextInt(5) - random.nextInt(10), 0, random.nextInt(5) - random.nextInt(10))));
                    level.addFreshEntity(bolt);
                }
            }
            case GUNPOWDER -> level.explode(null, pos.getX(), pos.getY(), pos.getZ(), FluidEffect.GUNPOWDER_POWER, false, Level.ExplosionInteraction.BLOCK);
            case CHLOROPHYLL -> {
                BlockPos at = pos.offset(random.nextInt(16) - 8, 0, random.nextInt(16) - 8);
                BlockPos top = level.getHeightmapPos(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, at);
                if (level.getBlockState(top.below()).is(BlockTags.DIRT) && level.isEmptyBlock(top)) {
                    level.setBlockAndUpdate(top, random.nextBoolean() ? Blocks.DANDELION.defaultBlockState() : Blocks.POPPY.defaultBlockState());
                }
            }
            default -> { }
        }
    }
}
