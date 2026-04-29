package net.mcreator.climber.network;

import net.mcreator.climber.client.ClimberClientData;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.network.NetworkEvent;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.function.Supplier;

public class AnchorSyncPacket {

    private final UUID playerId;
    @Nullable private final Vec3 rightAnchor;
    @Nullable private final Vec3 leftAnchor;

    public AnchorSyncPacket(UUID playerId, @Nullable Vec3 right, @Nullable Vec3 left) {
        this.playerId    = playerId;
        this.rightAnchor = right;
        this.leftAnchor  = left;
    }

    public static void encode(AnchorSyncPacket msg, FriendlyByteBuf buf) {
        buf.writeUUID(msg.playerId);
        writeVec(buf, msg.rightAnchor);
        writeVec(buf, msg.leftAnchor);
    }

    public static AnchorSyncPacket decode(FriendlyByteBuf buf) {
        return new AnchorSyncPacket(buf.readUUID(), readVec(buf), readVec(buf));
    }

    public static void handle(AnchorSyncPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() ->
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () ->
                ClimberClientData.applyAnchors(msg.playerId, msg.rightAnchor, msg.leftAnchor)
            )
        );
        ctx.get().setPacketHandled(true);
    }

    private static void writeVec(FriendlyByteBuf buf, @Nullable Vec3 v) {
        buf.writeBoolean(v != null);
        if (v != null) { buf.writeDouble(v.x); buf.writeDouble(v.y); buf.writeDouble(v.z); }
    }

    @Nullable
    private static Vec3 readVec(FriendlyByteBuf buf) {
        if (!buf.readBoolean()) return null;
        return new Vec3(buf.readDouble(), buf.readDouble(), buf.readDouble());
    }
}