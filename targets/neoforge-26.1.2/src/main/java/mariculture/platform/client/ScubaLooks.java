package mariculture.platform.client;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.properties.conditional.ConditionalItemModelProperty;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.event.RegisterConditionalItemModelPropertyEvent;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import mariculture.domain.content.Ids;
import mariculture.domain.diving.Scuba;
import mariculture.platform.Stacks;

/**
 * Thaumaturge looks for scuba gear: item models switch on the warded flag and the revealing upgrade, and warded pieces
 * wear the warded armour layers. Without Thaumaturge no stack carries either mark, so nothing changes.
 */
public final class ScubaLooks {
    private ScubaLooks() {}

    private static final Identifier GOGGLES = Identifier.fromNamespaceAndPath("thaumaturge", "goggles_upgrade");

    static boolean warded(ItemStack stack) {
        var data = stack.get(DataComponents.CUSTOM_DATA);
        return data != null && data.contains(Scuba.WARDED);
    }

    static boolean revealing(ItemStack stack) {
        DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(GOGGLES);
        return type != null && stack.has(type);
    }

    public record Warded() implements ConditionalItemModelProperty {
        public static final MapCodec<Warded> MAP_CODEC = MapCodec.unit(new Warded());

        @Override
        public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context) {
            return warded(stack);
        }

        @Override
        public MapCodec<Warded> type() {
            return MAP_CODEC;
        }
    }

    public record Revealing() implements ConditionalItemModelProperty {
        public static final MapCodec<Revealing> MAP_CODEC = MapCodec.unit(new Revealing());

        @Override
        public boolean get(ItemStack stack, ClientLevel level, LivingEntity entity, int seed, ItemDisplayContext context) {
            return revealing(stack);
        }

        @Override
        public MapCodec<Revealing> type() {
            return MAP_CODEC;
        }
    }

    public static void register(IEventBus modBus) {
        modBus.addListener((RegisterConditionalItemModelPropertyEvent e) -> {
            e.register(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "warded"), Warded.MAP_CODEC);
            e.register(Identifier.fromNamespaceAndPath(Ids.MOD_ID, "revealing"), Revealing.MAP_CODEC);
        });
        modBus.addListener((RegisterClientExtensionsEvent e) -> e.registerItem(new IClientItemExtensions() {
            @Override
            public Identifier getArmorTexture(ItemStack stack, EquipmentClientInfo.LayerType type, EquipmentClientInfo.Layer layer, Identifier fallback) {
                if (!warded(stack)) return fallback;
                String path = fallback.getPath();
                int slash = path.lastIndexOf('/');
                return Identifier.fromNamespaceAndPath(fallback.getNamespace(), path.substring(0, slash + 1) + "warded_" + path.substring(slash + 1));
            }
        }, Stacks.item(Ids.MOD_ID + ":" + Scuba.MASK), Stacks.item(Ids.MOD_ID + ":" + Scuba.SUIT), Stacks.item(Ids.MOD_ID + ":" + Scuba.FINS)));
    }
}
