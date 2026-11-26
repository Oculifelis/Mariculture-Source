package mariculture.platform.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import mariculture.platform.ModContent;

/** Holds the oyster's one item (TileOyster). Rules live in mariculture.domain.pearl.Oyster. */
public class OysterBlockEntity extends BlockEntity {
    private ItemStack contents = ItemStack.EMPTY;

    public OysterBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.oysterEntity(), pos, state);
    }

    /** Registry id of the contents, or null when empty. */
    public String contentsId() {
        return contents.isEmpty() ? null : idOf(contents);
    }

    public void setContents(String itemId) {
        contents = itemId == null ? ItemStack.EMPTY : new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse(itemId)));
        setChanged();
    }

    /** Pops the contents out above the oyster and empties it. */
    public void dropContents(Level level) {
        if (contents.isEmpty()) return;
        BlockPos pos = getBlockPos();
        Containers.dropItemStack(level, pos.getX(), pos.getY() + 0.5, pos.getZ(), contents);
        contents = ItemStack.EMPTY;
        setChanged();
    }

    public static String idOf(ItemStack stack) {
        return stack.isEmpty() ? null : BuiltInRegistries.ITEM.getKey(stack.getItem()).toString();
    }

    @Override
    protected void loadAdditional(ValueInput in) {
        super.loadAdditional(in);
        contents = in.read("Item", ItemStack.CODEC).orElse(ItemStack.EMPTY);
    }

    @Override
    protected void saveAdditional(ValueOutput out) {
        super.saveAdditional(out);
        if (!contents.isEmpty()) out.store("Item", ItemStack.CODEC, contents);
    }

    /** Since 1.21.5 blocks no longer get onRemove; the block entity drops its own contents. */
    @Override
    public void preRemoveSideEffects(BlockPos pos, BlockState state) {
        super.preRemoveSideEffects(pos, state);
        if (level != null) dropContents(level);
    }
}
