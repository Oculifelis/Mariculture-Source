package mariculture.platform.entity;

import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import mariculture.domain.maritech.Fludd;
import mariculture.platform.ModContent;

/** The F.L.U.D.D.'s squirt (legacy EntityFLUDDSquirt): a trail of cloud, 10 damage to what it hits, a splash. Not drawn. */
public class FluddSquirt extends ThrowableItemProjectile {
    public FluddSquirt(EntityType<? extends FluddSquirt> type, Level level) {
        super(type, level);
    }

    public FluddSquirt(Level level, LivingEntity thrower) {
        super(ModContent.fluddSquirtType(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return Items.WATER_BUCKET;
    }

    @Override
    public void tick() {
        super.tick();
        if (level().isClientSide()) {
            Vec3 m = getDeltaMovement();
            for (int l = 0; l < 15; l++) level().addParticle(ParticleTypes.CLOUD, getX() + m.x * l / 5, getY() - 0.5, getZ() + m.z * l / 5, 0, 0, 0);
        }
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        if (level() instanceof ServerLevel) result.getEntity().hurt(damageSources().thrown(this, getOwner()), Fludd.SQUIRT_DAMAGE);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (level() instanceof ServerLevel server) {
            server.sendParticles(ParticleTypes.SPLASH, getX(), getY(), getZ(), 8, 0.1, 0.1, 0.1, 0);
            discard();
        }
    }
}
