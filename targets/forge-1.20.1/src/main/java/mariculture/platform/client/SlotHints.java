package mariculture.platform.client;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.resources.ResourceLocation;

import mariculture.domain.machine.SlotInfo;
import mariculture.platform.Stacks;
import mariculture.platform.machine.MachineBlockEntity;

/**
 * Menu slot hints: the items an empty machine slot shows faintly (cycling once a second) and the tooltip naming
 * what the slot is for. Both come from the machine's {@link SlotInfo}, which reads the machine's own tables.
 */
final class SlotHints {
    /** Faint cover drawn over a ghost item: the slot colour, mostly opaque. */
    static final int COVER = 0xB08B8B8B;

    private final List<List<ItemStack>> ghosts = new ArrayList<>();

    SlotHints(java.util.function.IntFunction<SlotInfo> infos, int count) {
        for (int i = 0; i < count; i++) {
            List<ItemStack> out = new ArrayList<>();
            for (String id : infos.apply(i).ghosts()) {
                if (id.startsWith("#")) {
                    for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, new ResourceLocation(MachineBlockEntity.forgeTag(id.substring(1)))))) {
                        out.add(new ItemStack(h.value()));
                        if (out.size() >= 24) break;
                    }
                } else {
                    Item item = Stacks.item(id);
                    if (item != null) out.add(new ItemStack(item));
                }
            }
            ghosts.add(out);
        }
    }

    /** The faint item for an empty slot now, or empty. */
    ItemStack ghost(int menuIndex) {
        List<ItemStack> g = ghosts.get(menuIndex);
        return g.isEmpty() ? ItemStack.EMPTY : g.get((int) ((System.currentTimeMillis() / 1000) % g.size()));
    }

    static List<Component> tooltip(SlotInfo info) {
        return List.of(Component.translatable(info.key()), Component.translatable(info.role().key()).withStyle(ChatFormatting.GRAY));
    }
}
