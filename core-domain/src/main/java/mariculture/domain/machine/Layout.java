package mariculture.domain.machine;

import java.util.ArrayList;
import java.util.List;

/**
 * A machine screen described as data, so each target needs one generic menu and screen. Coordinates
 * are GUI pixels from the panel's top-left corner. Legacy screens were 176 wide plus a 25 pixel side
 * panel for upgrades and the redstone and eject buttons (core/gui/feature/FeatureUpgrades etc.).
 */
public record Layout(int width, int height, int inventoryX, int inventoryY, List<Widget> widgets) {
    public static final int SIDE_PANEL_X = 179;

    public Layout {
        widgets = List.copyOf(widgets);
    }

    public sealed interface Widget permits SlotW, TankW, ProgressW, ButtonW, LabelW, FaceW {}

    /** A machine slot. */
    public record SlotW(int slot, int x, int y) implements Widget {}

    /** A tank gauge showing the fluid of tank {@code tank}. */
    public record TankW(int tank, int x, int y, int w, int h) implements Widget {}

    /**
     * A bar filled by gauge {@code value} over gauge {@code max} (or the constant {@code fixedMax} when
     * {@code max} is negative), growing in {@code direction} ("right", "up", "down").
     */
    public record ProgressW(int value, int max, int fixedMax, int x, int y, int w, int h, String direction, int color) implements Widget {}

    /** A button sending {@code id}; its label is lang key {@code key + "." + gauge(stateGauge)}. */
    public record ButtonW(int id, int x, int y, int w, int h, String key, int stateGauge) implements Widget {}

    /** Text: lang key {@code key} formatted with the value of gauge {@code gauge} (or none when negative). */
    public record LabelW(String key, int gauge, int x, int y) implements Widget {}

    /** A face letter (D, U, N, S, W, E from a Face ordinal gauge) drawn in the corner of the slot at x, y. */
    public record FaceW(int gauge, int x, int y) implements Widget {}

    public static Builder builder(int height) {
        return new Builder(height);
    }

    public static final class Builder {
        private final int height;
        private final List<Widget> widgets = new ArrayList<>();
        private int width = 176;

        private Builder(int height) {
            this.height = height;
        }

        public Builder slot(int slot, int x, int y) {
            widgets.add(new SlotW(slot, x, y));
            return this;
        }

        /** A row of {@code count} slots starting at slot {@code first}. */
        public Builder row(int first, int count, int x, int y) {
            for (int i = 0; i < count; i++) slot(first + i, x + i * 18, y);
            return this;
        }

        /** A grid of slots, row by row. */
        public Builder grid(int first, int columns, int rows, int x, int y) {
            for (int r = 0; r < rows; r++) row(first + r * columns, columns, x, y + r * 18);
            return this;
        }

        public Builder tank(int tank, int x, int y, int w, int h) {
            widgets.add(new TankW(tank, x, y, w, h));
            return this;
        }

        public Builder progress(int value, int max, int x, int y, int w, int h, String direction, int color) {
            widgets.add(new ProgressW(value, max, 0, x, y, w, h, direction, color));
            return this;
        }

        public Builder progressFixed(int value, int fixedMax, int x, int y, int w, int h, String direction, int color) {
            widgets.add(new ProgressW(value, -1, fixedMax, x, y, w, h, direction, color));
            return this;
        }

        public Builder button(int id, int x, int y, int w, int h, String key, int stateGauge) {
            widgets.add(new ButtonW(id, x, y, w, h, key, stateGauge));
            return this;
        }

        public Builder face(int gauge, int x, int y) {
            widgets.add(new FaceW(gauge, x, y));
            return this;
        }

        public Builder label(String key, int gauge, int x, int y) {
            widgets.add(new LabelW(key, gauge, x, y));
            return this;
        }

        /** The standard side panel: three upgrade slots, redstone mode and eject buttons. */
        public Builder standardSidePanel(boolean upgrades, boolean ejectButton) {
            width = 176 + 26;
            if (upgrades) for (int i = 0; i < 3; i++) slot(i, SIDE_PANEL_X + 3, 8 + i * 18);
            button(StandardMachine.BUTTON_REDSTONE, SIDE_PANEL_X + 2, 66, 18, 14, "gui.mariculture.redstone", StandardMachine.G_REDSTONE);
            if (ejectButton) button(StandardMachine.BUTTON_EJECT, SIDE_PANEL_X + 2, 82, 18, 14, "gui.mariculture.eject", StandardMachine.G_EJECT);
            return this;
        }

        public Layout build() {
            return new Layout(width, height, 8, height - 82, widgets);
        }
    }
}
