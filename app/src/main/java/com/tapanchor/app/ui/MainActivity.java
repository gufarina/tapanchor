package com.tapanchor.app.ui;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.view.Gravity;
import android.view.InputDevice;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import com.tapanchor.app.domain.Binding;
import com.tapanchor.app.infra.AnchorService;
import com.tapanchor.app.infra.Names;
import java.util.LinkedHashSet;
import java.util.Set;
import rikka.shizuku.Shizuku;

/** Console: logo, passo a passo do Shizuku, escolha do controle, sinal (detector) e âncoras. */
public final class MainActivity extends Activity {
    private static final String SHIZUKU_PKG = "moe.shizuku.privileged.api";
    private LinearLayout left, right;

    private final Shizuku.OnRequestPermissionResultListener permResult = (code, result) -> runOnUiThread(this::render);
    private final Shizuku.OnBinderReceivedListener binderUp = () -> runOnUiThread(this::render);

    @Override
    protected void onCreate(Bundle s) {
        super.onCreate(s);
        getWindow().setStatusBarColor(Ui.INK);
        getWindow().setNavigationBarColor(Ui.INK);
        Shizuku.addRequestPermissionResultListener(permResult);
        Shizuku.addBinderReceivedListenerSticky(binderUp);
        build();
    }

    @Override
    protected void onDestroy() {
        Shizuku.removeRequestPermissionResultListener(permResult);
        Shizuku.removeBinderReceivedListener(binderUp);
        super.onDestroy();
    }

    @Override
    public void onConfigurationChanged(Configuration c) {
        super.onConfigurationChanged(c);
        build();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AnchorService.onChange = () -> runOnUiThread(this::render);
        render();
    }

    @Override
    protected void onPause() {
        AnchorService.onChange = null;
        AnchorService svc = AnchorService.instance;
        if (svc != null && svc.isLearning()) svc.stopLearning();
        super.onPause();
    }

