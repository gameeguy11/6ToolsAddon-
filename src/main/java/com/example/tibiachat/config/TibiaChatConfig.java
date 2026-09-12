package com.example.tibiachat.config;

import com.google.gson.*;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;

public final class TibiaChatConfig {

    public static final float MIN_BAR_SCALE = 0.5f;
    public static final float MAX_BAR_SCALE = 2.0f;
    public static final float MIN_ICON_SCALE = 0.5f;
    public static final float MAX_ICON_SCALE = 3.0f;
    public static final int MIN_BAR_OFFSET = -2000;
    public static final int MAX_BAR_OFFSET = 2000;
    public static final int MIN_ICON_OFFSET = -2000;
    public static final int MAX_ICON_OFFSET = 2000;
    public static final int MIN_BAR_WIDTH = 50;
    public static final int MAX_BAR_WIDTH = 2000;

    private float tabBarScale = 1.0f;
    private float notifIconScale = 1.0f;
    private int tabBarOffsetX = 0;
    private int tabBarOffsetY = 0;
    private int tabBarWidth = 300;
    private int notifIconOffsetX = 0;
    private int notifIconOffsetY = 0;

    public float tabBarScale() { return tabBarScale; }
    public void setTabBarScale(float v) { tabBarScale = clamp(v, MIN_BAR_SCALE, MAX_BAR_SCALE); }

    public float notifIconScale() { return notifIconScale; }
    public void setNotifIconScale(float v) { notifIconScale = clamp(v, MIN_ICON_SCALE, MAX_ICON_SCALE); }

    public int tabBarOffsetX() { return tabBarOffsetX; }
    public void setTabBarOffsetX(int v) { tabBarOffsetX = (int) clamp(v, MIN_BAR_OFFSET, MAX_BAR_OFFSET); }

    public int tabBarOffsetY() { return tabBarOffsetY; }
    public void setTabBarOffsetY(int v) { tabBarOffsetY = (int) clamp(v, MIN_BAR_OFFSET, MAX_BAR_OFFSET); }

    public int tabBarWidth() { return tabBarWidth; }
    public void setTabBarWidth(int v) { tabBarWidth = (int) clamp(v, MIN_BAR_WIDTH, MAX_BAR_WIDTH); }

    public int notifIconOffsetX() { return notifIconOffsetX; }
    public void setNotifIconOffsetX(int v) { notifIconOffsetX = (int) clamp(v, MIN_ICON_OFFSET, MAX_ICON_OFFSET); }

    public int notifIconOffsetY() { return notifIconOffsetY; }
    public void setNotifIconOffsetY(int v) { notifIconOffsetY = (int) clamp(v, MIN_ICON_OFFSET, MAX_ICON_OFFSET); }

    public void resetHudDefaults() {
        tabBarScale = 1.0f;
        notifIconScale = 1.0f;
        tabBarOffsetX = 0;
        tabBarOffsetY = 0;
        tabBarWidth = 300;
        notifIconOffsetX = 0;
        notifIconOffsetY = 0;
    }

    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private String whisperCommand = "/w";
    private List<String> whisperAliases = new ArrayList<>(List.of("w", "msg", "tell", "whisper"));
    private List<String> incomingWhisperRegexes = new ArrayList<>(List.of(
            "^(?:\\[\\d{2}:\\d{2}\\]\\s*)?([a-zA-Z0-0_]{3,16})\\s+whispers:\\s*(.*)$",
            "^(?:\\[\\d{2}:\\d{2}\\]\\s*)?([a-zA-Z0-9_]{3,16})\\s+->\\s+you:\\s*(.*)$"
    ));

    public String whisperCommand() { return whisperCommand; }
    public List<String> incomingWhisperRegexes() { return Collections.unmodifiableList(incomingWhisperRegexes); }

    public boolean isWhisperCommand(String base) {
        String configured = whisperCommand.startsWith("/") ? whisperCommand.substring(1) : whisperCommand;
        return base.equalsIgnoreCase(configured) || whisperAliases.stream().anyMatch(a -> a.equalsIgnoreCase(base));
    }

    public void setWhisperCommand(String value) {
        if (value != null && !value.isBlank()) whisperCommand = value.startsWith("/") ? value : "/" + value;
    }

    public static TibiaChatConfig load() {
        Path p = path();
        if (!Files.exists(p)) return new TibiaChatConfig();
        try {
            return new Gson().fromJson(Files.readString(p), TibiaChatConfig.class);
        } catch (Exception e) {
            return new TibiaChatConfig();
        }
    }

    public void save() {
        try {
            Files.createDirectories(path().getParent());
            Files.writeString(path(), new GsonBuilder().setPrettyPrinting().create().toJson(this));
        } catch (Exception ignored) {}
    }

    private static Path path() {
        return FabricLoader.getInstance().getConfigDir().resolve("tibia_chat_tabs.json");
    }
}