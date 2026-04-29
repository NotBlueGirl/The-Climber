package net.mcreator.climber.network;

import net.mcreator.climber.client.ClimberClientData;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class StaminaSyncPacket {

    private final UUID playerId;
    private final float   rightStamina;
    private final float   leftStamina;
    private final boolean rightExhausted;
    private final boolean leftExhausted;

    public StaminaSyncPacket(UUID playerId, float rightStamina, float leftStamina,
                              boolean rightExhausted, boolean leftExhausted) {
        this.playerId       = playerId;
        this.rightStamina   = rightStamina;
        this.leftStamina    = leftStamina;
        this.rightExhausted = rightExhausted;
        this.leftExhausted  = leftExhausted;
    }

    public static void encode(StaminaSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeFloat(msg.rightStamina);
        buf.writeFloat(msg.leftStamina);
        buf.writeBoolean(msg.rightExhausted);
        buf.writeBoolean(msg.leftExhausted);
    }

    public static StaminaSyncPacket decode(FriendlyByteBuf buf) {
        return new StaminaSyncPacket(
                buf.readUUID(), buf.readFloat(), buf.readFloat(),
                buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(StaminaSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClimberClientData.applyStamina(msg.playerId,
                        msg.rightStamina, msg.leftStamina,
                        msg.rightExhausted, msg.leftExhausted);
            })
        );
        ctx.get().setPacketHandled(true);
    }
}