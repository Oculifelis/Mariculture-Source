package mariculture.platform.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import mariculture.platform.Stacks;

/** Food that leaves an item behind (a bowl), legacy ItemFood with a container; 1.20.1 has no "converts to" property. */
public class FoodItem extends Item {
    private final String remainder;

    public FoodItem(Properties props, String remainder) {
        super(props);
        this.remainder = remainder;
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (remainder == null || (entity instanceof Player p && p.getAbilities().instabuild)) return result;
        ItemStack left = new ItemStack(Stacks.item(remainder));
        if (result.isEmpty()) return left;
        if (entity instanceof Player p && !p.getInventory().add(left)) p.drop(left, false);
        return result;
    }
}
