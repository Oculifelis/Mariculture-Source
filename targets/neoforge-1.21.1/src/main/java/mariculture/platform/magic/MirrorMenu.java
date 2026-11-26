package mariculture.platform.magic;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;

import mariculture.domain.magic.Jewelry;
import mariculture.domain.magic.JewelryMaterial;
import mariculture.domain.magic.JewelryType;
import mariculture.domain.magic.MirrorEnchanting;
import mariculture.domain.magic.WornJewelry;
import mariculture.platform.ModContent;
import mariculture.platform.Rngs;
import mariculture.platform.Stacks;

/**
 * The mirror (legacy ContainerMirror): the ring, bracelet and necklace in the player's first Curios slot of each
 * type (so jewelry can be worn from here or from the Curios screen), an enchanting slot that is
 * emptied when it closes, and for magic and celestial mirrors three enchanting levels (buttons 0-2).
 */
public class MirrorMenu extends AbstractContainerMenu {
    private final Player player;
    private Runnable onChange = () -> {};
    private final SimpleContainer storage = new SimpleContainer(WornJewelry.SLOTS) {
        @Override
        public void setChanged() {
            super.setChanged();
            onChange.run();
        }
    };
    private final ContainerData levels;
    private final InteractionHand hand;

    /** Client side. */
    public MirrorMenu(int id, Inventory inv) {
        this(id, inv, null, new SimpleContainerData(3));
    }

    /** Server side: backed by the player's worn jewelry. */
    public MirrorMenu(int id, Inventory inv, InteractionHand hand) {
        this(id, inv, hand, new SimpleContainerData(3));
        JewelryType[] types = JewelryType.values();
        for (JewelryType t : types) storage.setItem(t.slot, WornStore.first(inv.player, t).copy());
        onChange = () -> {
            for (JewelryType t : types) {
                ItemStack now = storage.getItem(t.slot);
                if (!ItemStack.matches(now, WornStore.first(inv.player, t))) WornStore.setFirst(inv.player, t, now.copy());
            }
        };
        int[] l = MirrorEnchanting.levels(Stacks.toDomain(inv.player.getItemInHand(hand)));
        for (int i = 0; i < 3; i++) levels.set(i, l[i]);
    }

