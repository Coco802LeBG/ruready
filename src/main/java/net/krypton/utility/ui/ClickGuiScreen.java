package net.krypton.utility.ui;

import net.krypton.utility.core.Category;
import net.krypton.utility.core.ConfigManager;
import net.krypton.utility.core.Module;
import net.krypton.utility.core.ModuleManager;
import net.krypton.utility.core.setting.BoolSetting;
import net.krypton.utility.core.setting.ModeSetting;
import net.krypton.utility.core.setting.NumberSetting;
import net.krypton.utility.core.setting.Setting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

/**
 * The ClickGUI. Layout is recomputed every frame; every interactive element registers a Hit rectangle while it is
 * drawn, and input handlers resolve clicks against that list (topmost last).
 */
public final class ClickGuiScreen extends Screen {
    private enum HitType { TAB, TOGGLE, HEADER, BOOL, MODE, SLIDER, ACCENT, EDIT_HUD }

    private record Hit(HitType type, int x, int y, int w, int h, Module module, Setting<?> setting, int index) {
    }

    private static final int SIDEBAR_W = 156;
    private static final int CARD_H = 38;
    private static final int TAB_H = 30;

    private final List<Hit> hits = new ArrayList<>();
    private final Category[] categories = Category.values();
    private Hit dragging;
    private int selected;
    private float tabIndicator;
    private float open;
    private float scroll;
    private float targetScroll;
    private int contentHeight;
    private int viewHeight;
    private long last = System.nanoTime();

    // clip rectangle of the module list (set during render)
    private int clipX;
    private int clipY;
    private int clipW;
    private int clipH;

    public ClickGuiScreen() {
        super(Component.literal("Krypton Utility"));
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    // ------------------------------------------------------------------ rendering

    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        long now = System.nanoTime();
        float dt = Math.min(0.1f, (now - last) / 1.0E9f);
        last = now;
        open = Math.min(1f, open + dt * 6f);
        float e = Draw.easeOut(open);
        Draw.alphaMul = e;
        hits.clear();

        int ww = Math.min(660, width - 40);
        int wh = Math.min(410, height - 40);
        int wx = (width - ww) / 2;
        int wy = (height - wh) / 2 + Math.round((1f - e) * 12f);

        g.fillGradient(0, 0, width, height, Draw.col(0x70050510), Draw.col(0xB4080818));
        Draw.shadow(g, wx, wy, ww, wh, 12, 8);
        Draw.round(g, wx, wy, ww, wh, 12, Theme.WINDOW);
        Draw.roundOutline(g, wx, wy, ww, wh, 12, 0x20FFFFFF);

        renderSidebar(g, mouseX, mouseY, dt, wx + 8, wy + 8, SIDEBAR_W, wh - 16);
        int px = wx + 8 + SIDEBAR_W + 16;
        int pw = wx + ww - 16 - px;
        renderContent(g, mouseX, mouseY, dt, px, wy, pw, wh);
        Draw.alphaMul = 1f;
    }

