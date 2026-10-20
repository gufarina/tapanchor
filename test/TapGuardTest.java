import com.tapanchor.app.domain.*;
import java.util.List;

/** Prova das invariantes da SPEC. Rodar: test/run.sh */
public class TapGuardTest {
    static int fails = 0;

    static void check(String name, boolean ok) {
        System.out.println((ok ? "OK   " : "FAIL ") + name);
        if (!ok) fails++;
    }

    public static void main(String[] a) {
        Trigger btnA = Trigger.key(96, 304);
        Trigger unknown1 = Trigger.key(0, 700);
        Trigger unknown2 = Trigger.key(0, 701);
        List<Binding> bs = List.of(
            new Binding(1, btnA, new Anchor(100, 200)),
            new Binding(2, unknown1, new Anchor(300, 400)));

        TapGuard g = new TapGuard();
        TapGuard.Decision d = g.decide(new KeyPress(btnA, true, 0, 0), bs);
        check("I3 aperto toca exatamente no anchor", d.tap != null && d.tap.x == 100 && d.tap.y == 200);
        check("I1 soltar nao toca, mas consome", g.decide(new KeyPress(btnA, false, 0, 50), bs).tap == null
            && g.decide(new KeyPress(btnA, false, 0, 50), bs).consume);
        check("I1 segurar (repeticao) nao toca", g.decide(new KeyPress(btnA, true, 1, 60), bs).tap == null);
        check("botao sem binding segue pro sistema", !g.decide(new KeyPress(Trigger.key(24, 115), true, 0, 70), bs).consume);
        check("keyCode 0 diferenciado por scanCode",
            g.decide(new KeyPress(unknown1, true, 0, 80), bs).tap.x == 300
            && !g.decide(new KeyPress(unknown2, true, 0, 90), bs).consume);

        Trigger side = Trigger.key(275, 0);
        List<Binding> sb = List.of(new Binding(3, side, new Anchor(5, 6)));
        TapGuard m = new TapGuard();
        check("botao lateral (275) toca no anchor", m.decide(new KeyPress(side, true, 0, 0), sb).tap.x == 5);

        TapGuard r = new TapGuard();
        int taps = 0;
        for (int i = 0; i < 30; i++) if (r.decide(new KeyPress(btnA, true, 0, i * 10), bs).tap != null) taps++;
        check("I4 freio: 30 apertos em 300ms viram 10 toques", taps == 10);
        check("I4 freio libera depois de 1s", r.decide(new KeyPress(btnA, true, 0, 1000), bs).tap != null);

        if (fails > 0) { System.out.println(fails + " falha(s)"); System.exit(1); }
        System.out.println("todas as invariantes passaram");
    }
}
