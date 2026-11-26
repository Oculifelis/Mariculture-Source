package mariculture.tools;

/**
 * Resource-format differences between the three supported editions. Every difference the
 * exporter handles is listed here so a new target is one enum constant plus verification.
 */
enum Target {
    // Forge 1.20.1: plural data folders, {"item": ...} results, forge: common tags, pack.mcmeta required,
    // Patchouli's own book recipe serializer.
    FORGE_1_20_1("forge-1.20.1", true, true, false, false, false, false, 15),
    // NeoForge 1.21.1: singular data folders (1.21), {"id": ...} results, c: common tags, and the book is a
    // patchouli:book data component on a vanilla recipe (Patchouli 1.20.5+ dropped its recipe serializers).
    NEOFORGE_1_21_1("neoforge-1.21.1", false, false, false, false, true, false, 0),
    // NeoForge 26.1.2: as 1.21.1, plus string ingredients (1.21.2+), client item definitions (1.21.4+) and
    // equipment asset files for armour (1.21.2+).
    NEOFORGE_26_1_2("neoforge-26.1.2", false, false, true, true, true, true, 0);

    final String dir;
    final boolean pluralDataFolders;
    final boolean forgeTagNamespace;
    final boolean stringIngredients;
    final boolean clientItemDefinitions;
    final boolean bookComponent;
    final boolean equipmentAssets;
    /** Block models name their chunk layer with "render_type" (Forge/NeoForge before 26.1, which infers it). */
    final boolean modelRenderTypes;
    /** 0 means the loader supplies mod pack metadata and no pack.mcmeta is written. */
    final int packFormat;

    Target(String dir, boolean pluralDataFolders, boolean forgeTagNamespace, boolean stringIngredients, boolean clientItemDefinitions,
           boolean bookComponent, boolean equipmentAssets, int packFormat) {
        this.dir = dir;
        this.pluralDataFolders = pluralDataFolders;
        this.forgeTagNamespace = forgeTagNamespace;
        this.stringIngredients = stringIngredients;
        this.clientItemDefinitions = clientItemDefinitions;
        this.bookComponent = bookComponent;
        this.equipmentAssets = equipmentAssets;
        this.modelRenderTypes = !equipmentAssets;
        this.packFormat = packFormat;
    }

    String recipes() { return pluralDataFolders ? "recipes" : "recipe"; }
    String lootTables() { return pluralDataFolders ? "loot_tables" : "loot_table"; }
    String blockTags() { return pluralDataFolders ? "tags/blocks" : "tags/block"; }
    String itemTags() { return pluralDataFolders ? "tags/items" : "tags/item"; }

    static Target byDir(String dir) {
        for (Target t : values()) if (t.dir.equals(dir)) return t;
        throw new IllegalArgumentException("Unknown target " + dir);
    }
}
