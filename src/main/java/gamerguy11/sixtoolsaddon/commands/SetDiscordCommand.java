package gamerguy11.sixtoolsaddon.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import gamerguy11.sixtoolsaddon.modules.utility.DiscordNotifier;
import meteordevelopment.meteorclient.commands.Command;
import meteordevelopment.meteorclient.systems.modules.Modules;
import net.minecraft.command.CommandSource;

public class SetDiscordCommand extends Command {
    public SetDiscordCommand() {
        super("setdiscord", "Manage the Discord webhook used by DiscordNotifier.");
    }

    private DiscordNotifier module() {
        return Modules.get().get(DiscordNotifier.class);
    }

    @Override
    public void build(LiteralArgumentBuilder<CommandSource> builder) {
        builder.then(literal("set").then(argument("url", StringArgumentType.greedyString()).executes(ctx -> {
            String url = StringArgumentType.getString(ctx, "url").trim();
            DiscordNotifier module = module();

            if (!module.isValidWebhookUrl(url)) {
                module.notifyError("That doesn't look like a Discord webhook URL. It should start with (highlight)https://discord.com/api/webhooks/(default).");
                return SINGLE_SUCCESS;
            }

            module.setWebhookUrl(url);
            module.notifyInfo("Discord webhook set. Use (highlight).discordnotifier(default) to toggle what gets sent.");
            return SINGLE_SUCCESS;
        })));

        builder.then(literal("clear").executes(ctx -> {
            DiscordNotifier module = module();

            if (!module.hasWebhook()) {
                module.notifyInfo("No Discord webhook is set.");
                return SINGLE_SUCCESS;
            }

            module.clearWebhookUrl();
            module.notifyInfo("Discord webhook cleared.");
            return SINGLE_SUCCESS;
        }));
    }
}
