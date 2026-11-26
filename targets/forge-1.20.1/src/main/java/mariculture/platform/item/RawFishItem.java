package mariculture.platform.item;

import java.util.List;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import mariculture.domain.fish.FishEffects;
import mariculture.platform.FishActions;
import mariculture.platform.entity.BassBomb;

/**
 * A raw (dead) fish of one species (legacy ItemVanillaFish metas): eaten with its species effect; raw bass are
 * thrown as bombs, raw angelfish pull nearby items, raw rainbow trout are a one-hit weapon.
 */
public class RawFishItem extends Item {
    private final String species;

    public RawFishItem(Properties props, String species) {
        super(props);
        this.species = species;
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        switch (FishEffects.rightClick(species)) {
            case THROW_BASS_BOMB -> {
                level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.SNOWBALL_THROW, SoundSource.NEUTRAL, 0.5f,
                        0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
                if (!level.isClientSide) {
                    BassBomb bomb = new BassBomb(level, player);
                    bomb.setItem(stack);
                    bomb.shootFromRotation(player, player.getXRot(), player.getYRot(), 0f, 1.5f, 1f);
                    level.addFreshEntity(bomb);
                }
                if (!player.getAbilities().instabuild) stack.shrink(1);
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            case PULL_ITEMS -> {
                if (!level.isClientSide) {
                    int r = FishEffects.ANGELFISH_PULL_RANGE;
                    List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, player.getBoundingBox().inflate(r));
                    for (ItemEntity item : items) item.setPos(player.getX(), player.getY(), player.getZ());
                }
                return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
            }
            default -> {
                return super.use(level, player, hand);
            }
        }
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        boolean day = level.isDay();
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (level instanceof ServerLevel) FishActions.apply(FishEffects.eaten(species, day), entity);
        return result;
    }

    /** Raw rainbow trout hit for +10 attack damage in the main hand (FishTrout.getAttributeModifiers). */
    @Override
    @SuppressWarnings("deprecation")
    public com.google.common.collect.Multimap<net.minecraft.world.entity.ai.attributes.Attribute, net.minecraft.world.entity.ai.attributes.AttributeModifier>
            getDefaultAttributeModifiers(net.minecraft.world.entity.EquipmentSlot slot) {
        if (!FishEffects.isWeapon(species) || slot != net.minecraft.world.entity.EquipmentSlot.MAINHAND) return super.getDefaultAttributeModifiers(slot);
        return com.google.common.collect.ImmutableMultimap.of(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE,
                new net.minecraft.world.entity.ai.attributes.AttributeModifier(BASE_ATTACK_DAMAGE_UUID, "Weapon modifier",
                        FishEffects.TROUT_ATTACK_DAMAGE, net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADDITION));
    }

    @Override
    public boolean hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
        if (FishEffects.isWeapon(species)) stack.shrink(1);
        return super.hurtEnemy(stack, target, attacker);
    }
}
