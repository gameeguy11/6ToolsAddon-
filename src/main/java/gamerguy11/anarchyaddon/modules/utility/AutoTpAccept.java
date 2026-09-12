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
import net.minecraft.client.network.PlayerListEntry;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static meteordevelopment.meteorclient.MeteorClient.mc;

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
        .description("Player names treated as enemies (not case sensitive). Only used when mode is Enemies. Meteor has no built-in enemy list, so this is addon-managed, same as Player Tracker HUD's list.")
        .visible(() -> mode.get() == Mode.Enemies)
        .build()
    );

    private final Setting<String> requestPattern = sgGeneral.add(new StringSetting.Builder()
        .name("request-pattern")
        .description("Regex matched against each incoming chat line, with capture group 1 being the requester's name. Default matches 6b6t's actual /tpa notification (\"<name> wants to teleport to you.\") - only needs changing if you're playing on a different server.")
        .defaultValue("^(\\w+) wants to teleport to you\\.$")
        .build()
    );

    private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-feedback")
        .description("Prints who was auto-accepted (or ignored) in chat.")
        .defaultValue(true)
        .build()
    );

    private Pattern compiledPattern;
    private String compiledFrom;

    public AutoTpAccept() {
        super(AnarchyAddon.CATEGORY, "auto-tpy", "Automatically runs /tpy for teleport requests from friends, enemies, or everyone.");
    }

    @EventHandler
    private void onMessage(ReceiveMessageEvent event) {
        String text = event.getMessage().getString();

        String patternSource = requestPattern.get();
        if (compiledPattern == null || !patternSource.equals(compiledFrom)) {
            try {
                compiledPattern = Pattern.compile(patternSource);
                compiledFrom = patternSource;
            } catch (RuntimeException e) {
                error("Invalid request-pattern regex: %s", e.getMessage());
                toggle();
                return;
            }
        }

        Matcher matcher = compiledPattern.matcher(text);
        if (!matcher.find() || matcher.groupCount() < 1) return;

        String requester = matcher.group(1);

        if (!shouldAccept(requester)) {
            if (chatFeedback.get()) info("Ignoring teleport request from (highlight)%s(default) - not on the allowed list.", requester);
            return;
        }

        ChatUtils.sendPlayerMsg("/tpy " + requester);
        if (chatFeedback.get()) info("Auto-accepted teleport request from (highlight)%s(default).", requester);
    }

    private boolean shouldAccept(String requester) {
        return switch (mode.get()) {
            case Everyone -> true;
            case Friends -> isFriend(requester);
            case Enemies -> isEnemy(requester);
        };
    }

    private boolean isFriend(String requester) {
        if (mc.player == null || mc.player.networkHandler == null) return false;

        PlayerListEntry entry = mc.player.networkHandler.getPlayerListEntry(requester);
        return entry != null && Friends.get().isFriend(entry);
    }

    private boolean isEnemy(String requester) {
        for (String enemy : enemyNames.get()) {
            if (enemy.equalsIgnoreCase(requester)) return true;
        }
        return false;
    }
}