    private MirrorMenu(int id, Inventory inv, InteractionHand hand, ContainerData levels) {
        super(ModContent.mirrorMenu(), id);
        this.player = inv.player;
        this.hand = hand;
        this.levels = levels;
        int[][] pos = {{26, 17}, {26, 39}, {26, 61}, {80, 39}};
        for (int i = 0; i < WornJewelry.SLOTS; i++) {
            final int slot = i;
            addSlot(new Slot(storage, i, pos[i][0], pos[i][1]) {
                @Override
                public boolean mayPlace(ItemStack stack) {
                    if (!WornJewelry.accepts(slot, Stacks.id(stack.getItem()))) return false;
                    if (slot == WornJewelry.ENCHANT_SLOT) return true;
                    JewelryType t = JewelryType.byItem(Stacks.id(stack.getItem()));
                    return t != null && WornStore.hasSlot(player, t);
                }

                @Override
                public int getMaxStackSize() {
                    return 1;
                }
            });
        }
        for (int r = 0; r < 3; r++) for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c + r * 9 + 9, 8 + c * 18, 84 + r * 18));
        for (int c = 0; c < 9; c++) addSlot(new Slot(inv, c, 8 + c * 18, 142));
        addDataSlots(levels);
    }

    /** The level button {@code i} offers (0 when the mirror cannot enchant). */
    public int level(int i) {
        return levels.get(i);
    }

    @Override
    public boolean clickMenuButton(Player p, int id) {
        if (id < 0 || id > 2 || hand == null) return false;
        int level = levels.get(id);
        ItemStack stack = storage.getItem(WornJewelry.ENCHANT_SLOT);
        if (level <= 0 || stack.isEmpty() || (p.experienceLevel < level && !p.isCreative())) return false;
        var reg = p.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        String item = Stacks.id(stack.getItem());
        JewelryMaterial pearl = JewelryMaterial.byItem(item);
        boolean book = stack.is(Items.BOOK);
        List<MirrorEnchanting.Candidate> candidates = new ArrayList<>();
        int enchantability = pearl != null ? MirrorEnchanting.PEARL_ENCHANTABILITY : book ? 1 : stack.getEnchantmentValue();
        if (pearl == null) {
            for (Holder<Enchantment> h : reg.get(EnchantmentTags.IN_ENCHANTING_TABLE).map(t -> t.stream().toList()).orElse(List.of())) {
                if (!book && !h.value().isPrimaryItem(stack)) continue;
                Enchantment e = h.value();
                String eid = h.getRegisteredName();
                candidates.add(new MirrorEnchanting.Candidate(eid, e.getWeight(), e.getMinLevel(), e.getMaxLevel(), e::getMinCost, e::getMaxCost,
                        false, eid.equals("minecraft:protection") || eid.equals("minecraft:fortune")));
            }
        }
        List<Jewelry.Enchant> rolled = MirrorEnchanting.enchant(Rngs.of(p.level().getRandom()), enchantability, level, candidates,
                (a, b) -> compatible(p, a, b), pearl == null ? null : pearl.pearl, book, Jewelry::enchantMaxLevel);
        if (rolled.isEmpty()) return false;
        if (!p.level().isClientSide()) {
            p.giveExperienceLevels(-level);
            ItemStack out = book ? new ItemStack(Items.ENCHANTED_BOOK) : stack;
            Map<String, Integer> all = new java.util.LinkedHashMap<>(Enchants.read(out));
            for (Jewelry.Enchant e : rolled) all.merge(e.id(), e.level(), Math::max);
            if (book) {
                ItemEnchantments.Mutable m = new ItemEnchantments.Mutable(ItemEnchantments.EMPTY);
                all.forEach((eid, lv) -> Enchants.holder(p.level().registryAccess(), eid).ifPresent(h -> m.set(h, lv)));
                out.set(DataComponents.STORED_ENCHANTMENTS, m.toImmutable());
            } else {
                Enchants.apply(out, all, p.level().registryAccess());
            }
            storage.setItem(WornJewelry.ENCHANT_SLOT, out);
        }
        return true;
    }

    private static boolean compatible(Player p, String a, String b) {
        if (!MirrorEnchanting.jewelryCompatible(a, b)) return false;
        var ha = Enchants.holder(p.level().registryAccess(), a);
        var hb = Enchants.holder(p.level().registryAccess(), b);
        return ha.isEmpty() || hb.isEmpty() || Enchantment.areCompatible(ha.get(), hb.get());
    }

    @Override
    public void removed(Player p) {
        super.removed(p);
        if (!p.level().isClientSide()) {
            ItemStack left = storage.removeItemNoUpdate(WornJewelry.ENCHANT_SLOT);
            if (!left.isEmpty()) p.getInventory().placeItemBackInInventory(left);
        }
    }

    @Override
    public boolean stillValid(Player p) {
        return hand == null || p.getItemInHand(hand).getItem() instanceof MirrorItem;
    }

    /** ContainerMirror.transferStackInSlot: jewelry to its slot, enchantable items one at a time to the enchanting slot. */
    @Override
    public ItemStack quickMoveStack(Player p, int index) {
        Slot slot = getSlot(index);
        if (slot == null || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack stack = slot.getItem();
        ItemStack copy = stack.copy();
        int size = WornJewelry.SLOTS;
        if (index < size) {
            if (!moveItemStackTo(stack, size, size + 36, true)) return ItemStack.EMPTY;
        } else {
            JewelryType t = JewelryType.byItem(Stacks.id(stack.getItem()));
            int target = t != null ? t.slot : WornJewelry.ENCHANT_SLOT;
            if (!moveItemStackTo(stack, target, target + 1, false)) return ItemStack.EMPTY;
        }
        if (stack.isEmpty()) slot.setByPlayer(ItemStack.EMPTY);
        else slot.setChanged();
        return copy;
    }

    /** Whether the enchanting slot's item can take one of the buttons now (client display). */
    public boolean canEnchant(int i) {
        return level(i) > 0 && !storage.getItem(WornJewelry.ENCHANT_SLOT).isEmpty()
                && (player.experienceLevel >= level(i) || player.isCreative());
    }
}
