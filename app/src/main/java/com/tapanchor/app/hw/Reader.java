package com.tapanchor.app.hw;

import android.os.SystemClock;
import android.system.Os;
import android.system.OsConstants;
import android.system.StructPollfd;
import android.view.InputDevice;
import android.view.InputEvent;
import android.view.MotionEvent;
import com.tapanchor.app.domain.Anchor;
import com.tapanchor.app.domain.Binding;
import com.tapanchor.app.domain.KeyPress;
import com.tapanchor.app.domain.TapGuard;
import com.tapanchor.app.domain.Trigger;
import com.tapanchor.app.infra.BindingStore;
import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * Roda no processo do Shizuku (permissão de shell). Toma o controle só para si (EVIOCGRAB): o Android
 * deixa de receber o controle, então nenhum botão vira "voltar" nem move a setinha. Cada botão lido do
 * hardware passa pelo TapGuard (I1-I5) e, se ligado a uma âncora, vira UM toque injetado.
 */
public final class Reader extends IReader.Stub {
    private static final int EVIOCGRAB = 0x40044590;
    private static final int EV_KEY = 1;

    private final TapGuard guard = new TapGuard();
    private volatile List<Binding> bindings = new ArrayList<>();
    private volatile boolean paused;
    private volatile String device;
    private volatile IKeyListener listener;
    private Thread loop;

    @Override
    public void destroy() {
        stop();
        System.exit(0);
    }

    @Override
    public synchronized void start(String deviceName, IKeyListener l) {
        listener = l;
        device = deviceName;
        if (loop == null || !loop.isAlive()) {
            loop = new Thread(this::run, "tapanchor-reader");
            loop.start();
        }
    }

    @Override
    public void stop() {
        device = null;
    }

    @Override
    public void setBindings(String serialized) {
        bindings = BindingStore.parse(serialized);
    }

    @Override
    public void setPaused(boolean p) {
        paused = p;
    }

    /** Libera a sobreposição dos marcadores sem o usuário ir nas configurações. */
    @Override
    public void allowOverlay(String pkg) {
        sh("appops", "set", pkg, "SYSTEM_ALERT_WINDOW", "allow");
    }

    private static String sh(String... cmd) {
        try {
            Process p = Runtime.getRuntime().exec(cmd);
            String out = new BufferedReader(new InputStreamReader(p.getInputStream())).readLine();
            p.waitFor();
            return out == null ? "" : out.trim();
        } catch (Exception e) {
            return "";
        }
    }

    private void run() {
        byte[] ev = new byte[24];
        while (device != null) {
            String node = findNode(device);
            if (node == null) { SystemClock.sleep(1500); continue; }
            FileDescriptor fd = null;
            try {
                fd = Os.open(node, OsConstants.O_RDONLY, 0);
                grab(fd);
                notifyGrab(true);
                StructPollfd p = new StructPollfd();
                p.fd = fd;
                p.events = (short) OsConstants.POLLIN;
                String mine = device;
                while (device != null && device.equals(mine)) {
                    if (Os.poll(new StructPollfd[] {p}, 400) <= 0) continue;
                    if (Os.read(fd, ev, 0, 24) < 24) continue;
                    int type = u16(ev, 16), code = u16(ev, 18), value = ev[20];
                    if (type == EV_KEY && value != 2) onKey(code, value == 1); // 2 = repetição automática: ignorada (I1)
                }
            } catch (Exception e) {
                SystemClock.sleep(1000); // controle desligou ou dormiu: tenta de novo
            } finally {
                if (fd != null) try { Os.close(fd); } catch (Exception ignored) {} // fechar solta o controle
                notifyGrab(false);
            }
        }
    }

    private void onKey(int code, boolean down) {
        IKeyListener l = listener;
        if (l != null) try { l.onKey(code, down); } catch (Exception ignored) {}
        if (paused) return;
        TapGuard.Decision d = guard.decide(new KeyPress(Trigger.key(code, 0), down, 0, SystemClock.uptimeMillis()), bindings);
        if (d.tap != null) tap(d.tap);
    }

    private void notifyGrab(boolean on) {
        IKeyListener l = listener;
        if (l != null) try { l.onGrab(on); } catch (Exception ignored) {}
    }

    private static int u16(byte[] b, int i) {
        return (b[i] & 0xff) | (b[i + 1] & 0xff) << 8;
    }

    /** Acha o /dev/input/eventN pelo nome do aparelho (o nome de sistema não é legível pelo shell). */
    private static String findNode(String name) {
        try {
            Process gp = Runtime.getRuntime().exec(new String[] {"getevent", "-p"});
            BufferedReader r = new BufferedReader(new InputStreamReader(gp.getInputStream()));
            String line, cur = null, found = null;
            while ((line = r.readLine()) != null) {
                if (line.startsWith("add device")) cur = line.substring(line.indexOf("/dev/"));
                else if (line.trim().startsWith("name:") && line.contains("\"" + name + "\"")) found = cur;
            }
            gp.waitFor();
            return found;
        } catch (Exception e) {
            return null;
        }
    }

    private static void grab(FileDescriptor fd) throws Exception {
        Object os = Class.forName("libcore.io.Libcore").getField("os").get(null);
        try {
            os.getClass().getMethod("ioctlInt", FileDescriptor.class, int.class).invoke(os, fd, EVIOCGRAB);
        } catch (NoSuchMethodException old) { // Android 7-9
            Class<?> ref = Class.forName("android.util.MutableInt");
            os.getClass().getMethod("ioctlInt", FileDescriptor.class, int.class, ref)
                .invoke(os, fd, EVIOCGRAB, ref.getConstructor(int.class).newInstance(1));
        }
    }

    private static Object inputManager;
    private static Method inject;

    private static void ensureInject() throws Exception {
        if (inject == null) {
            Class<?> c;
            try {
                c = Class.forName("android.hardware.input.InputManagerGlobal"); // Android 14+
            } catch (ClassNotFoundException e) {
                c = Class.forName("android.hardware.input.InputManager");
            }
            inputManager = c.getMethod("getInstance").invoke(null);
            inject = c.getMethod("injectInputEvent", InputEvent.class, int.class);
        }
    }

    private static void tap(Anchor a) {
        try {
            ensureInject();
            long t = SystemClock.uptimeMillis();
            for (int act : new int[] {MotionEvent.ACTION_DOWN, MotionEvent.ACTION_UP}) {
                MotionEvent e = MotionEvent.obtain(t, SystemClock.uptimeMillis(), act, a.x, a.y, 0);
                e.setSource(InputDevice.SOURCE_TOUCHSCREEN);
                inject.invoke(inputManager, e, 0);
                e.recycle();
            }
        } catch (Exception ignored) {}
    }
}
