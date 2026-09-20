package gamerguy11.sixtoolsaddon.modules.utility;

import gamerguy11.sixtoolsaddon.SixToolsAddon;
import gamerguy11.sixtoolsaddon.sound.SoundEngine;
import gamerguy11.sixtoolsaddon.sound.SoundType;
import gamerguy11.sixtoolsaddon.systems.enemies.Enemies;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.gui.GuiTheme;
import meteordevelopment.meteorclient.gui.widgets.WWidget;
import meteordevelopment.meteorclient.gui.widgets.containers.WHorizontalList;
import meteordevelopment.meteorclient.gui.widgets.containers.WSection;
import meteordevelopment.meteorclient.gui.widgets.containers.WVerticalList;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.settings.StringSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Util;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

public class SoundEditor extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Integer> masterVolume = sgGeneral.add(new IntSetting.Builder()
        .name("master-volume")
        .description("Overall volume of every sound this module plays (percent).")
        .defaultValue(100)
        .min(0).max(200)
        .sliderRange(0, 200)
        .build()
    );

    private final Setting<Boolean> followGameVolume = sgGeneral.add(new BoolSetting.Builder()
        .name("follow-game-volume")
        .description("Also scale by Minecraft's own Master Volume slider so these sounds obey it.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<String>> keywords;

    private final Map<SoundType, TypeSettings> types = new EnumMap<>(SoundType.class);

    private static final class TypeSettings {
        Setting<Boolean> enabled;
        Setting<Integer> volume;
        Setting<Double> pitch;
        Setting<Double> pitchVariation;
        Setting<Integer> cooldown;
        Setting<SoundEngine.Mode> mode;
        Setting<String> file;
        long lastPlayed;
    }

    private boolean wasDead;

    public SoundEditor() {
        super(SixToolsAddon.CATEGORY, "sound-editor",
            "Plays sounds from your own files for addon events. Put .ogg/.wav files in config/sixtoolsaddon/sounds/<type>/ "
                + "(one folder per sound type, created automatically). Several files in one folder? Pick Random, "
                + "Sequential or Specific per type below, or use the file list at the bottom of this window. "
                + "Nothing plays for a type until its folder has a file.");

        SoundEngine.INSTANCE.rescan();

        Setting<List<String>> keywordSetting = null;

        for (SoundType type : SoundType.values()) {
            SettingGroup group = settings.createGroup(type.display);
            TypeSettings ts = new TypeSettings();

            ts.enabled = group.add(new BoolSetting.Builder()
                .name("enabled")
                .description(type.description)
                .defaultValue(type.defaultEnabled)
                .build()
            );

            Setting<Boolean> enabled = ts.enabled;

            ts.volume = group.add(new IntSetting.Builder()
                .name("volume")
                .description("Volume of this sound (percent).")
                .defaultValue(type.defaultVolume)
                .min(0).max(200)
                .sliderRange(0, 200)
                .visible(enabled::get)
                .build()
            );

            ts.pitch = group.add(new DoubleSetting.Builder()
                .name("pitch")
                .description("Playback speed. 1 = normal, 2 = an octave higher and twice as fast.")
                .defaultValue(1.0)
                .min(0.25).max(4.0)
                .sliderRange(0.5, 2.0)
                .visible(enabled::get)
                .build()
            );

            ts.pitchVariation = group.add(new DoubleSetting.Builder()
                .name("pitch-variation")
                .description("Random pitch wobble each time it plays (0.2 = up to 20% either way). Makes repeats feel less robotic.")
                .defaultValue(type == SoundType.TYPING ? 0.2 : 0.0)
                .min(0).max(0.9)
                .sliderRange(0, 0.5)
                .visible(enabled::get)
                .build()
            );

            ts.cooldown = group.add(new IntSetting.Builder()
                .name("cooldown")
                .description("Minimum time between plays of this sound, in milliseconds. 0 = no limit.")
                .defaultValue(type.defaultCooldownMs)
                .min(0).max(60000)
                .sliderRange(0, 5000)
                .visible(enabled::get)
                .build()
            );

            ts.mode = group.add(new EnumSetting.Builder<SoundEngine.Mode>()
                .name("mode")
                .description("How to choose when the folder has several files. Random: any file, not the same twice in a row. "
                    + "Sequential: go through them in alphabetical order. Specific: always the file named below.")
                .defaultValue(SoundEngine.Mode.Random)
                .visible(enabled::get)
                .build()
            );

            Setting<SoundEngine.Mode> mode = ts.mode;

            ts.file = group.add(new StringSetting.Builder()
                .name("file")
                .description("File name to play in Specific mode, e.g. boom.ogg (the extension is optional). "
                    + "The file list at the bottom of this window has a 'Use' button that fills this in for you. "
                    + "If the file isn't found, a random one plays instead.")
                .defaultValue("")
                .visible(() -> enabled.get() && mode.get() == SoundEngine.Mode.Specific)
                .build()
            );

            if (type == SoundType.CHAT_KEYWORD) {
                keywordSetting = group.add(new StringListSetting.Builder()
                    .name("keywords")
                    .description("Words or phrases to listen for in incoming chat (not case sensitive). Empty by default: it never triggers until you add some.")
                    .visible(enabled::get)
                    .build()
                );
            }

            types.put(type, ts);
        }

        keywords = keywordSetting;
    }

    public void play(SoundType type) {
        play(type, false);
    }

    private void play(SoundType type, boolean ignoreActive) {
        if (!ignoreActive && !isActive()) return;

        TypeSettings ts = types.get(type);
        if (ts == null || !ts.enabled.get()) return;

        long now = System.currentTimeMillis();
        int cooldown = ts.cooldown.get();
        if (cooldown > 0 && now - ts.lastPlayed < cooldown) return;
        ts.lastPlayed = now;

        SoundEngine.INSTANCE.play(type, ts.mode.get(), ts.file.get(), volumeFor(ts), pitchFor(ts));
    }

    public void onModuleToggled(Module module) {
        if (mc.world == null && mc.currentScreen == null) return;

        boolean on = module.isActive();
        play(on ? SoundType.MODULE_ON : SoundType.MODULE_OFF, module == this && !on);
    }

    private void preview(SoundType type, String fileName) {
        TypeSettings ts = types.get(type);
        if (ts == null) return;

        SoundEngine.INSTANCE.playNamed(type, fileName, volumeFor(ts), pitchFor(ts));
    }

    private float volumeFor(TypeSettings ts) {
        float volume = (ts.volume.get() / 100f) * (masterVolume.get() / 100f);
        if (followGameVolume.get() && mc.options != null) volume *= mc.options.getSoundVolume(SoundCategory.MASTER);
        return volume;
    }

    private float pitchFor(TypeSettings ts) {
        double pitch = ts.pitch.get();
        double variation = ts.pitchVariation.get();
        if (variation > 0) pitch *= 1.0 + (ThreadLocalRandom.current().nextDouble() * 2.0 - 1.0) * variation;
        return (float) pitch;
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (event.action != KeyAction.Press) return;
        if (!(mc.currentScreen instanceof ChatScreen)) return;

        play(SoundType.TYPING);
    }

    @EventHandler
    public void onMessage(ReceiveMessageEvent event) {
        if (keywords == null || keywords.get().isEmpty()) return;

        String message = event.getMessage().getString().toLowerCase(Locale.ROOT);
        for (String keyword : keywords.get()) {
            if (keyword == null || keyword.isBlank()) continue;

            if (message.contains(keyword.toLowerCase(Locale.ROOT))) {
                play(SoundType.CHAT_KEYWORD);
                return;
            }
        }
    }

    @EventHandler
    public void onEntityAdded(EntityAddedEvent event) {
        if (!(event.entity instanceof PlayerEntity player)) return;
        if (player == mc.player) return;
        if (!Enemies.get().isEnemy(player.getName().getString())) return;

        play(SoundType.ENEMY_SPOTTED);
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        boolean dead = mc.player.getHealth() <= 0 || mc.player.isDead();
        if (dead && !wasDead) play(SoundType.DEATH);
        wasDead = dead;
    }

    @EventHandler
    public void onGameLeft(GameLeftEvent event) {
        wasDead = false;
    }

    @Override
    public void onActivate() {
        wasDead = false;
        SoundEngine.INSTANCE.rescan();
    }

    @Override
    public WWidget getWidget(GuiTheme theme) {
        SoundEngine engine = SoundEngine.INSTANCE;
        engine.rescan();

        WVerticalList list = theme.verticalList();

        WHorizontalList top = list.add(theme.horizontalList()).expandX().widget();
        top.add(theme.button("Open sounds folder")).expandX().widget().action = () -> Util.getOperatingSystem().open(engine.getRoot());
        top.add(theme.button("Reload files")).widget().action = () -> {
            engine.reload();
            mc.setScreen(theme.moduleScreen(this));
        };

        list.add(theme.label("Folder: " + engine.getRoot()));
        list.add(theme.label("Supports .ogg and .wav. Files are picked up automatically; press Reload after editing an existing file."));

        for (SoundType type : SoundType.values()) {
            List<String> names = engine.getFileNames(type);
            TypeSettings ts = types.get(type);

            WSection section = list.add(theme.section(type.display + " (" + names.size() + (names.size() == 1 ? " file)" : " files)"), false)).expandX().widget();
            section.add(theme.label(type.description));

            WHorizontalList actions = section.add(theme.horizontalList()).expandX().widget();
            actions.add(theme.button("Open folder")).widget().action = () -> Util.getOperatingSystem().open(engine.getFolder(type));

            if (names.isEmpty()) {
                section.add(theme.label("No sounds yet - drop .ogg / .wav files into sounds/" + type.folder + "/"));
                continue;
            }

            String current = ts.mode.get() == SoundEngine.Mode.Specific ? ts.file.get() : "";
            actions.add(theme.label("Mode: " + ts.mode.get() + (current.isBlank() ? "" : " (" + current + ")")));

            for (String name : names) {
                WHorizontalList row = section.add(theme.horizontalList()).expandX().widget();
                row.add(theme.label(name)).expandX();

                row.add(theme.button("Play")).widget().action = () -> preview(type, name);

                row.add(theme.button("Use")).widget().action = () -> {
                    ts.mode.set(SoundEngine.Mode.Specific);
                    ts.file.set(name);
                    mc.setScreen(theme.moduleScreen(this));
                };
            }
        }

        return list;
    }
}
