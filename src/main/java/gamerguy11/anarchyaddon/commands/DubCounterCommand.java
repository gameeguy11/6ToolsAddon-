package gamerguy11.anarchyaddon.commands;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.command.CommandSource;
import net.minecraft.world.chunk.WorldChunk;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class DubCounterCommand extends Command {
    public static int lastDubs = -1;
    public static int lastNormalChests = -1;
    public static CountMode lastMode = null;

    public DubCounterCommand() {
        super("dub", "Counts how many double chests are nearby.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.executes(ctx -> {
            count(CountMode.Loaded, 8);
            return SINGLE_SUCCESS;
        });

        builder.then(literal("rendered").executes(ctx -> {
            count(CountMode.Rendered, 8);
            return SINGLE_SUCCESS;
        }).then(argument("radius", IntegerArgumentType.integer(1, 32)).executes(ctx -> {
            int radius = IntegerArgumentType.getInteger(ctx, "radius");
            count(CountMode.Rendered, radius);
            return SINGLE_SUCCESS;
        })));
    }

    private void count(CountMode mode, int radius) {
        int length = mode == CountMode.Rendered ? scanChunksAround(radius) : scanChunksAround(Integer.MAX_VALUE);
        int dubs = length / 2;

        lastDubs = dubs;
        lastNormalChests = length;
        lastMode = mode;

        ChatUtils.info(
            "There are roughly (highlight)%s(default) (%s normal chests) %s double chests.",
            dubs,
            length,
            mode == CountMode.Rendered ? "rendered" : "loaded"
        );
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
