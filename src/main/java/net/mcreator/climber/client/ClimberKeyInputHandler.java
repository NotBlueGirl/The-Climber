package net.mcreator.climber.client;

import net.mcreator.climber.ClimberMod;
import net.mcreator.climber.network.ClimbJumpPacket;

import net.minecraft.client.Minecraft;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.phys.Vec3;

import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class ClimberKeyInputHandler {

    @SubscribeEvent
public void onKeyInput(InputEvent.Key event) {
    if (!ClimberClientData.climbEnabled) return;

    Minecraft mc = Minecraft.getInstance();
    if (mc.player == null || mc.screen != null) return;

    if (!mc.options.keyJump.matches(event.getKey(), event.getScanCode())) return;
    if (event.getAction() != GLFW.GLFW_PRESS) return;

    boolean anyGrabbing = ClimberClientData.rightGrabbing || ClimberClientData.leftGrabbing;
    if (!anyGrabbing) return;

    // Snapshot BEFORE clearing — needed for local prediction
    boolean bothHands = ClimberClientData.rightGrabbing && ClimberClientData.leftGrabbing;
    Vec3 rightAnchor = ClimberClientData.rightAnchor;
    Vec3 leftAnchor  = ClimberClientData.leftAnchor;

    // Release locally immediately
    ClimberClientData.rightGrabbing  = false;
    ClimberClientData.leftGrabbing   = false;
    ClimberClientData.rightAnchor    = null;
    ClimberClientData.leftAnchor     = null;
    ClimberClientData.rightAnchorDir = null;
    ClimberClientData.leftAnchorDir  = null;

    // ---- LOCAL PREDICTION ----
    // Apply the same impulse the server will apply so we don't dip for a tick.
    if (bothHands && rightAnchor != null && leftAnchor != null) {
        Vec3 look = mc.player.getLookAngle();
        mc.player.setDeltaMovement(new Vec3(look.x, look.y + 0.55, look.z).normalize().scale(0.85));
    } else {
        Vec3 look = mc.player.getLookAngle();
        mc.player.setDeltaMovement(new Vec3(look.x, look.y + 0.3, look.z).normalize().scale(0.85));
    }
    mc.player.setNoGravity(false);
    // ---------------------------

    ClimberMod.PACKET_HANDLER.sendToServer(new ClimbJumpPacket(bothHands));
}
}