package mariculture.tools;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

import mariculture.domain.content.ArmorMaterialSpec;
import mariculture.domain.content.BlockSpec;
import mariculture.domain.content.Catalog;
import mariculture.domain.content.Ids;
import mariculture.domain.content.Ingredient;
import mariculture.domain.content.ItemKind;
import mariculture.domain.content.ItemSpec;
import mariculture.domain.content.OreFeature;
import mariculture.domain.content.Shape;
import mariculture.domain.content.Recipe;
import mariculture.domain.content.TagNames;
import mariculture.domain.fluid.FluidSpec;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.magic.Binding;
import mariculture.domain.magic.Jewelry;
import mariculture.domain.magic.JewelryEnchantment;
import mariculture.domain.magic.JewelryMaterial;
import mariculture.domain.magic.JewelryType;
import mariculture.domain.metal.MetalRates;
import mariculture.domain.plugin.Plugins;

/**
 * Writes each target's created resources from the shared catalog and content-source/.
 * Usage: ResourceExporter <repo root> [target dir names...]. Output goes to
 * targets/<target>/src/generated/resources, which is wiped first so stale files cannot linger.
 */
public final class ResourceExporter {
    private static final String NS = Ids.MOD_ID;
    private final Path repo;
    private final Target target;
    private final Path out;
    private final Map<String, String> lang = new TreeMap<>();

    private ResourceExporter(Path repo, Target target) {
        this.repo = repo;
        this.target = target;
        this.out = repo.resolve("targets").resolve(target.dir).resolve("src/generated/resources");
    }

    public static void main(String[] args) throws IOException {
        if (args.length < 1) throw new IllegalArgumentException("Usage: ResourceExporter <repo root> [targets...]");
        Path repo = Path.of(args[0]).toAbsolutePath().normalize();
        List<Target> targets = new ArrayList<>();
        if (args.length == 1) targets.addAll(List.of(Target.values()));
        for (int i = 1; i < args.length; i++) targets.add(Target.byDir(args[i]));
        for (Target t : targets) {
            new ResourceExporter(repo, t).run();
            System.out.println("Generated resources for " + t.dir);
        }
        writeIdMapping(repo.resolve("docs/id_mapping.csv"));
    }

    /** Developer-facing legacy metadata to modern id table (docs/migration.md). */
    private static void writeIdMapping(Path file) throws IOException {
        StringBuilder sb = new StringBuilder("legacy_ref,modern_id,kind,note\n");
        for (BlockSpec b : Catalog.blocks()) sb.append(b.legacy()).append(',').append(Ids.full(b.id())).append(",block,\n");
        for (ItemSpec i : Catalog.items()) sb.append(i.legacy()).append(',').append(Ids.full(i.id())).append(",item,\n");
        Catalog.vanillaAliases().forEach((legacy, vanilla) -> sb.append(legacy).append(',').append(vanilla).append(",vanilla_alias,replaced by vanilla content\n"));
        Files.createDirectories(file.getParent());
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }

    private void run() throws IOException {
        deleteTree(out);
        for (BlockSpec b : Catalog.blocks()) block(b);
        for (ItemSpec i : Catalog.items()) item(i);
        for (JewelryType t : JewelryType.values()) jewelryModels(t);
        jewelryEnchantments();
        for (ArmorMaterialSpec m : Catalog.armorMaterials()) armorTextures(m);
        for (FluidSpec f : Fluids.all()) fluid(f);
        for (Recipe r : Catalog.recipes()) recipe(r);
        for (Plugins.Conditional c : Plugins.recipes()) pluginRecipe(c.plugin(), c.recipe().id(), recipeJson(c.recipe()));
        for (Recipe r : mariculture.domain.plugin.CompatTags.foreignFishRecipes()) tagGatedRecipe(r.id(), recipeJson(r), mariculture.domain.plugin.ForeignFish.TAG);
        if (target == Target.FORGE_1_20_1) tinkers();
        else hephaestus();
        if (target == Target.FORGE_1_20_1) bloodMagic();
        else neoVitae();
        projectE();
        if (target != Target.NEOFORGE_26_1_2) forestry();
        complicatedBees();
        starcatcher();
        stardewCatch();
        tide();
        for (OreFeature o : Catalog.ores()) ore(o);
        seabed();
        oysters();
        oceanDecor();
        rockhopper();
        tags();
        advancements();
        textures();
        dataFiles();
        thaumaturge();
        book();
        lang.put("itemGroup." + NS, "Mariculture");
        mergeLang(repo.resolve("content-source/lang/en_us.json"));
        write("assets/" + NS + "/lang/en_us.json", Json.write(lang));
        if (target.packFormat > 0) {
            write("pack.mcmeta", Json.write(Map.of("pack", map("description", "Mariculture resources", "pack_format", target.packFormat))));
        }
    }

    // ---- blocks ------------------------------------------------------------------------

