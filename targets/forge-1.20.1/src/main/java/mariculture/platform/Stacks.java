package mariculture.platform;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

import mariculture.domain.machine.Stack;

/**
 * Converts between loader item stacks and domain {@link Stack}s. On 1.20.1 Mariculture item data is the
 * stack's NBT tag; its keys are the domain map's keys.
 */
public final class Stacks {
    private Stacks() {}

    public static Stack toDomain(ItemStack stack) {
        if (stack.isEmpty()) return Stack.EMPTY;
        Map<String, Object> data = stack.getTag() == null ? Map.of() : toMap(stack.getTag());
        return new Stack(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(), stack.getCount(), data);
    }

    public static ItemStack toLoader(Stack stack) {
        if (stack.isEmpty()) return ItemStack.EMPTY;
        Item item = item(stack.item());
        if (item == null) return ItemStack.EMPTY;
        ItemStack out = new ItemStack(item, stack.count());
        if (!stack.data().isEmpty()) out.setTag(toTag(stack.data()));
        mariculture.platform.item.JewelryItem.apply(out, stack);
        return out;
    }

    public static Item item(String id) {
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || !BuiltInRegistries.ITEM.containsKey(key)) return null;
        return BuiltInRegistries.ITEM.get(key);
    }

    public static String id(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).toString();
    }

    public static Map<String, Object> data(ItemStack stack) {
        return stack.getTag() == null ? Map.of() : toMap(stack.getTag());
    }

    public static void setData(ItemStack stack, Map<String, Object> data) {
        stack.setTag(data.isEmpty() ? null : toTag(data));
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
