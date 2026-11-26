package mariculture.domain.fish;

import java.util.List;

/**
 * The species shipped with Mariculture, for item registration (items exist before data packs load).
 * Names, legacy texture names (FishSpecies.getSimpleName) and raw-fish food values come from the
 * shipped species files; CatalogTest checks this table against content-source/data. Species added by
 * other data packs breed and work in machines but have no item of their own (they use live_fish).
 */
public final class ShippedSpecies {
    /**
     * @param texture legacy texture name under textures/items/fish/ (anglerfish has _male/_female variants)
     * @param hunger  raw fish hunger, or -1 when inedible (metal fish)
     * @param fishMeal fish meal from one raw fish (FishSpecies.getFishMealSize), also sizing sushi and miso recipes
     */
    public record Entry(String id, String englishName, String texture, boolean gendered, int hunger, float saturation, int fishMeal) {
        public boolean edible() {
            return hunger >= 0;
        }

        /** Raw (dead) form; cod, salmon, clownfish and pufferfish were vanilla fish metas 0-3 and stay vanilla. */
        public String rawItem() {
            return switch (id) {
                case "cod" -> "minecraft:cod";
                case "salmon" -> "minecraft:salmon";
                case "clownfish" -> "minecraft:tropical_fish";
                case "pufferfish" -> "minecraft:pufferfish";
                default -> "mariculture:raw_" + id;
            };
        }

        public boolean hasOwnRawItem() {
            return rawItem().startsWith("mariculture:");
        }
    }

