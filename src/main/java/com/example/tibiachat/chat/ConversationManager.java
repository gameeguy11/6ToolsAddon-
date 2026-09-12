package com.example.tibiachat.chat;

import java.util.*;

public final class ConversationManager {
    public static final String MAIN = "main";
    private final Map<String, Conversation> conversations = new LinkedHashMap<>();

    public ConversationManager() {
    }

    public Conversation getOrCreate(String name, UUID uuid) {
        String key = name.toLowerCase(Locale.ROOT);
        Conversation c = conversations.get(key);
        if (c == null) {
            c = new Conversation(key, name, uuid);
            conversations.put(key, c);
        } else {
            c.updateIdentity(name, uuid);
        }
        return c;
    }

    public Conversation getByKey(String key) {
        return conversations.get(key);
    }

    public Collection<Conversation> all() {
        return Collections.unmodifiableCollection(conversations.values());
    }

    public boolean isEmpty() {
        return conversations.isEmpty();
    }

    public void remove(String key) {
        conversations.remove(key);
    }

    public List<Conversation> asList() {
        return new ArrayList<>(conversations.values());
    }

    public int indexOf(String key) {
        if (key == null) {
            return -1;
        }

        int i = 0;
        for (String k : conversations.keySet()) {
            if (k.equals(key)) {
                return i;
            }
            i++;
        }

        return -1;
    }

    public void moveToIndex(String key, int newIndex) {
        if (key == null || !conversations.containsKey(key)) {
            return;
        }

        List<Map.Entry<String, Conversation>> entries = new ArrayList<>(conversations.entrySet());

        int oldIndex = -1;
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i).getKey().equals(key)) {
                oldIndex = i;
                break;
            }
        }

        if (oldIndex < 0) {
            return;
        }

        int clampedIndex = Math.max(0, Math.min(entries.size() - 1, newIndex));
        if (clampedIndex == oldIndex) {
            return;
        }

        Map.Entry<String, Conversation> moved = entries.remove(oldIndex);
        entries.add(clampedIndex, moved);

        conversations.clear();
        for (Map.Entry<String, Conversation> entry : entries) {
            conversations.put(entry.getKey(), entry.getValue());
        }
    }
}