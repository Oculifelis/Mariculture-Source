package mariculture.platform.diving;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
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
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import mariculture.domain.content.Ids;
import mariculture.domain.diving.Scuba;
import mariculture.platform.Stacks;

/** Scuba gear effects (domain Scuba): tank breath, mask lights and wear, fins, suit mining, fire-protected tank. */
public final class ScubaEvents {
    private static final Identifier SWIM = Identifier.fromNamespaceAndPath(Ids.MOD_ID, "swimfin_swim");
    private static final Identifier LAND = Identifier.fromNamespaceAndPath(Ids.MOD_ID, "swimfin_land");
    private static final Identifier WARDED_SWIM = Identifier.fromNamespaceAndPath(Ids.MOD_ID, "warded_swim");

    private ScubaEvents() {}

    public static void register() {
        NeoForge.EVENT_BUS.addListener(ScubaEvents::onTick);
        NeoForge.EVENT_BUS.addListener(ScubaEvents::onBreakSpeed);
        NeoForge.EVENT_BUS.addListener(ScubaEvents::onDamage);
    }

    private static boolean wearing(Player p, EquipmentSlot slot, String id) {
        return Stacks.id(p.getItemBySlot(slot).getItem()).equals(Ids.MOD_ID + ":" + id);
    }

    public static boolean fireProtection(Player p, ItemStack stack) {
        if (stack.isEmpty()) return false;
        return p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.FIRE_PROTECTION)
                .map(h -> stack.getEnchantmentLevel(h) > 0).orElse(false);
    }

    /** ScubaEvent.canActivate for the piece in {@code slot}. */
    public static boolean active(Player p, EquipmentSlot slot) {
        return Scuba.active(p.isEyeInFluid(FluidTags.WATER), p.isEyeInFluid(FluidTags.LAVA), fireProtection(p, p.getItemBySlot(slot)));
    }

    private static void onTick(PlayerTickEvent.Post event) {
        Player p = event.getEntity();
        if (p.level().isClientSide()) return;
        fins(p);
        warded(p);
        if (!wearing(p, EquipmentSlot.HEAD, Scuba.MASK)) return;
        long time = p.level().getGameTime();
        if (time % Scuba.EVERY_TICKS != 0) return;
        ItemStack mask = p.getItemBySlot(EquipmentSlot.HEAD);
        ItemStack chest = p.getItemBySlot(EquipmentSlot.CHEST);
        Scuba.Check check = Scuba.check(time, Stacks.toDomain(mask), Stacks.toDomain(chest), p.isEyeInFluid(FluidTags.WATER), p.isEyeInFluid(FluidTags.LAVA));
        if (check.fullBreath()) p.setAirSupply(p.getMaxAirSupply());
        if (!check.tank().data().equals(Stacks.data(chest))) Stacks.setData(chest, check.tank().data());
        if (check.maskWear() > 0) mask.hurtAndBreak(check.maskWear(), p, EquipmentSlot.HEAD);
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
        modifier(p.getAttribute(NeoForgeMod.SWIM_SPEED), SWIM, swim);
        modifier(p.getAttribute(Attributes.MOVEMENT_SPEED), LAND, land);
    }

    /** Whether the piece in {@code slot} is the Mariculture item {@code id} carrying the Thaumaturge warded flag. */
    private static boolean warded(Player p, EquipmentSlot slot, String id) {
        if (!wearing(p, slot, id)) return false;
        var data = p.getItemBySlot(slot).get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        return data != null && data.contains(Scuba.WARDED);
    }

    /** Warded mask, suit and fins worn together: full breath underwater and faster swimming. */
    private static void warded(Player p) {
        boolean set = Scuba.wardedSet(warded(p, EquipmentSlot.HEAD, Scuba.MASK), warded(p, EquipmentSlot.LEGS, Scuba.SUIT),
                warded(p, EquipmentSlot.FEET, Scuba.FINS));
        modifier(p.getAttribute(NeoForgeMod.SWIM_SPEED), WARDED_SWIM, set ? Scuba.WARDED_SWIM : 0);
        if (set && p.isEyeInFluid(FluidTags.WATER) && p.getAirSupply() < p.getMaxAirSupply()) p.setAirSupply(p.getMaxAirSupply());
    }

    private static void modifier(AttributeInstance attribute, Identifier id, double amount) {
        if (attribute == null) return;
        AttributeModifier old = attribute.getModifier(id);
        if (old != null && old.amount() == amount) return;
        if (old != null) attribute.removeModifier(id);
        if (amount != 0) attribute.addTransientModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
    }

    private static void onBreakSpeed(PlayerEvent.BreakSpeed event) {
        Player p = event.getEntity();
        boolean suit = wearing(p, EquipmentSlot.LEGS, Scuba.SUIT), fins = wearing(p, EquipmentSlot.FEET, Scuba.FINS);
        if (!suit && !fins) return;
        boolean oyster = BuiltInRegistries.BLOCK.getKey(event.getState().getBlock()).toString().equals(Ids.MOD_ID + ":oyster");
        event.setNewSpeed(Scuba.breakSpeed(event.getNewSpeed(), suit, fins, active(p, EquipmentSlot.LEGS), p.hasCorrectToolForDrops(event.getState()),
                oyster, p.onGround()));
    }

    private static void onDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide()) return;
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
