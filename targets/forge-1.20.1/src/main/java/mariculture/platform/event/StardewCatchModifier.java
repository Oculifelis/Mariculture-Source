package mariculture.platform.event;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import mariculture.domain.content.Ids;

/** "mariculture:fishing_catch": Mariculture's catch in place of the vanilla fishing loot while Stardew Fishing is on (S2). */
public final class StardewCatchModifier extends LootModifier {
    public static final Codec<StardewCatchModifier> CODEC = RecordCodecBuilder.create(i -> codecStart(i).apply(i, StardewCatchModifier::new));
    private static final DeferredRegister<Codec<? extends IGlobalLootModifier>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.Keys.GLOBAL_LOOT_MODIFIER_SERIALIZERS, Ids.MOD_ID);

    static {
        SERIALIZERS.register("fishing_catch", () -> CODEC);
    }

    public StardewCatchModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    public static void register(IEventBus modBus) {
        SERIALIZERS.register(modBus);
    }

    @Override
    protected ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        if (!BuiltInLootTables.FISHING.equals(context.getQueriedLootTableId())) return loot;
        if (!(context.getParamOrNull(LootContextParams.THIS_ENTITY) instanceof FishingHook hook)) return loot;
        ItemStack caught = FisheryEvents.stardewCatch(hook);
        if (caught == null) return loot;
        ObjectArrayList<ItemStack> out = new ObjectArrayList<>();
        out.add(caught);
        return out;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC;
    }
}
