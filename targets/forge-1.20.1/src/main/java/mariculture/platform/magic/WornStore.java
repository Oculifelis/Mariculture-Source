package mariculture.platform.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.event.entity.player.PlayerEvent;

import mariculture.domain.magic.Jewelry;
import mariculture.domain.magic.JewelryMaterial;
import mariculture.domain.magic.JewelryType;
import top.theillusivec4.curios.api.CuriosApi;
import mariculture.domain.magic.WornJewelry;
import mariculture.platform.Stacks;

/**
 * The jewelry a player wears, read from its Curios ring, bracelet and necklace slots. The per-player jewelry state
 * is kept in the player's persistent data and copied to the new player on respawn (1.20.1 has no data attachments).
 * Loaded once per player object and written back on change.
 */
public final class WornStore {
    private static final String KEY = "mariculture_jewelry";
    private static final Map<Player, Worn> CACHE = new WeakHashMap<>();

    /**
     * Per-player state: jumps for Leapfrog wear, whether the One Ring and jewelry flight were on last
     * tick, and a pending resurrection (the kept inventory, where to respawn as "dimension x y z", resistance ticks
     * and the wear it costs).
     */
    public static final class Worn {
        public int jumps;
        public boolean oneRing;
        public boolean flight;
        public List<ItemStack> kept = new ArrayList<>();
        public String respawn = "";
        public int resist;
        public int resurrectWear;

        CompoundTag save() {
            CompoundTag t = new CompoundTag();
            t.putInt("jumps", jumps);
            t.putBoolean("one_ring", oneRing);
            t.putBoolean("flight", flight);
            t.put("kept", list(kept));
            t.putString("respawn", respawn);
            t.putInt("resist", resist);
            t.putInt("resurrect_wear", resurrectWear);
            return t;
        }

        static Worn load(CompoundTag t) {
            Worn w = new Worn();
            w.jumps = t.getInt("jumps");
            w.oneRing = t.getBoolean("one_ring");
            w.flight = t.getBoolean("flight");
            w.kept = stacks(t.getList("kept", Tag.TAG_COMPOUND));
            w.respawn = t.getString("respawn");
            w.resist = t.getInt("resist");
            w.resurrectWear = t.getInt("resurrect_wear");
            return w;
        }

        private static ListTag list(List<ItemStack> stacks) {
            ListTag l = new ListTag();
            for (ItemStack s : stacks) l.add(s.save(new CompoundTag()));
            return l;
        }

        private static List<ItemStack> stacks(ListTag l) {
            List<ItemStack> out = new ArrayList<>();
            for (int i = 0; i < l.size(); i++) out.add(ItemStack.of(l.getCompound(i)));
            return out;
        }
    }

    private WornStore() {}

    public static Worn get(Player player) {
        return CACHE.computeIfAbsent(player, p -> Worn.load(p.getPersistentData().getCompound(KEY)));
    }

    /** Writes the player's data back so it is saved. */
    public static void changed(Player player) {
        player.getPersistentData().put(KEY, get(player).save());
    }

    /** PlayerEvent.Clone: the new player object takes the old one's jewelry (also after death). */
    public static void onClone(PlayerEvent.Clone event) {
        Player old = event.getOriginal();
        CompoundTag data = CACHE.containsKey(old) ? get(old).save() : old.getPersistentData().getCompound(KEY);
        event.getEntity().getPersistentData().put(KEY, data.copy());
        CACHE.remove(event.getEntity());
    }

    /** Curios slot ids for the three jewelry types (the Curios presets "ring", "bracelet" and "necklace"). */
    public static String slotId(JewelryType type) {
        return type.id();
    }

    private static java.util.Optional<top.theillusivec4.curios.api.type.inventory.ICurioStacksHandler> handler(Player player, JewelryType type) {
        return CuriosApi.getCuriosInventory(player).resolve().flatMap(inv -> inv.getStacksHandler(slotId(type))).filter(h -> h.getSlots() > 0);
    }

    /** The live stacks in the player's Curios jewelry slots: rings, then bracelets, then necklaces (empty ones too). */
    public static List<ItemStack> worn(Player player) {
        List<ItemStack> out = new ArrayList<>();
        for (JewelryType t : JewelryType.values()) {
            handler(player, t).ifPresent(h -> {
                for (int i = 0; i < h.getSlots(); i++) out.add(h.getStacks().getStackInSlot(i));
            });
        }
        return out;
    }

    /** The first Curios slot of a jewelry type (the mirror shows it), or empty when the player has none. */
    public static ItemStack first(Player player, JewelryType type) {
        return handler(player, type).map(h -> h.getStacks().getStackInSlot(0)).orElse(ItemStack.EMPTY);
    }

    /** Whether the player has a Curios slot for this jewelry type. */
    public static boolean hasSlot(Player player, JewelryType type) {
        return handler(player, type).isPresent();
    }

    /** Puts a stack in the first Curios slot of a jewelry type. */
    public static void setFirst(Player player, JewelryType type, ItemStack stack) {
        handler(player, type).ifPresent(h -> h.getStacks().setStackInSlot(0, stack));
    }

    /** The worn pieces (every Curios ring, bracelet and necklace slot) for the jewelry rules; the index is into {@link #worn}. */
    public static List<WornJewelry.Piece> pieces(Player player) {
        List<WornJewelry.Piece> out = new ArrayList<>();
        List<ItemStack> worn = worn(player);
        for (int i = 0; i < worn.size(); i++) {
            ItemStack s = worn.get(i);
            if (s.isEmpty()) continue;
            JewelryMaterial m = Jewelry.material(Stacks.toDomain(s));
            out.add(new WornJewelry.Piece(i, m, Enchants.read(s), s.isDamageableItem() && s.getDamageValue() >= s.getMaxDamage(),
                    JewelryType.byItem(Stacks.id(s.getItem()))));
        }
        return out;
    }

    public static int strength(Player player, String enchantment) {
        return WornJewelry.strength(pieces(player), enchantment);
    }

    /** MirrorHandler.handleDamage: {@code points} of wear on each unbroken piece with the enchantment (not in creative). */
    public static void wear(Player player, String enchantment, int points, JewelryMaterial elemental) {
        if (points <= 0 || player.isCreative() || !(player instanceof ServerPlayer sp)) return;
        List<ItemStack> worn = worn(player);
        for (int slot : WornJewelry.wearSlots(pieces(player), enchantment, elemental)) {
            ItemStack s = worn.get(slot);
            for (int i = 0; i < points; i++) s.hurt(1, player.getRandom(), sp);
            s.setDamageValue(Math.min(s.getMaxDamage(), s.getDamageValue()));
        }
    }
}
