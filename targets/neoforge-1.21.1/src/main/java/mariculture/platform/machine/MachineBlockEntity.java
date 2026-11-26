package mariculture.platform.machine;

import mariculture.domain.machine.FluidAmount;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.TagKey;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidUtil;
import net.neoforged.neoforge.fluids.capability.IFluidHandler;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;

import mariculture.domain.content.Face;
import mariculture.domain.fish.FishEffects;
import mariculture.domain.fish.Salinity;
import mariculture.domain.fishery.Droplets;
import mariculture.domain.machine.Machine;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.MachineWorld;
import mariculture.domain.machine.Stack;
import mariculture.domain.machine.Tank;
import mariculture.domain.random.Rng;
import mariculture.platform.Env;
import mariculture.platform.FishActions;
import mariculture.platform.ModContent;
import mariculture.platform.Rngs;
import mariculture.platform.Stacks;

/**
 * The one block entity behind every Mariculture machine: it owns the item slots and runs the domain
 * {@link Machine} of its block's {@link MachineType}. Hoppers see it as a sided container.
 */
public class MachineBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    private final MachineType type;
    private final Machine machine;
    private final NonNullList<ItemStack> items;
    private final World world = new World();
    private final TankHandler tanks;

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(ModContent.machineEntity(), pos, state);
        this.type = ((MachineBlock) state.getBlock()).type();
        this.machine = type.create();
        this.items = NonNullList.withSize(machine.slotCount(), ItemStack.EMPTY);
        machine.attach(new mariculture.domain.machine.Inventory() {
            @Override
            public int size() {
                return items.size();
            }

            @Override
            public Stack get(int slot) {
                return Stacks.toDomain(items.get(slot));
            }

            @Override
            public void set(int slot, Stack stack) {
                items.set(slot, Stacks.toLoader(stack));
                setChanged();
            }

            @Override
            public int maxStackSize(Stack stack) {
                ItemStack s = Stacks.toLoader(stack.withCount(1));
                return s.isEmpty() ? 64 : s.getMaxStackSize();
            }

            @Override
            public int slotLimit(int slot) {
                return machine.slotLimit(slot);
            }

            @Override
            public java.util.List<int[]> enchantments(int slot) {
                java.util.List<int[]> out = new java.util.ArrayList<>();
                for (var e : items.get(slot).getEnchantments().entrySet()) {
                    out.add(new int[] {e.getIntValue(), e.getKey().value().getWeight()});
                }
                return out;
            }

            @Override
            public boolean moveDisenchanted(int from, int to) {
                if (!items.get(to).isEmpty() || items.get(from).isEmpty()) return false;
                ItemStack out = items.get(from).copyWithCount(1);
                net.minecraft.world.item.enchantment.EnchantmentHelper.setEnchantments(out, net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                items.get(from).shrink(1);
                items.set(to, out);
                setChanged();
                return true;
            }

            @Override
            public void enchant(int slot, java.util.Map<String, Integer> enchantments) {
                if (level == null) return;
                mariculture.platform.magic.Enchants.apply(items.get(slot), enchantments, level.registryAccess());
                setChanged();
            }

            @Override
            public boolean hasLoaderData(int slot) {
                ItemStack s = items.get(slot);
                return !s.isEmpty() && !ItemStack.isSameItemSameComponents(s, Stacks.toLoader(Stacks.toDomain(s)));
            }

            @Override
            public int enchantLevel(int slot, String enchantment) {
                if (level == null) return 0;
                return level.registryAccess().registryOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
                        .getHolder(net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ENCHANTMENT, net.minecraft.resources.ResourceLocation.parse(enchantment)))
                        .map(h -> net.minecraft.world.item.enchantment.EnchantmentHelper.getItemEnchantmentLevel(h, items.get(slot))).orElse(0);
            }

            @Override
            public void setData(int slot, java.util.Map<String, Object> data) {
                if (items.get(slot).isEmpty()) return;
                mariculture.platform.Stacks.setData(items.get(slot), data);
                setChanged();
            }

            @Override
            public void damage(int slot, int amount) {
                if (!(level instanceof ServerLevel) || items.get(slot).isEmpty()) return;
                items.get(slot).hurtAndBreak(amount, (ServerLevel) level, (net.minecraft.server.level.ServerPlayer) null, i -> {});
                setChanged();
            }
        });
        this.tanks = new TankHandler(machine, null);
    }

    public Machine machine() {
        return machine;
    }

    public MachineType type() {
        return type;
    }

    /** Broken or unloaded: drop any spawn guard this machine set. */
    @Override
    public void setRemoved() {
        if (level != null && !level.isClientSide()) {
            mariculture.domain.fishery.SpawnGuard.set(level.dimension().location().toString(), worldPosition.asLong(), null);
        }
        super.setRemoved();
    }

    public static void serverTick(net.minecraft.world.level.Level level, BlockPos pos, BlockState state, MachineBlockEntity be) {
        be.machine.tick(be.world);
    }

    public MachineWorld world() {
        return world;
    }

    // ---- saving ------------------------------------------------------------------------

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        machine.load(tag.contains("Machine") ? Stacks.toMap(tag.getCompound("Machine")) : Map.of());
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        Map<String, Object> m = new java.util.LinkedHashMap<>();
        machine.save(m);
        tag.put("Machine", Stacks.toTag(m));
    }

    // ---- container ---------------------------------------------------------------------

    @Override
    public int getContainerSize() {
        return items.size();
    }

    @Override
    public boolean isEmpty() {
        return items.stream().allMatch(ItemStack::isEmpty);
    }

    @Override
    public ItemStack getItem(int slot) {
        return items.get(slot);
    }

    @Override
    public ItemStack removeItem(int slot, int amount) {
        ItemStack out = ContainerHelper.removeItem(items, slot, amount);
        if (!out.isEmpty()) changed(slot);
        return out;
    }

    @Override
    public ItemStack removeItemNoUpdate(int slot) {
        ItemStack out = ContainerHelper.takeItem(items, slot);
        changed(slot);
        return out;
    }

    @Override
    public void setItem(int slot, ItemStack stack) {
        items.set(slot, stack);
        changed(slot);
    }

    private void changed(int slot) {
        machine.slotChanged(slot);
        setChanged();
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return machine.slotLimit(slot) > 0 && machine.isValid(slot, Stacks.toDomain(stack));
    }

    @Override
    public boolean stillValid(Player player) {
        return Container.stillValidBlockEntity(this, player);
    }

    @Override
    public void clearContent() {
        items.clear();
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return machine.slots(face(side));
    }

    @Override
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) {
        if (machine.slotLimit(slot) <= items.get(slot).getCount()) return false;
        return machine.canInsert(slot, Stacks.toDomain(stack), side == null ? null : face(side));
    }

    @Override
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) {
        return machine.canExtract(slot, side == null ? null : face(side));
    }

    /** Ghost slots hold copies (fish sorter filters); empty them before the contents drop. */
    public void clearGhostSlots() {
        for (int i = 0; i < items.size(); i++) if (machine.isGhost(i)) items.set(i, ItemStack.EMPTY);
    }

    /** A tag's first item, Mariculture and vanilla items first; null when the tag is empty or unknown. */
    public static String firstInTag(String tag) {
        String first = null;
        for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag)))) {
            String id = BuiltInRegistries.ITEM.getKey(h.value()).toString();
            if (id.startsWith("mariculture:") || id.startsWith("minecraft:")) return id;
            if (first == null) first = id;
        }
        return first;
    }

    /** Tanks: the dropped item carries the machine's state. */
    public ItemStack keptItem() {
        ItemStack stack = new ItemStack(getBlockState().getBlock().asItem());
        java.util.Map<String, Object> m = new java.util.LinkedHashMap<>();
        machine.save(m);
        boolean empty = machine.tanks().stream().allMatch(mariculture.domain.machine.Tank::isEmpty);
        if (!empty) Stacks.setData(stack, java.util.Map.of("machine", m));
        return stack;
    }

    /** Restores a kept machine state from a placed item. */
    public void restoreFrom(ItemStack stack) {
        if (Stacks.data(stack).get("machine") instanceof java.util.Map<?, ?> m) {
            @SuppressWarnings("unchecked") java.util.Map<String, ?> data = (java.util.Map<String, ?>) m;
            machine.load(data);
            setChanged();
        }
    }

    public NonNullList<ItemStack> items() {
        return items;
    }

    public IFluidHandler fluidHandler(Direction side) {
        return machine.tanks().isEmpty() ? null : new TankHandler(machine, side == null ? null : face(side));
    }

    private BatteryHandler battery;

    /** FE access to the machine's battery, or null when it has none. */
    public BatteryHandler energyHandler(Direction side) {
        if (machine.battery() == null) return null;
        if (battery == null) battery = new BatteryHandler(machine);
        return battery;
    }

    public TankHandler tanks() {
        return tanks;
    }

    // ---- menu --------------------------------------------------------------------------

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new MachineMenu(id, inventory, this);
    }

    // ---- helpers -----------------------------------------------------------------------

    public static Face face(Direction d) {
        return switch (d) {
            case DOWN -> Face.DOWN;
            case UP -> Face.UP;
            case NORTH -> Face.NORTH;
            case SOUTH -> Face.SOUTH;
            case WEST -> Face.WEST;
            case EAST -> Face.EAST;
        };
    }

    public static Direction direction(Face f) {
        return switch (f) {
            case DOWN -> Direction.DOWN;
            case UP -> Direction.UP;
            case NORTH -> Direction.NORTH;
            case SOUTH -> Direction.SOUTH;
            case WEST -> Direction.WEST;
            case EAST -> Direction.EAST;
        };
    }

    public static Fluid fluid(String id) {
        if (id == null) return null;
        ResourceLocation rl = ResourceLocation.tryParse(id);
        return rl == null || !BuiltInRegistries.FLUID.containsKey(rl) ? null : BuiltInRegistries.FLUID.get(rl);
    }

    public static String fluidId(Fluid f) {
        return BuiltInRegistries.FLUID.getKey(f).toString();
    }

    /** The world as the domain machine sees it. */
    private final class World implements MachineWorld {
        private ServerLevel level() {
            return (ServerLevel) MachineBlockEntity.this.level;
        }

        private BlockPos at(int dx, int dy, int dz) {
            return worldPosition.offset(dx, dy, dz);
        }

        @Override
        public Rng random() {
            return Rngs.of(level().random);
        }

        @Override
        public long gameTime() {
            return level().getGameTime();
        }

        @Override
        public boolean isDay() {
            return level().isDay();
        }

        @Override
        public boolean isRaining() {
            return level().isRaining();
        }

        @Override
        public boolean poweredByRedstone() {
            return level().hasNeighborSignal(worldPosition);
        }

        @Override
        public String dimension() {
            return level().dimension().location().toString();
        }

        @Override
        public void guardSpawns(int[] box) {
            int[] abs = null;
            if (box != null) {
                BlockPos p = worldPosition;
                abs = new int[] {p.getX() + box[0], p.getY() + box[1], p.getZ() + box[2], p.getX() + box[3], p.getY() + box[4], p.getZ() + box[5]};
            }
            mariculture.domain.fishery.SpawnGuard.set(dimension(), worldPosition.asLong(), abs);
        }

        @Override
        public boolean isNether() {
            return Env.dimensionKind(level()).equals("nether");
        }

        @Override
        public boolean isEnd() {
            return Env.dimensionKind(level()).equals("end");
        }

        @Override
        public int y() {
            return worldPosition.getY();
        }

        @Override
        public int biomeTemperature() {
            return Env.biome(level(), worldPosition).temperature();
        }

        @Override
        public Salinity salinity() {
            return Env.salinity(level(), worldPosition);
        }

        @Override
        public int temperature() {
            return Env.temperature(level(), worldPosition);
        }

        @Override
        public String block(int dx, int dy, int dz) {
            return BuiltInRegistries.BLOCK.getKey(level().getBlockState(at(dx, dy, dz)).getBlock()).toString();
        }

        @Override
        public boolean blockHasTag(int dx, int dy, int dz, String tag) {
            return level().getBlockState(at(dx, dy, dz)).is(TagKey.create(Registries.BLOCK, ResourceLocation.parse(tag)));
        }

        @Override
        public String fluid(int dx, int dy, int dz) {
            BlockPos p = at(dx, dy, dz);
            if (!level().isLoaded(p)) return "";
            FluidState fs = level().getFluidState(p);
            return fs.isEmpty() ? "" : fluidId(fs.getType() instanceof net.minecraft.world.level.material.FlowingFluid ff ? ff.getSource() : fs.getType());
        }

        @Override
        public boolean isFluidSource(int dx, int dy, int dz) {
            return level().getFluidState(at(dx, dy, dz)).isSource();
        }

        @Override
        public boolean isAir(int dx, int dy, int dz) {
            return level().isEmptyBlock(at(dx, dy, dz));
        }

        @Override
        public void setBlock(int dx, int dy, int dz, String block) {
            ResourceLocation rl = ResourceLocation.parse(block);
            level().setBlockAndUpdate(at(dx, dy, dz), BuiltInRegistries.BLOCK.get(rl).defaultBlockState());
        }

        @Override
        public boolean researchDone(String ownerUuid, String entry) {
            return mariculture.platform.compat.thaumaturge.ThaumaturgeBridge.researchPastFirstStage(level(), ownerUuid, entry);
        }

        @Override
        public float addAuraVis(float amount) {
            return mariculture.platform.compat.thaumaturge.ThaumaturgeBridge.addVis(level(), worldPosition, amount);
        }

        @Override
        public float drainAuraFlux(float amount) {
            return mariculture.platform.compat.thaumaturge.ThaumaturgeBridge.drainFlux(level(), worldPosition, amount);
        }

        @Override
        public Machine machine(int dx, int dy, int dz) {
            return level().getBlockEntity(at(dx, dy, dz)) instanceof MachineBlockEntity be ? be.machine : null;
        }

        @Override
        public boolean itemHasTag(String item, String tag) {
            var i = Stacks.item(item);
            return i != null && new ItemStack(i).is(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag)));
        }

        @Override
        public void drop(Stack stack) {
            ItemStack s = Stacks.toLoader(stack);
            if (s.isEmpty()) return;
            ItemEntity e = new ItemEntity(level(), worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, s);
            level().addFreshEntity(e);
        }

        @Override
        public Stack ejectToNeighbours(Stack stack) {
            return Stacks.toDomain(ejectLoader(Stacks.toLoader(stack)));
        }

        private ItemStack ejectLoader(ItemStack s) {
            for (Face f : shuffledFaces()) {
                if (s.isEmpty()) break;
                Direction d = direction(f);
                BlockPos p = worldPosition.relative(d);
                if (level().getBlockEntity(p) instanceof MachineBlockEntity) continue;
                IItemHandler h = level().getCapability(Capabilities.ItemHandler.BLOCK, p, d.getOpposite());
                if (h != null) s = ItemHandlerHelper.insertItemStacked(h, s, false);
            }
            return s;
        }

        @Override
        public mariculture.domain.fishing.FishingRules.Cast castBelow(mariculture.domain.fish.Species.Rod rod, int luck) {
            BlockPos below = worldPosition.below();
            return new mariculture.domain.fishing.FishingRules.Cast(rod, luck, mariculture.platform.Env.dimensionKind(level()), below.getY(),
                    level().isDay(), mariculture.platform.Env.salinity(level(), below),
                    mariculture.platform.Env.temperature(level(), below), null, 0);
        }

        @Override
        public boolean deliverCatch(mariculture.domain.fishing.FishingRules.Catch c, int rodSlot, int[] outputs, boolean eject) {
            ItemStack out = mariculture.platform.event.FisheryEvents.resolveCatch(c, (ServerLevel) level(),
                    net.minecraft.world.phys.Vec3.atCenterOf(worldPosition.below()), items.get(rodSlot), null);
            if (out.isEmpty()) return false;
            if (eject) out = ejectLoader(out);
            for (int slot : outputs) {
                if (out.isEmpty()) break;
                ItemStack in = items.get(slot);
                if (in.isEmpty()) {
                    items.set(slot, out);
                    out = ItemStack.EMPTY;
                } else if (ItemStack.isSameItemSameComponents(in, out)) {
                    int add = Math.min(out.getCount(), in.getMaxStackSize() - in.getCount());
                    in.grow(add);
                    out.shrink(add);
                }
            }
            if (!out.isEmpty()) {
                level().addFreshEntity(new ItemEntity(level(), worldPosition.getX() + 0.5, worldPosition.getY() + 1, worldPosition.getZ() + 0.5, out));
            }
            setChanged();
            return true;
        }

        @Override
        public Stack ejectTo(Face face, Stack stack) {
            ItemStack s = Stacks.toLoader(stack);
            Direction d = direction(face);
            BlockPos p = worldPosition.relative(d);
            IItemHandler h = level().getCapability(Capabilities.ItemHandler.BLOCK, p, d.getOpposite());
            if (h != null) s = ItemHandlerHelper.insertItemStacked(h, s, false);
            return Stacks.toDomain(s);
        }

        @Override
        public void dropAt(Face face, Stack stack) {
            ItemStack s = Stacks.toLoader(stack);
            if (s.isEmpty()) return;
            BlockPos p = worldPosition.relative(direction(face));
            level().addFreshEntity(new ItemEntity(level(), p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, s));
        }

        @Override
        public boolean tagHasItems(String tag) {
            return MachineBlockEntity.firstInTag(tag) != null;
        }

        @Override
        public String firstItemInTag(String tag) {
            return MachineBlockEntity.firstInTag(tag);
        }

        @Override
        public java.util.Set<String> itemTags(String item) {
            java.util.Set<String> out = new java.util.LinkedHashSet<>();
            BuiltInRegistries.ITEM.get(ResourceLocation.parse(item)).builtInRegistryHolder().tags().forEach(t -> out.add(t.location().toString()));
            return out;
        }

        @Override
        public List<String> itemsInTag(String tag) {
            List<String> out = new ArrayList<>();
            for (var h : BuiltInRegistries.ITEM.getTagOrEmpty(TagKey.create(Registries.ITEM, ResourceLocation.parse(tag)))) out.add(BuiltInRegistries.ITEM.getKey(h.value()).toString());
            return out;
        }

        @Override
        public Face facing() {
            BlockState st = getBlockState();
            if (st.hasProperty(DirectionalMachineBlock.FACING_ALL)) return face(st.getValue(DirectionalMachineBlock.FACING_ALL));
            if (st.hasProperty(MachineBlock.FACING)) return face(st.getValue(MachineBlock.FACING));
            return Face.NORTH;
        }

        @Override
        public boolean passable(int dx, int dy, int dz) {
            BlockPos p = at(dx, dy, dz);
            return level().getBlockState(p).getCollisionShape(level(), p).isEmpty();
        }

        @Override
        public void moveEntities(List<int[]> offsets, java.util.function.UnaryOperator<EntityMotion> rule) {
            for (int[] o : offsets) {
                for (net.minecraft.world.entity.Entity e : level().getEntities((net.minecraft.world.entity.Entity) null, new AABB(at(o[0], o[1], o[2])))) {
                    e.resetFallDistance();
                    Vec3 v = e.getDeltaMovement();
                    EntityMotion m = rule.apply(new EntityMotion(v.x, v.y, v.z, e instanceof ItemEntity));
                    if (m == null) continue;
                    e.setDeltaMovement(m.x(), m.y(), m.z());
                    e.hurtMarked = true;
                }
            }
        }

        @Override
        public void particle(String type, double dx, double dy, double dz) {
            level().sendParticles(net.minecraft.core.particles.ParticleTypes.CLOUD, worldPosition.getX() + dx, worldPosition.getY() + dy,
                    worldPosition.getZ() + dz, 1, 0, 0, 0, 0);
        }

        @Override
        public Stack uncraft(Stack one) {
            net.minecraft.world.item.crafting.CraftingInput input = net.minecraft.world.item.crafting.CraftingInput.of(1, 1, List.of(Stacks.toLoader(one)));
            return level().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.CRAFTING, input, level())
                    .map(r -> Stacks.toDomain(r.value().assemble(input, level().registryAccess()))).orElse(Stack.EMPTY);
        }

        @Override
        public void refillAir(double rx, double ry, double rz, String helmet, String top) {
            for (Player p : level().getEntitiesOfClass(Player.class, new AABB(worldPosition).inflate(rx, ry, rz))) {
                if (!worn(p, net.minecraft.world.entity.EquipmentSlot.HEAD, helmet) || !worn(p, net.minecraft.world.entity.EquipmentSlot.CHEST, top)) continue;
                if (p.isEyeInFluid(net.minecraft.tags.FluidTags.WATER)) p.setAirSupply(p.getMaxAirSupply());
            }
        }

        private boolean worn(Player p, net.minecraft.world.entity.EquipmentSlot slot, String item) {
            return BuiltInRegistries.ITEM.getKey(p.getItemBySlot(slot).getItem()).toString().equals(item);
        }

        @Override
        public Stack takeFrom(Face face, int max) {
            Direction d = direction(face);
            IItemHandler h = level().getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(d), d.getOpposite());
            if (h == null) return Stack.EMPTY;
            for (int i = 0; i < h.getSlots(); i++) {
                ItemStack got = h.extractItem(i, max, false);
                if (!got.isEmpty()) return Stacks.toDomain(got);
            }
            return Stack.EMPTY;
        }

        @Override
        public FluidAmount drainFrom(Face face, int max, boolean execute) {
            Direction d = direction(face);
            BlockPos p = worldPosition.relative(d);
            IFluidHandler h = level().getCapability(Capabilities.FluidHandler.BLOCK, p, d.getOpposite());
            if (h == null) return null;
            net.neoforged.neoforge.fluids.FluidStack out = h.drain(max, execute ? IFluidHandler.FluidAction.EXECUTE : IFluidHandler.FluidAction.SIMULATE);
            return out.isEmpty() ? null : new FluidAmount(fluidId(out.getFluid()), out.getAmount());
        }

        @Override
        public int insertEnergy(int dx, int dy, int dz, Face side, int amount) {
            net.neoforged.neoforge.energy.IEnergyStorage h = level().getCapability(Capabilities.EnergyStorage.BLOCK, at(dx, dy, dz), direction(side));
            if (h == null || amount <= 0 || !h.canReceive()) return 0;
            return h.receiveEnergy(amount, false);
        }

        @Override
        public String blockOfItem(String item) {
            net.minecraft.world.item.Item i = Stacks.item(item);
            return i instanceof net.minecraft.world.item.BlockItem bi ? BuiltInRegistries.BLOCK.getKey(bi.getBlock()).toString() : null;
        }

        @Override
        public boolean fluidExists(String fluid) {
            return MachineBlockEntity.fluid(fluid) != null;
        }

        @Override
        public int fillTo(Face face, String id, int amount, boolean execute) {
            Fluid f = MachineBlockEntity.fluid(id);
            Direction d = direction(face);
            net.neoforged.neoforge.fluids.capability.IFluidHandler h = level().getCapability(Capabilities.FluidHandler.BLOCK, worldPosition.relative(d), d.getOpposite());
            if (f == null || h == null || amount <= 0) return 0;
            return h.fill(new net.neoforged.neoforge.fluids.FluidStack(f, amount),
                    execute ? net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.EXECUTE : net.neoforged.neoforge.fluids.capability.IFluidHandler.FluidAction.SIMULATE);
        }

        @Override
        public Face facingAt(int dx, int dy, int dz) {
            BlockState st = level().getBlockState(at(dx, dy, dz));
            if (st.hasProperty(DirectionalMachineBlock.FACING_ALL)) return face(st.getValue(DirectionalMachineBlock.FACING_ALL));
            if (st.hasProperty(MachineBlock.FACING)) return face(st.getValue(MachineBlock.FACING));
            return Face.NORTH;
        }

        @Override
        public boolean placeFluid(int dx, int dy, int dz, String id) {
            Fluid f = MachineBlockEntity.fluid(id);
            if (f == null) return false;
            BlockState st = f.defaultFluidState().createLegacyBlock();
            if (st.isAir()) return false;
            return level().setBlockAndUpdate(at(dx, dy, dz), st);
        }

        @Override
        public boolean isSolidCube(int dx, int dy, int dz) {
            BlockPos p = at(dx, dy, dz);
            return level().getBlockState(p).isCollisionShapeFullBlock(level(), p);
        }

        @Override
        public float hardness(int dx, int dy, int dz) {
            BlockPos p = at(dx, dy, dz);
            return level().getBlockState(p).getDestroySpeed(level(), p);
        }

        @Override
        public void breakBlock(int dx, int dy, int dz) {
            level().destroyBlock(at(dx, dy, dz), true);
        }

        @Override
        public void hurtLiving(double[] box, float amount) {
            AABB area = new AABB(worldPosition.getX() + box[0], worldPosition.getY() + box[1], worldPosition.getZ() + box[2],
                    worldPosition.getX() + box[3], worldPosition.getY() + box[4], worldPosition.getZ() + box[5]);
            for (net.minecraft.world.entity.LivingEntity e : level().getEntitiesOfClass(net.minecraft.world.entity.LivingEntity.class, area)) {
                e.hurt(level().damageSources().generic(), amount);
            }
        }

        @Override
        public boolean ejectFluidExact(Tank tank, int[] amounts, Face skip) {
            if (tank.isEmpty()) return false;
            Fluid f = MachineBlockEntity.fluid(tank.fluid());
            if (f == null) return false;
            for (Face face : shuffledFaces()) {
                if (face == skip) continue;
                Direction d = direction(face);
                BlockPos p = worldPosition.relative(d);
                if (level().getBlockEntity(p) instanceof MachineBlockEntity other && other.type().id().equals(type().id())) continue;
                IFluidHandler h = level().getCapability(Capabilities.FluidHandler.BLOCK, p, d.getOpposite());
                if (h == null) continue;
                for (int amount : amounts) {
                    if (tank.amount() < amount) continue;
                    net.neoforged.neoforge.fluids.FluidStack offer = new net.neoforged.neoforge.fluids.FluidStack(f, amount);
                    if (h.fill(offer, IFluidHandler.FluidAction.SIMULATE) < amount) continue;
                    h.fill(offer, IFluidHandler.FluidAction.EXECUTE);
                    tank.drain(amount, false);
                    return true;
                }
            }
            return false;
        }

        @Override
        public Stack damage(Stack item, int amount) {
            ItemStack s = Stacks.toLoader(item);
            s.hurtAndBreak(amount, (ServerLevel) level(), (net.minecraft.server.level.ServerPlayer) null, i -> {});
            return Stacks.toDomain(s);
        }

        @Override
        public boolean playerNearby(double range) {
            return level().getNearestPlayer(worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5, range, false) != null;
        }

        @Override
        public int maxEnchantmentWeight() {
            return level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT).listElements()
                    .mapToInt(h -> h.value().getWeight()).max().orElse(mariculture.domain.metal.Ethereal.VANILLA_MAX_WEIGHT);
        }

        @Override
        public void explode(float radius) {
            var r = level().getRandom();
            level().explode(null, worldPosition.getX() + r.nextDouble(), worldPosition.getY() + 2 + r.nextDouble(), worldPosition.getZ() + r.nextDouble(),
                    radius, net.minecraft.world.level.Level.ExplosionInteraction.TNT);
        }

        @Override
        public int ejectFluid(Tank tank, int amount) {
            if (tank.isEmpty()) return 0;
            Fluid f = MachineBlockEntity.fluid(tank.fluid());
            if (f == null) return 0;
            int moved = 0;
            for (Face face : shuffledFaces()) {
                Direction d = direction(face);
                BlockPos p = worldPosition.relative(d);
                if (level().getBlockEntity(p) instanceof MachineBlockEntity) continue;
                IFluidHandler h = level().getCapability(Capabilities.FluidHandler.BLOCK, p, d.getOpposite());
                if (h == null) continue;
                int give = Math.min(amount - moved, tank.amount());
                int filled = h.fill(new net.neoforged.neoforge.fluids.FluidStack(f, give), IFluidHandler.FluidAction.EXECUTE);
                tank.drain(filled, false);
                moved += filled;
                if (moved >= amount || tank.isEmpty()) break;
            }
            return moved;
        }

        @Override
        public ContainerResult processContainer(Stack container, Tank tank) {
            Droplets.Droplet drop = Droplets.of(container);
            if (drop != null) {
                if (drop.fluid() == null || tank.fill(drop.fluid(), drop.amount(), true) < drop.amount()) return null;
                tank.fill(drop.fluid(), drop.amount(), false);
                return new ContainerResult(Stack.of(Droplets.USELESS), true);
            }
            TankHandler handler = new TankHandler(List.of(tank));
            ItemStack in = Stacks.toLoader(container.withCount(1));
            FluidActionResult r = FluidUtil.tryEmptyContainer(in, handler, Integer.MAX_VALUE, null, true);
            if (r.isSuccess()) return new ContainerResult(Stacks.toDomain(r.getResult()), true);
            r = FluidUtil.tryFillContainer(in, handler, Integer.MAX_VALUE, null, true);
            if (r.isSuccess()) return new ContainerResult(Stacks.toDomain(r.getResult()), false);
            return null;
        }

        @Override
        public List<ItemEntityView> itemsAt(List<int[]> offsets) {
            List<ItemEntityView> out = new ArrayList<>();
            for (int[] o : offsets) {
                for (ItemEntity e : level().getEntitiesOfClass(ItemEntity.class, new AABB(at(o[0], o[1], o[2])))) {
                    out.add(new ItemEntityView() {
                        @Override
                        public Stack stack() {
                            return Stacks.toDomain(e.getItem());
                        }

                        @Override
                        public boolean inWater() {
                            return e.isInWater();
                        }

                        @Override
                        public boolean inLava() {
                            return e.isInLava();
                        }

                        @Override
                        public void setStack(Stack stack) {
                            if (stack.isEmpty()) e.discard();
                            else e.setItem(Stacks.toLoader(stack));
                        }
                    });
                }
            }
            return out;
        }

        @Override
        public List<Face> shuffledFaces() {
            List<Face> faces = new ArrayList<>(List.of(Face.values()));
            Collections.shuffle(faces, new java.util.Random(level().random.nextLong()));
            return faces;
        }

        @Override
        public void affectLiving(List<int[]> offsets, Function<FishEffects.Living, List<FishEffects.Action>> rule) {
            for (int[] o : offsets) {
                for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, new AABB(at(o[0], o[1], o[2])))) {
                    FishActions.apply(rule.apply(FishActions.view(e)), e);
                }
            }
        }

        @Override
        public void affectLivingNear(List<int[]> offsets, int radius, Function<FishEffects.Living, List<FishEffects.Action>> rule) {
            if (offsets.isEmpty()) return;
            int[] min = offsets.get(0).clone(), max = offsets.get(0).clone();
            for (int[] o : offsets) {
                for (int i = 0; i < 3; i++) {
                    min[i] = Math.min(min[i], o[i]);
                    max[i] = Math.max(max[i], o[i]);
                }
            }
            BlockPos a = at(min[0], min[1], min[2]), b = at(max[0], max[1], max[2]);
            AABB box = new AABB(a.getX() - radius, a.getY() - radius, a.getZ() - radius,
                    b.getX() + 1 + radius, b.getY() + 1 + radius, b.getZ() + 1 + radius);
            for (LivingEntity e : level().getEntitiesOfClass(LivingEntity.class, box)) {
                if (e.isAlive()) FishActions.apply(rule.apply(FishActions.view(e)), e);
            }
        }

        @Override
        public boolean bonemeal(int dx, int dy, int dz) {
            BlockPos pos = at(dx, dy, dz);
            if (!net.minecraft.world.item.BoneMealItem.growCrop(new ItemStack(net.minecraft.world.item.Items.BONE_MEAL), level(), pos)) return false;
            level().levelEvent(1505, pos, 15);
            return true;
        }

        @Override
        public int light(int dx, int dy, int dz) {
            return level().getMaxLocalRawBrightness(at(dx, dy, dz));
        }

        @Override
        public void affectWorld(FishEffects.WorldAction action, List<int[]> offsets) {
            ServerLevel level = level();
            if (action instanceof FishEffects.SpawnMob s) {
                int[] o = offsets.get(s.coord());
                if (count(s.countType(), offsets) >= s.cap()) return;
                EntityType<?> type = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(s.entity()));
                Entity e = type.create(level);
                if (e == null) return;
                BlockPos p = at(o[0], o[1], o[2]);
                e.moveTo(p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, level.random.nextFloat() * 360f, 0f);
                if (e instanceof Mob mob) {
                    mob.finalizeSpawn(level, level.getCurrentDifficultyAt(p), MobSpawnType.SPAWNER, null);
                    if (s.entity().equals("minecraft:skeleton")) mob.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
                            new ItemStack(net.minecraft.world.item.Items.BOW));
                }
                level.addFreshEntity(e);
            } else if (action instanceof FishEffects.StoneToGlowstone g) {
                int[] o = offsets.get(g.coord());
                BlockPos p = at(o[0], o[1], o[2]).relative(direction(Face.values()[g.face()]));
                if (level.getBlockState(p).is(Blocks.STONE)) level.setBlockAndUpdate(p, Blocks.GLOWSTONE.defaultBlockState());
            } else if (action instanceof FishEffects.LightningNear l) {
                LightningBolt bolt = EntityType.LIGHTNING_BOLT.create(level);
                if (bolt != null) {
                    bolt.moveTo(Vec3.atBottomCenterOf(worldPosition.offset(l.dx(), 0, l.dz())));
                    level.addFreshEntity(bolt);
                }
            } else if (action instanceof FishEffects.PushEnergy pe) {
                for (Direction d : Direction.values()) {
                    IEnergyStorage es = level.getCapability(Capabilities.EnergyStorage.BLOCK, worldPosition.relative(d), d.getOpposite());
                    if (es != null && es.canReceive()) es.receiveEnergy(pe.amount(), false);
                }
            } else if (action instanceof FishEffects.BreedAnimals b) {
                List<Animal> animals = new ArrayList<>();
                for (int[] o : offsets) animals.addAll(level.getEntitiesOfClass(Animal.class, new AABB(at(o[0], o[1], o[2]))));
                if (animals.size() < b.cap()) {
                    for (Animal a : animals) if (!a.isBaby() && !a.isInLove()) a.setInLove(null);
                }
            } else if (action instanceof FishEffects.CollectItems c) {
                for (Direction d : Direction.values()) {
                    IItemHandler h = level.getCapability(Capabilities.ItemHandler.BLOCK, worldPosition.relative(d), d.getOpposite());
                    if (h == null || level.getBlockEntity(worldPosition.relative(d)) instanceof MachineBlockEntity) continue;
                    for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, new AABB(worldPosition.below()).inflate(c.range()))) {
                        ItemStack left = ItemHandlerHelper.insertItemStacked(h, item.getItem(), false);
                        if (left.isEmpty()) item.discard();
                        else item.setItem(left);
                    }
                }
            } else if (action instanceof FishEffects.SpawnLoot l) {
                int[] o = offsets.get(l.coord());
                BlockPos p = at(o[0], o[1], o[2]);
                String table = FishEffects.KOI_LOOT.get(level.random.nextInt(FishEffects.KOI_LOOT.size()));
                LootTable loot = level.getServer().reloadableRegistries().getLootTable(
                        net.minecraft.resources.ResourceKey.create(Registries.LOOT_TABLE, ResourceLocation.parse(table)));
                List<ItemStack> stacks = loot.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(p))
                        .create(LootContextParamSets.CHEST));
                if (!stacks.isEmpty()) level.addFreshEntity(new ItemEntity(level, p.getX() + 0.5, p.getY() + 0.5, p.getZ() + 0.5, stacks.get(0)));
            }
        }

        private int count(String type, List<int[]> offsets) {
            int n = 0;
            for (int[] o : offsets) {
                AABB box = new AABB(at(o[0], o[1], o[2]));
                if (type.equals("animal")) {
                    n += level().getEntitiesOfClass(Animal.class, box).size();
                } else {
                    EntityType<?> t = BuiltInRegistries.ENTITY_TYPE.get(ResourceLocation.parse(type));
                    n += level().getEntities(t, box, e -> true).size();
                }
            }
            return n;
        }
    }
}
