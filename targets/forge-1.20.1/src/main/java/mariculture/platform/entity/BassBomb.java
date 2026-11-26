package mariculture.platform.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.projectile.ThrowableItemProjectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;

import mariculture.domain.fish.FishEffects;
import mariculture.platform.ModContent;

/** A thrown raw bass (legacy EntityBass): hurts what it hits, then explodes. */
public class BassBomb extends ThrowableItemProjectile {
    public BassBomb(EntityType<? extends BassBomb> type, Level level) {
        super(type, level);
    }

    public BassBomb(Level level, LivingEntity thrower) {
        super(ModContent.bassBombType(), thrower, level);
    }

    @Override
    protected Item getDefaultItem() {
        return ModContent.stack("raw_bass").getItem();
    }

    @Override
    protected void onHitEntity(EntityHitResult result) {
        super.onHitEntity(result);
        result.getEntity().hurt(damageSources().thrown(this, getOwner()), FishEffects.BASS_BOMB_DAMAGE);
    }

    @Override
    protected void onHit(HitResult result) {
        super.onHit(result);
        if (!level().isClientSide) {
            level().explode(null, getX(), getY(), getZ(), FishEffects.BASS_BOMB_POWER, Level.ExplosionInteraction.MOB);
            discard();
        }
    }
}
