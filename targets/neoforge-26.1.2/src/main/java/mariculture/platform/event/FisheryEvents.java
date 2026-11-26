package mariculture.platform.event;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.stats.Stats;
import net.minecraft.tags.ItemTags;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FishingHook;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.neoforged.neoforge.event.AddServerReloadListenersEvent;
import net.neoforged.neoforge.event.TagsUpdatedEvent;
import net.neoforged.neoforge.event.entity.item.ItemExpireEvent;
import net.neoforged.neoforge.event.entity.player.ItemFishedEvent;

import mariculture.domain.fish.FishRegistry;
import mariculture.domain.fish.LiveFish;
import mariculture.domain.fish.Mutation;
import mariculture.domain.fish.Species;
import mariculture.domain.fishery.SquidDrops;
import mariculture.domain.fishing.FishingHat;
import mariculture.domain.fishing.FishingRules;
import mariculture.domain.machine.Stack;
import mariculture.domain.pearl.PearlColor;
import mariculture.platform.Env;
import mariculture.platform.Rngs;
import mariculture.platform.Stacks;
import mariculture.platform.item.RodItem;

/** Fish species data packs, Mariculture fishing catches and live fish dying on land. */
public final class FisheryEvents {
    private static final Logger LOG = LogUtils.getLogger();
    private static List<Species> species = List.of();
    private static List<Mutation> mutations = List.of();

    private FisheryEvents() {}

    // ---- data packs --------------------------------------------------------------------

    public static void onAddReloadListeners(AddServerReloadListenersEvent event) {
        event.addListener(Identifier.fromNamespaceAndPath("mariculture", "fish_data"), new SimplePreparableReloadListener<List<List<?>>>() {
            @Override
            protected List<List<?>> prepare(ResourceManager manager, ProfilerFiller profiler) {
                List<Species> s = new ArrayList<>();
                List<Mutation> m = new ArrayList<>();
                read(manager, "mariculture/fish_species", (id, json, src) -> s.add(Species.parse(id, json, src)));
                read(manager, "mariculture/fish_mutation", (id, json, src) -> m.add(Mutation.parse(id, json, src)));
                List<mariculture.domain.plugin.Tweaks.Change> t = new ArrayList<>();
                List<mariculture.domain.fishing.FishingSpeed.Entry> f = new ArrayList<>();
                read(manager, "mariculture/tweaks", (id, json, src) -> t.addAll(mariculture.domain.plugin.TweakData.parse(id, json, src)));
                read(manager, "mariculture/fishing_speed", (id, json, src) -> f.add(mariculture.domain.fishing.FishingSpeed.parse(json, src)));
                List<mariculture.domain.env.BiomeEnvironment.Layer> b = new ArrayList<>();
                read(manager, "mariculture/biome_environment", (id, json, src) -> b.add(mariculture.domain.env.BiomeEnvironment.parse(json, src)));
                List<mariculture.domain.fishing.RodTiers.Layer> r = new ArrayList<>();
                read(manager, "mariculture/rod_tiers", (id, json, src) -> r.add(mariculture.domain.fishing.RodTiers.parse(json, src)));
                return List.of(s, m, t, f, b, r);
            }

            @Override
            @SuppressWarnings("unchecked")
            protected void apply(List<List<?>> data, ResourceManager manager, ProfilerFiller profiler) {
                species = (List<Species>) data.get(0);
                mutations = (List<Mutation>) data.get(1);
                mariculture.domain.plugin.Tweaks.setData((List<mariculture.domain.plugin.Tweaks.Change>) data.get(2));
                mariculture.domain.fishing.FishingSpeed.setData((List<mariculture.domain.fishing.FishingSpeed.Entry>) data.get(3));
                mariculture.domain.env.BiomeEnvironment.setData((List<mariculture.domain.env.BiomeEnvironment.Layer>) data.get(4));
                mariculture.domain.fishing.RodTiers.setData((List<mariculture.domain.fishing.RodTiers.Layer>) data.get(5));
                rebuild();
            }
        });
    }

