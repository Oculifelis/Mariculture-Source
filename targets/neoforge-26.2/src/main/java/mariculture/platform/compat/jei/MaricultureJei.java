package mariculture.platform.compat.jei;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.builder.IRecipeSlotBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.RecipeIngredientRole;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.material.Fluid;

import mariculture.domain.content.Ids;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.view.RecipeViews;
import mariculture.platform.Stacks;
import mariculture.platform.machine.MachineBlockEntity;

/** JEI pages for every Mariculture machine, drawn from {@link RecipeViews}. Only JEI loads this class. */
@JeiPlugin
public final class MaricultureJei implements IModPlugin {
    private static final Map<RecipeViews.Category, RecipeType<RecipeViews.View>> TYPES = new LinkedHashMap<>();

    static {
        for (RecipeViews.Category c : RecipeViews.CATEGORIES) TYPES.put(c, RecipeType.create(Ids.MOD_ID, c.id(), RecipeViews.View.class));
    }

    @Override
    public Identifier getPluginUid() {
        return Identifier.fromNamespaceAndPath(Ids.MOD_ID, "jei");
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        IGuiHelper gui = registration.getJeiHelpers().getGuiHelper();
        for (var e : TYPES.entrySet()) registration.addRecipeCategories(new ViewCategory(e.getKey(), e.getValue(), gui));
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (var e : TYPES.entrySet()) {
            List<RecipeViews.View> shown = new ArrayList<>();
            for (RecipeViews.View v : RecipeViews.views(e.getKey(), LiveFish.registry())) if (resolves(v)) shown.add(v);
            registration.addRecipes(e.getValue(), shown);
        }
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        for (var e : TYPES.entrySet()) {
            for (String machine : e.getKey().machines()) {
                Item item = Stacks.item(machine);
                if (item != null) registration.addRecipeCatalyst(new ItemStack(item), e.getValue());
            }
        }
    }

    /** Pages whose items, tags and fluids all exist in this game (other mods' metals, empty tags). */
    private static boolean resolves(RecipeViews.View v) {
        for (RecipeViews.Slot s : v.slots()) {
            if (s.fluid() ? MachineBlockEntity.fluid(s.id()) == null : items(s.id(), 1).isEmpty()) return false;
        }
        return true;
    }

    static List<ItemStack> items(String id, int count) {
        List<ItemStack> out = new ArrayList<>();
        if (id.startsWith("#")) {
            for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, Identifier.parse(id.substring(1))))) {
                out.add(new ItemStack(h.value(), count));
            }
        } else {
            Item item = Stacks.item(id);
            if (item != null) out.add(new ItemStack(item, count));
        }
        return out;
    }

    static Component text(RecipeViews.Line line) {
        Object[] args = line.args().clone();
        for (int i = 0; i < args.length; i++) if (args[i] instanceof RecipeViews.Line l) args[i] = text(l);
        return Component.translatable(line.key(), args);
    }

    private static final class ViewCategory implements IRecipeCategory<RecipeViews.View> {
        private final RecipeViews.Category category;
        private final RecipeType<RecipeViews.View> type;
        private final IDrawable icon;

        ViewCategory(RecipeViews.Category category, RecipeType<RecipeViews.View> type, IGuiHelper gui) {
            this.category = category;
            this.type = type;
            Item item = Stacks.item(category.icon());
            this.icon = gui.createDrawableItemStack(item == null ? ItemStack.EMPTY : new ItemStack(item));
        }

        @Override
        public RecipeType<RecipeViews.View> getRecipeType() {
            return type;
        }

        @Override
        public Component getTitle() {
            return Component.translatable("jei." + Ids.MOD_ID + "." + category.id());
        }

        @Override
        public int getWidth() {
            return category.width();
        }

        @Override
        public int getHeight() {
            return category.height();
        }

        @Override
        public IDrawable getIcon() {
            return icon;
        }

        @Override
        public void setRecipe(IRecipeLayoutBuilder builder, RecipeViews.View view, IFocusGroup focuses) {
            for (RecipeViews.Slot s : view.slots()) {
                RecipeIngredientRole role = switch (s.role()) {
                    case INPUT -> RecipeIngredientRole.INPUT;
                    case OUTPUT -> RecipeIngredientRole.OUTPUT;
                    case CATALYST -> RecipeIngredientRole.CRAFTING_STATION;
                };
                IRecipeSlotBuilder slot = builder.addSlot(role, s.x() + 1, s.y() + 1).setStandardSlotBackground();
                if (s.fluid()) {
                    Fluid fluid = MachineBlockEntity.fluid(s.id());
                    slot.addFluidStack(fluid, s.amount()).setFluidRenderer(Math.max(1000, s.amount()), false, 16, 16);
                } else {
                    List<ItemStack> stacks = items(s.id(), Math.max(1, s.amount()));
                    if (!s.data().isEmpty()) stacks.forEach(st -> Stacks.setData(st, s.data()));
                    slot.addItemStacks(stacks);
                }
            }
        }

        @Override
        public void createRecipeExtras(IRecipeExtrasBuilder builder, RecipeViews.View view, IFocusGroup focuses) {
            List<FormattedText> lines = new ArrayList<>();
            for (RecipeViews.Line l : view.lines()) lines.add(text(l));
            for (RecipeViews.Slot s : view.slots()) {
                if (s.chanceOneIn() > 1) lines.add(text(new RecipeViews.Line("jei.mariculture.chance", s.chanceOneIn())));
            }
            if (!lines.isEmpty()) builder.addText(lines, category.width(), category.height() - category.textY() + 2).setPosition(0, category.textY()).setColor(0xFF404040);
        }
    }

    /** Rods whose recipe needs another mod stay out of JEI without it, as they stay out of the creative tab. */
    @Override
    public void onRuntimeAvailable(mezz.jei.api.runtime.IJeiRuntime runtime) {
        List<ItemStack> hidden = new ArrayList<>();
        for (Item item : BuiltInRegistries.ITEM) {
            var key = BuiltInRegistries.ITEM.getKey(item);
            if (key.getNamespace().equals(Ids.MOD_ID) && mariculture.domain.plugin.Plugins.hiddenWithoutPlugin(key.toString())) {
                hidden.add(new ItemStack(item));
            }
        }
        if (!hidden.isEmpty()) {
            runtime.getIngredientManager().removeIngredientsAtRuntime(mezz.jei.api.constants.VanillaTypes.ITEM_STACK, hidden);
        }
    }
}