    private void renderSidebar(GuiGraphics g, int mx, int my, float dt, int sx, int sy, int sw, int sh) {
        Draw.round(g, sx, sy, sw, sh, 9, Theme.SIDEBAR);
        Draw.textGradient(g, "KRYPTON", sx + 16, sy + 16, 1.6f, Theme.accent(), Theme.accent2());
        Draw.text(g, "U T I L I T Y", sx + 17, sy + 33, Theme.MUTED);
        Draw.rect(g, sx + 16, sy + 49, sw - 32, 1, 0x18FFFFFF);

        tabIndicator = Draw.approach(tabIndicator, selected, dt, 16f);
        int ty0 = sy + 60;
        int indY = ty0 + Math.round(tabIndicator * TAB_H);
        Draw.round(g, sx + 8, indY, sw - 16, 26, 7, Draw.withAlpha(Theme.accent(), 0.16f));
        Draw.gradientV(g, sx + 8, indY + 6, 3, 14, Theme.accent(), Theme.accent2());

        for (int i = 0; i < categories.length; i++) {
            Category c = categories[i];
            int ry = ty0 + i * TAB_H;
            boolean hover = Draw.inside(mx, my, sx + 8, ry, sw - 16, 26);
            if (hover && i != selected) {
                Draw.round(g, sx + 8, ry, sw - 16, 26, 7, 0x10FFFFFF);
            }
            Draw.round(g, sx + 22, ry + 9, 8, 8, 4, c.color);
            Draw.text(g, c.label, sx + 38, ry + 9, i == selected ? Theme.TEXT : Theme.MUTED);
            List<Module> mods = ModuleManager.in(c);
            int on = 0;
            for (Module m : mods) {
                if (m.isEnabled()) {
                    on++;
                }
            }
            Draw.textRight(g, on + "/" + mods.size(), sx + sw - 18, ry + 9, Theme.MUTED);
            hits.add(new Hit(HitType.TAB, sx + 8, ry, sw - 16, 26, null, null, i));
        }

        // accent picker
        int ay = sy + sh - 40;
        Draw.text(g, "ACCENT", sx + 16, ay - 12, Theme.MUTED);
        for (int i = 0; i < Theme.accentCount(); i++) {
            int cx = sx + 16 + i * 20;
            int mid = Draw.lerp(Theme.accentOf(i, false), Theme.accentOf(i, true), 0.5f);
            Draw.round(g, cx, ay, 14, 14, 7, mid);
            if (i == Theme.accentIndex()) {
                Draw.roundOutline(g, cx - 2, ay - 2, 18, 18, 9, Theme.TEXT);
            }
            hits.add(new Hit(HitType.ACCENT, cx - 2, ay - 2, 18, 18, null, null, i));
        }
    }

    private void renderContent(GuiGraphics g, int mx, int my, float dt, int px, int wy, int pw, int wh) {
        Category cat = categories[selected];
        Draw.textScaled(g, cat.label, px, wy + 20, 1.4f, Theme.TEXT);
        Draw.text(g, cat.subtitle, px + 1, wy + 36, Theme.MUTED);

        if (cat == Category.INTERFACE) {
            int bw = 92;
            int bx = px + pw - bw;
            boolean hover = Draw.inside(mx, my, bx, wy + 18, bw, 22);
            Draw.round(g, bx, wy + 18, bw, 22, 7, hover ? Draw.lerp(Theme.CARD_HOVER, Theme.accent(), 0.35f) : Theme.CARD);
            Draw.textCentered(g, "Edit HUD", bx + bw / 2, wy + 25, Theme.TEXT);
            hits.add(new Hit(HitType.EDIT_HUD, bx, wy + 18, bw, 22, null, null, 0));
        }

        clipX = px;
        clipY = wy + 56;
        clipW = pw;
        clipH = wh - 56 - 14;
        viewHeight = clipH;
        scroll = Draw.approach(scroll, targetScroll, dt, 16f);

        g.enableScissor(clipX, clipY, clipX + clipW, clipY + clipH);
        int y = clipY - Math.round(scroll);
        int startY = y;
        for (Module m : ModuleManager.in(cat)) {
            y += renderModule(g, m, mx, my, dt, px, y, pw) + 8;
        }
        g.disableScissor();
        contentHeight = y - startY;
        float max = Math.max(0, contentHeight - viewHeight);
        targetScroll = Mth.clamp(targetScroll, 0f, max);

        // scroll indicator
        if (max > 0) {
            int trackH = clipH;
            int thumbH = Math.max(24, Math.round(trackH * (viewHeight / (float) contentHeight)));
            int thumbY = clipY + Math.round((trackH - thumbH) * (scroll / max));
            Draw.round(g, px + pw + 6, thumbY, 3, thumbH, 1, Draw.withAlpha(Theme.accent(), 0.7f));
        }
    }

    private int settingsHeight(List<Setting<?>> settings) {
        int h = 0;
        for (Setting<?> s : settings) {
            h += s instanceof NumberSetting ? 32 : 24;
        }
        return h;
    }

    private String fit(String s, int maxWidth) {
        if (Draw.width(s) <= maxWidth) {
            return s;
        }
        String t = s;
        while (t.length() > 1 && Draw.width(t + "...") > maxWidth) {
            t = t.substring(0, t.length() - 1);
        }
        return t + "...";
    }

