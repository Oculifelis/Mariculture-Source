package mariculture.domain.env;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Loads the shipped biome_environment files into {@link BiomeEnvironment}, in id order like a server. */
public final class BiomeData {
    private BiomeData() {}

    public static void loadShipped() {
        Path dir = Path.of("../content-source/data/mariculture/mariculture/biome_environment");
        List<BiomeEnvironment.Layer> layers = new ArrayList<>();
        try (Stream<Path> files = Files.list(dir)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                layers.add(BiomeEnvironment.parse(Files.readString(f), f.getFileName().toString()));
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        BiomeEnvironment.setData(layers);
    }
}
