package mariculture.domain.fish;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/** Loads the shipped species and mutation files from content-source/ for tests. */
public final class FishData {
    static final Path ROOT = Path.of("../content-source/data/mariculture/mariculture");

    static List<Species> species() {
        List<Species> out = new ArrayList<>();
        for (Path p : files("fish_species")) out.add(Species.parse("mariculture:" + name(p), read(p), p.toString()));
        return out;
    }

    static List<Mutation> mutations() {
        List<Mutation> out = new ArrayList<>();
        for (Path p : files("fish_mutation")) out.add(Mutation.parse("mariculture:" + name(p), read(p), p.toString()));
        return out;
    }

    public static FishRegistry registry(Set<String> itemTags) {
        return FishRegistry.build(species(), mutations(), itemTags);
    }

    public static FishRegistry registry() {
        return registry(Set.of());
    }

    private static List<Path> files(String dir) {
        try (Stream<Path> s = Files.list(ROOT.resolve(dir))) {
            return s.filter(p -> p.toString().endsWith(".json")).sorted().toList();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String name(Path p) {
        String f = p.getFileName().toString();
        return f.substring(0, f.length() - 5);
    }

    private static String read(Path p) {
        try {
            return Files.readString(p);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
}
