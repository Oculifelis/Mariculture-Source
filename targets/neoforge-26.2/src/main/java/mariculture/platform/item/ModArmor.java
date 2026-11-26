package mariculture.platform.item;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.equipment.ArmorMaterial;
import net.minecraft.world.item.equipment.ArmorType;
import net.minecraft.world.item.equipment.EquipmentAsset;
import net.minecraft.world.item.equipment.EquipmentAssets;
import net.minecraft.world.item.equipment.Equippable;

import mariculture.domain.content.ArmorMaterialSpec;
import mariculture.domain.content.ArmorSlot;
import mariculture.domain.content.Catalog;
import mariculture.domain.content.Ids;

/**
 * Armour from the catalog. Since 1.21.2 a material is a plain record pointing at an equipment asset
 * (assets/mariculture/equipment/<id>.json, written by the resource exporter).
 */
public final class ModArmor {
    private ModArmor() {}

    public static Item item(Item.Properties props, String materialId, ArmorSlot slot) {
        if (materialId.startsWith("scuba") || materialId.equals("swimfin")) return new ScubaArmorItem(props(props, materialId, slot));
        if (materialId.equals("fludd")) return new FluddItem(props(props, materialId, slot));
        return new Item(props(props, materialId, slot));
    }

    public static Item.Properties props(Item.Properties props, String materialId, ArmorSlot slot) {
        ArmorMaterialSpec spec = Catalog.armorMaterial(materialId);
        ArmorMaterial material = material(spec);
        ArmorType type = type(slot);
        // Item.Properties#humanoidArmor, minus the enchantable component when the legacy value is 0 (it must be positive).
        // A durability factor of 0 never wears (the F.L.U.D.D.); max damage must otherwise be positive.
        if (spec.durabilityFactor() > 0) props = props.durability(type.getDurability(material.durability()));
        props = props
                .attributes(material.createAttributes(type))
                .component(DataComponents.EQUIPPABLE, Equippable.builder(type.getSlot())
                        .setEquipSound(material.equipSound()).setAsset(material.assetId()).build())
                .repairable(material.repairIngredient());
        if (spec.enchantability() > 0) props = props.enchantable(spec.enchantability());
        return props;
    }

    private static ArmorMaterial material(ArmorMaterialSpec spec) {
        Map<ArmorType, Integer> defense = new EnumMap<>(ArmorType.class);
        for (ArmorSlot slot : ArmorSlot.values()) defense.put(type(slot), spec.defense(slot));
        ResourceKey<EquipmentAsset> asset = ResourceKey.create(EquipmentAssets.ROOT_ID, Identifier.fromNamespaceAndPath(Ids.MOD_ID, spec.id()));
        return new ArmorMaterial(spec.durabilityFactor(), defense, spec.enchantability(), SoundEvents.ARMOR_EQUIP_LEATHER, 0f, 0f,
                ItemTags.create(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "repairs/" + spec.id())), asset);
    }

    private static ArmorType type(ArmorSlot slot) {
        return switch (slot) {
            case HEAD -> ArmorType.HELMET;
            case CHEST -> ArmorType.CHESTPLATE;
            case LEGS -> ArmorType.LEGGINGS;
            case FEET -> ArmorType.BOOTS;
        };
    }
}
