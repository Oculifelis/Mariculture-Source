package mariculture.platform.item;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import mariculture.domain.fish.FishText;
import mariculture.domain.maritech.Dna;
import mariculture.platform.Stacks;
import mariculture.platform.Texts;

/** Fish DNA (domain Dna): the gene it carries in the tooltip and its remaining injections as a bar. */
public class DnaItem extends Item {
    public DnaItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return Dna.isDna(Stacks.toDomain(stack));
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return Dna.bar(Stacks.toDomain(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0x40C0FF;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        for (FishText.Line l : Dna.lines(Stacks.toDomain(stack))) lines.accept(Texts.line(l));
    }
}
