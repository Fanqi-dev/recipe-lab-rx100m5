package com.voxivoid.recipelab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

/**
 * The name editor, full screen and Canvas-drawn like MenuView: a title, the name field, the reason the last OK was
 * refused, the keyboard grid of {@link NameEntry}, and the legend. The placeholder name is drawn dimmed, to say the
 * first character replaces it; a typed name ends in a cursor. Shift and delete are drawn, not typed — the camera font
 * has no arrow glyphs.
 */
public class KeyboardView extends View {
    private static final int ACCENT = SonyUi.ACCENT, INK = SonyUi.ON_ACCENT;

    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG), head = new Paint(Paint.ANTI_ALIAS_FLAG), field = new Paint(Paint.ANTI_ALIAS_FLAG),
            box = new Paint(Paint.ANTI_ALIAS_FLAG), err = new Paint(Paint.ANTI_ALIAS_FLAG), cell = new Paint(Paint.ANTI_ALIAS_FLAG),
            label = new Paint(Paint.ANTI_ALIAS_FLAG), glyph = new Paint(Paint.ANTI_ALIAS_FLAG), rule = new Paint();
    private final RectF r = new RectF();
    private final Path path = new Path();
    private final Legend legend;
    private final UiChrome chrome;
    private final float d;
    private String title = "";
    private NameEntry entry = NameEntry.blank("");
    private Keys.Hints hints = Keys.hints(Keys.H_NAME, Keys.Caps.UNKNOWN);

    public KeyboardView(Context c, AttributeSet a) {
        super(c, a);
        d = UiScale.of(c);
        legend = new Legend(d);chrome=new UiChrome(d);
        bg.setColor(SonyUi.BACKGROUND);
        head.setColor(SonyUi.TEXT); head.setTextSize(SonyUi.TITLE * d); head.setFakeBoldText(false);
        field.setTextSize(SonyUi.INPUT * d); field.setFakeBoldText(false);
        box.setColor(ACCENT); box.setStyle(Paint.Style.STROKE); box.setStrokeWidth(SonyUi.ICON_STROKE * d);
        err.setColor(SonyUi.ATTENTION); err.setTextSize(SonyUi.SECONDARY * d);
        label.setTextSize(SonyUi.BODY * d); label.setFakeBoldText(false); label.setTextAlign(Paint.Align.CENTER);
        glyph.setStyle(Paint.Style.STROKE); glyph.setStrokeWidth(SonyUi.ICON_STROKE * d);
        rule.setColor(SonyUi.RULE);
    }

    /** the display language's typeface ({@link UiFont}) */
    public void setTypeface(Typeface tf) {
        head.setTypeface(tf); field.setTypeface(tf); err.setTypeface(tf); label.setTypeface(tf); legend.setTypeface(tf);
        invalidate();
    }

    public void set(String title, NameEntry entry, Keys.Hints hints) { this.title = title; this.entry = entry; this.hints = hints; invalidate(); }

    @Override
    protected void onMeasure(int w, int hh) { setMeasuredDimension(MeasureSpec.getSize(w), MeasureSpec.getSize(hh)); }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), pad = SonyUi.PAGE_PAD * d;
        float legTop = chrome.footerTop(w,h,legend,hints);
        c.drawRect(0, 0, w, h, bg);
        chrome.header(c,w,title,head);
        float top = SonyUi.HEADER_HEIGHT * d;

        // ---- the field
        float fy = top + 10 * d, fh = 32 * d;
        r.set(pad, fy, w - pad, fy + fh); c.drawRect(r, box);
        String text = CanvasText.fit(field, entry.shown(), w - 2 * pad - 24 * d);
        field.setColor(entry.isPlaceholder() ? SonyUi.MUTED : SonyUi.TEXT);
        float base = fy + fh / 2 - (field.ascent() + field.descent()) / 2, tx = pad + 10 * d;
        c.drawText(text, tx, base, field);
        if (!entry.isPlaceholder()) {                           // the cursor after what is typed
            float cx = tx + field.measureText(text) + 2 * d;
            field.setColor(ACCENT);
            c.drawRect(cx, fy + 7 * d, cx + 2 * d, fy + fh - 7 * d, field);
        }
        float ey = fy + fh + 14 * d;
        if (entry.error() != null) c.drawText(CanvasText.fit(err, entry.error(), w - 2 * pad), pad, ey, err);

        // ---- the keyboard
        float gTop = ey + 8 * d, gBottom = legTop - 10 * d, rows = NameEntry.rows();
        float rh = Math.max(1, Math.min(34 * d, (gBottom - gTop) / rows)), gw = w - 2 * pad, gap = 4 * d;
        for (int row = 0; row < rows; row++) {
            int n = NameEntry.cols(row);
            float cw = gw / n, y = gTop + row * rh;
            for (int col = 0; col < n; col++) {
                boolean on = row == entry.row() && col == entry.col();
                float x = pad + col * cw;
                r.set(x + gap / 2, y + gap / 2, x + cw - gap / 2, y + rh - gap / 2);
                cell.setColor(SonyUi.HEADER);
                if(on)chrome.selection(c,r.left,r.top,r.right,r.bottom,true);else c.drawRect(r,cell);
                int ink = SonyUi.TEXT;
                label.setColor(ink); glyph.setColor(ink);
                drawCell(c, NameEntry.cell(row, col), r.centerX(), r.centerY());
            }
        }

        chrome.footer(c,w,legTop,legend,hints);
    }

    private void drawCell(Canvas c, char k, float cx, float cy) {
        float ty = cy - (label.ascent() + label.descent()) / 2, s = 5 * d;
        switch (k) {
            case NameEntry.OK: c.drawText(Lang.t("key_ok"), cx, ty, label); return;
            case ' ': c.drawText(Lang.t("key_space"), cx, ty, label); return;
            case NameEntry.SHIFT: {                             // an up arrow, filled while letters type in capitals
                path.reset();
                path.moveTo(cx, cy - s); path.lineTo(cx + s, cy); path.lineTo(cx + s / 2, cy); path.lineTo(cx + s / 2, cy + s);
                path.lineTo(cx - s / 2, cy + s); path.lineTo(cx - s / 2, cy); path.lineTo(cx - s, cy); path.close();
                glyph.setStyle(entry.upper() ? Paint.Style.FILL_AND_STROKE : Paint.Style.STROKE);
                c.drawPath(path, glyph);
                glyph.setStyle(Paint.Style.STROKE);
                return;
            }
            case NameEntry.BACK: {                              // a key with a cross: delete
                path.reset();
                path.moveTo(cx - 1.6f * s, cy); path.lineTo(cx - 0.8f * s, cy - s); path.lineTo(cx + 1.4f * s, cy - s);
                path.lineTo(cx + 1.4f * s, cy + s); path.lineTo(cx - 0.8f * s, cy + s); path.close();
                c.drawPath(path, glyph);
                c.drawLine(cx - 0.1f * s, cy - 0.45f * s, cx + 0.8f * s, cy + 0.45f * s, glyph);
                c.drawLine(cx - 0.1f * s, cy + 0.45f * s, cx + 0.8f * s, cy - 0.45f * s, glyph);
                return;
            }
            default:
                char shown = Character.isLetter(k) && !entry.upper() ? Character.toLowerCase(k) : k;
                c.drawText(String.valueOf(shown), cx, ty, label);
        }
    }
}
