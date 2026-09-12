package com.example.tibiachat.commands;

import com.example.tibiachat.TibiaChatTabsClient;
import com.example.tibiachat.chat.Conversation;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

public class ChatTabsDebugCommand extends Command {
    public ChatTabsDebugCommand() {
        super("chattabsdebug", "Prints internal chat tabs state.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            ChatUtils.info("Selected tab: " + TibiaChatTabsClient.CHAT.selectedKey());
            ChatUtils.info("Main history size: " + TibiaChatTabsClient.CHAT.main().size());
            ChatUtils.info("Open tabs: " + TibiaChatTabsClient.CHAT.conversations().all().size());

            for (Conversation c : TibiaChatTabsClient.CHAT.conversations().all()) {
                ChatUtils.info(" - " + c.playerName() + " (" + c.key() + "): "
                    + c.messages().size() + " messages, " + c.unread() + " unread");
            }

            return SINGLE_SUCCESS;
        });
    }
}
