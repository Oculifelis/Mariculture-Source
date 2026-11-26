package mariculture.platform.machine;

import com.mojang.serialization.MapCodec;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import mariculture.domain.content.BlockSpec;
import mariculture.domain.fishery.Sifter;
import mariculture.domain.machine.MachineType;
import mariculture.domain.machine.Machines;
import mariculture.platform.ModContent;
import mariculture.platform.Stacks;

/** A machine block: opens its machine's screen and drops its slots when broken. */
public class MachineBlock extends BaseEntityBlock {
    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;
    /** Sifters sit a pixel low so dropped items fall inside and get sifted (legacy onEntityCollidedWithBlock). */
    private static final VoxelShape SIFTER = Block.box(0, 0, 0, 16, 15, 16);
    private final BlockSpec spec;
    private final MachineType type;

    public MachineBlock(Properties props, BlockSpec spec) {
        super(props);
        this.spec = spec;
        this.type = Machines.byId(spec.id());
        if (facesHorizontally(spec)) registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }

    /** Machines with a front texture, and modelled machines with a horizontal model, face the player who placed them. */
    static boolean facesHorizontally(BlockSpec spec) {
        return spec.facing() || spec.shape() == mariculture.domain.content.Shape.MACHINE_MODEL
                && mariculture.domain.content.MachineModel.of(spec.id()).facing() == mariculture.domain.content.MachineModel.Facing.HORIZONTAL;
    }

    public BlockSpec spec() {
        return spec;
    }

    /** The way a modelled machine faces, for its outline. */
    protected Direction modelFacing(BlockState state) {
        return state.getValue(FACING);
    }

    private final java.util.Map<Direction, VoxelShape> modelShapes = new java.util.EnumMap<>(Direction.class);

    private VoxelShape modelShape(BlockState state) {
        return modelShapes.computeIfAbsent(modelFacing(state), d -> {
            double[] b = mariculture.domain.content.MachineModel.of(spec.id()).bounds(MachineBlockEntity.face(d));
            return Block.box(b[0], b[1], b[2], b[3], b[4], b[5]);
        });
    }

    public MachineType type() {
        return type;
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        throw new UnsupportedOperationException("Machine blocks are not data-driven");
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        // The spec is not known yet in the constructor's super call, so every machine block carries the property.
        builder.add(FACING);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx) {
        return defaultBlockState().setValue(FACING, facesHorizontally(spec) ? ctx.getHorizontalDirection().getOpposite() : Direction.NORTH);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        if (spec.shape() == mariculture.domain.content.Shape.MACHINE_MODEL) return modelShape(state);
        return type.id().equals("sifter") ? SIFTER : Shapes.block();
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new MachineBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> beType) {
        if (level.isClientSide()) return null;
        return createTickerHelper(beType, ModContent.machineEntity(), (l, p, s, be) -> {
            MachineBlockEntity.serverTick(l, p, s, be);
            if (l.getGameTime() % 20 == 0) be.setChanged();
        });
    }

