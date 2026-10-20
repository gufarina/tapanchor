package com.tapanchor.app.ui;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.tapanchor.app.R;

/** Sistema visual "etiqueta técnica": paleta, fontes e formas. */
public final class Ui {
    public static final int AMBER = 0xFFFFA51F;
    public static final int INK = 0xFF0A0A0A;
    public static final int PINK = 0xFFFF2E88;
    public static final int GAME = 0xFF2A2416;

    private static Typeface title, mono, monoBold;

    private Ui() {}

    public static Typeface title(Context c) { return title != null ? title : (title = font(c, "chakra_bold")); }
    public static Typeface mono(Context c) { return mono != null ? mono : (mono = font(c, "space_mono")); }
    public static Typeface monoBold(Context c) { return monoBold != null ? monoBold : (monoBold = font(c, "space_mono_bold")); }

    private static Typeface font(Context c, String name) {
        return Typeface.createFromAsset(c.getAssets(), "fonts/" + name + ".ttf");
    }

    public static int dp(Context c, float v) {
        return Math.round(v * c.getResources().getDisplayMetrics().density);
    }

    public static TextView text(Context c, String s, Typeface tf, float sp, int color) {
        TextView t = new TextView(c);
        t.setText(s);
        t.setTypeface(tf);
        t.setTextSize(sp);
        t.setTextColor(color);
        t.setIncludeFontPadding(false);
        return t;
    }

    /** Botão chanfrado. filled = fundo cheio; senão só contorno. */
    public static TextView button(Context c, String label, int bg, int fg, boolean filled, Runnable action) {
        TextView b = text(c, label, title(c), 15, fg);
        b.setGravity(Gravity.CENTER);
        int p = dp(c, 12);
        b.setPadding(p, p, p, p);
        b.setBackground(new Chamfer(bg, dp(c, 10), filled ? 0 : dp(c, 2)));
        b.setOnClickListener(v -> action.run());
        b.setClickable(true);
        return b;
    }

    /** Logo em etiqueta: bloco preto com a âncora + nome em vetor (desenhado da Tomorrow Black). */
    public static View logo(Context c) {
        LinearLayout tag = new LinearLayout(c);
        tag.setOrientation(LinearLayout.HORIZONTAL);
        tag.setBackground(new Chamfer(INK, dp(c, 14), 0));
        int p = dp(c, 10);
        tag.setPadding(p, p, dp(c, 18), p);
        tag.setGravity(Gravity.CENTER_VERTICAL);

        View mark = new View(c);
        mark.setBackground(c.getDrawable(R.drawable.ic_anchor_mark));
        tag.addView(mark, new LinearLayout.LayoutParams(dp(c, 48), dp(c, 48)));

        LinearLayout words = new LinearLayout(c);
        words.setOrientation(LinearLayout.VERTICAL);
        words.setPadding(dp(c, 12), 0, 0, 0);
        android.widget.ImageView name = new android.widget.ImageView(c); // nome em vetor: não quebra nem segue a fonte do sistema
        name.setImageResource(R.drawable.wordmark);
        name.setAdjustViewBounds(true);
        name.setScaleType(android.widget.ImageView.ScaleType.FIT_START);
        name.setContentDescription("TapAnchor");
        words.addView(name, new LinearLayout.LayoutParams(-1, -2));
        TextView rule = text(c, "1 PRESS = 1 TAP", monoBold(c), 11, PINK);
        rule.setPadding(0, dp(c, 4), 0, 0);
        words.addView(rule);
        tag.addView(words, new LinearLayout.LayoutParams(0, -2, 1f));
        return tag;
    }

    /** Fita de risco + código de barras, a "matéria" da etiqueta. */
    public static View strip(Context c) {
        LinearLayout row = new LinearLayout(c);
        row.setGravity(Gravity.CENTER_VERTICAL);
        View hz = new View(c);
        hz.setBackground(new Hazard(dp(c, 6)));
        row.addView(hz, new LinearLayout.LayoutParams(0, dp(c, 12), 1));
        View bc = new View(c);
        bc.setBackground(new Barcode(dp(c, 1)));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(dp(c, 64), dp(c, 18));
        lp.leftMargin = dp(c, 10);
        row.addView(bc, lp);
        return row;
    }

    /** Retângulo com dois cantos chanfrados (superior direito e inferior esquerdo). */
    public static final class Chamfer extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int cut;

        public Chamfer(int color, int cut, int stroke) {
            this.cut = cut;
            paint.setColor(color);
            if (stroke > 0) {
                paint.setStyle(Paint.Style.STROKE);
                paint.setStrokeWidth(stroke);
            }
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            float s = paint.getStyle() == Paint.Style.STROKE ? paint.getStrokeWidth() / 2 : 0;
            float l = b.left + s, t = b.top + s, r = b.right - s, btm = b.bottom - s;
            Path p = new Path();
            p.moveTo(l, t);
            p.lineTo(r - cut, t);
            p.lineTo(r, t + cut);
            p.lineTo(r, btm);
            p.lineTo(l + cut, btm);
            p.lineTo(l, btm - cut);
            p.close();
            canvas.drawPath(p, paint);
        }

        @Override public void setAlpha(int a) { paint.setAlpha(a); }
        @Override public void setColorFilter(ColorFilter f) { paint.setColorFilter(f); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    static final class Hazard extends Drawable {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int w;

        Hazard(int w) {
            this.w = w;
            paint.setColor(INK);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            canvas.save();
            canvas.clipRect(b);
            for (int x = b.left - b.height(); x < b.right; x += w * 2) {
                Path p = new Path();
                p.moveTo(x, b.bottom);
                p.lineTo(x + w, b.bottom);
                p.lineTo(x + w + b.height(), b.top);
                p.lineTo(x + b.height(), b.top);
                p.close();
                canvas.drawPath(p, paint);
            }
            canvas.restore();
        }

        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(ColorFilter f) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }

    static final class Barcode extends Drawable {
        private static final int[] BARS = {3, 1, 2, 1, 4, 1, 2, 3, 1, 2, 1, 3, 2, 1, 4, 2, 1};
        private final Paint paint = new Paint();
        private final int unit;

        Barcode(int unit) {
            this.unit = unit;
            paint.setColor(INK);
        }

        @Override
        public void draw(Canvas canvas) {
            Rect b = getBounds();
            int x = b.left;
            for (int i = 0; i < BARS.length && x < b.right; i++) {
                if (i % 2 == 0) canvas.drawRect(x, b.top, x + BARS[i] * unit, b.bottom, paint);
                x += BARS[i] * unit + unit;
            }
        }

        @Override public void setAlpha(int a) {}
        @Override public void setColorFilter(ColorFilter f) {}
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
