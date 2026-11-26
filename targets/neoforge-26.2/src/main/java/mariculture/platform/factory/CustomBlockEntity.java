package mariculture.platform.factory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.mojang.serialization.Codec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.model.data.ModelData;
import net.neoforged.neoforge.model.data.ModelProperty;
import net.neoforged.neoforge.transfer.energy.EnergyHandler;
import net.neoforged.neoforge.transfer.transaction.SnapshotJournal;
import net.neoforged.neoforge.transfer.transaction.TransactionContext;

import mariculture.domain.factory.CustomBlocks;
import mariculture.domain.factory.CustomFaces;
import mariculture.domain.machine.Battery;
import mariculture.platform.ModContent;
import mariculture.platform.Stacks;

/**
 * A sawmill block's faces (TileCustom), sent to the client for its model, and for the two energy blocks their FE
 * (TileCustomPowered). Rules live in mariculture.domain.factory.
 */
public class CustomBlockEntity extends BlockEntity {
    public static final ModelProperty<CustomFaces> FACES = new ModelProperty<>();

    private CustomFaces faces = CustomFaces.DEFAULT;
    private final Battery battery = new Battery(CustomBlocks.RF_CAPACITY);
    private final Journal journal = new Journal();
    private boolean passing;

    public CustomBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.customEntity(), pos, state);
    }

    public CustomFaces faces() {
        return faces;
    }

    public void setFaces(CustomFaces faces) {
        this.faces = faces;
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_ALL);
    }

    /** This block as an item that keeps its faces. */
    public ItemStack asItem(int count) {
        ItemStack stack = new ItemStack(getBlockState().getBlock(), count);
        Stacks.setData(stack, faces.toData());
        return stack;
    }

    /** The block a face shows, or null for air. */
    public static Block faceBlock(String id) {
        Identifier key = Identifier.tryParse(id);
        if (key == null || id.equals(CustomFaces.AIR) || !BuiltInRegistries.BLOCK.containsKey(key)) return null;
        return BuiltInRegistries.BLOCK.getValue(key);
    }

    /** The average of the six faces' hardness (or blast resistance), TileCustom.updateHardness. */
    public float average(boolean resistance) {
        float[] v = new float[6];
        for (int i = 0; i < 6; i++) {
            Block b = faceBlock(faces.blocks().get(i));
            v[i] = b == null ? 0 : resistance ? b.getExplosionResistance() : b.defaultDestroyTime();
        }
        return CustomFaces.average(v);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        faces = CustomFaces.fromData(input.read("Faces", CompoundTag.CODEC).map(Stacks::toMap).orElse(Map.of()));
        battery.set(input.read("Energy", Codec.INT).orElse(0));
        if (level != null && level.isClientSide()) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.store("Faces", CompoundTag.CODEC, Stacks.toTag(faces.toData()));
        if (battery.stored() > 0) output.store("Energy", Codec.INT, battery.stored());
    }

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        return saveCustomOnly(registries);
    }

    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public ModelData getModelData() {
        return ModelData.of(FACES, faces);
    }

    /** FE for the energy blocks, seen from {@code side}. */
    public EnergyHandler energy(Direction side) {
        return new Energy(side);
    }

    /** TileCustomPowered.receiveEnergy: what comes in goes on to another neighbour that takes energy, if there is one. */
    private void pass(Direction from, TransactionContext tx) {
        if (level == null || passing) return;
        passing = true;
        try {
            List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
            java.util.Collections.shuffle(sides, new java.util.Random(level.getRandom().nextLong()));
            for (Direction d : sides) {
                if (d == from || battery.stored() <= 0) continue;
                EnergyHandler h = level.getCapability(Capabilities.Energy.BLOCK, worldPosition.relative(d), d.getOpposite());
                if (h == null) continue;
                int moved = h.insert(battery.stored(), tx);
                if (moved > 0) {
                    journal.updateSnapshots(tx);
                    battery.extract(moved, false);
                    return;
                }
            }
        } finally {
            passing = false;
        }
    }

    private final class Energy implements EnergyHandler {
        private final Direction side;

        Energy(Direction side) {
            this.side = side;
        }

        @Override
        public long getAmountAsLong() {
            return battery.stored();
        }

        @Override
        public long getCapacityAsLong() {
            return battery.capacity();
        }

        @Override
        public int insert(int amount, TransactionContext tx) {
            int in = battery.receive(amount, true);
            if (in <= 0) return 0;
            journal.updateSnapshots(tx);
            battery.receive(in, false);
            pass(side, tx);
            return in;
        }

        @Override
        public int extract(int amount, TransactionContext tx) {
            int out = battery.extract(amount, true);
            if (out <= 0) return 0;
            journal.updateSnapshots(tx);
            return battery.extract(out, false);
        }
    }

    private final class Journal extends SnapshotJournal<Integer> {
        @Override
        protected Integer createSnapshot() {
            return battery.stored();
        }

        @Override
        protected void revertToSnapshot(Integer snapshot) {
            battery.set(snapshot);
        }

        @Override
        protected void onRootCommit(Integer original) {
            setChanged();
        }
    }
}