    @Override
    protected InteractionResult useItemOn(ItemStack held, BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand,
                                              BlockHitResult hit) {
        if (!held.isEmpty() && level.getBlockEntity(pos) instanceof MachineBlockEntity hbe) {
            mariculture.domain.machine.Stack after = hbe.machine().useHeld(Stacks.toDomain(held), player.isShiftKeyDown());
            if (after != null) {
                if (!level.isClientSide()) {
                    player.setItemInHand(hand, Stacks.toLoader(after));
                    hbe.setChanged();
                }
                return InteractionResult.SUCCESS;
            }
        }
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity wbe
                && (wbe.machine() instanceof mariculture.domain.metal.Anvil || wbe.machine() instanceof mariculture.domain.metal.Autohammer)) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (useWorkshop(level, pos, player, hand, wbe, hit.getDirection())) return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity mbe && !mbe.machine().tanks().isEmpty() && !(mbe.machine() instanceof Sifter)) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (useMachine(level, pos, player, hand, mbe)) return InteractionResult.CONSUME;
        }
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be && be.machine() instanceof Sifter sifter) {
            if (level.isClientSide()) return InteractionResult.SUCCESS;
            if (held.is(Stacks.item(Sifter.UPGRADE)) && sifter.addInventory()) {
                if (!player.getAbilities().instabuild) held.shrink(1);
                be.setChanged();
                return InteractionResult.CONSUME;
            }
            if (!held.isEmpty() && sifter.sift(be.world(), Stacks.toDomain(held))) {
                if (!player.getAbilities().instabuild) held.setCount(0);
                return InteractionResult.CONSUME;
            }
        }
        return InteractionResult.TRY_WITH_EMPTY_HAND;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity wbe
                && (wbe.machine() instanceof mariculture.domain.metal.Anvil || wbe.machine() instanceof mariculture.domain.metal.Autohammer)) {
            if (!level.isClientSide()) useWorkshop(level, pos, player, InteractionHand.MAIN_HAND, wbe, hit.getDirection());
            return InteractionResult.SUCCESS;
        }
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity ube && ube.machine().usesClick() && !player.isShiftKeyDown()) {
            if (!level.isClientSide()) useMachine(level, pos, player, InteractionHand.MAIN_HAND, ube);
            return InteractionResult.SUCCESS;
        }
        if (!type.hasScreen()) return InteractionResult.PASS;
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            if (be.machine() instanceof Sifter s && !s.hasInventory()) return InteractionResult.PASS;
            if (!level.isClientSide()) player.openMenu(be, pos);
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity,
                                net.minecraft.world.entity.InsideBlockEffectApplier effects, boolean precise) {
        if (level.isClientSide() || !(entity instanceof ItemEntity item)) return;
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be && be.machine() instanceof Sifter sifter) {
            if (sifter.sift(be.world(), Stacks.toDomain(item.getItem()))) item.discard();
        }
    }


    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, net.minecraft.world.entity.LivingEntity placer, ItemStack stack) {
        super.setPlacedBy(level, pos, state, placer, stack);
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity be) {
            be.restoreFrom(stack);
            if (placer instanceof Player && be.machine() instanceof mariculture.domain.fishery.FishFeeder feeder) {
                feeder.setOwner(placer.getUUID().toString());
                be.setChanged();
            }
        }
    }

    /** Fluid containers fill or empty the machine's tanks; casters hand out their casts. True when handled. */
    private static boolean useMachine(Level level, BlockPos pos, Player player, InteractionHand hand, MachineBlockEntity be) {
        if (!be.machine().tanks().isEmpty() && !player.getItemInHand(hand).isEmpty()
                && net.neoforged.neoforge.transfer.fluid.FluidUtil.interactWithFluidHandler(player, hand, level, pos, null)) return true;
        if (player.isShiftKeyDown() || hand != InteractionHand.MAIN_HAND) return false;
        java.util.List<mariculture.domain.machine.Stack> given = be.machine().onUse();
        if (given == null) return false;
        for (mariculture.domain.machine.Stack s : given) {
            ItemStack out = Stacks.toLoader(s);
            if (!player.getInventory().add(out)) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, out));
        }
        if (be.machine() instanceof mariculture.domain.factory.FluidDictionary d) player.sendSystemMessage(mariculture.platform.factory.FactoryItems.target(d.target()));
        be.setChanged();
        return true;
    }

    /** Anvil and autohammer right-clicks (BlockRenderedMachine.onBlockActivated). True when handled. */
    private static boolean useWorkshop(Level level, BlockPos pos, Player player, InteractionHand hand, MachineBlockEntity be, Direction face) {
        if (hand != InteractionHand.MAIN_HAND) return false;
        ItemStack held = player.getItemInHand(hand);
        java.util.List<ItemStack> give = new java.util.ArrayList<>();
        if (be.machine() instanceof mariculture.domain.metal.Anvil anvil) {
            if (held.getItem() instanceof mariculture.platform.item.HammerItem && be.items().get(0).isEmpty()) return false;
            boolean placing = be.items().get(0).isEmpty();
            for (var s : anvil.use(Stacks.toDomain(held))) give.add(Stacks.toLoader(s));
            if (placing && !held.isEmpty() && !player.getAbilities().instabuild) held.shrink(1);
        } else if (be.machine() instanceof mariculture.domain.metal.Autohammer hammer) {
            var back = hammer.use(MachineBlockEntity.face(face), Stacks.toDomain(held));
            if (back == null) return false;
            if (back.isEmpty()) {
                if (!player.getAbilities().instabuild) held.shrink(1);
            } else give.add(Stacks.toLoader(back));
        } else return false;
        for (ItemStack out : give) if (!player.getInventory().add(out)) level.addFreshEntity(new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, out));
        be.setChanged();
        return true;
    }

    /** Left-clicking the anvil with the hammer works or repairs the item on it (TileAnvil.workItem). */
    @Override
    protected void attack(BlockState state, Level level, BlockPos pos, Player player) {
        super.attack(state, level, pos, player);
        if (level.isClientSide() || !(level.getBlockEntity(pos) instanceof MachineBlockEntity be) || !(be.machine() instanceof mariculture.domain.metal.Anvil anvil)) return;
        ItemStack hammer = player.getMainHandItem();
        if (!(hammer.getItem() instanceof mariculture.platform.item.HammerItem)) return;
        Integer ret = anvil.work(n -> mariculture.domain.metal.Anvil.hasXp(player.experienceProgress, player.experienceLevel, n),
                mariculture.platform.Rngs.of(level.getRandom()));
        if (ret == null) ret = repair(level, player, be.items().get(0), hammer);
        if (ret == null || ret == 0) return;
        be.setChanged();
        level.playSound(null, pos, net.minecraft.sounds.SoundEvents.ANVIL_USE, net.minecraft.sounds.SoundSource.BLOCKS, 0.5f, 1.2f);
        if (ret > 0) hurtHammer(hammer, player);
    }

    /** Repair one point (plus fortune) for a share of a level; null when the item cannot be repaired. */
    private static Integer repair(Level level, Player player, ItemStack stack, ItemStack hammer) {
        if (stack.isEmpty() || !stack.isDamageableItem() || !stack.isDamaged()) return null;
        float cost = mariculture.domain.metal.Anvil.repairCost(player.getXpNeededForNextLevel(), enchantments(stack), ench(level, "efficiency", hammer));
        if (!mariculture.domain.metal.Anvil.hasXp(player.experienceProgress, player.experienceLevel, cost)) return 0;
        int fixed = mariculture.domain.metal.Anvil.repairAmount(ench(level, "fortune", hammer), mariculture.platform.Rngs.of(level.getRandom()));
        stack.setDamageValue(Math.max(0, stack.getDamageValue() - fixed));
        float xp = player.experienceProgress - cost;
        if (xp <= 0) {
            player.experienceProgress = 1.0f;
            player.experienceLevel = Math.max(0, player.experienceLevel - 1);
        } else player.experienceProgress = xp;
        return mariculture.domain.metal.Anvil.repairCode(cost, stack.getDamageValue() == 0);
    }

    private static int ench(Level level, String name, ItemStack stack) {
        var key = name.equals("efficiency") ? net.minecraft.world.item.enchantment.Enchantments.EFFICIENCY : net.minecraft.world.item.enchantment.Enchantments.FORTUNE;
        return level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT).get(key)
                .map(h -> stack.getEnchantmentLevel(h)).orElse(0);
    }

    private static java.util.List<int[]> enchantments(ItemStack stack) {
        java.util.List<int[]> out = new java.util.ArrayList<>();
        for (var e : stack.getTagEnchantments().entrySet()) out.add(new int[] {e.getIntValue(), e.getKey().value().getMaxCost(1) - e.getKey().value().getMinCost(1)});
        return out;
    }

    private static void hurtHammer(ItemStack hammer, Player player) {
        hammer.hurtAndBreak(1, player, net.minecraft.world.entity.EquipmentSlot.MAINHAND);
    }
}
