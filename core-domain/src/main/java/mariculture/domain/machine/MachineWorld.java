package mariculture.domain.machine;

import java.util.List;

import mariculture.domain.content.Face;
import mariculture.domain.fish.Salinity;
import mariculture.domain.random.Rng;

/**
 * What a machine may ask of the world around it. Targets implement this once over their block entity;
 * positions are offsets from the machine.
 */
public interface MachineWorld {
    Rng random();

    long gameTime();

    boolean isDay();

    boolean isRaining();

    boolean poweredByRedstone();

    /** Dimension id ("minecraft:overworld"). */
    String dimension();

    boolean isNether();

    boolean isEnd();

    /** Absolute height of the machine. */
    int y();

    /** Biome base temperature and salinity at the machine (legacy EnvironmentHandler table). */
    int biomeTemperature();

    Salinity salinity();

    /** Legacy EnvironmentHandler.getTemperature: biome temperature adjusted for sun, height and rain. */
    int temperature();

    String block(int dx, int dy, int dz);

    boolean blockHasTag(int dx, int dy, int dz, String tag);

    /** Fluid id of the fluid at an offset ("minecraft:water", "minecraft:lava", "" if none). */
    String fluid(int dx, int dy, int dz);

    boolean isFluidSource(int dx, int dy, int dz);

    boolean isAir(int dx, int dy, int dz);

    void setBlock(int dx, int dy, int dz, String block);

    /** The Mariculture machine at an offset, or null. */
    Machine machine(int dx, int dy, int dz);

    boolean itemHasTag(String item, String tag);

    /** Spawns the stack as an item entity on top of the machine. */
    void drop(Stack stack);

    /** Tries every adjacent inventory in random side order (BlockTransferHelper.ejectToSides); returns the rest. */
    Stack ejectToNeighbours(Stack stack);

    /** Inserts into the inventory on one face, machines included (fish sorter); returns the rest. */
    default Stack ejectTo(Face face, Stack stack) {
        return stack;
    }

    /** Spawns the stack as an item entity in the block on that face. */
    default void dropAt(Face face, Stack stack) {
        drop(stack);
    }

    /** Whether any item has the tag (tag-gated recipes of other mods' metals). */
    default boolean tagHasItems(String tag) {
        return false;
    }

    /** The first item id in the tag, preferring Mariculture and vanilla items, or null. */
    default String firstItemInTag(String tag) {
        return null;
    }

    /** Drains the fluid handler on a face (crucible fuel from below); null when nothing came out. */
    default FluidAmount drainFrom(Face face, int max, boolean execute) {
        return null;
    }

    /**
     * Legacy BlockTransferHelper.ejectFluid(int[]): for each side in random order (except {@code skip}), offer the
     * first amount the neighbour takes whole; one transfer at most. Returns whether one happened.
     */
    default boolean ejectFluidExact(Tank tank, int[] amounts, Face skip) {
        return false;
    }

    /** Whether a player is within the range of the machine. */
    default boolean playerNearby(double range) {
        return false;
    }

    /** The item after taking damage, or EMPTY when it breaks (autohammer hammers). */
    default Stack damage(Stack item, int amount) {
        return item;
    }

    /** The highest weight of any registered enchantment (crucible ethereal melting). */
    default int maxEnchantmentWeight() {
        return mariculture.domain.metal.Ethereal.VANILLA_MAX_WEIGHT;
    }

    /** A block-breaking explosion just above the machine (crucible gunpowder and TNT fuel). */
    default void explode(float radius) {}

    /** Pushes up to {@code amount} from the tank into adjacent fluid handlers; returns how much left the tank. */
    int ejectFluid(Tank tank, int amount);

    /** Fills a container item from a tank or empties it into one; adapters use the loader's fluid item API. */
    ContainerResult processContainer(Stack container, Tank tank);

    /** Item entities in the given block offsets (feeder food pick-up). */
    List<ItemEntityView> itemsAt(List<int[]> offsets);

    /** Adjacent faces in random order (legacy BlockTransferHelper.getSides shuffle). */
    List<Face> shuffledFaces();

    /**
     * Runs a fish tank living effect on every living entity standing in the given block offsets and performs the
     * actions the rule returns (TileFeeder.doEffect).
     */
    void affectLiving(List<int[]> offsets, java.util.function.Function<mariculture.domain.fish.FishEffects.Living,
            List<mariculture.domain.fish.FishEffects.Action>> rule);

    /**
     * Like {@link #affectLiving} but over one box: the bounds of the offsets grown by {@code radius} blocks, each
     * entity once (fish abilities).
     */
    default void affectLivingNear(List<int[]> offsets, int radius, java.util.function.Function<mariculture.domain.fish.FishEffects.Living,
            List<mariculture.domain.fish.FishEffects.Action>> rule) {
        affectLiving(offsets, rule);
    }

    /** Applies bone meal to the block at an offset as a dispenser would; whether it grew. */
    default boolean bonemeal(int dx, int dy, int dz) {
        return false;
    }

    /** The light level 0-15 at an offset, sky light included (as mobs see it). */
    default int light(int dx, int dy, int dz) {
        return 15;
    }

    /** Performs a fish tank world effect; coordinates in the action index into {@code offsets}. */
    void affectWorld(mariculture.domain.fish.FishEffects.WorldAction action, List<int[]> offsets);

