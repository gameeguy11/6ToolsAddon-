// Part of Chat Tabs, originally by Fractal420: https://github.com/gameeguy11/Chat-Tabs-Mod
// Synced with the standalone mod's drag-to-reorder tab feature.
package com.example.tibiachat.mixin;

import com.example.tibiachat.TibiaChatTabsClient;
import com.example.tibiachat.chat.Conversation;
import com.example.tibiachat.chat.ConversationManager;
import com.example.tibiachat.hud.HudLayout;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.client.input.KeyInput;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.List;

@Mixin(ChatScreen.class)
public abstract class ChatScreenTabBarMixin {

    @Shadow
    protected TextFieldWidget chatField;

    private static final int TAB_W_MAIN_BASE = 50;
    private static final int TAB_W_MIN_BASE = 60;
    private static final int TAB_W_MAX_BASE = 120;
    private static final int TAB_SCROLL_STEP = 40;
    private static final int CLOSE_SIZE = 8;
    private static final int DRAG_THRESHOLD = 4;

    private int tibiaChatTabs$tabScroll = 0;

    private String tibiaChatTabs$dragKey = null;
    private boolean tibiaChatTabs$dragActive = false;
    private double tibiaChatTabs$dragPressX;
    private double tibiaChatTabs$dragPressY;
    private int tibiaChatTabs$dragPointerOffsetX;

    private int tibiaChatTabs$tabMainWidth() {
        return Math.round(TAB_W_MAIN_BASE * HudLayout.tabTextScale());
    }

    private int tibiaChatTabs$tabWidth(MinecraftClient mc, String label) {
        float scale = HudLayout.tabTextScale();
        int baseW = mc.textRenderer.getWidth(label) + 24;
        int clampedBase = Math.max(TAB_W_MIN_BASE, Math.min(TAB_W_MAX_BASE, baseW));
        return Math.round(clampedBase * scale);
    }

    @Inject(method = "render", at = @At("TAIL"))
    private void tibiaChatTabs$drawTabs(
            DrawContext ctx,
            int mouseX,
            int mouseY,
            float delta,
            CallbackInfo ci
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        int left = HudLayout.tabBarLeft();
        int right = HudLayout.tabBarRight(screenW);
        int top = HudLayout.tabBarTop(screenH);
        int bottom = HudLayout.tabBarBottom(screenH);

        tibiaChatTabs$tickDrag(mc, mouseX, mouseY, left, right, top, bottom);

        ctx.fill(left, top, right, bottom, 0xB0101010);

        int mainW = tibiaChatTabs$tabMainWidth();
        int x = left + 2;

        x = tibiaChatTabs$drawTab(
                ctx,
                mc,
                "Main",
                ConversationManager.MAIN,
                x,
                mainW,
                top,
                bottom,
                mouseX,
                mouseY,
                0
        );

        int tabsLeft = x;
        int tabsRight = right - 4;

        tibiaChatTabs$clampTabScroll(mc, tabsLeft, tabsRight);

        int maxScroll = tibiaChatTabs$maxTabScroll(mc, tabsLeft, tabsRight);

        if (tabsRight > tabsLeft) {
            ctx.enableScissor(tabsLeft, top, tabsRight, bottom);

            int tabX = tabsLeft - tibiaChatTabs$tabScroll;

            Conversation dragged = null;
            int draggedW = 0;

            for (Conversation c : TibiaChatTabsClient.CHAT.conversations().all()) {
                int w = tibiaChatTabs$tabWidth(mc, c.playerName());

                boolean isDragged = tibiaChatTabs$dragActive && c.key().equals(tibiaChatTabs$dragKey);

                if (isDragged) {
                    dragged = c;
                    draggedW = w;
                } else if (tabX + w > tabsLeft && tabX < tabsRight) {
                    tibiaChatTabs$drawTab(
                            ctx,
                            mc,
                            c.playerName(),
                            c.key(),
                            tabX,
                            w,
                            top,
                            bottom,
                            mouseX,
                            mouseY,
                            c.unread()
                    );
                }

                tabX += w;
            }

            if (dragged != null) {
                int floatX = (int) Math.round(mouseX - tibiaChatTabs$dragPointerOffsetX);
                floatX = Math.max(tabsLeft, Math.min(tabsRight - draggedW, floatX));

                tibiaChatTabs$drawTab(
                        ctx,
                        mc,
                        dragged.playerName(),
                        dragged.key(),
                        floatX,
                        draggedW,
                        top,
                        bottom,
                        mouseX,
                        mouseY,
                        dragged.unread()
                );
            }

            ctx.disableScissor();
        }

        if (tibiaChatTabs$tabScroll > 0) {
            ctx.fill(tabsLeft, top, tabsLeft + 8, bottom, 0xCC101010);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal("‹"), tabsLeft + 1, top + 3, 0xFFFFFFFF);
        }

