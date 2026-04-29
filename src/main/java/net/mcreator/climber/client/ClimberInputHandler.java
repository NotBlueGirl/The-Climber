package net.mcreator.climber.client;

import net.mcreator.climber.ClimberMod;
import net.mcreator.climber.network.HandGrabPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class ClimberInputHandler {

    @SubscribeEvent
    public void onMouseButton(InputEvent.MouseButton.Pre event) {
        if (!ClimberClientData.climbEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        int button = event.getButton();
        int action = event.getAction();

        if (button == GLFW.GLFW_MOUSE_BUTTON_RIGHT) {
            sendGrabPacket(mc, HandGrabPacket.HAND_RIGHT, action == GLFW.GLFW_PRESS);
            event.setCanceled(true);
        } else if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
            sendGrabPacket(mc, HandGrabPacket.HAND_LEFT, action == GLFW.GLFW_PRESS);
            event.setCanceled(true);
        }
    }

    private static void sendGrabPacket(Minecraft mc, byte hand, boolean grabbing) {
        if (grabbing) {
            // Don't attempt to grab with an exhausted hand
            boolean exhausted = (hand == HandGrabPacket.HAND_RIGHT)
                    ? ClimberClientData.rightExhausted
                    : ClimberClientData.leftExhausted;
            if (exhausted) return;

            Vec3 anchor = getTargetSurface(mc);
            if (anchor != null) {
                // Compute the frozen anchor→shoulder direction right now, before any
                // server round-trip.  This is what ClimberArmRenderer uses every frame
                // to position and orient the pinned arm — by freezing it here we ensure
                // the arm never rotates after the hand is planted.
                Vec3 frozenDir = computeAnchorDir(mc, anchor);

                if (hand == HandGrabPacket.HAND_RIGHT) {
                    ClimberClientData.rightGrabbing  = true;
                    ClimberClientData.rightAnchor    = anchor;
                    ClimberClientData.rightAnchorDir = frozenDir;
                } else {
                    ClimberClientData.leftGrabbing  = true;
                    ClimberClientData.leftAnchor    = anchor;
                    ClimberClientData.leftAnchorDir = frozenDir;
                }

                ClimberMod.PACKET_HANDLER.sendToServer(
                        new HandGrabPacket(hand, true, anchor.x, anchor.y, anchor.z));
            } else {
                // Clicked air — treat as a failed grab.
                releaseLocal(hand);
                ClimberMod.PACKET_HANDLER.sendToServer(new HandGrabPacket(hand, false));
            }
        } else {
            // Button released.
            releaseLocal(hand);
            ClimberMod.PACKET_HANDLER.sendToServer(new HandGrabPacket(hand, false));
        }
    }

    /** Clears all local client state for the given hand. */
    private static void releaseLocal(byte hand) {
        if (hand == HandGrabPacket.HAND_RIGHT) {
            ClimberClientData.rightGrabbing  = false;
            ClimberClientData.rightAnchor    = null;
            ClimberClientData.rightAnchorDir = null;
        } else {
            ClimberClientData.leftGrabbing  = false;
            ClimberClientData.leftAnchor    = null;
            ClimberClientData.leftAnchorDir = null;
        }
    }

    /**
     * Returns the exact world-space hit-point on the block face the player is
     * looking at, or null if they are not looking at a block.
     */
    private static Vec3 getTargetSurface(Minecraft mc) {
        HitResult hit = mc.hitResult;
        if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
            return hit.getLocation();
        }
        return null;
    }

    /**
     * Computes the normalised world-space direction from {@code anchor} toward
     * the player's shoulder at the current moment.  This is captured once at
     * grab-time so the arm renderer never needs to recompute it.
     */
    private static Vec3 computeAnchorDir(Minecraft mc, Vec3 anchor) {
        Vec3 shoulder = mc.player.position().add(0, mc.player.getEyeHeight() * 0.5, 0);
        Vec3 toShoulder = shoulder.subtract(anchor);
        double len = toShoulder.length();
        return len > 0.001 ? toShoulder.scale(1.0 / len) : new Vec3(0, 1, 0);
    }
}