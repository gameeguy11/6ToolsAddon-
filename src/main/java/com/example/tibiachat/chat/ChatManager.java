package com.example.tibiachat.chat;

import com.example.tibiachat.TibiaChatTabsClient;
import com.example.tibiachat.config.TibiaChatConfig;
import com.mojang.authlib.GameProfile;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;

public final class ChatManager {
    private final TibiaChatConfig config = TibiaChatTabsClient.CONFIG;
    private final MessageClassifier classifier = new MessageClassifier(config);
    private final ConversationManager conversations = new ConversationManager();
    private final List<ChatMessage> main = new ArrayList<>();
    private final Deque<PendingEcho> pendingEchoes = new ArrayDeque<>();
    private String selectedKey = ConversationManager.MAIN;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

    public List<ChatMessage> main() {
        return Collections.unmodifiableList(main);
    }

    public ConversationManager conversations() {
        return conversations;
    }

    public String selectedKey() {
        return selectedKey;
    }

    public void select(String key) {
        selectedKey = key;
        Conversation c = conversations.getByKey(key);
        if (c != null) c.clearUnread();
    }

    public List<ChatMessage> selectedMessages() {
        if (ConversationManager.MAIN.equals(selectedKey)) return main;
        Conversation c = conversations.getByKey(selectedKey);
        return c == null ? List.of() : c.messages();
    }

    public Conversation selectedConversation() {
        return conversations.getByKey(selectedKey);
    }

    public int totalUnread() {
        int total = 0;
        for (Conversation c : conversations.all()) total += c.unread();
        return total;
    }

    public String classifyAndStore(Text message, GameProfile sender, Instant timestamp) {
        String raw = message.getString();
        Classification cl = classifier.incoming(message, sender);

        if (cl.type() == MessageType.WHISPER_OUTGOING) {
            String fp = fingerprint(cl.playerName(), cl.body());
            removeMatchingPendingBody(raw);
            removeMatchingPending(fp);

            ChatMessage cm = make(
                message,
                MessageType.WHISPER_OUTGOING,
                cl.playerName(),
                cl.playerUuid(),
                null,
                timestamp,
                fp
            );

            main.add(cm);
            return ConversationManager.MAIN;
        }

        if (cl.type() == MessageType.WHISPER_INCOMING) {
            String fp = fingerprint(cl.playerName(), cl.body());
            if (removeMatchingPending(fp)) return null;

            Conversation c = conversations.getOrCreate(cl.playerName(), cl.playerUuid());
            ChatMessage cm = make(
                message,
                MessageType.WHISPER_INCOMING,
                cl.playerName(),
                cl.playerUuid(),
                c.key(),
                timestamp,
                fp
            );

            c.add(cm);
            main.add(cm);

            if (!c.key().equals(selectedKey)) {
                c.markUnread();
            }

            return c.key();
        }

        ChatMessage cm = make(
            message,
            cl.type(),
            cl.playerName(),
            cl.playerUuid(),
            null,
            timestamp,
            fingerprint(cl.playerName(), raw)
        );

        main.add(cm);
        return ConversationManager.MAIN;
    }

    public void onOutgoingCommand(String command) {
        classifier.outgoingCommand(command).ifPresent(out -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc.player == null) return;

            Conversation c = conversations.getOrCreate(
                out.playerName(),
                findUuid(out.playerName())
            );

            String fp = fingerprint(out.playerName(), out.body());

            Text rendered = Text.literal("[")
                .append(Text.literal(TIME.format(LocalTime.now())))
                .append(Text.literal("] "))
                .append(Text.literal("You: "))
                .append(Text.literal(out.body()));

            ChatMessage cm = make(
                rendered,
                MessageType.WHISPER_OUTGOING,
                mc.player.getName().getString(),
                mc.player.getUuid(),
                c.key(),
                Instant.now(),
                fp
            );

            c.add(cm);
            pendingEchoes.addLast(new PendingEcho(fp, System.nanoTime()));

            if (c.key().equals(selectedKey)) {
                mc.inGameHud.getChatHud().addMessage(cm.component());
            }
        });
    }

    private UUID findUuid(String name) {
        MinecraftClient mc = MinecraftClient.getInstance();

        if (mc.player != null && mc.player.getName().getString().equalsIgnoreCase(name)) {
            return mc.player.getUuid();
        }

        if (mc.getNetworkHandler() != null) {
            var p = mc.getNetworkHandler().getPlayerListEntry(name);
            if (p != null) return p.getProfile().id();
        }

        return null;
    }

    private ChatMessage make(
        Text text,
        MessageType type,
        String name,
        UUID uuid,
        String key,
        Instant at,
        String fp
    ) {
        Text withTime = text;

        String raw = text.getString();
        if (!raw.startsWith("[") && (type == MessageType.WHISPER_INCOMING
            || type == MessageType.PUBLIC
            || type == MessageType.SYSTEM)) {
            withTime = Text.literal("[")
                .append(Text.literal(TIME.format(
                    at.atZone(ZoneId.systemDefault()).toLocalTime()
                )))
                .append(Text.literal("] "))
                .append(text);
        }

        return new ChatMessage(withTime, type, name, uuid, key, at, fp);
    }

    private String fingerprint(String name, String body) {
        return (name == null ? "" : name.toLowerCase(Locale.ROOT)) + "|" + body.trim();
    }

    private boolean removeMatchingPendingBody(String raw) {
        long now = System.nanoTime();

        while (!pendingEchoes.isEmpty()
            && now - pendingEchoes.peekFirst().nanoTime() > 5_000_000_000L) {
            pendingEchoes.removeFirst();
        }

        var it = pendingEchoes.iterator();

        while (it.hasNext()) {
            PendingEcho p = it.next();
            String body = p.fingerprint().substring(
                p.fingerprint().indexOf('|') + 1
            );

            if (!body.isBlank() && raw.contains(body)) {
                it.remove();
                return true;
            }
        }

        return false;
    }

    private boolean removeMatchingPending(String fp) {
        long now = System.nanoTime();

        while (!pendingEchoes.isEmpty()
            && now - pendingEchoes.peekFirst().nanoTime() > 5_000_000_000L) {
            pendingEchoes.removeFirst();
        }

        var it = pendingEchoes.iterator();

        while (it.hasNext()) {
            PendingEcho p = it.next();

            if (p.fingerprint().equals(fp)) {
                it.remove();
                return true;
            }
        }

        return false;
    }

    public void tick() {
    }

    private record PendingEcho(String fingerprint, long nanoTime) {
    }
}