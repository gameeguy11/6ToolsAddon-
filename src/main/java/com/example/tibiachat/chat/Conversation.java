package com.example.tibiachat.chat;

import java.util.*;
public final class Conversation {
    private final String key;
    private String playerName;
    private UUID playerUuid;
    private final List<ChatMessage> messages = new ArrayList<>();
    private int unread;
    private int scroll;

    public Conversation(String key, String playerName, UUID playerUuid) {
        this.key=key; this.playerName=playerName; this.playerUuid=playerUuid;
    }
    public String key(){return key;}
    public String playerName(){return playerName;}
    public UUID playerUuid(){return playerUuid;}
    public void updateIdentity(String name, UUID uuid){ if(name!=null&&!name.isBlank())playerName=name; if(uuid!=null)playerUuid=uuid; }
    public List<ChatMessage> messages(){return Collections.unmodifiableList(messages);}
    public void add(ChatMessage m){messages.add(m);}
    public int unread(){return unread;}
    public void markUnread(){unread++;}
    public void clearUnread(){unread=0;}
    public int scroll(){return scroll;}
    public void scrollBy(int delta){scroll=Math.max(0,scroll+delta);}
    public void setScroll(int value){scroll=Math.max(0,value);}
}
