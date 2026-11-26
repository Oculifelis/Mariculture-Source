package mariculture.domain.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import mariculture.domain.config.Settings;
import mariculture.domain.content.Catalog;
import mariculture.domain.content.Ids;
import mariculture.domain.env.BiomeEnvironment;
import mariculture.domain.fish.Salinity;
import mariculture.domain.fish.Species;
import mariculture.domain.fishery.Bait;
import mariculture.domain.metal.Anvil;
import mariculture.domain.metal.CrucibleFuels;
import mariculture.domain.metal.Melting;
import mariculture.domain.metal.MetalRates;

class PluginsTest {
    private static void installed(String... mods) {
        Set<String> set = Set.of(mods);
        Plugins.setModLoaded(set::contains);
    }

    @AfterEach
    void reset() {
        Plugins.setModLoaded(id -> false);
        for (Plugins.Plugin p : Plugins.ALL) p.toggle().set(true);
    }

    @Test
    void aPluginNeedsItsModAndItsSwitch() {
        installed();
        assertFalse(Plugins.active(Plugins.RAILCRAFT));
        installed("railcraft");
        assertTrue(Plugins.active(Plugins.RAILCRAFT));
        Settings.PLUGIN_RAILCRAFT.set(false);
        assertFalse(Plugins.active(Plugins.RAILCRAFT));
        assertEquals(Plugins.TCONSTRUCT, Plugins.byName("tconstruct"));
        assertNull(Plugins.byName("hee"));
    }

    @Test
    void compatRodsHideWithoutTheirMod() {
        installed();
        assertTrue(Plugins.hiddenWithoutPlugin("mariculture:livingwood_fishing_rod"));
        assertTrue(Plugins.hiddenWithoutPlugin("mariculture:bound_fishing_rod"));
        assertFalse(Plugins.hiddenWithoutPlugin("mariculture:titanium_fishing_rod"));
        installed("botania", "neovitae");
        assertFalse(Plugins.hiddenWithoutPlugin("mariculture:livingwood_fishing_rod"));
        assertFalse(Plugins.hiddenWithoutPlugin("mariculture:bound_fishing_rod"));
        installed("bloodmagic");
        assertFalse(Plugins.hiddenWithoutPlugin("mariculture:bound_fishing_rod"));
    }

    @Test
    void railcraftSteelToolsMeltWithAStickBack() {
        assertNull(Melting.find("railcraft:steel_pickaxe", 2000, t -> false, null));
        installed("railcraft");
        Melting.Recipe r = Melting.find("railcraft:steel_pickaxe", 2000, t -> false, null);
        assertEquals("mariculture:molten_steel", r.fluid());
        assertEquals(MetalRates.INGOT * 3, r.amount());
        assertEquals("minecraft:stick", r.bonus());
        assertEquals(MetalRates.INGOT * 8, Melting.find("railcraft:steel_chestplate", 2000, t -> false, null).amount());
        assertNull(Melting.find("railcraft:steel_pickaxe", 1369, t -> false, null));
    }

    @Test
    void cokeFuelsTheCrucibleByTag() {
        CrucibleFuels.Fuel block = CrucibleFuels.item("railcraft:coal_coke_block", t -> t.equals("c:storage_blocks/coal_coke"));
        assertEquals(32400, block.ticksPer());
        assertEquals(256, CrucibleFuels.item("railcraft:coal_coke", t -> t.equals("c:coal_coke")).maxTempPer());
    }

    @Test
    void thermalRedstoneAndGlowstoneMeltIntoThermalFluids() {
        assertNull(Melting.find("minecraft:redstone", 2000, t -> false, null));
        installed("thermal");
        assertEquals("thermal:redstone", Melting.find("minecraft:redstone_block", 2000, t -> false, null).fluid());
        assertEquals(900, Melting.find("minecraft:redstone_block", 2000, t -> false, null).amount());
        assertEquals(1000, Melting.find("minecraft:glowstone", 2000, t -> false, null).amount());
        assertTrue(Melting.table().stream().anyMatch(r -> r.fluid() != null && r.fluid().equals("thermal:glowstone")));
    }

