package mariculture.domain.config;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * The server config (legacy core/config/GeneralStuff.java:22-37 and core/config/Machines.java:12-75, 1.3.0). Each
 * edition builds its loader's config spec from {@link #ALL} and pushes the loaded values back with {@code set}; rules
 * read {@code get()}, which returns the legacy default until a config has loaded (unit tests, data generation).
 *
 * <p>Only options with a port counterpart are listed. Dropped: debug mode, spawn books (Enchiridion), NEI casting
 * (JEI shows casting always), packet distance and tank update amounts (the loaders sync block entities), and the
 * "Molten Metal Nugget mB Value": recipes are generated data at 16 mB per nugget, so changing it at runtime would
 * desynchronise every melting and casting recipe (see docs/feature_matrix.csv CORE-089). The crucible purity bonus
 * (Machines.java:61-65) is not listed: legacy read it only in TileCrucible.getFluidAmount (TileCrucible.java:347-353), which
 * nothing called, and the fluid eject tick (Machines.java:29) was never read either.
 */
public final class Settings {
    public sealed interface Setting permits Bool, Int, Num, Strs {
        String section();

        String key();

        String comment();
    }

    public static final class Bool implements Setting {
        private final String section, key, comment;
        private final boolean def;
        private volatile boolean value;

        Bool(String section, String key, boolean def, String comment) {
            this.section = section; this.key = key; this.def = def; this.comment = comment; this.value = def;
        }

        public boolean get() { return value; }
        public boolean def() { return def; }
        public void set(boolean v) { value = v; }
        public String section() { return section; }
        public String key() { return key; }
        public String comment() { return comment; }
    }

    public static final class Int implements Setting {
        private final String section, key, comment;
        private final int def, min, max;
        private volatile int value;

        Int(String section, String key, int def, int min, int max, String comment) {
            this.section = section; this.key = key; this.def = def; this.min = min; this.max = max; this.comment = comment; this.value = def;
        }

        public int get() { return value; }
        public int def() { return def; }
        public int min() { return min; }
        public int max() { return max; }
        public void set(int v) { value = Math.max(min, Math.min(max, v)); }
        public String section() { return section; }
        public String key() { return key; }
        public String comment() { return comment; }
    }

    /** A probability in [0, 1]; {@link #probability()} validates it like every other chance (BUG-001). */
    public static final class Num implements Setting {
        private final String section, key, comment;
        private final double def;
        private volatile Probability value;

        Num(String section, String key, double def, String comment) {
            this.section = section; this.key = key; this.def = def; this.comment = comment;
            this.value = Probability.of(section + "." + key, def);
        }

        public Probability probability() { return value; }
        public double def() { return def; }
        public void set(double v) { value = Probability.of(section + "." + key, v); }
        public String section() { return section; }
        public String key() { return key; }
        public String comment() { return comment; }
    }

    /** A list of strings. */
    public static final class Strs implements Setting {
        private final String section, key, comment;
        private final List<String> def;
        private volatile List<String> value;

        Strs(String section, String key, List<String> def, String comment) {
            this.section = section; this.key = key; this.def = List.copyOf(def); this.comment = comment; this.value = this.def;
        }

        public List<String> get() { return value; }
        public List<String> def() { return def; }
        public void set(List<String> v) { value = List.copyOf(v); }
        public String section() { return section; }
        public String key() { return key; }
        public String comment() { return comment; }
    }

    private static final List<Setting> LIST = new ArrayList<>();
    public static final List<Setting> ALL = Collections.unmodifiableList(LIST);

