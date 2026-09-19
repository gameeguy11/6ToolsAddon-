package gamerguy11.anarchyaddon.modules.visual;

import gamerguy11.anarchyaddon.AnarchyAddon;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.meteor.KeyEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.input.Input;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.OtherClientPlayerEntity;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;

public class Parkinsons extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
        .name("speed")
        .description("Camera speed in blocks per tick. 1 = the original (20 blocks per second).")
        .defaultValue(1.0)
        .min(0.05)
        .sliderRange(0.1, 3.0)
        .build()
    );

    private float storedYaw, storedPitch;
    private boolean forward, backward, left, right, up, down;

    public Parkinsons() {
        super(AnarchyAddon.CATEGORY, "parkinsons",
            "WARNINGS: your real player stays standing still "
                + "Don't run it together with any Freecam. Turns itself off when you leave the world.");
    }

    @Override
    public void onActivate() {
        if (mc.player == null || mc.world == null) {
            toggle();
            return;
        }

        storedYaw = mc.player.getYaw();
        storedPitch = mc.player.getPitch();

        forward = Input.isPressed(mc.options.forwardKey);
        backward = Input.isPressed(mc.options.backKey);
        left = Input.isPressed(mc.options.leftKey);
        right = Input.isPressed(mc.options.rightKey);
        up = Input.isPressed(mc.options.jumpKey);
        down = Input.isPressed(mc.options.sneakKey);
        unpress();

        OtherClientPlayerEntity freecamEntity = new OtherClientPlayerEntity(mc.world, mc.player.getGameProfile());

        freecamEntity.copyPositionAndRotation(mc.player);
        freecamEntity.setYaw(mc.player.getYaw());
        freecamEntity.setPitch(mc.player.getPitch());
        freecamEntity.setNoGravity(true);
        freecamEntity.noClip = true;
        freecamEntity.setOnGround(false);

        mc.setCameraEntity(freecamEntity);
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        Entity camera = mc.getCameraEntity();
        if (camera == null || camera == mc.player) return;

        camera.setYaw(mc.player.getYaw());
        camera.setPitch(mc.player.getPitch());

        Vec3d look = Vec3d.fromPolar(0, camera.getYaw()).normalize();
        Vec3d strafe = new Vec3d(-look.z, 0, look.x).normalize();
        Vec3d velocity = Vec3d.ZERO;

        if (forward) velocity = velocity.add(look);
        if (backward) velocity = velocity.subtract(look);
        if (left) velocity = velocity.subtract(strafe);
        if (right) velocity = velocity.add(strafe);
        if (up) velocity = velocity.add(0, 1, 0);
        if (down) velocity = velocity.add(0, -1, 0);

        if (velocity.lengthSquared() > 0) {
            velocity = velocity.normalize().multiply(speed.get());
            Vec3d pos = camera.getEntityPos();

            camera.setPos(pos.x + velocity.x, pos.y + velocity.y, pos.z + velocity.z);
        }
    }

    @EventHandler
    public void onKey(KeyEvent event) {
        if (mc.currentScreen != null) return;

        int key = event.key();
        boolean pressed = event.action != KeyAction.Release;
        boolean handled = true;

        if (Input.getKey(mc.options.forwardKey) == key) { forward = pressed; mc.options.forwardKey.setPressed(false); }
        else if (Input.getKey(mc.options.backKey) == key) { backward = pressed; mc.options.backKey.setPressed(false); }
        else if (Input.getKey(mc.options.leftKey) == key) { left = pressed; mc.options.leftKey.setPressed(false); }
        else if (Input.getKey(mc.options.rightKey) == key) { right = pressed; mc.options.rightKey.setPressed(false); }
        else if (Input.getKey(mc.options.jumpKey) == key) { up = pressed; mc.options.jumpKey.setPressed(false); }
        else if (Input.getKey(mc.options.sneakKey) == key) { down = pressed; mc.options.sneakKey.setPressed(false); }
        else handled = false;

        if (handled) event.cancel();
    }

    @EventHandler
    public void onGameLeft(GameLeftEvent event) {
        if (isActive()) toggle();
    }

    private void unpress() {
        mc.options.forwardKey.setPressed(false);
        mc.options.backKey.setPressed(false);
        mc.options.leftKey.setPressed(false);
        mc.options.rightKey.setPressed(false);
        mc.options.jumpKey.setPressed(false);
        mc.options.sneakKey.setPressed(false);
    }

    @Override
    public void onDeactivate() {
        forward = backward = left = right = up = down = false;

        if (mc.player == null) return;

        mc.setCameraEntity(mc.player);

        mc.player.setYaw(storedYaw);
        mc.player.setPitch(storedPitch);
    }
}
