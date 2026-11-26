package mariculture.domain.fish;

import java.util.Locale;

/** Water salinity in legacy ordinal order (mariculture.api.core.Environment.Salinity). */
public enum Salinity {
    FRESH, BRACKISH, SALINE;

    public static Salinity byName(String name) {
        return valueOf(name.toUpperCase(Locale.ROOT));
    }

    /** Shifts by upgrade steps, clamped to the ends (legacy FishyHelper.canLive). */
    public Salinity shift(int steps) {
        int i = Math.max(0, Math.min(2, ordinal() + steps));
        return values()[i];
    }
}
