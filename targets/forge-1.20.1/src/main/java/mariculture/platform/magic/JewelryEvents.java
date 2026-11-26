package mariculture.platform.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import java.util.UUID;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Silverfish;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingFallEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import mariculture.domain.content.Ids;
import mariculture.domain.fishery.Bait;
import mariculture.domain.magic.Elemental;
import mariculture.domain.magic.JewelryEffects;
import mariculture.domain.magic.JewelryEnchantment;
import mariculture.domain.magic.JewelryMaterial;
import mariculture.domain.magic.JewelryTick;
import mariculture.domain.magic.WornJewelry;
import mariculture.platform.Rngs;
import mariculture.platform.Stacks;

/** What worn jewelry does (core-domain JewelryTick, JewelryEffects and Elemental), applied to the player. */
public final class JewelryEvents {
    private static final DeferredRegister<MobEffect> EFFECTS = DeferredRegister.create(ForgeRegistries.MOB_EFFECTS, Ids.MOD_ID);
    /** Spiderman: the client climbs walls while the player has it. */
    public static final RegistryObject<MobEffect> SPIDER = EFFECTS.register("spider", () -> new MobEffect(MobEffectCategory.BENEFICIAL, 0x4A4A4A) {});
    private static final UUID SPEED_ID = UUID.fromString("6f3c58a2-0b3e-4d0e-9c55-7a1f0c2b9e11");
    private static final UUID STEP_ID = UUID.fromString("9b1d2e47-55a8-4c63-8f2e-3d6a7c4b0f22");

    private JewelryEvents() {}

    public static void register(IEventBus modBus) {
        EFFECTS.register(modBus);
        MinecraftForge.EVENT_BUS.addListener(JewelryEvents::onTick);
        MinecraftForge.EVENT_BUS.addListener(JewelryEvents::onFall);
        MinecraftForge.EVENT_BUS.addListener(JewelryEvents::onDamage);
        MinecraftForge.EVENT_BUS.addListener(JewelryEvents::onDeath);
        MinecraftForge.EVENT_BUS.addListener(JewelryEvents::onBreak);
        MinecraftForge.EVENT_BUS.addListener(JewelryEvents::onJump);
        MinecraftForge.EVENT_BUS.addListener(JewelryEvents::onRespawn);
        MinecraftForge.EVENT_BUS.addListener(WornStore::onClone);
    }

    private static int strength(List<WornJewelry.Piece> worn, JewelryEnchantment e) {
        return WornJewelry.strength(worn, e.id());
    }

    private static void hidden(Player p, MobEffect effect, int amplifier) {
        p.addEffect(new MobEffectInstance(effect, 40, amplifier, true, false, false));
    }

    private static void modifier(Player p, net.minecraft.world.entity.ai.attributes.Attribute attr, UUID id, double amount,
            AttributeModifier.Operation op) {
        AttributeInstance a = p.getAttribute(attr);
        if (a == null) return;
        AttributeModifier old = a.getModifier(id);
        if (old != null && old.getAmount() == amount) return;
        if (old != null) a.removeModifier(id);
        if (amount != 0) a.addTransientModifier(new AttributeModifier(id, "Mariculture jewelry", amount, op));
    }

    // ---- every tick ------------------------------------------------------------------------

