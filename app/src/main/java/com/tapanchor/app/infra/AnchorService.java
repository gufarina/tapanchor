package com.tapanchor.app.infra;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.ServiceInfo;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import com.tapanchor.app.domain.Anchor;
import com.tapanchor.app.domain.Binding;
import com.tapanchor.app.domain.TapGuard;
import com.tapanchor.app.domain.Trigger;
import com.tapanchor.app.hw.IKeyListener;
import com.tapanchor.app.hw.IReader;
import com.tapanchor.app.hw.Reader;
import com.tapanchor.app.ui.MainActivity;
import com.tapanchor.app.ui.Ui;
import java.util.ArrayList;
import java.util.List;
import rikka.shizuku.Shizuku;

/**
 * Mantém as âncoras na tela e liga o app ao leitor de hardware (que roda pelo Shizuku).
 * Enquanto ensina um botão ou move marcadores, o leitor fica pausado: nenhum toque é disparado.
 */
public final class AnchorService extends Service {
    public static AnchorService instance;
    public static Runnable onChange;

    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<View> overlays = new ArrayList<>();
    private BindingStore store;
    private WindowManager wm;
    private List<Binding> bindings = new ArrayList<>();
    private IReader reader;
    private boolean learning, editing, grabbed;
    private Integer lastCode;

    private final Shizuku.UserServiceArgs args = new Shizuku.UserServiceArgs(
        new ComponentName("com.tapanchor.app", Reader.class.getName()))
        .daemon(false).processNameSuffix("reader").version(9);

