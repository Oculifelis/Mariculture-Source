package mariculture.platform.recipe;

import java.util.ArrayList;
import java.util.List;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import mariculture.domain.machine.Stack;
import mariculture.domain.magic.Jewelry;
import mariculture.domain.random.Rng;
import mariculture.platform.Stacks;
import mariculture.platform.magic.Enchants;

/**
 * Jewelry crafting from core-domain Jewelry.craft: the worked piece for the anvil. The shuffle is seeded from the grid
 * so the client's preview and the server's result agree.
 */
public final class JewelryCraftingRecipe extends CustomRecipe {
    private static final JewelryCraftingRecipe RECIPE = new JewelryCraftingRecipe();
    public static final RecipeSerializer<JewelryCraftingRecipe> SERIALIZER = new RecipeSerializer<>(MapCodec.unit(RECIPE), StreamCodec.unit(RECIPE));

    private JewelryCraftingRecipe() {}

    private static ItemStack result(CraftingInput grid) {
        List<Stack> stacks = new ArrayList<>();
        List<ItemStack> loader = new ArrayList<>();
        boolean full = grid.width() == 3 && grid.height() == 3;
        for (int i = 0; i < grid.size(); i++) {
            ItemStack s = grid.getItem(i);
            if (!full && s.isEmpty()) continue;
            stacks.add(Stacks.toDomain(s));
            loader.add(s);
        }
        Stack out = Jewelry.craft(stacks, i -> {
            List<Jewelry.Enchant> e = new ArrayList<>();
            Enchants.read(loader.get(i)).forEach((id, level) -> e.add(new Jewelry.Enchant(id, level)));
            return e;
        }, Rng.of(new java.util.Random(stacks.hashCode())));
        return Stacks.toLoader(out);
    }

    @Override
    public boolean matches(CraftingInput grid, Level level) {
        return !result(grid).isEmpty();
    }

    @Override
    public ItemStack assemble(CraftingInput grid) {
        return result(grid);
    }

    @Override
    public RecipeSerializer<JewelryCraftingRecipe> getSerializer() {
        return SERIALIZER;
    }
}
