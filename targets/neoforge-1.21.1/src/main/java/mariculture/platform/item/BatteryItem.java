package mariculture.platform.item;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.neoforged.neoforge.energy.IEnergyStorage;

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
    public void appendHoverText(ItemStack stack, TooltipContext context, java.util.List<Component> lines, TooltipFlag flag) {
        Stack s = Stacks.toDomain(stack);
        EnergyItem.Spec spec = EnergyItem.spec(s);
        if (spec == null) return;
        if (spec.infinite()) {
            lines.add(Component.translatable("item.mariculture.battery.infinite").withStyle(ChatFormatting.GRAY));
            return;
        }
        lines.add(Component.translatable("item.mariculture.battery.charge", EnergyItem.stored(s), spec.capacity()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("item.mariculture.battery.transfer", spec.maxExtract()).withStyle(ChatFormatting.GRAY));
    }

    /** The item's FE (IEnergyStorage); the charge is the item data's "Energy". */
    public static IEnergyStorage storage(ItemStack stack) {
        return new IEnergyStorage() {
            @Override
            public int receiveEnergy(int max, boolean simulate) {
                EnergyItem.Transfer t = EnergyItem.receive(Stacks.toDomain(stack), max, simulate);
                if (!simulate && t.amount() > 0) Stacks.setData(stack, t.stack().data());
                return t.amount();
            }

            @Override
            public int extractEnergy(int max, boolean simulate) {
                Stack before = Stacks.toDomain(stack);
                EnergyItem.Transfer t = EnergyItem.extract(before, max, simulate);
                if (!simulate && t.amount() > 0 && !t.stack().equals(before)) Stacks.setData(stack, t.stack().data());
                return t.amount();
            }

            @Override
            public int getEnergyStored() {
                return EnergyItem.stored(Stacks.toDomain(stack));
            }

            @Override
            public int getMaxEnergyStored() {
                EnergyItem.Spec spec = EnergyItem.spec(Stacks.toDomain(stack));
                return spec == null ? 0 : spec.capacity();
            }

            @Override
            public boolean canExtract() {
                return true;
            }

            @Override
            public boolean canReceive() {
                EnergyItem.Spec spec = EnergyItem.spec(Stacks.toDomain(stack));
                return spec != null && !spec.infinite();
            }
        };
    }
}
