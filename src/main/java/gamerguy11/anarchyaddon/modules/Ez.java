package gamerguy11.anarchyaddon.modules;

import gamerguy11.anarchyaddon.AnarchyAddon;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

public class Ez extends Module {
    public Ez() {
        super(AnarchyAddon.CATEGORY, "ez", "Sends a message you write yourself when a nearby player dies.");
    }

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgKill = settings.createGroup("Kill");
    private final SettingGroup sgPop = settings.createGroup("Pop");

    private final Setting<Double> range = sgGeneral.add(new DoubleSetting.Builder()
        .name("range")
        .description("Only trigger for players within this distance of you.")
        .defaultValue(25)
        .min(0)
        .sliderRange(0, 50)
        .build()
    );

    private final Setting<Integer> delay = sgGeneral.add(new IntSetting.Builder()
        .name("delay")
        .description("Ticks to wait between sending queued messages.")
        .defaultValue(50)
        .min(0)
        .sliderRange(0, 100)
        .build()
    );

    private final Setting<Boolean> killEnabled = sgKill.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Sends a message when a nearby non-friend player dies, regardless of who killed them.")
        .defaultValue(true)
        .build()
    );

    private final Setting<List<String>> killMessages = sgKill.add(new StringListSetting.Builder()
        .name("kill-messages")
        .description("Write your own messages here - one is picked at random each time. Use <NAME> to insert the player's name. Empty by default: nothing sends until you add something.")
        .build()
    );

    private final Setting<Boolean> popEnabled = sgPop.add(new BoolSetting.Builder()
        .name("enabled")
        .description("Sends a message when a nearby non-friend player pops a totem.")
        .defaultValue(false)
        .build()
    );

    private final Setting<List<String>> popMessages = sgPop.add(new StringListSetting.Builder()
        .name("pop-messages")
        .description("Write your own messages here - one is picked at random each time. Use <NAME> to insert the player's name. Empty by default: nothing sends until you add something.")
        .build()
    );

    private final Random random = new Random();
    private final List<String> queue = new LinkedList<>();
    private final Set<Integer> deadNotified = new HashSet<>();
    private int lastKillIndex = -1;
    private int lastPopIndex = -1;
    private int timer;

    @Override
    public void onActivate() {
        timer = 0;
        queue.clear();
        deadNotified.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        if (mc.player == null || mc.world == null) return;

        timer++;
        if (timer >= delay.get() && !queue.isEmpty()) {
            ChatUtils.sendPlayerMsg(queue.remove(0));
            timer = 0;
        }

        if (!killEnabled.get()) return;

        for (PlayerEntity player : mc.world.getPlayers()) {
            if (player == null || player == mc.player) continue;
            if (Friends.get().isFriend(player)) continue;
            if (mc.player.getEntityPos().distanceTo(player.getEntityPos()) > range.get()) continue;

            int id = player.getId();
            if (player.getHealth() <= 0) {

                if (deadNotified.add(id)) {
                    queueMessage(killMessages.get(), lastKillIndex, i -> lastKillIndex = i, player.getName().getString());
                }
            } else {
                deadNotified.remove(id);
            }
        }
    }

    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) return;

        if (popEnabled.get() && event.packet instanceof EntityStatusS2CPacket packet && packet.getStatus() == 35) {
            Entity entity = packet.getEntity(mc.world);

            if (entity instanceof PlayerEntity player && entity != mc.player
                && !Friends.get().isFriend(player)
                && mc.player.getEntityPos().distanceTo(player.getEntityPos()) <= range.get()) {

                queueMessage(popMessages.get(), lastPopIndex, i -> lastPopIndex = i, player.getName().getString());
            }
        }
    }

    private void queueMessage(List<String> messages, int lastIndex, java.util.function.IntConsumer setLastIndex, String victimName) {
        if (messages.isEmpty()) return;

        int index = random.nextInt(messages.size());
        if (messages.size() > 1 && index == lastIndex) index = (index + 1) % messages.size();
        setLastIndex.accept(index);

        queue.add(messages.get(index).replace("<NAME>", victimName));
    }
}
