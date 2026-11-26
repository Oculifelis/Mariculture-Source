package mariculture.domain.content;

/** How a block is placed and modelled. Targets map each to a block class and model template. */
public enum Shape {
    /** Plain cube; textures: "all", or "end"+"side" for a column-textured cube. */
    CUBE,
    /** Log-like axis block (legacy limestone pillar metas 7-9). Textures: "end", "side". */
    PILLAR,
    /** Six-way facing block (legacy limestone pedestal metas 10-15). Textures: "top", "bottom", "side". */
    PEDESTAL,
    /**
     * Pearl oyster: a shallow, waterlogged shell on the sea floor with a block entity holding one item and a
     * horizontal facing. Textures: "shell", "lid"; its item uses the item texture named after the block.
     */
    OYSTER,
    /**
     * A machine with a block entity running a {@link mariculture.domain.machine.MachineType} of the same id.
     * Textures: "all", or "front", "side", "top", "bottom" for a horizontally facing machine.
     */
    MACHINE,
    /** As MACHINE, with see-through (cutout) textures: tanks, the hatchery, the sifter mesh. */
    MACHINE_GLASS,
    /** Fishing net lying on water (legacy BlockTicking): one sixteenth high, random-ticks a catch. Texture "all". */
    NET,
    /** Neon lamp (legacy BlockNeonLamp): glass cube with a "lit" state, lit unless powered. Textures "on", "off". */
    LAMP,
    /** See-through cube (heat glass, plastic): translucent, drops itself. Texture "all". */
    GLASS,
    /** Ancient sand: a cube whose drops come from {@link AncientSand}. Texture "all". */
    ANCIENT_SAND,
    /** Bubbling sand: looks and drops like sand, bubbles rise through the water above it. Texture "all". */
    BUBBLE_SAND,
    /**
     * Dried coral (legacy plantStatic): a cross-shaped plant, waterloggable, no collision, breaks when the block
     * below is not in {@code #mariculture:coral_ground}. Texture "all"; its item shows the same texture.
     */
    CORAL,
    /** Living coral (legacy plantGrowable): as CORAL, drops the dried coral and spreads on coral rock (random ticks). */
    LIVING_CORAL,
    /**
     * A machine the legacy mod drew with its own renderer: its {@link MachineModel} (same id) gives the boxes and
     * whether it faces horizontally or six ways. Textures: "particle" plus the slots the model names.
     */
    MACHINE_MODEL,
    /**
     * A sawmill block (factory/blocks/BlockCustom*.java): a vanilla shape picked by {@link
     * mariculture.domain.factory.Plans.Type#block} with a block entity holding its {@link
     * mariculture.domain.factory.CustomFaces}. Texture "all" is the template the client paints over.
     */
    CUSTOM,
    /** Natural gas (legacy BlockAir): see-through, no collision, replaceable, no drops, flammable. Texture "all". */
    GAS
}
