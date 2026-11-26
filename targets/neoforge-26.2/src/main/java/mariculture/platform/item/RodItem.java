package mariculture.platform.item;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.FishingRodItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.network.chat.Component;
import net.minecraft.ChatFormatting;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

import mariculture.domain.content.Ids;
import mariculture.domain.content.ItemKind;
import mariculture.domain.fishery.Bait;
import mariculture.domain.maritech.EnergyItem;
import mariculture.platform.Stacks;

/**
 * A Mariculture fishing rod (legacy ItemRod with FishingHandler.handleRightClick). Since 1.21.2 durability,
 * enchantability and the anvil repair material are item components. It only casts with bait its tier may use in
 * the hotbar slot beside it, uses one bait per cast, and better bait shortens the wait.
 */
public class RodItem extends FishingRodItem {
    private static final int FLUX_CAST = mariculture.domain.maritech.Autofisher.FLUX_CAST;
    private final ItemKind.Rod spec;

    public RodItem(Properties props, String id, ItemKind.Rod spec) {
        super(spec.usesEnergy() ? props.stacksTo(1) : props.durability(spec.durability()).enchantable(spec.enchantability())
                .repairable(ItemTags.create(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "repairs/" + id))));
        this.spec = spec;
    }

    public ItemKind.Rod spec() {
        return spec;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack rod = player.getItemInHand(hand);
        if (spec.tier() == mariculture.domain.fish.Species.Rod.BLOOD) {
            Boolean done = BoundRodUse.use(level, player, hand, rod);
            if (done != null) return done ? InteractionResult.SUCCESS : InteractionResult.FAIL;
        }
        if (player.fishing != null) {
            if (!level.isClientSide()) drain(rod);
            return super.use(level, player, hand);
        }
        if (!charged(rod)) return InteractionResult.FAIL;
        var inv = player.getInventory();
        int slot = Bait.findSlot(inv.getSelectedSlot(), i -> Stacks.id(inv.getItem(i).getItem()), spec.tier());
        if (slot < 0) return InteractionResult.FAIL;
        int quality = Bait.quality(Stacks.id(inv.getItem(slot).getItem()));
        level.playSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.FISHING_BOBBER_THROW, SoundSource.NEUTRAL, 0.5f,
                0.4f / (level.getRandom().nextFloat() * 0.4f + 0.8f));
        if (level instanceof ServerLevel server) {
            int lure = (int) (EnchantmentHelper.getFishingTimeReduction(server, rod, player) * 20f) + Bait.lureTicks(quality);
            int luck = EnchantmentHelper.getFishingLuckBonus(server, rod, player);
            Projectile.spawnProjectile(new mariculture.platform.entity.LavaHook(player, level, luck, lure, lure, mariculture.domain.fishing.FishingSpeed.forRod(Stacks.id(rod.getItem()))), server, rod);
            if (!player.getAbilities().instabuild) inv.getItem(slot).shrink(1);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        rod.causeUseVibration(player, GameEvent.ITEM_INTERACT_START);
        return InteractionResult.SUCCESS;
    }

    /** The flux rod (ItemFluxRod): no durability; 100 FE a cast from its own charge, shown as a bar like a battery. */
    private boolean charged(ItemStack rod) {
        return !spec.usesEnergy() || EnergyItem.stored(Stacks.toDomain(rod)) >= FLUX_CAST;
    }

    private void drain(ItemStack rod) {
        if (!spec.usesEnergy()) return;
        Stacks.setData(rod, EnergyItem.extract(Stacks.toDomain(rod), FLUX_CAST, false).stack().data());
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return spec.usesEnergy() || super.isBarVisible(stack);
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return spec.usesEnergy() ? EnergyItem.bar(Stacks.toDomain(stack)) : super.getBarWidth(stack);
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return spec.usesEnergy() ? 0xFF3030 : super.getBarColor(stack);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        if (spec.tier() == mariculture.domain.fish.Species.Rod.BLOOD) {
            lines.accept(Component.translatable(BoundRodUse.active(stack) ? "item.mariculture.bound_fishing_rod.activated"
                    : "item.mariculture.bound_fishing_rod.deactivated").withStyle(ChatFormatting.GRAY));
        }
        if (!spec.usesEnergy()) return;
        var s = Stacks.toDomain(stack);
        lines.accept(Component.translatable("item.mariculture.battery.charge", EnergyItem.stored(s), EnergyItem.spec(s).capacity()).withStyle(ChatFormatting.GRAY));
    }
}
