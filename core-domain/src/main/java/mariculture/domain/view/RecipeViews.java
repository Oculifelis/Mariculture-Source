package mariculture.domain.view;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import mariculture.domain.fish.FishRegistry;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.fish.Mutation;
import mariculture.domain.fish.Species;
import mariculture.domain.plugin.Plugins;
import mariculture.domain.fishery.Bait;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.fluid.Fluids;
import mariculture.domain.machine.FluidAmount;
import mariculture.domain.magic.Binding;
import mariculture.domain.magic.Jewelry;
import mariculture.domain.magic.JewelryMaterial;
import mariculture.domain.magic.JewelryType;
import mariculture.domain.metal.Anvil;
import mariculture.domain.metal.Casting;
import mariculture.domain.metal.CrucibleFuels;
import mariculture.domain.metal.Ethereal;
import mariculture.domain.metal.Melting;
import mariculture.domain.metal.VatRecipes;

/**
 * Recipe pages for recipe viewers (JEI), built from the same tables the machines use, so the viewer never shows a
 * recipe the machine would not run. Editions draw every category with one generic layout: slots at fixed positions,
 * then lines of translated text under them.
 */
public final class RecipeViews {
    public enum Role { INPUT, OUTPUT, CATALYST }

    /** An item ("#tag" or id) or fluid slot; {@code chanceOneIn} > 1 marks a chance output. */
    public record Slot(Role role, boolean fluid, String id, int amount, int x, int y, int chanceOneIn, java.util.Map<String, Object> data) {
        public Slot(Role role, boolean fluid, String id, int amount, int x, int y, int chanceOneIn) {
            this(role, fluid, id, amount, x, y, chanceOneIn, java.util.Map.of());
        }
    }

    /** A translation key and its arguments. */
    public record Line(String key, Object... args) {}

    /** An item or fluid whose display name the edition fills in. */
    public record Name(boolean fluid, String id) {
        public static Name fluid(String id) { return new Name(true, id); }
        public static Name item(String id) { return new Name(false, id); }
    }

    public record View(String id, List<Slot> slots, List<Line> lines) {}

    /** {@code machines} are the catalyst items of the category. */
    public record Category(String id, String icon, List<String> machines, int width, int height, int textY) {
        public Category(String id, String icon, List<String> machines, int width, int height) {
            this(id, icon, machines, width, height, 24);
        }
    }

    public static final Category MELTING = new Category("crucible_melting", "mariculture:crucible", List.of("mariculture:crucible"), 140, 46);
    public static final Category CRUCIBLE_FUEL = new Category("crucible_fuel", "mariculture:crucible", List.of("mariculture:crucible"), 120, 46);
    public static final Category VAT = new Category("vat", "mariculture:vat", List.of("mariculture:vat"), 140, 46);
    public static final Category CASTING = new Category("casting", "mariculture:ingot_caster",
            List.of("mariculture:nugget_caster", "mariculture:ingot_caster", "mariculture:block_caster"), 120, 36);
    public static final Category ANVIL = new Category("anvil", "mariculture:anvil", List.of("mariculture:anvil", "mariculture:autohammer"), 120, 36);
    public static final Category SIFTER = new Category("sifter", "mariculture:sifter", List.of("mariculture:sifter"), 120, 36);
    /** Shaped jewelry (the legacy NEI jewelry handler, plugins/nei/NEIJewelryShapedHandler.java), finished on the anvil. */
    public static final Category JEWELRY = new Category("jewelry", "mariculture:ring", List.of("mariculture:anvil"), 150, 88, 58);
    /** Fishing (the legacy NEI handlers had none): loot by rod, bait by rod, where each species bites. */
    public static final Category FISHING_LOOT = new Category("fishing_loot", "mariculture:titanium_fishing_rod", rodItems(), 140, 56);
    public static final Category FISHING_BAIT = new Category("fishing_bait", "mariculture:bait_worm", rodItems(), 140, 36);
    public static final Category FISH_CATCH = new Category("fish_catch", "mariculture:reed_fishing_rod", rodItems(), 150, 56);
    /** Fish breeding and fish products (plugins/nei/NEIFishBreedingMutationHandler.java, NEIFishProductHandler.java). */
    public static final Category FISH_BREEDING = new Category("fish_breeding", "mariculture:fish_eggs", List.of("mariculture:fish_feeder"), 140, 36);
    public static final Category FISH_PRODUCTS = new Category("fish_products", "mariculture:fish_feeder", List.of("mariculture:fish_feeder"), 140, 36);
    public static final List<Category> CATEGORIES = List.of(MELTING, CRUCIBLE_FUEL, VAT, CASTING, ANVIL, SIFTER, JEWELRY,
            FISHING_LOOT, FISHING_BAIT, FISH_CATCH, FISH_BREEDING, FISH_PRODUCTS);

