package mariculture.platform.item;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.crafting.Ingredient;

import mariculture.domain.content.ArmorMaterialSpec;
import mariculture.domain.content.ArmorSlot;
import mariculture.domain.content.Catalog;
import mariculture.domain.content.Ids;

/**
 * Armour from the catalog. On 1.20.1 a material is an object whose name ("mariculture:<id>") picks the layer
 * textures textures/models/armor/<id>_layer_1.png and _layer_2.png.
 */
public final class ModArmor {
    private ModArmor() {}

    public static Item item(Item.Properties props, String materialId, ArmorSlot slot) {
        if (materialId.startsWith("scuba") || materialId.equals("swimfin")) {
            return new ScubaArmorItem(new Material(Catalog.armorMaterial(materialId)), type(slot), props);
        }
        if (materialId.equals("fludd")) return new FluddItem(new Material(Catalog.armorMaterial(materialId)), type(slot), props);
        return new ArmorItem(new Material(Catalog.armorMaterial(materialId)), type(slot), props);
    }

    private record Material(ArmorMaterialSpec spec) implements ArmorMaterial {
        @Override
        public int getDurabilityForType(ArmorItem.Type type) {
            return spec.durability(slot(type));
        }

        @Override
        public int getDefenseForType(ArmorItem.Type type) {
            return spec.defense(slot(type));
        }

        @Override
        public int getEnchantmentValue() {
            return spec.enchantability();
        }

        @Override
        public SoundEvent getEquipSound() {
            return SoundEvents.ARMOR_EQUIP_LEATHER;
        }

        @Override
        public Ingredient getRepairIngredient() {
            return Ingredient.of(ItemTags.create(new ResourceLocation(Ids.MOD_ID, "repairs/" + spec.id())));
        }

        @Override
        public String getName() {
            return Ids.MOD_ID + ":" + spec.id();
        }

        @Override
        public float getToughness() {
            return 0f;
        }

        @Override
        public float getKnockbackResistance() {
            return 0f;
        }
    }

    private static ArmorItem.Type type(ArmorSlot slot) {
        return switch (slot) {
            case HEAD -> ArmorItem.Type.HELMET;
            case CHEST -> ArmorItem.Type.CHESTPLATE;
            case LEGS -> ArmorItem.Type.LEGGINGS;
            case FEET -> ArmorItem.Type.BOOTS;
        };
    }

    private static ArmorSlot slot(ArmorItem.Type type) {
        return switch (type) {
            case HELMET -> ArmorSlot.HEAD;
            case CHESTPLATE -> ArmorSlot.CHEST;
            case LEGGINGS -> ArmorSlot.LEGS;
            case BOOTS -> ArmorSlot.FEET;
        };
    }
}
