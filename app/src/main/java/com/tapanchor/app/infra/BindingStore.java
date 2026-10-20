package com.tapanchor.app.infra;

import android.content.Context;
import android.content.SharedPreferences;
import com.tapanchor.app.domain.Anchor;
import com.tapanchor.app.domain.Binding;
import com.tapanchor.app.domain.Trigger;
import java.util.ArrayList;
import java.util.List;

/** Guarda os Bindings como "id,código,x,y;..." (código = botão lido do hardware). Também vai assim ao leitor. */
public final class BindingStore {
    private final SharedPreferences prefs;

    public BindingStore(Context c) {
        prefs = c.getSharedPreferences("anchors", Context.MODE_PRIVATE);
    }

    public List<Binding> load() {
        return parse(prefs.getString("all", ""));
    }

    public void save(List<Binding> bindings) {
        prefs.edit().putString("all", serialize(bindings)).apply();
    }

    public String device() {
        return prefs.getString("device", null);
    }

    public void setDevice(String name) {
        prefs.edit().putString("device", name).apply();
    }

    public static List<Binding> parse(String s) {
        List<Binding> out = new ArrayList<>();
        for (String row : s.split(";")) {
            String[] f = row.split(",");
            if (f.length != 4) continue;
            out.add(new Binding(Integer.parseInt(f[0]), Trigger.key(Integer.parseInt(f[1]), 0),
                new Anchor(Integer.parseInt(f[2]), Integer.parseInt(f[3]))));
        }
        return out;
    }

    public static String serialize(List<Binding> bindings) {
        StringBuilder sb = new StringBuilder();
        for (Binding b : bindings) {
            sb.append(b.id).append(',').append(b.trigger.keyCode).append(',')
              .append(b.anchor.x).append(',').append(b.anchor.y).append(';');
        }
        return sb.toString();
    }
}
