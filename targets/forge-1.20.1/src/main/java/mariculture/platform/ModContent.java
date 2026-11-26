package mariculture.platform;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.PlaceOnWaterBlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import mariculture.domain.content.BlockSpec;
import mariculture.domain.content.Catalog;
import mariculture.domain.content.Ids;
import mariculture.domain.content.ItemKind;
import mariculture.domain.content.ItemSpec;
import mariculture.platform.block.OysterBlock;
import mariculture.platform.block.OysterBlockEntity;
import mariculture.platform.block.PedestalBlock;
import mariculture.platform.block.NetBlock;
import mariculture.platform.block.NeonLampBlock;
import mariculture.platform.entity.BassBomb;
import mariculture.platform.fluid.ModFluids;
import mariculture.platform.item.FishEggItem;
import mariculture.platform.item.FoodItem;
import mariculture.platform.item.LiveFishItem;
import mariculture.platform.item.ModArmor;
import mariculture.platform.item.RawFishItem;
import mariculture.platform.machine.MachineBlock;
import mariculture.platform.machine.MachineBlockEntity;
import mariculture.platform.machine.MachineMenu;
import mariculture.platform.recipe.FishCraftingRecipe;
import mariculture.platform.item.RodItem;
import mariculture.platform.item.ScannerItem;
import mariculture.platform.item.TemperatureControlItem;
import mariculture.platform.worldgen.OysterFeature;
import mariculture.platform.worldgen.SeabedFeature;

