package mariculture.platform.event;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.monster.Zombie;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

import mariculture.domain.config.Settings;
import mariculture.domain.fishing.ZombieFishingGear;
import mariculture.platform.ModContent;
import mariculture.platform.Rngs;

/**
 * Zombies (and subclasses, as in legacy) that spawn with a fishing rod and a dyed bucket hat. Rules and the BUG-001
 * fix live in {@link ZombieFishingGear}; this rolls once for a freshly spawned zombie, after vanilla has given it its
 * own equipment, and never for one loaded from disk.
 */
public final class ZombieGearEvents {
    private ZombieGearEvents() {}

    public static void onJoin(EntityJoinLevelEvent event) {
        if (event.loadedFromDisk() || event.getLevel().isClientSide || !(event.getEntity() instanceof Zombie zombie)) return;
        ZombieFishingGear.roll(Settings.ZOMBIE_GEAR_CHANCE.probability(), zombie.getMainHandItem().isEmpty(), zombie.getItemBySlot(EquipmentSlot.HEAD).isEmpty(),
                Rngs.of(zombie.getRandom())).ifPresent(gear -> {
            zombie.setItemSlot(EquipmentSlot.MAINHAND, ModContent.stack(gear.rod().itemId()));
            zombie.setItemSlot(EquipmentSlot.HEAD, ModContent.stack(gear.hatItemId()));
            zombie.setDropChance(EquipmentSlot.MAINHAND, (float) Settings.ZOMBIE_ROD_DROP.probability().value());
            zombie.setDropChance(EquipmentSlot.HEAD, (float) Settings.ZOMBIE_HAT_DROP.probability().value());
            zombie.addEffect(new MobEffectInstance(MobEffects.MOVEMENT_SPEED, ZombieFishingGear.SPEED_TICKS, ZombieFishingGear.SPEED_AMPLIFIER));
            zombie.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, ZombieFishingGear.RESISTANCE_TICKS, ZombieFishingGear.RESISTANCE_AMPLIFIER));
            zombie.addEffect(new MobEffectInstance(MobEffects.DAMAGE_BOOST, ZombieFishingGear.STRENGTH_TICKS, ZombieFishingGear.STRENGTH_AMPLIFIER));
        });
    }
}
