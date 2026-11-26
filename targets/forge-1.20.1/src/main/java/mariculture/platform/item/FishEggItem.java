package mariculture.platform.item;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import mariculture.domain.fish.FishText;
import mariculture.platform.Stacks;
import mariculture.platform.Texts;

/** Fish eggs (legacy ItemEgg): the tooltip shows how many eggs are left to hatch. */
public class FishEggItem extends Item {
    public FishEggItem(Properties props) {
        super(props);
    }

    @Override
    public void appendHoverText(ItemStack stack, net.minecraft.world.level.Level level, List<Component> lines, TooltipFlag flag) {
        if (Stacks.data(stack).isEmpty()) return;
        for (FishText.Line l : FishText.eggLines(Stacks.data(stack))) lines.add(Texts.line(l));
    }
}
