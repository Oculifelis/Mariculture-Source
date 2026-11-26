package mariculture.domain.view;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import mariculture.domain.fishery.FishFeeder;
import mariculture.domain.machine.FakeWorld;
import mariculture.domain.machine.ListInventory;
import mariculture.domain.metal.Crucible;
import mariculture.domain.metal.FluidTankBlock;

/** Jade tooltip lines come from the machines themselves. */
class MachineDescribeTest {
    private static List<String> keys(mariculture.domain.machine.Machine m) {
        List<RecipeViews.Line> lines = new ArrayList<>();
        m.describe(lines);
        List<String> out = new ArrayList<>();
        for (RecipeViews.Line l : lines) out.add(l.key());
        return out;
    }

    @Test
    void tanksShowTheirFluidAndTheCrucibleItsTemperature() {
        var tank = FluidTankBlock.COPPER.create();
        new ListInventory(tank);
        tank.tanks().get(0).fill("minecraft:water", 1000, false);
        assertTrue(keys(tank).contains("jade.mariculture.tank"));
        Crucible c = new Crucible();
        new ListInventory(c);
        assertTrue(keys(c).contains("jade.mariculture.temperature"));
    }

    @Test
    void anEmptyFeederSaysItNeedsFish() {
        FishFeeder f = new FishFeeder();
        new ListInventory(f);
        new FakeWorld(1).tick(f);
        List<String> k = keys(f);
        assertTrue(k.contains("jade.mariculture.feeder.problem.0") && k.contains("jade.mariculture.feeder.problem.1"), k.toString());
    }
}