    private interface Parser {
        void parse(String id, String json, String source);
    }

    private static void read(ResourceManager manager, String folder, Parser parser) {
        for (Map.Entry<Identifier, Resource> e : manager.listResources(folder, rl -> rl.getPath().endsWith(".json")).entrySet()) {
            Identifier rl = e.getKey();
            String name = rl.getPath().substring(folder.length() + 1, rl.getPath().length() - 5);
            try (Reader r = e.getValue().openAsReader()) {
                parser.parse(rl.getNamespace() + ":" + name, new String(readAll(r)), rl.toString());
            } catch (IOException | RuntimeException ex) {
                LOG.error("Skipping Mariculture data {}: {}", rl, ex.getMessage());
            }
        }
    }

    private static char[] readAll(Reader r) throws IOException {
        StringBuilder sb = new StringBuilder();
        char[] buf = new char[4096];
        int n;
        while ((n = r.read(buf)) > 0) sb.append(buf, 0, n);
        return sb.toString().toCharArray();
    }

    public static void onTagsUpdated(TagsUpdatedEvent event) {
        if (event.getUpdateCause() == TagsUpdatedEvent.UpdateCause.SERVER_DATA_LOAD) rebuild();
    }

    private static void rebuild() {
        Set<String> tags = new HashSet<>();
        BuiltInRegistries.ITEM.getTags().forEach(set -> {
            if (set.size() > 0) tags.add(set.key().location().toString());
        });
        FishRegistry r = FishRegistry.build(species, mutations, tags);
        r.problems().forEach(p -> LOG.debug("Mariculture fish data: {}", p));
        LiveFish.setRegistry(r);
    }

    // ---- fishing -----------------------------------------------------------------------

    /** Replaces vanilla's catch for Mariculture rods (and the vanilla rod as the DIRE tier), FishingHandler.getCatch. */
    public static void onItemFished(ItemFishedEvent event) {
        if (event.getHookEntity() == null) return; // BUG-077: Tide posts the event with no FishingHook
        if (STARDEW_ROLLED.remove(event.getHookEntity())) return; // S2: the drops are already Mariculture's catch
        if (reelIn(event.getHookEntity())) event.setCanceled(true);
    }

    /** Hooks whose fishing loot {@link #stardewCatch} replaced; their ItemFishedEvent is left alone. */
    private static final java.util.Set<FishingHook> STARDEW_ROLLED = java.util.Collections.newSetFromMap(new java.util.WeakHashMap<>());
    private static final ThreadLocal<Boolean> ROLLING = ThreadLocal.withInitial(() -> false);

    /**
     * Compat study S2 (PLUG-031): with Stardew Fishing on, Mariculture's catch replaces the minecraft:gameplay/fishing
     * loot (a global loot modifier) instead of being swapped in after the reel-in, so Stardew's minigame plays for the
     * fish that bit. Null leaves the loot alone: plugin off, a rod Mariculture leaves to vanilla, or a roll of our own.
     */
    public static ItemStack stardewCatch(FishingHook hook) {
        if (ROLLING.get() || !mariculture.domain.plugin.Plugins.active(mariculture.domain.plugin.Plugins.STARDEW_FISHING)) return null;
        Player player = hook.getPlayerOwner();
        if (player == null || !(hook.level() instanceof ServerLevel level) || hook instanceof mariculture.platform.entity.LavaHook) return null;
        ItemStack rod = rodOf(player);
        Species.Rod tier = tierOf(rod);
        if (tier == null || LiveFish.registry().allSpecies().isEmpty()) return null;
        ItemStack caught;
        ROLLING.set(true);
        try {
            caught = resolveCatch(cast(level, hook.blockPosition(), player, rod, tier), level, hook.position(), rod, hook);
        } finally {
            ROLLING.set(false);
        }
        STARDEW_ROLLED.add(hook);
        return caught.isEmpty() ? new ItemStack(Items.STICK) : caught;
    }

    private static ItemStack rodOf(Player player) {
        return player.getMainHandItem().getItem() instanceof net.minecraft.world.item.FishingRodItem ? player.getMainHandItem() : player.getOffhandItem();
    }

