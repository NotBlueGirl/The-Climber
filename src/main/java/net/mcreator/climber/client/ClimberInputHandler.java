package net.mcreator.climber.client;

import net.mcreator.climber.ClimberConfig;
import net.mcreator.climber.ClimberMod;
import net.mcreator.climber.init.ClimberModSounds;
import net.mcreator.climber.network.HandGrabPacket;

import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.registries.ForgeRegistries;
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
            boolean exhausted = (hand == HandGrabPacket.HAND_RIGHT)
                    ? ClimberClientData.rightExhausted
                    : ClimberClientData.leftExhausted;
            if (exhausted) return;

            HitResult hit = mc.hitResult;
            if (hit != null && hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                Vec3 anchor = blockHit.getLocation();
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

                // FIX: pass the real block pos from the hit result
                playGrabHitSound(mc, blockHit.getBlockPos());
            } else {
                releaseLocal(hand);
                ClimberMod.PACKET_HANDLER.sendToServer(new HandGrabPacket(hand, false));
            }
        } else {
            releaseLocal(hand);
            ClimberMod.PACKET_HANDLER.sendToServer(new HandGrabPacket(hand, false));
        }
    }

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

    // FIX: accept BlockPos so we don't re-derive it from the face-boundary Vec3
    private static void playGrabHitSound(Minecraft mc, BlockPos soundPos) {
        String customPath = ClimberConfig.INSTANCE.getGrabHitSound();

        if (customPath != null && !customPath.isEmpty()) {
            if (ClimberSoundPreloader.isFilePath(customPath)) {
                ClimberSoundPreloader.playAt(customPath, 0.6f, 0.8f,
                        Vec3.atCenterOf(soundPos));
                return;
            }
            try {
                ResourceLocation rl = new ResourceLocation(customPath);
                SoundEvent ev = ForgeRegistries.SOUND_EVENTS.getValue(rl);
                if (ev != null) {
                    mc.player.playSound(ev, 0.6f, 1.0f);
                    return;
                }
            } catch (Exception e) {
                ClimberMod.LOGGER.warn("[Climber] Invalid grab-hit registry ID: '{}'", customPath);
            }
        }

        // Default: break sound of the pinned block
        BlockState state = mc.level.getBlockState(soundPos);
        if (!state.isAir()) {
            SoundType st = state.getSoundType();
            mc.player.playSound(st.getBreakSound(), st.getVolume() * 0.6f, st.getPitch() * 0.8f);
            return;
        }

        // Ultimate fallback
        mc.player.playSound(ClimberModSounds.GRAB_HIT.get(), 0.6f, 1.0f);
    }

    private static Vec3 computeAnchorDir(Minecraft mc, Vec3 anchor) {
        Vec3 shoulder = mc.player.position().add(0, mc.player.getEyeHeight() * 0.5, 0);
        Vec3 toShoulder = shoulder.subtract(anchor);
        double len = toShoulder.length();
        return len > 0.001 ? toShoulder.scale(1.0 / len) : new Vec3(0, 1, 0);
    }
}