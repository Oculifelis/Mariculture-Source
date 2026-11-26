package mariculture.platform;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;

import mariculture.domain.env.BiomeEnvironment;
import mariculture.domain.fish.Salinity;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.plugin.CompatTags;

/** Legacy environment values (salinity, temperature, dimension kind) at a position. */
public final class Env {
    private Env() {}

    public static BiomeEnvironment.Entry biome(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        String id = biome.unwrapKey().map(k -> k.location().toString()).orElse("");
        return BiomeEnvironment.of(id, tag -> biome.is(TagKey.create(Registries.BIOME, new ResourceLocation(tag.startsWith("c:") ? "forge:" + tag.substring(2) : tag))));
    }

    public static Salinity salinity(Level level, BlockPos pos) {
        return biome(level, pos).salinity();
    }

    public static int temperature(Level level, BlockPos pos) {
        float sun = BiomeEnvironment.sunBrightness(level.getTimeOfDay(1f), level.getRainLevel(1f), level.getThunderLevel(1f));
        return BiomeEnvironment.temperature(biome(level, pos).temperature(), sun, pos.getY(), level.getRainLevel(1f));
    }

    public static String dimensionKind(Level level) {
        if (level.dimension() == Level.NETHER || level.dimensionType().ultraWarm()) return "nether";
        if (level.dimension() == Level.END) return "end";
        return "overworld_like";
    }

    /** A5: fluids besides water and Nether lava that a Mariculture hook floats on (#mariculture:hook_floats_on). */
    public static final TagKey<Fluid> HOOK_FLOATS_ON = TagKey.create(Registries.FLUID, new ResourceLocation(CompatTags.HOOK_FLOATS_ON));

    /** A5: the biome and fluid at a cast, for species limited by catch.biomes / catch.fluid. */
    public static FishingRules.Where where(Level level, BlockPos pos) {
        Holder<Biome> biome = level.getBiome(pos);
        String biomeId = biome.unwrapKey().map(k -> k.location().toString()).orElse("");
        FluidState fluid = level.getFluidState(pos);
        return new FishingRules.Where() {
            @Override
            public boolean inBiome(String s) {
                return s.startsWith("#") ? biome.is(TagKey.create(Registries.BIOME, new ResourceLocation(s.substring(1)))) : s.equals(biomeId);
            }

            @Override
            public boolean inFluid(String s) {
                if (fluid.isEmpty()) return false;
                return s.startsWith("#") ? fluid.is(TagKey.create(Registries.FLUID, new ResourceLocation(s.substring(1))))
                        : s.equals(String.valueOf(BuiltInRegistries.FLUID.getKey(fluid.getType())));
            }

            @Override
            public boolean otherFluid() {
                return !fluid.isEmpty() && !fluid.is(FluidTags.WATER) && !fluid.is(FluidTags.LAVA);
            }
        };
    }
}
