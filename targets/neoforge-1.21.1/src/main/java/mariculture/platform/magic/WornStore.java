package mariculture.platform.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.neoforged.bus.api.IEventBus;
import top.theillusivec4.curios.api.CuriosApi;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

import mariculture.domain.content.Ids;
import mariculture.domain.magic.Jewelry;
import mariculture.domain.magic.JewelryMaterial;
import mariculture.domain.magic.JewelryType;
import mariculture.domain.magic.WornJewelry;
import mariculture.platform.Stacks;

/**
 * The jewelry a player wears, read from its Curios ring, bracelet and necklace slots, plus the per-player jewelry
 * state kept with the player through death.
 */
public final class WornStore {
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

        Worn() {}

        Worn(int jumps, boolean oneRing, boolean flight, List<ItemStack> kept, String respawn, int resist, int resurrectWear) {
            this.jumps = jumps;
            this.oneRing = oneRing;
            this.flight = flight;
            this.kept = new ArrayList<>(kept);
            this.respawn = respawn;
            this.resist = resist;
            this.resurrectWear = resurrectWear;
        }
    }

    private static final Codec<Worn> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("jumps", 0).forGetter(w -> w.jumps),
            Codec.BOOL.optionalFieldOf("one_ring", false).forGetter(w -> w.oneRing),
            Codec.BOOL.optionalFieldOf("flight", false).forGetter(w -> w.flight),
            ItemStack.OPTIONAL_CODEC.listOf().optionalFieldOf("kept", List.of()).forGetter(w -> w.kept),
            Codec.STRING.optionalFieldOf("respawn", "").forGetter(w -> w.respawn),
            Codec.INT.optionalFieldOf("resist", 0).forGetter(w -> w.resist),
            Codec.INT.optionalFieldOf("resurrect_wear", 0).forGetter(w -> w.resurrectWear)).apply(i, Worn::new));

    private static final DeferredRegister<AttachmentType<?>> ATTACHMENTS = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, Ids.MOD_ID);
    private static final Supplier<AttachmentType<Worn>> WORN = ATTACHMENTS.register("worn_jewelry",
            () -> AttachmentType.builder(Worn::new).serialize(CODEC).copyOnDeath().build());

    private WornStore() {}

    public static void register(IEventBus modBus) {
        ATTACHMENTS.register(modBus);
    }

    public static Worn get(Player player) {
        return player.getData(WORN);
    }

    /** Marks the data changed so it is saved. */
    public static void changed(Player player) {
        player.setData(WORN, get(player));
    }

    /** Curios slot ids for the three jewelry types (the Curios presets "ring", "bracelet" and "necklace"). */
    public static String slotId(JewelryType type) {
        return type.id();
    }

    /** The live stacks in the player's Curios jewelry slots: rings, then bracelets, then necklaces (empty ones too). */
    public static List<ItemStack> worn(Player player) {
        List<ItemStack> out = new ArrayList<>();
        var inv = CuriosApi.getCuriosInventory(player);
        if (inv.isEmpty()) return out;
        for (JewelryType t : JewelryType.values()) {
            inv.get().getStacksHandler(slotId(t)).ifPresent(h -> {
                for (int i = 0; i < h.getSlots(); i++) out.add(h.getStacks().getStackInSlot(i));
            });
        }
        return out;
    }

    /** The first Curios slot of a jewelry type (the mirror shows it), or empty when the player has none. */
    public static ItemStack first(Player player, JewelryType type) {
        return CuriosApi.getCuriosInventory(player).flatMap(inv -> inv.getStacksHandler(slotId(type)))
                .filter(h -> h.getSlots() > 0).map(h -> h.getStacks().getStackInSlot(0)).orElse(ItemStack.EMPTY);
    }

    /** Whether the player has a Curios slot for this jewelry type. */
    public static boolean hasSlot(Player player, JewelryType type) {
        return CuriosApi.getCuriosInventory(player).flatMap(inv -> inv.getStacksHandler(slotId(type))).map(h -> h.getSlots() > 0).orElse(false);
    }

    /** Puts a stack in the first Curios slot of a jewelry type. */
    public static void setFirst(Player player, JewelryType type, ItemStack stack) {
        CuriosApi.getCuriosInventory(player).flatMap(inv -> inv.getStacksHandler(slotId(type)))
                .filter(h -> h.getSlots() > 0).ifPresent(h -> h.getStacks().setStackInSlot(0, stack));
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
        if (points <= 0 || player.isCreative() || !(player.level() instanceof ServerLevel level)) return;
        List<ItemStack> worn = worn(player);
        for (int slot : WornJewelry.wearSlots(pieces(player), enchantment, elemental)) {
            ItemStack s = worn.get(slot);
            for (int i = 0; i < points; i++) {
                int amount = EnchantmentHelper.processDurabilityChange(level, s, 1);
                s.setDamageValue(Math.min(s.getMaxDamage(), s.getDamageValue() + amount));
            }
        }
    }
}
