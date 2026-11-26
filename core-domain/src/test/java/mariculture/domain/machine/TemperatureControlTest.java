package mariculture.domain.machine;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.junit.jupiter.api.Test;

class TemperatureControlTest {
    @Test
    void holdsFiveTemperatureUpgradesAndSumsThem() {
        Stack c = Stack.of(TemperatureControl.ITEM);
        assertNull(TemperatureControl.add(c, Stack.of("mariculture:basic_storage_upgrade")), "only heating/cooling");
        assertNull(TemperatureControl.add(c, Stack.of(TemperatureControl.ITEM)));
        for (int i = 0; i < 4; i++) c = TemperatureControl.add(c, Stack.of("mariculture:ultimate_heating_upgrade"));
        c = TemperatureControl.add(c, Stack.of("mariculture:basic_cooling_upgrade"));
        assertEquals(4 * 14 - 1, TemperatureControl.temperature(c));
        assertNull(TemperatureControl.add(c, Stack.of("mariculture:basic_heating_upgrade")), "full at five");
        assertEquals("mariculture:basic_cooling_upgrade", TemperatureControl.last(c));
        assertEquals(56, TemperatureControl.temperature(TemperatureControl.withoutLast(c)));
    }

    @Test
    void countsInAMachinesUpgradeSlots() {
        Stack c = TemperatureControl.add(Stack.of(TemperatureControl.ITEM), Stack.of("mariculture:advanced_heating_upgrade"));
        assertTrue(Upgrades.isUpgrade(c));
        Upgrades.Totals t = Upgrades.totals(List.of(c, Stack.of("mariculture:basic_heating_upgrade")));
        assertEquals(6, t.temperature());
        assertTrue(t.has("control"));
    }
}
