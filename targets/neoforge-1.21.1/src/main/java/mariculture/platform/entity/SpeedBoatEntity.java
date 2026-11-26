package mariculture.platform.entity;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.animal.Squid;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.vehicle.Boat;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import mariculture.domain.maritech.SpeedBoat;

/** maritech/entity/EntitySpeedBoat.java: the rules live in {@link SpeedBoat}; this moves, steers and hits things. */
public class SpeedBoatEntity extends Entity {
    /** Legacy drew it 0.1 below posY, which sat half the hull (0.3) above its bottom. */
    public static final float RENDER_LIFT = SpeedBoat.HEIGHT / 2 - 0.1f;
    private final SpeedBoat.Motor motor = new SpeedBoat.Motor();
    private double multiplier = SpeedBoat.START_MULTIPLIER;
    public float motorPos;

    public SpeedBoatEntity(EntityType<? extends SpeedBoatEntity> type, Level level) {
        super(type, level);
        blocksBuilding = true;
    }

    public static ItemStack item() {
        return new ItemStack(BuiltInRegistries.ITEM.get(ResourceLocation.parse(SpeedBoat.ITEM)));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {}

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {}

    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYRot, lerpXRot;

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        lerpX = x;
        lerpY = y;
        lerpZ = z;
        lerpYRot = yRot;
        lerpXRot = xRot;
        lerpSteps = 10;
    }

    /** Boat.tickLerp: others' boats glide to where the server says. */
    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            syncPacketPositionCodec(getX(), getY(), getZ());
        }
        if (lerpSteps > 0) {
            double x = getX() + (lerpX - getX()) / lerpSteps, y = getY() + (lerpY - getY()) / lerpSteps, z = getZ() + (lerpZ - getZ()) / lerpSteps;
            setYRot(getYRot() + (float) Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps);
            setXRot(getXRot() + (float) (lerpXRot - getXRot()) / lerpSteps);
            lerpSteps--;
            setPos(x, y, z);
            setRot(getYRot(), getXRot());
        }
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return Boat.canVehicleCollide(this, entity);
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public ItemStack getPickResult() {
        return item();
    }

    @Override
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity rider ? rider : null;
    }

    @Override
    protected Vec3 getPassengerAttachmentPoint(Entity passenger, EntityDimensions dimensions, float scale) {
        double[] o = SpeedBoat.riderOffset(getYRot());
        return new Vec3(o[0], dimensions.height() / 3, o[1]);
    }

    /** Any player's hit picks it up into their inventory (dropped if full, kept in creative); nothing else harms it. */
    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (level().isClientSide() || isRemoved() || !(source.getEntity() instanceof Player player)) return true;
        if (!player.isCreative() && !player.getInventory().add(item())) spawnAtLocation(item());
        discard();
        return true;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (getFirstPassenger() instanceof Player other && other != player) return InteractionResult.PASS;
        if (!level().isClientSide()) player.startRiding(this);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void tick() {
        super.tick();
        tickLerp();
        if (level().isClientSide()) motorPos = motor.tick(isVehicle());
        double before = getDeltaMovement().horizontalDistance();
        if (before > SpeedBoat.SPLASH_SPEED) splash(before);
        if (isControlledByLocalInstance()) {
            LivingEntity rider = getControllingPassenger();
            Vec3 m = getDeltaMovement();
            SpeedBoat.State s = SpeedBoat.drive(new SpeedBoat.State(m.x, m.y, m.z, multiplier), new SpeedBoat.Input(water(), rider != null,
                    rider == null ? 0 : rider.getYRot(), rider == null ? 0 : rider.xxa, rider == null ? 0 : rider.zza, onGround()));
            double px = getX(), pz = getZ();
            move(MoverType.SELF, new Vec3(s.x(), s.y(), s.z()));
            s = SpeedBoat.coast(s, rider != null);
            multiplier = s.multiplier();
            setDeltaMovement(s.x(), s.y(), s.z());
            setYRot(SpeedBoat.turn(getYRot(), px, pz, getX(), getZ()));
            setXRot(0);
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
        if (level() instanceof ServerLevel server) hitThings(server);
    }

    /** How much of the hull is in water, in fifths (EntitySpeedBoat.java:182-190). */
    private double water() {
        AABB box = getBoundingBox();
        double h = box.maxY - box.minY, in = 0;
        for (int i = 0; i < SpeedBoat.SLICES; i++) {
            double lo = box.minY + h * i / SpeedBoat.SLICES - 0.125, hi = box.minY + h * (i + 1) / SpeedBoat.SLICES - 0.125;
            if (waterIn(new AABB(box.minX, lo, box.minZ, box.maxX, hi, box.maxZ))) in += 1.0 / SpeedBoat.SLICES;
        }
        return in;
    }

    private boolean waterIn(AABB box) {
        for (BlockPos pos : BlockPos.betweenClosed(Mth.floor(box.minX), Mth.floor(box.minY), Mth.floor(box.minZ),
                Mth.floor(box.maxX), Mth.floor(box.maxY), Mth.floor(box.maxZ))) {
            FluidState fluid = level().getFluidState(pos);
            if (fluid.is(FluidTags.WATER) && pos.getY() + fluid.getHeight(level(), pos) >= box.minY) return true;
        }
        return false;
    }

    private void splash(double speed) {
        if (!level().isClientSide()) return;
        double c = Math.cos(getYRot() * Math.PI / 180), s = Math.sin(getYRot() * Math.PI / 180);
        Vec3 m = getDeltaMovement();
        for (int j = 0; j < 1 + speed * 60; j++) {
            double a = random.nextFloat() * 2 - 1, b = (random.nextInt(2) * 2 - 1) * 0.7;
            double x, z;
            if (random.nextBoolean()) {
                x = getX() - c * a * 0.8 + s * b;
                z = getZ() - s * a * 0.8 - c * b;
            } else {
                x = getX() + c + s * a * 0.7;
                z = getZ() + s - c * a * 0.7;
            }
            level().addParticle(ParticleTypes.SPLASH, x, getY() + SpeedBoat.HEIGHT / 2 - 0.125, z, m.x, m.y, m.z);
        }
    }

    /** Lily pads around it break, squid beside it take 25, vanilla boats get shoved (EntitySpeedBoat.java:307-374). */
    private void hitThings(ServerLevel server) {
        for (int i = -1; i < 2; i++) {
            for (int k = -1; k < 2; k++) {
                BlockPos pos = BlockPos.containing(getX() + i, getY() + SpeedBoat.HEIGHT / 2, getZ() + k);
                if (server.getBlockState(pos).is(Blocks.LILY_PAD)) server.destroyBlock(pos, true);
            }
        }
        for (Squid squid : server.getEntitiesOfClass(Squid.class, getBoundingBox().inflate(1))) {
            squid.hurt(damageSources().inWall(), SpeedBoat.SQUID_DAMAGE);
        }
        List<Entity> near = server.getEntities(this, getBoundingBox().inflate(0.2, 0, 0.2));
        for (Entity e : near) if (e instanceof Boat && !hasPassenger(e) && e.isPushable()) e.push(this);
    }
}
