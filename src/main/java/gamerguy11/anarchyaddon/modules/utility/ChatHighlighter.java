package gamerguy11.anarchyaddon.modules.utility;

import gamerguy11.anarchyaddon.AnarchyAddon;
import gamerguy11.anarchyaddon.systems.enemies.Enemies;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.MutableText;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ChatHighlighter extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgColors = settings.createGroup("Colors");

    private final Setting<Boolean> highlightSelf = sgGeneral.add(new BoolSetting.Builder()
            .name("highlight-self")
            .description("Colors your own name in chat.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> highlightFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("highlight-friends")
            .description("Colors the names of players on your Meteor friends list.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> highlightEnemies = sgGeneral.add(new BoolSetting.Builder()
            .name("highlight-enemies")
            .description("Colors the names of players on your enemies list (.enemy add/remove/list).")
            .defaultValue(true)
            .build()
    );

    private final Setting<String> usernamePattern = sgGeneral.add(new StringSetting.Builder()
            .name("username-pattern")
            .description("Regex used to find the sender's name at the start of a chat line. Capture group 1 must be the name.")
            .defaultValue("([A-Za-z0-9_]{2,16})")
            .build()
    );

    private final Setting<SettingColor> selfColor = sgColors.add(new ColorSetting.Builder()
            .name("self-color")
            .description("Color for your own name.")
            .defaultValue(new SettingColor(AnarchyAddon.THEME_COLOR.r, AnarchyAddon.THEME_COLOR.g, AnarchyAddon.THEME_COLOR.b))
            .visible(highlightSelf::get)
            .build()
    );

    private final Setting<SettingColor> friendColor = sgColors.add(new ColorSetting.Builder()
            .name("friend-color")
            .description("Color for players on your Meteor friends list.")
            .defaultValue(new SettingColor(75, 225, 75))
            .visible(highlightFriends::get)
            .build()
    );

    private final Setting<SettingColor> enemyColor = sgColors.add(new ColorSetting.Builder()
            .name("enemy-color")
            .description("Color for players on your enemies list.")
            .defaultValue(new SettingColor(225, 75, 75))
            .visible(highlightEnemies::get)
            .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .description("Prints each chat line's exact characters (as unicode escapes) and whether the pattern matched.")
            .defaultValue(false)
            .build()
    );

    private Pattern compiledPattern;
    private String compiledFrom;

    public ChatHighlighter() {
        super(
                AnarchyAddon.CATEGORY,
                "chat-highlight",
                "Colors player names in chat: yourself, friends, and a custom enemy list."
        );
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        Text message = event.getMessage();

        List<Segment> segments = collectSegments(message);
        String plain = joinSegments(segments);
        if (plain.isEmpty()) return;
        if (plain.contains("[Chat Highlight]")) return;

        Pattern pattern = compilePattern();
        if (pattern == null) return;

        Matcher matcher = pattern.matcher(plain);
        boolean matched = matcher.find() && matcher.groupCount() >= 1;

        if (debug.get()) {
            info("HL DEBUG: [%s] matched=%s", escapeNonAscii(plain), matched);
        }

        if (!matched) return;

        String name = matcher.group(1);
        if (name == null || name.isEmpty()) return;

        SettingColor color = colorFor(name);

        if (debug.get()) {
            String selfName = mc.player == null ? "null" : mc.player.getName().getString();
            info(
                "HL DEBUG: name='%s' isSelf=%s selfName='%s' isFriend=%s isEnemy=%s color=%s",
                name,
                isSelf(name),
                selfName,
                Friends.get().get(name) != null,
                Enemies.get().isEnemy(name),
                color == null ? "null" : (color.r + "," + color.g + "," + color.b)
            );
        }

        if (color == null) return;

        try {
            event.setMessage(highlightRange(segments, matcher.start(1), matcher.end(1), color));
        } catch (RuntimeException e) {
            error("Failed to highlight name '%s': %s", name, e.toString());
        }
    }

    private String escapeNonAscii(String text) {
        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);

            if (c == ' ') {
                sb.append('_');
            } else if (c >= 0x20 && c < 0x7F) {
                sb.append(c);
            } else {
                sb.append(String.format("\\u%04X", (int) c));
            }
        }

        return sb.toString();
    }

    private Pattern compilePattern() {
        String patternSource = usernamePattern.get();

        if (compiledPattern == null || !patternSource.equals(compiledFrom)) {
            try {
                compiledPattern = Pattern.compile(patternSource);
                compiledFrom = patternSource;
            } catch (RuntimeException e) {
                error("Invalid username-pattern regex: %s", e.getMessage());
                toggle();
                return null;
            }
        }

        return compiledPattern;
    }

    private SettingColor colorFor(String name) {
        if (highlightSelf.get() && isSelf(name)) return selfColor.get();
        if (highlightFriends.get() && Friends.get().get(name) != null) return friendColor.get();
        if (highlightEnemies.get() && Enemies.get().isEnemy(name)) return enemyColor.get();
        return null;
    }

    private boolean isSelf(String name) {
        if (mc.player == null) return false;
        return mc.player.getName().getString().equalsIgnoreCase(name);
    }

    private record Segment(String text, Style style) {}

    private List<Segment> collectSegments(Text message) {
        List<Segment> segments = new ArrayList<>();

        message.visit((style, text) -> {
            if (!text.isEmpty()) {
                segments.add(new Segment(text, style));
            }
            return Optional.empty();
        }, Style.EMPTY);

        return segments;
    }

    private String joinSegments(List<Segment> segments) {
        StringBuilder sb = new StringBuilder();
        for (Segment segment : segments) sb.append(segment.text());
        return sb.toString();
    }

    private Text highlightRange(List<Segment> segments, int start, int end, SettingColor color) {
        TextColor textColor = TextColor.fromRgb(((color.r & 0xFF) << 16) | ((color.g & 0xFF) << 8) | (color.b & 0xFF));

        MutableText result = Text.empty();
        int pos = 0;

        for (Segment segment : segments) {
            String text = segment.text();
            Style style = segment.style();

            int segStart = pos;
            int segEnd = segStart + text.length();
            pos = segEnd;

            int overlapStart = Math.max(segStart, start);
            int overlapEnd = Math.min(segEnd, end);

            if (overlapEnd <= overlapStart) {
                result.append(Text.literal(text).setStyle(style));
            } else {
                if (overlapStart > segStart) {
                    result.append(Text.literal(text.substring(0, overlapStart - segStart)).setStyle(style));
                }

                result.append(Text.literal(text.substring(overlapStart - segStart, overlapEnd - segStart))
                        .setStyle(style.withColor(textColor)));

                if (overlapEnd < segEnd) {
                    result.append(Text.literal(text.substring(overlapEnd - segStart)).setStyle(style));
                }
            }
        }

        return result;
    }
}
