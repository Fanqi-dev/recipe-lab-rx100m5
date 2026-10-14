package com.voxivoid.recipelab;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Two-column recipe browser: groups left, recipes of the highlighted group right. Canvas-drawn.
 * The first group is Favourites (the marked recipes, in marking order), the second Custom (the custom recipes from the
 * memory card, A to Z); the rest are the brands.
 */
public class PickerView extends View {
    private static final int ACCENT = SonyUi.ACCENT, INK = SonyUi.TEXT;

    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG), edge = new Paint(Paint.ANTI_ALIAS_FLAG), sel = new Paint(Paint.ANTI_ALIAS_FLAG),
            head = new Paint(Paint.ANTI_ALIAS_FLAG), item = new Paint(Paint.ANTI_ALIAS_FLAG), small = new Paint(Paint.ANTI_ALIAS_FLAG), rule = new Paint(),
            track = new Paint(Paint.ANTI_ALIAS_FLAG), thumb = new Paint(Paint.ANTI_ALIAS_FLAG), tagBg = new Paint(Paint.ANTI_ALIAS_FLAG),
            star = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float d;
    private final Legend legend;
    private Keys.Caps caps = Keys.Caps.UNKNOWN;
    private final Paint outline = new Paint(Paint.ANTI_ALIAS_FLAG);
    private int selected = 0, column = 1, group = 0;              // column: 0 groups, 1 recipes · group: Favourites.GROUP or a brand
    private List<Integer> favs = new ArrayList<Integer>();
    private Library lib = new Library();
    private boolean card = true;                                  // a memory card is in, so custom recipes can be kept
    private boolean applied;

    public PickerView(Context c, AttributeSet a) {
        super(c, a);
        d = UiScale.of(c);
        legend = new Legend(d);
        bg.setColor(SonyUi.BACKGROUND);
        edge.setColor(SonyUi.HEADER);
        sel.setColor(SonyUi.SELECTED);
        outline.setColor(ACCENT); outline.setStyle(Paint.Style.STROKE); outline.setStrokeWidth(d);
        head.setColor(SonyUi.TEXT); head.setTextSize(14 * d); head.setFakeBoldText(false);
        item.setColor(0xFFFFFFFF); item.setTextSize(13 * d);
        small.setColor(0x99FFFFFF); small.setTextSize(10 * d);
        rule.setColor(SonyUi.RULE);
        track.setColor(SonyUi.TRACK); thumb.setColor(SonyUi.MUTED);
    }

    /** what the key probe found; until it is set only the universal keys are named */
    public void setCaps(Keys.Caps k) { caps = k; invalidate(); }

    /** the display language's typeface ({@link UiFont}) */
    public void setTypeface(Typeface tf) { head.setTypeface(tf); item.setTypeface(tf); small.setTypeface(tf); legend.setTypeface(tf); invalidate(); }

    /**
     * The highlighted recipe, the active column, the group the left column is on, the favourites in
     * marking order, the recipes with the custom ones, and whether a memory card is in.
     */
    public void set(int recipe, int col, int grp, List<Integer> favourites, Library library, boolean cardIn, boolean isApplied) {
        selected = recipe; column = col; group = grp; favs = favourites; lib = library; card = cardIn; applied = isApplied; invalidate();
    }

    @Override
    protected void onDraw(Canvas c) {
        float w = getWidth(), h = getHeight(), pad = 12 * d;
        c.drawRect(0, 0, w, h, bg);
        c.drawRect(0, 0, w, 28 * d, edge);

        int g = group;
        boolean favGroup = g == Favourites.GROUP;
        int count = Favourites.groupCount(g, favs, lib);
        float colX = w * 0.30f;
        int mode = column == 0 ? Keys.H_BRANDS : lib.isCustom(selected) ? Keys.H_RECIPES_CUSTOM : Keys.H_RECIPES;
        Keys.Hints hints = Keys.hints(mode, caps);
        float legTop = h - pad - legend.height(legend.lines(w - 2 * pad, hints)) + 2 * d;   // the legend, on as many lines as it needs
        float top = 28 * d, detailTop = legTop - 25 * d, bottom = detailTop - 5 * d;
        float sbW = 4 * d;                                      // scrollbar width
        head.setColor(SonyUi.TEXT);
        c.drawText(Lang.t("menu_browse"), pad, 20 * d, head);
        small.setColor(SonyUi.MUTED);
        String groupTitle = Favourites.groupName(g) + "  " + count;
        float titleSpace = w - 2 * pad - head.measureText(Lang.t("menu_browse")) - 12 * d;
        String fittedGroup = CanvasText.fit(small, groupTitle, titleSpace);
        c.drawText(fittedGroup, w - pad - small.measureText(fittedGroup), 20 * d, small);
        head.setColor(0x99FFFFFF);
        c.drawLine(colX, top + 4 * d, colX, bottom, rule);
        c.drawLine(pad, top + 3 * d, w - pad, top + 3 * d, rule);

        // ---- left: Favourites, Custom, then the brands
        int ng = Favourites.groupRows(), gRow = Favourites.groupRow(g);
        float listTop = top + 6 * d, listH = bottom - listTop;
        float rowH = 24 * d;
        int gVisible = SonyUi.visibleRows(listH, rowH);
        int gFirst = SonyUi.firstRow(ng, gVisible, gRow);
        float gRight = colX - 8 * d - (ng > gVisible ? sbW + 4 * d : 0);
        float y = listTop;
        item.setTextSize(12 * d);
        for (int i = gFirst; i < Math.min(ng, gFirst + gVisible); i++, y += rowH) {
            int gi = Favourites.groupAt(i);
            boolean on = i == gRow, active = on && column == 0;
            if (on) {
                r.set(pad - 4 * d, y, gRight, y + rowH);
                c.drawRect(r, sel);
                if (active) c.drawRect(r, outline);
            }
            item.setColor(on ? SonyUi.TEXT : SonyUi.MUTED); item.setFakeBoldText(false);
            float tx = pad;
            if (gi == Favourites.GROUP) { star.setColor(SonyUi.MUTED); Legend.star(c, pad + 5 * d, y + rowH / 2, 5 * d, star); tx += 13 * d; }
            small.setColor(active ? SonyUi.TEXT : SonyUi.MUTED);
            c.drawText(CanvasText.fit(item, Favourites.groupName(gi), gRight - tx - 4 * d), tx, y + rowH / 2 + item.getTextSize() * 0.36f, item);
            if (i == 1) c.drawLine(pad, y + rowH - d, gRight, y + rowH - d, rule);   // Favourites and Custom are set apart from the brands
        }
        item.setFakeBoldText(false);
        if (ng > gVisible && gVisible > 0) scrollbar(c, colX - 6 * d - sbW, listTop, listH, sbW, gFirst, gVisible, ng);

        // ---- right: the group's recipes, windowed around the highlight
        float x = colX + pad;
        item.setTextSize(13 * d);
        if (count == 0) {                                       // an empty Favourites or Custom group says so, and how to fill it
            item.setColor(0xCCFFFFFF);
            c.drawText(CanvasText.fit(item, Favourites.emptyTitle(g, card), w - pad - x), x, listTop + 20 * d, item);
            small.setColor(0x99FFFFFF);
            c.drawText(CanvasText.fit(small, Favourites.emptyHint(g), w - pad - x), x, listTop + 36 * d, small);
        } else {
            float rh = 28 * d;
            int visible = SonyUi.visibleRows(listH, rh);
            int selPos = Math.max(0, Favourites.positionIn(g, selected, favs, lib));
            int first = 0;
            boolean scroll = count > visible;
            if (scroll) { first = SonyUi.firstRow(count, visible, selPos); }
            float xr = w - pad - (scroll ? sbW + 6 * d : 0);
            y = listTop;
            for (int k = first; k < Math.min(count, first + visible); k++, y += rh) {
                int idx = Favourites.recipeAt(g, k, favs, lib);
                boolean on = idx == selected, active = on && column == 1;
                if (on) {
                    r.set(x - 4 * d, y, xr, y + rh);
                    c.drawRect(r, sel);
                    if (active) c.drawRect(r, outline);
                }
                Recipes.Recipe rc = lib.get(idx);
                item.setColor(SonyUi.TEXT); item.setFakeBoldText(false);
                // a Picture Effect recipe works only as JPEG: say so; every other recipe needs no tag
                float tx = rc.isEffect() ? tag(c, Lang.t("tag_jpeg_only"), xr - 4 * d, y + 3 * d, active, SonyUi.HEADER, SonyUi.MUTED) : xr;
                if (!favGroup && favs.contains(idx)) { star.setColor(SonyUi.MUTED); Legend.star(c, tx - 4 * d - 6 * d, y + rh / 2, 5 * d, star); }
                float textRight = tx - ((!favGroup && favs.contains(idx)) ? 20 * d : 4 * d);
                c.drawText(CanvasText.fit(item, Recipes.displayName(rc), textRight - x), x, y + rh / 2 + item.getTextSize() * 0.36f, item);
            }
            item.setFakeBoldText(false);
            if (scroll && visible > 0) scrollbar(c, w - pad - sbW, listTop, listH, sbW, first, visible, count);
        }

        if (count > 0 && lib.valid(selected) && Favourites.positionIn(g,selected,favs,lib) >= 0) {
            Recipes.Recipe rc = lib.get(selected);
            String original = Recipes.originalName(rc);
            String description = original == null ? Recipes.groupLabel(rc.group) : original;
            String state = Lang.t(applied ? "state_active" : "state_preview");
            small.setColor(SonyUi.MUTED);
            float stateWidth = small.measureText(state);
            c.drawText(CanvasText.fit(small, description, w - 2 * pad - stateWidth - 18 * d), pad, detailTop + 15 * d, small);
            if (applied) {
                outline.setColor(SonyUi.TEXT);
                float cx = w - pad - stateWidth - 12 * d, cy = detailTop + 11 * d;
                c.drawLine(cx - 4 * d, cy, cx - d, cy + 3 * d, outline);
                c.drawLine(cx - d, cy + 3 * d, cx + 5 * d, cy - 4 * d, outline);
                outline.setColor(ACCENT);
            }
            c.drawText(state, w - pad - stateWidth, detailTop + 15 * d, small);
        }

        // ---- footer: icon legend
        c.drawLine(pad, legTop - 2 * d, w - pad, legTop - 2 * d, rule);
        legend.drawWrapped(c, pad, legTop, w - 2 * pad, hints);
    }

    /** a small pill ending at {@code right} on the row at {@code y}; returns its left edge */
    private float tag(Canvas c, String text, float right, float y, boolean active, int bgColor, int textColor) {
        float tw = small.measureText(text) + 8 * d, tx = right - tw;
        r.set(tx, y + 5 * d, tx + tw, y + 17 * d);
        tagBg.setColor(bgColor);
        c.drawRoundRect(r, 2 * d, 2 * d, tagBg);
        small.setColor(textColor);
        c.drawText(text, tx + 4 * d, y + 14 * d, small);
        return tx;
    }

    /** vertical scrollbar: track + thumb proportional to the visible window */
    private void scrollbar(Canvas c, float x, float top, float height, float width, int first, int visible, int total) {
        r.set(x, top, x + width, top + height); c.drawRoundRect(r, width / 2, width / 2, track);
        float thumbH = Math.max(12 * d, height * visible / total);
        float thumbY = top + (height - thumbH) * first / Math.max(1, total - visible);
        r.set(x, thumbY, x + width, thumbY + thumbH); c.drawRoundRect(r, width / 2, width / 2, thumb);
    }
}