    // GeneralStuff.java:34-36 (beta 1.3.0 values)
    public static final Num ZOMBIE_GEAR_CHANCE = num("general", "zombie_fishing_gear_chance", 0.0125,
            "Chance (0-1) for a naturally spawned zombie to carry a fishing rod and bucket hat.");
    public static final Num ZOMBIE_HAT_DROP = num("general", "zombie_hat_drop_chance", 0.1,
            "Chance (0-1) that such a zombie drops its bucket hat.");
    public static final Num ZOMBIE_ROD_DROP = num("general", "zombie_rod_drop_chance", 0.05,
            "Chance (0-1) that such a zombie drops its fishing rod.");
    // GeneralStuff.java:31-33
    public static final Bool DRAGON_SPAWN_EGG = bool("general", "dragon_spawn_egg_enabled", true,
            "Whether the Dragon Spawn Egg can summon an Ender Dragon.");
    public static final Bool GAS_CATCHES_FIRE = bool("general", "natural_gas_catches_fire", true,
            "Whether natural gas next to fire, torches or lava catches fire.");
    // GeneralStuff.java:28 (used by the diving module)
    public static final Int HARDCORE_DIVING = integer("general", "hardcore_diving", 0, 0, 100,
            "Makes your air run out faster underwater; higher is faster, 0 turns it off.");
    // Fish.java:151 "Prevent Mob Spawns"; FishStargazer.java:94-118 (disabled in 1.3.0 for its unbounded memory, BUG-004)
    /** FishMechanics.java:37 ("Give me fish NAOW", 50): taken off the wait for a bite every tick (EntityHook.java:254). */
    public static final Int FISHING_SPEED = integer("fishery", "fishing_speed_multiplier", 50, 0, 1000,
            "Taken off the wait for a fish every tick while a Mariculture rod's hook is in water or lava; 0 is vanilla speed. Data packs can override it (mariculture/fishing_speed).");
    public static final Bool STARGAZER_BLOCKS_SPAWNS = bool("fishery", "stargazer_prevents_spawns", true,
            "Whether a fish feeder with a living Stargazer stops hostile mobs spawning within 16 blocks (8 up and down) of its tank.");

    // Machines.java:14-18 (the sawmill joins with the factory module)
    public static final Int SAWMILL_SPEED = integer("machines", "sawmill_speed", 650, 100, 100000,
            "Work per sawmill batch, in hundredths (650 is 6 ticks).");
    // Machines.java:69 "Sawmill > Output Multiplier"
    public static final Int SAWMILL_MULTIPLIER = integer("machines", "sawmill_output_multiplier", 2, 1, 64,
            "Multiplies how many blocks the sawmill makes from one set of six.");
    public static final Int FEEDER_SPEED = integer("machines", "fish_feeder_speed", 200, 1, 100000,
            "Life a fish loses per feeder cycle (higher ages fish faster).");
    public static final Int NET_SPEED = integer("machines", "fishing_net_speed", 300, 1, 100000,
            "A fishing net catches on one random tick in this many.");
    public static final Int CRUCIBLE_SPEED = integer("machines", "crucible_furnace_speed", 40000, 1, 10000000,
            "Work the crucible furnace needs per melted item.");
    public static final Int HATCHERY_SPEED = integer("machines", "hatchery_speed", 100, 1, 100000,
            "Work a hatchery needs per egg.");
    // maritech ExtensionMachines.java:22-24
    public static final Int INCUBATOR_SPEED = integer("machines", "incubator_speed", 400, 1, 100000,
            "Work an incubator needs per batch of hatching.");
    public static final Int DNA_SPEED = integer("machines", "dna_machine_speed", 1000, 1, 100000,
            "Work the DNA extractor needs per try (the DNA injector needs five times this).");
    /** ExtensionGeneralStuff "Speedboat Vertical Modifier" (2.0), as a percentage. */
    public static final Int SPEEDBOAT_VERTICAL_PERCENT = integer("general", "speedboat_vertical_percent", 200, 0, 1000,
            "How fast a speed boat at least 90% under water rises, in percent of the normal 0.007 a tick.");
    public static final Int AUTOFISHER_SPEED = integer("machines", "autofisher_speed", 2500, 1, 100000,
            "Work an autofisher needs per cast.");
    // Machines.java:28
    public static final Int ITEM_EJECT_TICK = integer("machines", "item_eject_ticks", 20, 1, 1200,
            "Ticks between a machine pushing items out.");
    // Machines.java:62
    public static final Bool CRUCIBLE_DUSTS = bool("machines", "crucible_bonus_dusts", true,
            "Whether melting ores in the crucible furnace can give bonus dusts.");