    /** The item of each rod tier; vanilla's rod is DIRE, Redstone Arsenal's flux-infused rod INFUSED. */
    public static String rodItem(Species.Rod rod) {
        return switch (rod) {
            case OLD -> "mariculture:reed_fishing_rod";
            case GOOD -> "mariculture:wooden_fishing_rod";
            case DIRE -> "minecraft:fishing_rod";
            case SUPER -> "mariculture:titanium_fishing_rod";
            case FLUX -> "mariculture:flux_rod";
            case MANA -> "mariculture:livingwood_fishing_rod";
            case BLOOD -> "mariculture:bound_fishing_rod";
            case INFUSED -> "redstone_arsenal:flux_fishing_rod";
        };
    }

    private static List<String> rodItems() {
        List<String> out = new ArrayList<>();
        for (Species.Rod r : Species.Rod.values()) out.add(rodItem(r));
        return out;
    }

    private static final int IN_X = 10, OUT_X = 80, Y = 4;

    private RecipeViews() {}

    public static List<View> views(Category c, FishRegistry fish) {
        if (c == MELTING) return melting(fish);
        if (c == CRUCIBLE_FUEL) return fuels();
        if (c == VAT) return vat();
        if (c == CASTING) return casting();
        if (c == ANVIL) return anvil();
        if (c == SIFTER) return sifter();
        if (c == JEWELRY) return jewelry();
        if (c == FISHING_LOOT) return fishingLoot();
        if (c == FISHING_BAIT) return fishingBait();
        if (c == FISH_CATCH) return fishCatch(fish);
        if (c == FISH_BREEDING) return fishBreeding(fish);
        if (c == FISH_PRODUCTS) return fishProducts(fish);
        return List.of();
    }

    static List<View> melting(FishRegistry fish) {
        List<View> out = new ArrayList<>();
        int n = 0;
        for (Melting.Recipe r : Melting.table()) {
            List<Slot> slots = new ArrayList<>();
            slots.add(new Slot(Role.INPUT, false, r.input(), 1, IN_X, Y, 0));
            if (r.fluid() != null) {
                slots.add(new Slot(Role.OUTPUT, true, r.fluid(), r.amount(), OUT_X, Y, 0));
            } else {
                for (int i = 0; i < r.randomFluids().size(); i++) {
                    slots.add(new Slot(Role.OUTPUT, true, r.randomFluids().get(i), r.amount(), OUT_X + 18 * (i % 2), Y + 18 * (i / 2), 0));
                    if (i == 3) break;
                }
            }
            if (r.bonus() != null) slots.add(new Slot(Role.OUTPUT, false, r.bonus(), 1, OUT_X + 18 * 2, Y, Math.max(1, r.chance())));
            out.add(new View("melting/" + n++, slots, List.of(new Line("jei.mariculture.temperature", r.temp()))));
        }
        // Ethereal upgrade: any enchanted item, 100 mB per enchantment value (metal/Ethereal).
        out.add(new View("melting/ethereal", List.of(new Slot(Role.CATALYST, false, "mariculture:ethereal_upgrade", 1, IN_X, Y, 0),
                new Slot(Role.OUTPUT, true, Fluids.XP, Ethereal.XP_PER_VALUE, OUT_X, Y, 0)),
                List.of(new Line("jei.mariculture.temperature", Ethereal.MELTING_POINT), new Line("jei.mariculture.ethereal"))));
        if (fish != null) {
            for (Species sp : fish.allSpecies()) {
                Melting.Recipe r = Melting.fishOilRecipe(FishingRules.rawForm(sp.id()), fish);
                if (r == null) continue;
                List<Slot> slots = new ArrayList<>();
                slots.add(new Slot(Role.INPUT, false, r.input(), 1, IN_X, Y, 0));
                slots.add(new Slot(Role.OUTPUT, true, r.fluid(), r.amount(), OUT_X, Y, 0));
                if (r.bonus() != null) slots.add(new Slot(Role.OUTPUT, false, r.bonus(), 1, OUT_X + 36, Y, Math.max(1, r.chance())));
                out.add(new View("melting/fish/" + sp.id().replace(':', '_'), slots, List.of(new Line("jei.mariculture.temperature", r.temp()))));
            }
        }
        return out;
    }

