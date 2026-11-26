package mariculture.platform.item;

import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.PickaxeItem;
import net.minecraft.world.item.Tier;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import mariculture.domain.metal.Hammer;
import mariculture.platform.Stacks;

/** The hammer: a brick-tier pickaxe, three times faster on stone (domain Hammer); area mining in HammerEvents. */
public class HammerItem extends PickaxeItem {
    static final Tier BRICK = new Tier() {
        @Override public int getUses() { return Hammer.USES; }
        @Override public float getSpeed() { return Hammer.SPEED; }
        @Override public float getAttackDamageBonus() { return Hammer.DAMAGE; }
        @Override public TagKey<Block> getIncorrectBlocksForDrops() { return BlockTags.INCORRECT_FOR_STONE_TOOL; }
        @Override public int getEnchantmentValue() { return Hammer.ENCHANTABILITY; }
        @Override public Ingredient getRepairIngredient() { return Ingredient.of(Stacks.item("mariculture:burnt_brick")); }
    };

    public HammerItem(Properties props) {
        super(BRICK, props.attributes(PickaxeItem.createAttributes(BRICK, 1, -2.8f)));
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return state.is(Blocks.STONE) && speed > 1 ? speed * Hammer.STONE_SPEED_FACTOR : speed;
    }
}
