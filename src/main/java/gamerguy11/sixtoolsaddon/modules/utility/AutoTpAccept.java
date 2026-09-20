package gamerguy11.sixtoolsaddon.modules.utility;

import gamerguy11.sixtoolsaddon.SixToolsAddon;
import gamerguy11.sixtoolsaddon.homes.Home;
import gamerguy11.sixtoolsaddon.homes.HomeStore;
import gamerguy11.sixtoolsaddon.systems.enemies.Enemies;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
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
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> acceptFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("accept-friends")
            .description("Auto-accept requests from friends.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> acceptEnemies = sgGeneral.add(new BoolSetting.Builder()
            .name("accept-enemies")
            .description("Auto-accept requests from enemies.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> acceptEveryone = sgGeneral.add(new BoolSetting.Builder()
            .name("accept-everyone")
            .description("Auto-accept requests from everyone.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreEnemies = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-enemy-players")
            .description("Never react to requests from enemies, even with accept-everyone on.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> denyEnemies = sgGeneral.add(new BoolSetting.Builder()
            .name("deny-enemies")
            .description("Answer requests from enemies with /tpn.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> denyFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("deny-friends")
            .description("Answer requests from friends with /tpn.")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<String>> enemyNames = sgGeneral.add(new StringListSetting.Builder()
            .name("enemy-names")
            .description("Extra player names treated as enemies (the Enemies tab is always used too).")
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
    private String lastHandledRequester;
    private long lastHandledAt;

    public AutoTpAccept() {
        super(
                SixToolsAddon.CATEGORY,
                "auto-tpy",
                "Automatically runs /tpy for teleport requests. Respects protected homes."
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
        if (requester.equalsIgnoreCase(lastHandledRequester) && (now - lastHandledAt) < 2000) {
            return;
        }
        lastHandledRequester = requester;
        lastHandledAt = now;

        boolean friend = isFriend(requester);
        boolean enemy = isEnemy(requester);

        // Home protection beats everything else.
        Home home = HomeStore.protectedHomeAtPlayer();
        if (home != null && !(friend && home.allowFriends)) {
            if (home.denyInstead) ChatUtils.sendPlayerMsg("/tpn " + requester);

            if (chatFeedback.get()) {
                info("Home (highlight)%s(default) is protected - %s request from (highlight)%s(default).",
                        home.name, home.denyInstead ? "denied" : "ignored", requester);
            }
            return;
        }

        if (enemy && ignoreEnemies.get()) {
            if (chatFeedback.get()) info("Ignoring request from enemy (highlight)%s(default).", requester);
            return;
        }

        if ((enemy && denyEnemies.get()) || (friend && denyFriends.get())) {
            ChatUtils.sendPlayerMsg("/tpn " + requester);
            if (chatFeedback.get()) info("Denied teleport request from (highlight)%s(default).", requester);
            return;
        }

        if (!shouldAccept(friend, enemy)) {
            if (chatFeedback.get()) {
                info("Ignoring teleport request from (highlight)%s(default) - not allowed by current settings.", requester);
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

    private boolean shouldAccept(boolean friend, boolean enemy) {
        if (acceptEveryone.get()) return true;
        return (friend && acceptFriends.get()) || (enemy && acceptEnemies.get());
    }

    private boolean isFriend(String requester) {
        return Friends.get().get(requester) != null;
    }

    private boolean isEnemy(String requester) {
        if (Enemies.get().get(requester) != null) return true;

        for (String enemy : enemyNames.get()) {
            if (enemy.equalsIgnoreCase(requester)) {
                return true;
            }
        }

        return false;
    }
}