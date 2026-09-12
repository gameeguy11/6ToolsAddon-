package com.example.tibiachat.chat;

import java.time.Instant;
import java.util.UUID;
import net.minecraft.text.Text;

public record ChatMessage(
    Text component,
    MessageType type,
    String speakerName,
    UUID speakerUuid,
    String conversationKey,
    Instant receivedAt,
    String fingerprint
) {}
