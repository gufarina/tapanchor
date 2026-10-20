package com.tapanchor.app.domain;

public final class KeyPress {
    public final Trigger trigger;
    public final boolean down;
    public final int repeatCount;
    public final long timeMs;

    public KeyPress(Trigger trigger, boolean down, int repeatCount, long timeMs) {
        this.trigger = trigger;
        this.down = down;
        this.repeatCount = repeatCount;
        this.timeMs = timeMs;
    }
}
