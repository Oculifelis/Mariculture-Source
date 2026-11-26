package mariculture.platform.recipe;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import com.mojang.serialization.MapCodec;

import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

import mariculture.domain.fishery.FishRecipes;
import mariculture.domain.machine.Stack;
import mariculture.platform.Stacks;

/** Crafting recipes whose result comes from core-domain FishRecipes (caviar from eggs, fish DNA reset). */
public final class FishCraftingRecipe extends CustomRecipe {
    private static final FishCraftingRecipe CAVIAR_RECIPE = new FishCraftingRecipe(true);
    private static final FishCraftingRecipe RESET_RECIPE = new FishCraftingRecipe(false);
    public static final RecipeSerializer<FishCraftingRecipe> CAVIAR = new RecipeSerializer<>(MapCodec.unit(CAVIAR_RECIPE), StreamCodec.unit(CAVIAR_RECIPE));
    public static final RecipeSerializer<FishCraftingRecipe> FISH_RESET = new RecipeSerializer<>(MapCodec.unit(RESET_RECIPE), StreamCodec.unit(RESET_RECIPE));

    private final boolean caviar;

    private FishCraftingRecipe(boolean caviar) {
        this.caviar = caviar;
    }

    private ItemStack result(CraftingInput grid) {
        List<Stack> stacks = new ArrayList<>();
        for (int i = 0; i < grid.size(); i++) stacks.add(Stacks.toDomain(grid.getItem(i)));
        Function<List<Stack>, Stack> rule = caviar ? FishRecipes::caviar : FishRecipes::reset;
        return Stacks.toLoader(rule.apply(stacks));
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
    public RecipeSerializer<FishCraftingRecipe> getSerializer() {
        return caviar ? CAVIAR : FISH_RESET;
    }
}
