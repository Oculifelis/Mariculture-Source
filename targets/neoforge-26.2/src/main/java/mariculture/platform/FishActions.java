package mariculture.platform;

import net.minecraft.world.entity.EntityTypes;
import java.util.List;
import java.util.Optional;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.skeleton.Skeleton;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

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
                effect(p.effect()).ifPresent(e -> target.addEffect(new MobEffectInstance(e, p.ticks(), p.amplifier(), p.ambient(), true)));
            } else if (a instanceof FishEffects.Fire f) {
                target.igniteForSeconds(f.seconds());
            } else if (a instanceof FishEffects.EnderTeleport t) {
                ModLiquidBlock.enderTeleport(level, target, t.range());
            } else if (a instanceof FishEffects.Lightning) {
                LightningBolt bolt = EntityTypes.LIGHTNING_BOLT.create(level, EntitySpawnReason.TRIGGERED);
                if (bolt != null) {
                    bolt.snapTo(target.position());
                    level.addFreshEntity(bolt);
                }
            } else if (a instanceof FishEffects.Give g) {
                ItemStack stack = Stacks.toLoader(mariculture.domain.machine.Stack.of(g.item(), g.count()));
                if (target instanceof Player player) {
                    if (!player.getInventory().add(stack)) player.drop(stack, false);
                } else {
                    target.spawnAtLocation(level, stack);
                }
            } else if (a instanceof FishEffects.Xp x) {
                if (target instanceof Player player) player.giveExperiencePoints(x.points());
            } else if (a instanceof FishEffects.Feed f) {
                if (target instanceof Player player) player.getFoodData().eat(f.hunger(), f.saturation());
            } else if (a instanceof FishEffects.Damage d) {
                DamageSource source = d.source().equals("wither") ? level.damageSources().wither() : level.damageSources().generic();
                target.hurtServer(level, source, d.amount());
            } else if (a instanceof FishEffects.Heal h) {
                target.heal(h.amount());
            } else if (a instanceof FishEffects.Explode e) {
                level.explode(null, target.getX(), target.getY() + 1, target.getZ(), e.power(),
                        e.destroyBlocks() ? Level.ExplosionInteraction.MOB : Level.ExplosionInteraction.NONE);
            } else if (a instanceof FishEffects.WitherSkeleton && target instanceof Skeleton skeleton) {
                skeleton.convertTo(EntityTypes.WITHER_SKELETON, ConversionParams.single(skeleton, true, true), wither -> {
                    wither.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.STONE_SWORD));
                    if (wither.getAttribute(Attributes.ATTACK_DAMAGE) != null) wither.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(4.0);
                });
            }
        }
    }

    public static Optional<? extends Holder<MobEffect>> effect(String id) {
        return BuiltInRegistries.MOB_EFFECT.get(Identifier.parse(id));
    }
}
