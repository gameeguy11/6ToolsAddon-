package gamerguy11.sixtoolsaddon.modules.visual;

import gamerguy11.sixtoolsaddon.SixToolsAddon;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;

public class SwingSpeed extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("speed")
            .description("Multiplier for how fast your own arm swing animation plays. 1 = vanilla speed.")
            .defaultValue(1.0)
            .min(0.1)
            .sliderMax(5.0)
            .build()
    );

    public SwingSpeed() {
        super(SixToolsAddon.CATEGORY, "swing-speed", "Changes how fast your arm swing animation plays. Purely visual, client-side only.");
    }

    public double getSpeedMultiplier() {
        return speed.get();
    }
}
