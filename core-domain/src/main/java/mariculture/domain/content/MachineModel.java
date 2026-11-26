package mariculture.domain.content;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The block model of a machine the legacy mod drew with its own renderer (shape MACHINE_MODEL): boxes in model
 * pixels (0-16) facing UP (six-way machines) or SOUTH (horizontal machines). The exporter writes it as a JSON model,
 * targets build the outline from {@link #bounds}. Texture keys refer to the block's texture slots.
 */
public record MachineModel(Facing facing, List<Box> boxes) {
    public enum Facing { HORIZONTAL, ALL }

    /** One textured face: texture slot and uv (0-16), or null uv for the default. */
    public record FaceTex(String texture, double[] uv) {}

    /** A box; faces by name (down, up, north, south, west, east). */
    public record Box(double[] from, double[] to, Map<String, FaceTex> faces) {}

    private static final Map<String, MachineModel> BY_ID = new LinkedHashMap<>();

    public static void register(String id, MachineModel model) {
        if (BY_ID.put(id, model) != null) throw new IllegalStateException("Duplicate machine model " + id);
    }

    public static MachineModel of(String id) {
        MachineModel m = BY_ID.get(id);
        if (m == null) throw new IllegalArgumentException("No machine model " + id);
        return m;
    }

    /** A box with one texture on every face, uv from the box position. */
    public static Box box(double x1, double y1, double z1, double x2, double y2, double z2, String texture) {
        Map<String, FaceTex> faces = new LinkedHashMap<>();
        for (String f : List.of("down", "up", "north", "south", "west", "east")) faces.put(f, new FaceTex(texture, null));
        return new Box(new double[] {x1, y1, z1}, new double[] {x2, y2, z2}, faces);
    }

    /**
     * A legacy ModelRenderer box drawn by a tile renderer: the renderer moved to ({@code ox}, {@code oy}, {@code oz})
     * pixels, turned 180 degrees round Z and scaled each model unit to {@code scale} pixels. {@code u}, {@code v}
     * is the texture offset on a {@code texSize} square texture; faces use the ModelBox layout.
     */
    public static Box entityBox(double ox, double oy, double oz, double scale, float x, float y, float z, int w, int h, int d,
                                int u, int v, int texSize, String texture) {
        double k = 16.0 / texSize;
        Map<String, FaceTex> faces = new LinkedHashMap<>();
        faces.put("up", new FaceTex(texture, uv(k, u + d, v, u + d + w, v + d)));
        faces.put("down", new FaceTex(texture, uv(k, u + d + w, v, u + d + 2 * w, v + d)));
        faces.put("north", new FaceTex(texture, uv(k, u + d, v + d, u + d + w, v + d + h)));
        faces.put("south", new FaceTex(texture, uv(k, u + 2 * d + w, v + d, u + 2 * d + 2 * w, v + d + h)));
        faces.put("west", new FaceTex(texture, uv(k, u + d + w, v + d, u + 2 * d + w, v + d + h)));
        faces.put("east", new FaceTex(texture, uv(k, u, v + d, u + d, v + d + h)));
        // The 180-degree turn round Z mirrors x and y.
        double[] from = {ox - (x + w) * scale, oy - (y + h) * scale, oz + z * scale};
        double[] to = {ox - x * scale, oy - y * scale, oz + (z + d) * scale};
        return new Box(round(from), round(to), faces);
    }

    private static double[] uv(double k, double u1, double v1, double u2, double v2) {
        return new double[] {u1 * k, v1 * k, u2 * k, v2 * k};
    }

    private static double[] round(double[] a) {
        for (int i = 0; i < a.length; i++) a[i] = Math.round(a[i] * 1000) / 1000.0;
        return a;
    }

    /** The outline (x1, y1, z1, x2, y2, z2 in pixels) of the model turned to face {@code f}. */
    public double[] bounds(Face f) {
        double[] b = {16, 16, 16, 0, 0, 0};
        for (Box box : boxes) {
            for (int c = 0; c < 8; c++) {
                double[] p = {(c & 1) == 0 ? box.from()[0] : box.to()[0], (c & 2) == 0 ? box.from()[1] : box.to()[1],
                        (c & 4) == 0 ? box.from()[2] : box.to()[2]};
                double[] r = turn(p[0] - 8, p[1] - 8, p[2] - 8, f);
                for (int i = 0; i < 3; i++) {
                    b[i] = Math.min(b[i], r[i] + 8);
                    b[i + 3] = Math.max(b[i + 3], r[i] + 8);
                }
            }
        }
        // Models reaching past the block (rotor blades) keep the outline inside it.
        for (int i = 0; i < 6; i++) b[i] = Math.max(0, Math.min(16, b[i]));
        return round(b);
    }

    /** Matches the exporter's blockstate rotations (six-way like vanilla end rods, horizontal by y turns). */
    private double[] turn(double x, double y, double z, Face f) {
        if (facing == Facing.ALL) {
            return switch (f) {
                case UP -> new double[] {x, y, z};
                case DOWN -> new double[] {x, -y, -z};
                case NORTH -> new double[] {x, z, -y};
                case SOUTH -> new double[] {x, -z, y};
                case WEST -> new double[] {-y, x, z};
                case EAST -> new double[] {y, -x, z};
            };
        }
        return switch (f) {
            case WEST -> new double[] {-z, y, x};
            case NORTH -> new double[] {-x, y, -z};
            case EAST -> new double[] {z, y, -x};
            default -> new double[] {x, y, z};
        };
    }
}
