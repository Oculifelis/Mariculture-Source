package mariculture.platform.factory;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.client.model.data.ModelData;
import net.neoforged.neoforge.client.model.data.ModelProperty;
import net.neoforged.neoforge.energy.IEnergyStorage;

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
        ResourceLocation key = ResourceLocation.tryParse(id);
        if (key == null || id.equals(CustomFaces.AIR) || !BuiltInRegistries.BLOCK.containsKey(key)) return null;
        return BuiltInRegistries.BLOCK.get(key);
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
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        faces = CustomFaces.fromData(Stacks.toMap(tag.getCompound("Faces")));
        battery.set(tag.getInt("Energy"));
        if (level != null && level.isClientSide) {
            requestModelDataUpdate();
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_IMMEDIATE);
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.put("Faces", Stacks.toTag(faces.toData()));
        if (battery.stored() > 0) tag.putInt("Energy", battery.stored());
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
    public IEnergyStorage energy(Direction side) {
        return new Energy(side);
    }

    /** TileCustomPowered.receiveEnergy: what comes in goes on to another neighbour that takes energy, if there is one. */
    private void pass(Direction from) {
        if (level == null || passing) return;
        passing = true;
        try {
            List<Direction> sides = new ArrayList<>(List.of(Direction.values()));
            java.util.Collections.shuffle(sides, new java.util.Random(level.getRandom().nextLong()));
            for (Direction d : sides) {
                if (d == from || battery.stored() <= 0) continue;
                IEnergyStorage h = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(d), d.getOpposite());
                if (h == null || !h.canReceive()) continue;
                int moved = h.receiveEnergy(battery.stored(), false);
                if (moved > 0) {
                    battery.extract(moved, false);
                    setChanged();
                    return;
                }
            }
        } finally {
            passing = false;
        }
    }

    private final class Energy implements IEnergyStorage {
        private final Direction side;

        Energy(Direction side) {
            this.side = side;
        }

        @Override
        public int receiveEnergy(int max, boolean simulate) {
            int in = battery.receive(max, simulate);
            if (in > 0 && !simulate) {
                setChanged();
                pass(side);
            }
            return in;
        }

        @Override
        public int extractEnergy(int max, boolean simulate) {
            int out = battery.extract(max, simulate);
            if (out > 0 && !simulate) setChanged();
            return out;
        }

        @Override
        public int getEnergyStored() {
            return battery.stored();
        }

        @Override
        public int getMaxEnergyStored() {
            return battery.capacity();
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    }
}