    @Test
    void tinkersManyullynMeltsAtTinkersRates() {
        installed("tconstruct");
        Melting.Recipe r = Melting.find("tconstruct:manyullyn_ingot", 2000, t -> t.equals("c:ingots/manyullyn"), null);
        assertEquals("tconstruct:molten_manyullyn", r.fluid());
        assertEquals(90, r.amount());
    }

    @Test
    void aquacultureWormIsBaitAndItsFishMeltIntoOil() {
        assertEquals(0, Bait.quality("aquaculture:worm"));
        installed("aquaculture");
        assertEquals(25, Bait.quality("aquaculture:worm"));
        assertTrue(Bait.canUse(Species.Rod.DIRE, "aquaculture:worm"));
        assertFalse(Bait.canUse(Species.Rod.GOOD, "aquaculture:worm"));
        assertFalse(Plugins.recipes().isEmpty());
        assertTrue(Plugins.recipes().stream().filter(c -> c.recipe().id().startsWith("aquaculture/")).allMatch(c -> c.plugin() == Plugins.AQUACULTURE));
    }

    @Test
    void anyModsFishInTheForeignFishTagMeltIntoOil() {
        // G1 / PLUG-027: the tag decides, not the mod id, and no plugin needs to be installed.
        java.util.function.Predicate<String> inTag = t -> t.equals(ForeignFish.TAG);
        Melting.Recipe oil = Melting.find("upgrade_aquatic:pike", 200, inTag, null);
        assertEquals(Plugins.FOREIGN_FISH_OIL, oil.amount());
        assertEquals("minecraft:bone", oil.bonus());
        assertNotNull(Melting.find("aquaculture:pike", 200, inTag, null));
        assertNull(Melting.find("othermod:pike", 200, t -> t.equals("c:foods/raw_fish"), null));
        assertFalse(ForeignFish.is("mariculture:raw_bass", inTag));
        assertTrue(ForeignFish.ITEMS.stream().noneMatch(i -> i.startsWith("minecraft:") || i.startsWith("mariculture:")));
    }

    @Test
    void newFishingModPluginsAreListed() {
        for (String n : java.util.List.of("upgrade_aquatic", "alexscaves", "starcatcher", "stardew_fishing", "tide")) assertNotNull(Plugins.byName(n));
    }

    @Test
    void aquacultureRecipesStayAquacultureOnly() {
        assertFalse(Plugins.recipes().isEmpty());
        assertTrue(Plugins.recipes().stream().filter(c -> c.recipe().id().startsWith("aquaculture/")).allMatch(c -> c.plugin() == Plugins.AQUACULTURE));
    }

    @Test
    void biomesOPlentyBiomesAndFlowers() {
        mariculture.domain.env.BiomeData.loadShipped();
        assertEquals(10, BiomeEnvironment.of("biomesoplenty:volcano", t -> false).temperature());
        installed("biomesoplenty");
        mariculture.domain.env.BiomeData.loadShipped();
        BiomeEnvironment.Entry v = BiomeEnvironment.of("biomesoplenty:volcano", t -> false);
        assertEquals(75, v.temperature());
        assertEquals(Salinity.BRACKISH, BiomeEnvironment.of("biomesoplenty:jade_cliffs", t -> false).salinity());
        Anvil.Recipe r = Anvil.recipe("biomesoplenty:lavender");
        assertNotNull(r);
        assertEquals("minecraft:purple_dye", r.output());
        assertEquals(2, r.count());
        assertTrue(Anvil.recipes().stream().anyMatch(a -> a.input().equals("biomesoplenty:icy_iris")));
        Settings.PLUGIN_BIOMES_O_PLENTY.set(false);
        assertNull(Anvil.recipe("biomesoplenty:lavender"));
        BiomeEnvironment.rebuild();
        assertEquals(10, BiomeEnvironment.of("biomesoplenty:volcano", t -> false).temperature(), "switch off drops the BoP file");
    }

