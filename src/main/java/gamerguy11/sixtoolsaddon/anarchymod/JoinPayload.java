package gamerguy11.sixtoolsaddon.anarchymod;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.c2s.common.CustomPayloadC2SPacket;
import net.minecraft.util.Identifier;

public record JoinPayload() implements CustomPayload {

    public static final CustomPayload.Id<JoinPayload> ID =
        new CustomPayload.Id<>(Identifier.of("anarchymod", "join"));

    public static final PacketCodec<net.minecraft.network.PacketByteBuf, JoinPayload> CODEC =
        PacketCodec.unit(new JoinPayload());

    @Override
    public CustomPayload.Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static void register() {
        PayloadTypeRegistry.playC2S().register(ID, CODEC);
    }

    public static CustomPayloadC2SPacket createPacket() {
        return new CustomPayloadC2SPacket(new JoinPayload());
    }
}