    private void build() {
        boolean wide = getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE;
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(wide ? LinearLayout.HORIZONTAL : LinearLayout.VERTICAL);
        int p = dp(18);
        root.setPadding(p, p, p, p);
        left = column();
        right = column();
        LinearLayout.LayoutParams lw = wide ? new LinearLayout.LayoutParams(0, -2, 1f) : new LinearLayout.LayoutParams(-1, -2);
        LinearLayout.LayoutParams rw = wide ? new LinearLayout.LayoutParams(0, -2, 1.35f) : new LinearLayout.LayoutParams(-1, -2);
        if (wide) rw.leftMargin = dp(18); else rw.topMargin = dp(18);
        root.addView(left, lw);
        root.addView(right, rw);
        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Ui.AMBER);
        scroll.addView(root);
        setContentView(scroll);
        render();
    }

    // --- estado do Shizuku ---

    private boolean shizukuInstalled() {
        try {
            getPackageManager().getPackageInfo(SHIZUKU_PKG, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    private boolean shizukuReady() {
        return Shizuku.pingBinder() && Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
    }

    private void ensureService() {
        if (AnchorService.instance != null) { AnchorService.instance.connect(); return; }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {"android.permission.POST_NOTIFICATIONS"}, 2);
        }
        Intent i = new Intent(this, AnchorService.class);
        if (Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
    }

    // --- tela ---

    private void render() {
        if (left == null) return;
        left.removeAllViews();
        right.removeAllViews();
        left.addView(Ui.logo(this), new LinearLayout.LayoutParams(-1, -2));
        add(left, Ui.strip(this), 12);

        if (!shizukuReady()) {
            add(left, signal("DESLIGADO", "falta liberar o acesso ao controle", "○ parado"), 12);
            shizukuSteps();
        } else {
            ensureService();
            AnchorService svc = AnchorService.instance;
            if (svc == null || !svc.readerReady()) {
                add(left, signal("LIGANDO", "conectando ao controle...", "◌ aguarde"), 12);
            } else if (svc.device() == null) {
                add(left, signal("ESCOLHA O CONTROLE", "o TapAnchor vai cuidar dele", "● pronto"), 12);
                devices(svc);
            } else {
                controls(svc);
                anchors(svc);
            }
        }
        add(left, mono("FREIO 10/s // SEM MACRO // SEM TIMER // 1 APERTO = 1 TOQUE", Ui.INK), 14);
    }

    private void shizukuSteps() {
        right.addView(Ui.text(this, "UMA PERMISSÃO SÓ", Ui.title(this), 17, Ui.INK));
        add(right, body("O Android não deixa um app comum ler o controle antes do sistema. Por isso o seu botão "
            + "virava \"voltar\" e dependia da setinha. O Shizuku é um app gratuito e de código aberto que libera "
            + "esse acesso, só com a sua autorização."), 6);
        add(right, body("Com ele, o TapAnchor cuida do controle sozinho: cada botão faz só o que você mandar, "
            + "e a setinha do controle some enquanto o TapAnchor estiver ativo. Ele não lê a tela, não lê "
            + "mensagens e não usa internet."), 6);
        boolean installed = shizukuInstalled();
        add(right, step((installed ? "✓ " : "") + "Instale o Shizuku (Play Store)."), 10);
        if (!installed) {
            add(right, Ui.button(this, "BAIXAR SHIZUKU", Ui.INK, Ui.INK, false, () -> startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=" + SHIZUKU_PKG)))), 6);
        }
        boolean running = Shizuku.pingBinder();
        add(right, step((running ? "✓ " : "") + "Abra o Shizuku e toque em \"Iniciar\" via Depuração sem fio. "
            + "Ele mostra o passo a passo na tela (é feito uma vez; depois de reiniciar o celular, só tocar em Iniciar de novo)."), 6);
        if (installed && !running) {
            add(right, Ui.button(this, "ABRIR SHIZUKU", Ui.INK, Ui.INK, false, () -> {
                Intent i = getPackageManager().getLaunchIntentForPackage(SHIZUKU_PKG);
                if (i != null) startActivity(i);
            }), 6);
        }
        add(right, step("Volte aqui e toque em PERMITIR."), 6);
        if (running) {
            add(right, Ui.button(this, "PERMITIR", Ui.PINK, Ui.INK, true, () -> Shizuku.requestPermission(1)), 8);
        }
    }

    private void devices(AnchorService svc) {
        right.addView(Ui.text(this, "CONTROLES CONECTADOS", Ui.title(this), 17, Ui.INK));
        Set<String> names = new LinkedHashSet<>();
        for (int id : InputDevice.getDeviceIds()) {
            InputDevice d = InputDevice.getDevice(id);
            if (d == null || d.isVirtual()) continue;
            if (Build.VERSION.SDK_INT >= 29 && !d.isExternal()) continue;
            names.add(d.getName());
        }
        if (names.isEmpty()) add(right, body("Nenhum controle Bluetooth ligado. Ligue o controle e volte aqui."), 8);
        for (String n : names) add(right, Ui.button(this, "USAR " + n.toUpperCase(), Ui.INK, Ui.INK, false, () -> svc.useDevice(n)), 8);
    }

    private void controls(AnchorService svc) {
        Integer code = svc.lastCode();
        if (svc.isLearning()) {
            add(left, signal("APERTE O BOTÃO", "qualquer botão do " + svc.device(), "◉ aprendendo"), 12);
            add(left, Ui.button(this, "CANCELAR", Ui.INK, Ui.INK, false, svc::stopLearning), 10);
            return;
        }
        add(left, signal(code == null ? "AGUARDANDO" : Names.of(code).toUpperCase(),
            svc.device() + (svc.grabbed() ? "" : " · desconectado"),
            svc.grabbed() ? "● controle tomado" : "○ ligue o controle"), 12);
        LinearLayout row = new LinearLayout(this);
        row.addView(Ui.button(this, "+ ANCORAR BOTÃO", Ui.PINK, Ui.INK, true, svc::startLearning), weight(0));
        if (!svc.bindings().isEmpty()) {
            row.addView(Ui.button(this, svc.isEditing() ? "TRAVAR" : "MOVER", Ui.INK, Ui.INK, false,
                () -> svc.setEditing(!svc.isEditing())), weight(dp(8)));
        }
        add(left, row, 10);
        add(left, Ui.button(this, "SOLTAR CONTROLE", Ui.INK, Ui.INK, false, svc::release), 8);
    }

    private View signal(String big, String small, String status) {
        LinearLayout box = column();
        box.setBackground(new Ui.Chamfer(Ui.INK, dp(14), 0));
        box.setPadding(dp(14), dp(12), dp(14), dp(14));
        LinearLayout head = new LinearLayout(this);
        head.addView(mono("SINAL", Ui.AMBER), new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(mono(status, Ui.PINK));
        box.addView(head);
        add(box, Ui.text(this, big, Ui.title(this), 22, Ui.AMBER), 6);
        add(box, mono(small, 0xBFFFA51F), 2);
        return box;
    }

    private void anchors(AnchorService svc) {
        LinearLayout head = new LinearLayout(this);
        head.setGravity(Gravity.BOTTOM);
        head.addView(Ui.text(this, "ÂNCORAS", Ui.title(this), 17, Ui.INK), new LinearLayout.LayoutParams(0, -2, 1f));
        head.addView(mono(String.format("%02d / slots", svc.bindings().size()), Ui.INK));
        right.addView(head);
        if (svc.bindings().isEmpty()) {
            add(right, step("Toque em + ANCORAR BOTÃO e aperte o botão do controle."), 8);
            add(right, step("Um círculo aparece no meio da tela. Abra o jogo e arraste com o dedo até onde quer tocar."), 6);
            add(right, step("Toque em TRAVAR POSIÇÃO. Pronto: cada aperto vira um toque ali."), 6);
            add(right, step("Para mover de novo, toque na etiqueta rosa \"01 ✎\" ao lado do círculo."), 6);
        }
        for (Binding b : svc.bindings()) add(right, slot(svc, b), 8);
    }

    private View slot(AnchorService svc, Binding b) {
        LinearLayout s = new LinearLayout(this);
        s.setGravity(Gravity.CENTER_VERTICAL);
        s.setBackground(new Ui.Chamfer(Ui.INK, dp(12), 0));
        s.setPadding(dp(14), dp(10), dp(12), dp(10));
        s.addView(Ui.text(this, String.format("%02d", b.id), Ui.title(this), 22, Ui.PINK), new LinearLayout.LayoutParams(dp(40), -2));
        LinearLayout mid = column();
        mid.addView(Ui.text(this, Names.of(b.trigger.keyCode).toUpperCase(), Ui.title(this), 15, Ui.AMBER));
        add(mid, mono("código " + b.trigger.keyCode, 0xBFFFA51F), 3);
        s.addView(mid, new LinearLayout.LayoutParams(0, -2, 1f));
        TextView xy = mono(String.format("X %04d\nY %04d", b.anchor.x, b.anchor.y), Ui.AMBER);
        xy.setGravity(Gravity.END);
        s.addView(xy);
        TextView del = Ui.text(this, "✕", Ui.title(this), 18, Ui.PINK);
        del.setPadding(dp(14), dp(6), dp(4), dp(6));
        del.setContentDescription("Remover âncora " + b.id);
        del.setOnClickListener(v -> svc.remove(b.id));
        s.addView(del);
        return s;
    }

    private LinearLayout column() {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        return c;
    }

    private TextView step(String s) {
        TextView t = body("▸ " + s);
        t.setTypeface(Ui.title(this));
        return t;
    }

    private TextView body(String s) {
        TextView t = Ui.text(this, s, Ui.mono(this), 12, Ui.INK);
        t.setLineSpacing(0, 1.15f);
        return t;
    }

    private TextView mono(String s, int color) {
        return Ui.text(this, s, Ui.mono(this), 11, color);
    }

    private void add(LinearLayout parent, View v, int topDp) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(-1, -2);
        lp.topMargin = dp(topDp);
        parent.addView(v, lp);
    }

    private LinearLayout.LayoutParams weight(int leftMargin) {
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, -2, 1f);
        lp.leftMargin = leftMargin;
        return lp;
    }

    private int dp(int v) {
        return Ui.dp(this, v);
    }
}