/** Registers the shared catalog's simple blocks and items. */
public final class ModContent {
    private static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, Ids.MOD_ID);
    private static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, Ids.MOD_ID);
    private static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Ids.MOD_ID);
    private static final List<RegistryObject<? extends Item>> TAB_ITEMS = new ArrayList<>();
    private static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(ForgeRegistries.FEATURES, Ids.MOD_ID);
    private static final Map<String, Supplier<? extends Block>> BLOCKS_BY_ID = new HashMap<>();
    private static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, Ids.MOD_ID);
    private static final RegistryObject<BlockEntityType<OysterBlockEntity>> OYSTER_ENTITY = BLOCK_ENTITIES.register("oyster",
            () -> BlockEntityType.Builder.of(OysterBlockEntity::new, block("oyster")).build(null));

    private static final DeferredRegister<net.minecraft.world.item.crafting.RecipeSerializer<?>> RECIPE_SERIALIZERS = DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Ids.MOD_ID);
    private static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(ForgeRegistries.MENU_TYPES, Ids.MOD_ID);
    private static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(ForgeRegistries.ENTITY_TYPES, Ids.MOD_ID);
    private static final RegistryObject<MenuType<mariculture.platform.magic.MirrorMenu>> MIRROR_MENU = MENUS.register("mirror",
            () -> new MenuType<>(mariculture.platform.magic.MirrorMenu::new, net.minecraft.world.flag.FeatureFlags.VANILLA_SET));
    private static final RegistryObject<MenuType<MachineMenu>> MACHINE_MENU = MENUS.register("machine",
            () -> IForgeMenuType.create((id, inv, buf) -> new MachineMenu(id, inv, buf.readBlockPos())));
    private static final RegistryObject<EntityType<BassBomb>> BASS_BOMB = ENTITIES.register("bass_bomb",
            () -> EntityType.Builder.<BassBomb>of(BassBomb::new, MobCategory.MISC).sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10)
                    .build("bass_bomb"));
    private static final RegistryObject<EntityType<mariculture.platform.entity.SpeedBoatEntity>> SPEED_BOAT = ENTITIES.register(
            mariculture.domain.maritech.SpeedBoat.ID, () -> EntityType.Builder.<mariculture.platform.entity.SpeedBoatEntity>of(
                    mariculture.platform.entity.SpeedBoatEntity::new, MobCategory.MISC)
                    .sized(mariculture.domain.maritech.SpeedBoat.WIDTH, mariculture.domain.maritech.SpeedBoat.HEIGHT).clientTrackingRange(10)
                    .build(mariculture.domain.maritech.SpeedBoat.ID));
    // FISH-033: lava-fished catches (EntityItemFireImmune), drawn as ordinary dropped items.
    private static final RegistryObject<EntityType<mariculture.platform.entity.FireImmuneItem>> FIRE_IMMUNE_ITEM = ENTITIES.register("fire_immune_item",
            () -> EntityType.Builder.<mariculture.platform.entity.FireImmuneItem>of(mariculture.platform.entity.FireImmuneItem::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).fireImmune().clientTrackingRange(6).updateInterval(20).build("fire_immune_item"));
    private static final RegistryObject<EntityType<mariculture.platform.entity.FluddSquirt>> FLUDD_SQUIRT = ENTITIES.register("fludd_squirt",
            () -> EntityType.Builder.<mariculture.platform.entity.FluddSquirt>of(mariculture.platform.entity.FluddSquirt::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f).clientTrackingRange(4).updateInterval(10).build("fludd_squirt"));
    private static final RegistryObject<EntityType<mariculture.platform.entity.Rockhopper>> ROCKHOPPER = ENTITIES.register(
            mariculture.domain.worldgen.Rockhopper.ID, () -> EntityType.Builder.<mariculture.platform.entity.Rockhopper>of(
                    mariculture.platform.entity.Rockhopper::new, MobCategory.CREATURE)
                    .sized(mariculture.domain.worldgen.Rockhopper.WIDTH, mariculture.domain.worldgen.Rockhopper.HEIGHT).clientTrackingRange(10)
                    .build(mariculture.domain.worldgen.Rockhopper.ID));
    private static final DeferredRegister<net.minecraft.sounds.SoundEvent> SOUNDS = DeferredRegister.create(net.minecraftforge.registries.ForgeRegistries.SOUND_EVENTS, Ids.MOD_ID);
    private static final Map<String, Supplier<net.minecraft.sounds.SoundEvent>> SOUNDS_BY_ID = new HashMap<>();
    private static final RegistryObject<BlockEntityType<MachineBlockEntity>> MACHINE_ENTITY = BLOCK_ENTITIES.register("machine",
            () -> BlockEntityType.Builder.of(MachineBlockEntity::new, Catalog.blocks().stream().filter(BlockSpec::isMachine)
                    .map(b -> block(b.id())).toArray(Block[]::new)).build(null));
    private static final RegistryObject<BlockEntityType<mariculture.platform.factory.CustomBlockEntity>> CUSTOM_ENTITY = BLOCK_ENTITIES.register("custom",
            () -> BlockEntityType.Builder.of(mariculture.platform.factory.CustomBlockEntity::new, Catalog.blocks().stream()
                    .filter(b -> b.shape() == mariculture.domain.content.Shape.CUSTOM).map(b -> block(b.id())).toArray(Block[]::new)).build(null));

    static {
        for (BlockSpec spec : Catalog.blocks()) {
            RegistryObject<Block> block = BLOCKS.register(spec.id(), () -> createBlock(spec));
            BLOCKS_BY_ID.put(spec.id(), block);
            if (spec.hasItem() && spec.shape() == mariculture.domain.content.Shape.NET) {
                TAB_ITEMS.add(ITEMS.register(spec.id(), () -> new PlaceOnWaterBlockItem(block.get(), new Item.Properties())));
            } else if (spec.hasItem() && spec.alsoPlacesOnWater()) {
                TAB_ITEMS.add(ITEMS.register(spec.id(), () -> new mariculture.platform.item.WaterPlacedBlockItem(block.get(), new Item.Properties())));
            } else if (spec.hasItem() && spec.shape() == mariculture.domain.content.Shape.CUSTOM) {
                // Sawmill blocks only come out of the sawmill, with their faces; an unpainted one is not offered in the tab.
                ITEMS.register(spec.id(), () -> new mariculture.platform.factory.FactoryItems.CustomItem(block.get(), new Item.Properties()));
            } else if (spec.hasItem()) {
                TAB_ITEMS.add(ITEMS.register(spec.id(), () -> new BlockItem(block.get(), new Item.Properties())));
            }
        }
        for (ItemSpec spec : Catalog.items()) {
            TAB_ITEMS.add(ITEMS.register(spec.id(), () -> createItem(spec, new Item.Properties().stacksTo(spec.maxStack()))));
        }
        // PLUG-004: the aquatic backpacks are Forestry items, so they only exist while Forestry is loaded.
        if (net.minecraftforge.fml.ModList.get().isLoaded("forestry")) mariculture.platform.compat.forestry.ForestryCompat.registerBackpacks(ITEMS, TAB_ITEMS);
        FEATURES.register("seabed", SeabedFeature::new);
        FEATURES.register("oysters", OysterFeature::new);
        FEATURES.register("ocean_decor", mariculture.platform.worldgen.OceanDecorFeature::new);
        for (String s : mariculture.domain.worldgen.Rockhopper.SOUNDS) {
            String id = "entity.rockhopper." + s;
            SOUNDS_BY_ID.put(id, SOUNDS.register(id, () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new ResourceLocation(Ids.MOD_ID, id))));
        }
        SOUNDS_BY_ID.put(mariculture.domain.maritech.Fludd.SOUND, SOUNDS.register(mariculture.domain.maritech.Fludd.SOUND,
                () -> net.minecraft.sounds.SoundEvent.createVariableRangeEvent(new ResourceLocation(Ids.MOD_ID, mariculture.domain.maritech.Fludd.SOUND))));
        TABS.register(Ids.MOD_ID, () -> CreativeModeTab.builder()
                .title(Component.translatable("itemGroup." + Ids.MOD_ID))
                .icon(() -> new ItemStack(ITEMS.getEntries().iterator().next().get()))
                .displayItems((params, output) -> {
                    List<Item> sorted = new ArrayList<>();
                    TAB_ITEMS.forEach(item -> sorted.add(item.get()));
                    sorted.removeIf(item -> mariculture.domain.plugin.Plugins.hiddenWithoutPlugin(
                            net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString()));
                    mariculture.domain.content.CreativeOrder.sort(sorted,
                            item -> net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(item).toString());
                    sorted.forEach(output::accept);
                })
                .build());
    }

    private ModContent() {}

    static void register(IEventBus modBus) {
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        TABS.register(modBus);
        FEATURES.register(modBus);
        BLOCK_ENTITIES.register(modBus);
        MENUS.register(modBus);
        RECIPE_SERIALIZERS.register("caviar", () -> FishCraftingRecipe.CAVIAR);
        RECIPE_SERIALIZERS.register("fish_reset", () -> FishCraftingRecipe.FISH_RESET);
        RECIPE_SERIALIZERS.register("jewelry", () -> mariculture.platform.recipe.JewelryCraftingRecipe.SERIALIZER);
        RECIPE_SERIALIZERS.register(modBus);
        mariculture.platform.magic.ModEnchantments.register(modBus);
        ENTITIES.register(modBus);
        SOUNDS.register(modBus);
        modBus.addListener((net.minecraftforge.event.entity.EntityAttributeCreationEvent e) ->
                e.put(ROCKHOPPER.get(), mariculture.platform.entity.Rockhopper.createAttributes().build()));
        modBus.addListener((net.minecraftforge.event.entity.SpawnPlacementRegisterEvent e) -> e.register(ROCKHOPPER.get(),
                net.minecraft.world.entity.SpawnPlacements.Type.ON_GROUND, net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                mariculture.platform.entity.Rockhopper::checkSpawnRules, net.minecraftforge.event.entity.SpawnPlacementRegisterEvent.Operation.REPLACE));
        ModFluids.register(modBus);
    }

    public static BlockEntityType<mariculture.platform.factory.CustomBlockEntity> customEntity() {
        return CUSTOM_ENTITY.get();
    }

    public static BlockEntityType<MachineBlockEntity> machineEntity() {
        return MACHINE_ENTITY.get();
    }

    public static MenuType<mariculture.platform.magic.MirrorMenu> mirrorMenu() {
        return MIRROR_MENU.get();
    }

    public static MenuType<MachineMenu> machineMenu() {
        return MACHINE_MENU.get();
    }

    public static EntityType<mariculture.platform.entity.Rockhopper> rockhopperType() {
        return ROCKHOPPER.get();
    }

    public static net.minecraft.sounds.SoundEvent sound(String id) {
        return SOUNDS_BY_ID.get(id).get();
    }

    public static EntityType<mariculture.platform.entity.FireImmuneItem> fireImmuneItemType() {
        return FIRE_IMMUNE_ITEM.get();
    }

    public static EntityType<mariculture.platform.entity.SpeedBoatEntity> speedBoatType() {
        return SPEED_BOAT.get();
    }

    public static EntityType<mariculture.platform.entity.FluddSquirt> fluddSquirtType() {
        return FLUDD_SQUIRT.get();
    }

    public static EntityType<BassBomb> bassBombType() {
        return BASS_BOMB.get();
    }

    private static Item createItem(ItemSpec spec, Item.Properties props) {
        if (spec.kind() instanceof ItemKind.Rod rod) return new RodItem(props, spec.id(), rod);
        if (spec.kind() instanceof ItemKind.Armor armor) return ModArmor.item(props, armor.material(), armor.slot());
        if (spec.kind() instanceof ItemKind.Food f) {
            FoodProperties.Builder food = new FoodProperties.Builder().nutrition(f.hunger()).saturationMod(f.saturation());
            if (f.alwaysEdible()) food.alwaysEat();
            if (spec.id().startsWith("bait_")) food.fast();
            return new FoodItem(props.food(food.build()), f.remainder());
        }
        if (spec.kind() instanceof ItemKind.LiveFish) return new LiveFishItem(props);
        if (spec.kind() instanceof ItemKind.RawFish r) {
            return new RawFishItem(props.food(new FoodProperties.Builder().nutrition(r.hunger()).saturationMod(r.saturation()).build()), r.species());
        }
        if (spec.kind() instanceof ItemKind.FluidBucket b) return new BucketItem(ModFluids.sourceSupplier(b.fluid()), props.craftRemainder(Items.BUCKET));
        if (spec.kind() instanceof ItemKind.FishEggs) return new FishEggItem(props);
        if (spec.kind() instanceof ItemKind.FluidContainer fc) return new mariculture.platform.item.FluidContainerItem(props, fc);
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("mob_magnet")) return new mariculture.platform.magic.MobMagnetItem(props);
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("infiwater")) return new mariculture.platform.magic.InfiwaterItem(props);
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("hydrophobic_initiator")) return new Item(props) {
            @Override
            public void appendHoverText(ItemStack stack, @org.jetbrains.annotations.Nullable net.minecraft.world.level.Level world,
                                        java.util.List<net.minecraft.network.chat.Component> lines, net.minecraft.world.item.TooltipFlag flag) {
                lines.add(mariculture.platform.magic.MagicToolEvents.initiatorTooltip());
            }
        };
        if (spec.kind() instanceof ItemKind.Special sp && (sp.behaviour().equals("thermometer") || sp.behaviour().equals("dragon_spawn"))) return new mariculture.platform.item.UseItem(props, sp.behaviour());
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().endsWith("mirror")) {
            return new mariculture.platform.magic.MirrorItem(props.durability(sp.durability()), spec.id());
        }
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("jewelry")) {
            return new mariculture.platform.item.JewelryItem(props, mariculture.domain.magic.JewelryType.byItem(Ids.MOD_ID + ":" + spec.id()));
        }
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("spawn_egg")) {
            return new net.minecraftforge.common.ForgeSpawnEggItem(ROCKHOPPER, 0x1A1A1A, 0xF2D21B, props);
        }
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("hammer")) return new mariculture.platform.item.HammerItem(props);
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("worked")) return new mariculture.platform.item.WorkedItem(props);
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("scanner")) return new ScannerItem(props);
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("metal_droplet")) return new mariculture.platform.item.MetalDropletItem(props);
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("item_filter")) return new mariculture.platform.item.FilterItem(props);
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("battery")) return new mariculture.platform.item.BatteryItem(props);
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("speed_boat")) return new mariculture.platform.item.SpeedBoatItem(props.stacksTo(1));
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("chalk")) return new mariculture.platform.factory.FactoryItems.Chalk(props.durability(sp.durability()));
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("paintbrush")) return new mariculture.platform.factory.FactoryItems.Paintbrush(props.durability(sp.durability()));
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("dna")) return new mariculture.platform.item.DnaItem(props);
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("rotor")) return new mariculture.platform.item.RotorItem(props);
        if (spec.kind() instanceof ItemKind.Special sp && sp.behaviour().equals("temperature_control")) return new TemperatureControlItem(props);
        if (spec.kind() instanceof ItemKind.Special sp) return new Item(sp.durability() > 0 ? props.durability(sp.durability()) : props);
        return new Item(props);
    }

    /** A stack of one catalog item by its path. */
    public static ItemStack stack(String id) {
        return new ItemStack(ForgeRegistries.ITEMS.getValue(new ResourceLocation(Ids.MOD_ID, id)));
    }

    public static BlockEntityType<OysterBlockEntity> oysterEntity() {
        return OYSTER_ENTITY.get();
    }

    /** A catalog block by its path, for adapters such as world generation. */
    public static Block block(String id) {
        Supplier<? extends Block> block = BLOCKS_BY_ID.get(id);
        if (block == null) throw new IllegalArgumentException("no such block: " + id);
        return block.get();
    }

    private static Block createBlock(BlockSpec spec) {
        BlockBehaviour.Properties props = BlockBehaviour.Properties.of()
                .mapColor(mapColor(spec))
                .strength(spec.hardness(), spec.resistance())
                .sound(sound(spec));
        if (spec.pickaxe()) props = props.requiresCorrectToolForDrops();
        return switch (spec.shape()) {
            case CUBE -> new Block(props);
            case PILLAR -> new RotatedPillarBlock(props);
            case PEDESTAL -> new PedestalBlock(props);
            case OYSTER -> new OysterBlock(props.noOcclusion().randomTicks());
            case MACHINE -> new MachineBlock(props, spec);
            case MACHINE_GLASS -> new MachineBlock(props.noOcclusion().forceSolidOn(), spec);
            case MACHINE_MODEL -> mariculture.domain.content.MachineModel.of(spec.id()).facing() == mariculture.domain.content.MachineModel.Facing.ALL
                    ? new mariculture.platform.machine.DirectionalMachineBlock(props.noOcclusion().forceSolidOn(), spec) : new MachineBlock(props.noOcclusion().forceSolidOn(), spec);
            case NET -> new NetBlock(props.noOcclusion().noCollission().instabreak());
            case LAMP -> new NeonLampBlock(props.noOcclusion());
            case ANCIENT_SAND -> new mariculture.platform.block.GroundBlock(props, true);
            case BUBBLE_SAND -> new mariculture.platform.block.GroundBlock(props, false);
            case CUSTOM -> mariculture.platform.factory.CustomShapes.create(spec.id(), props);
            case GAS -> new mariculture.platform.block.GasBlock(props.noOcclusion().noCollission().replaceable().noLootTable()
                    .pushReaction(net.minecraft.world.level.material.PushReaction.DESTROY).isViewBlocking((s, l, q) -> false));
            case CORAL, LIVING_CORAL -> new mariculture.platform.block.CoralPlantBlock(props.noOcclusion().noCollission()
                    .mapColor(MapColor.COLOR_PINK), spec.id());
            case GLASS -> new net.minecraft.world.level.block.GlassBlock(props.noOcclusion().isValidSpawn((st, l, p, e) -> false)
                    .isRedstoneConductor((st, l, p) -> false).isSuffocating((st, l, p) -> false).isViewBlocking((st, l, p) -> false));
        };
    }

    private static SoundType sound(BlockSpec spec) {
        return switch (spec.sound()) {
            case STONE -> SoundType.STONE;
            case METAL -> SoundType.METAL;
            case GLASS -> SoundType.GLASS;
            case WOOD -> SoundType.WOOD;
            case SAND -> SoundType.SAND;
            case WOOL -> SoundType.WOOL;
            case SNOW -> SoundType.SNOW;
            case CORAL -> SoundType.CORAL_BLOCK;
        };
    }

    private static MapColor mapColor(BlockSpec spec) {
        return spec.sound() == mariculture.domain.content.Sound.METAL ? MapColor.METAL : MapColor.STONE;
    }
}
