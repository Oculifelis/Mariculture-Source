package mariculture.platform.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.transfer.access.ItemAccess;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.item.ItemResource;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import mariculture.domain.machine.Stack;
import mariculture.domain.maritech.EnergyItem;
import mariculture.platform.Stacks;

/** Copper, titanium and creative batteries (domain EnergyItem): a charge bar, the charge in the tooltip, FE for other mods. */
public class BatteryItem extends Item {
    public BatteryItem(Properties props) {
        super(props);
    }

    @Override
    public boolean isBarVisible(ItemStack stack) {
        return true;
    }

    @Override
    public int getBarWidth(ItemStack stack) {
        return EnergyItem.bar(Stacks.toDomain(stack));
    }

    @Override
    public int getBarColor(ItemStack stack) {
        return 0xFF3030;
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, net.minecraft.world.item.component.TooltipDisplay display,
                                java.util.function.Consumer<Component> lines, TooltipFlag flag) {
        Stack s = Stacks.toDomain(stack);
        EnergyItem.Spec spec = EnergyItem.spec(s);
        if (spec == null) return;
        if (spec.infinite()) {
            lines.accept(Component.translatable("item.mariculture.battery.infinite").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.accept(Component.translatable("item.mariculture.battery.charge", EnergyItem.stored(s), spec.capacity()).withStyle(ChatFormatting.GRAY));
        lines.accept(Component.translatable("item.mariculture.battery.transfer", spec.maxExtract()).withStyle(ChatFormatting.GRAY));
    }

    /** The item's FE through the 26.1 transfer API; the charge is the item data's "Energy". */
    public static EnergyHandler handler(ItemAccess access) {
        return new EnergyHandler() {
            private Stack stack() {
                return access.getAmount() == 1 ? Stacks.toDomain(access.getResource().toStack()) : Stack.EMPTY;
            }

            @Override
            public long getAmountAsLong() {
                return EnergyItem.stored(stack());
            }

            @Override
            public long getCapacityAsLong() {
                EnergyItem.Spec spec = EnergyItem.spec(stack());
                return spec == null ? 0 : spec.capacity();
            }

            @Override
            public int insert(int amount, TransactionContext transaction) {
                EnergyItem.Transfer t = EnergyItem.receive(stack(), amount, false);
                return t.amount() > 0 && access.exchange(ItemResource.of(Stacks.toLoader(t.stack())), 1, transaction) == 1 ? t.amount() : 0;
            }

            @Override
            public int extract(int amount, TransactionContext transaction) {
                Stack before = stack();
                EnergyItem.Transfer t = EnergyItem.extract(before, amount, false);
                if (t.amount() <= 0) return 0;
                if (t.stack().equals(before)) return t.amount();
                return access.exchange(ItemResource.of(Stacks.toLoader(t.stack())), 1, transaction) == 1 ? t.amount() : 0;
            }
        };
    }
}
