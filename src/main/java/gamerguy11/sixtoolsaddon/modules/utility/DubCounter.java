package gamerguy11.sixtoolsaddon.modules.utility;

import gamerguy11.sixtoolsaddon.SixToolsAddon;
import gamerguy11.sixtoolsaddon.commands.DubCounterCommand;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.world.chunk.WorldChunk;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DubCounter extends Module {
    public SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<CountMode> countMode = sgGeneral.add(new EnumSetting.Builder<CountMode>()
        .name("count-mode")
        .description("The way the chests are counted.")
        .defaultValue(CountMode.Loaded)
        .build()
    );

    private final Setting<Integer> radius = sgGeneral.add(new IntSetting.Builder()
        .name("radius")
        .description("Chunk radius to scan around you when count-mode is Rendered.")
        .defaultValue(8)
        .min(1)
        .sliderMax(32)
        .visible(() -> countMode.get() == CountMode.Rendered)
        .build()
    );

    private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
        .name("chat-feedback")
        .description("Prints the result in chat as well as updating the HUD element.")
        .defaultValue(true)
        .build()
    );

    public int lastDubs = -1;
    public int lastNormalChests = -1;
    public CountMode lastMode = null;

    public DubCounter() {
        super(SixToolsAddon.CATEGORY, "dub-counter", "Counts how many double chests are nearby.");
    }

    @Override
    public void onActivate() {
        count();
        toggle();
    }

    private void count() {
        int length = countMode.get() == CountMode.Rendered ? countRendered() : countLoaded();
        int dubs = length / 2;

        lastDubs = dubs;
        lastNormalChests = length;
        lastMode = countMode.get();

        DubCounterCommand.lastDubs = dubs;
        DubCounterCommand.lastNormalChests = length;
        DubCounterCommand.lastMode = DubCounterCommand.CountMode.valueOf(countMode.get().name());

        if (chatFeedback.get()) {
            info(
                "There are roughly (highlight)%s(default) (%s normal chests) %s double chests.",
                dubs,
                length,
                countMode.get() == CountMode.Rendered ? "rendered" : "loaded"
            );
        }
    }

    private int countLoaded() {
        if (mc.world == null) return 0;
        return scanChunksAround(Integer.MAX_VALUE);
    }

    private int countRendered() {
        if (mc.world == null) return 0;
        return scanChunksAround(radius.get());
    }

    private int scanChunksAround(int chunkRadius) {
        ClientWorld world = mc.world;
        if (world == null || mc.player == null) return 0;

        int playerChunkX = mc.player.getChunkPos().x;
        int playerChunkZ = mc.player.getChunkPos().z;

        int count = 0;

        int scanRadius = chunkRadius == Integer.MAX_VALUE ? 32 : chunkRadius;

        for (int dx = -scanRadius; dx <= scanRadius; dx++) {
            for (int dz = -scanRadius; dz <= scanRadius; dz++) {
                WorldChunk chunk = world.getChunk(playerChunkX + dx, playerChunkZ + dz);
                if (chunk == null) continue;

                for (BlockEntity blockEntity : chunk.getBlockEntities().values()) {
                    if (blockEntity instanceof ChestBlockEntity) count++;
                }
            }
        }

        return count;
    }

    public enum CountMode {
        Rendered,
        Loaded
    }
}
