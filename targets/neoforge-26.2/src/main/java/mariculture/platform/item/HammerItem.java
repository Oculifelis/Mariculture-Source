package mariculture.platform.item;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import mariculture.domain.metal.Hammer;

/** The hammer: a brick-tier pickaxe, three times faster on stone (domain Hammer); area mining in HammerEvents. */
public class HammerItem extends Item {
    static final ToolMaterial BRICK = new ToolMaterial(BlockTags.INCORRECT_FOR_STONE_TOOL, Hammer.USES, Hammer.SPEED, Hammer.DAMAGE,
            Hammer.ENCHANTABILITY, TagKey.create(Registries.ITEM, Identifier.parse("mariculture:hammer_repair")));

    public HammerItem(Properties props) {
        super(props.pickaxe(BRICK, 1, -2.8f));
    }

    @Override
    public float getDestroySpeed(ItemStack stack, BlockState state) {
        float speed = super.getDestroySpeed(stack, state);
        return state.is(Blocks.STONE) && speed > 1 ? speed * Hammer.STONE_SPEED_FACTOR : speed;
    }
}
