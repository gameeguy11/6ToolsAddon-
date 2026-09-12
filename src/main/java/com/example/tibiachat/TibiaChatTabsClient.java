// Chat Tabs - bundled from https://github.com/gameeguy11/Chat-Tabs-Mod, originally by
// Fractal420. Package left unchanged so it drops in as a second, independent Fabric
// client entrypoint alongside AnarchyAddon's Meteor entrypoint - see fabric.mod.json's
// "entrypoints" and "mixins" for the wiring. Synced with the standalone mod's
// drag-to-reorder tabs and in-game settings screen (open with the configurable keybind).
package com.example.tibiachat;

import com.example.tibiachat.chat.ChatManager;
import com.example.tibiachat.chat.ConversationManager;
import com.example.tibiachat.config.TibiaChatConfig;
import com.example.tibiachat.gui.TibiaChatConfigScreen;
import com.example.tibiachat.hud.HudLayout;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientSendMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public final class TibiaChatTabsClient implements ClientModInitializer {
    public static final String MOD_ID = "tibia_chat_tabs";
    public static final TibiaChatConfig CONFIG = TibiaChatConfig.load();
    public static final ChatManager CHAT = new ChatManager();

    private static KeyBinding openSettingsKey;

    @Override
    public void onInitializeClient() {
        openSettingsKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.chat_tabs.open_settings",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_UNKNOWN,
                KeyBinding.Category.MISC
        ));

        ClientReceiveMessageEvents.ALLOW_CHAT.register((message, signedMessage, sender, params, timestamp) -> {
            String key = CHAT.classifyAndStore(message, sender, timestamp);
            if (key == null) return false;
            return key.equals(ConversationManager.MAIN) || key.equals(CHAT.selectedKey());
        });

        ClientReceiveMessageEvents.ALLOW_GAME.register((message, overlay) -> {
            if (overlay) return true;
            String key = CHAT.classifyAndStore(message, null, java.time.Instant.now());
            if (key == null) return false;
            return key.equals(ConversationManager.MAIN) || key.equals(CHAT.selectedKey());
        });

        ClientSendMessageEvents.COMMAND.register(CHAT::onOutgoingCommand);

        ClientLifecycleEvents.CLIENT_STOPPING.register(client -> CONFIG.save());

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            CHAT.tick();

            while (openSettingsKey.wasPressed()) {
                if (client.currentScreen == null) {
                    client.setScreen(new TibiaChatConfigScreen(null));
                }
            }
        });

        HudRenderCallback.EVENT.register(this::renderUnreadBadge);
    }

    private void renderUnreadBadge(DrawContext ctx, net.minecraft.client.render.RenderTickCounter tickCounter) {
        int unread = CHAT.totalUnread();
        if (unread <= 0) return;

        MinecraftClient mc = MinecraftClient.getInstance();
        String label = "\u2709 " + unread;
        int textWidth = mc.textRenderer.getWidth(label);

        int x = HudLayout.notifIconX();
        int y = HudLayout.notifIconY(mc.getWindow().getScaledHeight());

        int w = HudLayout.notifIconWidth(textWidth);
        int h = HudLayout.notifIconHeight();

        ctx.fill(x - 3, y - 2, x - 3 + w, y - 2 + h, 0x90000000);
        ctx.drawTextWithShadow(mc.textRenderer, Text.literal(label), x, y, 0xFFFFD24A);
    }
}
