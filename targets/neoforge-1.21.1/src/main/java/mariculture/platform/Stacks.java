package mariculture.platform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.ByteTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.DoubleTag;
import net.minecraft.nbt.FloatTag;
import net.minecraft.nbt.IntTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.nbt.NumericTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

import mariculture.domain.machine.Stack;

/**
 * Converts between loader item stacks and domain {@link Stack}s. Mariculture item data lives in the
 * item-family data component (1.20.5+, see ItemData); its keys are the domain map's keys.
 */
public final class Stacks {
    private Stacks() {}

    public static Stack toDomain(ItemStack stack) {
        if (stack.isEmpty()) return Stack.EMPTY;
        Map<String, Object> data = data(stack);
        return new Stack(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), data);
    }

    public static ItemStack toLoader(Stack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        Item item = item(stack.item());
        if (item == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(item, stack.count());
        if (!stack.data().isEmpty()) {
            Map<String, Object> own = new LinkedHashMap<>();
            Map<String, Object> foreign = new LinkedHashMap<>();
            stack.data().forEach((k, v) -> (k.indexOf(':') >= 0 ? foreign : own).put(k, v));
            if (!own.isEmpty()) setData(out, own);
            if (!foreign.isEmpty()) applyComponents(out, foreign);
        }
        mariculture.platform.item.JewelryItem.apply(out, stack);
        return out;
    }

    /**
     * Sets other mods' data components named by full id in a species product's {@code components} (Thaumaturge's
     * crystal aspect). Each value is decoded with the component's own codec; an unknown component or a value the
     * codec refuses is skipped, so a pack typo never breaks a fish.
     */
    private static void applyComponents(ItemStack out, Map<String, Object> components) {
        net.minecraft.server.MinecraftServer server = net.neoforged.neoforge.server.ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        net.minecraft.resources.RegistryOps<Tag> ops = net.minecraft.resources.RegistryOps.create(net.minecraft.nbt.NbtOps.INSTANCE, server.registryAccess());
        for (Map.Entry<String, Object> e : components.entrySet()) {
            ResourceLocation key = ResourceLocation.tryParse(e.getKey());
            if (key == null || !BuiltInRegistries.DATA_COMPONENT_TYPE.containsKey(key)) continue;
            net.minecraft.core.component.DataComponentType<?> type = BuiltInRegistries.DATA_COMPONENT_TYPE.get(key);
            if (type == null || type.codec() == null) continue;
            Tag value = toTag(e.getValue());
            if (value != null) set(out, type, ops, value);
        }
    }

    private static <T> void set(ItemStack out, net.minecraft.core.component.DataComponentType<T> type,
                                net.minecraft.resources.RegistryOps<Tag> ops, Tag value) {
        type.codec().parse(ops, value).result().ifPresent(v -> out.set(type, v));
    }

    public static Item item(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) return null;
        return BuiltInRegistries.ITEM.get(key);
    }

    public static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    /** The item-family component for this stack's item (ItemData). */
    private static net.minecraft.core.component.DataComponentType<CompoundTag> type(ItemStack stack) {
        return ModContent.itemData(mariculture.domain.content.ItemData.component(id(stack.getItem())));
    }

    /**
     * The stack's Mariculture data. Stacks from 0.4.x and older still carry it as custom data; that is read as a
     * fallback and moves to the family component on the next write.
     */
    public static Map<String, Object> data(ItemStack stack) {
        CompoundTag tag = stack.get(type(stack));
        if (tag != null) return toMap(tag);
        CustomData custom = stack.get(DataComponents.CUSTOM_DATA);
        return custom == null ? Map.of() : toMap(custom.copyTag());
    }

    public static void setData(ItemStack stack, Map<String, Object> data) {
        stack.remove(DataComponents.CUSTOM_DATA);
        if (data.isEmpty()) stack.remove(type(stack));
        else stack.set(type(stack), toTag(data));
    }

    public static Map<String, Object> toMap(CompoundTag tag) {
        Map<String, Object> m = new LinkedHashMap<>();
        for (String key : tag.getAllKeys()) {
            Object v = fromTag(tag.get(key));
            if (v != null) m.put(key, v);
        }
        return m;
    }

    private static Object fromTag(Tag t) {
        if (t instanceof CompoundTag c) return toMap(c);
        if (t instanceof ListTag l) {
            List<Object> out = new ArrayList<>();
            for (Tag e : l) out.add(fromTag(e));
            return out;
        }
        if (t instanceof StringTag s) return s.getAsString();
        if (t instanceof ByteTag b) return (int) b.getAsByte();
        if (t instanceof IntTag i) return i.getAsInt();
        if (t instanceof LongTag l) return l.getAsLong();
        if (t instanceof FloatTag f) return (double) f.getAsFloat();
        if (t instanceof DoubleTag d) return d.getAsDouble();
        if (t instanceof NumericTag n) return n.getAsInt();
        return null;
    }

    public static CompoundTag toTag(Map<String, ?> map) {
        CompoundTag tag = new CompoundTag();
        map.forEach((k, v) -> {
            Tag t = toTag(v);
            if (t != null) tag.put(k, t);
        });
        return tag;
    }

    @SuppressWarnings("unchecked")
    private static Tag toTag(Object v) {
        if (v instanceof Map<?, ?> m) return toTag((Map<String, ?>) m);
        if (v instanceof List<?> l) {
            ListTag list = new ListTag();
            for (Object o : l) list.add(toTag(o));
            return list;
        }
        if (v instanceof String s) return StringTag.valueOf(s);
        if (v instanceof Boolean b) return ByteTag.valueOf(b);
        if (v instanceof Integer i) return IntTag.valueOf(i);
        if (v instanceof Long l) return LongTag.valueOf(l);
        if (v instanceof Float f) return FloatTag.valueOf(f);
        if (v instanceof Double d) return DoubleTag.valueOf(d);
        if (v instanceof Number n) return IntTag.valueOf(n.intValue());
        return null;
    }
}
