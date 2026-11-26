package mariculture.platform.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import mariculture.platform.ModContent;

/** A dropped item that lava and fire cannot destroy: catches from a lava hook (fishery/EntityItemFireImmune.java, FISH-033). */
public class FireImmuneItem extends ItemEntity {
    public FireImmuneItem(EntityType<? extends FireImmuneItem> type, Level level) {
        super(type, level);
    }

    public FireImmuneItem(Level level, double x, double y, double z, ItemStack stack) {
        this(ModContent.fireImmuneItemType(), level);
        setPos(x, y, z);
        setItem(stack);
    }

    @Override
    public boolean fireImmune() {
        return true;
    }
}
