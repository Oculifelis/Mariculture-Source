package mariculture.domain.plugin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import mariculture.domain.config.Settings;
import mariculture.domain.fish.Species;
import mariculture.domain.metal.Anvil;

/** The successor plugins (Neo Vitae, Hephaestus, Complicated Bees), the KubeJS binding and the aquatic backpack. */
class CompatSuccessorsTest {
    @AfterEach
    void reset() {
        Tweaks.clear();
        Plugins.setModLoaded(id -> false);
    }

    @Test
    void neoVitaeCarriesTheBloodMagicTableOnItsIds() {
        long blood = Plugins.LOOT.stream().filter(l -> l.plugin() == Plugins.BLOOD_MAGIC).count();
        List<Plugins.PluginLoot> vitae = Plugins.LOOT.stream().filter(l -> l.plugin() == Plugins.NEO_VITAE).toList();
        assertEquals(blood, vitae.size());
        assertTrue(vitae.stream().allMatch(l -> l.rod() == Species.Rod.BLOOD));
        assertTrue(vitae.stream().allMatch(l -> l.item().startsWith("neovitae:") || l.item().equals("minecraft:cobweb")));
        assertTrue(Plugins.BAIT.stream().anyMatch(b -> b.plugin() == Plugins.NEO_VITAE && b.item().equals(BoundRod.NEO_VITAE_SHARD)));
        assertEquals(Settings.BOUND_ROD_WEAK_SHARD_FISH.get(), BoundRod.shardLimit(BoundRod.NEO_VITAE_SHARD));
        assertEquals(-1, BoundRod.shardLimit("neovitae:tabula_rasa"));
        assertEquals(List.of("neovitae:tabula_rasa"), Plugins.EXTRA_REPAIRS.get(BoundRod.ID));
    }

    @Test
    void successorPluginsHaveTheirOwnSwitches() {
        assertTrue(Plugins.ALL.containsAll(List.of(Plugins.NEO_VITAE, Plugins.HEPHAESTUS, Plugins.COMPLICATED_BEES)));
        Plugins.setModLoaded(Set.of("hephaestus")::contains);
        assertTrue(Plugins.hephaestusImport());
        assertFalse(Plugins.tinkersImport());
        assertEquals(12, Plugins.HEPHAESTUS_PARTS.size());
        assertEquals(Plugins.HEPHAESTUS_INGOT, Plugins.HEPHAESTUS_PARTS.get("tool_handle"));
    }

    @Test
    void kubeJsTakesTheDataFormatAndScriptNumbers() {
        Map<String, Object> change = new LinkedHashMap<>();
        change.put("type", "anvil_add");
        change.put("input", "minecraft:cobblestone");
        change.put("output", "minecraft:gravel");
        change.put("hits", 2); // Rhino hands integers over as Integer
        ScriptTweaks.INSTANCE.add(change);
        assertEquals("minecraft:gravel", Anvil.recipe("minecraft:cobblestone").output());
        ScriptTweaks.INSTANCE.add("{\"changes\": [{\"type\": \"anvil_remove\", \"input\": \"minecraft:stone\"}]}");
        assertEquals(null, Anvil.recipe("minecraft:stone"));
        ScriptTweaks.INSTANCE.reset();
        assertTrue(Tweaks.changes().isEmpty());
        Anvil.Recipe after = Anvil.recipe("minecraft:cobblestone");
        assertTrue(after == null || !after.output().equals("minecraft:gravel"));
        assertThrows(IllegalArgumentException.class, () -> ScriptTweaks.INSTANCE.add(42));
    }

    @Test
    void craftTweakerBeatsKubeJsBeatsDataPacks() {
        Tweaks.setData(TweakData.parse("p:a", "{\"type\": \"anvil_add\", \"input\": \"minecraft:dirt\", \"output\": \"minecraft:sand\", \"hits\": 1}", "a"));
        ScriptTweaks.INSTANCE.add(Map.of("type", "anvil_add", "input", "minecraft:dirt", "output", "minecraft:gravel", "hits", 1));
        assertEquals("minecraft:gravel", Anvil.recipe("minecraft:dirt").output());
        Tweaks.apply(new Tweaks.AnvilAdd(new Anvil.Recipe("minecraft:dirt", "minecraft:clay", 1, 1)));
        assertEquals("minecraft:clay", Anvil.recipe("minecraft:dirt").output());
    }

    @Test
    void aquaBackpackHoldsLegacysItems() {
        for (String id : List.of("oyster", "white_pearl", "raw_cod", "fish_meal", "fishing_net", "bait_ant", "droplet_aqua")) {
            assertTrue(Plugins.aquaBackpackItem(id), id);
        }
        assertFalse(Plugins.aquaBackpackItem("titanium_ingot"));
    }
}
