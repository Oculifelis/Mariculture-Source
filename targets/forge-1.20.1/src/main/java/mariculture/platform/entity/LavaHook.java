package mariculture.platform.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import mariculture.domain.fishing.LavaBite;
import mariculture.platform.Env;
import mariculture.platform.Rngs;
import mariculture.platform.event.FisheryEvents;

/**
 * The hook Mariculture rods cast (fishery/EntityHook.java, FISH-033). Out of lava it is the vanilla hook; in the
 * Nether it floats and fish bite in lava ({@link LavaBite}), and the catch comes out fire-immune. It keeps the vanilla
 * bobber's entity type, so clients draw an ordinary bobber; the lava behaviour runs on the server only. It floats the
 * same way on the fluids in {@code #mariculture:hook_floats_on} (A5), with water's particles.
 *
 * <p>While the fishing speed multiplier is on ({@link mariculture.domain.fishing.FishingSpeed}, legacy 50) the same bite
 * steps run in water too, with the multiplier taken off the wait every tick (EntityHook.java:254).
 */
public class LavaHook extends FishingHook {
    /**
     * Vanilla's FishingHook.DATA_BITING (private): Entity defines data ids 0-7 and FishingHook 8 (hooked entity) and 9.
     * Setting it makes the client bobber dip while a fish bites, as the vanilla hook does; without it players can't see
     * when to reel in.
     */
    private static final EntityDataAccessor<Boolean> BITING = new EntityDataAccessor<>(9, EntityDataSerializers.BOOLEAN);
    private final int lureTicks, speed;
    private LavaBite bite;

    public LavaHook(Player player, Level level, int luck, int lure, int lureTicks, int speed) {
        super(player, level, luck, lure);
        this.lureTicks = lureTicks;
        this.speed = speed;
    }

    @Override
    public boolean fireImmune() {
        return true;
    }

    private boolean inNetherLava() {
        return Env.dimensionKind(level()).equals("nether") && level().getFluidState(blockPosition()).is(FluidTags.LAVA);
    }

    /** A5: another mod's fluid in #mariculture:hook_floats_on (Alex's Caves acid and purple soda), in any dimension. */
    private boolean inModFluid() {
        return level().getFluidState(blockPosition()).is(Env.HOOK_FLOATS_ON);
    }

    private boolean inFastWater() {
        return speed > 0 && getHookedIn() == null && level().getFluidState(blockPosition()).is(FluidTags.WATER);
    }

    @Override
    public void tick() {
        boolean lava = level() instanceof ServerLevel && inNetherLava();
        if (!(level() instanceof ServerLevel level) || !(lava || inModFluid() || inFastWater())) {
            if (bite != null && getEntityData().get(BITING)) getEntityData().set(BITING, false);
            bite = null;
            super.tick();
            return;
        }
        baseTick();
        Player owner = getPlayerOwner();
        if (owner == null || !owner.isAlive() || distanceToSqr(owner) > 1024 || !(owner.getMainHandItem().getItem() instanceof FishingRodItem
                || owner.getOffhandItem().getItem() instanceof FishingRodItem)) {
            discard();
            return;
        }
        if (bite == null) bite = new LavaBite(lureTicks, speed);
        BlockPos pos = blockPosition();
        double surface = pos.getY() + level.getFluidState(pos).getHeight(level, pos);
        LavaBite.Event event = bite.tick(Rngs.of(random), !level.canSeeSky(pos.above()));
        if (getEntityData().get(BITING) != bite.biting()) getEntityData().set(BITING, bite.biting());
        effects(level, event, surface, lava);
        // Vanilla's bobbing (FishingHook.tick, BOBBING): pulled towards the surface, dragged under while a fish bites.
        Vec3 v = getDeltaMovement();
        double d = getY() + v.y - surface;
        if (Math.abs(d) < 0.01) d += Math.signum(d) * 0.1;
        double pull = bite.biting() ? 0.1 * random.nextFloat() * random.nextFloat() : 0;
        setDeltaMovement(v.x * 0.9, v.y - d * random.nextFloat() * 0.2 - pull, v.z * 0.9);
        move(MoverType.SELF, getDeltaMovement());
        updateRotation();
        setDeltaMovement(getDeltaMovement().scale(0.92));
        reapplyPosition();
    }

    private void effects(ServerLevel level, LavaBite.Event event, double surface, boolean lava) {
        if (event == LavaBite.Event.HOOKED) {
            playSound(SoundEvents.FISHING_BOBBER_SPLASH, 0.25f, 1 + (random.nextFloat() - random.nextFloat()) * 0.4f);
            if (lava) level.sendParticles(ParticleTypes.LAVA, getX(), surface, getZ(), 3, getBbWidth(), 0, getBbWidth(), 0.2);
            else level.sendParticles(ParticleTypes.FISHING, getX(), surface, getZ(), 6, getBbWidth(), 0, getBbWidth(), 0.2);
        } else if (event == LavaBite.Event.SWIMMING) {
            double a = Math.toRadians(bite.angle()), r = bite.swimTicks() * 0.1;
            double x = getX() + Math.sin(a) * r, z = getZ() + Math.cos(a) * r;
            if (random.nextFloat() < 0.15f) level.sendParticles(lava ? ParticleTypes.FLAME : ParticleTypes.BUBBLE, x, surface - 0.1, z, 1, 0, 0.05, 0, 0);
            level.sendParticles(lava ? ParticleTypes.SMOKE : ParticleTypes.FISHING, x, surface, z, 1, 0, 0.02, 0, 0);
        }
    }

    @Override
    public int retrieve(ItemStack rod) {
        Player player = getPlayerOwner();
        if (bite == null || !bite.biting() || player == null || getHookedIn() != null || !(level() instanceof ServerLevel)) return super.retrieve(rod);
        FisheryEvents.reelIn(this);
        discard();
        return 1;
    }
}
