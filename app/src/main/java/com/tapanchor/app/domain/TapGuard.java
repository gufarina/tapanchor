package com.tapanchor.app.domain;

import java.util.ArrayDeque;
import java.util.List;

/** Única regra que decide se um KeyPress vira toque. Invariantes I1-I5 da SPEC. */
public final class TapGuard {
    public static final int MAX_TAPS_PER_SECOND = 10;

    /** consume: o evento não segue para o sistema. tap: onde tocar, ou null. */
    public static final class Decision {
        public final boolean consume;
        public final Anchor tap;

        Decision(boolean consume, Anchor tap) {
            this.consume = consume;
            this.tap = tap;
        }
    }

    private static final Decision PASS = new Decision(false, null);
    private static final Decision SWALLOW = new Decision(true, null);

    private final ArrayDeque<Long> recentTaps = new ArrayDeque<>();

    public Decision decide(KeyPress press, List<Binding> bindings) {
        Binding binding = find(press.trigger, bindings);
        if (binding == null) return PASS;
        if (!press.down || press.repeatCount > 0) return SWALLOW;       // I1
        while (!recentTaps.isEmpty() && press.timeMs - recentTaps.peekFirst() >= 1000) {
            recentTaps.pollFirst();
        }
        if (recentTaps.size() >= MAX_TAPS_PER_SECOND) return SWALLOW;   // I4
        recentTaps.addLast(press.timeMs);
        return new Decision(true, binding.anchor);                       // I3, I5
    }

    public static Binding find(Trigger trigger, List<Binding> bindings) {
        for (Binding b : bindings) {
            if (b.trigger.matches(trigger)) return b;
        }
        return null;
    }
}