    /** Result of {@link #processContainer}: the new container stack, or null when nothing happened. */
    record ContainerResult(Stack container, boolean filledTank) {}

    /** A live item entity a machine may consume. */
    interface ItemEntityView {
        Stack stack();

        boolean inWater();

        boolean inLava();

        void setStack(Stack stack);
    }

    /**
     * Guards a box (minX, minY, minZ, maxX, maxY, maxZ relative to the machine) from hostile spawns, or clears the
     * machine's box with null (fishery/SpawnGuard).
     */
    default void guardSpawns(int[] box) {}

    /** The way the machine faces: its horizontal or six-way facing, NORTH for machines without one. */
    default Face facing() {
        return Face.NORTH;
    }

    /** Whether entities and geyser jets pass through the block at an offset (no collision box). */
    default boolean passable(int dx, int dy, int dz) {
        return isAir(dx, dy, dz);
    }

    /**
     * Applies {@code rule} to the motion of every entity in the given block offsets and resets their fall distance
     * (TileGeyser.doSquirt). A null result leaves that entity's motion.
     */
    default void moveEntities(List<int[]> offsets, java.util.function.UnaryOperator<EntityMotion> rule) {}

    /** An entity's motion as a machine sees it. */
    record EntityMotion(double x, double y, double z, boolean item) {}

    /** Spawns particles for nearby players; coordinates are relative to the machine's corner. */
    default void particle(String type, double dx, double dy, double dz) {}

    /** The 1x1 crafting result of one item (UnpackerHelper.unpack), or EMPTY. */
    default Stack uncraft(Stack one) {
        return Stack.EMPTY;
    }

    /**
     * TileAirPump.supplyWithAir: every player inside the machine's box grown by rx, ry, rz who wears {@code helmet}
     * and {@code top} and is in water gets full air.
     */
    default void refillAir(double rx, double ry, double rz, String helmet, String top) {}

    /** Takes the first stack (up to {@code max}) automation may extract from the inventory on a face, or EMPTY. */
    default Stack takeFrom(Face face, int max) {
        return Stack.EMPTY;
    }

    /** Every tag an item is in ("c:ingots/copper"). */
    default java.util.Set<String> itemTags(String item) {
        return java.util.Set.of();
    }

    /** Every item in a tag, in registry order. */
    default List<String> itemsInTag(String tag) {
        return List.of();
    }

    /** Offers FE to the energy receiver at an offset through its {@code side}; returns how much it took. */
    default int insertEnergy(int dx, int dy, int dz, mariculture.domain.content.Face side, int amount) {
        return 0;
    }

    /** The way the machine at an offset faces (generators behind their rotors); NORTH when it has no facing. */
    default mariculture.domain.content.Face facingAt(int dx, int dy, int dz) {
        return mariculture.domain.content.Face.NORTH;
    }

    /** Fills the fluid handler on a face; returns how much it took (or would take). */
    default int fillTo(mariculture.domain.content.Face face, String fluid, int amount, boolean execute) {
        return 0;
    }

    /** Places a source block of the fluid at an offset; false when the fluid has no block. */
    default boolean placeFluid(int dx, int dy, int dz, String fluid) {
        return false;
    }

    /** Whether the block at an offset is a full solid cube (World.isBlockNormalCubeDefault). */
    default boolean isSolidCube(int dx, int dy, int dz) {
        return false;
    }

    /** The block id an item places, or null when it is not a block (the sawmill's faces). */
    default String blockOfItem(String item) {
        return null;
    }

    /** Whether a fluid is registered (the fluid dictionary skips missing ones). */
    default boolean fluidExists(String fluid) {
        return true;
    }

    /** The hardness of the block at an offset (-1 unbreakable, like bedrock). */
    default float hardness(int dx, int dy, int dz) {
        return -1f;
    }

    /** Breaks the block at an offset as a player would, dropping it (BlockHelper.destroyBlock). */
    default void breakBlock(int dx, int dy, int dz) {}

    /** Hurts every living entity in a box (x1, y1, z1, x2, y2, z2 in blocks from the machine's corner). */
    default void hurtLiving(double[] box, float amount) {}

    /** A cast into the block below the machine (its height, biome, time of day) with the rod tier and luck given. */
    default mariculture.domain.fishing.FishingRules.Cast castBelow(mariculture.domain.fish.Species.Rod rod, int luck) {
        return null;
    }

    /**
     * Turns a catch into an item (vanilla fishing loot rolled with the rod in {@code rodSlot}, enchanting as the catch
     * says) and puts it into the neighbours when {@code eject}, then the {@code outputs}, then drops it. False when
     * nothing was caught.
     */
    default boolean deliverCatch(mariculture.domain.fishing.FishingRules.Catch c, int rodSlot, int[] outputs, boolean eject) {
        return false;
    }

    /**
     * Whether the player with this UUID finished a research entry of a research mod (Thaumaturge). False when the
     * mod is missing, the player is unknown or the uuid is null.
     */
    default boolean researchDone(String ownerUuid, String entry) {
        return false;
    }

    /** Adds magic energy (Thaumaturge vis) to the aura at the machine; returns what was added, 0 without the mod. */
    default float addAuraVis(float amount) {
        return 0;
    }

    /** Drains aura pollution (Thaumaturge flux) at the machine; returns what was drained, 0 without the mod. */
    default float drainAuraFlux(float amount) {
        return 0;
    }
}
