package com.tapanchor.app.domain;

/** O botão físico, pelo código lido do hardware. scanCode diferencia botões sem código conhecido (0). */
public final class Trigger {
    public final int keyCode;
    public final int scanCode;

    private Trigger(int keyCode, int scanCode) {
        this.keyCode = keyCode;
        this.scanCode = scanCode;
    }

    public static Trigger key(int keyCode, int scanCode) {
        return new Trigger(keyCode, scanCode);
    }

    public boolean matches(Trigger other) {
        if (keyCode != other.keyCode) return false;
        return keyCode != 0 || scanCode == other.scanCode;
    }
}