    // core/config/Enchantments.java:57-70 (jewelry). Offline mode keyed jewelry by name in world data and has no
    // counterpart now that it lives with the player; the factors stay fixed (JewelryEffects) as they are not chances.
    public static final Bool JEWELRY_DROPS = bool("jewelry", "drops_on_death", false,
            "Whether jewelry in Curios slots drops when its wearer dies (unless keepInventory or Reaper keeps it).");
    public static final Bool JEWELRY_EXTRA_SLOTS_STACK = bool("jewelry", "extra_slots_stack", false,
            "Whether two pieces of the same jewelry type (from extra Curios slots) add their enchantment levels. Off: only the strongest ring, bracelet and necklace count.");
    public static final Int JUMPS_PER_WEAR = integer("jewelry", "leapfrog_jumps_per_damage", 10, 1, 100000,
            "Jumps per point of wear on Leapfrog jewelry.");
    public static final Int SPEED_TICKS_PER_WEAR = integer("jewelry", "sonic_ticks_per_damage", 1200, 1, 1000000,
            "Ticks of sprinting per point of wear on Sonic the Hedgehog jewelry.");
    public static final Int REPAIR_TICKS = integer("jewelry", "restoration_ticks_between_repairs", 100, 1, 1000000,
            "Ticks between Restoration repairs.");
    public static final Int BLINK_COOLDOWN_MS = integer("jewelry", "blink_milliseconds_between_blinks", 0, 0, 600000,
            "Milliseconds a player waits between blinks.");
    public static final Int BLINK_RAY_TRACE = integer("jewelry", "blink_ray_trace_distance", 2000, 1, 2000,
            "The furthest a blink reaches, in blocks (Blink also allows 16 per level).");

    // core/config/AutoDictionary.java:9-25. Ore dictionary prefixes become groups of material tags (c:ingots/copper is in
    // group "ingots"); the blacklisted dye items are the legacy dyes that were really materials.
    public static final Strs DICTIONARY_GROUPS = strs("factory", "dictionary_tag_groups",
            List.of("ores", "ingots", "storage_blocks", "nuggets", "dusts", "gems", "raw_materials", "dyes"),
            "Groups of material tags (c:<group>/<material>, forge: on 1.20.1) whose items the Automatic Dictionary Converter treats as the same.");
    public static final Strs DICTIONARY_BLACKLIST = strs("factory", "dictionary_blacklist",
            List.of("minecraft:lapis_lazuli", "minecraft:ink_sac", "minecraft:cocoa_beans", "minecraft:bone_meal", "minecraft:green_dye"),
            "Items the Automatic Dictionary Converter never converts from or to.");
    // factory/Factory.java:107-110 (FluidDicHandler.register); xp mods that exist on these editions replace the 1.7 ones.
    public static final Strs FLUID_DICTIONARY = strs("factory", "fluid_dictionary",
            List.of("water=minecraft:water=2000", "xp=mariculture:xp=200", "xp=enderio:xp_juice=200", "xp=mob_grinding_utils:fluid_xp=200",
                    "xp=industrialforegoing:essence=200", "xp=cofh_core:experience=200",
                    // plugins/PluginThermalFoundation.java:23 (Thermal's ender at the droplet's 25)
                    "ender=mariculture:liquid_ender=25", "ender=thermal:ender=25"),
            "Fluids the Fluid Auto-Dictionary converts between: family=fluid=ratio. Fluids of one family convert by their ratios; missing fluids are skipped.");

