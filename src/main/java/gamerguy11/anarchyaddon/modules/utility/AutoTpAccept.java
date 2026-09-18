package gamerguy11.anarchyaddon.modules.utility;

import gamerguy11.anarchyaddon.AnarchyAddon;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoTpAccept extends Module {
    public enum Mode {
        Friends,
        Enemies,
        Everyone
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Mode> mode = sgGeneral.add(new EnumSetting.Builder<Mode>()
            .name("mode")
            .description("Who to auto-accept teleport requests from.")
            .defaultValue(Mode.Friends)
            .build()
    );

    private final Setting<List<String>> enemyNames = sgGeneral.add(new StringListSetting.Builder()
            .name("enemy-names")
            .description("Player names treated as enemies. Only used when mode is Enemies.")
            .visible(() -> mode.get() == Mode.Enemies)
            .build()
    );

    private final Setting<String> requestPattern = sgGeneral.add(new StringSetting.Builder()
            .name("request-pattern")
            .description("Regex used to detect teleport requests. Capture group 1 must be the player's name.")
            .defaultValue("([A-Za-z0-9_.]{2,32}) wants to teleport to you\\.")
            .build()
    );

    private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
            .name("chat-feedback")
            .description("Shows when teleport requests are accepted or ignored.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
            .name("debug")
            .description("Shows teleport-related messages exactly as Meteor receives them.")
            .defaultValue(false)
            .build()
    );

    private Pattern compiledPattern;
    private String compiledFrom;
    private String lastHandledText;
    private long lastHandledAt;

    public AutoTpAccept() {
        super(
                AnarchyAddon.CATEGORY,
                "auto-tpy",
                "Automatically runs /tpy for teleport requests from friends, enemies, or everyone."
        );
    }

    @EventHandler
    private void onMessage(ReceiveMessageEvent event) {
        String text = event.getMessage().getString();

        if (debug.get() && text.toLowerCase(Locale.ROOT).contains("teleport")) {
            String debugText = text
                    .replace(" ", "_")
                    .replace("\n", "\\n")
                    .replace("\r", "\\r")
                    .replace("\t", "\\t");

            info("TPA DEBUG: [%s] length=%d", debugText, text.length());
        }

        String patternSource = requestPattern.get();

        if (compiledPattern == null || !patternSource.equals(compiledFrom)) {
            try {
                compiledPattern = Pattern.compile(
                        patternSource,
                        Pattern.CASE_INSENSITIVE
                );

                compiledFrom = patternSource;
            } catch (RuntimeException e) {
                error("Invalid request-pattern regex: %s", e.getMessage());
                toggle();
                return;
            }
        }

        Matcher matcher = compiledPattern.matcher(text);

        if (!matcher.find() || matcher.groupCount() < 1) {
            return;
        }

        String requester = matcher.group(1);

        long now = System.currentTimeMillis();
        if (text.equals(lastHandledText) && (now - lastHandledAt) < 2000) {
            return;
        }
        lastHandledText = text;
        lastHandledAt = now;

        if (!shouldAccept(requester)) {
            if (chatFeedback.get()) {
                info(
                        "Ignoring teleport request from (highlight)%s(default) - not allowed by current mode.",
                        requester
                );
            }

            return;
        }

        ChatUtils.sendPlayerMsg("/tpy " + requester);

        if (chatFeedback.get()) {
            info(
                    "Auto-accepted teleport request from (highlight)%s(default).",
                    requester
            );
        }
    }

    private boolean shouldAccept(String requester) {
        return switch (mode.get()) {
            case Everyone -> true;
            case Friends -> isFriend(requester);
            case Enemies -> isEnemy(requester);
        };
    }

    private boolean isFriend(String requester) {
        return Friends.get().get(requester) != null;
    }

    private boolean isEnemy(String requester) {
        for (String enemy : enemyNames.get()) {
            if (enemy.equalsIgnoreCase(requester)) {
                return true;
            }
        }

        return false;
    }
}