package gamerguy11.anarchyaddon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gamerguy11.anarchyaddon.systems.enemies.Enemy;
import gamerguy11.anarchyaddon.systems.enemies.Enemies;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import net.minecraft.command.CommandSource;

public class EnemyCommand extends Command {
    public EnemyCommand() {
        super("enemy", "Manages your enemies list.");
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("add").then(argument("name", StringArgumentType.word()).executes(ctx -> {
            String name = StringArgumentType.getString(ctx, "name");

            if (Enemies.get().add(new Enemy(name))) {
                ChatUtils.info("Added (highlight)%s(default) to enemies.", name);
            } else {
                ChatUtils.info("(highlight)%s(default) is already on your enemies list.", name);
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("remove").then(argument("name", StringArgumentType.word()).executes(ctx -> {
            String name = StringArgumentType.getString(ctx, "name");
            Enemy enemy = Enemies.get().get(name);

            if (enemy != null && Enemies.get().remove(enemy)) {
                ChatUtils.info("Removed (highlight)%s(default) from enemies.", name);
            } else {
                ChatUtils.info("(highlight)%s(default) is not on your enemies list.", name);
            }

            return SINGLE_SUCCESS;
        })));

        builder.then(literal("list").executes(ctx -> {
            if (Enemies.get().isEmpty()) {
                ChatUtils.info("Your enemies list is empty.");
                return SINGLE_SUCCESS;
            }

            StringBuilder names = new StringBuilder();
            for (Enemy enemy : Enemies.get()) {
                if (!names.isEmpty()) names.append(", ");
                names.append(enemy.getName());
            }

            ChatUtils.info("Enemies ((highlight)%s(default)): %s", Enemies.get().count(), names);
            return SINGLE_SUCCESS;
        }));
    }
}
