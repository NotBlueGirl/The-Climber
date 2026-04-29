package net.mcreator.climber.network;

import net.mcreator.climber.client.ClimberClientData;

import net.minecraft.network.FriendlyByteBuf;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class ClimbStatePacket {

    private final UUID playerId;
    private final boolean climbEnabled;
    private final boolean rightGrabbing;
    private final boolean leftGrabbing;

    public ClimbStatePacket(UUID playerId, boolean climbEnabled, boolean rightGrabbing, boolean leftGrabbing) {
        this.playerId      = playerId;
        this.climbEnabled  = climbEnabled;
        this.rightGrabbing = rightGrabbing;
        this.leftGrabbing  = leftGrabbing;
    }

    public static void encode(ClimbStatePacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        buf.writeBoolean(msg.climbEnabled);
        buf.writeBoolean(msg.rightGrabbing);
        buf.writeBoolean(msg.leftGrabbing);
    }

    public static ClimbStatePacket decode(FriendlyByteBuf buf) {
        return new ClimbStatePacket(buf.readUUID(), buf.readBoolean(), buf.readBoolean(), buf.readBoolean());
    }

    public static void handle(ClimbStatePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClimberClientData.applyStateFromServer(msg.playerId, msg.climbEnabled, msg.rightGrabbing, msg.leftGrabbing);
                if (!msg.climbEnabled) ClimberClientData.applyAnchors(msg.playerId, null, null);
            })
        );
        ctx.get().setPacketHandled(true);
    }
}