    private void block(BlockSpec b) {
        String id = b.id();
        String model = NS + ":block/" + id;
        switch (b.shape()) {
            case CUBE -> {
                if (b.textures().containsKey("all")) {
                    blockModel(id, "minecraft:block/cube_all", map("all", tex(b, "all")));
                } else {
                    blockModel(id, "minecraft:block/cube_column", map("end", tex(b, "end"), "side", tex(b, "side")));
                }
                write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", map("", map("model", model)))));
            }
            case PILLAR -> {
                Map<String, Object> t = map("end", tex(b, "end"), "side", tex(b, "side"));
                blockModel(id, "minecraft:block/cube_column", t);
                blockModel(id + "_horizontal", "minecraft:block/cube_column_horizontal", t);
                write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", map(
                        "axis=y", map("model", model),
                        "axis=z", map("model", model + "_horizontal", "x", 90),
                        "axis=x", map("model", model + "_horizontal", "x", 90, "y", 90)))));
            }
            case PEDESTAL -> {
                // Base model points up; rotations follow vanilla's facing blocks (e.g. end_rod).
                blockModel(id, "minecraft:block/cube_bottom_top", map("top", tex(b, "top"), "bottom", tex(b, "bottom"), "side", tex(b, "side")));
                Map<String, Object> variants = new LinkedHashMap<>();
                variants.put("facing=up", map("model", model));
                variants.put("facing=down", map("model", model, "x", 180));
                variants.put("facing=north", map("model", model, "x", 90));
                variants.put("facing=south", map("model", model, "x", 90, "y", 180));
                variants.put("facing=west", map("model", model, "x", 90, "y", 270));
                variants.put("facing=east", map("model", model, "x", 90, "y", 90));
                write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", variants)));
            }
            case OYSTER -> {
                // Simplified from 1.3.0 RenderOyster: a two-step shell, a rim, and an open lid hinged at the back.
                String shell = tex(b, "shell");
                String lid = tex(b, "lid");
                List<Object> elements = List.of(
                        box(3.2, 0, 3.2, 12.8, 0.8, 12.8, null),
                        box(1.6, 0.8, 1.6, 14.4, 1.6, 14.4, null),
                        box(1.6, 1.6, 1.6, 2.4, 2.4, 14.4, null),
                        box(13.6, 1.6, 1.6, 14.4, 2.4, 14.4, null),
                        box(2.4, 1.6, 13.6, 13.6, 2.4, 14.4, null),
                        box(2.4, 1.6, 1.6, 13.6, 2.4, 2.4, null),
                        map("from", List.of(2.4, 1.6, 1.6), "to", List.of(13.6, 2.4, 13.6),
                                "rotation", map("origin", List.of(8, 2, 1.6), "axis", "x", "angle", -45),
                                "faces", faces("#lid")));
                write("assets/" + NS + "/models/block/" + id + ".json", Json.write(map(
                        "parent", "minecraft:block/block",
                        "textures", map("particle", shell, "shell", shell, "lid", lid),
                        "elements", elements)));
                Map<String, Object> variants = new LinkedHashMap<>();
                variants.put("facing=north", map("model", model));
                variants.put("facing=east", map("model", model, "y", 90));
                variants.put("facing=south", map("model", model, "y", 180));
                variants.put("facing=west", map("model", model, "y", 270));
                write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", variants)));
            }
            case MACHINE, MACHINE_GLASS -> {
                Map<String, Object> m;
                if (b.facing()) {
                    m = map("parent", "minecraft:block/orientable_with_bottom", "textures", map("front", tex(b, "front"), "side", tex(b, "side"),
                            "top", tex(b, "top"), "bottom", tex(b, "bottom"), "particle", tex(b, "side")));
                } else if (b.textures().containsKey("down")) {
                    m = map("parent", "minecraft:block/cube", "textures", map("down", tex(b, "down"), "up", tex(b, "up"), "north", tex(b, "north"),
                            "south", tex(b, "south"), "west", tex(b, "west"), "east", tex(b, "east"), "particle", tex(b, "north")));
                } else {
                    m = map("parent", "minecraft:block/cube_all", "textures", map("all", tex(b, "all")));
                }
                // 26.1.2 picks the cutout layer from the texture itself; older loaders read render_type.
                if (b.shape() == Shape.MACHINE_GLASS && target.modelRenderTypes) m.put("render_type", "minecraft:cutout");
                write("assets/" + NS + "/models/block/" + id + ".json", Json.write(m));
                if (b.facing()) {
                    Map<String, Object> variants = new LinkedHashMap<>();
                    variants.put("facing=north", map("model", model));
                    variants.put("facing=east", map("model", model, "y", 90));
                    variants.put("facing=south", map("model", model, "y", 180));
                    variants.put("facing=west", map("model", model, "y", 270));
                    write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", variants)));
                } else {
                    write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", map("", map("model", model)))));
                }
            }
            case MACHINE_MODEL -> {
                mariculture.domain.content.MachineModel mm = mariculture.domain.content.MachineModel.of(id);
                Map<String, Object> textures = new LinkedHashMap<>();
                for (String slot : new java.util.TreeSet<>(b.textures().keySet())) textures.put(slot, tex(b, slot));
                List<Object> elements = new ArrayList<>();
                for (mariculture.domain.content.MachineModel.Box box : mm.boxes()) {
                    Map<String, Object> faces = new LinkedHashMap<>();
                    box.faces().forEach((face, ft) -> faces.put(face, ft.uv() == null ? map("texture", "#" + ft.texture())
                            : map("texture", "#" + ft.texture(), "uv", List.of(ft.uv()[0], ft.uv()[1], ft.uv()[2], ft.uv()[3]))));
                    elements.add(map("from", List.of(box.from()[0], box.from()[1], box.from()[2]), "to", List.of(box.to()[0], box.to()[1], box.to()[2]),
                            "faces", faces));
                }
                Map<String, Object> m = map("parent", "minecraft:block/block", "textures", textures, "elements", elements);
                if (target.modelRenderTypes) m.put("render_type", "minecraft:cutout");
                write("assets/" + NS + "/models/block/" + id + ".json", Json.write(m));
                Map<String, Object> variants = new LinkedHashMap<>();
                if (mm.facing() == mariculture.domain.content.MachineModel.Facing.ALL) {
                    // Same turns as PEDESTAL (and MachineModel.bounds): the model points up.
                    variants.put("facing=up", map("model", model));
                    variants.put("facing=down", map("model", model, "x", 180));
                    variants.put("facing=north", map("model", model, "x", 90));
                    variants.put("facing=south", map("model", model, "x", 90, "y", 180));
                    variants.put("facing=west", map("model", model, "x", 90, "y", 270));
                    variants.put("facing=east", map("model", model, "x", 90, "y", 90));
                } else {
                    // The model faces south.
                    variants.put("facing=south", map("model", model));
                    variants.put("facing=west", map("model", model, "y", 90));
                    variants.put("facing=north", map("model", model, "y", 180));
                    variants.put("facing=east", map("model", model, "y", 270));
                }
                write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", variants)));
            }
            case NET -> {
                Map<String, Object> m = map("parent", "minecraft:block/block", "textures", map("particle", tex(b, "all"), "net", tex(b, "all")),
                        "elements", List.of(map("from", List.of(0, 0, 0), "to", List.of(16, 1, 16), "faces", map(
                                "up", map("texture", "#net"), "down", map("texture", "#net")))));
                if (target.modelRenderTypes) m.put("render_type", "minecraft:cutout");
                write("assets/" + NS + "/models/block/" + id + ".json", Json.write(m));
                write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", map("", map("model", model)))));
            }
            case GLASS -> {
                Map<String, Object> m = map("parent", "minecraft:block/cube_all", "textures", map("all", tex(b, "all")));
                if (target.modelRenderTypes) m.put("render_type", "minecraft:translucent");
                write("assets/" + NS + "/models/block/" + id + ".json", Json.write(m));
                write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", map("", map("model", model)))));
            }
            case GAS -> {
                Map<String, Object> m = map("parent", "minecraft:block/cube_all", "textures", map("all", tex(b, "all")));
                if (target.modelRenderTypes) m.put("render_type", "minecraft:translucent");
                write("assets/" + NS + "/models/block/" + id + ".json", Json.write(m));
                write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", map("", map("model", model)))));
            }
            case CORAL, LIVING_CORAL -> {
                // A cross plant like vanilla coral; the waterlogged state uses the same model.
                Map<String, Object> m = map("parent", "minecraft:block/cross", "textures", map("cross", tex(b, "all")));
                if (target.modelRenderTypes) m.put("render_type", "minecraft:cutout");
                write("assets/" + NS + "/models/block/" + id + ".json", Json.write(m));
                write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", map("", map("model", model)))));
            }
            case ANCIENT_SAND, BUBBLE_SAND -> {
                blockModel(id, "minecraft:block/cube_all", map("all", tex(b, "all")));
                write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", map("", map("model", model)))));
            }
            case CUSTOM -> customBlock(b);
            case LAMP -> {
                // Legacy render pass 1: translucent. 26.1.2 picks the layer from the texture.
                for (String state : List.of("on", "off")) {
                    Map<String, Object> m = map("parent", "minecraft:block/cube_all", "textures", map("all", tex(b, state)));
                    if (target.modelRenderTypes) m.put("render_type", "minecraft:translucent");
                    write("assets/" + NS + "/models/block/" + id + (state.equals("on") ? "" : "_off") + ".json", Json.write(m));
                }
                write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(map("variants", map(
                        "lit=true", map("model", model), "lit=false", map("model", model + "_off")))));
            }
        }
        if (b.hasItem() && (b.shape() == Shape.OYSTER || b.shape() == Shape.NET)) {
            requireTexture("item", id, id);
            write("assets/" + NS + "/models/item/" + id + ".json",
                    Json.write(map("parent", "minecraft:item/generated", "textures", map("layer0", NS + ":item/" + id))));
        } else if (b.hasItem() && (b.shape() == Shape.CORAL || b.shape() == Shape.LIVING_CORAL)) {
            write("assets/" + NS + "/models/item/" + id + ".json",
                    Json.write(map("parent", "minecraft:item/generated", "textures", map("layer0", tex(b, "all")))));
        } else if (b.hasItem() && b.shape() != Shape.CUSTOM) {
            write("assets/" + NS + "/models/item/" + id + ".json", Json.write(map("parent", model)));
        }
        if (b.hasItem()) {
            clientItem(id, NS + ":item/" + id);
            lang.put("block." + NS + "." + id, b.englishName());
            // Tanks drop themselves with their fluid from the block entity instead.
            if (b.shape() == Shape.BUBBLE_SAND) dropLoot(id, "minecraft:sand");
            // BlockCoral.getItemDropped: living coral drops the dried coral.
            else if (b.shape() == Shape.LIVING_CORAL) dropLoot(id, NS + ":" + id.substring("live_".length()));
            else if (b.shape() == Shape.CUSTOM) { /* the block drops itself with its faces (CustomBlocks) */ }
            else if (b.shape() == Shape.ANCIENT_SAND) { /* drops come from the block class (AncientSand) */ }
            else if (!(b.isMachine() && mariculture.domain.machine.Machines.byId(id).create().keepsContents())) selfDropLoot(id);
        }
    }

    /**
     * A sawmill block: vanilla's own shape models (same rotations as vanilla's blockstates) on the template texture.
     * The client swaps every face for the face of the block it was cut from, so the models only give the shape.
     */
    private void customBlock(BlockSpec b) {
        String id = b.id(), base = NS + ":block/" + id, t = tex(b, "all");
        mariculture.domain.factory.Plans.Type type = null;
        for (mariculture.domain.factory.Plans.Type p : mariculture.domain.factory.Plans.Type.values()) if (p.block.equals(id)) type = p;
        if (type == null) throw new IllegalStateException(id + " is not a sawmill block");
        String item = base;
        Map<String, Object> variants = new LinkedHashMap<>();
        List<Object> multipart = new ArrayList<>();
        List<String> turn = List.of("north", "east", "south", "west"); // a quarter turn each
        switch (type) {
            case BLOCK, LIGHT, RF -> {
                blockModel(id, "minecraft:block/cube_all", map("all", t));
                variants.put("", map("model", base));
            }
            case FLOOR -> {
                blockModel(id, "minecraft:block/carpet", map("wool", t));
                variants.put("", map("model", base));
            }
            case STAIRS -> {
                Map<String, Object> tx = map("bottom", t, "top", t, "side", t);
                blockModel(id, "minecraft:block/stairs", tx);
                blockModel(id + "_inner", "minecraft:block/inner_stairs", tx);
                blockModel(id + "_outer", "minecraft:block/outer_stairs", tx);
                // BlockModelGenerators.createStairs: east 0, south 90, west 180, north 270; left shapes turn back a quarter
                // on the bottom half, right shapes forward a quarter on the top half.
                Map<String, Integer> y = Map.of("east", 0, "south", 90, "west", 180, "north", 270);
                for (String f : List.of("east", "north", "south", "west")) {
                    for (String h : List.of("bottom", "top")) {
                        for (String s : List.of("inner_left", "inner_right", "outer_left", "outer_right", "straight")) {
                            int r = y.get(f);
                            if (h.equals("bottom") && s.endsWith("left")) r += 270;
                            if (h.equals("top") && s.endsWith("right")) r += 90;
                            r %= 360;
                            Map<String, Object> v = map("model", base + (s.equals("straight") ? "" : "_" + s.substring(0, 5)));
                            if (h.equals("top")) v.put("x", 180);
                            if (r != 0) v.put("y", r);
                            if (h.equals("top") || r != 0) v.put("uvlock", true);
                            variants.put("facing=" + f + ",half=" + h + ",shape=" + s, v);
                        }
                    }
                }
            }
            case SLABS -> {
                Map<String, Object> tx = map("bottom", t, "top", t, "side", t);
                blockModel(id, "minecraft:block/slab", tx);
                blockModel(id + "_top", "minecraft:block/slab_top", tx);
                blockModel(id + "_double", "minecraft:block/cube_all", map("all", t));
                variants.put("type=bottom", map("model", base));
                variants.put("type=double", map("model", base + "_double"));
                variants.put("type=top", map("model", base + "_top"));
            }
            case FENCE -> {
                blockModel(id + "_post", "minecraft:block/fence_post", map("texture", t));
                blockModel(id + "_side", "minecraft:block/fence_side", map("texture", t));
                blockModel(id + "_inventory", "minecraft:block/fence_inventory", map("texture", t));
                item = base + "_inventory";
                multipart.add(map("apply", map("model", base + "_post")));
                for (int i = 0; i < 4; i++) {
                    multipart.add(map("when", map(turn.get(i), "true"), "apply", i == 0 ? map("model", base + "_side", "uvlock", true)
                            : map("model", base + "_side", "uvlock", true, "y", i * 90)));
                }
            }
            case GATE -> {
                for (String s : List.of("", "_open", "_wall", "_wall_open")) {
                    blockModel(id + s, "minecraft:block/template_fence_gate" + s, map("texture", t));
                }
                Map<String, Integer> y = Map.of("south", 0, "west", 90, "north", 180, "east", 270);
                for (String f : List.of("east", "north", "south", "west")) {
                    for (String w : List.of("false", "true")) {
                        for (String o : List.of("false", "true")) {
                            Map<String, Object> v = map("model", base + (w.equals("true") ? "_wall" : "") + (o.equals("true") ? "_open" : ""), "uvlock", true);
                            if (y.get(f) != 0) v.put("y", y.get(f));
                            variants.put("facing=" + f + ",in_wall=" + w + ",open=" + o, v);
                        }
                    }
                }
            }
            case WALL, RF_WALL -> {
                blockModel(id + "_post", "minecraft:block/template_wall_post", map("wall", t));
                blockModel(id + "_side", "minecraft:block/template_wall_side", map("wall", t));
                blockModel(id + "_side_tall", "minecraft:block/template_wall_side_tall", map("wall", t));
                blockModel(id + "_inventory", "minecraft:block/wall_inventory", map("wall", t));
                item = base + "_inventory";
                multipart.add(map("when", map("up", "true"), "apply", map("model", base + "_post")));
                for (String h : List.of("low", "tall")) {
                    String m = base + (h.equals("low") ? "_side" : "_side_tall");
                    for (int i = 0; i < 4; i++) {
                        multipart.add(map("when", map(turn.get(i), h), "apply", i == 0 ? map("model", m, "uvlock", true)
                                : map("model", m, "uvlock", true, "y", i * 90)));
                    }
                }
            }
        }
        write("assets/" + NS + "/blockstates/" + id + ".json", Json.write(multipart.isEmpty() ? map("variants", variants) : map("multipart", multipart)));
        write("assets/" + NS + "/models/item/" + id + ".json", Json.write(map("parent", item)));
    }

    private static Map<String, Object> box(double x1, double y1, double z1, double x2, double y2, double z2, String texture) {
        return map("from", List.of(x1, y1, z1), "to", List.of(x2, y2, z2), "faces", faces(texture == null ? "#shell" : texture));
    }

    private static Map<String, Object> faces(String texture) {
        Map<String, Object> faces = new LinkedHashMap<>();
        for (String f : List.of("down", "up", "north", "south", "west", "east")) faces.put(f, map("texture", texture));
        return faces;
    }

    private String tex(BlockSpec b, String slot) {
        if (b.textures().get(slot).contains(":")) return b.textures().get(slot); // a vanilla texture
        requireTexture("block", b.textures().get(slot), b.id());
        return NS + ":block/" + b.textures().get(slot);
    }

    private void requireTexture(String kind, String name, String owner) {
        if (!Files.exists(repo.resolve("content-source/textures/" + kind + "/" + name + ".png"))) {
            throw new IllegalStateException(owner + " references missing texture " + kind + "/" + name + ".png");
        }
    }

    private void blockModel(String name, String parent, Map<String, Object> textures) {
        write("assets/" + NS + "/models/block/" + name + ".json", Json.write(map("parent", parent, "textures", textures)));
    }

    private void selfDropLoot(String id) {
        dropLoot(id, NS + ":" + id);
    }

    private void dropLoot(String id, String item) {
        Map<String, Object> pool = map(
                "rolls", 1.0,
                "entries", List.of(map("type", "minecraft:item", "name", item)),
                "conditions", List.of(map("condition", "minecraft:survives_explosion")));
        Map<String, Object> table = map("type", "minecraft:block", "pools", List.of(pool));
        if (!target.pluralDataFolders) table.put("random_sequence", NS + ":blocks/" + id);
        write("data/" + NS + "/" + target.lootTables() + "/blocks/" + id + ".json", Json.write(table));
    }

    // ---- items -------------------------------------------------------------------------

    private void item(ItemSpec i) {
        requireTexture("item", i.texture(), i.id());
        if (i.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("jewelry")) {
            lang.put("item." + NS + "." + i.id(), i.englishName());
            return; // models from jewelryModels
        }
        String parent = i.kind() instanceof ItemKind.Rod ? "minecraft:item/handheld_rod" : "minecraft:item/generated";
        if (scubaLooks(i)) {
            lang.put("item." + NS + "." + i.id(), i.englishName());
            return;
        }
        write("assets/" + NS + "/models/item/" + i.id() + ".json",
                Json.write(map("parent", parent, "textures", map("layer0", NS + ":item/" + i.texture()))));
        if (i.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("metal_droplet") && target.clientItemDefinitions) {
            // Tinted from the colour the edition puts in its custom model data (MetalDropletItem.apply).
            write("assets/" + NS + "/items/" + i.id() + ".json", Json.write(map("model", map("type", "minecraft:model",
                    "model", NS + ":item/" + i.id(), "tints", List.of(map("type", "minecraft:custom_model_data", "index", 0, "default", -1))))));
        } else {
            clientItem(i.id(), NS + ":item/" + i.id());
        }
        lang.put("item." + NS + "." + i.id(), i.englishName());
    }


    /**
     * Thaumaturge looks for scuba gear (NeoForge editions): the warded infusion and the revealing upgrade switch the
     * item model, on the client properties mariculture:warded and mariculture:revealing (ScubaLooks). Revealing wins
     * over warded on the mask. Returns false for every other item.
     */
    private boolean scubaLooks(ItemSpec i) {
        if (target != Target.NEOFORGE_26_1_2 && target != Target.NEOFORGE_1_21_1) return false;
        List<String> looks = switch (i.id()) {
            case "scuba_mask" -> List.of("warded", "revealing");
            case "scuba_suit", "swimfin" -> List.of("warded");
            default -> List.of();
        };
        if (looks.isEmpty()) return false;
        String base = NS + ":item/" + i.id();
        for (String look : looks) {
            requireTexture("item", look + "_" + i.id(), i.id());
            write("assets/" + NS + "/models/item/" + look + "_" + i.id() + ".json", Json.write(map("parent", "minecraft:item/generated",
                    "textures", map("layer0", NS + ":item/" + look + "_" + i.id()))));
        }
        if (target.clientItemDefinitions) {
            write("assets/" + NS + "/models/item/" + i.id() + ".json", Json.write(map("parent", "minecraft:item/generated",
                    "textures", map("layer0", NS + ":item/" + i.texture()))));
            Object model = map("type", "minecraft:model", "model", base);
            for (String look : looks) {
                model = map("type", "minecraft:condition", "property", NS + ":" + look,
                        "on_true", map("type", "minecraft:model", "model", NS + ":item/" + look + "_" + i.id()), "on_false", model);
            }
            write("assets/" + NS + "/items/" + i.id() + ".json", Json.write(map("model", model)));
        } else {
            List<Object> overrides = new ArrayList<>();
            for (String look : looks) overrides.add(map("predicate", map(NS + ":" + look, 1), "model", NS + ":item/" + look + "_" + i.id()));
            write("assets/" + NS + "/models/item/" + i.id() + ".json", Json.write(map("parent", "minecraft:item/generated",
                    "textures", map("layer0", NS + ":item/" + i.texture()), "overrides", overrides)));
        }
        return true;
    }

    /**
     * Jewelry looks from their parts (legacy ItemJewelry render passes): a ring is its binding under its pearl, a
     * bracelet or necklace its pearl alone. One model per binding and material, picked by the stack's custom model
     * data: a string select on 26.1.2, integer overrides before (Jewelry.modelKey / modelIndex).
     */
    private void jewelryModels(JewelryType t) {
        String base = NS + ":item/jewelry/" + t.id() + "/";
        Map<String, String> byKey = new LinkedHashMap<>();
        Map<Integer, String> byIndex = new TreeMap<>();
        for (Binding b : Binding.values()) {
            for (JewelryMaterial m : JewelryMaterial.values()) {
                requireTexture("item", "jewelry/" + t.id() + "/" + m.id(), t.id());
                String name = t.id() + "_" + b.id() + "_" + m.id();
                Map<String, Object> textures = t == JewelryType.RING
                        ? map("layer0", base + "binding_" + b.id(), "layer1", base + m.id()) : map("layer0", base + m.id());
                write("assets/" + NS + "/models/item/" + name + ".json", Json.write(map("parent", "minecraft:item/generated", "textures", textures)));
                mariculture.domain.machine.Stack piece = Jewelry.create(t, b, m);
                byKey.put(Jewelry.modelKey(piece), NS + ":item/" + name);
                byIndex.put(Jewelry.modelIndex(piece), NS + ":item/" + name);
            }
        }
        if (t == JewelryType.RING) {
            write("assets/" + NS + "/models/item/ring_one_ring.json",
                    Json.write(map("parent", "minecraft:item/generated", "textures", map("layer0", base + "one_ring"))));
            byKey.put(Jewelry.ONE_RING, NS + ":item/ring_one_ring");
            byIndex.put(Jewelry.modelIndex(Jewelry.oneRing()), NS + ":item/ring_one_ring");
        }
        String plain = byKey.get(Binding.BASIC.id() + "_" + JewelryMaterial.WHITE.id());
        if (target.clientItemDefinitions) {
            List<Object> cases = new ArrayList<>();
            byKey.forEach((key, model) -> cases.add(map("when", key, "model", map("type", "minecraft:model", "model", model))));
            write("assets/" + NS + "/items/" + t.id() + ".json", Json.write(map("model", map("type", "minecraft:select",
                    "property", "minecraft:custom_model_data", "index", 0, "cases", cases,
                    "fallback", map("type", "minecraft:model", "model", plain)))));
        } else {
            List<Object> overrides = new ArrayList<>();
            byIndex.forEach((index, model) -> overrides.add(map("predicate", map("custom_model_data", index), "model", model)));
            write("assets/" + NS + "/models/item/" + t.id() + ".json", Json.write(map("parent", plain, "overrides", overrides)));
        }
    }

    /**
     * The jewelry enchantments as data (1.21+ registry): pearls only, all in one exclusive set, in no vanilla
     * enchanting tag so only the mirror and dungeon loot give them. The costs are the domain formulas at levels 1
     * and 2; the mirror uses the exact ones. 1.20.1 registers them in code.
     */
    private void jewelryEnchantments() {
        if (target.pluralDataFolders) return;
        List<String> ids = new ArrayList<>();
        for (JewelryEnchantment e : JewelryEnchantment.values()) {
            ids.add(e.id());
            write("data/" + NS + "/enchantment/" + e.path() + ".json", Json.write(map(
                    "description", map("translate", "enchantment." + NS + "." + e.path()),
                    "supported_items", "#" + NS + ":pearls",
                    "weight", e.weight,
                    "max_level", e.maxLevel,
                    "min_cost", map("base", e.minCost(1), "per_level_above_first", e.minCost(2) - e.minCost(1)),
                    "max_cost", map("base", e.maxCost(1), "per_level_above_first", e.maxCost(2) - e.maxCost(1)),
                    "anvil_cost", 4,
                    "slots", List.of("any"),
                    "exclusive_set", "#" + NS + ":exclusive_set/jewelry")));
        }
        write("data/" + NS + "/tags/enchantment/exclusive_set/jewelry.json", Json.write(map("values", ids)));
    }

    /**
     * Armour layers: 1.20.1 and 1.21.1 read textures/models/armor/<material>_layer_1.png (layer_2 for leggings);
     * 26.1.2 reads an equipment asset JSON pointing at textures/entity/equipment/humanoid[_leggings]/.
     */
    private void armorTextures(ArmorMaterialSpec m) throws IOException {
        requireTexture("armor", m.texture(), m.id());
        Path src = repo.resolve("content-source/textures/armor/" + m.texture() + ".png");
        Path legs = repo.resolve("content-source/textures/armor/" + m.texture() + "_legs.png");
        if (m.hasLegsLayer()) requireTexture("armor", m.texture() + "_legs", m.id());
        if (target.equipmentAssets) {
            Map<String, Object> layers = map("humanoid", List.of(map("texture", NS + ":" + m.id())));
            copy(src, "assets/" + NS + "/textures/entity/equipment/humanoid/" + m.id() + ".png");
            if (m.hasLegsLayer()) {
                layers.put("humanoid_leggings", List.of(map("texture", NS + ":" + m.id())));
                copy(legs, "assets/" + NS + "/textures/entity/equipment/humanoid_leggings/" + m.id() + ".png");
            }
            write("assets/" + NS + "/equipment/" + m.id() + ".json", Json.write(map("layers", layers)));
        } else {
            copy(src, "assets/" + NS + "/textures/models/armor/" + m.id() + "_layer_1.png");
            if (m.hasLegsLayer()) copy(legs, "assets/" + NS + "/textures/models/armor/" + m.id() + "_layer_2.png");
        }
    }

    private void copy(Path from, String rel) throws IOException {
        Path p = out.resolve(rel);
        Files.createDirectories(p.getParent());
        if (Files.exists(p)) throw new IllegalStateException("Two generators wrote " + rel);
        Files.copy(from, p);
    }

    /** Fluid type name, and for liquids a world block whose model only names the particle texture (the loader draws the fluid). */
    private void fluid(FluidSpec f) {
        if (f.ownTextures()) {
            requireTexture("block", "fluid/" + f.texture() + "_still", f.id());
            requireTexture("block", "fluid/" + f.texture() + "_flow", f.id());
        }
        lang.put("fluid_type." + NS + "." + f.id(), f.englishName());
        if (!f.hasBlock()) return;
        lang.put("block." + NS + "." + f.blockId(), f.englishName());
        write("assets/" + NS + "/models/block/" + f.blockId() + ".json", Json.write(map("textures", map("particle", f.stillTexture()))));
        write("assets/" + NS + "/blockstates/" + f.blockId() + ".json", Json.write(map("variants", map("", map("model", NS + ":block/" + f.blockId())))));
    }

    private void clientItem(String id, String model) {
        if (!target.clientItemDefinitions) return;
        write("assets/" + NS + "/items/" + id + ".json", Json.write(map("model", map("type", "minecraft:model", "model", model))));
    }

    // ---- advancements ------------------------------------------------------------------

    private void advancements() {
        String folder = target.pluralDataFolders ? "advancements" : "advancement";
        for (mariculture.domain.content.Advancements.Entry a : mariculture.domain.content.Advancements.all()) {
            String key = "advancements." + NS + "." + a.id();
            lang.put(key + ".title", a.title());
            lang.put(key + ".description", a.description());
            Map<String, Object> display = new LinkedHashMap<>();
            String icon = Ids.full(a.icon());
            display.put("icon", target.pluralDataFolders ? map("item", icon) : map("id", icon));
            display.put("title", map("translate", key + ".title"));
            display.put("description", map("translate", key + ".description"));
            display.put("frame", a.frame().name().toLowerCase(java.util.Locale.ROOT));
            if (a.parent() == null) {
                display.put("background", target.clientItemDefinitions ? "minecraft:block/prismarine_bricks"
                        : "minecraft:textures/block/prismarine_bricks.png");
                display.put("announce_to_chat", false);
                display.put("show_toast", false);
            }
            Map<String, Object> criteria = new LinkedHashMap<>();
            List<Object> any = new ArrayList<>();
            for (int i = 0; i < a.items().size(); i++) {
                String it = a.items().get(i);
                Object predicate;
                if (it.startsWith("#")) {
                    predicate = target.pluralDataFolders ? map("tag", it.substring(1)) : map("items", it);
                } else {
                    predicate = target.pluralDataFolders ? map("items", List.of(Ids.full(it))) : map("items", Ids.full(it));
                }
                String name = "has_" + i;
                criteria.put(name, map("trigger", "minecraft:inventory_changed", "conditions", map("items", List.of(predicate))));
                any.add(name);
            }
            Map<String, Object> json = new LinkedHashMap<>();
            if (a.parent() != null) json.put("parent", NS + ":" + a.parent());
            json.put("display", display);
            json.put("criteria", criteria);
            json.put("requirements", List.of(any));
            write("data/" + NS + "/" + folder + "/" + a.id() + ".json", Json.write(json));
        }
    }

    // ---- recipes -----------------------------------------------------------------------

    private void recipe(Recipe r) {
        write("data/" + NS + "/" + target.recipes() + "/" + r.id() + ".json", Json.write(recipeJson(r)));
    }

    /**
     * A recipe that only loads while a plugin is active: the loader's mod_loaded condition, then Mariculture's
     * plugin condition (the "plugins" config switch), registered by each target.
     */
    private void pluginRecipe(Plugins.Plugin p, String id, Map<String, Object> json) {
        pluginRecipe(p, id, json, List.of());
    }

    /** The same, also needing every mod in {@code alsoNeeds} (an addon of the plugin's mod). */
    private void pluginRecipe(Plugins.Plugin p, String id, Map<String, Object> json, List<String> alsoNeeds) {
        String loader = target.forgeTagNamespace ? "forge" : "neoforge";
        List<Object> conditions = new java.util.ArrayList<>(List.of(map("type", loader + ":mod_loaded", "modid", p.modId())));
        for (String mod : alsoNeeds) conditions.add(map("type", loader + ":mod_loaded", "modid", mod));
        conditions.add(map("type", NS + ":plugin", "plugin", p.name()));
        json.put(target.forgeTagNamespace ? "conditions" : "neoforge:conditions", conditions);
        write("data/" + NS + "/" + target.recipes() + "/" + id + ".json", Json.write(json));
    }

    /**
     * A recipe over another mod's items that loads only while its item tag has something in it (the loader's not and
     * tag_empty conditions): an ingredient on an empty tag fails to load on newer versions.
     */
    private void tagGatedRecipe(String id, Map<String, Object> json, String tag) {
        String loader = target.forgeTagNamespace ? "forge" : "neoforge";
        Object notEmpty = map("type", loader + ":not", "value", map("type", loader + ":tag_empty", "tag", TagNames.forLoader(tag, target.forgeTagNamespace)));
        json.put(target.forgeTagNamespace ? "conditions" : "neoforge:conditions", List.of(notEmpty));
        write("data/" + NS + "/" + target.recipes() + "/" + id + ".json", Json.write(json));
    }

    /**
     * Tinkers' Construct smeltery data (PluginTConstruct, PLUG-003). Tinkers only has a Forge 1.20.1 release among
     * the three editions, so only that edition gets these files; their format is Tinkers 3.x's.
     */
    private void tinkers() {
        Plugins.Plugin p = Plugins.TCONSTRUCT;
        pluginRecipe(p, "tconstruct/natural_gas_fuel", map("type", "tconstruct:melting_fuel",
                "fluid", map("fluid", Fluids.NATURAL_GAS, "amount", Plugins.TINKERS_GAS_AMOUNT),
                "duration", Plugins.TINKERS_GAS_DURATION, "temperature", Plugins.TINKERS_GAS_TEMPERATURE, "rate", Plugins.TINKERS_GAS_RATE));
        for (Plugins.TinkersMetal m : Plugins.TINKERS_METALS) {
            String fluid = Fluids.molten(m.metal());
            String[][] forms = {{"ingots", "ingot", "" + MetalRates.INGOT, "60"}, {"nuggets", "nugget", "" + MetalRates.NUGGET, "20"},
                    {"storage_blocks", "block", "" + MetalRates.BLOCK, "180"}, {"ores", "ore", "" + MetalRates.ORE, "90"}};
            for (String[] f : forms) {
                if (f[1].equals("ore") && !m.ore()) continue;
                pluginRecipe(p, "tconstruct/melting/" + m.metal() + "_" + f[1], map("type", "tconstruct:melting",
                        "ingredient", map("tag", TagNames.forLoader("c:" + f[0] + "/" + m.metal(), true)),
                        "result", map("fluid", fluid, "amount", Integer.parseInt(f[2])), "temperature", m.temperature(), "time", Integer.parseInt(f[3])));
            }
            for (String form : List.of("ingot", "nugget")) {
                int amount = form.equals("ingot") ? MetalRates.INGOT : MetalRates.NUGGET;
                int cooling = form.equals("ingot") ? 60 : 20;
                pluginRecipe(p, "tconstruct/casting/" + m.metal() + "_" + form + "_gold_cast", map("type", "tconstruct:casting_table",
                        "cast", map("tag", "tconstruct:casts/multi_use/" + form), "fluid", map("fluid", fluid, "amount", amount),
                        "result", map("item", NS + ":" + m.metal() + "_" + form), "cooling_time", cooling));
                pluginRecipe(p, "tconstruct/casting/" + m.metal() + "_" + form + "_sand_cast", map("type", "tconstruct:casting_table",
                        "cast", map("tag", "tconstruct:casts/single_use/" + form), "cast_consumed", true, "fluid", map("fluid", fluid, "amount", amount),
                        "result", map("item", NS + ":" + m.metal() + "_" + form), "cooling_time", cooling));
            }
            pluginRecipe(p, "tconstruct/casting/" + m.metal() + "_block", map("type", "tconstruct:casting_basin",
                    "fluid", map("fluid", fluid, "amount", MetalRates.BLOCK), "result", map("item", NS + ":" + m.metal() + "_block"), "cooling_time", 180));
        }
        pluginRecipe(p, "tconstruct/melting/limestone", map("type", "tconstruct:melting", "ingredient", map("tag", "mariculture:limestone"),
                "result", map("fluid", "mariculture:quicklime", "amount", 900), "temperature", Plugins.TINKERS_LIMESTONE_TEMPERATURE, "time", 60));
        int n = Plugins.TINKERS_ALLOY_AMOUNT;
        pluginRecipe(p, "tconstruct/alloy/titanium", map("type", "tconstruct:alloy",
                "inputs", List.of(map("fluid", Fluids.molten("rutile"), "amount", n), map("fluid", Fluids.molten("magnesium"), "amount", n)),
                "result", map("fluid", Fluids.molten("titanium"), "amount", n), "temperature", 1400));        tinkersTitanium(p);
    }

    /**
     * The Tinkers plugin (PLUG-003) on Hephaestus, its NeoForge 1.21.1 and 26.1.2 successor. Mariculture titanium melts
     * into Hephaestus' own titanium on its scale (Plugins.HEPHAESTUS_TITANIUM); rutile and magnesium melt into
     * Mariculture's fluids on Mariculture's scale; rutile and magnesium alloy into titanium; limestone melts into
     * quicklime; and titanium casts every Hephaestus Tools part. The two Hephaestus versions write recipes differently
     * (see docs/compat_targets.md); both formats are copied from their jars (1.0.20 and 1.0.19).
     */
    private void hephaestus() {
        Plugins.Plugin p = Plugins.HEPHAESTUS;
        boolean v21 = target == Target.NEOFORGE_1_21_1;
        String ti = Plugins.HEPHAESTUS_TITANIUM;
        int t = Plugins.HEPHAESTUS_TEMPERATURE;
        Object[][] titanium = {{"ingots", "ingot", Plugins.HEPHAESTUS_INGOT, 100}, {"nuggets", "nugget", Plugins.HEPHAESTUS_NUGGET, 33},
                {"storage_blocks", "block", Plugins.HEPHAESTUS_BLOCK, 200}};
        for (Object[] f : titanium) hephMelt(p, "titanium_" + f[1], "#c:" + f[0] + "/titanium", ti, (int) f[2], t, (int) f[3], v21);
        for (Plugins.TinkersMetal m : Plugins.TINKERS_METALS) {
            if (m.metal().equals("titanium")) continue;
            String fluid = Fluids.molten(m.metal());
            Object[][] forms = {{"ingots", "ingot", MetalRates.INGOT, 100}, {"nuggets", "nugget", MetalRates.NUGGET, 33},
                    {"storage_blocks", "block", MetalRates.BLOCK, 200}, {"ores", "ore", MetalRates.ORE, 150}};
            for (Object[] f : forms) {
                if (f[1].equals("ore") && !m.ore()) continue;
                hephMelt(p, m.metal() + "_" + f[1], "#c:" + f[0] + "/" + m.metal(), fluid, (int) f[2], Math.min(m.temperature(), t), (int) f[3], v21);
            }
            hephTable(p, m.metal() + "_ingot", "hephaestus:ingot_cast", fluid, MetalRates.INGOT, NS + ":" + m.metal() + "_ingot", 100, v21);
            hephTable(p, m.metal() + "_nugget", "hephaestus:nugget_cast", fluid, MetalRates.NUGGET, NS + ":" + m.metal() + "_nugget", 33, v21);
            hephBasin(p, m.metal() + "_block", fluid, MetalRates.BLOCK, NS + ":" + m.metal() + "_block", 200, v21);
        }
        hephTable(p, "titanium_ingot", "hephaestus:ingot_cast", ti, Plugins.HEPHAESTUS_INGOT, NS + ":titanium_ingot", 100, v21);
        hephTable(p, "titanium_nugget", "hephaestus:nugget_cast", ti, Plugins.HEPHAESTUS_NUGGET, NS + ":titanium_nugget", 33, v21);
        hephBasin(p, "titanium_block", ti, Plugins.HEPHAESTUS_BLOCK, NS + ":titanium_block", 200, v21);
        hephMelt(p, "limestone", "#mariculture:limestone", "mariculture:quicklime", 900, Plugins.TINKERS_LIMESTONE_TEMPERATURE, 60, v21);
        // One rutile ingot and one magnesium ingot make one titanium ingot, as the Tinkers alloy did (PluginTConstruct.java:144).
        pluginRecipe(p, "hephaestus/alloy/titanium", map("type", v21 ? "hephaestus:alloying" : "hephaestus:alloy",
                "inputs", List.of(map("amount", MetalRates.INGOT, "id", Fluids.molten("rutile")), map("amount", MetalRates.INGOT, "id", Fluids.molten("magnesium"))),
                "output", map("amount", Plugins.HEPHAESTUS_INGOT, "id", ti), "temperature", t));
        // Tool parts. Hephaestus Tools 26.1.2 ships the titanium material itself (behind a non-empty c:ingots/titanium);
        // 1.21.1 (1.0.3) has none, so this edition ships it, with 26.1.2's stats. Its part recipes there only load with
        // other titanium mods, so both editions cast and melt the parts themselves.
        if (v21) {
            write("data/hephaestus_tools/hephaestus_tools/materials/titanium.json", Json.write(map("id", "hephaestus_tools:titanium",
                    "ingredient", map("tag", "c:ingots/titanium"), "color", 10652854,
                    "head_stats", map("durability", 850, "mining_speed", 8.0, "attack_damage", 2.5, "attack_speed", 1.0, "tier", 3,
                            "enchantability", 14, "trait", "lightweight"),
                    "handle_stats", map("durability_mult", 0.05, "speed_mult", 0.02, "damage_mult", 0.03, "attack_speed_mult", 0.0),
                    "binding_stats", map("durability", 170))));
        }
        List<String> tools = List.of("hephaestus_tools");
        new java.util.TreeMap<>(Plugins.HEPHAESTUS_PARTS).forEach((part, amount) -> {
            String item = "hephaestus_tools:" + part;
            pluginRecipe(p, "hephaestus/tools/casting/titanium_" + part, map("type", "hephaestus:casting_table",
                    "cast", v21 ? map("item", item + "_cast") : item + "_cast", "cast_consumed", false, "cooling_time", 120,
                    "fluid", map("amount", amount, "id", ti),
                    "result", v21 ? item : map("count", 1, "id", item)), tools);
            Map<String, Object> ingredient = new java.util.LinkedHashMap<>();
            ingredient.put(v21 ? "type" : "neoforge:ingredient_type", "neoforge:components");
            ingredient.put("components", map("hephaestus_tools:part_material", "hephaestus_tools:titanium"));
            ingredient.put("items", item);
            ingredient.put("strict", false);
            pluginRecipe(p, "hephaestus/tools/melting/titanium_" + part, map("type", "hephaestus:melting",
                    "fuel", map("amount", 50, "id", "minecraft:lava"), "ingredient", ingredient,
                    "result", map("amount", amount, "id", ti), "temperature", t, "time", 120), tools);
        });
    }

    private void hephMelt(Plugins.Plugin p, String id, String input, String fluid, int amount, int temperature, int time, boolean v21) {
        Object ingredient = v21 ? (input.startsWith("#") ? map("tag", input.substring(1)) : map("item", input)) : input;
        pluginRecipe(p, "hephaestus/melting/" + id, map("type", "hephaestus:melting", "fuel", map("amount", 50, "id", "minecraft:lava"),
                "ingredient", ingredient, "result", map("amount", amount, "id", fluid), "temperature", temperature, "time", time));
    }

    private void hephTable(Plugins.Plugin p, String id, String cast, String fluid, int amount, String result, int cooling, boolean v21) {
        pluginRecipe(p, "hephaestus/casting/" + id, map("type", "hephaestus:casting_table", "cast", v21 ? map("item", cast) : cast,
                "cooling_time", cooling, "fluid", map("amount", amount, "id", fluid), "result", v21 ? result : map("id", result)));
    }

    private void hephBasin(Plugins.Plugin p, String id, String fluid, int amount, String result, int cooling, boolean v21) {
        Map<String, Object> json = v21
                ? map("type", "hephaestus:casting_basin", "input", map("amount", amount, "id", fluid), "result", result, "time", cooling)
                : map("type", "hephaestus:casting_basin", "cooling_time", cooling, "fluid", map("amount", amount, "id", fluid), "result", map("id", result));
        pluginRecipe(p, "hephaestus/casting/" + id, json);
    }

    /**
     * The titanium tool material (TitaniumTools, Plugins.TITANIUM_*) as Tinkers 3 material data: definition (behind the
     * same two conditions as the recipes), stats, traits, the part builder and casting recipes, and render info that
     * tints Tinkers' own grey part sprites. A material stands for one ingot, as Mariculture's 144 mB.
     */
    private void tinkersTitanium(Plugins.Plugin p) {
        String material = NS + ":titanium", dir = "data/" + NS + "/tinkering/materials/";
        write(dir + "definition/titanium.json", Json.write(map("condition", map("type", "forge:and", "values", List.of(
                        map("type", "forge:mod_loaded", "modid", p.modId()), map("type", NS + ":plugin", "plugin", p.name()))),
                "craftable", false, "hidden", false, "sortOrder", 10, "tier", Plugins.TITANIUM_TIER)));
        write(dir + "stats/titanium.json", Json.write(map("stats", map(
                "tconstruct:head", map("durability", Plugins.TITANIUM_DURABILITY, "melee_attack", Plugins.TITANIUM_ATTACK,
                        "mining_speed", Plugins.TITANIUM_MINING_SPEED, "mining_tier", "minecraft:diamond"),
                "tconstruct:handle", map("durability", Plugins.TITANIUM_HANDLE_DURABILITY, "melee_damage", 0.0, "melee_speed", 0.0, "mining_speed", 0.0),
                "tconstruct:binding", map()))));
        write(dir + "traits/titanium.json", Json.write(map("default", List.of(map("level", Plugins.TITANIUM_REINFORCED, "name", "tconstruct:reinforced")))));
        write("assets/" + NS + "/tinkering/materials/titanium.json", Json.write(map("color", Plugins.TITANIUM_COLOUR, "fallbacks", List.of("metal"))));
        String fluid = Fluids.molten("titanium");
        String[][] forms = {{"ingots", "1", "1"}, {"nuggets", "9", "1"}, {"storage_blocks", "1", "9"}};
        for (String[] f : forms) {
            Map<String, Object> json = map("type", "tconstruct:material", "ingredient", map("tag", TagNames.forLoader("c:" + f[0] + "/titanium", true)),
                    "material", material, "needed", Integer.parseInt(f[1]), "value", Integer.parseInt(f[2]));
            if (f[0].equals("storage_blocks")) json.put("leftover", map("count", 1, "tag", TagNames.forLoader("c:ingots/titanium", true)));
            pluginRecipe(p, "tconstruct/materials/titanium/" + f[0], json);
        }
        pluginRecipe(p, "tconstruct/materials/titanium/casting", map("type", "tconstruct:material_fluid",
                "fluid", map("fluid", fluid, "amount", MetalRates.INGOT), "output", material, "temperature", 1400));
        pluginRecipe(p, "tconstruct/materials/titanium/melting", map("type", "tconstruct:material_melting",
                "input", material, "result", map("fluid", fluid, "amount", MetalRates.INGOT), "temperature", 1400));
    }

    /**
     * Blood Magic's binding array turns a titanium rod into the bound rod (PluginBloodMagic.java:100,
     * BindingRegistry.registerRecipe). Blood Magic 3 only has a Forge 1.20.1 release among the three editions; the
     * format is its own bloodmagic:array recipes'.
     */
    private void bloodMagic() {
        pluginRecipe(Plugins.BLOOD_MAGIC, "bloodmagic/bound_fishing_rod", map("type", "bloodmagic:array",
                "addedinput", map("item", NS + ":titanium_fishing_rod"), "baseinput", map("item", "bloodmagic:reagentbinding"),
                "output", map("item", NS + ":bound_fishing_rod"), "texture", "bloodmagic:textures/models/alchemyarrays/bindingarray.png"));
    }

    /**
     * The bound rod's binding in Neo Vitae (PLUG-010 on NeoForge 1.21.1 and 26.1.2), the successor of Blood Magic: the
     * same binding array on its ids. Neo Vitae 1.21.1 writes ingredients as objects, 26.1.2 as plain ids.
     */
    private void neoVitae() {
        boolean objects = target == Target.NEOFORGE_1_21_1;
        Object rod = objects ? map("item", NS + ":titanium_fishing_rod") : NS + ":titanium_fishing_rod";
        Object reagent = objects ? map("item", "neovitae:reagent_binding") : "neovitae:reagent_binding";
        pluginRecipe(Plugins.NEO_VITAE, "neovitae/bound_fishing_rod", map("type", "neovitae:array", "addedinput", rod, "baseinput", reagent,
                "output", map("count", 1, "id", NS + ":bound_fishing_rod"), "texture", "neovitae:textures/models/alchemyarrays/bindingarray.png"));
    }

    /**
     * ProjectE EMC values and the anvil's item conversions (PluginEE3, PLUG-021) as ProjectE custom conversions.
     * ProjectE 1.20.1 takes plain id maps; 1.21+ takes typed entries.
     */
    private void projectE() {
        boolean plain = target.forgeTagNamespace;
        Object before;
        if (plain) {
            before = new java.util.LinkedHashMap<String, Object>(Plugins.EMC);
        } else {
            before = Plugins.EMC.entrySet().stream().map(e -> map("type", "projecte:item", "id", e.getKey(), "emc_value", e.getValue())).toList();
        }
        List<Object> conversions = new java.util.ArrayList<>();
        for (mariculture.domain.metal.Anvil.Recipe r : mariculture.domain.metal.Anvil.recipes()) {
            if (!r.input().startsWith(NS + ":") && !r.output().startsWith(NS + ":")) continue;
            conversions.add(plain ? map("count", r.count(), "ingredients", List.of(r.input()), "output", r.output())
                    : map("count", r.count(), "ingredients", List.of(map("type", "projecte:item", "id", r.input())),
                            "output", map("type", "projecte:item", "id", r.output())));
        }
        write("data/" + NS + "/pe_custom_conversions/mariculture.json", Json.write(map(
                "comment", "Mariculture EMC values (legacy EE3 plugin) and anvil conversions.",
                "groups", map("anvil", map("comment", "Hammering on the anvil.", "conversions", conversions)),
                "values", map("before", before))));
    }

    /**
     * Forestry squeezer and bee-bait recipes (PluginForestry, PLUG-004). Forestry has releases for Forge 1.20.1
     * (2.x) and NeoForge 1.21.1 (3.0 alpha), with different stack formats; the formats are copied from their jars.
     */
    private void forestry() {
        Plugins.Plugin p = Plugins.FORESTRY;
        boolean old = target == Target.FORGE_1_20_1;
        Path dir = repo.resolve("content-source/data/mariculture/mariculture/fish_species");
        List<Path> files;
        try (var list = Files.list(dir)) {
            files = list.filter(f -> f.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (Path f : files) {
            String id = f.getFileName().toString().replace(".json", "");
            mariculture.domain.fish.ShippedSpecies.Entry entry = mariculture.domain.fish.ShippedSpecies.byId(id);
            if (entry == null) continue;
            mariculture.domain.fish.Species sp;
            try {
                sp = mariculture.domain.fish.Species.parse(id, Files.readString(f), f.getFileName().toString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            Plugins.Squeeze q = Plugins.squeeze(sp, entry.rawItem());
            if (q == null) continue;
            Object output = old ? map("Amount", q.fishOil(), "FluidName", Fluids.FISH_OIL)
                    : map("stack", map("amount", q.fishOil(), "id", Fluids.FISH_OIL));
            Object remnant = old ? map("Count", 1, "id", q.remnant()) : map("count", 1, "id", q.remnant());
            Map<String, Object> json = map("type", "forestry:squeezer", "chance", q.chance(), "output", output,
                    "remnant", remnant, "resources", List.of(map("item", q.input())), "time", q.time());
            // Forestry 3.0's recipe codecs require the recipe's own id in the file (its own recipes all carry one).
            if (!old) json.put("id", NS + ":forestry/squeezer/" + id);
            pluginRecipe(p, "forestry/squeezer/" + id, json);
        }
        Plugins.FORESTRY_BEE_BAIT.forEach((bee, count) -> {
            String item = old ? "forestry:bee_" + bee + "_ge" : "forestry:" + bee + "_bee";
            Map<String, Object> json = recipeJson(new Recipe.Shapeless("forestry/bee_bait_from_" + bee,
                    List.of(Ingredient.item(item)), "mariculture:bait_bee", count, "PluginForestry.java:90-92"));
            pluginRecipe(p, "forestry/bee_bait_from_" + bee, json);
        });
        forestryBackpacks(p, old);
        // Fuels (PluginForestry.java:97, 113): Forestry 3.0 reads them from data maps (merged with Forestry's own);
        // Forestry 2.x keeps them in Java maps, which the Forge 1.20.1 edition fills at common setup.
        if (!old) {
            write("data/forestry/data_maps/fluid/biogas_fuels.json", Json.write(map("values", map(Fluids.FISH_OIL,
                    map("burn_duration", Plugins.FISH_OIL_BIOGAS_BURN, "dissipation_multiplier", Plugins.FISH_OIL_BIOGAS_DISSIPATION,
                            "power_per_cycle", Plugins.FISH_OIL_BIOGAS_POWER)))));
            write("data/forestry/data_maps/item/fermenter_fuels.json", Json.write(map("values", map(Plugins.FERMENTER_KELP,
                    map("burn_duration", Plugins.KELP_BURN_DURATION, "ferment_per_cycle", Plugins.KELP_FERMENT_PER_CYCLE)))));
        }
    }

    // Starcatcher fish entries (StarcatcherFish, SC1): read only by Starcatcher, so no load condition is needed.
    /**
     * S2 (PLUG-031): the "mariculture:fishing_catch" global loot modifier, which puts Mariculture's catch into the vanilla
     * fishing loot while the Stardew Fishing plugin is on (it checks the switch itself). Forge 1.20.1 and NeoForge 1.21.1
     * list modifiers in global_loot_modifiers.json; NeoForge 26.1.2 loads every file under loot_modifiers.
     */
    private void stardewCatch() {
        write("data/" + NS + "/loot_modifiers/stardew_catch.json", Json.write(map("type", NS + ":fishing_catch", "conditions", List.of())));
        if (target == Target.NEOFORGE_26_1_2) return;
        String ns = target == Target.FORGE_1_20_1 ? "forge" : "neoforge";
        write("data/" + ns + "/loot_modifiers/global_loot_modifiers.json",
                Json.write(map("replace", false, "entries", List.of(NS + ":stardew_catch"))));
    }

    /** Tide (Forge 1.20.1 and NeoForge 1.21.1; no 26.1.2 release): fish entries plus their journal lines (T2). */
    private void tide() {
        if (target == Target.NEOFORGE_26_1_2) return;
        Path dir = repo.resolve("content-source/data/mariculture/mariculture/fish_species");
        List<Path> files;
        try (var list = Files.list(dir)) {
            files = list.filter(f -> f.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (Path f : files) {
            String id = f.getFileName().toString().replace(".json", "");
            mariculture.domain.fish.ShippedSpecies.Entry entry = mariculture.domain.fish.ShippedSpecies.byId(id);
            if (entry == null) continue;
            mariculture.domain.fish.Species sp;
            try {
                sp = mariculture.domain.fish.Species.parse(id, Files.readString(f), f.getFileName().toString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (!mariculture.domain.plugin.TideFish.eligible(sp, entry.rawItem())) continue;
            write("data/" + NS + "/fishing/fish/" + mariculture.domain.plugin.TideFish.group(sp) + "/" + id + ".json",
                    Json.write(mariculture.domain.plugin.TideFish.json(id, sp, entry.rawItem())));
            lang.put(mariculture.domain.plugin.TideFish.descriptionKey(id), mariculture.domain.plugin.TideFish.description(sp));
        }
    }

    private void starcatcher() {
        Path dir = repo.resolve("content-source/data/mariculture/mariculture/fish_species");
        List<Path> files;
        try (var list = Files.list(dir)) {
            files = list.filter(f -> f.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        for (Path f : files) {
            String id = f.getFileName().toString().replace(".json", "");
            mariculture.domain.fish.ShippedSpecies.Entry entry = mariculture.domain.fish.ShippedSpecies.byId(id);
            if (entry == null) continue;
            mariculture.domain.fish.Species sp;
            try {
                sp = mariculture.domain.fish.Species.parse(id, Files.readString(f), f.getFileName().toString());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
            if (!mariculture.domain.plugin.StarcatcherFish.eligible(sp, entry.rawItem())) continue;
            write("data/" + NS + "/starcatcher/fish/" + id + ".json", Json.write(mariculture.domain.plugin.StarcatcherFish.json(sp, entry.rawItem())));
        }
    }

    /**
     * Complicated Bees (all three editions; Forestry's bees have no 26.1.2 release): its bees craft into bee bait in
     * the Forestry numbers (PluginForestry.java:90-92). Its bee items carry the species as data, so any species works.
     */
    private void complicatedBees() {
        Plugins.FORESTRY_BEE_BAIT.forEach((bee, count) -> {
            String id = "complicated_bees/bee_bait_from_" + bee;
            pluginRecipe(Plugins.COMPLICATED_BEES, id, recipeJson(new Recipe.Shapeless(id, List.of(Ingredient.item("complicated_bees:" + bee)),
                    "mariculture:bait_bee", count, "PluginForestry.java:90-92")));
        });
    }

    /**
     * The aquatic backpacks (Plugins.AQUA_BACKPACK): their item tag, models on Forestry's own tinted backpack models
     * (no new textures), the crafting recipe and the woven one in Forestry's carpenter. The items themselves register
     * in the edition while Forestry is loaded.
     */
    private void forestryBackpacks(Plugins.Plugin p, boolean old) {
        List<String> allowed = new java.util.ArrayList<>();
        for (BlockSpec b : Catalog.blocks()) if (Plugins.aquaBackpackItem(b.id())) allowed.add(Ids.full(b.id()));
        for (ItemSpec i : Catalog.items()) if (Plugins.aquaBackpackItem(i.id())) allowed.add(Ids.full(i.id()));
        allowed.addAll(Plugins.AQUA_BACKPACK_EXTRA);
        tagFile(target.itemTags(), Plugins.AQUA_BACKPACK_TAG, allowed);
        for (String[] b : new String[][] {{Plugins.AQUA_BACKPACK, "normal"}, {Plugins.WOVEN_AQUA_BACKPACK, "woven"}}) {
            List<Object> overrides = new java.util.ArrayList<>();
            String[] modes = {"locked", "receive", "resupply"};
            for (int m = 0; m < modes.length; m++) {
                overrides.add(map("model", "forestry:item/backpack/" + b[1] + "_" + modes[m], "predicate", map("minecraft:mode", m + 1.0)));
            }
            write("assets/" + NS + "/models/item/" + b[0] + ".json", Json.write(map("parent", "forestry:item/backpack/" + b[1] + "_neutral",
                    "overrides", overrides)));
        }
        Map<String, Object> shaped = recipeJson(new Recipe.Shaped("forestry/" + Plugins.AQUA_BACKPACK, List.of("SWS", "FCF", "SWS"),
                Map.of('S', Ingredient.tag("c:strings"), 'W', Ingredient.tag("minecraft:wool"), 'F', Ingredient.tag("minecraft:fishes"),
                        'C', Ingredient.tag("c:chests/wooden")), NS + ":" + Plugins.AQUA_BACKPACK, 1, "PluginForestry.java:110"));
        pluginRecipe(p, "forestry/" + Plugins.AQUA_BACKPACK, shaped);
        Map<String, Object> key = map("T", map("item", NS + ":" + Plugins.AQUA_BACKPACK), "W", map("item", "forestry:woven_silk"),
                "X", map("item", "minecraft:diamond"));
        List<String> pattern = List.of("WXW", "WTW", "WWW");
        String woven = NS + ":" + Plugins.WOVEN_AQUA_BACKPACK;
        Map<String, Object> inner = old
                ? map("type", "minecraft:crafting_shaped", "category", "equipment", "key", key, "pattern", pattern, "result", map("item", woven))
                : map("category", "equipment", "key", key, "pattern", pattern, "result", map("count", 1, "id", woven));
        Map<String, Object> carpenter = map("type", "forestry:carpenter", "box", List.of(),
                "liquid", old ? map("Amount", 1000, "FluidName", "minecraft:water") : map("amount", 1000, "id", "minecraft:water"),
                "recipe", inner, "time", 200);
        if (!old) carpenter.put("id", NS + ":forestry/" + Plugins.WOVEN_AQUA_BACKPACK);
        pluginRecipe(p, "forestry/" + Plugins.WOVEN_AQUA_BACKPACK, carpenter);
    }

    private Map<String, Object> recipeJson(Recipe r) {
        Map<String, Object> json;
        if (r instanceof Recipe.Shaped s) {
            Map<String, Object> key = new TreeMap<>();
            s.key().forEach((c, ing) -> key.put(String.valueOf(c), ingredient(ing)));
            json = map("type", "minecraft:crafting_shaped", "category", "building", "pattern", s.pattern(), "key", key,
                    "result", result(s.result(), s.count()));
        } else if (r instanceof Recipe.Shapeless s) {
            Map<String, Object> result = result(s.result(), s.count());
            if (!s.resultData().isEmpty()) {
                // Item data: Mariculture's item-family component since 1.20.5, the stack's NBT tag on Forge 1.20.1.
                if (target.pluralDataFolders) result.put("nbt", new TreeMap<>(s.resultData()));
                else result.put("components", map(NS + ":" + mariculture.domain.content.ItemData.component(s.result()), new TreeMap<>(s.resultData())));
            }
            json = map("type", "minecraft:crafting_shapeless", "category", "misc",
                    "ingredients", s.ingredients().stream().map(this::ingredient).toList(), "result", result);
        } else if (r instanceof Recipe.Smelting s) {
            Object res = target.pluralDataFolders ? Ids.full(s.result()) : map("id", Ids.full(s.result()));
            json = map("type", "minecraft:smelting", "category", "blocks", "ingredient", ingredient(s.input()),
                    "result", res, "experience", s.experience(), "cookingtime", s.cookingTime());
        } else if (r instanceof Recipe.Special sp) {
            json = map("type", NS + ":" + sp.type(), "category", "misc");
        } else if (r instanceof Recipe.BookShapeless b && target.bookComponent) {
            Map<String, Object> result = result("patchouli:guide_book", 1);
            result.put("components", map("patchouli:book", b.book()));
            json = map("type", "minecraft:crafting_shapeless", "category", "misc",
                    "ingredients", b.ingredients().stream().map(this::ingredient).toList(), "result", result);
            // Patchouli is optional on editions it has not reached (26.2); the book recipe loads only beside it.
            json.put("neoforge:conditions", List.of(map("type", "neoforge:mod_loaded", "modid", "patchouli")));
        } else if (r instanceof Recipe.BookShapeless b) {
            json = map("type", "patchouli:shapeless_book_recipe",
                    "ingredients", b.ingredients().stream().map(this::ingredient).toList(), "book", b.book(),
                    // Patchouli fills the result in when absent on some versions; stating it is harmless and explicit.
                    "result", result("patchouli:guide_book", 1));
        } else {
            throw new IllegalStateException("Unhandled recipe " + r);
        }
        return json;
    }

    private Object ingredient(Ingredient ing) {
        String value = ing.tag() ? TagNames.forLoader(ing.value(), target.forgeTagNamespace) : ing.value();
        if (target.stringIngredients) return ing.tag() ? "#" + value : value;
        return ing.tag() ? map("tag", value) : map("item", value);
    }

    private Map<String, Object> result(String id, int count) {
        return target.pluralDataFolders ? map("item", Ids.full(id), "count", count) : map("id", Ids.full(id), "count", count);
    }

    // ---- worldgen ---------------------------------------------------------------------

    private void ore(OreFeature o) {
        write("data/" + NS + "/worldgen/configured_feature/" + o.id() + ".json", Json.write(map(
                "type", "minecraft:ore",
                "config", map(
                        "discard_chance_on_air_exposure", 0.0,
                        "size", o.size(),
                        "targets", List.of(map(
                                "state", map("Name", Ids.full(o.block())),
                                "target", map("predicate_type", "minecraft:tag_match", "tag", o.replaceableTag())))))));
        List<Object> placement = new java.util.ArrayList<>();
        if (o.oneInChunks() > 1) placement.add(map("type", "minecraft:rarity_filter", "chance", o.oneInChunks()));
        placement.addAll(List.of(
                        map("type", "minecraft:count", "count", o.veinsPerChunk()),
                        map("type", "minecraft:in_square"),
                        map("type", "minecraft:height_range", "height", map(
                                "type", "minecraft:uniform",
                                "min_inclusive", map("absolute", o.minY()),
                                "max_inclusive", map("absolute", o.maxYExclusive() - 1))),
                        map("type", "minecraft:biome")));
        write("data/" + NS + "/worldgen/placed_feature/" + o.id() + ".json", Json.write(map(
                "feature", NS + ":" + o.id(),
                "placement", placement)));
        String loader = target.forgeTagNamespace ? "forge" : "neoforge";
        write("data/" + NS + "/" + loader + "/biome_modifier/" + o.id() + ".json", Json.write(map(
                "type", loader + ":add_features",
                "biomes", "#" + o.biomeTag(),
                "features", NS + ":" + o.id(),
                "step", "underground_ores")));
    }

    /** The legacy limestone sea bed: a custom feature run once per chunk, see SeabedFeature in each target. */
    private void seabed() {
        write("data/" + NS + "/worldgen/configured_feature/seabed.json", Json.write(map(
                "type", NS + ":seabed",
                "config", map())));
        write("data/" + NS + "/worldgen/placed_feature/seabed.json", Json.write(map(
                "feature", NS + ":seabed",
                "placement", List.of())));
        write("data/" + NS + "/tags/worldgen/biome/has_limestone_seabed.json", Json.write(map(
                "replace", false,
                "values", List.of("#minecraft:is_ocean", "#minecraft:is_river", "#minecraft:is_beach"))));
        // Loose sea floor the limestone bed lies under (kept as it is); a data pack can add other mods' floors.
        tagFile(target.blockTags(), NS + ":seabed_floor", List.of("#minecraft:sand", "minecraft:gravel", "minecraft:clay",
                "minecraft:sandstone", "#minecraft:dirt", "minecraft:mud"));
        String loader = target.forgeTagNamespace ? "forge" : "neoforge";
        write("data/" + NS + "/" + loader + "/biome_modifier/seabed.json", Json.write(map(
                "type", loader + ":add_features",
                "biomes", "#" + NS + ":has_limestone_seabed",
                "features", NS + ":seabed",
                "step", "raw_generation")));
    }

    /** Pearl oysters on saline sea floors: a custom feature run once per chunk, see OysterFeature in each target. */
    private void oysters() {
        write("data/" + NS + "/worldgen/configured_feature/oysters.json", Json.write(map(
                "type", NS + ":oysters",
                "config", map())));
        write("data/" + NS + "/worldgen/placed_feature/oysters.json", Json.write(map(
                "feature", NS + ":oysters",
                "placement", List.of())));
        // Legacy salinity table (Core.java:276-316): frozen ocean, ocean and deep ocean are SALINE.
        write("data/" + NS + "/tags/worldgen/biome/is_saline.json", Json.write(map(
                "replace", false, "values", List.of("#minecraft:is_ocean"))));
        write("data/" + NS + "/tags/worldgen/biome/has_oysters.json", Json.write(map(
                "replace", false, "values", List.of("#" + NS + ":is_saline"))));
        String loader = target.forgeTagNamespace ? "forge" : "neoforge";
        write("data/" + NS + "/" + loader + "/biome_modifier/oysters.json", Json.write(map(
                "type", loader + ":add_features",
                "biomes", "#" + NS + ":has_oysters",
                "features", NS + ":oysters",
                "step", "vegetal_decoration")));
    }

    /** Coral reefs, kelp forests and ancient sand on saline floors: one custom feature, see OceanDecorFeature in each target. */
    private void oceanDecor() {
        write("data/" + NS + "/worldgen/configured_feature/ocean_decor.json", Json.write(map(
                "type", NS + ":ocean_decor",
                "config", map())));
        write("data/" + NS + "/worldgen/placed_feature/ocean_decor.json", Json.write(map(
                "feature", NS + ":ocean_decor",
                "placement", List.of())));
        String loader = target.forgeTagNamespace ? "forge" : "neoforge";
        write("data/" + NS + "/" + loader + "/biome_modifier/ocean_decor.json", Json.write(map(
                "type", loader + ":add_features",
                "biomes", "#" + NS + ":is_saline",
                "features", NS + ":ocean_decor",
                "step", "vegetal_decoration")));
        // WorldGenKelp.generateChest: nextInt(max - min) + min weighted picks (WorldPlus.java:131-156).
        mariculture.domain.worldgen.OceanDecor.Settings s = mariculture.domain.worldgen.OceanDecor.Settings.LEGACY;
        List<Object> entries = new ArrayList<>();
        for (mariculture.domain.worldgen.OceanDecor.ChestLoot l : mariculture.domain.worldgen.OceanDecor.OCEAN_CHEST) {
            Map<String, Object> e = map("type", "minecraft:item", "name", l.item(), "weight", l.weight());
            if (l.max() > 1) {
                Object count = l.min() == l.max() ? (Object) (double) l.min()
                        : map("type", "minecraft:uniform", "min", (double) l.min(), "max", (double) l.max());
                e.put("functions", List.of(map("function", "minecraft:set_count", "count", count)));
            }
            entries.add(e);
        }
        Map<String, Object> pool = map(
                "rolls", map("type", "minecraft:uniform", "min", (double) s.chestMin(), "max", (double) (s.chestMax() - 1)),
                "entries", entries);
        Map<String, Object> table = map("type", "minecraft:chest", "pools", List.of(pool));
        if (!target.pluralDataFolders) table.put("random_sequence", NS + ":chests/ocean_chest");
        write("data/" + NS + "/" + target.lootTables() + "/chests/ocean_chest.json", Json.write(table));
    }

    /** The Rockhopper's sounds and beach spawns, see Rockhopper in each target. */
    private void rockhopper() throws IOException {
        Map<String, Object> sounds = new LinkedHashMap<>();
        for (String s : mariculture.domain.worldgen.Rockhopper.SOUNDS) {
            sounds.put("entity.rockhopper." + s, map("subtitle", "subtitles." + NS + ".entity.rockhopper." + s,
                    "sounds", List.of(NS + ":rockhopper_" + s)));
        }
        sounds.put(mariculture.domain.maritech.Fludd.SOUND, map("subtitle", "subtitles." + NS + "." + mariculture.domain.maritech.Fludd.SOUND,
                "sounds", List.of(NS + ":fludd")));
        write("assets/" + NS + "/sounds.json", Json.write(sounds));
        copyTree(repo.resolve("content-source/sounds"), out.resolve("assets/" + NS + "/sounds"));
        write("data/" + NS + "/tags/worldgen/biome/has_rockhoppers.json", Json.write(map(
                "replace", false, "values", mariculture.domain.worldgen.Rockhopper.BIOMES)));
        String loader = target.forgeTagNamespace ? "forge" : "neoforge";
        write("data/" + NS + "/" + loader + "/biome_modifier/rockhopper_spawns.json", Json.write(map(
                "type", loader + ":add_spawns",
                "biomes", "#" + NS + ":has_rockhoppers",
                "spawners", map("type", NS + ":" + mariculture.domain.worldgen.Rockhopper.ID,
                        "weight", mariculture.domain.worldgen.Rockhopper.SPAWN_WEIGHT,
                        "minCount", mariculture.domain.worldgen.Rockhopper.SPAWN_MIN,
                        "maxCount", mariculture.domain.worldgen.Rockhopper.SPAWN_MAX))));
        lang.put("entity." + NS + ".rockhopper", "Rockhopper");
        lang.put("subtitles." + NS + ".entity.rockhopper.ambient", "Rockhopper squawks");
        lang.put("subtitles." + NS + ".entity.rockhopper.hurt", "Rockhopper hurts");
        lang.put("subtitles." + NS + ".entity.rockhopper.death", "Rockhopper dies");
    }

    // ---- tags --------------------------------------------------------------------------

    private void tags() {
        Map<String, List<String>> blockTags = new LinkedHashMap<>();
        Catalog.blockTags().forEach((k, v) -> blockTags.put(k, new ArrayList<>(v)));
        for (BlockSpec b : Catalog.blocks()) {
            if (b.pickaxe()) blockTags.computeIfAbsent("minecraft:mineable/pickaxe", k -> new ArrayList<>()).add(Ids.full(b.id()));
            if (b.harvestLevel().vanillaTag() != null) {
                blockTags.computeIfAbsent("minecraft:" + b.harvestLevel().vanillaTag(), k -> new ArrayList<>()).add(Ids.full(b.id()));
            }
        }
        blockTags.forEach((tag, entries) -> tagFile(target.blockTags(), tag, entries));
        // Catalog item tags plus the compat entries naming other mods (CompatTags), those written optional.
        Map<String, List<String>> itemTags = new LinkedHashMap<>();
        Catalog.itemTags().forEach((k, v) -> itemTags.put(k, new ArrayList<>(v)));
        Map<String, List<String>> optional = new LinkedHashMap<>();
        for (mariculture.domain.plugin.CompatTags.Entry e : mariculture.domain.plugin.CompatTags.ITEM_TAGS) {
            itemTags.computeIfAbsent(e.tag(), k -> new ArrayList<>()).addAll(e.required());
            optional.computeIfAbsent(e.tag(), k -> new ArrayList<>()).addAll(e.optional());
        }
        itemTags.forEach((tag, entries) -> tagFile(target.itemTags(), tag, entries, optional.getOrDefault(tag, List.of())));
        // Common fluid tags: molten metals as c:molten_<metal> (what Hephaestus, Tinkers and Create addons read),
        // fish oil, and liquid experience as c:experience; each tag holds the source and the flowing form.
        String fluidFolder = target.pluralDataFolders ? "tags/fluids" : "tags/fluid";
        for (FluidSpec f : Fluids.all()) {
            String tag = f.id().startsWith("molten_") || f.id().equals("fish_oil") ? "c:" + f.id()
                    : f.id().equals("xp") ? "c:experience" : null;
            if (tag != null) tagFile(fluidFolder, tag, List.of(Ids.full(f.id()), Ids.full("flowing_" + f.id())));
        }
        for (mariculture.domain.plugin.CompatTags.Entry e : mariculture.domain.plugin.CompatTags.FLUID_TAGS) {
            tagFile(fluidFolder, e.tag(), e.required(), e.optional());
        }
        // Curios (required): jewelry goes in the ring, bracelet and necklace slots, which every player gets.
        List<String> slots = new ArrayList<>();
        for (mariculture.domain.magic.JewelryType t : mariculture.domain.magic.JewelryType.values()) {
            tagFile(target.itemTags(), "curios:" + t.id(), List.of(t.item()));
            slots.add(t.id());
        }
        write("data/" + NS + "/curios/entities/jewelry.json", Json.write(map("entities", List.of("player"), "slots", slots)));
        // Anvil repair materials, one tag per rod or armour material ("mariculture:repairs/<id>"). Entries are optional
        // so a repair item from a later slice (or another mod) can be named before it exists.
        for (ItemSpec i : Catalog.items()) {
            if (i.kind() instanceof ItemKind.Rod rod && rod.repairItem() != null) {
                List<String> all = new java.util.ArrayList<>(List.of(rod.repairItem()));
                all.addAll(Plugins.EXTRA_REPAIRS.getOrDefault(i.id(), List.of()));
                repairTag(i.id(), all);
            }
            String mirror = mariculture.domain.magic.MirrorEnchanting.repairItem(Ids.full(i.id()));
            if (mirror != null) repairTag(i.id(), mirror);
        }
        for (ArmorMaterialSpec m : Catalog.armorMaterials()) repairTag(m.id(), m.repairItem());
    }

    private void repairTag(String id, String item) {
        repairTag(id, item == null ? List.of() : List.of(item));
    }

    private void repairTag(String id, List<String> items) {
        List<Object> values = items.stream().<Object>map(item -> map("id", item, "required", false)).toList();
        write("data/" + NS + "/" + target.itemTags() + "/repairs/" + id + ".json", Json.write(map("replace", false, "values", values)));
    }

    private void tagFile(String folder, String tag, List<String> entries) {
        tagFile(folder, tag, entries, List.of());
    }

    /** A tag file; {@code optional} entries are written {"id", "required": false} so a missing mod's items are skipped. */
    private void tagFile(String folder, String tag, List<String> entries, List<String> optional) {
        String resolved = TagNames.forLoader(tag, target.forgeTagNamespace);
        String ns = resolved.substring(0, resolved.indexOf(':'));
        String path = resolved.substring(resolved.indexOf(':') + 1);
        List<Object> values = new ArrayList<>();
        for (String e : entries) values.add(tagEntry(e));
        for (String e : optional) values.add(map("id", tagEntry(e), "required", false));
        write("data/" + ns + "/" + folder + "/" + path + ".json", Json.write(map("replace", false, "values", values)));
    }

    private String tagEntry(String e) {
        return e.startsWith("#") ? "#" + TagNames.forLoader(e.substring(1), target.forgeTagNamespace) : e;
    }

    // ---- copied assets -----------------------------------------------------------------

    /** Block, item and GUI textures copy as they are; armour layers are placed per edition by {@link #armorTextures}. */
    private void textures() throws IOException {
        try (Stream<Path> dirs = Files.list(repo.resolve("content-source/textures"))) {
            for (Path dir : dirs.filter(Files::isDirectory).sorted().toList()) {
                if (dir.getFileName().toString().equals("armor")) continue;
                copyTree(dir, out.resolve("assets/" + NS + "/textures/" + dir.getFileName()));
            }
        }
    }

    private void dataFiles() throws IOException {
        copyTree(repo.resolve("content-source/data"), out.resolve("data"));
    }

    /**
     * Thaumaturge item aspects (legacy PluginThaumcraft, PLUG-005, plus every item without a crafting recipe, made by
     * tools/thaum_aspects.py). Thaumaturge exists for NeoForge 1.21.1 and 26.1.2 with the same data map; Forge 1.20.1
     * gets nothing. The file carries a mod_loaded condition, so Mariculture loads without it.
     */
    private void thaumaturge() throws IOException {
        if (target != Target.NEOFORGE_26_1_2 && target != Target.NEOFORGE_1_21_1) return;
        Path to = out.resolve("data/thaumaturge/data_maps/item/base_aspects.json");
        Files.createDirectories(to.getParent());
        Files.copy(repo.resolve("content-source/compat/thaumaturge/base_aspects.json"), to);
        // Crucible and infusion recipes, the Aquamancy research tab, entries and scans (tools/thaum_research.py).
        // The recipe formats differ per edition, so each edition has its own tree; every file has a mod_loaded condition.
        copyTree(repo.resolve("content-source/compat/thaumaturge/editions/" + target.dir), out);
        mergeLang(repo.resolve("content-source/compat/thaumaturge/en_us.json"));
        // Worn layers for warded scuba gear, picked in code (ScubaLooks on 26.1.2, ScubaArmorItem on 1.21.1).
        Path armor = repo.resolve("content-source/textures/armor");
        if (target.equipmentAssets) {
            copy(armor.resolve("warded_scuba.png"), "assets/" + NS + "/textures/entity/equipment/humanoid/warded_scuba.png");
            copy(armor.resolve("warded_scuba_legs.png"), "assets/" + NS + "/textures/entity/equipment/humanoid_leggings/warded_scuba.png");
            copy(armor.resolve("warded_swimfin.png"), "assets/" + NS + "/textures/entity/equipment/humanoid/warded_swimfin.png");
        } else {
            copy(armor.resolve("warded_scuba.png"), "assets/" + NS + "/textures/models/armor/warded_scuba_layer_1.png");
            copy(armor.resolve("warded_scuba_legs.png"), "assets/" + NS + "/textures/models/armor/warded_scuba_layer_2.png");
            copy(armor.resolve("warded_swimfin.png"), "assets/" + NS + "/textures/models/armor/warded_swimfin_layer_1.png");
        }
    }

    /**
     * Writes the compendium, leaving out every entry that shows an item, recipe or image this
     * edition does not have yet, so the book never advertises missing content. The omissions
     * are listed in docs/book_coverage.md by {@link BookFilter}.
     */
    private void book() throws IOException {
        Path src = repo.resolve("content-source/book");
        if (!Files.isDirectory(src)) return;
        Path bookJson = src.resolve("book.json");
        if (Files.exists(bookJson)) {
            Path dest = out.resolve("data/" + NS + "/patchouli_books/compendium/book.json");
            Files.createDirectories(dest.getParent());
            Files.copy(bookJson, dest, StandardCopyOption.REPLACE_EXISTING);
            // Legacy handed its guide books out on first login (ItemGuide, see docs/book_migration.csv). Here a hidden
            // advancement that any tick completes runs a function giving the compendium once per player; a datapack
            // can remove or replace either file.
            String give = target.pluralDataFolders
                    ? "give @s patchouli:guide_book{\"patchouli:book\":\"" + Catalog.BOOK_ID + "\"}"
                    : "give @s patchouli:guide_book[patchouli:book=\"" + Catalog.BOOK_ID + "\"]";
            write("data/" + NS + "/" + (target.pluralDataFolders ? "functions" : "function") + "/give_compendium.mcfunction", give + "\n");
            Map<String, Object> giveAdv = map("criteria", map("tick", map("trigger", "minecraft:tick")),
                    "rewards", map("function", NS + ":give_compendium"));
            if (!target.forgeTagNamespace)
                giveAdv.put("neoforge:conditions", List.of(map("type", "neoforge:mod_loaded", "modid", "patchouli")));
            write("data/" + NS + "/" + (target.pluralDataFolders ? "advancements" : "advancement") + "/give_compendium.json",
                    Json.write(giveAdv));
        }
        BookFilter filter = new BookFilter(repo);
        // Tinkers data only ships for Forge 1.20.1 (tinkers()), Hephaestus data for the NeoForge editions (hephaestus()).
        if (target != Target.FORGE_1_20_1) filter.exclude("integrations/tinkers_construct");
        else filter.exclude("integrations/hephaestus");
        // The bound rod is bound in Blood Magic 3 on Forge 1.20.1 (bloodMagic()) and in Neo Vitae on NeoForge (neoVitae()).
        if (target == Target.FORGE_1_20_1) filter.exclude("integrations/thaumaturge");
        if (target != Target.FORGE_1_20_1) filter.exclude("integrations/blood_magic");
        else filter.exclude("integrations/neo_vitae");
        try (Stream<Path> locales = Files.list(src)) {
            for (Path dir : locales.filter(Files::isDirectory).sorted().toList()) {
                Path dest = out.resolve("assets/" + NS + "/patchouli_books/compendium/" + dir.getFileName());
                filter.export(dir, dest);
            }
        }
        if (target == Target.FORGE_1_20_1) filter.writeReport(repo.resolve("docs/book_coverage.md"));
        mergeLang(src.resolve("en_us_lang.json"));
    }

    private void mergeLang(Path file) throws IOException {
        if (!Files.exists(file)) return;
        for (Map.Entry<String, String> e : MiniJson.readStringMap(Files.readString(file, StandardCharsets.UTF_8)).entrySet()) {
            if (lang.containsKey(e.getKey()) && !lang.get(e.getKey()).equals(e.getValue())) {
                throw new IllegalStateException("Conflicting lang key " + e.getKey() + " in " + file);
            }
            lang.put(e.getKey(), e.getValue());
        }
    }

    // ---- io ----------------------------------------------------------------------------

    private void write(String rel, String content) {
        Path p = out.resolve(rel);
        try {
            Files.createDirectories(p.getParent());
            if (Files.exists(p)) throw new IllegalStateException("Two generators wrote " + rel);
            Files.writeString(p, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void copyTree(Path from, Path to) throws IOException {
        if (!Files.isDirectory(from)) return;
        try (Stream<Path> files = Files.walk(from)) {
            for (Path f : files.filter(Files::isRegularFile).toList()) {
                Path dest = to.resolve(from.relativize(f).toString());
                Files.createDirectories(dest.getParent());
                Files.copy(f, dest, StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

    private static void deleteTree(Path dir) throws IOException {
        if (!Files.exists(dir)) return;
        try (Stream<Path> files = Files.walk(dir)) {
            for (Path p : files.sorted(Comparator.reverseOrder()).toList()) Files.delete(p);
        }
    }

    /** Insertion-ordered map literal; values may repeat keys only by mistake, which throws. */
    static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (int i = 0; i < kv.length; i += 2) {
            if (m.put((String) kv[i], kv[i + 1]) != null) throw new IllegalArgumentException("Duplicate key " + kv[i]);
        }
        return m;
    }
}
