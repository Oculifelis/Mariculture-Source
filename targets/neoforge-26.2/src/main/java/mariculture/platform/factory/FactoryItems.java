package mariculture.platform.factory;

import java.util.function.Consumer;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;

import mariculture.domain.content.Face;
import mariculture.domain.factory.CustomBlocks;
import mariculture.domain.factory.CustomFaces;
import mariculture.domain.factory.Plans;
import mariculture.domain.machine.Stack;
import mariculture.platform.Stacks;

/** Planning chalk, the paintbrush and the sawmill blocks' items. Rules live in mariculture.domain.factory. */
public final class FactoryItems {
    private FactoryItems() {}

    static Face face(Direction d) {
        return Face.values()[d.get3DDataValue()];
    }

    static Component faceName(String block) {
        Block b = CustomBlockEntity.faceBlock(block);
        return b == null ? Component.translatable("tooltip.mariculture.custom.air") : b.getName();
    }

    /** A sawmill block's item: lists the six faces it was cut from. */
    public static class CustomItem extends BlockItem {
        public CustomItem(Block block, Properties props) {
            super(block, props);
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
            CustomFaces faces = CustomFaces.fromData(Stacks.data(stack));
            for (Face f : Face.values()) {
                lines.accept(Component.translatable("tooltip.mariculture.custom.face", Component.translatable("tooltip.mariculture.face."
                        + f.name().toLowerCase(java.util.Locale.ROOT)), faceName(faces.block(f))).withStyle(ChatFormatting.GRAY));
            }
        }
    }

    /** ItemChalk: used on a block with a blank plan in the inventory, draws that block's plan. */
    public static class Chalk extends Item {
        public Chalk(Properties props) {
            super(props);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Player player = ctx.getPlayer();
            Level level = ctx.getLevel();
            BlockPos pos = ctx.getClickedPos();
            if (player == null) return InteractionResult.PASS;
            int blank = -1;
            for (int i = 0; i < player.getInventory().getContainerSize() && blank < 0; i++) {
                if (Stacks.id(player.getInventory().getItem(i).getItem()).equals(Plans.BLANK)) blank = i;
            }
            if (blank < 0) return InteractionResult.PASS;
            Plans.Type plan = planFor(level, pos);
            if (plan == null) {
                if (!level.isClientSide()) player.sendSystemMessage(Component.translatable("item.mariculture.chalk.nothing").withStyle(ChatFormatting.GRAY));
                return InteractionResult.FAIL;
            }
            if (level instanceof ServerLevel server) {
                ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 1.5, pos.getZ() + 0.5, Stacks.toLoader(Plans.of(plan)));
                level.addFreshEntity(drop);
                server.sendParticles(ParticleTypes.EXPLOSION, pos.getX() + 0.5, pos.getY() + 1, pos.getZ() + 0.5, 1, 0, 0, 0, 0);
                if (!player.isCreative()) player.getInventory().removeItem(blank, 1);
                ctx.getItemInHand().hurtAndBreak(1, player, ctx.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
            }
            return InteractionResult.SUCCESS;
        }

        static Plans.Type planFor(Level level, BlockPos pos) {
            BlockState st = level.getBlockState(pos);
            Block b = st.getBlock();
            Plans.Kind kind = b instanceof FenceGateBlock ? Plans.Kind.GATE : b instanceof FenceBlock ? Plans.Kind.FENCE
                    : b instanceof SlabBlock ? Plans.Kind.SLAB : b instanceof StairBlock ? Plans.Kind.STAIRS : b instanceof WallBlock ? Plans.Kind.WALL
                    : b instanceof CarpetBlock ? Plans.Kind.CARPET : Plans.Kind.OTHER;
            boolean energyBelow = level.getCapability(Capabilities.Energy.BLOCK, pos.below(), Direction.UP) != null;
            boolean energyHere = level.getCapability(Capabilities.Energy.BLOCK, pos, null) != null;
            return Plans.forBlock(kind, energyBelow, energyHere, st.getLightEmission(level, pos) > 0, st.hasBlockEntity(), st.isAir());
        }
    }

    /** ItemPaintbrush: sneak-use loads a face, use paints it onto a sawmill block's face. */
    public static class Paintbrush extends Item {
        public Paintbrush(Properties props) {
            super(props);
        }

        @Override
        public InteractionResult useOn(UseOnContext ctx) {
            Player player = ctx.getPlayer();
            Level level = ctx.getLevel();
            BlockPos pos = ctx.getClickedPos();
            ItemStack brush = ctx.getItemInHand();
            Face face = face(ctx.getClickedFace());
            CustomBlockEntity custom = level.getBlockEntity(pos) instanceof CustomBlockEntity c ? c : null;
            boolean gate = level.getBlockState(pos).getBlock() instanceof FenceGateBlock;
            if (player != null && player.isShiftKeyDown() && !(custom != null && gate)) {
                if (!level.isClientSide()) {
                    Stack loaded = custom != null ? CustomBlocks.load(Stacks.toDomain(brush), custom.faces().block(face), custom.faces().side(face))
                            : CustomBlocks.load(Stacks.toDomain(brush), BuiltInRegistries.BLOCK.getKey(level.getBlockState(pos).getBlock()).toString(), face);
                    Stacks.setData(brush, loaded.data());
                }
                return InteractionResult.SUCCESS;
            }
            if (custom == null) return InteractionResult.PASS;
            if (!level.isClientSide()) {
                CustomFaces painted = CustomBlocks.paint(custom.faces(), face, Stacks.toDomain(brush));
                if (painted != null) {
                    custom.setFaces(painted);
                    if (player != null) brush.hurtAndBreak(1, player, ctx.getHand() == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
                }
            }
            return InteractionResult.SUCCESS;
        }

        @Override
        public InteractionResult use(Level level, Player player, InteractionHand hand) {
            if (!player.isShiftKeyDown()) return InteractionResult.PASS;
            ItemStack brush = player.getItemInHand(hand);
            if (!level.isClientSide()) Stacks.setData(brush, CustomBlocks.loadAir(Stacks.toDomain(brush)).data());
            return InteractionResult.SUCCESS;
        }

        @Override
        public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> lines, TooltipFlag flag) {
            String block = Stacks.toDomain(stack).stringData(CustomBlocks.BRUSH_BLOCK, null);
            if (block == null) lines.accept(Component.translatable("item.mariculture.paintbrush.tooltip").withStyle(ChatFormatting.GRAY));
            else if (block.equals(CustomFaces.AIR)) lines.accept(Component.translatable("item.mariculture.paintbrush.air").withStyle(ChatFormatting.GRAY));
            else lines.accept(Component.translatable("item.mariculture.paintbrush.loaded", faceName(block)).withStyle(ChatFormatting.GRAY));
        }
    }

    /** The fluid dictionary names the fluid it makes (BlockTank.onBlockActivated chat message). */
    public static Component target(String fluid) {
        var f = mariculture.platform.machine.MachineBlockEntity.fluid(fluid);
        return Component.translatable("block.mariculture.fluid_dictionary.target",
                f == null ? Component.literal(fluid) : f.getFluidType().getDescription());
    }
}