    @Test
    void projectEValuesNameRealItemsAndAreWhole() {
        Set<String> ids = new java.util.HashSet<>();
        Catalog.items().forEach(i -> ids.add(Ids.full(i.id())));
        Catalog.blocks().forEach(b -> ids.add(Ids.full(b.id())));
        for (var e : Plugins.EMC.entrySet()) {
            assertTrue(ids.contains(e.getKey()), e.getKey());
            assertTrue(e.getValue() >= 1, e.getKey());
        }
        assertEquals(24576L, Plugins.EMC.get("mariculture:oyster"));
        assertEquals(114L, Plugins.EMC.get("mariculture:droplet_gold"));
        assertFalse(Plugins.EMC.containsKey("mariculture:droplet_useless"));
    }

    @Test
    void forestryBioethanolIsTheLegacyBioethanol() {
        assertEquals(CrucibleFuels.fluid("mariculture:bioethanol"), CrucibleFuels.fluid("forestry:bio_ethanol"));
        assertNotNull(CrucibleFuels.fluid("forestry:bio_ethanol"));
        var plastic = mariculture.domain.metal.VatRecipes.find("forestry:bio_ethanol", 1500, null, 0,
                mariculture.domain.machine.Stack.of("minecraft:glass"), t -> false);
        assertNotNull(plastic);
        assertEquals("mariculture:plastic_block", plastic.outItem().item());
        assertEquals(7, Plugins.FORESTRY_BEE_BAIT.get("queen"));
    }

    @Test
    void forestrySqueezesWholeBucketsOfFishOil() {
        Species cod = species(2500, "minecraft:leather", 6);
        Plugins.Squeeze q = Plugins.squeeze(cod, "minecraft:cod");
        assertEquals(2000, q.fishOil());
        assertEquals(0.06f, q.chance(), 1e-6);
        assertEquals("minecraft:leather", q.remnant());
        assertNull(Plugins.squeeze(species(500, "minecraft:leather", 6), "minecraft:cod"));
        assertNull(Plugins.squeeze(species(2500, null, 0), "minecraft:cod"));
    }

