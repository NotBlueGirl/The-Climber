package net.mcreator.climber.client;

import net.mcreator.climber.ClimberConfig;
import net.mcreator.climber.ClimberMod;
import net.mcreator.climber.init.ClimberModSounds;
import net.mcreator.climber.network.ClimbJumpPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.registries.ForgeRegistries;

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

        boolean bothHands = ClimberClientData.rightGrabbing && ClimberClientData.leftGrabbing;
        Vec3 rightAnchor = ClimberClientData.rightAnchor;
        Vec3 leftAnchor  = ClimberClientData.leftAnchor;

        ClimberClientData.rightGrabbing  = false;
        ClimberClientData.leftGrabbing   = false;
        ClimberClientData.rightAnchor    = null;
        ClimberClientData.leftAnchor     = null;
        ClimberClientData.rightAnchorDir = null;
        ClimberClientData.leftAnchorDir  = null;

        if (bothHands && rightAnchor != null && leftAnchor != null) {
            Vec3 look = mc.player.getLookAngle();
            float strength = ClimberConfig.INSTANCE.getStrengthJumpBothHands();
            mc.player.setDeltaMovement(new Vec3(look.x, look.y + 0.55, look.z).normalize().scale(strength));
        } else {
            Vec3 look = mc.player.getLookAngle();
            float strength = ClimberConfig.INSTANCE.getStrengthJumpOneHand();
            mc.player.setDeltaMovement(new Vec3(look.x, look.y + 0.3, look.z).normalize().scale(strength));
        }
        mc.player.setNoGravity(false);

        // ---- SAFE CUSTOM SOUND (file or registry) ----
        boolean played = false;
        String customJump = ClimberConfig.INSTANCE.getGrabJumpSound();
        if (customJump != null && !customJump.isEmpty()) {
            if (ClimberSoundPreloader.isFilePath(customJump)) {
                ClimberSoundPreloader.playAt(customJump, 0.7f, 1.0f, mc.player.position());
                played = true;
            } else {
                try {
                    ResourceLocation rl = new ResourceLocation(customJump);
                    SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(rl);
                    if (ev != null) {
                        mc.player.playSound(ev, 0.7f, 1.0f);
                        played = true;
                    }
                } catch (Exception e) {
                    ClimberMod.LOGGER.warn("[Climber] Invalid grab-jump registry ID: '{}'", customJump);
                }
            }
        }
        if (!played) {
            mc.player.playSound(ClimberModSounds.GRAB_JUMP.get(), 0.7f, 1.0f);
        }
        // -----------------------------------------------

        ClimberMod.PACKET_HANDLER.sendToServer(new ClimbJumpPacket(bothHands));
    }
}