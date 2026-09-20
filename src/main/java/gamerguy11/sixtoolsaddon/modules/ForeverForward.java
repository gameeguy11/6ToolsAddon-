package gamerguy11.sixtoolsaddon.modules;

import gamerguy11.sixtoolsaddon.SixToolsAddon;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class ForeverForward extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> sprint = sgGeneral.add(new BoolSetting.Builder()
        .name("sprint")
        .description("Also holds the sprint key down while active.")
        .defaultValue(true)
        .build()
    );

    public ForeverForward() {
        super(SixToolsAddon.CATEGORY, "forever-forward", "Holds the forward-movement key down for you.");
    }

    @Override
    public void onDeactivate() {
        if (!isKeyPhysicallyPressed(mc.options.forwardKey)) mc.options.forwardKey.setPressed(false);
        if (!isKeyPhysicallyPressed(mc.options.sprintKey)) mc.options.sprintKey.setPressed(false);
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        mc.options.forwardKey.setPressed(true);
        mc.options.sprintKey.setPressed(sprint.get() || isKeyPhysicallyPressed(mc.options.sprintKey));
    }

    private boolean isKeyPhysicallyPressed(KeyBinding binding) {
        if (mc.getWindow() == null) return binding.isPressed();

        InputUtil.Key key = InputUtil.fromTranslationKey(binding.getBoundKeyTranslationKey());
        if (key.getCategory() != InputUtil.Type.KEYSYM) return binding.isPressed();

        return InputUtil.isKeyPressed(mc.getWindow(), key.getCode());
    }
}