    static List<View> fuels() {
        List<View> out = new ArrayList<>();
        for (Map.Entry<String, CrucibleFuels.Fuel> e : CrucibleFuels.items().entrySet()) {
            out.add(fuel("fuel/item/" + out.size(), new Slot(Role.INPUT, false, e.getKey(), 1, IN_X, Y, 0), e.getValue()));
        }
        for (Map.Entry<String, CrucibleFuels.Fuel> e : CrucibleFuels.fluids().entrySet()) {
            String path = e.getKey();
            for (String ns : new String[] {"mariculture:", "minecraft:"}) {
                out.add(fuel("fuel/fluid/" + out.size(), new Slot(Role.INPUT, true, ns + path, 10, IN_X, Y, 0), e.getValue()));
            }
        }
        return out;
    }

    private static View fuel(String id, Slot in, CrucibleFuels.Fuel f) {
        List<Line> lines = new ArrayList<>();
        lines.add(new Line("jei.mariculture.fuel.max_temperature", f.maxTemp()));
        if (f.explosive()) lines.add(new Line("jei.mariculture.fuel.explosive"));
        return new View(id, List.of(in), lines);
    }

    static List<View> vat() {
        List<View> out = new ArrayList<>();
        for (VatRecipes.Recipe r : VatRecipes.all()) {
            List<Slot> slots = new ArrayList<>();
            int x = IN_X;
            for (FluidAmount f : new FluidAmount[] {r.fluid1(), r.fluid2()}) {
                if (f == null) continue;
                slots.add(new Slot(Role.INPUT, true, f.fluid(), f.amount(), x, Y, 0));
                x += 18;
            }
            if (r.item() != null) slots.add(new Slot(Role.INPUT, false, r.item(), r.itemCount(), x, Y, 0));
            int ox = OUT_X + 20;
            if (r.outFluid() != null) {
                slots.add(new Slot(Role.OUTPUT, true, r.outFluid().fluid(), r.outFluid().amount(), ox, Y, 0));
                ox += 18;
            }
            if (r.outItem() != null && !r.outItem().isEmpty()) slots.add(new Slot(Role.OUTPUT, false, r.outItem().item(), r.outItem().count(), ox, Y, 0));
            out.add(new View("vat/" + r.id(), slots, List.of(new Line("jei.mariculture.time", r.time()))));
        }
        return out;
    }

    static List<View> casting() {
        List<View> out = new ArrayList<>();
        for (Casting.Kind kind : Casting.Kind.values()) {
            for (String fluid : castable()) {
                Casting.Result r = Casting.recipeFor(kind, fluid);
                if (r == null) continue;
                List<Slot> slots = List.of(new Slot(Role.INPUT, true, fluid, r.amount(), IN_X, Y, 0),
                        new Slot(Role.CATALYST, false, CASTING.machines().get(kind.ordinal()), 1, IN_X + 30, Y, 0),
                        new Slot(Role.OUTPUT, false, r.item(), 1, OUT_X, Y, 0));
                out.add(new View("casting/" + kind.name().toLowerCase(java.util.Locale.ROOT) + "/" + fluid.replace(':', '_'), slots, List.of()));
            }
        }
        return out;
    }

    private static List<String> castable() {
        List<String> fluids = new ArrayList<>(List.of(Fluids.WATER, "mariculture:molten_glass"));
        for (String[] m : Fluids.METALS) fluids.add(Fluids.molten(m[0]));
        return fluids;
    }

    static List<View> anvil() {
        List<View> out = new ArrayList<>();
        for (Anvil.Recipe r : Anvil.recipes()) {
            List<Slot> slots = List.of(new Slot(Role.INPUT, false, r.input(), 1, IN_X, Y, 0),
                    new Slot(Role.CATALYST, false, "mariculture:hammer", 1, IN_X + 30, Y, 0),
                    new Slot(Role.OUTPUT, false, r.output(), r.count(), OUT_X, Y, 0));
            out.add(new View("anvil/" + r.input().replace(':', '_'), slots, List.of(new Line("jei.mariculture.hits", r.hits()))));
        }
        return out;
    }

