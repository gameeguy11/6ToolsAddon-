package gamerguy11.anarchyaddon.modules.utility;

import com.google.gson.Gson;
import gamerguy11.anarchyaddon.AnarchyAddon;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.MinecraftClient;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DiscordNotifier extends Module {

    private static final Logger LOGGER = Logger.getLogger("AnarchyAddon-DiscordNotifier");
    private static final Gson GSON = new Gson();
    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .build();

    private static final int FLUSH_INTERVAL_SECONDS = 2;
    private static final int DISCORD_CONTENT_LIMIT = 1900;
    private static final int MAX_QUEUED_LINES = 500;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> sendChat = sgGeneral.add(new BoolSetting.Builder()
            .name("send-chat")
            .description("Forwards every chat message to your Discord webhook.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> sendCoords = sgGeneral.add(new BoolSetting.Builder()
            .name("send-coords")
            .description("Forwards messages from other players that contain coordinates to your Discord webhook.")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ignoreOwnMessages = sgGeneral.add(new BoolSetting.Builder()
            .name("ignore-own-messages")
            .description("Doesn't forward chat messages you sent yourself.")
            .defaultValue(true)
            .build()
    );

    private final Setting<String> coordPattern = sgGeneral.add(new StringSetting.Builder()
            .name("coord-pattern")
            .description("Regex used to detect coordinates in a chat message.")
            .defaultValue("-?\\d{1,7}[,\\s]+-?\\d{1,4}[,\\s]+-?\\d{1,7}")
            .build()
    );

    private final Setting<Boolean> sendDeathCoords = sgGeneral.add(new BoolSetting.Builder()
            .name("send-death-coords")
            .description("Forwards your coordinates to your Discord webhook whenever you die.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> sendKills = sgGeneral.add(new BoolSetting.Builder()
            .name("send-kills")
            .description("Forwards the name of any player you killed to your Discord webhook.")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> sendKilledBy = sgGeneral.add(new BoolSetting.Builder()
            .name("send-killed-by")
            .description("Forwards the name of whoever killed you to your Discord webhook.")
            .defaultValue(false)
            .build()
    );

    private final Setting<String> deathPattern = sgGeneral.add(new StringSetting.Builder()
            .name("death-pattern")
            .description("Regex used to detect death messages. Named group <victim> must be the player who died; named group <killer>, if present, is whoever killed them.")
            .defaultValue("^(?<victim>[A-Za-z0-9_]{1,16}) (?:was slain by|was shot by|was fireballed by|was killed by|was pummeled by|was impaled by|was stung to death by|was poked to death by|was killed trying to hurt|was doomed to fall by|was blown up by|was shot off some vertical surface by|walked into a cactus while trying to escape|drowned|died|blew up|hit the ground too hard|fell from a high place|fell off|went up in flames|burned to death|was burned to a crisp|tried to swim in lava|suffocated in a wall|froze to death|starved to death|withered away|was struck by lightning)(?:.*?\\bby (?<killer>[A-Za-z0-9_]{1,16}))?")
            .build()
    );

    private final Setting<String> webhookUrl = sgGeneral.add(new StringSetting.Builder()
            .name("webhook-url")
            .description("Discord webhook URL. Use .setdiscord set/clear to manage this.")
            .defaultValue("")
            .visible(() -> false)
            .build()
    );

    private Pattern compiledCoordPattern;
    private String compiledCoordFrom;

    private Pattern compiledDeathPattern;
    private String compiledDeathFrom;

    private final Queue<String> queue = new ConcurrentLinkedQueue<>();
    private ScheduledExecutorService scheduler;

    public DiscordNotifier() {
        super(
                AnarchyAddon.CATEGORY,
                "discord-notifier",
                "Forwards chat and/or coordinates shared by other players to a Discord webhook."
        );
    }

    @Override
    public void onActivate() {
        queue.clear();
        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "AnarchyAddon-DiscordNotifier");
            t.setDaemon(true);
            return t;
        });
        scheduler.scheduleAtFixedRate(this::flush, FLUSH_INTERVAL_SECONDS, FLUSH_INTERVAL_SECONDS, TimeUnit.SECONDS);
    }

    @Override
    public void onDeactivate() {
        if (scheduler != null) {
            scheduler.shutdownNow();
            scheduler = null;
        }
        queue.clear();
    }

    @EventHandler
    private void onMessage(ReceiveMessageEvent event) {
        String text = event.getMessage().getString();
        if (text.isEmpty()) return;

        boolean own = isOwnMessage(text);
        if (own && ignoreOwnMessages.get()) return;

        if (sendChat.get()) {
            enqueue(text);
        }

        if (sendCoords.get() && containsCoords(text)) {
            enqueue("[Coords] " + text);
        }

        checkDeathMessage(text);
    }

    private boolean isOwnMessage(String text) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getSession() == null) return false;

        String username = client.getSession().getUsername();
        if (username == null || username.isEmpty()) return false;

        String prefix = "<" + username + ">";
        return text.regionMatches(true, 0, prefix, 0, prefix.length());
    }

    private boolean containsCoords(String text) {
        String patternSource = coordPattern.get();

        if (compiledCoordPattern == null || !patternSource.equals(compiledCoordFrom)) {
            try {
                compiledCoordPattern = Pattern.compile(patternSource);
                compiledCoordFrom = patternSource;
            } catch (RuntimeException e) {
                error("Invalid coord-pattern regex: %s", e.getMessage());
                toggle();
                return false;
            }
        }

        return compiledCoordPattern.matcher(text).find();
    }

    private void checkDeathMessage(String text) {
        if (!sendDeathCoords.get() && !sendKills.get() && !sendKilledBy.get()) return;

        Pattern pattern = compileDeathPattern();
        if (pattern == null) return;

        Matcher matcher = pattern.matcher(text);
        if (!matcher.find()) return;

        String victim = matcher.group("victim");
        if (victim == null || victim.isEmpty()) return;

        String killer;
        try {
            killer = matcher.group("killer");
        } catch (IllegalArgumentException e) {
            killer = null;
        }

        String localUsername = localUsername();
        if (localUsername == null) return;

        boolean weAreVictim = victim.equalsIgnoreCase(localUsername);
        boolean weAreKiller = killer != null && killer.equalsIgnoreCase(localUsername);

        if (weAreVictim && sendDeathCoords.get()) {
            enqueue("[Death] Died at " + formatOwnCoords());
        }

        if (weAreVictim && killer != null && sendKilledBy.get()) {
            enqueue("[Killed By] " + killer);
        }

        if (weAreKiller && !weAreVictim && sendKills.get()) {
            enqueue("[Kill] " + victim);
        }
    }

    private Pattern compileDeathPattern() {
        String patternSource = deathPattern.get();

        if (compiledDeathPattern == null || !patternSource.equals(compiledDeathFrom)) {
            try {
                compiledDeathPattern = Pattern.compile(patternSource);
                compiledDeathFrom = patternSource;
            } catch (RuntimeException e) {
                error("Invalid death-pattern regex: %s", e.getMessage());
                toggle();
                return null;
            }
        }

        return compiledDeathPattern;
    }

    private String localUsername() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.getSession() == null) return null;
        return client.getSession().getUsername();
    }

    private String formatOwnCoords() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return "unknown coordinates";
        return client.player.getBlockX() + ", " + client.player.getBlockY() + ", " + client.player.getBlockZ();
    }

    private void enqueue(String line) {
        while (queue.size() >= MAX_QUEUED_LINES) {
            queue.poll();
        }
        queue.offer(line);
    }

    private void flush() {
        try {
            String url = webhookUrl.get();
            if (url == null || url.isBlank()) {
                queue.clear();
                return;
            }

            if (queue.isEmpty()) return;

            StringBuilder batch = new StringBuilder();
            String line;
            while ((line = queue.peek()) != null) {
                int extra = (batch.length() > 0 ? 1 : 0) + line.length();
                if (batch.length() + extra > DISCORD_CONTENT_LIMIT) {
                    if (batch.length() == 0) {
                        queue.poll();
                        batch.append(line, 0, DISCORD_CONTENT_LIMIT);
                    }
                    break;
                }

                queue.poll();
                if (batch.length() > 0) batch.append('\n');
                batch.append(line);
            }

            if (batch.length() > 0) {
                send(url, batch.toString());
            }
        } catch (RuntimeException e) {
            LOGGER.warning("DiscordNotifier flush failed: " + e.getMessage());
        }
    }

    private void send(String url, String content) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("content", content);
        String json = GSON.toJson(body);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(java.time.Duration.ofSeconds(10))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json, StandardCharsets.UTF_8))
                .build();

        HTTP.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                .thenAccept(response -> {
                    int status = response.statusCode();
                    if (status >= 300) {
                        LOGGER.warning("Discord webhook returned status " + status);
                    }
                })
                .exceptionally(error -> {
                    LOGGER.warning("Failed to send Discord webhook: " + error.getMessage());
                    return null;
                });
    }

    public boolean isValidWebhookUrl(String url) {
        if (url == null) return false;
        String lower = url.trim().toLowerCase(Locale.ROOT);
        return lower.startsWith("https://discord.com/api/webhooks/")
                || lower.startsWith("https://discordapp.com/api/webhooks/")
                || lower.startsWith("https://ptb.discord.com/api/webhooks/")
                || lower.startsWith("https://canary.discord.com/api/webhooks/");
    }

    public void setWebhookUrl(String url) {
        webhookUrl.set(url.trim());
    }

    public void clearWebhookUrl() {
        webhookUrl.set("");
        queue.clear();
    }

    public boolean hasWebhook() {
        String url = webhookUrl.get();
        return url != null && !url.isBlank();
    }

    public void notifyInfo(String message, Object... args) {
        info(message, args);
    }

    public void notifyError(String message, Object... args) {
        error(message, args);
    }
}
