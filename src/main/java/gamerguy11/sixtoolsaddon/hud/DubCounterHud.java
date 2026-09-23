package gamerguy11.sixtoolsaddon.hud;

import gamerguy11.sixtoolsaddon.utils.ThemeColorUtils;
import gamerguy11.sixtoolsaddon.SixToolsAddon;
import gamerguy11.sixtoolsaddon.commands.DubCounterCommand;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.hud.Hud;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;

public class DubCounterHud extends HudElement {
    public static final HudElementInfo<DubCounterHud> INFO = new HudElementInfo<>(
        SixToolsAddon.HUD_GROUP,
        "dub-counter",
        "Shows the last result from the .dub command.",
        DubCounterHud::new
    );

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> showMode = sgGeneral.add(new BoolSetting.Builder()
        .name("show-mode")
        .description("Shows whether the count was Loaded or Rendered.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
        .name("shadow")
        .description("Renders a shadow behind the text.")
        .defaultValue(true)
        .build()
    );

    private final Setting<SettingColor> color = sgGeneral.add(new ColorSetting.Builder()
        .name("color")
        .description("Color used for the label text (the count itself stays white).")
        .defaultValue(new SettingColor(SixToolsAddon.THEME_COLOR.r, SixToolsAddon.THEME_COLOR.g, SixToolsAddon.THEME_COLOR.b))
        .build()
    );

    private final Setting<Boolean> colorUseTheme = sgGeneral.add(new BoolSetting.Builder()
        .name("color-use-theme")
        .description("Use the current Meteor theme accent color.")
        .defaultValue(false)
        .build()
    );

    private static final SettingColor WHITE = new SettingColor(255, 255, 255);

    public DubCounterHud() {
        super(INFO);
    }

    private String label() {
        return "Dubs: ";
    }

    private String number() {
        return DubCounterCommand.lastDubs < 0 ? "-" : String.valueOf(DubCounterCommand.lastDubs);
    }

    private String suffix() {
        if (!showMode.get() || DubCounterCommand.lastMode == null) return "";
        return " (" + DubCounterCommand.lastMode.name() + ")";
    }

    @Override
    public void tick(HudRenderer renderer) {
        double width = renderer.textWidth(label(), shadow.get(), getScale())
            + renderer.textWidth(number(), shadow.get(), getScale())
            + renderer.textWidth(suffix(), shadow.get(), getScale());

        setSize(width, renderer.textHeight(shadow.get(), getScale()));
    }

    @Override
    public void render(HudRenderer renderer) {
        double drawX = x;

        drawX = renderer.text(label(), drawX, y, ThemeColorUtils.resolve(color.get(), colorUseTheme.get()), shadow.get(), getScale());
        drawX = renderer.text(number(), drawX, y, WHITE, shadow.get(), getScale());
        renderer.text(suffix(), drawX, y, ThemeColorUtils.resolve(color.get(), colorUseTheme.get()), shadow.get(), getScale());
    }

    private double getScale() {
        return Hud.get().getTextScale();
    }
}
