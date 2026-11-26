package mariculture.platform.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.BreedGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.FollowParentGoal;
import net.minecraft.world.entity.ai.goal.LeapAtTargetGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.animal.squid.Squid;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import org.jspecify.annotations.Nullable;

import mariculture.domain.content.Ids;
import mariculture.domain.machine.Stack;
import mariculture.platform.ModContent;
import mariculture.platform.Rngs;
import mariculture.platform.Stacks;

/** The Rockhopper penguin (domain Rockhopper): tameable with fish, hunts squid, breathes underwater. */
public class Rockhopper extends TamableAnimal {
    /** Raw and cooked fish: tempt, breed, tame and heal. */
    public static final TagKey<net.minecraft.world.item.Item> FOOD = TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(Ids.MOD_ID, "rockhopper_food"));
    public static final TagKey<Block> SPAWNS_ON = TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Ids.MOD_ID, "rockhopper_spawnable_on"));

    public Rockhopper(EntityType<? extends Rockhopper> type, Level level) {
        super(type, level);
        setTame(false, false);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createAnimalAttributes()
                .add(Attributes.MAX_HEALTH, mariculture.domain.worldgen.Rockhopper.MAX_HEALTH)
                .add(Attributes.MOVEMENT_SPEED, mariculture.domain.worldgen.Rockhopper.MOVEMENT_SPEED)
                .add(Attributes.ATTACK_DAMAGE, mariculture.domain.worldgen.Rockhopper.ATTACK_DAMAGE)
                .add(Attributes.STEP_HEIGHT, mariculture.domain.worldgen.Rockhopper.STEP_HEIGHT);
    }

    /** Animal spawn rules on the beach blocks of #mariculture:rockhopper_spawnable_on. */
    public static boolean checkSpawnRules(EntityType<Rockhopper> type, LevelAccessor level, EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        return level.getBlockState(pos.below()).is(SPAWNS_ON) && isBrightEnoughToSpawn(level, pos);
    }

    @Override
    protected void registerGoals() {
        // EntityRockhopper constructor, same priorities.
        goalSelector.addGoal(0, new FloatGoal(this));
        goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
        goalSelector.addGoal(2, new BreedGoal(this, 1.0));
        goalSelector.addGoal(3, new TemptGoal(this, 1.0, s -> s.is(FOOD), false));
        goalSelector.addGoal(3, new LeapAtTargetGoal(this, 0.4f));
        goalSelector.addGoal(4, new FollowParentGoal(this, 1.1));
        goalSelector.addGoal(4, new MeleeAttackGoal(this, 1.0, true));
        goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0));
        goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 10.0f, 2.0f));
        goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 6.0f));
        goalSelector.addGoal(7, new RandomLookAroundGoal(this));
        targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Squid.class, 750, false, false, null));
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(FOOD);
    }

    @Override
    public boolean canBreatheUnderwater() {
        return true;
    }

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        return target.hurtServer(level, damageSources().mobAttack(this), mariculture.domain.worldgen.Rockhopper.ATTACK_DAMAGE);
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float damage) {
        if (isInvulnerableTo(level, source)) return false;
        setOrderedToSit(false);
        Entity by = source.getEntity();
        float d = mariculture.domain.worldgen.Rockhopper.incomingDamage(damage, by != null, by instanceof Player || by instanceof AbstractArrow);
        return super.hurtServer(level, source, d);
    }

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (isTame()) {
            if (isFood(stack) && getHealth() < getMaxHealth()) {
                feed(player, hand, stack, mariculture.domain.worldgen.Rockhopper.FISH_HEAL, mariculture.domain.worldgen.Rockhopper.FISH_HEAL);
                return InteractionResult.SUCCESS;
            }
            InteractionResult result = super.mobInteract(player, hand);
            if (!result.consumesAction() && isOwnedBy(player)) {
                setOrderedToSit(!isOrderedToSit());
                jumping = false;
                navigation.stop();
                setTarget(null);
                return InteractionResult.SUCCESS.withoutItem();
            }
            return result;
        } else if (isFood(stack)) {
            // Taming was never finished in legacy (EntityTameable with no way to tame): one fish in three, like a wolf's bone.
            if (level().isClientSide()) return InteractionResult.SUCCESS;
            stack.consume(1, player);
            if (random.nextInt(3) == 0 && !net.neoforged.neoforge.event.EventHooks.onAnimalTame(this, player)) {
                tame(player);
                navigation.stop();
                setTarget(null);
                setOrderedToSit(true);
                level().broadcastEntityEvent(this, (byte) 7);
            } else {
                level().broadcastEntityEvent(this, (byte) 6);
            }
            return InteractionResult.SUCCESS_SERVER;
        }
        return super.mobInteract(player, hand);
    }

    @Override
    public @Nullable AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        Rockhopper baby = ModContent.rockhopperType().create(level, EntitySpawnReason.BREEDING);
        if (baby != null && isTame()) {
            baby.setOwnerReference(getOwnerReference());
            baby.setTame(true, true);
        }
        return baby;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return mariculture.domain.worldgen.Rockhopper.MAX_IN_CHUNK;
    }

    @Override
    public boolean removeWhenFarAway(double distSqr) {
        return !isTame() && tickCount > mariculture.domain.worldgen.Rockhopper.DESPAWN_AFTER_TICKS;
    }

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        int looting = source.getEntity() instanceof LivingEntity killer ? level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT)
                .get(Enchantments.LOOTING).map(h -> EnchantmentHelper.getEnchantmentLevel(h, killer)).orElse(0) : 0;
        for (Stack s : mariculture.domain.worldgen.Rockhopper.drops(killedByPlayer, looting, Rngs.of(random))) {
            ItemEntity drop = spawnAtLocation(level, Stacks.toLoader(s), 1f);
            if (drop != null) drop.setDefaultPickUpDelay();
        }
    }

    @Override
    protected @Nullable SoundEvent getAmbientSound() {
        return ModContent.sound("entity.rockhopper.ambient");
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return ModContent.sound("entity.rockhopper.hurt");
    }

    @Override
    protected SoundEvent getDeathSound() {
        return ModContent.sound("entity.rockhopper.death");
    }
}
