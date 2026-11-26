package mariculture.platform;

import java.util.List;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Skeleton;
import net.minecraft.world.entity.monster.WitherSkeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

import mariculture.domain.fish.FishEffects;
import mariculture.platform.fluid.ModLiquidBlock;

/** Performs the actions returned by {@link FishEffects} on one living entity. */
public final class FishActions {
    private FishActions() {}

    public static FishEffects.Living view(LivingEntity e) {
        return new FishEffects.Living(e instanceof Player, e instanceof Skeleton, e.getHealth(), e.level().getDifficulty().getId(),
                e instanceof net.minecraft.world.entity.monster.Enemy);
    }

    public static void apply(List<FishEffects.Action> actions, LivingEntity target) {
        if (!(target.level() instanceof ServerLevel level)) return;
        for (FishEffects.Action a : actions) {
            if (a instanceof FishEffects.Potion p) {
                MobEffect e = effect(p.effect());
                if (e != null) target.addEffect(new MobEffectInstance(e, p.ticks(), p.amplifier(), p.ambient(), true));
            } else if (a instanceof FishEffects.Fire f) {
                target.setSecondsOnFire(f.seconds());
            } else if (a instanceof FishEffects.EnderTeleport t) {
                ModLiquidBlock.enderTeleport(level, target, t.range());
            } else if (a instanceof FishEffects.Lightning) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(target.position());
                    level.addFreshEntity(bolt);
                }
            } else if (a instanceof FishEffects.Give g) {
                ItemStack stack = Stacks.toLoader(mariculture.domain.machine.Stack.of(g.item(), g.count()));
                if (target instanceof Player player) {
                    if (!player.getInventory().add(stack)) player.drop(stack, false);
                } else {
                    target.spawnAtLocation(stack);
                }
            } else if (a instanceof FishEffects.Xp x) {
                if (target instanceof Player player) player.giveExperiencePoints(x.points());
            } else if (a instanceof FishEffects.Feed f) {
                if (target instanceof Player player) player.getFoodData().eat(f.hunger(), f.saturation());
            } else if (a instanceof FishEffects.Damage d) {
                DamageSource source = d.source().equals("wither") ? level.damageSources().wither() : level.damageSources().generic();
                target.hurt(source, d.amount());
            } else if (a instanceof FishEffects.Heal h) {
                target.heal(h.amount());
            } else if (a instanceof FishEffects.Explode e) {
                level.explode(null, target.getX(), target.getY() + 1, target.getZ(), e.power(),
                        e.destroyBlocks() ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
            } else if (a instanceof FishEffects.WitherSkeleton && target instanceof Skeleton skeleton) {
                WitherSkeleton wither = skeleton.convertTo(EntityType.WITHER_SKELETON, true);
                if (wither != null) {
                    wither.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
                    if (wither.getAttribute(Attributes.ATTACK_DAMAGE) != null) wither.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0);
                }
            }
        }
    }

    public static MobEffect effect(String id) {
        return ForgeRegistries.MOB_EFFECTS.getValue(new ResourceLocation(id));
    }
}
