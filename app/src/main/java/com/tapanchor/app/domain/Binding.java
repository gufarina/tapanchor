package com.tapanchor.app.domain;

/** Um botão ligado a um ponto (I5). */
public final class Binding {
    public final int id;
    public final Trigger trigger;
    public final Anchor anchor;

    public Binding(int id, Trigger trigger, Anchor anchor) {
        this.id = id;
        this.trigger = trigger;
        this.anchor = anchor;
    }

    public Binding movedTo(Anchor a) {
        return new Binding(id, trigger, a);
    }
}
