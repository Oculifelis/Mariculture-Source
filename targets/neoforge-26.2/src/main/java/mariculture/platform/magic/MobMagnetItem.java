package mariculture.platform.magic;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import mariculture.domain.magic.MobMagnet;
import mariculture.platform.Stacks;

/** Mob Magnet (domain MobMagnet): right-click pulls every mob of the bound type within 32 blocks to the player. */
public class MobMagnetItem extends Item {
    public MobMagnetItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        String bound = MobMagnet.bound(Stacks.toDomain(stack));
        if (bound == null) return InteractionResult.PASS;
        if (!level.isClientSide()) {
            EntityType<?> type = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.Identifier.tryParse(bound)).orElse(null);
            if (type == null) return InteractionResult.PASS;
            int[] t = MobMagnet.target(player.getX(), player.getY(), player.getZ());
            level.playSound(null, t[0], t[1], t[2], SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
            for (LivingEntity e : level.getEntitiesOfClass(LivingEntity.class, player.getBoundingBox().inflate(MobMagnet.RANGE),
                    e -> e.getType() == type && e != player)) {
                e.teleportTo(t[0] + 0.5, t[1], t[2] + 0.5);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, net.minecraft.world.item.TooltipFlag flag) {
        String bound = MobMagnet.bound(Stacks.toDomain(stack));
        EntityType<?> type = bound == null ? null : net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getOptional(net.minecraft.resources.Identifier.tryParse(bound)).orElse(null);
        lines.accept((type == null ? Component.translatable("item.mariculture.mob_magnet.unbound")
                : Component.translatable("item.mariculture.mob_magnet.bound", type.getDescription())).withStyle(ChatFormatting.GRAY));
    }
}
