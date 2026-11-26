package mariculture.domain.machine;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import mariculture.domain.fishery.FishFeeder;
import mariculture.domain.fishery.FishSorter;
import mariculture.domain.fishery.FishTank;
import mariculture.domain.fishery.Hatchery;
import mariculture.domain.fishery.Sifter;

/** Every machine type, by block id. Each has a catalog block of shape MACHINE or MACHINE_GLASS. */
public final class Machines {
    private static final Map<String, MachineType> BY_ID = new LinkedHashMap<>();

    static {
        for (MachineType t : List.of(FishFeeder.TYPE, Hatchery.TYPE, FishTank.TYPE, Sifter.TYPE, FishSorter.TYPE,
                mariculture.domain.metal.Crucible.TYPE, mariculture.domain.metal.Vat.TYPE, mariculture.domain.metal.Caster.INGOT,
                mariculture.domain.metal.Caster.NUGGET, mariculture.domain.metal.Caster.BLOCK, mariculture.domain.metal.FluidTankBlock.COPPER,
                mariculture.domain.metal.FluidTankBlock.ALUMINUM, mariculture.domain.metal.FluidTankBlock.TITANIUM,
                mariculture.domain.metal.Anvil.TYPE, mariculture.domain.metal.Autohammer.TYPE, mariculture.domain.metal.VoidBottle.TYPE, mariculture.domain.metal.Bookshelf.TYPE,
                mariculture.domain.diving.AirPump.TYPE, mariculture.domain.factory.Geyser.TYPE, mariculture.domain.factory.Unpacker.TYPE,
                mariculture.domain.factory.DictionaryConverter.TYPE, mariculture.domain.diving.AirCompressor.TYPE,
                mariculture.domain.maritech.Generator.TYPE, mariculture.domain.maritech.Rotor.COPPER_TYPE, mariculture.domain.maritech.Rotor.ALUMINUM_TYPE,
                mariculture.domain.maritech.Rotor.TITANIUM_TYPE, mariculture.domain.maritech.Sluice.TYPE, mariculture.domain.maritech.Sluice.ADVANCED,
                mariculture.domain.maritech.GasTurbine.TYPE, mariculture.domain.maritech.Incubator.TYPE, mariculture.domain.maritech.Autofisher.TYPE,
                mariculture.domain.maritech.Sponge.TYPE, mariculture.domain.maritech.DnaExtractor.TYPE, mariculture.domain.maritech.DnaInjector.TYPE,
                mariculture.domain.maritech.PressureVessel.TYPE, mariculture.domain.maritech.FluddStand.TYPE,
                mariculture.domain.factory.Sawmill.TYPE, mariculture.domain.factory.FluidDictionary.TYPE)) {
            if (BY_ID.put(t.id(), t) != null) throw new IllegalStateException("Duplicate machine " + t.id());
        }
    }

    private Machines() {}

    public static MachineType byId(String id) {
        MachineType t = BY_ID.get(id);
        if (t == null) throw new IllegalArgumentException("No machine type " + id);
        return t;
    }

    public static List<MachineType> all() {
        return List.copyOf(BY_ID.values());
    }
}
