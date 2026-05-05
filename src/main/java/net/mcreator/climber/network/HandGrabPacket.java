package net.mcreator.climber.network;

import net.mcreator.climber.ClimberConfig;
import net.mcreator.climber.ClimberMod;
import net.mcreator.climber.capability.ClimbCapability;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.function.Supplier;

public class HandGrabPacket {

    public static final byte HAND_RIGHT = 0;
    public static final byte HAND_LEFT  = 1;

    private static final double MAX_REACH_SQ = 5.0 * 5.0;

    private final byte    hand;
    private final boolean grabbing;
    private final double  anchorX, anchorY, anchorZ;

    public HandGrabPacket(byte hand, boolean grabbing, double ax, double ay, double az) {
        this.hand = hand; this.grabbing = grabbing;
        this.anchorX = ax; this.anchorY = ay; this.anchorZ = az;
    }

    public HandGrabPacket(byte hand, boolean grabbing) {
        this(hand, grabbing, 0, 0, 0);
    }

    public static void encode(HandGrabPacket msg, FriendlyByteBuf buf) {
        buf.writeByte(msg.hand);
        buf.writeBoolean(msg.grabbing);
        buf.writeDouble(msg.anchorX);
        buf.writeDouble(msg.anchorY);
        buf.writeDouble(msg.anchorZ);
    }

    public static HandGrabPacket decode(FriendlyByteBuf buf) {
        return new HandGrabPacket(
                buf.readByte(), buf.readBoolean(),
                buf.readDouble(), buf.readDouble(), buf.readDouble());
    }

    public static void handle(HandGrabPacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            ClimbCapability.get(player).ifPresent(data -> {
                if (!data.isClimbEnabled()) return;

                if (msg.grabbing) {
                    boolean exhausted = (msg.hand == HAND_RIGHT)
                            ? data.isRightExhausted()
                            : data.isLeftExhausted();
                    if (exhausted) return;
                }

                Vec3 anchor = null;
                if (msg.grabbing) {
                    Vec3 candidate = new Vec3(msg.anchorX, msg.anchorY, msg.anchorZ);

                    if (candidate.distanceToSqr(player.getEyePosition()) <= MAX_REACH_SQ) {
                        Level level = player.level();
                        BlockPos bp = BlockPos.containing(candidate);

                        if (isGrabbableAt(level, bp)
                                || isGrabbableAt(level, bp.above())
                                || isGrabbableAt(level, bp.below())
                                || isGrabbableAt(level, bp.north())
                                || isGrabbableAt(level, bp.south())
                                || isGrabbableAt(level, bp.east())
                                || isGrabbableAt(level, bp.west())) {
                            anchor = candidate;
                        }
                    }
                }

                boolean valid = (anchor != null);

                if (msg.hand == HAND_RIGHT) {
                    data.setRightHandGrabbing(valid);
                    data.setRightAnchor(anchor);
                } else {
                    data.setLeftHandGrabbing(valid);
                    data.setLeftAnchor(anchor);
                }

                // >>> FIX: broadcast to every player who can see this climber <<<
                ClimberMod.PACKET_HANDLER.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new AnchorSyncPacket(player.getUUID(), data.getRightAnchor(), data.getLeftAnchor()));

                ClimberMod.PACKET_HANDLER.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                        new ClimbStatePacket(
                                player.getUUID(),
                                data.isClimbEnabled(),
                                data.isRightHandGrabbing(),
                                data.isLeftHandGrabbing()));
            });
        });
        ctx.get().setPacketHandled(true);
    }

    private static boolean isGrabbableAt(Level level, BlockPos pos) {
        BlockState state = level.getBlockState(pos);
        // Always enforce the solid-block rule, regardless of include/exclude lists
        if (state.isAir() || !state.isSolidRender(level, pos)) return false;

        ResourceLocation id = ForgeRegistries.BLOCKS.getKey(state.getBlock());
        if (id == null) return false;

        return ClimberConfig.INSTANCE.isGrabbable(id.toString());
    }
}