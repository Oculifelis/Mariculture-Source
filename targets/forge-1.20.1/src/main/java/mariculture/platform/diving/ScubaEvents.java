package mariculture.platform.diving;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.event.entity.living.LivingAttackEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.TickEvent;

import mariculture.domain.content.Ids;
import mariculture.domain.diving.Scuba;
import mariculture.platform.Stacks;

/** Scuba gear effects (domain Scuba): tank breath, mask lights and wear, fins, suit mining, fire-protected tank. */
public final class ScubaEvents {
    private static final java.util.UUID SWIM = java.util.UUID.fromString("5d8f2c1e-6b4a-4a8e-9f0d-3b1c7e2a9d41");
    private static final java.util.UUID LAND = java.util.UUID.fromString("a2e7c4b9-1f3d-4c6a-8e5b-7d9f0a1b2c63");

    private ScubaEvents() {}

    public static void register() {
        MinecraftForge.EVENT_BUS.addListener(ScubaEvents::onTick);
        MinecraftForge.EVENT_BUS.addListener(ScubaEvents::onBreakSpeed);
        MinecraftForge.EVENT_BUS.addListener(ScubaEvents::onDamage);
    }

    private static boolean wearing(Player p, EquipmentSlot slot, String id) {
        return Stacks.id(p.getItemBySlot(slot).getItem()).equals(Ids.MOD_ID + ":" + id);
    }

    public static boolean fireProtection(Player p, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(Enchantments.FIRE_PROTECTION, stack) > 0;
    }

    /** ScubaEvent.canActivate for the piece in {@code slot}. */
    public static boolean active(Player p, EquipmentSlot slot) {
        return Scuba.active(p.isEyeInFluid(FluidTags.WATER), p.isEyeInFluid(FluidTags.LAVA), fireProtection(p, p.getItemBySlot(slot)));
    }

    private static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = event.player;
        if (p.level().isClientSide) return;
        fins(p);
        if (!wearing(p, EquipmentSlot.HEAD, Scuba.MASK)) return;
        long time = p.level().getGameTime();
        if (time % Scuba.EVERY_TICKS != 0) return;
        ItemStack mask = p.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        Scuba.Check check = Scuba.check(time, Stacks.toDomain(mask), Stacks.toDomain(chest), p.isEyeInFluid(FluidTags.WATER), p.isEyeInFluid(FluidTags.LAVA));
        if (check.fullBreath()) p.setAirSupply(p.getMaxAirSupply());
        if (!check.tank().data().equals(Stacks.data(chest))) Stacks.setData(chest, check.tank().data());
        if (check.maskWear() > 0) mask.hurtAndBreak(check.maskWear(), p, pl -> pl.broadcastBreakEvent(EquipmentSlot.HEAD));
        MobEffectInstance vision = p.getEffect(MobEffects.NIGHT_VISION);
        if (Scuba.lights(active(p, EquipmentSlot.HEAD), Stacks.toDomain(mask))) {
            if (vision == null || vision.getDuration() < Scuba.NIGHT_VISION_TICKS - Scuba.EVERY_TICKS * 2) {
                p.addEffect(new MobEffectInstance(MobEffects.NIGHT_VISION, Scuba.NIGHT_VISION_TICKS, 0, true, false, false));
            }
        } else if (vision != null && vision.isAmbient() && !vision.isVisible() && vision.getDuration() <= Scuba.NIGHT_VISION_TICKS) {
            p.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static void fins(Player p) {
        boolean fins = wearing(p, EquipmentSlot.FEET, Scuba.FINS) && !p.getAbilities().instabuild;
        double swim = fins ? Scuba.finsSwim(active(p, EquipmentSlot.FEET), wearing(p, EquipmentSlot.LEGS, Scuba.SUIT)) : 0;
        double land = fins ? Scuba.finsLand(p.isInWater() || p.isInLava(), p.getAbilities().flying) : 0;
        modifier(p.getAttribute(ForgeMod.SWIM_SPEED.get()), SWIM, swim, "Swimfins");
        modifier(p.getAttribute(Attributes.MOVEMENT_SPEED), LAND, land, "Swimfins on land");
    }

    private static void modifier(AttributeInstance attribute, java.util.UUID id, double amount, String name) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null && old.getAmount() == amount) return;
        if (old != null) attribute.removeModifier(id);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, name, amount, AttributeModifier.Operation.MULTIPLY_BASE));
    }

    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        boolean suit = wearing(p, EquipmentSlot.LEGS, Scuba.SUIT), fins = wearing(p, EquipmentSlot.FEET, Scuba.FINS);
        if (!suit && !fins) return;
        boolean oyster = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).toString().equals(Ids.MOD_ID + ":oyster");
        event.setNewSpeed(Scuba.breakSpeed(event.getNewSpeed(), suit, fins, active(p, EquipmentSlot.LEGS), p.hasCorrectToolForDrops(event.getState()),
                oyster, p.onGround()));
    }

    private static void onDamage(LivingAttackEvent event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide) return;
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        var after = Scuba.fireHit(event.getSource().is(DamageTypeTags.IS_FIRE), wearing(p, EquipmentSlot.HEAD, Scuba.MASK), Stacks.toDomain(chest),
                fireProtection(p, chest));
        if (after == null) return;
        Stacks.setData(chest, after.data());
        p.clearFire();
        if (!p.hasEffect(MobEffects.FIRE_RESISTANCE)) p.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, Scuba.FIRE_RESISTANCE_TICKS, 0, true, true));
        event.setCanceled(true);
    }
}
