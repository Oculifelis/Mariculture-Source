package mariculture.tools;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Stream;

import mariculture.domain.content.Catalog;
import mariculture.domain.content.Ids;
import mariculture.domain.json.JsonReader;

/**
 * Decides which compendium entries can ship: every Mariculture item, recipe and image an entry
 * shows must exist. Vanilla and Patchouli ids are assumed present. Categories without a shippable
 * entry are left out; a category whose own icon is missing borrows its first entry's icon.
 */
final class BookFilter {
    private final Path repo;
    private final Set<String> items = new HashSet<>();
    private final Set<String> recipes = new HashSet<>();
    /** entry id -> missing references, for the coverage report. */
    private final Map<String, List<String>> excluded = new TreeMap<>();
    private final Set<String> included = new java.util.TreeSet<>();
    /** Entries that show no Mariculture item or recipe but may ship anyway (general rules text). */
    private final Set<String> textOnlyAllowed = new HashSet<>();
    private final Set<String> excludedById = new HashSet<>();

    BookFilter(Path repo) {
        this.repo = repo;
        Catalog.blocks().forEach(b -> items.add(Ids.full(b.id())));
        Catalog.items().forEach(i -> items.add(Ids.full(i.id())));
        Catalog.recipes().forEach(r -> recipes.add(Ids.full(r.id())));
        // Plugin recipes ship with conditions; entries showing them carry the plugin mod's flag.
        mariculture.domain.plugin.Plugins.recipes().forEach(c -> recipes.add(Ids.full(c.recipe().id())));
        try {
            Path allow = repo.resolve("content-source/book/text_only_entries.json");
            if (Files.exists(allow)) {
                for (Object o : (List<?>) JsonReader.parse(Files.readString(allow, StandardCharsets.UTF_8))) textOnlyAllowed.add((String) o);
            }
        } catch (IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    /** Leaves an entry out of this edition's book (its content is not shipped there). */
    void exclude(String id) {
        excludedById.add(id);
    }

    void export(Path localeDir, Path dest) throws IOException {
        Map<String, String> categoryIcons = new TreeMap<>();
        Path entries = localeDir.resolve("entries");
        if (Files.isDirectory(entries)) {
            try (Stream<Path> files = Files.walk(entries)) {
                for (Path f : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                    String rel = entries.relativize(f).toString().replace('\\', '/');
                    String id = rel.substring(0, rel.length() - 5);
                    Map<?, ?> entry = (Map<?, ?>) JsonReader.parse(Files.readString(f, StandardCharsets.UTF_8));
                    List<String> missing = new ArrayList<>();
                    collect(entry, missing);
                    // A rules-only entry describes a mechanic; it waits until that mechanic is in the
                    // game unless it is explicitly allowed as general text.
                    if (missing.isEmpty() && !mentionsMariculture(entry) && !textOnlyAllowed.contains(id)) {
                        missing.add("mechanic not implemented yet (text-only entry)");
                    }
                    if (excludedById.contains(id)) missing.add("not shipped on this edition");
                    if (missing.isEmpty()) {
                        included.add(id);
                        Path out = dest.resolve("entries").resolve(rel);
                        Files.createDirectories(out.getParent());
                        Files.copy(f, out);
                        String cat = String.valueOf(entry.get("category"));
                        categoryIcons.putIfAbsent(cat, String.valueOf(entry.get("icon")));
                    } else {
                        excluded.put(id, missing);
                    }
                }
            }
        }
        Path cats = localeDir.resolve("categories");
        if (!Files.isDirectory(cats)) return;
        try (Stream<Path> files = Files.list(cats)) {
            for (Path f : files.filter(p -> p.toString().endsWith(".json")).sorted().toList()) {
                String name = f.getFileName().toString();
                String catId = Ids.MOD_ID + ":" + name.substring(0, name.length() - 5);
                if (!categoryIcons.containsKey(catId)) continue;
                @SuppressWarnings("unchecked")
                Map<String, Object> cat = (Map<String, Object>) JsonReader.parse(Files.readString(f, StandardCharsets.UTF_8));
                if (!itemKnown(String.valueOf(cat.get("icon")))) cat.put("icon", categoryIcons.get(catId));
                Path out = dest.resolve("categories").resolve(name);
                Files.createDirectories(out.getParent());
                Files.writeString(out, Json.write(cat), StandardCharsets.UTF_8);
            }
        }
    }

    private void collect(Object node, List<String> missing) {
        if (node instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                String k = String.valueOf(e.getKey());
                Object v = e.getValue();
                if ((k.equals("icon") || k.equals("item")) && v instanceof String s) {
                    for (String part : s.split(",")) if (!itemKnown(part.trim())) missing.add("item " + part.trim());
                } else if ((k.equals("recipe") || k.equals("recipe2")) && v instanceof String s) {
                    if (s.startsWith(Ids.MOD_ID + ":") && !recipes.contains(s)) missing.add("recipe " + s);
                } else if (k.equals("images") && v instanceof List<?> l) {
                    for (Object img : l) if (!imageKnown(String.valueOf(img))) missing.add("image " + img);
                } else {
                    collect(v, missing);
                }
            }
        } else if (node instanceof List<?> l) {
            for (Object o : l) collect(o, missing);
        }
    }

    private static boolean mentionsMariculture(Object node) {
        if (node instanceof Map<?, ?> m) {
            for (Map.Entry<?, ?> e : m.entrySet()) {
                String k = String.valueOf(e.getKey());
                if ((k.equals("item") || k.equals("recipe") || k.equals("recipe2")) && String.valueOf(e.getValue()).startsWith(Ids.MOD_ID + ":")) return true;
                if (!k.equals("icon") && mentionsMariculture(e.getValue())) return true;
            }
        } else if (node instanceof List<?> l) {
            for (Object o : l) if (mentionsMariculture(o)) return true;
        }
        return false;
    }

    private boolean itemKnown(String id) {
        String bare = id.contains("{") ? id.substring(0, id.indexOf('{')) : id;
        bare = bare.contains("#") ? bare.substring(0, bare.indexOf('#')) : bare;
        return !bare.startsWith(Ids.MOD_ID + ":") || items.contains(bare);
    }

    private boolean imageKnown(String id) {
        if (!id.startsWith(Ids.MOD_ID + ":")) return true;
        return Files.exists(repo.resolve("content-source/" + id.substring(Ids.MOD_ID.length() + 1)));
    }

    void writeReport(Path file) throws IOException {
        StringBuilder sb = new StringBuilder("# Compendium coverage\n\n");
        sb.append("Written by `./gradlew generateResources`; do not edit. An entry ships only when every Mariculture item, ")
          .append("recipe and image it shows exists, so the book never advertises missing content.\n\n");
        sb.append("Shipping: ").append(included.size()).append(" entries. Held back: ").append(excluded.size()).append(" entries.\n\n");
        sb.append("## Shipping\n\n");
        for (String id : included) sb.append("- ").append(id).append('\n');
        sb.append("\n## Held back (first missing references)\n\n");
        excluded.forEach((id, missing) -> sb.append("- ").append(id).append(": ")
                .append(String.join(", ", missing.subList(0, Math.min(3, missing.size()))))
                .append(missing.size() > 3 ? " and " + (missing.size() - 3) + " more" : "").append('\n'));
        Files.writeString(file, sb.toString(), StandardCharsets.UTF_8);
    }
}
