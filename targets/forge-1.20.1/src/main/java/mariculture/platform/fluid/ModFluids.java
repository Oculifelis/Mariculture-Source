package mariculture.platform.fluid;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.common.ForgeMod;
import net.minecraftforge.common.SoundActions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;

import mariculture.domain.content.Ids;
import mariculture.domain.fluid.FluidEffect;
import mariculture.domain.fluid.FluidSpec;
import mariculture.domain.fluid.Fluids;

/** Registers every catalog fluid: type (with its client textures), source, flowing form and world block. */
public final class ModFluids {
    private static final DeferredRegister<FluidType> TYPES = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, Ids.MOD_ID);
    private static final DeferredRegister<Fluid> FLUIDS = DeferredRegister.create(ForgeRegistries.FLUIDS, Ids.MOD_ID);
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Ids.MOD_ID);

    public record Entry(FluidSpec spec, Supplier<FluidType> type, Supplier<FlowingFluid> source, Supplier<FlowingFluid> flowing,
                        Supplier<LiquidBlock> block) {}

    private static final Map<String, Entry> ENTRIES = new LinkedHashMap<>();

    static {
        for (FluidSpec spec : Fluids.all()) register(spec);
    }

    private ModFluids() {}

    private static void register(FluidSpec spec) {
        Supplier<FluidType> type = TYPES.register(spec.id(), () -> new FluidType(properties(spec)) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                ResourceLocation still = new ResourceLocation(spec.stillTexture());
                ResourceLocation flow = new ResourceLocation(spec.flowTexture());
                // Mariculture's own fluid textures are already coloured; vanilla water reused for pressurised water is tinted blue.
                int tint = spec.effect() == FluidEffect.PRESSURISED ? 0xFF3F76E4 : 0xFFFFFFFF;
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return still;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return flow;
                    }

                    @Override
                    public int getTintColor() {
                        return tint;
                    }
                });
            }
        });
        @SuppressWarnings("unchecked") Supplier<FlowingFluid>[] source = new Supplier[1];
        @SuppressWarnings("unchecked") Supplier<FlowingFluid>[] flowing = new Supplier[1];
        @SuppressWarnings("unchecked") Supplier<LiquidBlock>[] block = new Supplier[1];
        Supplier<ForgeFlowingFluid.Properties> props = () -> {
            ForgeFlowingFluid.Properties p = new ForgeFlowingFluid.Properties(type, source[0], flowing[0]);
            if (spec.hasBucket()) p.bucket(() -> bucket(spec));
            if (spec.hasBlock()) p.block(block[0]);
            if (spec.effect() == FluidEffect.HOT) p.tickRate(30).slopeFindDistance(2).levelDecreasePerBlock(2);
            return p;
        };
        source[0] = FLUIDS.register(spec.id(), () -> new ForgeFlowingFluid.Source(props.get()));
        flowing[0] = FLUIDS.register("flowing_" + spec.id(), () -> new ForgeFlowingFluid.Flowing(props.get()));
        if (spec.hasBlock()) {
            block[0] = BLOCKS.register(spec.blockId(), () -> new ModLiquidBlock(source[0], spec,
                    BlockBehaviour.Properties.of().mapColor(MapColor.WATER).replaceable().noCollission().strength(100f).noLootTable()
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
        return ForgeRegistries.ITEMS.getValue(new ResourceLocation(Ids.MOD_ID, spec.bucketId()));
    }

    public static void register(IEventBus modBus) {
        TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        ForgeMod.enableMilkFluid();
    }

    public static Map<String, Entry> entries() {
        return ENTRIES;
    }

    /** The source fluid of a catalog fluid id ("mariculture:fish_oil" or "fish_oil"). */
    public static Supplier<FlowingFluid> sourceSupplier(String id) {
        Entry e = ENTRIES.get(id.startsWith("mariculture:") ? id.substring(12) : id);
        return e == null ? null : e.source();
    }
}
