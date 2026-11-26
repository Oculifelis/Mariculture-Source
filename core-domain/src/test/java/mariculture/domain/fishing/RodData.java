package mariculture.domain.fishing;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Loads the shipped rod_tiers files into {@link RodTiers}, in id order like a server. */
public final class RodData {
    private RodData() {}

    public static void loadShipped() {
        Path dir = Path.of("../content-source/data/mariculture/mariculture/rod_tiers");
        List<RodTiers.Layer> layers = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                layers.add(RodTiers.parse(Files.readString(f), f.getFileName().toString()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        RodTiers.setData(layers);
    }
}