    private void addHit(Hit h) {
        // keep only the part of the rectangle that is inside the visible list area
        int y1 = Math.max(h.y(), clipY);
        int y2 = Math.min(h.y() + h.h(), clipY + clipH);
        if (y2 > y1) {
            hits.add(new Hit(h.type(), h.x(), y1, h.w(), y2 - y1, h.module(), h.setting(), h.index()));
        }
    }

    private int renderModule(GuiGraphics g, Module m, int mx, int my, float dt, int x, int y, int w) {
        m.toggleAnim = Draw.approach(m.toggleAnim, m.isEnabled() ? 1f : 0f, dt, 14f);
        m.expandAnim = Draw.approach(m.expandAnim, m.expanded ? 1f : 0f, dt, 14f);
        List<Setting<?>> settings = m.visibleSettings();
        int extra = settings.isEmpty() ? 0 : settingsHeight(settings) + 10;
        int h = CARD_H + Math.round(extra * m.expandAnim);
        if (y + h < clipY || y > clipY + clipH) {
            return h;
        }

        boolean hover = Draw.inside(mx, my, x, Math.max(y, clipY), w, Math.min(CARD_H, clipY + clipH - y)) && my >= clipY && my < clipY + clipH;
        Draw.round(g, x, y, w, h, 8, hover ? Theme.CARD_HOVER : Theme.CARD);
        if (m.toggleAnim > 0.02f) {
            Draw.gradientV(g, x + 1, y + 9, 3, 20, Draw.withAlpha(Theme.accent(), m.toggleAnim), Draw.withAlpha(Theme.accent2(), m.toggleAnim));
        }

        int tw = 32;
        int th = 16;
        int tx = x + w - tw - 14;
        int ty = y + (CARD_H - th) / 2;
        int textMax = w - 14 - (tw + 28) - (settings.isEmpty() ? 0 : 56);
        Draw.text(g, m.name(), x + 14, y + 9, Theme.TEXT);
        Draw.text(g, fit(m.description(), textMax), x + 14, y + 21, Theme.MUTED);
        if (!settings.isEmpty()) {
            Draw.textRight(g, settings.size() + " options", tx - 12, y + 15, m.expanded ? Theme.accent() : Theme.MUTED);
        }

        // switch
        Draw.round(g, tx, ty, tw, th, 8, Draw.lerp(Theme.TRACK, Theme.accent(), m.toggleAnim));
        int kx = tx + 2 + Math.round((tw - th) * m.toggleAnim);
        Draw.round(g, kx, ty + 2, th - 4, th - 4, 6, 0xFFFFFFFF);

        addHit(new Hit(HitType.HEADER, x, y, w, CARD_H, m, null, 0));
        addHit(new Hit(HitType.TOGGLE, tx - 4, ty - 4, tw + 8, th + 8, m, null, 0));

        if (m.expandAnim > 0.01f && !settings.isEmpty()) {
            g.enableScissor(x, Math.max(y + CARD_H, clipY), x + w, Math.min(y + h, clipY + clipH));
            Draw.rect(g, x + 14, y + CARD_H - 2, w - 28, 1, 0x14FFFFFF);
            int sy = y + CARD_H + 6;
            boolean interactive = m.expandAnim > 0.9f;
            for (Setting<?> s : settings) {
                sy += renderSetting(g, s, m, dt, x + 16, sy, w - 32, interactive);
            }
            g.disableScissor();
        }
        return h;
    }

