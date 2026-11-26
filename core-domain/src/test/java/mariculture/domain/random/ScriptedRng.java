package mariculture.domain.random;

import java.util.ArrayDeque;
import java.util.Deque;

/** Replays fixed draws so tests can hit exact boundaries; fails loudly if a rule draws more. */
public final class ScriptedRng implements Rng {
    private final Deque<Object> draws = new ArrayDeque<>();

    public ScriptedRng floats(float... values) {
        for (float v : values) draws.add(v);
        return this;
    }

    public ScriptedRng ints(int... values) {
        for (int v : values) draws.add(v);
        return this;
    }

    @Override
    public float nextFloat() {
        Object next = draws.poll();
        if (!(next instanceof Float f)) throw new AssertionError("Expected a float draw to be scripted, got " + next);
        return f;
    }

    @Override
    public int nextInt(int bound) {
        Object next = draws.poll();
        if (!(next instanceof Integer i)) throw new AssertionError("Expected an int draw to be scripted, got " + next);
        if (i < 0 || i >= bound) throw new AssertionError("Scripted int " + i + " outside [0," + bound + ")");
        return i;
    }

    public boolean exhausted() {
        return draws.isEmpty();
    }
}
