package mariculture.domain.fish;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Names and tooltip lines for live fish and eggs as lang keys, so every target renders them the same
 * (legacy ItemFishy.getItemStackDisplayName, FishDNABase.getInformationDisplay, ItemEgg.addInformation).
 * Hidden genes show only after the fish has been scanned (item data "scanned").
 */
public final class FishText {
    /** Lang keys for the upper and lower species, whether it is a hybrid, and the gender symbol. */
    public record Name(String species, String upperKey, String lowerKey, boolean hybrid, String symbol) {}

    /** A tooltip line: lang key and its arguments; String arguments starting with "lang:" are themselves lang keys. */
    public record Line(String key, Object[] args) {}

    public static final String SCANNED = "scanned";

    private FishText() {}

    public static String speciesKey(String species) {
        int c = species.indexOf(':');
        String ns = c < 0 ? "mariculture" : species.substring(0, c);
        String path = c < 0 ? species : species.substring(c + 1);
        if (ns.equals("mariculture") && ShippedSpecies.byId(path) != null) return "item.mariculture." + path;
        return "species." + ns + "." + path;
    }

    public static Name name(Map<String, ?> data) {
        if (!(data.get("species") instanceof String upper)) return null;
        String lower = data.get("lower_species") instanceof String l ? l : upper;
        int gender = data.get(Genes.GENDER.key()) instanceof Number n ? n.intValue() : -1;
        String symbol = gender == Fish.MALE ? " ♂" : gender == Fish.FEMALE ? " ♀" : "";
        return new Name(upper, speciesKey(upper), speciesKey(lower), !upper.equals(lower), symbol);
    }

    public static List<Line> lines(Map<String, ?> data) {
        List<Line> out = new ArrayList<>();
        if (!(data.get("species") instanceof String)) return out;
        boolean scanned = Boolean.TRUE.equals(data.get(SCANNED)) || (data.get(SCANNED) instanceof Number n && n.intValue() != 0);
        for (Gene g : Genes.ALL) {
            if (g == Genes.GENDER || (g.hidden() && !scanned)) continue;
            if (!(data.get(g.key()) instanceof Number v)) continue;
            int value = g.express(v.intValue(), Boosters.LEGACY_DEFAULTS.of(g));
            String range = g.rangeName(value);
            Object shown = g == Genes.LIFESPAN ? (Object) (value / 1200) : value;
            out.add(new Line("gene.mariculture." + g.key(), new Object[] {range == null ? String.valueOf(shown) : "lang:gene.mariculture." + g.key() + "." + range, shown}));
        }
        if (data.get(LiveFish.CURRENT_LIFE) instanceof Number life) {
            out.add(new Line("fish.mariculture.life", new Object[] {life.intValue() / 1200}));
        }
        if (!scanned) out.add(new Line("fish.mariculture.unscanned", new Object[0]));
        return out;
    }

    /**
     * The scanner readout (fishery/gui/GuiScanner.drawForeground): species, then every gene's active and inactive
     * value. Lines use "fish.mariculture.scan_gene" (title, active, inactive) or "scan_gene_ranged" (title, number, range per allele).
     */
    public static List<Line> scanReport(Map<String, ?> data) {
        List<Line> out = new ArrayList<>();
        Name name = name(data);
        if (name == null) return out;
        out.add(new Line("fish.mariculture.scan_species", new Object[] {"lang:" + name.upperKey(), "lang:" + name.lowerKey()}));
        for (Gene g : Genes.ALL) {
            if (g == Genes.GENDER) continue;
            Object up = data.get(g.key()), low = data.get("lower_" + g.key());
            if (!(up instanceof Number u)) continue;
            int l = low instanceof Number n ? n.intValue() : u.intValue();
            String title = "lang:gene.mariculture." + g.key() + ".title";
            String ru = range(g, u.intValue()), rl = range(g, l);
            out.add(ru == null || rl == null
                    ? new Line("fish.mariculture.scan_gene", new Object[] {title, number(g, u.intValue()), number(g, l)})
                    : new Line("fish.mariculture.scan_gene_ranged", new Object[] {title, number(g, u.intValue()), ru, number(g, l), rl}));
        }
        return out;
    }

    /** FishDNA.getScannedDisplay: the number (lifespan in minutes) and the range name as a "lang:" key, when the gene has ranges. */
    private static String number(Gene g, int stored) {
        int value = g.express(stored, Boosters.LEGACY_DEFAULTS.of(g));
        return String.valueOf(g == Genes.LIFESPAN ? value / 1200 : value);
    }

    private static String range(Gene g, int stored) {
        String range = g.rangeName(g.express(stored, Boosters.LEGACY_DEFAULTS.of(g)));
        return range == null ? null : "lang:gene.mariculture." + g.key() + "." + range;
    }

    /** The fish with its hidden genes revealed in the tooltip. */
    public static mariculture.domain.machine.Stack scanned(mariculture.domain.machine.Stack fish) {
        Map<String, Object> data = new java.util.LinkedHashMap<>(fish.data());
        data.put(SCANNED, true);
        return fish.withData(data);
    }

    public static List<Line> eggLines(Map<String, ?> data) {
        List<Line> out = new ArrayList<>();
        if (data.get("eggs_left") instanceof Number n && n.intValue() > 0) out.add(new Line("fish.mariculture.eggs_remaining", new Object[] {n.intValue()}));
        else out.add(new Line("fish.mariculture.eggs_undetermined", new Object[0]));
        return out;
    }
}
