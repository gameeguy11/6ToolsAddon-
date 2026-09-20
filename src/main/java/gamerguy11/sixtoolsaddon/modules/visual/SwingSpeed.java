package gamerguy11.sixtoolsaddon.modules.visual;

import gamerguy11.sixtoolsaddon.SixToolsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import gamerguy11.sixtoolsaddon.mixin.swing.LivingEntitySwingAccessor;

public class SwingSpeed extends Module {
    private static final float OUT_TICKS_BASE = 3f;
    private static final float BACK_TICKS_BASE = 3f;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("out-speed")
            .description("Multiplier for how fast the outward swing plays. 1 = vanilla speed, lower = slower. The return to rest always plays at vanilla speed.")
            .defaultValue(1.0)
            .min(0.001)
            .sliderMin(0.001)
            .sliderMax(5.0)
            .build()
    );

    private float progress = 0f;
    private float lastProgress = 0f;
    private boolean animating = false;

    public SwingSpeed() {
        super(SixToolsAddon.CATEGORY, "swing-speed", "Slows down the outward part of your arm swing animation. Purely visual, client-side only.");
    }

    @Override
    public void onDeactivate() {
        progress = 0f;
        lastProgress = 0f;
        animating = false;
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.player == null) return;

        lastProgress = progress;

        boolean vanillaSwinging = ((LivingEntitySwingAccessor) mc.player).isHandSwinging();

        if (!animating && vanillaSwinging) {
            animating = true;
            progress = 0f;
            lastProgress = 0f;
        }

        if (!animating) return;

        float step;
        if (progress < 0.5f) {
            step = (0.5f / OUT_TICKS_BASE) * speed.get().floatValue();
        } else {
            step = 0.5f / BACK_TICKS_BASE;
        }

        progress += step;

        if (progress >= 1f) {
            progress = 0f;
            lastProgress = 0f;
            animating = false;
        } else if (progress > 0.5f && lastProgress < 0.5f) {
            progress = 0.5f;
        }
    }

    public float getRenderProgress(float tickDelta) {
        if (!animating) return 0f;

        float delta = progress - lastProgress;
        if (delta < 0f) delta += 1f;

        return lastProgress + delta * tickDelta;
    }
}
