package com.example.tibiachat.gui;

import com.example.tibiachat.TibiaChatTabsClient;
import com.example.tibiachat.config.TibiaChatConfig;
import com.example.tibiachat.hud.HudLayout;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class HudEditScreen extends Screen {

    private enum Target { NONE, TAB_BAR, TAB_BAR_RESIZE, ICON }

    private final Screen parent;
    private final TibiaChatConfig config = TibiaChatTabsClient.CONFIG;
    private Target dragging = Target.NONE;

    public HudEditScreen(Screen parent) {
        super(Text.literal("Reposition Chat Tabs HUD"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        this.addDrawableChild(ButtonWidget.builder(Text.literal("Done"), btn -> this.close())
                .dimensions(this.width / 2 - 50, this.height - 28, 100, 20)
                .build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.fill(0, 0, this.width, this.height, 0x80000000);

        int barLeft = HudLayout.tabBarLeft();
        int barRight = HudLayout.tabBarRight(this.width);
        int barTop = HudLayout.tabBarTop(this.height);
        int barBottom = HudLayout.tabBarBottom(this.height);

        context.fill(barLeft, barTop, barRight, barBottom, 0xB03A6EA5);
        context.fill(barRight - 6, barTop, barRight, barBottom, 0xFF4A90E2);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("TAB BAR"),
                barLeft + (barRight - barLeft) / 2, barTop + (barBottom - barTop) / 2 - 4, 0xFFFFFFFF);

        String sample = "\u2709 3";
        int textW = this.textRenderer.getWidth(sample);
        int iconX = HudLayout.notifIconX();
        int iconY = HudLayout.notifIconY(this.height);
        int iconW = HudLayout.notifIconWidth(textW);
        int iconH = HudLayout.notifIconHeight();

        context.fill(iconX - 3, iconY - 2, iconX + iconW, iconY + iconH, 0xB0C77A2A);
        context.drawTextWithShadow(this.textRenderer, Text.literal(sample), iconX, iconY, 0xFFFFD24A);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Drag elements to move anywhere. Drag tab bar right edge to resize length."),
                this.width / 2, 12, 0xFFFFFFFF);

        super.render(context, mouseX, mouseY, delta);
    }

    private boolean withinResizeHandle(double x, double y) {
        int top = HudLayout.tabBarTop(this.height);
        int bottom = HudLayout.tabBarBottom(this.height);
        int right = HudLayout.tabBarRight(this.width);
        return x >= right - 8 && x <= right + 8 && y >= top && y < bottom;
    }

    private boolean withinTabBar(double x, double y) {
        int top = HudLayout.tabBarTop(this.height);
        int bottom = HudLayout.tabBarBottom(this.height);
        int left = HudLayout.tabBarLeft();
        int right = HudLayout.tabBarRight(this.width);
        return x >= left && x < right && y >= top && y < bottom;
    }

    private boolean withinIcon(double x, double y) {
        String sample = "\u2709 3";
        int textW = this.textRenderer.getWidth(sample);
        int iconX = HudLayout.notifIconX();
        int iconY = HudLayout.notifIconY(this.height);
        int iconW = HudLayout.notifIconWidth(textW);
        int iconH = HudLayout.notifIconHeight();
        return x >= iconX - 3 && x < iconX + iconW && y >= iconY - 2 && y < iconY + iconH;
    }

    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        if (withinIcon(click.x(), click.y())) {
            dragging = Target.ICON;
            return true;
        }
        if (withinResizeHandle(click.x(), click.y())) {
            dragging = Target.TAB_BAR_RESIZE;
            return true;
        }
        if (withinTabBar(click.x(), click.y())) {
            dragging = Target.TAB_BAR;
            return true;
        }
        dragging = Target.NONE;
        return super.mouseClicked(click, doubled);
    }

    @Override
    public boolean mouseDragged(Click click, double offsetX, double offsetY) {
        switch (dragging) {
            case TAB_BAR -> {
                config.setTabBarOffsetX(Math.round(config.tabBarOffsetX() + (float) offsetX));
                config.setTabBarOffsetY(Math.round(config.tabBarOffsetY() + (float) offsetY));
                return true;
            }
            case TAB_BAR_RESIZE -> {
                config.setTabBarWidth(Math.max(50, Math.round(config.tabBarWidth() + (float) offsetX)));
                return true;
            }
            case ICON -> {
                config.setNotifIconOffsetX(Math.round(config.notifIconOffsetX() + (float) offsetX));
                config.setNotifIconOffsetY(Math.round(config.notifIconOffsetY() + (float) offsetY));
                return true;
            }
            default -> {
                return super.mouseDragged(click, offsetX, offsetY);
            }
        }
    }

    @Override
    public boolean mouseReleased(Click click) {
        boolean wasDragging = dragging != Target.NONE;
        dragging = Target.NONE;
        if (wasDragging) {
            config.save();
            return true;
        }
        return super.mouseReleased(click);
    }

    @Override
    public void close() {
        config.save();
        if (this.client != null) {
            this.client.setScreen(parent);
        }
    }
}