    public static final List<Entry> ALL = List.of(
            new Entry("aluminumfish", "Aluminiumfish", "aluminum", false, -1, 0.0f, 0),
            new Entry("angelfish", "Angelfish", "angel", false, 2, 0.16f, 1),
            new Entry("anglerfish", "Anglerfish", "angler", true, 2, 0.2f, 4),
            new Entry("arditefish", "Arditefish", "ardite", false, -1, 0.0f, 0),
            new Entry("bass", "Bass", "bass", false, 1, 0.1f, 5),
            new Entry("blaasop", "Silver-Stripe Blaasop", "blaasop", false, 1, 0.045f, 1),
            new Entry("blazefish", "Blazefish", "blaze", false, 1, 0.1f, 2),
            new Entry("blue_tang", "Blue Tang", "tang", false, 1, 0.0725f, 1),
            new Entry("boneless_fish", "Boneless Fish", "boneless", false, -1, 0.0f, 0),
            new Entry("bowfin", "Bowfin", "bowfin", false, 4, 0.375f, 3),
            new Entry("brown_shroom_fish", "Brown Shroom Fish", "brownshroom", false, 2, 0.1355f, 3),
            new Entry("butterflyfish", "Butterflyfish", "butterfly", false, 1, 0.012f, 1),
            new Entry("catfish", "Catfish", "catfish", false, 2, 0.8f, 7),
            new Entry("chub", "Chub", "chub", false, 12, 0.6f, 1),
            new Entry("clownfish", "Clownfish", "clown", false, 2, 0.18f, 1),
            new Entry("cobaltfish", "Cobaltfish", "cobalt", false, -1, 0.0f, 0),
            new Entry("cod", "Cod", "cod", false, 1, 0.15f, 5),
            new Entry("copperfish", "Copperfish", "copper", false, -1, 0.0f, 0),
            new Entry("damselfish", "Damselfish", "damsel", false, 1, 0.1f, 1),
            new Entry("dragonfish", "Dragonfish", "dragon", false, 3, 0.65f, 13),
            new Entry("electric_ray", "Electric Ray", "electricray", false, 7, 0.6675f, 5),
            new Entry("enderfish", "Enderfish", "ender", false, 1, 0.045f, 2),
            new Entry("glowfish", "Glowfish", "glow", false, 3, 0.4f, 5),
            new Entry("goldeneye", "Goldeneye", "goldmetal", false, -1, 0.0f, 0),
            new Entry("goldfish", "Goldfish", "gold", false, 2, 0.1125f, 1),
            new Entry("ironfish", "Ironfish", "iron", false, -1, 0.0f, 0),
            new Entry("jellyfish", "Jellyfish", "jelly", false, 1, 0.1f, 0),
            new Entry("koi", "Koi", "koi", false, 4, 0.6f, 7),
            new Entry("lamprey", "Lamprey", "lamprey", false, 3, 0.225f, 2),
            new Entry("leadfish", "Leadfish", "lead", false, -1, 0.0f, 0),
            new Entry("lungfish", "Lungfish", "lungfish", false, 5, 0.43f, 4),
            new Entry("magnesiumfish", "Magnesiumfish", "magnesium", false, -1, 0.0f, 0),
            // Botania's manafish (PluginBotania.java:41, FishMana); disabled without Botania (requires_item_tag botania:petals).
            new Entry("manafish", "Manafish", "mana", false, 1, 0.0166f, 1),
            new Entry("man_o_war", "Man O' War", "manowar", false, 1, 0.1f, 0),
            new Entry("manta_ray", "Manta Ray", "mantaray", false, 2, 0.45f, 9),
            new Entry("minecraft_fish", "Minecraft Fish", "minecraft", false, 2, 0.45f, 2),
            new Entry("minnow", "Minnow", "minnow", false, 1, 0.024f, 1),
            new Entry("neon_tetra", "Neon Tetra", "tetra", false, 1, 0.05f, 1),
            new Entry("netherfish", "Netherfish", "nether", false, 1, 0.0666f, 1),
            new Entry("nickelfish", "Ferrousfish", "nickel", false, -1, 0.0f, 0),
            new Entry("nightfish", "Nightfish", "night", false, 1, 0.0333f, 1),
            new Entry("northern_pike", "Northern Pike", "pike", false, 5, 0.8f, 5),
            new Entry("osmiumfish", "Osmiumfish", "osmium", false, -1, 0.0f, 0),
            new Entry("perch", "Perch", "perch", false, 1, 0.022f, 3),
            new Entry("pickerel", "Pickerel", "pickerel", false, 2, 0.18f, 1),
            new Entry("piranha", "Piranha", "piranha", false, 4, 0.35f, 3),
            new Entry("platinumfish", "Shinyfish", "platinum", false, -1, 0.0f, 0),
            new Entry("pufferfish", "Pufferfish", "puffer", false, 5, 0.4625f, 4),
            new Entry("pupfish", "Pupfish", "pupfish", false, 1, 0.0066f, 1),
            new Entry("rainbow_trout", "Rainbow Trout", "trout", false, 4, 0.36f, 3),
            new Entry("red_herring", "Red Herring", "herring", false, 2, 0.105f, 1),
            new Entry("red_shroom_fish", "Red Shroom Fish", "redshroom", false, 1, 0.065f, 3),
            new Entry("rutilefish", "Rutilefish", "rutile", false, -1, 0.0f, 0),
            new Entry("salmon", "Salmon", "salmon", false, 5, 0.5f, 5),
            new Entry("siamese_fighting_fish", "Siamese Fightin' Fish", "siamese", false, 1, 0.0012f, 1),
            new Entry("silverfish_metal", "Silverfish", "silver", false, -1, 0.0f, 0),
            new Entry("spiderfish", "Spiderfish", "spider", false, 2, 0.135f, 1),
            new Entry("squid", "Squid", "squid", false, 0, 0.0f, 0),
            new Entry("stargazer", "Whitemargin Stargazer", "stargazer", false, 5, 0.4725f, 4),
            new Entry("stickleback", "Stickleback", "stickleback", false, 1, 0.0075f, 1),
            new Entry("sting_ray", "Sting Ray", "stingray", false, 2, 0.1725f, 3),
            new Entry("tinfish", "Tinfish", "tin", false, -1, 0.0f, 0),
            new Entry("tuna", "Tuna", "tuna", false, 4, 0.65f, 8),
            new Entry("undead_fish", "Undead Fish", "undead", false, 1, 0.1f, 1),
            new Entry("walleye", "Walleye", "walleye", false, 4, 0.3155f, 3),
            new Entry("zincfish", "Zincfish", "zinc", false, -1, 0.0f, 0),
            // A5: Alex's Caves cave fish, two per cave biome (one in Toxic Caves) plus three more candy fish; disabled without Alex's Caves
            // (requires_item_tag mariculture:present/alexscaves).
            new Entry("acid_lamprey", "Acid Lamprey", "acid_lamprey", false, 2, 0.2f, 2),
            new Entry("lodestone_ray", "Lodestone Ray", "lodestone_ray", false, 3, 0.3f, 3),
            new Entry("ferrofish", "Ferrofish", "ferrofish", false, 2, 0.15f, 1),
            new Entry("old_lungfish", "Old Lungfish", "old_lungfish", false, 5, 0.45f, 4),
            new Entry("amberfin", "Amberfin", "amberfin", false, 3, 0.3f, 2),
            new Entry("gulper", "Gulper", "gulper", false, 4, 0.4f, 5),
            new Entry("lanternjaw", "Lanternjaw", "lanternjaw", false, 2, 0.25f, 2),
            new Entry("gloomfish", "Gloomfish", "gloomfish", false, 2, 0.2f, 2),
            new Entry("ghost_minnow", "Ghost Minnow", "ghost_minnow", false, 1, 0.05f, 1),
            new Entry("taffy_tang", "Taffy Tang", "taffy_tang", false, 2, 0.3f, 1),
            new Entry("licorice_eel", "Licorice Eel", "licorice_eel", false, 3, 0.3f, 2),
            new Entry("peppermint_pike", "Peppermint Pike", "peppermint_pike", false, 3, 0.35f, 3),
            new Entry("chocolate_catfish", "Chocolate Catfish", "chocolate_catfish", false, 4, 0.4f, 3),
            new Entry("gumdrop_goldfish", "Gumdrop Goldfish", "gumdrop_goldfish", false, 1, 0.15f, 1),
            // Metallurgy: Recast ore metals (tools/metallurgy_fish.py); each drops mariculture:metal_droplet of
            // Metallurgy's molten fluid and is disabled without its ingot (requires_item_tag c:ingots/<metal>).
            new Entry("manganesefish", "Manganesefish", "manganesefish", false, -1, 0.0f, 0),
            new Entry("lutetiumfish", "Lutetiumfish", "lutetiumfish", false, -1, 0.0f, 0),
            new Entry("prometheumfish", "Prometheumfish", "prometheumfish", false, -1, 0.0f, 0),
            new Entry("deep_ironfish", "Deep Ironfish", "deep_ironfish", false, -1, 0.0f, 0),
            new Entry("infuscoliumfish", "Infuscoliumfish", "infuscoliumfish", false, -1, 0.0f, 0),
            new Entry("oureclasefish", "Oureclasefish", "oureclasefish", false, -1, 0.0f, 0),
            new Entry("astral_silverfish", "Astral Silverfish", "astral_silverfish", false, -1, 0.0f, 0),
            new Entry("carmotfish", "Carmotfish", "carmotfish", false, -1, 0.0f, 0),
            new Entry("mithrilfish", "Mithrilfish", "mithrilfish", false, -1, 0.0f, 0),
            new Entry("rubraciumfish", "Rubraciumfish", "rubraciumfish", false, -1, 0.0f, 0),
            new Entry("orichalcumfish", "Orichalcumfish", "orichalcumfish", false, -1, 0.0f, 0),
            new Entry("adamantinefish", "Adamantinefish", "adamantinefish", false, -1, 0.0f, 0),
            new Entry("atlarusfish", "Atlarusfish", "atlarusfish", false, -1, 0.0f, 0),
            new Entry("ignatiusfish", "Ignatiusfish", "ignatiusfish", false, -1, 0.0f, 0),
            new Entry("shadow_ironfish", "Shadow Ironfish", "shadow_ironfish", false, -1, 0.0f, 0),
            new Entry("lemuritefish", "Lemuritefish", "lemuritefish", false, -1, 0.0f, 0),
            new Entry("midasiumfish", "Midasiumfish", "midasiumfish", false, -1, 0.0f, 0),
            new Entry("vyroxeresfish", "Vyroxeresfish", "vyroxeresfish", false, -1, 0.0f, 0),
            new Entry("ceruclasefish", "Ceruclasefish", "ceruclasefish", false, -1, 0.0f, 0),
            new Entry("alduoritefish", "Alduoritefish", "alduoritefish", false, -1, 0.0f, 0),
            new Entry("kalendritefish", "Kalendritefish", "kalendritefish", false, -1, 0.0f, 0),
            new Entry("vulcanitefish", "Vulcanitefish", "vulcanitefish", false, -1, 0.0f, 0),
            new Entry("sanguinitefish", "Sanguinitefish", "sanguinitefish", false, -1, 0.0f, 0),
            new Entry("eximitefish", "Eximitefish", "eximitefish", false, -1, 0.0f, 0),
            new Entry("meutoitefish", "Meutoitefish", "meutoitefish", false, -1, 0.0f, 0),
            // Thaumaturge: six magic fish, bred only in a tank of Brine of Order once their Aquamancy entry is read;
            // disabled without Thaumaturge (requires_item_tag mariculture:present/thaumaturge).
            new Entry("auramfin", "Auramfin", "auramfin", false, 2, 0.2f, 2),
            new Entry("quicksilver_eel", "Quicksilver Eel", "quicksilver_eel", false, -1, 0.0f, 2),
            new Entry("wisp_angler", "Wisp Angler", "wisp_angler", false, 1, 0.1f, 1),
            new Entry("flux_lamprey", "Flux Lamprey", "flux_lamprey", false, 2, 0.1f, 2),
            new Entry("eldritch_gulper", "Eldritch Gulper", "eldritch_gulper", false, -1, 0.0f, 3),
            new Entry("thaumium_trout", "Thaumium Trout", "thaumium_trout", false, 3, 0.3f, 2)
    );

    private ShippedSpecies() {}

    public static Entry byId(String id) {
        String path = id.startsWith("mariculture:") ? id.substring(12) : id;
        for (Entry e : ALL) if (e.id().equals(path)) return e;
        return null;
    }
}
