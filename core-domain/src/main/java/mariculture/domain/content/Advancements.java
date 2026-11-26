package mariculture.domain.content;

import java.util.List;

/**
 * Mariculture's advancement tab. Legacy 1.7 had achievements only for a few steps (core/lib/Achievements); the
 * modern tree walks the main routes instead: fishing, fish breeding, pearls and jewelry, metals and diving.
 * Each entry is granted for holding any of {@code items} (an item id or {@code #tag}).
 */
public final class Advancements {
    public enum Frame { TASK, GOAL, CHALLENGE }

    public record Entry(String id, String parent, String icon, Frame frame, String title, String description, List<String> items) {}

    private static final List<Entry> ALL = List.of(
            new Entry("root", null, "reed_fishing_rod", Frame.TASK, "Mariculture",
                    "Pick up a fishing rod or limestone from the sea bed", List.of("#mariculture:fishing_rods", "#mariculture:limestone")),
            new Entry("bait", "root", "bait_worm", Frame.TASK, "Can of Worms", "Get some bait", List.of("#mariculture:bait")),
            new Entry("titanium_rod", "bait", "titanium_fishing_rod", Frame.GOAL, "Reel Strength",
                    "Make a titanium fishing rod", List.of("titanium_fishing_rod")),
            new Entry("live_fish", "root", "goldfish", Frame.TASK, "Something Fishy", "Catch a live fish", List.of("#mariculture:live_fish")),
            new Entry("sushi", "live_fish", "sushi", Frame.TASK, "Raw Talent", "Make sushi", List.of("sushi")),
            new Entry("fish_eggs", "live_fish", "fish_eggs", Frame.TASK, "Plenty More Fish",
                    "Breed two fish in a feeder and collect their eggs", List.of("fish_eggs")),
            new Entry("incubator", "fish_eggs", "incubator", Frame.GOAL, "Hatching a Plan", "Build an incubator", List.of("incubator")),
            new Entry("pearl", "root", "white_pearl", Frame.TASK, "Pearl Diver", "Find a pearl", List.of("#mariculture:pearls")),
            new Entry("jewelry", "pearl", "ring", Frame.TASK, "Put a Ring on It", "Make a ring, bracelet or necklace",
                    List.of("#mariculture:jewelry")),
            new Entry("magic_mirror", "jewelry", "magic_mirror", Frame.TASK, "Mirror, Mirror", "Make a magic mirror", List.of("magic_mirror")),
            new Entry("celestial_mirror", "magic_mirror", "celestial_mirror", Frame.CHALLENGE, "Written in the Stars",
                    "Make a celestial mirror", List.of("celestial_mirror")),
            new Entry("crucible", "root", "crucible", Frame.TASK, "Hot Stuff", "Build a crucible furnace", List.of("crucible")),
            new Entry("titanium", "crucible", "titanium_ingot", Frame.GOAL, "Light as a Feather, Hard as Nails",
                    "Cast a titanium ingot", List.of("titanium_ingot")),
            new Entry("vat", "crucible", "vat", Frame.TASK, "Big Vat Energy", "Build a vat", List.of("vat")),
            new Entry("diving", "root", "diving_helmet", Frame.TASK, "Into the Deep", "Get a diving helmet", List.of("diving_helmet")),
            new Entry("scuba", "diving", "scuba_mask", Frame.GOAL, "Deep Breath", "Get a scuba mask", List.of("scuba_mask")));

    private Advancements() {}

    public static List<Entry> all() {
        return ALL;
    }
}
