package mariculture.platform.item;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

import mariculture.domain.fish.FishEffects;
import mariculture.domain.fish.FishText;
import mariculture.domain.fish.LiveFish;
import mariculture.platform.Stacks;
import mariculture.platform.Texts;

/** A live fish (legacy ItemFishy): its name and tooltip come from the genome in its item data. */
public class LiveFishItem extends Item {
    public LiveFishItem(Properties props) {
        super(props);
    }

    @Override
    public Component getName(ItemStack stack) {
        FishText.Name n = FishText.name(Stacks.data(stack));
        if (n == null) return super.getName(stack);
        MutableComponent c = n.hybrid()
                ? Component.translatable("fish.mariculture.hybrid", Component.translatable(n.upperKey()), Component.translatable(n.lowerKey()))
                : Component.translatable(n.upperKey());
        return c.append(n.symbol()).withStyle(ChatFormatting.AQUA);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        for (FishText.Line l : FishText.lines(Stacks.data(stack))) lines.accept(Texts.line(l));
    }

    @Override
    public int getEntityLifespan(ItemStack stack, Level level) {
        FishText.Name n = FishText.name(Stacks.data(stack));
        return n == null ? super.getEntityLifespan(stack, level) : FishEffects.onLandLifespanTicks(n.species());
    }

    /** Creative-menu fish carry no genome; the first time one sits in a player's inventory it becomes a wild fish. */
    @Override
    public void inventoryTick(ItemStack stack, net.minecraft.server.level.ServerLevel level, net.minecraft.world.entity.Entity entity,
                              net.minecraft.world.entity.EquipmentSlot slot) {
        if (!Stacks.data(stack).isEmpty()) return;
        String species = "mariculture:" + Stacks.id(this).substring("mariculture:".length());
        var sp = LiveFish.registry().species(species);
        if (sp == null) return;
        var fish = LiveFish.stack(mariculture.domain.fish.Breeding.makePure(sp, mariculture.platform.Rngs.of(level.getRandom())));
        Stacks.setData(stack, fish.data());
    }
}
