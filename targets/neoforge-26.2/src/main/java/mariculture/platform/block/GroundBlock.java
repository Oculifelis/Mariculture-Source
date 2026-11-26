package mariculture.platform.block;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

import mariculture.domain.content.AncientSand;
import mariculture.platform.Rngs;
import mariculture.platform.Stacks;

/** Ancient sand (drops from domain AncientSand) and bubbling sand (bubbles rise through the water above). */
public class GroundBlock extends Block {
    private final boolean ancient;

    public GroundBlock(Properties props, boolean ancient) {
        super(props);
        this.ancient = ancient;
    }

    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        if (!ancient) return super.getDrops(state, params);
        var tool = params.getOptionalParameter(LootContextParams.TOOL);
        int fortune = tool == null ? 0 : params.getLevel().registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                .get(net.minecraft.world.item.enchantment.Enchantments.FORTUNE)
                .map(h -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(h, tool)).orElse(0);
        List<ItemStack> out = new ArrayList<>();
        for (var s : AncientSand.drops(fortune, Rngs.of(params.getLevel().getRandom()))) out.add(Stacks.toLoader(s));
        return out;
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (ancient) return;
        for (int i = 1; i <= 16 && level.getFluidState(pos.above(i)).is(Fluids.WATER); i++) {
            for (int j = 0; j < 2; j++) {
                level.addParticle(ParticleTypes.BUBBLE, pos.getX() + 0.5 + random.nextFloat() - random.nextFloat(), pos.getY() + i + random.nextFloat() - random.nextFloat(),
                        pos.getZ() + 0.5 + random.nextFloat() - random.nextFloat(), 0, 0, 0);
            }
        }
    }
}
