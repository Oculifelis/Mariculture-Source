package mariculture.platform.item;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import mariculture.domain.content.ArmorMaterialSpec;
import mariculture.domain.content.ArmorSlot;
import mariculture.domain.content.Catalog;
import mariculture.domain.content.Ids;

/** Armour materials from the catalog (registry entries since 1.20.5) and armour item construction. */
public final class ModArmor {
    private static final DeferredRegister<ArmorMaterial> MATERIALS = DeferredRegister.create(Registries.ARMOR_MATERIAL, Ids.MOD_ID);
    private static final Map<String, Holder<ArmorMaterial>> BY_ID = new HashMap<>();

    static {
        for (ArmorMaterialSpec spec : Catalog.armorMaterials()) {
            BY_ID.put(spec.id(), MATERIALS.register(spec.id(), () -> material(spec)));
        }
    }

    private ModArmor() {}

    public static void register(IEventBus modBus) {
        MATERIALS.register(modBus);
    }

    public static Item item(Item.Properties props, String materialId, ArmorSlot slot) {
        ArmorMaterialSpec spec = Catalog.armorMaterial(materialId);
        // A durability factor of 0 never wears (the F.L.U.D.D.).
        if (spec.durabilityFactor() > 0) props = props.durability(spec.durability(slot));
        else props = props.stacksTo(1);
        if (materialId.startsWith("scuba") || materialId.equals("swimfin")) {
            return new ScubaArmorItem(BY_ID.get(materialId), type(slot), props);
        }
        if (materialId.equals("fludd")) return new FluddItem(BY_ID.get(materialId), type(slot), props);
        return new ArmorItem(BY_ID.get(materialId), type(slot), props);
    }

    private static ArmorMaterial material(ArmorMaterialSpec spec) {
        Map<ArmorItem.Type, Integer> defense = new EnumMap<>(ArmorItem.Type.class);
        for (ArmorSlot slot : ArmorSlot.values()) defense.put(type(slot), spec.defense(slot));
        defense.put(ArmorItem.Type.BODY, 0);
        // One layer: vanilla picks textures/models/armor/<id>_layer_1.png, or _layer_2.png for leggings.
        List<ArmorMaterial.Layer> layers = List.of(new ArmorMaterial.Layer(ResourceLocation.fromNamespaceAndPath(Ids.MOD_ID, spec.id())));
        var repair = ItemTags.create(ResourceLocation.fromNamespaceAndPath(Ids.MOD_ID, "repairs/" + spec.id()));
        return new ArmorMaterial(defense, spec.enchantability(), SoundEvents.ARMOR_EQUIP_LEATHER, () -> Ingredient.of(repair), layers, 0f, 0f);
    }

    private static ArmorItem.Type type(ArmorSlot slot) {
        return switch (slot) {
            case HEAD -> ArmorItem.Type.HELMET;
            case CHEST -> ArmorItem.Type.CHESTPLATE;
            case LEGS -> ArmorItem.Type.LEGGINGS;
            case FEET -> ArmorItem.Type.BOOTS;
        };
    }
}