    /** The Mariculture tier a rod fishes at, or null when Mariculture leaves its catch to vanilla. */
    private static Species.Rod tierOf(ItemStack rod) {
        return rod.getItem() instanceof RodItem r ? r.spec().tier() : rod.is(Items.FISHING_ROD) ? Species.Rod.DIRE
                : mariculture.domain.plugin.Plugins.rodTier(Stacks.id(rod.getItem()));
    }

    /**
     * Mariculture's catch for a hook being reeled in with a bite: thrown to the player with experience, as fire-immune
     * items from a lava hook (FISH-033). Also called by {@link mariculture.platform.entity.LavaHook}, which fires no
     * event. False when Mariculture leaves the catch to vanilla.
     */
    public static boolean reelIn(FishingHook hook) {
        Player player = hook.getPlayerOwner();
        if (player == null || !(hook.level() instanceof ServerLevel level)) return false;
        ItemStack rod = rodOf(player);
        Species.Rod tier = tierOf(rod);
        if (tier == null || LiveFish.registry().allSpecies().isEmpty()) return false;
        FishingRules.Catch c = cast(level, hook.blockPosition(), player, rod, tier);
        ItemStack caught = resolveCatch(c, level, hook.position(), rod, hook);
        if (caught.isEmpty()) caught = new ItemStack(Items.STICK);
        // Only a catch from lava comes out fire-immune; that item type sank in water and never reached the player.
        ItemEntity item = hook instanceof mariculture.platform.entity.LavaHook
                && level.getFluidState(hook.blockPosition()).is(net.minecraft.tags.FluidTags.LAVA)
                ? new mariculture.platform.entity.FireImmuneItem(level, hook.getX(), hook.getY(), hook.getZ(), caught)
                : new ItemEntity(level, hook.getX(), hook.getY(), hook.getZ(), caught);
        double dx = player.getX() - hook.getX(), dy = player.getY() - hook.getY(), dz = player.getZ() - hook.getZ();
        item.setDeltaMovement(dx * 0.1, dy * 0.1 + Math.sqrt(Math.sqrt(dx * dx + dy * dy + dz * dz)) * 0.08, dz * 0.1);
        level.addFreshEntity(item);
        level.addFreshEntity(new ExperienceOrb(level, player.getX(), player.getY() + 0.5, player.getZ() + 0.5, level.getRandom().nextInt(6) + 1));
        if (caught.is(ItemTags.FISHES)) player.awardStat(Stats.FISH_CAUGHT, 1);
        return true;
    }

