package mariculture.platform.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

import mariculture.platform.Env;

/**
 * Thermometer and dragon spawn egg (core/items/ItemCrafting.onItemUse): the thermometer reports day or night, the
 * temperature (sneaking: the biome's, otherwise the precise one at the block) and the salinity; the egg spawns an ender
 * dragon ten blocks above the player ("Enable Ender Dragon Spawning", default on).
 */
public class UseItem extends Item {
    private final String behaviour;

    public UseItem(Properties props, String behaviour) {
        super(props);
        this.behaviour = behaviour;
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Level level = ctx.getLevel();
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (behaviour.equals("thermometer")) {
            var pos = ctx.getClickedPos();
            int temp = player.isShiftKeyDown() ? Env.biome(level, pos).temperature() : Env.temperature(level, pos);
            player.sendSystemMessage(Component.translatable("item.mariculture.thermometer.time." + (level.isBrightOutside() ? "day" : "night")).withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.translatable(player.isShiftKeyDown() ? "item.mariculture.thermometer.generic" : "item.mariculture.thermometer.precise", temp)
                    .withStyle(ChatFormatting.GRAY));
            player.sendSystemMessage(Component.translatable("item.mariculture.thermometer.salinity",
                    Component.translatable("salinity.mariculture." + Env.salinity(level, pos).name().toLowerCase(java.util.Locale.ROOT))).withStyle(ChatFormatting.GRAY));
            return InteractionResult.SUCCESS;
        }
        if (behaviour.equals("dragon_spawn") && mariculture.domain.config.Settings.DRAGON_SPAWN_EGG.get()) {
            var dragon = net.minecraft.world.entity.EntityType.ENDER_DRAGON.create(level, net.minecraft.world.entity.EntitySpawnReason.SPAWN_ITEM_USE);
            if (dragon != null) {
                dragon.setPos(player.getX(), player.getY() + 10, player.getZ());
                level.addFreshEntity(dragon);
                if (!player.getAbilities().instabuild) ctx.getItemInHand().shrink(1);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }
}
