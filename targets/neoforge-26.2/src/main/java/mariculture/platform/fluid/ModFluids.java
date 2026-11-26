package mariculture.platform.fluid;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForgeMod;
import net.neoforged.neoforge.common.SoundActions;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import mariculture.domain.content.Ids;
import mariculture.domain.fluid.FluidEffect;
import mariculture.domain.fluid.FluidSpec;
import mariculture.domain.fluid.Fluids;

/** Registers every catalog fluid: type, source, flowing form and world block. Buckets are catalog items. */
public final class ModFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Ids.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(Registries.FLUID, Ids.MOD_ID);
    private static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(Ids.MOD_ID);

    public record Entry(FluidSpec spec, Supplier<FluidType> type, Supplier<FlowingFluid> source, Supplier<FlowingFluid> flowing,
                        Supplier<LiquidBlock> block) {}

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        for (FluidSpec spec : Fluids.all()) register(spec);
    }

    private ModFluids() {}

    private static void register(FluidSpec spec) {
        Supplier<FluidType> type = TYPES.register(spec.id(), () -> new FluidType(properties(spec)));
        @SuppressWarnings("unchecked") Supplier<FlowingFluid>[] source = new Supplier[1];
        @SuppressWarnings("unchecked") Supplier<FlowingFluid>[] flowing = new Supplier[1];
        @SuppressWarnings("unchecked") Supplier<LiquidBlock>[] block = new Supplier[1];
        Supplier<BaseFlowingFluid.Properties> props = () -> {
            BaseFlowingFluid.Properties p = new BaseFlowingFluid.Properties(type, source[0], flowing[0]);
            if (spec.hasBucket()) p.bucket(() -> bucket(spec));
            if (spec.hasBlock()) p.block(block[0]);
            return p;
        };
        source[0] = FLUIDS.register(spec.id(), () -> new BaseFlowingFluid.Source(props.get()));
        flowing[0] = FLUIDS.register("flowing_" + spec.id(), () -> new BaseFlowingFluid.Flowing(props.get()));
        if (spec.hasBlock()) {
            block[0] = BLOCKS.register(spec.blockId(), key -> new ModLiquidBlock(source[0].get(), spec,
                    BlockBehaviour.Properties.of().setId(net.minecraft.resources.ResourceKey.create(Registries.BLOCK, key)).mapColor(MapColor.WATER).replaceable().noCollision().strength(100f).noLootTable()
                            .liquid().pushReaction(PushReaction.DESTROY).randomTicks().lightLevel(s -> spec.luminosity())));
        }
        ENTRIES.put(spec.id(), new Entry(spec, type, source[0], flowing[0], block[0]));
    }

    private static FluidType.Properties properties(FluidSpec spec) {
        FluidType.Properties p = FluidType.Properties.create()
                .descriptionId("fluid_type." + Ids.MOD_ID + "." + spec.id())
                .density(spec.density()).viscosity(spec.viscosity()).temperature(spec.temperature()).lightLevel(spec.luminosity())
                .sound(SoundActions.BUCKET_FILL, spec.effect() == FluidEffect.HOT ? SoundEvents.BUCKET_FILL_LAVA : SoundEvents.BUCKET_FILL)
                .sound(SoundActions.BUCKET_EMPTY, spec.effect() == FluidEffect.HOT ? SoundEvents.BUCKET_EMPTY_LAVA : SoundEvents.BUCKET_EMPTY);
        if (spec.effect() == FluidEffect.PRESSURISED) p = p.motionScale(FluidEffect.PRESSURISED_PUSH);
        if (spec.effect() == FluidEffect.HOT) p = p.canSwim(false).canDrown(false).canExtinguish(false).canHydrate(false);
        return p;
    }

    private static Item bucket(FluidSpec spec) {
        return BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath(Ids.MOD_ID, spec.bucketId()));
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        NeoForgeMod.enableMilkFluid();
    }

    public static Map<String, Entry> entries() {
        return ENTRIES;
    }

    /** The source fluid of a catalog fluid id ("mariculture:fish_oil" or "fish_oil"). */
    public static Fluid source(String id) {
        Entry e = ENTRIES.get(id.startsWith("mariculture:") ? id.substring(12) : id);
        return e == null ? null : e.source().get();
    }
}
