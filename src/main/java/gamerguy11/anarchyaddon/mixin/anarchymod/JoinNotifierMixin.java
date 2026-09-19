package gamerguy11.anarchyaddon.mixin.anarchymod;

import gamerguy11.anarchyaddon.anarchymod.Domains;
import gamerguy11.anarchyaddon.anarchymod.JoinPayload;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.packet.s2c.play.GameJoinS2CPacket;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.logging.Logger;

@Mixin(ClientPlayNetworkHandler.class)
public class JoinNotifierMixin {

    private static final Logger LOGGER = Logger.getLogger("AnarchyAddon-JoinNotifier");

    @Inject(method = "onGameJoin", at = @At("RETURN"))
    private void afterLogin(GameJoinS2CPacket packet, CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        ServerInfo server = client.getCurrentServerEntry();
        if (server == null || !Domains.contains(server.address)) {
            return;
        }

        ClientPlayNetworkHandler handler = (ClientPlayNetworkHandler) (Object) this;
        String address = server.address;

        client.execute(() -> trySend(handler, address));
    }

    private void trySend(ClientPlayNetworkHandler handler, String address) {
        try {
            ClientConnection connection = handler.getConnection();

            if (connection == null || !connection.isOpen()) {
                return;
            }

            connection.send(JoinPayload.createPacket());
        } catch (RuntimeException error) {
            LOGGER.warning("Failed to send join notification to " + address + ": " + error.getMessage());
        }
    }
}