    // core/config/Modules.java:28-35: one switch per plugin; a plugin also needs its mod installed.
    public static final Bool PLUGIN_AQUACULTURE = bool("plugins", "aquaculture", true,
            "Aquaculture 2: its worms are bait, Mariculture fish fillet into its fillets with salt, its fish melt into fish oil.");
    public static final Bool PLUGIN_BIOMES_O_PLENTY = bool("plugins", "biomesoplenty", true,
            "Biomes O' Plenty: salinity and temperature for its biomes, its flowers crush into two dye on the anvil.");
    public static final Bool PLUGIN_RAILCRAFT = bool("plugins", "railcraft", true,
            "Railcraft Reborn: its steel tools and armour melt in the crucible.");
    public static final Bool PLUGIN_THERMAL = bool("plugins", "thermal", true,
            "Thermal Series: redstone and glowstone melt into Thermal's fluids in the crucible.");
    public static final Bool PLUGIN_TCONSTRUCT = bool("plugins", "tconstruct", true,
            "Tinkers' Construct: natural gas fuels the smeltery; titanium, rutile and magnesium melt, cast and alloy in it.");
    // core/config/Modules.java:37 "Experimental TConstruct Recipes", on by default.
    public static final Bool PLUGIN_TCONSTRUCT_RECIPE_IMPORT = bool("plugins", "tconstruct_recipe_import", true,
            "Tinkers' Construct: every smeltery melting and every ingot, nugget and block casting also works in the crucible "
                    + "and the casters (Mariculture's own recipes come first).");
    public static final Bool PLUGIN_HEPHAESTUS = bool("plugins", "hephaestus", true,
            "Hephaestus (Tinkers' successor on NeoForge): titanium, rutile and magnesium melt, cast and alloy in its smeltery, "
                    + "and titanium makes Hephaestus tool parts.");
    public static final Bool PLUGIN_HEPHAESTUS_RECIPE_IMPORT = bool("plugins", "hephaestus_recipe_import", true,
            "Hephaestus: every smeltery melting and every ingot, nugget and block casting also works in the crucible "
                    + "and the casters (Mariculture's own recipes come first).");
    public static final Bool PLUGIN_FORESTRY = bool("plugins", "forestry", true,
            "Forestry: its bioethanol works as Mariculture's, fish squeeze into fish oil, and its bees craft into bee bait.");
    public static final Bool PLUGIN_COMPLICATED_BEES = bool("plugins", "complicated_bees", true,
            "Complicated Bees: its drones, princesses and queens craft into bee bait, as Forestry's do.");
    public static final Bool PLUGIN_BOTANIA = bool("plugins", "botania", true,
            "Botania: the living rod, petal bait, the mana fish, Botania loot on the living rod, and mana pearls from oysters.");
    public static final Bool PLUGIN_BLOOD_MAGIC = bool("plugins", "bloodmagic", true,
            "Blood Magic: the bound fishing rod (bound from a titanium rod), its bait, loot and blood-shard mass fishing.");
    public static final Bool PLUGIN_NEO_VITAE = bool("plugins", "neovitae", true,
            "Neo Vitae (Blood Magic's successor on NeoForge): binding the bound fishing rod, its bait, loot and blood-shard mass fishing.");
    public static final Bool PLUGIN_REDSTONE_ARSENAL = bool("plugins", "redstone_arsenal", true,
            "Redstone Arsenal: its flux-infused fishing rod catches Mariculture fish and loot, and flux dust is bait.");
    // Compat study 2026-11-15 (PLUG-027 onward): fishing mods new to the port.
    public static final Bool PLUGIN_UPGRADE_AQUATIC = bool("plugins", "upgrade_aquatic", true,
            "Upgrade Aquatic: its kelps and corals work in Mariculture recipes.");
    public static final Bool PLUGIN_ALEXS_CAVES = bool("plugins", "alexscaves", true,
            "Alex's Caves: its pearls count as pearls, and the Abyssal Chasm is cold salt water.");
    public static final Bool PLUGIN_STARCATCHER = bool("plugins", "starcatcher", true,
            "Starcatcher: its rods catch Mariculture fish and Mariculture fish join its catches and guide.");
    public static final Bool PLUGIN_STARDEW_FISHING = bool("plugins", "stardew_fishing", true,
            "Stardew Fishing: its minigame plays for the fish Mariculture rods really catch.");
    public static final Bool PLUGIN_TIDE = bool("plugins", "tide", true,
            "Tide: Mariculture fish join its catches and journal; Tide keeps the vanilla rod.");
    public static final Bool PLUGIN_THAUMATURGE = bool("plugins", "thaumaturge", true,
            "Thaumaturge: the Aquamancy research tab, six magic fish, molten thaumium, molten void and Brine of Order.");
    // core/config/FishMechanics.java:32: fish one weak blood shard lets an activated bound rod catch.
    public static final Int BOUND_ROD_WEAK_SHARD_FISH = integer("plugins", "bound_rod_weak_shard_fish", 8, 1, 1000,
            "Fish an activated bound fishing rod catches for one weak blood shard.");

    private Settings() {}

    private static Strs strs(String section, String key, List<String> def, String comment) {
        return add(new Strs(section, key, def, comment));
    }

    private static Bool bool(String section, String key, boolean def, String comment) {
        return add(new Bool(section, key, def, comment));
    }

    private static Int integer(String section, String key, int def, int min, int max, String comment) {
        return add(new Int(section, key, def, min, max, comment));
    }

    private static Num num(String section, String key, double def, String comment) {
        return add(new Num(section, key, def, comment));
    }

    private static <T extends Setting> T add(T s) {
        LIST.add(s);
        return s;
    }

    /** Puts every setting back to its default (tests). */
    public static void reset() {
        for (Setting s : LIST) {
            if (s instanceof Bool b) b.set(b.def());
            else if (s instanceof Int i) i.set(i.def());
            else if (s instanceof Num n) n.set(n.def());
            else if (s instanceof Strs l) l.set(l.def());
        }
    }
}