    private final ServiceConnection conn = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName n, IBinder b) {
            reader = IReader.Stub.asInterface(b);
            try {
                reader.allowOverlay(getPackageName());
                pushBindings();
                if (store.device() != null) reader.start(store.device(), keys);
            } catch (Exception ignored) {}
            main.postDelayed(AnchorService.this::redraw, 300); // sobreposição acabou de ser liberada
            notifyChange();
        }

        @Override
        public void onServiceDisconnected(ComponentName n) {
            reader = null;
            grabbed = false;
            notifyChange();
        }
    };

    private final IKeyListener keys = new IKeyListener.Stub() {
        @Override
        public void onKey(int code, boolean down) {
            if (down) main.post(() -> pressed(code));
        }

        @Override
        public void onGrab(boolean on) {
            main.post(() -> { grabbed = on; notifyChange(); });
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        wm = (WindowManager) getSystemService(WINDOW_SERVICE);
        store = new BindingStore(this);
        bindings = store.load();
        foreground();
        connect();
        redraw();
    }

    @Override
    public int onStartCommand(Intent i, int flags, int id) {
        connect();
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        clearOverlays();
        try { if (reader != null) reader.stop(); } catch (Exception ignored) {}
        try { Shizuku.unbindUserService(args, conn, true); } catch (Exception ignored) {}
        instance = null;
        notifyChange();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent i) {
        return null;
    }

    // --- estado lido pela tela ---

    public boolean readerReady() { return reader != null; }
    public boolean grabbed() { return grabbed; }
    public String device() { return store.device(); }
    public boolean isLearning() { return learning; }
    public boolean isEditing() { return editing; }
    public Integer lastCode() { return lastCode; }
    public List<Binding> bindings() { return bindings; }

    // --- comandos da tela ---

    public void connect() {
        if (reader != null || !Shizuku.pingBinder()) return;
        if (Shizuku.checkSelfPermission() != android.content.pm.PackageManager.PERMISSION_GRANTED) return;
        try { Shizuku.bindUserService(args, conn); } catch (Exception ignored) {}
    }

    public void useDevice(String name) {
        store.setDevice(name);
        try { if (reader != null) reader.start(name, keys); } catch (Exception ignored) {}
        notifyChange();
    }

    public void release() {
        store.setDevice(null);
        try { if (reader != null) reader.stop(); } catch (Exception ignored) {}
        notifyChange();
    }

    public void startLearning() { learning = true; pause(); notifyChange(); }
    public void stopLearning() { learning = false; pause(); notifyChange(); }

    public void setEditing(boolean on) {
        editing = on;
        pause();
        redraw();
        notifyChange();
    }

    public void remove(int id) {
        List<Binding> next = new ArrayList<>();
        for (Binding b : bindings) if (b.id != id) next.add(b);
        commit(next);
    }

    // --- interno ---

    private void pressed(int code) {
        lastCode = code;
        if (learning) learn(Trigger.key(code, 0));
        notifyChange();
    }

    private void learn(Trigger t) {
        learning = false;
        if (TapGuard.find(t, bindings) == null) { // I5: um botão, um ponto
            int id = 1;
            for (Binding b : bindings) id = Math.max(id, b.id + 1);
            DisplayMetrics m = getResources().getDisplayMetrics();
            List<Binding> next = new ArrayList<>(bindings);
            next.add(new Binding(id, t, new Anchor(m.widthPixels / 2, m.heightPixels / 2)));
            editing = true;
            commit(next);
        }
        pause();
    }

    private void pause() {
        try { if (reader != null) reader.setPaused(learning || editing); } catch (Exception ignored) {}
    }

    private void commit(List<Binding> next) {
        bindings = next;
        store.save(next);
        pushBindings();
        redraw();
        notifyChange();
    }

    private void pushBindings() {
        try { if (reader != null) reader.setBindings(BindingStore.serialize(bindings)); } catch (Exception ignored) {}
    }

    private void notifyChange() {
        if (onChange != null) onChange.run();
    }

    private void foreground() {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (Build.VERSION.SDK_INT >= 26) {
            nm.createNotificationChannel(new NotificationChannel("anchor", "TapAnchor ativo", NotificationManager.IMPORTANCE_LOW));
        }
        PendingIntent open = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), PendingIntent.FLAG_IMMUTABLE);
        Notification.Builder nb = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, "anchor") : new Notification.Builder(this);
        Notification n = nb.setSmallIcon(com.tapanchor.app.R.drawable.ic_anchor_mark)
            .setContentTitle("TapAnchor ativo")
            .setContentText("Toque para abrir. 1 aperto = 1 toque.")
            .setContentIntent(open).setOngoing(true).build();
        if (Build.VERSION.SDK_INT >= 34) startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        else startForeground(1, n);
    }

    // --- marcadores ---

    private void clearOverlays() {
        for (View v : overlays) wm.removeView(v);
        overlays.clear();
    }

    private int dp(int v) {
        return Ui.dp(this, v);
    }

    private WindowManager.LayoutParams params(boolean touchable) {
        int type = Build.VERSION.SDK_INT >= 26 ? WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : WindowManager.LayoutParams.TYPE_PHONE;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT, WindowManager.LayoutParams.WRAP_CONTENT, type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
                | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS
                | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN
                | (touchable ? 0 : WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE),
            PixelFormat.TRANSLUCENT);
        lp.gravity = Gravity.TOP | Gravity.START;
        if (Build.VERSION.SDK_INT >= 30) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        } else if (Build.VERSION.SDK_INT >= 28) {
            lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
        return lp;
    }

    private void redraw() {
        clearOverlays();
        if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) return;
        for (Binding b : bindings) addMarker(b);
        if (editing) addLockButton();
    }

    private void addMarker(Binding b) {
        int size = dp(editing ? 40 : 28); // travado: discreto; movendo: fácil de pegar com o dedo
        View v = new View(this);
        GradientDrawable ring = new GradientDrawable();
        ring.setShape(GradientDrawable.OVAL);
        ring.setColor(editing ? 0x66000000 : 0x22000000);
        ring.setStroke(dp(2), editing ? Ui.PINK : Ui.AMBER);
        v.setBackground(ring);
        v.setAlpha(editing ? 1f : 0.6f);

        WindowManager.LayoutParams lp = params(editing); // travado: o toque atravessa o círculo
        lp.width = size;
        lp.height = size;
        lp.x = b.anchor.x - size / 2;
        lp.y = b.anchor.y - size / 2;

        if (editing) {
            v.setOnTouchListener(new View.OnTouchListener() {
                float rawX, rawY;
                int startX, startY;

                @Override
                public boolean onTouch(View view, MotionEvent ev) {
                    switch (ev.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                            rawX = ev.getRawX(); rawY = ev.getRawY();
                            startX = lp.x; startY = lp.y;
                            return true;
                        case MotionEvent.ACTION_MOVE:
                            lp.x = startX + Math.round(ev.getRawX() - rawX);
                            lp.y = startY + Math.round(ev.getRawY() - rawY);
                            wm.updateViewLayout(view, lp);
                            return true;
                        case MotionEvent.ACTION_UP:
                            int[] loc = new int[2];
                            view.getLocationOnScreen(loc); // centro real na tela, já com recorte e barras
                            moveBinding(b.id, new Anchor(loc[0] + size / 2, loc[1] + size / 2));
                            return true;
                    }
                    return false;
                }
            });
        }
        wm.addView(v, lp);
        overlays.add(v);
        if (!editing) addUnlockTag(b, size);
    }

    /** Etiqueta "01 ✎" ao lado do marcador travado: tocar destrava e volta ao modo mover. */
    private void addUnlockTag(Binding b, int size) {
        TextView tag = Ui.text(this, String.format("%02d ✎", b.id), Ui.monoBold(this), 11, Ui.INK);
        tag.setBackground(new Ui.Chamfer(Ui.PINK, dp(5), 0));
        tag.setPadding(dp(6), dp(3), dp(8), dp(3));
        tag.setOnClickListener(x -> setEditing(true));
        WindowManager.LayoutParams lp = params(true);
        lp.x = b.anchor.x + size / 2 - dp(4);
        lp.y = b.anchor.y - size / 2 - dp(16);
        wm.addView(tag, lp);
        overlays.add(tag);
    }

    private void moveBinding(int id, Anchor a) {
        List<Binding> next = new ArrayList<>();
        for (Binding b : bindings) next.add(b.id == id ? b.movedTo(a) : b);
        bindings = next;
        store.save(next);
        pushBindings();
        notifyChange();
    }

    private void addLockButton() {
        TextView v = Ui.button(this, "TRAVAR POSIÇÃO", Ui.PINK, Ui.INK, true, () -> setEditing(false));
        WindowManager.LayoutParams lp = params(true);
        lp.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        lp.y = dp(24);
        wm.addView(v, lp);
        overlays.add(v);
    }
}
