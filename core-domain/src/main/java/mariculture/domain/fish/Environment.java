package mariculture.domain.fish;

/**
 * Whether a fish can live in given water. Legacy: core/handlers/EnvironmentHandler.matches
 * (112-125) and fishery/FishyHelper.canLive (213-261).
 *
 * <p>Fix: legacy computed the upper salinity bound with {@code Math.max(2, base + tolerance)},
 * so it was always at least SALINE and the upper limit never excluded anything (a freshwater
 * fish with no tolerance lived in the ocean). The intended {@code min} is used, matching the
 * range the legacy feeder tooltip already displayed (TileFeeder:540-547).
 */
public final class Environment {
    private Environment() {}

    public static boolean matches(Salinity salt, int temperature, Species species, int salinityTolerance, int temperatureTolerance) {
        int base = species.salinityBase().ordinal();
        int tol = species.ignoresSalinity() ? 2 : salinityTolerance;
        int minSalt = Math.max(0, base - tol);
        int maxSalt = Math.min(2, base + tol);
        int minTemp = species.temperatureBase() - temperatureTolerance;
        int maxTemp = species.temperatureBase() + temperatureTolerance;
        return temperature >= minTemp && temperature <= maxTemp && salt.ordinal() >= minSalt && salt.ordinal() <= maxSalt;
    }

    /** Tolerances are the expressed (upper, boosted) alleles of the fish. */
    public static boolean canLive(Genome fish, Species species, Salinity salt, int temperature, Boosters boosters) {
        return matches(salt, temperature, species,
                fish.expressed(Genes.SALINITY_TOLERANCE, boosters), fish.expressed(Genes.TEMPERATURE_TOLERANCE, boosters));
    }
}
