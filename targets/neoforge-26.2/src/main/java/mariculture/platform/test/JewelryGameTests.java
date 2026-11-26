package mariculture.platform.test;

import mariculture.domain.magic.JewelryType;
import mariculture.platform.Stacks;
import mariculture.platform.magic.Enchants;
import mariculture.platform.magic.WornStore;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.FunctionGameTestInstance;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.gametest.framework.TestData;
import net.minecraft.gametest.framework.TestEnvironmentDefinition;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/** Jewelry worn in Curios slots gives its effect: a ring of Fall in the ring slot, a necklace of 1-Up in the necklace slot. */
public final class JewelryGameTests {
    private static final DeferredRegister<Consumer<GameTestHelper>> FUNCTIONS = DeferredRegister.create(Registries.TEST_FUNCTION, "mariculture");

    private JewelryGameTests() {}

    private static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("mariculture", path);
    }

    public static void register(IEventBus bus) {
        FUNCTIONS.register("ring_in_curios_slot_softens_fall", () -> JewelryGameTests::ringSoftensFall);
        FUNCTIONS.register("necklace_in_curios_slot_saves_life", () -> JewelryGameTests::necklaceSavesLife);
        FUNCTIONS.register(bus);
        bus.addListener((RegisterGameTestsEvent event) -> {
            Holder<TestEnvironmentDefinition<?>> env = event.registerEnvironment(id("default"), new TestEnvironmentDefinition.AllOf(List.of()));
            for (String t : new String[] {"ring_in_curios_slot_softens_fall", "necklace_in_curios_slot_saves_life"}) {
                event.registerTest(id(t), new FunctionGameTestInstance(ResourceKey.create(Registries.TEST_FUNCTION, id(t)),
                        new TestData<>(env, id("empty7"), 60, 0, true)));
            }
        });
    }

    private static ItemStack piece(GameTestHelper h, JewelryType type, String enchantment, int level) {
        ItemStack s = new ItemStack(Stacks.item(type.item()));
        Enchants.apply(s, Map.of(enchantment, level), h.getLevel().registryAccess());
        return s;
    }

    private static Player wearer(GameTestHelper h, JewelryType type, ItemStack stack) {
        // Curios fills a player's slots when a real player logs in; a mock player (no connection, which
        // would reject Curios' sync packet) gets them from Curios' own reset, i.e. the data-pack slot list.
        Player p = h.makeMockPlayer(GameType.SURVIVAL);
        top.theillusivec4.curios.api.CuriosApi.getCuriosInventory(p)
                .ifPresent(top.theillusivec4.curios.api.type.capability.ICuriosItemHandler::reset);
        h.assertTrue(WornStore.hasSlot(p, type), "player has a Curios " + type.id() + " slot");
        WornStore.setFirst(p, type, stack);
        h.assertTrue(ItemStack.matches(WornStore.first(p, type), stack), type.id() + " sits in its Curios slot");
        return p;
    }

    public static void ringSoftensFall(GameTestHelper h) {
        Player p = wearer(h, JewelryType.RING, piece(h, JewelryType.RING, "mariculture:fall", 3));
        h.assertTrue(WornStore.strength(p, "mariculture:fall") == 3, "ring of Fall 3 counts as worn");
        p.causeFallDamage(10, 1.0F, p.damageSources().fall());
        h.assertTrue(p.getHealth() == p.getMaxHealth(), "a 10 block fall is cancelled, health " + p.getHealth());
        h.succeed();
    }

    public static void necklaceSavesLife(GameTestHelper h) {
        Player p = wearer(h, JewelryType.NECKLACE, piece(h, JewelryType.NECKLACE, "mariculture:health", 1));
        h.assertTrue(WornStore.strength(p, "mariculture:health") == 1, "necklace of 1-Up counts as worn");
        p.hurtServer(h.getLevel(), p.damageSources().generic(), 1000.0F);
        h.assertTrue(p.isAlive() && p.getHealth() == 5.0F, "a lethal hit leaves 5 health, have " + p.getHealth());
        h.succeed();
    }
}