    private static Species species(int oil, String product, int oneIn) {
        String json;
        try {
            json = java.nio.file.Files.readString(java.nio.file.Path.of("../content-source/data/mariculture/mariculture/fish_species/cod.json"));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
        json = json.replaceFirst("\"fish_oil_mb\": \\d+", "\"fish_oil_mb\": " + oil);
        if (product == null) json = json.replaceFirst(",\\s*\"liquified_product\": \\{[^}]*\\}", "");
        else json = json.replaceFirst("\"one_in\": \\d+", "\"one_in\": " + oneIn);
        return Species.parse("cod", json, "cod.json");
    }

    @Test
    void botaniaPetalsBaitTheLivingRodAndItsLootIsItsOwn() {
        assertEquals(0, Bait.quality("botania:red_petal"));
        assertFalse(Bait.canUse(Species.Rod.MANA, "mariculture:bait_grasshopper"));
        installed("botania");
        assertEquals(70, Bait.quality("botania:red_petal"));
        assertTrue(Bait.canUse(Species.Rod.MANA, "botania:red_petal"));
        assertTrue(Bait.canUse(Species.Rod.MANA, "mariculture:bait_grasshopper"));
        assertFalse(Bait.canUse(Species.Rod.SUPER, "botania:red_petal"));
        assertEquals(40, Bait.quality("mariculture:bait_grasshopper"));
        assertTrue(Plugins.loot(Species.Rod.SUPER).isEmpty());
        assertEquals(16 + 32 + 4 + 2 + 1, Plugins.loot(Species.Rod.MANA).size());
        Plugins.setItemExists(id -> !id.contains("record_gaia"));
        try {
            assertEquals(16 + 32 + 2 + 2 + 1, Plugins.loot(Species.Rod.MANA).size());
        } finally {
            Plugins.setItemExists(id -> true);
        }
        assertEquals(90, mariculture.domain.fishing.FishingRules.stats(Species.Rod.MANA).quality());
    }

    @Test
    void botaniaAddsTheManaPearlToOysters() {
        var settings = mariculture.domain.pearl.Oyster.Settings.LEGACY;
        assertTrue(mariculture.domain.pearl.Oyster.table(settings).stream().noneMatch(w -> w.item().equals(Plugins.MANA_PEARL)));
        installed("botania");
        assertTrue(mariculture.domain.pearl.Oyster.table(settings).stream().anyMatch(w -> w.item().equals(Plugins.MANA_PEARL) && w.weight() == 1));
        assertTrue(Plugins.recipes().stream().anyMatch(c -> c.plugin() == Plugins.BOTANIA && c.recipe().id().equals("livingwood_fishing_rod")));
    }

    @Test
    void bloodMagicBaitAndLootBelongToTheBoundRod() {
        assertEquals(0, Bait.quality(BoundRod.WEAK_SHARD));
        installed("bloodmagic");
        assertEquals(75, Bait.quality(BoundRod.WEAK_SHARD));
        assertEquals(35, Bait.quality("minecraft:rotten_flesh"));
        assertTrue(Bait.canUse(Species.Rod.BLOOD, "minecraft:rotten_flesh"));
        assertFalse(Bait.canUse(Species.Rod.SUPER, "minecraft:rotten_flesh"));
        assertEquals(10, Plugins.loot(Species.Rod.BLOOD).size());
        assertTrue(Plugins.loot(Species.Rod.MANA).isEmpty());
        assertEquals(95, mariculture.domain.fishing.FishingRules.stats(Species.Rod.BLOOD).quality());
    }

    @Test
    void boundRodShardSlotsWrapBothWays() {
        assertEquals(8, BoundRod.shardSlot(0, s -> s == 8));
        assertEquals(1, BoundRod.shardSlot(0, s -> s == 1));
        assertEquals(3, BoundRod.shardSlot(4, s -> s == 3 || s == 5));
        // BUG-072: legacy wrapped the right slot at 8, so a rod in slot 7 never saw slot 8.
        assertEquals(8, BoundRod.shardSlot(7, s -> s == 8));
        assertEquals(0, BoundRod.shardSlot(8, s -> s == 0));
        assertEquals(-1, BoundRod.shardSlot(4, s -> false));
        assertEquals(8, BoundRod.shardLimit(BoundRod.WEAK_SHARD));
        assertEquals(-1, BoundRod.shardLimit("minecraft:stone"));
    }

    @Test
    void boundRodFishesWaterAroundThePlayerUpToTheLimitPlusOne() {
        mariculture.domain.random.Rng zero = new mariculture.domain.random.Rng() {
            public float nextFloat() { return 0; }
            public int nextInt(int bound) { return 0; }
        };
        BoundRod.Result all = BoundRod.catches(p -> true, 8, zero);
        assertEquals(9, all.catches().size());
        assertNotNull(all.lightning());
        assertEquals(-3, all.catches().get(0)[0]);
        BoundRod.Result below = BoundRod.catches(p -> BoundRod.dy(p) < 0, 1000, zero);
        assertEquals(7 * 7 * 3, below.catches().size());
        assertTrue(below.catches().stream().allMatch(o -> o[1] < 0));
        for (int x = -3; x <= 3; x++) {
            int packed = BoundRod.pack(x, -x, 2);
            assertEquals(x, BoundRod.dx(packed));
            assertEquals(-x, BoundRod.dy(packed));
            assertEquals(2, BoundRod.dz(packed));
        }
    }

    @Test
    void redstoneArsenalFluxRodFishesAsTheInfusedTier() {
        String rod = "redstone_arsenal:flux_fishing_rod";
        mariculture.domain.fishing.RodData.loadShipped();
        assertNull(Plugins.rodTier(rod));
        assertNull(mariculture.domain.maritech.Autofisher.rod(rod));
        installed("redstone_arsenal");
        mariculture.domain.fishing.RodTiers.rebuild();
        assertEquals(Species.Rod.INFUSED, Plugins.rodTier(rod));
        assertEquals(Species.Rod.INFUSED, mariculture.domain.maritech.Autofisher.rod(rod));
        assertEquals(80, Bait.quality("redstone_arsenal:flux_dust"));
        assertTrue(Bait.canUse(Species.Rod.INFUSED, "redstone_arsenal:flux_dust"));
        assertTrue(Bait.canUse(Species.Rod.INFUSED, "mariculture:bait_maggot"));
        assertEquals(75, mariculture.domain.fishing.FishingRules.stats(Species.Rod.INFUSED).quality());
        Settings.PLUGIN_REDSTONE_ARSENAL.set(false);
        mariculture.domain.fishing.RodTiers.rebuild();
        assertNull(Plugins.rodTier(rod));
    }

    @Test
    void aquacultureRodsFishAsMaricultureTiersFromData() {
        mariculture.domain.fishing.RodData.loadShipped();
        assertNull(Plugins.rodTier("aquaculture:diamond_fishing_rod"));
        installed("aquaculture");
        mariculture.domain.fishing.RodTiers.rebuild();
        assertEquals(Species.Rod.GOOD, Plugins.rodTier("aquaculture:iron_fishing_rod"), "AQ1");
        assertEquals(Species.Rod.DIRE, mariculture.domain.maritech.Autofisher.rod("aquaculture:diamond_fishing_rod"));
        assertEquals(Species.Rod.SUPER, Plugins.rodTier("aquaculture:neptunium_fishing_rod"));
        java.util.List<mariculture.domain.fishing.RodTiers.Layer> l = new java.util.ArrayList<>();
        l.add(mariculture.domain.fishing.RodTiers.parse("{\"plugin\":\"aquaculture\",\"rods\":{\"aquaculture:iron_fishing_rod\":\"good\"}}", "a"));
        l.add(mariculture.domain.fishing.RodTiers.parse("{\"rods\":{\"aquaculture:iron_fishing_rod\":\"none\",\"pack:rod\":\"flux\"}}", "b"));
        mariculture.domain.fishing.RodTiers.setData(l);
        assertNull(Plugins.rodTier("aquaculture:iron_fishing_rod"), "a later file removes a rod");
        assertEquals(Species.Rod.FLUX, Plugins.rodTier("pack:rod"));
        Settings.PLUGIN_AQUACULTURE.set(false);
        mariculture.domain.fishing.RodData.loadShipped();
        assertNull(Plugins.rodTier("aquaculture:iron_fishing_rod"));
    }

    @Test
    void harvestCraftCooksMaricultureFishThroughItsTags() {
        var tags = Catalog.itemTags();
        assertFalse(tags.get("c:rawfish").isEmpty());
        assertEquals(tags.get("c:foods/raw_fish"), tags.get("c:rawfish"));
        assertEquals(tags.get("c:foods/raw_fish"), tags.get("c:fishes"));
    }

    @Test
    void thaumaturgeMachineRowsNeedThaumaturge() {
        installed();
        assertFalse(Plugins.vat().stream().anyMatch(r -> r.outFluid() != null && r.outFluid().fluid().equals("mariculture:brine_of_order")));
        assertTrue(Plugins.sifting().stream().noneMatch(r -> r.bait().startsWith("thaumaturge:")));
        assertEquals(0, Plugins.fishFood("thaumaturge:brain"));
        installed("thaumaturge");
        assertTrue(Plugins.vat().stream().anyMatch(r -> r.outFluid() != null && r.outFluid().fluid().equals("mariculture:brine_of_order")));
        assertTrue(Plugins.sifting().stream().anyMatch(r -> r.bait().equals("thaumaturge:amber")));
        assertTrue(Plugins.sifting().stream().anyMatch(r -> r.bait().equals("thaumaturge:nugget_quicksilver")));
        assertEquals(6, Plugins.fishFood("thaumaturge:brain"));
        assertTrue(Plugins.melting().stream().anyMatch(m -> m.plugin() == Plugins.THAUMATURGE && m.input().equals("thaumaturge:ingot_thaumium")));
        assertTrue(Plugins.melting().stream().anyMatch(m -> m.plugin() == Plugins.THAUMATURGE && m.input().equals("thaumaturge:metal_void")));
    }
}
