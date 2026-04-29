package net.mcreator.climber.network;

import net.mcreator.climber.ClimberMod;
import net.mcreator.climber.capability.ClimbCapability;
import net.mcreator.climber.event.ClimberServerEvents;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;

import java.util.function.Supplier;

public class ClimbJumpPacket {

    private final boolean bothHands;

    public ClimbJumpPacket(boolean bothHands) {
        this.bothHands = bothHands;
    }

    public static void encode(ClimbJumpPacket msg, FriendlyByteBuf buf) {
        buf.writeBoolean(msg.bothHands);
    }

    public static ClimbJumpPacket decode(FriendlyByteBuf buf) {
        return new ClimbJumpPacket(buf.readBoolean());
    }

    public static void handle(ClimbJumpPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ClimberServerEvents.handleClimbJump(player, msg.bothHands);

            ClimbCapability.get(player).ifPresent(data -> {
                // Tell every nearby client that anchors are cleared and hands are free
                ClimberMod.PACKET_HANDLER.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new AnchorSyncPacket(player.getUUID(), null, null));

                ClimberMod.PACKET_HANDLER.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new ClimbStatePacket(player.getUUID(),
                                data.isClimbEnabled(), false, false));
            });
        });
        ctx.get().setPacketHandled(true);
    }
}