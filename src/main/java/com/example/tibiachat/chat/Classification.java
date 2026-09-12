package com.example.tibiachat.chat;
import java.util.UUID;
public record Classification(MessageType type, String playerName, UUID playerUuid, String body, String conversationKey) {
    public boolean isWhisper(){ return type==MessageType.WHISPER_INCOMING || type==MessageType.WHISPER_OUTGOING; }
}