    private static void onTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player p = event.player;
        if (p.level().isClientSide()) {
            // EnchantmentSpider: against a wall the player climbs, sideways speed capped.
            if (p.hasEffect(SPIDER.get()) && p.horizontalCollision) {
                Vec3 v = p.getDeltaMovement();
                double cap = JewelryEffects.SPIDER_CAP;
                p.setDeltaMovement(Math.max(-cap, Math.min(cap, v.x)), JewelryEffects.SPIDER_CLIMB, Math.max(-cap, Math.min(cap, v.z)));
                p.resetFallDistance();
            }
            return;
        }
        WornStore.Worn w = WornStore.get(p);
        List<WornJewelry.Piece> worn = WornStore.pieces(p);
        JewelryTick.Plan plan = JewelryTick.plan(new JewelryTick.State(p.level().getGameTime(), p.isSprinting(), p.onGround(), p.isInWater(),
                p.horizontalCollision, p.getAbilities().flying, p.isCreative() || p.isSpectator(), p.getFoodData().getFoodLevel()),
                id -> WornJewelry.strength(worn, id));
        modifier(p, Attributes.MOVEMENT_SPEED, SPEED_ID, plan.speed(), AttributeModifier.Operation.MULTIPLY_BASE);
        modifier(p, ForgeMod.STEP_HEIGHT_ADDITION.get(), STEP_ID, plan.stepBonus(), AttributeModifier.Operation.ADDITION);
        if (plan.jumpAmplifier() >= 0) hidden(p, MobEffects.JUMP, plan.jumpAmplifier());
        if (plan.glide() && !p.isShiftKeyDown()) hidden(p, MobEffects.SLOW_FALLING, 0);
        if (plan.spiderAmplifier() >= 0) hidden(p, SPIDER.get(), plan.spiderAmplifier());
        flight(p, w, plan.mayFly());
        if (plan.feedSaturation() >= 0) p.getFoodData().eat(1, plan.feedSaturation());
        int repaired = repair(p, plan.repair());
        if (repaired > 0) WornStore.wear(p, JewelryEnchantment.REPAIR.id(), repaired, null);
        plan.wear().forEach((id, points) -> WornStore.wear(p, id, points, null));
        oneRing(p, w, strength(worn, JewelryEnchantment.ONE_RING) > 0);
    }

    /** EnchantmentFlight: may fly from level 2; taking it off ends flight (not for creative or spectators). */
    private static void flight(Player p, WornStore.Worn w, boolean mayFly) {
        if (p.isCreative() || p.isSpectator()) return;
        if (mayFly && !p.getAbilities().mayfly) {
            p.getAbilities().mayfly = true;
            p.getAbilities().setFlyingSpeed(JewelryEffects.FLIGHT_SPEED * 2);
            p.onUpdateAbilities();
            w.flight = true;
        } else if (!mayFly && w.flight) {
            p.getAbilities().mayfly = false;
            p.getAbilities().flying = false;
            p.getAbilities().setFlyingSpeed(0.05f);
            p.onUpdateAbilities();
            w.flight = false;
        }
    }

    /** EnchantmentRestore: one point on the held item, the hotbar from level 2, armour from level 3. Returns points. */
    private static int repair(Player p, JewelryEffects.Repair r) {
        int points = 0;
        if (r.held()) points += mend(p.getMainHandItem(), 1);
        if (r.hotbar()) for (int i = 0; i < 9; i++) if (i != p.getInventory().selected) points += mend(p.getInventory().getItem(i), 1);
        if (r.armourPoints() > 0) {
            for (var slot : net.minecraft.world.entity.EquipmentSlot.values()) {
                if (slot.getType() == net.minecraft.world.entity.EquipmentSlot.Type.ARMOR) points += mend(p.getItemBySlot(slot), r.armourPoints());
            }
        }
        return points;
    }

    private static int mend(ItemStack s, int points) {
        if (s.isEmpty() || !s.isDamageableItem() || !s.isDamaged()) return 0;
        int fixed = Math.min(points, s.getDamageValue());
        s.setDamageValue(s.getDamageValue() - fixed);
        return fixed;
    }

    /** The One Ring makes its wearer invisible; taking it off brings the withdrawal (EnchantmentOneRing.activate). */
    private static void oneRing(Player p, WornStore.Worn w, boolean on) {
        if (on) hidden(p, MobEffects.INVISIBILITY, 0);
        if (w.oneRing && !on) {
            JewelryEffects.RingWithdrawal r = JewelryEffects.oneRingWithdrawal(p.level().getDifficulty().getId());
            if (r != null && !p.isCreative()) {
                p.addEffect(new MobEffectInstance(MobEffects.HUNGER, r.hungerTicks()));
                p.addEffect(new MobEffectInstance(MobEffects.WITHER, r.witherTicks()));
                p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, r.weaknessTicks()));
                p.getFoodData().eat(r.food(), 0);
                p.getFoodData().setSaturation(Math.max(0, p.getFoodData().getSaturationLevel() + r.saturation()));
                p.hurt(p.damageSources().magic(), r.damage());
            }
        }
        if (w.oneRing != on) {
            w.oneRing = on;
            WornStore.changed(p);
        }
    }

    // ---- falls, jumps -----------------------------------------------------------------------

    private static void onFall(LivingFallEvent event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide()) return;
        List<WornJewelry.Piece> worn = WornStore.pieces(p);
        if (event.getDistance() >= 1 && strength(worn, JewelryEnchantment.GLIDE) > 0) WornStore.wear(p, JewelryEnchantment.GLIDE.id(), 1, null);
        JewelryEffects.Fall f = JewelryEffects.fall(event.getDistance(), strength(worn, JewelryEnchantment.FALL));
        if (f.wear() > 0) WornStore.wear(p, JewelryEnchantment.FALL.id(), f.wear(), null);
        if (f.cancel()) event.setCanceled(true);
        else event.setDistance(f.distance());
    }

    private static void onJump(LivingEvent.LivingJumpEvent event) {
        if (!(event.getEntity() instanceof Player p) || p.level().isClientSide()) return;
        if (strength(WornStore.pieces(p), JewelryEnchantment.JUMP) <= 0) return;
        WornStore.Worn w = WornStore.get(p);
        w.jumps++;
        if (JewelryTick.jumpWears(w.jumps)) WornStore.wear(p, JewelryEnchantment.JUMP.id(), 1, null);
        else WornStore.changed(p);
    }

    // ---- damage ----------------------------------------------------------------------------

    private static Elemental.Damage kind(DamageSource s) {
        if (s.is(DamageTypeTags.IS_DROWNING)) return Elemental.Damage.DROWN;
        if (s.is(DamageTypes.MAGIC) || s.is(DamageTypes.INDIRECT_MAGIC)) return Elemental.Damage.MAGIC;
        if (s.is(DamageTypeTags.IS_FIRE)) return Elemental.Damage.FIRE;
        if (s.is(DamageTypes.CACTUS)) return Elemental.Damage.CACTUS;
        if (s.is(DamageTypes.FALLING_BLOCK) || s.is(DamageTypes.FALLING_ANVIL) || s.is(DamageTypes.FALLING_STALACTITE)) return Elemental.Damage.FALLING_BLOCK;
        if (s.is(DamageTypes.IN_WALL)) return Elemental.Damage.IN_WALL;
        return Elemental.Damage.OTHER;
    }

    private static void onDamage(LivingHurtEvent event) {
        LivingEntity target = event.getEntity();
        if (target.level().isClientSide()) return;
        var rng = Rngs.of(target.level().getRandom());
        if (event.getSource().getEntity() instanceof Player attacker && attacker != target) {
            for (JewelryMaterial m : Elemental.active(WornStore.pieces(attacker))) {
                Elemental.Attack a = Elemental.attack(m, attacker.level().isDay(), attacker.getHealth() < attacker.getMaxHealth(), event.getAmount());
                if (a.extraMagicFactor() > 0) event.setAmount(event.getAmount() * a.extraMagicFactor());
                if (a.heal() > 0) attacker.heal(a.heal());
                if (Elemental.wears(a.wearOneIn(), rng)) WornStore.wear(attacker, JewelryEnchantment.ELEMENTAL.id(), 1, m);
            }
        }
        if (!(target instanceof Player p)) return;
        List<WornJewelry.Piece> worn = WornStore.pieces(p);
        Elemental.Damage kind = kind(event.getSource());
        for (JewelryMaterial m : Elemental.active(worn)) {
            Elemental.Defence d = Elemental.defend(m, kind, p.hasEffect(MobEffects.POISON));
            if (d.extinguish()) p.clearFire();
            if (d.curePoison()) p.removeEffect(MobEffects.POISON);
            if (Elemental.wears(d.wearOneIn(), rng)) WornStore.wear(p, JewelryEnchantment.ELEMENTAL.id(), 1, m);
            if (d.cancel()) {
                event.setCanceled(true);
                return;
            }
        }
        int health = strength(worn, JewelryEnchantment.HEALTH);
        float restored = JewelryEffects.oneUpHealth(p.getHealth(), event.getAmount(), health);
        if (restored > 0) {
            event.setCanceled(true);
            p.setHealth(Math.min(p.getMaxHealth(), restored));
            WornStore.wear(p, JewelryEnchantment.HEALTH.id(), health, null);
        }
    }

    // ---- kills, deaths ---------------------------------------------------------------------

    private static void onDeath(LivingDeathEvent event) {
        LivingEntity dead = event.getEntity();
        if (!(dead.level() instanceof ServerLevel level)) return;
        if (event.getSource().getEntity() instanceof Player killer && killer != dead) {
            var rng = Rngs.of(level.getRandom());
            for (JewelryMaterial m : Elemental.active(WornStore.pieces(killer))) {
                Elemental.Kill k = Elemental.kill(m, dead instanceof Silverfish, rng);
                if (k.goldNugget()) drop(level, dead.blockPosition(), new ItemStack(Items.GOLD_NUGGET));
                if (k.xpOrb() >= 0) ExperienceOrb.award(level, dead.position(), k.xpOrb());
                if (k.fishingCatch()) drop(level, dead.blockPosition(), new ItemStack(Items.COOKED_COD));
                if (Elemental.wears(k.wearOneIn(), rng)) WornStore.wear(killer, JewelryEnchantment.ELEMENTAL.id(), 1, m);
            }
        }
        if (dead instanceof ServerPlayer p) resurrect(p, event.getSource());
    }

    private static void drop(ServerLevel level, BlockPos pos, ItemStack stack) {
        level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack));
    }

    /** Reaper: the inventory is kept for the respawn (and from level 2 the respawn is where the player died). */
    private static void resurrect(ServerPlayer p, DamageSource source) {
        int strength = strength(WornStore.pieces(p), JewelryEnchantment.RESURRECTION);
        boolean unblockable = source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.BYPASSES_ARMOR);
        JewelryEffects.Resurrection r = JewelryEffects.resurrection(strength, unblockable);
        if (!r.keepInventory()) return;
        WornStore.Worn w = WornStore.get(p);
        w.kept = new ArrayList<>();
        var inv = p.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) w.kept.add(inv.removeItemNoUpdate(i));
        w.respawn = r.respawnAtDeath() ? p.level().dimension().location() + " " + p.getX() + " " + p.getY() + " " + p.getZ() : "";
        w.resist = r.invulnerableTicks();
        w.resurrectWear = r.wear();
        WornStore.changed(p);
    }

    /**
     * Whether worn jewelry stays in its Curios slots on death: always, unless the Enchantments config "Jewelry Drops on
     * Death" is on (off by default); a Reaper wearer who keeps the inventory keeps the jewelry too.
     */
    public static boolean keepsJewelry(net.minecraft.world.entity.LivingEntity wearer, DamageSource source) {
        if (!mariculture.domain.config.Settings.JEWELRY_DROPS.get()) return true;
        if (!(wearer instanceof ServerPlayer p)) return false;
        boolean unblockable = source.is(DamageTypeTags.IS_FIRE) || source.is(DamageTypeTags.BYPASSES_ARMOR);
        return JewelryEffects.resurrection(strength(WornStore.pieces(p), JewelryEnchantment.RESURRECTION), unblockable).keepInventory();
    }

    private static void onRespawn(PlayerEvent.PlayerRespawnEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer p) || event.isEndConquered()) return;
        WornStore.Worn w = WornStore.get(p);
        if (w.kept.isEmpty() && w.resurrectWear == 0) return;
        var inv = p.getInventory();
        for (int i = 0; i < w.kept.size() && i < inv.getContainerSize(); i++) {
            ItemStack s = w.kept.get(i);
            if (s.isEmpty()) continue;
            if (inv.getItem(i).isEmpty()) inv.setItem(i, s);
            else p.getInventory().placeItemBackInInventory(s);
        }
        if (!w.respawn.isEmpty()) {
            String[] parts = w.respawn.split(" ");
            ServerLevel level = p.server.getLevel(ResourceKey.create(Registries.DIMENSION, new ResourceLocation(parts[0])));
            if (level != null) p.teleportTo(level, Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3]),
                    p.getYRot(), p.getXRot());
        }
        if (w.resist > 0) p.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, w.resist, 4));
        int wear = w.resurrectWear;
        w.kept = new ArrayList<>();
        w.respawn = "";
        w.resist = 0;
        w.resurrectWear = 0;
        WornStore.wear(p, JewelryEnchantment.RESURRECTION.id(), wear, null);
        WornStore.changed(p);
    }

    // ---- blocks ----------------------------------------------------------------------------

    /** Earthworm Jim (brown pearl): dirt and grass also drop the sifter's dirt catch (BUG-060 fixed). */
    private static void onBreak(BlockEvent.BreakEvent event) {
        Player p = event.getPlayer();
        if (p == null || !(event.getLevel() instanceof ServerLevel level) || p.isCreative()) return;
        boolean dirt = event.getState().is(Blocks.DIRT) || event.getState().is(Blocks.GRASS_BLOCK);
        if (!dirt) return;
        var rng = Rngs.of(level.getRandom());
        for (JewelryMaterial m : Elemental.active(WornStore.pieces(p))) {
            if (m != JewelryMaterial.BROWN) continue;
            String item = event.getState().is(Blocks.DIRT) ? "minecraft:dirt" : "minecraft:grass_block";
            int drops = 0;
            ItemStack block = new ItemStack(event.getState().getBlock().asItem());
            List<Bait.SifterRecipe> recipes = Bait.sifterRecipes(item,
                    t -> block.is(net.minecraft.tags.ItemTags.create(new ResourceLocation(t))));
            for (Map.Entry<String, Integer> e : Bait.sift(recipes, rng).entrySet()) {
                var it = Stacks.item(e.getKey());
                if (it == null || e.getValue() <= 0) continue;
                BlockPos pos = event.getPos();
                level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, new ItemStack(it, e.getValue())));
                drops += e.getValue();
            }
            if (Elemental.wears(Elemental.breakWear(m, true, drops), rng)) WornStore.wear(p, JewelryEnchantment.ELEMENTAL.id(), 1, m);
        }
    }
}
