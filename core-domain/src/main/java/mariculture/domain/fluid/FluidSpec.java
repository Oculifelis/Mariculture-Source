package mariculture.domain.fluid;

import java.util.Objects;

import mariculture.domain.content.Ids;

/**
 * A Mariculture fluid. Legacy registered them with FluidHelper.addFluid (core/Core.java:191-229,
 * fishery/Fishery.java:204-224); every fluid here has a source and flowing form, a world block and a
 * bucket unless it is a gas.
 *
 * @param texture   base name of textures/block/fluid/{texture}_still.png and _flow.png, or a full texture id
 *                  ("minecraft:block/water") whose _still and _flow variants are vanilla textures, or a full id
 *                  ending in "!" that is used for both (legacy "dirt" drew the dirt block texture)
 * @param temperature in kelvin, for other mods' heat checks (molten metals use their legacy melting point)
 * @param effect    what the fluid block does to entities and the world (legacy BlockFluid subclasses)
 */
public record FluidSpec(String id, String englishName, String legacyName, String texture, int temperature, int luminosity,
                        int density, int viscosity, boolean gaseous, FluidEffect effect) {
    public FluidSpec {
        Ids.requireValid(id);
        Objects.requireNonNull(englishName);
        Objects.requireNonNull(texture);
        Objects.requireNonNull(effect);
    }

    /** Liquids get a block and a bucket; gases (natural gas) only exist in tanks and bottles until their block is ported. */
    public boolean hasBucket() {
        return !gaseous;
    }

    public boolean hasBlock() {
        return !gaseous;
    }

    public String stillTexture() {
        if (texture.endsWith("!")) return texture.substring(0, texture.length() - 1);
        return texture.indexOf(':') >= 0 ? texture + "_still" : "mariculture:block/fluid/" + texture + "_still";
    }

    public String flowTexture() {
        if (texture.endsWith("!")) return texture.substring(0, texture.length() - 1);
        return texture.indexOf(':') >= 0 ? texture + "_flow" : "mariculture:block/fluid/" + texture + "_flow";
    }

    /** True when the textures ship in content-source (false for vanilla texture references). */
    public boolean ownTextures() {
        return texture.indexOf(':') < 0;
    }

    public String bucketId() {
        return id + "_bucket";
    }

    public String blockId() {
        return id;
    }
}