        if (tibiaChatTabs$tabScroll < maxScroll) {
            ctx.fill(tabsRight - 8, top, tabsRight, bottom, 0xCC101010);
            ctx.drawTextWithShadow(mc.textRenderer, Text.literal("›"), tabsRight - 6, top + 3, 0xFFFFFFFF);
        }
    }

    private int tibiaChatTabs$totalTabsWidth(MinecraftClient mc) {
        int width = 0;
        for (Conversation c : TibiaChatTabsClient.CHAT.conversations().all()) {
            width += tibiaChatTabs$tabWidth(mc, c.playerName());
        }
        return width;
    }

    private int tibiaChatTabs$maxTabScroll(MinecraftClient mc, int tabsLeft, int tabsRight) {
        int availableWidth = Math.max(0, tabsRight - tabsLeft);
        int totalWidth = tibiaChatTabs$totalTabsWidth(mc);
        return Math.max(0, totalWidth - availableWidth);
    }

    private void tibiaChatTabs$clampTabScroll(MinecraftClient mc, int tabsLeft, int tabsRight) {
        int maxScroll = tibiaChatTabs$maxTabScroll(mc, tabsLeft, tabsRight);
        if (tibiaChatTabs$tabScroll < 0) {
            tibiaChatTabs$tabScroll = 0;
        }
        if (tibiaChatTabs$tabScroll > maxScroll) {
            tibiaChatTabs$tabScroll = maxScroll;
        }
    }

    private int tibiaChatTabs$drawTab(
            DrawContext ctx,
            MinecraftClient mc,
            String label,
            String key,
            int x,
            int w,
            int top,
            int bottom,
            int mouseX,
            int mouseY,
            int unread
    ) {
        boolean selected = TibiaChatTabsClient.CHAT.selectedKey().equals(key);
        boolean hover = mouseX >= x && mouseX < x + w && mouseY >= top && mouseY < bottom;

        ctx.fill(
                x,
                top,
                x + w - 1,
                bottom,
                selected ? 0xFF3A3A3A : (hover ? 0xFF303030 : 0xFF202020)
        );

        String shown = label;
        if (unread > 0) {
            shown = shown + " (" + unread + ")";
        }

        int textRightPadding = hover && !ConversationManager.MAIN.equals(key) ? CLOSE_SIZE + 6 : 5;

        if (mc.textRenderer.getWidth(shown) > w - textRightPadding - 4) {
            shown = mc.textRenderer.trimToWidth(shown, w - textRightPadding - 8) + "…";
        }

        ctx.drawTextWithShadow(
                mc.textRenderer,
                Text.literal(shown),
                x + 5,
                top + 4,
                unread > 0 ? 0xFFFFD24A : 0xFFFFFFFF
        );

        if (hover && !ConversationManager.MAIN.equals(key)) {
            int closeX = x + w - CLOSE_SIZE - 2;
            int closeY = top + 4;

            ctx.drawTextWithShadow(
                    mc.textRenderer,
                    Text.literal("×"),
                    closeX,
                    closeY - 1,
                    0xFFFFFFFF
            );
        }

        return x + w;
    }

    private boolean tibiaChatTabs$isCloseHovered(
            double mouseX,
            double mouseY,
            int tabX,
            int tabW,
            int top,
            int bottom
    ) {
        int closeX = tabX + tabW - CLOSE_SIZE - 2;
        int closeY = top + 2;

        return mouseX >= closeX &&
                mouseX < closeX + CLOSE_SIZE &&
                mouseY >= closeY &&
                mouseY < bottom - 1;
    }

    @Inject(
            method = "mouseScrolled",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void tibiaChatTabs$onMouseScrolled(
            double mouseX,
            double mouseY,
            double horizontalAmount,
            double verticalAmount,
            CallbackInfoReturnable<Boolean> cir
    ) {
        MinecraftClient mc = MinecraftClient.getInstance();

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        int left = HudLayout.tabBarLeft();
        int right = HudLayout.tabBarRight(screenW);
        int top = HudLayout.tabBarTop(screenH);
        int bottom = HudLayout.tabBarBottom(screenH);

        if (mouseX < left || mouseX >= right || mouseY < top || mouseY >= bottom) {
            return;
        }

        int tabsLeft = left + 2 + tibiaChatTabs$tabMainWidth();
        int tabsRight = right - 4;

        int maxScroll = tibiaChatTabs$maxTabScroll(mc, tabsLeft, tabsRight);
        if (maxScroll <= 0) {
            return;
        }

        double amount = Math.abs(horizontalAmount) > Math.abs(verticalAmount) ? horizontalAmount : verticalAmount;
        if (amount == 0) {
            return;
        }

        tibiaChatTabs$tabScroll -= (int) Math.round(amount * TAB_SCROLL_STEP);
        tibiaChatTabs$clampTabScroll(mc, tabsLeft, tabsRight);

        cir.setReturnValue(true);
    }

    @Inject(
            method = "mouseClicked",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void tibiaChatTabs$onClick(
            Click click,
            boolean doubled,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (click.button() != GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();

        int screenW = mc.getWindow().getScaledWidth();
        int screenH = mc.getWindow().getScaledHeight();

        int left = HudLayout.tabBarLeft();
        int right = HudLayout.tabBarRight(screenW);
        int top = HudLayout.tabBarTop(screenH);
        int bottom = HudLayout.tabBarBottom(screenH);

        if (click.x() < left || click.x() >= right || click.y() < top || click.y() >= bottom) {
            return;
        }

        int mainW = tibiaChatTabs$tabMainWidth();
        int x = left + 2;

        if (click.x() >= x && click.x() < x + mainW) {
            tibiaChatTabs$dragKey = null;
            tibiaChatTabs$dragActive = false;
            tibiaChatTabs$select(ConversationManager.MAIN);
            cir.setReturnValue(true);
            return;
        }

        int tabsLeft = x + mainW;
        int tabsRight = right - 4;

        if (click.x() < tabsLeft || click.x() > tabsRight) {
            return;
        }

        tibiaChatTabs$clampTabScroll(mc, tabsLeft, tabsRight);

        int tabX = tabsLeft - tibiaChatTabs$tabScroll;

        for (Conversation c : TibiaChatTabsClient.CHAT.conversations().all()) {
            int w = tibiaChatTabs$tabWidth(mc, c.playerName());

            if (tabX + w <= tabsLeft) {
                tabX += w;
                continue;
            }

            if (tabX >= tabsRight) {
                break;
            }

            if (click.x() >= tabX && click.x() < tabX + w) {
                if (tibiaChatTabs$isCloseHovered(click.x(), click.y(), tabX, w, top, bottom)) {
                    tibiaChatTabs$closeTab(c.key());
                } else {
                    tibiaChatTabs$dragKey = c.key();
                    tibiaChatTabs$dragActive = false;
                    tibiaChatTabs$dragPressX = click.x();
                    tibiaChatTabs$dragPressY = click.y();
                    tibiaChatTabs$dragPointerOffsetX = (int) Math.round(click.x() - tabX);
                    tibiaChatTabs$select(c.key());
                }
                cir.setReturnValue(true);
                return;
            }

            tabX += w;
        }
    }

    @Inject(
            method = "mouseReleased",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void tibiaChatTabs$onMouseReleased(
            Click click,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (tibiaChatTabs$dragKey == null) {
            return;
        }

        boolean wasDragging = tibiaChatTabs$dragActive;

        tibiaChatTabs$dragKey = null;
        tibiaChatTabs$dragActive = false;

        if (wasDragging) {
            cir.setReturnValue(true);
        }
    }

    private void tibiaChatTabs$tickDrag(
            MinecraftClient mc,
            int mouseX,
            int mouseY,
            int left,
            int right,
            int top,
            int bottom
    ) {
        if (tibiaChatTabs$dragKey == null) {
            return;
        }

        long window = mc.getWindow().getHandle();
        boolean leftDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

        if (!leftDown) {
            tibiaChatTabs$dragKey = null;
            tibiaChatTabs$dragActive = false;
            return;
        }

        if (!tibiaChatTabs$dragActive) {
            double dx = mouseX - tibiaChatTabs$dragPressX;
            double dy = mouseY - tibiaChatTabs$dragPressY;
            if (Math.abs(dx) < DRAG_THRESHOLD && Math.abs(dy) < DRAG_THRESHOLD) {
                return;
            }
            tibiaChatTabs$dragActive = true;
        }

        int tabsLeft = left + 2 + tibiaChatTabs$tabMainWidth();
        int tabsRight = right - 4;

        double pointerX = Math.max(tabsLeft, Math.min(tabsRight, (double) mouseX));

        tibiaChatTabs$updateDragOrder(mc, tabsLeft, tabsRight, pointerX);
    }

    private void tibiaChatTabs$updateDragOrder(MinecraftClient mc, int tabsLeft, int tabsRight, double pointerX) {
        ConversationManager conversations = TibiaChatTabsClient.CHAT.conversations();
        List<Conversation> order = conversations.asList();

        int currentIndex = -1;
        for (int i = 0; i < order.size(); i++) {
            if (order.get(i).key().equals(tibiaChatTabs$dragKey)) {
                currentIndex = i;
                break;
            }
        }

        if (currentIndex < 0) {
            return;
        }

        double x = tabsLeft - tibiaChatTabs$tabScroll;
        int targetIndex = currentIndex;

        for (int i = 0; i < order.size(); i++) {
            int w = tibiaChatTabs$tabWidth(mc, order.get(i).playerName());
            double mid = x + w / 2.0;

            if (i == currentIndex) {
                x += w;
                continue;
            }

            if (i < currentIndex) {
                if (pointerX < mid) {
                    targetIndex = Math.min(targetIndex, i);
                }
            } else {
                if (pointerX > mid) {
                    targetIndex = Math.max(targetIndex, i);
                }
            }

            x += w;
        }

        if (targetIndex != currentIndex) {
            conversations.moveToIndex(tibiaChatTabs$dragKey, targetIndex);
        }
    }

    private void tibiaChatTabs$closeTab(String key) {
        if (key.equals(tibiaChatTabs$dragKey)) {
            tibiaChatTabs$dragKey = null;
            tibiaChatTabs$dragActive = false;
        }

        String selectedKey = TibiaChatTabsClient.CHAT.selectedKey();
        boolean selected = key.equals(selectedKey);

        TibiaChatTabsClient.CHAT.conversations().remove(key);

        if (!selected) {
            tibiaChatTabs$clampTabScrollAfterClose();
            return;
        }

        String nextKey = ConversationManager.MAIN;
        for (Conversation c : TibiaChatTabsClient.CHAT.conversations().all()) {
            nextKey = c.key();
            break;
        }

        tibiaChatTabs$select(nextKey);
    }

    private void tibiaChatTabs$clampTabScrollAfterClose() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc.getWindow().getScaledWidth();

        int left = HudLayout.tabBarLeft();
        int right = HudLayout.tabBarRight(screenW);

        int tabsLeft = left + 2 + tibiaChatTabs$tabMainWidth();
        int tabsRight = right - 4;

        tibiaChatTabs$clampTabScroll(mc, tabsLeft, tabsRight);
    }

    private void tibiaChatTabs$select(String key) {
        if (TibiaChatTabsClient.CHAT.selectedKey().equals(key)) {
            return;
        }

        TibiaChatTabsClient.CHAT.select(key);

        ChatHud hud = MinecraftClient.getInstance().inGameHud.getChatHud();
        hud.clear(false);

        for (var msg : TibiaChatTabsClient.CHAT.selectedMessages()) {
            hud.addMessage(msg.component());
        }

        tibiaChatTabs$scrollSelectedIntoView();
    }

    private void tibiaChatTabs$scrollSelectedIntoView() {
        MinecraftClient mc = MinecraftClient.getInstance();
        int screenW = mc.getWindow().getScaledWidth();

        int left = HudLayout.tabBarLeft();
        int right = HudLayout.tabBarRight(screenW);

        int tabsLeft = left + 2 + tibiaChatTabs$tabMainWidth();
        int tabsRight = right - 4;

        String selectedKey = TibiaChatTabsClient.CHAT.selectedKey();

        if (ConversationManager.MAIN.equals(selectedKey)) {
            tibiaChatTabs$tabScroll = 0;
            return;
        }

        int tabX = tabsLeft;

        for (Conversation c : TibiaChatTabsClient.CHAT.conversations().all()) {
            int w = tibiaChatTabs$tabWidth(mc, c.playerName());

            if (c.key().equals(selectedKey)) {
                int visibleLeft = tabX - tibiaChatTabs$tabScroll;
                int visibleRight = visibleLeft + w;

                if (visibleLeft < tabsLeft) {
                    tibiaChatTabs$tabScroll -= tabsLeft - visibleLeft;
                } else if (visibleRight > tabsRight) {
                    tibiaChatTabs$tabScroll += visibleRight - tabsRight;
                }

                tibiaChatTabs$clampTabScroll(mc, tabsLeft, tabsRight);
                return;
            }

            tabX += w;
        }
    }

    @Inject(
            method = "keyPressed",
            at = @At("HEAD"),
            cancellable = true,
            require = 0
    )
    private void tibiaChatTabs$onKey(
            KeyInput input,
            CallbackInfoReturnable<Boolean> cir
    ) {
        if (input.key() != GLFW.GLFW_KEY_ENTER && input.key() != GLFW.GLFW_KEY_KP_ENTER) {
            return;
        }

        String key = TibiaChatTabsClient.CHAT.selectedKey();
        if (ConversationManager.MAIN.equals(key)) {
            return;
        }

        Conversation c = TibiaChatTabsClient.CHAT.conversations().getByKey(key);
        if (c == null || chatField == null) {
            return;
        }

        String text = chatField.getText().trim();
        if (text.isEmpty()) {
            cir.setReturnValue(true);
            return;
        }

        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            return;
        }

        String whisperCmd = TibiaChatTabsClient.CONFIG.whisperCommand();
        String body = whisperCmd + " " + c.playerName() + " " + text;
        String command = body.startsWith("/") ? body.substring(1) : body;

        mc.player.networkHandler.sendChatCommand(command);
        chatField.setText("");

        cir.setReturnValue(true);
    }
}
