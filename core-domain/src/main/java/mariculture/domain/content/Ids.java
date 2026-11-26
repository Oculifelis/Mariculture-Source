package mariculture.domain.content;

import java.util.regex.Pattern;

/** Resource-location helpers that do not depend on any Minecraft class. */
public final class Ids {
    public static final String MOD_ID = "mariculture";
    private static final Pattern PATH = Pattern.compile("[a-z0-9_./-]+");
    private static final Pattern NAMESPACED = Pattern.compile("[a-z0-9_.-]+:[a-z0-9_./-]+");

    private Ids() {}

    public static String requireValid(String path) {
        if (path == null || !PATH.matcher(path).matches()) throw new IllegalArgumentException("Invalid resource path: " + path);
        return path;
    }

    /** Namespaced id; bare paths get the Mariculture namespace. */
    public static String full(String idOrPath) {
        if (idOrPath.indexOf(':') >= 0) {
            if (!NAMESPACED.matcher(idOrPath).matches()) throw new IllegalArgumentException("Invalid resource id: " + idOrPath);
            return idOrPath;
        }
        return MOD_ID + ":" + requireValid(idOrPath);
    }

    /** Namespaced id; bare paths get the minecraft namespace (vanilla effects, items and tags). */
    public static String vanilla(String idOrPath) {
        return idOrPath.indexOf(':') >= 0 ? full(idOrPath) : "minecraft:" + requireValid(idOrPath);
    }
}
