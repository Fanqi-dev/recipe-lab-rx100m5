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
    private final Paint bg = new Paint(Paint.ANTI_ALIAS_FLAG),
            head = new Paint(Paint.ANTI_ALIAS_FLAG), item = new Paint(Paint.ANTI_ALIAS_FLAG), small = new Paint(Paint.ANTI_ALIAS_FLAG), rule = new Paint(),
            track = new Paint(Paint.ANTI_ALIAS_FLAG), thumb = new Paint(Paint.ANTI_ALIAS_FLAG),
            star = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF r = new RectF();
    private final float d;
    private final Legend legend;
    private final UiChrome chrome;
    private Keys.Caps caps = Keys.Caps.UNKNOWN;
    private int selected = 0, column = 1, group = 0;              // column: 0 groups, 1 recipes · group: Favourites.GROUP or a brand
    private List<Integer> favs = new ArrayList<Integer>();
    private Library lib = new Library();
    private boolean card = true;                                  // a memory card is in, so custom recipes can be kept
    private boolean applied;

    public PickerView(Context c, AttributeSet a) {
        super(c, a);
        d = UiScale.of(c);
        legend = new Legend(d);
        chrome = new UiChrome(d);
        bg.setColor(SonyUi.BACKGROUND);
        head.setColor(SonyUi.TEXT); head.setTextSize(SonyUi.TITLE * d); head.setFakeBoldText(false);
        item.setColor(SonyUi.TEXT); item.setTextSize(SonyUi.BODY * d);
        small.setColor(SonyUi.MUTED); small.setTextSize(SonyUi.SECONDARY * d);
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
        float w = getWidth(), h = getHeight(), pad = SonyUi.PAGE_PAD * d;
        c.drawRect(0, 0, w, h, bg);
        chrome.header(c,w,Lang.t("menu_browse"),head);

        int g = group;
        boolean favGroup = g == Favourites.GROUP;
        int count = Favourites.groupCount(g, favs, lib);
        float colX = w * 0.30f;
        int mode = column == 0 ? Keys.H_BRANDS : lib.isCustom(selected) ? Keys.H_RECIPES_CUSTOM : Keys.H_RECIPES;
        Keys.Hints hints = Keys.hints(mode, caps);
        float legTop = chrome.footerTop(w,h,legend,hints);
        float top = SonyUi.HEADER_HEIGHT * d, detailTop = legTop - 24 * d, bottom = detailTop - 5 * d;
        float sbW = 4 * d;                                      // scrollbar width
        head.setColor(SonyUi.TEXT);
        small.setColor(SonyUi.MUTED);
        String groupTitle = Favourites.groupName(g) + "  " + count;
        float titleSpace = w - 2 * pad - head.measureText(Lang.t("menu_browse")) - 12 * d;
        String fittedGroup = CanvasText.fit(small, groupTitle, titleSpace);
        c.drawText(fittedGroup, w - pad - small.measureText(fittedGroup), CanvasText.baseline(small,top/2), small);
        c.drawLine(colX, top + SonyUi.LIST_GAP * d, colX, bottom, rule);

        // ---- left: Favourites, Custom, then the brands
        int ng = Favourites.groupRows(), gRow = Favourites.groupRow(g);
        float listTop = top + SonyUi.LIST_GAP * d, listH = bottom - listTop;
        float rowH = SonyUi.ROW_HEIGHT * d;
        int gVisible = SonyUi.visibleRows(listH, rowH);
        int gFirst = SonyUi.firstRow(ng, gVisible, gRow);
        float gRight = colX - 8 * d - (ng > gVisible ? sbW + 4 * d : 0);
        float y = listTop;
        for (int i = gFirst; i < Math.min(ng, gFirst + gVisible); i++, y += rowH) {
            int gi = Favourites.groupAt(i);
            boolean on = i == gRow, active = on && column == 0;
            if (on) {
                chrome.selection(c,pad-4*d,y,gRight,y+rowH-2*d,active);
            }
            item.setColor(on ? SonyUi.TEXT : SonyUi.MUTED); item.setFakeBoldText(false);
            float tx = pad;
            if (gi == Favourites.GROUP) { star.setColor(SonyUi.MUTED); Legend.star(c, pad + 5 * d, y + rowH / 2, 5 * d, star); tx += 13 * d; }
            small.setColor(active ? SonyUi.TEXT : SonyUi.MUTED);
            c.drawText(CanvasText.fit(item, Favourites.groupName(gi), gRight - tx - 4 * d), tx, CanvasText.baseline(item,y+rowH/2), item);
            if (i == 1) c.drawLine(pad, y + rowH - d, gRight, y + rowH - d, rule);   // Favourites and Custom are set apart from the brands
        }
        item.setFakeBoldText(false);
        if (ng > gVisible && gVisible > 0) scrollbar(c, colX - 6 * d - sbW, listTop, listH, sbW, gFirst, gVisible, ng);

        // ---- right: the group's recipes, windowed around the highlight
        float x = colX + pad;
        if (count == 0) {                                       // an empty Favourites or Custom group says so, and how to fill it
            item.setColor(SonyUi.TEXT);
            c.drawText(CanvasText.fit(item, Favourites.emptyTitle(g, card), w - pad - x), x, listTop + 20 * d, item);
            small.setColor(SonyUi.MUTED);
            c.drawText(CanvasText.fit(small, Favourites.emptyHint(g), w - pad - x), x, listTop + 36 * d, small);
        } else {
            float rh = SonyUi.ROW_HEIGHT * d;
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
                    chrome.selection(c,x-4*d,y,xr,y+rh-2*d,active);
                }
                Recipes.Recipe rc = lib.get(idx);
                item.setColor(SonyUi.TEXT); item.setFakeBoldText(false);
                // a Picture Effect recipe works only as JPEG: say so; every other recipe needs no tag
                float tx = rc.isEffect() ? chrome.tag(c,xr-4*d,y+rh/2,Lang.t("tag_jpeg_only"),small) : xr;
                if (!favGroup && favs.contains(idx)) { star.setColor(SonyUi.MUTED); Legend.star(c, tx - 4 * d - 6 * d, y + rh / 2, 5 * d, star); }
                float textRight = tx - ((!favGroup && favs.contains(idx)) ? 20 * d : 4 * d);
                c.drawText(CanvasText.fit(item, Recipes.displayName(rc), textRight - x), x, CanvasText.baseline(item,y+rh/2), item);
            }
            item.setFakeBoldText(false);
            if (scroll && visible > 0) scrollbar(c, w - pad - sbW, listTop, listH, sbW, first, visible, count);
        }

        if (count > 0 && lib.valid(selected) && Favourites.positionIn(g,selected,favs,lib) >= 0) {
            Recipes.Recipe rc = lib.get(selected);
            String original = Recipes.originalName(rc);
            String description = original == null ? Recipes.groupLabel(rc.group) : original;
            small.setColor(SonyUi.MUTED);
            float stateWidth = CanvasText.statusWidth(small,d), cy = detailTop + 11*d;
            c.drawText(CanvasText.fit(small, description, w - 2 * pad - stateWidth - 18 * d), pad, CanvasText.baseline(small,cy), small);
            chrome.status(c,w-pad,cy,small,applied);
        }

        // ---- footer: icon legend
        chrome.footer(c,w,legTop,legend,hints);
    }

    /** vertical scrollbar: track + thumb proportional to the visible window */
    private void scrollbar(Canvas c, float x, float top, float height, float width, int first, int visible, int total) {
        r.set(x, top, x + width, top + height); c.drawRoundRect(r, width / 2, width / 2, track);
        float thumbH = Math.max(12 * d, height * visible / total);
        float thumbY = top + (height - thumbH) * first / Math.max(1, total - visible);
        r.set(x, thumbY, x + width, thumbY + thumbH); c.drawRoundRect(r, width / 2, width / 2, thumb);
    }
}