    static List<View> sifter() {
        List<View> out = new ArrayList<>();
        int n = 0;
        for (Bait.SifterRecipe r : Bait.sifterAll()) {
            List<Slot> slots = List.of(new Slot(Role.INPUT, false, r.input(), 1, IN_X, Y, 0),
                    new Slot(Role.OUTPUT, false, r.bait(), r.max(), OUT_X, Y, 0));
            out.add(new View("sifter/" + n++, slots, List.of(new Line("jei.mariculture.sifter.chance", r.min(), r.max(), r.chance()))));
        }
        return out;
    }

    /** One page per type, binding and pearl: the grid (A binding item, B pearl), the anvil hits and the piece's stats. */
    static List<View> jewelry() {
        List<View> out = new ArrayList<>();
        for (JewelryType type : JewelryType.values()) {
            String[] rows = Jewelry.pattern(type);
            for (Binding b : Binding.values()) {
                for (JewelryMaterial m : JewelryMaterial.values()) {
                    List<Slot> slots = new ArrayList<>();
                    for (int i = 0; i < 9; i++) {
                        char c = rows[i / 3].charAt(i % 3);
                        if (c == ' ') continue;
                        slots.add(new Slot(Role.INPUT, false, c == 'A' ? b.item(type) : m.item(), 1, (i % 3) * 18, (i / 3) * 18, 0));
                    }
                    slots.add(new Slot(Role.CATALYST, false, "mariculture:anvil", 1, 70, 18, 0));
                    slots.add(new Slot(Role.OUTPUT, false, type.item(), 1, 110, 18, 0, Jewelry.data(b, m)));
                    out.add(new View("jewelry/" + type.id() + "/" + b.id() + "/" + m.id(), slots, List.of(
                            new Line("jei.mariculture.hits", Jewelry.hits(type, b, m)),
                            new Line("jei.mariculture.jewelry.stats", Jewelry.durability(type, b, m), Jewelry.maxEnchantments(type, m),
                                    Jewelry.maxLevel(type, b, m)))));
                }
            }
        }
        return out;
    }

    // ---- fishing -----------------------------------------------------------------------

    /** The weakest rod in {@code rods} by catch quality. */
    private static Species.Rod weakest(Set<Species.Rod> rods) {
        Species.Rod best = null;
        for (Species.Rod r : rods) if (best == null || FishingRules.stats(r).quality() < FishingRules.stats(best).quality()) best = r;
        return best;
    }

    private static Line rodLine(Set<Species.Rod> rods) {
        if (rods.size() == Species.Rod.values().length) return new Line("jei.mariculture.fishing.any_rod");
        if (rods.size() == 1) return new Line("jei.mariculture.fishing.only_rod");
        return new Line("jei.mariculture.fishing.rod_or_better");
    }

    /** One page per loot entry: the weakest rod that lands it, the item, its pool and chance, and where. */
    static List<View> fishingLoot() {
        Map<FishingRules.Loot, Set<Species.Rod>> rods = new LinkedHashMap<>();
        for (Species.Rod rod : Species.Rod.values()) {
            for (FishingRules.Loot l : FishingRules.lootFor(rod)) rods.computeIfAbsent(l, k -> EnumSet.noneOf(Species.Rod.class)).add(rod);
        }
        List<View> out = new ArrayList<>();
        int n = 0;
        for (Map.Entry<FishingRules.Loot, Set<Species.Rod>> e : rods.entrySet()) {
            FishingRules.Loot l = e.getKey();
            List<Slot> slots = List.of(new Slot(Role.CATALYST, false, rodItem(weakest(e.getValue())), 1, IN_X, Y, 0),
                    new Slot(Role.OUTPUT, false, l.item(), 1, OUT_X, Y, 0));
            String rarity = "jei.mariculture.fishing.rarity." + l.rarity().name().toLowerCase(Locale.ROOT);
            out.add(new View("fishing_loot/" + n++, slots, List.of(new Line("jei.mariculture.fishing.loot", new Line(rarity), l.chancePercent()),
                    rodLine(e.getValue()), new Line("jei.mariculture.fishing.dimension." + (l.dimension() == null ? "any" : l.dimension())))));
        }
        return out;
    }

    /** One page per bait and rod that takes it, with the bait's quality. */
    static List<View> fishingBait() {
        Set<String> items = new TreeSet<>(Bait.QUALITY.keySet());
        for (Plugins.PluginBait b : Plugins.BAIT) items.add(b.item());
        List<View> out = new ArrayList<>();
        for (String item : items) {
            for (Species.Rod rod : Species.Rod.values()) {
                if (!Bait.canUse(rod, item)) continue;
                List<Slot> slots = List.of(new Slot(Role.INPUT, false, item, 1, IN_X, Y, 0), new Slot(Role.CATALYST, false, rodItem(rod), 1, OUT_X, Y, 0));
                out.add(new View("fishing_bait/" + item.replace(':', '_') + "/" + rod.name().toLowerCase(Locale.ROOT), slots,
                        List.of(new Line("jei.mariculture.fishing.bait_quality", Bait.quality(item)))));
            }
        }
        return out;
    }

