package mariculture.domain.machine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class MachineBasicsTest {
    @Test
    void upgradeTotalsStartSpeedAtOne() {
        Upgrades.Totals t = Upgrades.totals(List.of(Stack.of("mariculture:advanced_speed_upgrade"), Stack.of("mariculture:basic_heating_upgrade"),
                Stack.of("mariculture:eternal_male_upgrade")));
        assertEquals(5, t.speed());
        assertEquals(1, t.temperature());
        assertTrue(t.has("male"));
        assertEquals(1, Upgrades.Totals.NONE.speed());
    }

    @Test
    void tankFillDrainAndCapacityClamp() {
        Tank t = new Tank(1000, f -> !f.equals("minecraft:lava"));
        assertEquals(0, t.fill("minecraft:lava", 500, false));
        assertEquals(1000, t.fill("minecraft:water", 1500, false));
        assertEquals(0, t.fill("mariculture:fish_oil", 10, false), "one fluid at a time");
        t.setCapacity(400);
        assertEquals(400, t.amount());
        assertEquals(400, t.drain("minecraft:water", 1000, false));
        assertNull(t.fluid());
    }

    @Test
    void redstoneAndEjectSettings() {
        assertTrue(RedstoneMode.LOW.canWork(false));
        assertFalse(RedstoneMode.LOW.canWork(true));
        assertEquals(RedstoneMode.DISABLED, RedstoneMode.HIGH.next());
        assertEquals(EjectSetting.ITEM, EjectSetting.NONE.toggle(EjectSetting.ITEM));
        assertEquals(EjectSetting.NONE, EjectSetting.ITEM.toggle(EjectSetting.ITEM));
        assertEquals(EjectSetting.ITEM, EjectSetting.NONE.toggle(EjectSetting.ITEM_AND_FLUID));
    }
}