    /** FishingHandler.getCatch for a cast of {@code rod} (tier {@code tier}) by {@code player} into the water at {@code pos}. */
    public static FishingRules.Catch cast(ServerLevel level, net.minecraft.core.BlockPos pos, Player player, ItemStack rod, Species.Rod tier) {
        FishingHat.Effects hat = hatEffects(player);
        double armour = hat == null ? 0 : hat.aliveModifier();
        int lots = level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.LUCK_OF_THE_SEA)
                .map(h -> EnchantmentHelper.getItemEnchantmentLevel(h, rod)).orElse(0);
        FishingRules.Cast cast = new FishingRules.Cast(tier, lots, Env.dimensionKind(level), pos.getY(), level.isBrightOutside(), Env.salinity(level, pos),
                Env.temperature(level, pos), hat, armour, Env.where(level, pos));
        return FishingRules.cast(cast, LiveFish.registry(), Rngs.of(level.getRandom()));
    }

    /** A catch as an item: vanilla fishing loot rolled at {@code origin} with the rod, or the domain item, enchanted as it says. */
    public static ItemStack resolveCatch(FishingRules.Catch c, ServerLevel level, net.minecraft.world.phys.Vec3 origin, ItemStack rod,
                                         @org.jetbrains.annotations.Nullable net.minecraft.world.entity.Entity hook) {
        boolean enchant;
        int enchantLevel;
        ItemStack out;
        if (c instanceof FishingRules.VanillaLoot v) {
            Identifier table = Identifier.withDefaultNamespace("gameplay/fishing/" + v.table());
            LootTable loot = level.getServer().reloadableRegistries().getLootTable(ResourceKey.create(Registries.LOOT_TABLE, table));
            List<ItemStack> items = loot.getRandomItems(new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN, origin)
                    .withParameter(LootContextParams.TOOL, rod).withOptionalParameter(LootContextParams.THIS_ENTITY, hook).create(LootContextParamSets.FISHING));
            out = items.isEmpty() ? ItemStack.EMPTY : items.get(0);
            enchant = v.enchant();
            enchantLevel = v.enchantLevel();
        } else {
            Stack s = FishingRules.toStack(c, LiveFish.registry(), Rngs.of(level.getRandom()));
            out = s == null ? ItemStack.EMPTY : Stacks.toLoader(s);
            if (c instanceof FishingRules.ItemCatch i && i.item().equals("mariculture:potion/fire_resistance")) {
                out = net.minecraft.world.item.alchemy.PotionContents.createItemStack(Items.POTION, net.minecraft.world.item.alchemy.Potions.FIRE_RESISTANCE);
            }
            enchant = c instanceof FishingRules.ItemCatch i2 && i2.enchant();
            enchantLevel = c instanceof FishingRules.ItemCatch i3 ? i3.enchantLevel() : 0;
        }
        if (enchant && enchantLevel > 0 && out.isEnchantable()) {
            out = EnchantmentHelper.enchantItem(level.getRandom(), out, enchantLevel, level.registryAccess(), Optional.empty());
        }
        return out;
    }

    private static FishingHat.Effects hatEffects(LivingEntity player) {
        ItemStack head = player.getItemBySlot(EquipmentSlot.HEAD);
        String id = Stacks.id(head.getItem());
        if (id.equals("mariculture:" + FishingHat.PLAIN_ID)) return FishingHat.effects(null);
        for (PearlColor c : PearlColor.values()) if (id.equals("mariculture:" + FishingHat.itemId(c))) return FishingHat.effects(c);
        return null;
    }

    // ---- commands ----------------------------------------------------------------------

    /** /mariculture info: what the fish data packs loaded (a diagnostic; legacy had no commands). */
    public static void onRegisterCommands(net.neoforged.neoforge.event.RegisterCommandsEvent event) {
        event.getDispatcher().register(net.minecraft.commands.Commands.literal("mariculture")
                .then(net.minecraft.commands.Commands.literal("info").executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal(LiveFish.registry().summary() + "; " + mariculture.domain.plugin.Tweaks.summary()), false);
                    return 1;
                })));
    }

    // ---- live fish on land -------------------------------------------------------------

    public static void onItemExpire(ItemExpireEvent event) {
        ItemEntity entity = event.getEntity();
        Stack stack = Stacks.toDomain(entity.getItem());
        if (!LiveFish.isLiveFish(stack)) return;
        Stack raw = LiveFish.onExpire(stack, entity.isInWater());
        if (raw == null) {
            event.setExtraLife(20);
        } else {
            entity.setItem(Stacks.toLoader(raw));
            event.setExtraLife(LiveFish.DEAD_FISH_LIFESPAN);
        }
    }

    // ---- squid drops -------------------------------------------------------------------

    public static void onLivingDrops(net.neoforged.neoforge.event.entity.living.LivingDropsEvent event) {
        LivingEntity entity = event.getEntity();
        if (!(entity.level() instanceof ServerLevel level) || !SquidDrops.ENTITY.equals(BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString())) return;
        int looting = event.getSource().getEntity() instanceof LivingEntity killer ? level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).get(Enchantments.LOOTING).map(h -> EnchantmentHelper.getEnchantmentLevel(h, killer)).orElse(0) : 0;
        ItemStack raw = Stacks.toLoader(Stack.of(SquidDrops.ITEM, SquidDrops.count(looting, Rngs.of(level.getRandom()))));
        event.getDrops().add(new ItemEntity(level, entity.getX(), entity.getY(), entity.getZ(), raw));
    }
}