    private static List<Species> sorted(FishRegistry fish) {
        List<Species> all = new ArrayList<>(fish.allSpecies());
        all.sort((a, b) -> a.id().compareTo(b.id()));
        return all;
    }

    /** One page per species: the weakest rod that lands it, its raw and live forms, and where it bites. */
    static List<View> fishCatch(FishRegistry fish) {
        List<View> out = new ArrayList<>();
        if (fish == null) return out;
        for (Species sp : sorted(fish)) {
            List<Slot> slots = List.of(new Slot(Role.CATALYST, false, rodItem(sp.rod()), 1, IN_X, Y, 0),
                    new Slot(Role.OUTPUT, false, FishingRules.rawForm(sp.id()), 1, OUT_X, Y, 0),
                    new Slot(Role.OUTPUT, false, LiveFish.itemFor(sp.id()), 1, OUT_X + 18, Y, 0));
            String salinity = "jei.mariculture.salinity." + sp.salinityBase().name().toLowerCase(Locale.ROOT);
            List<Line> lines = new ArrayList<>();
            lines.add(new Line("jei.mariculture.fishing.catch_dimension." + sp.catchDimension()));
            // A5: the first named biome (biome.<ns>.<path>, the game's own biome name) and a fluid tag's own line.
            sp.catchBiomes().stream().filter(b -> !b.startsWith("#")).findFirst()
                    .ifPresent(b -> lines.add(new Line("jei.mariculture.fishing.catch_biome", new Line("biome." + b.replace(':', '.')))));
            if (sp.catchFluid() != null) {
                String f = sp.catchFluid().startsWith("#") ? sp.catchFluid().substring(1) : sp.catchFluid();
                lines.add(new Line("jei.mariculture.fishing.catch_fluid." + f.substring(Math.max(f.indexOf(':'), f.lastIndexOf('/')) + 1)));
            }
            lines.add(new Line("jei.mariculture.fishing.temperature", sp.temperatureBase() - sp.temperatureTolerance(), sp.temperatureBase() + sp.temperatureTolerance()));
            lines.add(sp.ignoresSalinity() ? new Line("jei.mariculture.fishing.any_salinity")
                    : new Line("jei.mariculture.fishing.salinity", new Line(salinity), sp.salinityTolerance()));
            out.add(new View("fish_catch/" + sp.id().replace(':', '_'), slots, List.copyOf(lines)));
        }
        return out;
    }

    /** One page per mutation: the two parents and the child, at the mutation's chance. */
    static List<View> fishBreeding(FishRegistry fish) {
        List<View> out = new ArrayList<>();
        if (fish == null) return out;
        for (Mutation m : fish.mutations()) {
            List<Slot> slots = List.of(new Slot(Role.INPUT, false, LiveFish.itemFor(m.parentA()), 1, IN_X, Y, 0),
                    new Slot(Role.INPUT, false, LiveFish.itemFor(m.parentB()), 1, IN_X + 18, Y, 0),
                    new Slot(Role.OUTPUT, false, LiveFish.itemFor(m.child()), 1, OUT_X, Y, 0));
            out.add(new View("fish_breeding/" + m.id().replace(':', '_'), slots, List.of(new Line("jei.mariculture.fishing.mutation", m.chancePercent()))));
        }
        return out;
    }

    /** One page per species product: the live fish in a feeder and what it makes, with its chance per cycle. */
    static List<View> fishProducts(FishRegistry fish) {
        List<View> out = new ArrayList<>();
        if (fish == null) return out;
        for (Species sp : sorted(fish)) {
            int n = 0;
            for (Species.Product p : sp.products()) {
                List<Slot> slots = List.of(new Slot(Role.INPUT, false, LiveFish.itemFor(sp.id()), 1, IN_X, Y, 0),
                        new Slot(Role.OUTPUT, false, p.item(), p.count(), OUT_X, Y, 0));
                out.add(new View("fish_products/" + sp.id().replace(':', '_') + "/" + n++, slots,
                        List.of(new Line("jei.mariculture.fishing.product", p.chancePercent()))));
            }
        }
        return out;
    }
}