    private int renderSetting(GuiGraphics g, Setting<?> s, Module m, float dt, int x, int y, int w, boolean interactive) {
        if (s instanceof BoolSetting b) {
            b.anim = Draw.approach(b.anim, b.get() ? 1f : 0f, dt, 16f);
            Draw.text(g, b.name, x, y + 8, b.get() ? Theme.TEXT : Theme.MUTED);
            int tw = 26;
            int th = 14;
            int tx = x + w - tw;
            Draw.round(g, tx, y + 5, tw, th, 7, Draw.lerp(Theme.TRACK, Theme.accent(), b.anim));
            Draw.round(g, tx + 2 + Math.round((tw - th) * b.anim), y + 7, th - 4, th - 4, 5, 0xFFFFFFFF);
            if (interactive) {
                addHit(new Hit(HitType.BOOL, x, y, w, 24, m, b, 0));
            }
            return 24;
        }
        if (s instanceof ModeSetting ms) {
            Draw.text(g, ms.name, x, y + 8, Theme.MUTED);
            String label = "<  " + ms.get() + "  >";
            int pw = Draw.width(label) + 14;
            int px = x + w - pw;
            Draw.round(g, px, y + 3, pw, 18, 6, Theme.TRACK);
            Draw.textCentered(g, label, px + pw / 2, y + 8, Theme.accent());
            if (interactive) {
                addHit(new Hit(HitType.MODE, x, y, w, 24, m, ms, 0));
            }
            return 24;
        }
        if (s instanceof NumberSetting ns) {
            Draw.text(g, ns.name, x, y + 5, Theme.MUTED);
            Draw.textRight(g, ns.display(), x + w, y + 5, Theme.TEXT);
            int ty = y + 21;
            int fill = Math.round(ns.fraction() * w);
            Draw.round(g, x, ty, w, 4, 2, Theme.TRACK);
            if (fill > 0) {
                Draw.gradientH(g, x, ty, fill, 4, Theme.accent(), Theme.accent2());
            }
            Draw.round(g, x + fill - 5, ty - 3, 10, 10, 5, 0xFFFFFFFF);
            if (interactive) {
                addHit(new Hit(HitType.SLIDER, x, y + 14, w, 18, m, ns, 0));
            }
            return 32;
        }
        return 0;
    }

    // ------------------------------------------------------------------ input

    private void updateSlider(Hit h, double mouseX) {
        if (h.setting() instanceof NumberSetting ns) {
            double t = Mth.clamp((mouseX - h.x()) / h.w(), 0.0, 1.0);
            ns.set(ns.min + t * (ns.max - ns.min));
        }
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
        double mx = event.x();
        double my = event.y();
        int button = event.button();
        for (int i = hits.size() - 1; i >= 0; i--) {
            Hit h = hits.get(i);
            if (!Draw.inside(mx, my, h.x(), h.y(), h.w(), h.h())) {
                continue;
            }
            switch (h.type()) {
                case TAB -> {
                    selected = h.index();
                    targetScroll = 0;
                    scroll = 0;
                }
                case TOGGLE -> h.module().toggle();
                case HEADER -> {
                    if (button == 1) {
                        h.module().toggle();
                    } else if (!h.module().visibleSettings().isEmpty()) {
                        h.module().expanded = !h.module().expanded;
                    }
                }
                case BOOL -> ((BoolSetting) h.setting()).toggle();
                case MODE -> ((ModeSetting) h.setting()).cycle(button == 1 ? -1 : 1);
                case SLIDER -> {
                    dragging = h;
                    updateSlider(h, mx);
                }
                case ACCENT -> Theme.setAccent(h.index());
                case EDIT_HUD -> {
                    ConfigManager.save();
                    Minecraft.getInstance().setScreen(new HudEditorScreen(this));
                }
            }
            return true;
        }
        return super.mouseClicked(event, doubleClick);
    }

    @Override
    public boolean mouseDragged(MouseButtonEvent event, double dx, double dy) {
        if (dragging != null) {
            updateSlider(dragging, event.x());
            return true;
        }
        return super.mouseDragged(event, dx, dy);
    }

    @Override
    public boolean mouseReleased(MouseButtonEvent event) {
        dragging = null;
        return super.mouseReleased(event);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        float max = Math.max(0, contentHeight - viewHeight);
        targetScroll = Mth.clamp(targetScroll - (float) scrollY * 32f, 0f, max);
        return true;
    }

    @Override
    public boolean keyPressed(KeyEvent event) {
        if (event.key() == GLFW.GLFW_KEY_RIGHT_SHIFT) {
            onClose();
            return true;
        }
        return super.keyPressed(event);
    }

    @Override
    public void onClose() {
        ConfigManager.save();
        super.onClose();
    }
}
