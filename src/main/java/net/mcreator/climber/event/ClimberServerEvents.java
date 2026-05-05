package net.mcreator.climber.event;

import net.mcreator.climber.ClimberConfig;
import net.mcreator.climber.ClimberMod;
import net.mcreator.climber.capability.ClimbCapability;
import net.mcreator.climber.network.AnchorSyncPacket;
import net.mcreator.climber.network.ClimbStatePacket;
import net.mcreator.climber.network.StaminaSyncPacket;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.network.PacketDistributor;

public class ClimberServerEvents {

    @SubscribeEvent
    public void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Player player = event.player;

        if (player.level().isClientSide()) return;
        if (!player.isAlive() || player.isPassenger() || player.isSpectator() || player.getAbilities().flying) return;

        ClimbCapability.get(player).ifPresent(data -> {
            if (!data.isClimbEnabled() || (!data.isRightHandGrabbing() && !data.isLeftHandGrabbing())) {
                player.setNoGravity(false);

                if (ClimberConfig.INSTANCE.isStaminaEnabled()) {
                    tickStaminaRecovery(player, data);
                }
                return;
            }

            applyClimbPhysics(player, data.isRightHandGrabbing(), data.isLeftHandGrabbing(),
                    data.getRightAnchor(), data.getLeftAnchor());

            if (ClimberConfig.INSTANCE.isStaminaEnabled()) {
                tickStaminaDrain(player, data);
            }

            // >>> FIX: periodic full sync so newly-tracking players catch up <<<
            if (player instanceof ServerPlayer sp && player.tickCount % 20 == 0) {
                syncStamina(sp, data);
                ClimberMod.PACKET_HANDLER.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                        new AnchorSyncPacket(sp.getUUID(), data.getRightAnchor(), data.getLeftAnchor()));
                ClimberMod.PACKET_HANDLER.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                        new ClimbStatePacket(sp.getUUID(), data.isClimbEnabled(),
                                data.isRightHandGrabbing(), data.isLeftHandGrabbing()));
            }
        });
    }

    // -----------------------------------------------------------------------
    // Stamina logic
    // -----------------------------------------------------------------------

    private static void tickStaminaDrain(Player player, ClimbCapability.IClimbData data) {
        boolean changed = false;

        if (data.isRightHandGrabbing()) {
            float s = data.getRightStamina() - ClimberConfig.INSTANCE.getStaminaDrainRate();
            data.setRightStamina(s);
            if (s <= 0 && !data.isRightExhausted()) {
                data.setRightExhausted(true);
                forceReleaseHand(player, data, true);
            }
            changed = true;
        } else if (!data.isRightExhausted()) {
            float s = data.getRightStamina() + ClimberConfig.INSTANCE.getStaminaRegenRate();
            data.setRightStamina(s);
            if (s >= ClimberConfig.INSTANCE.getStaminaMax()) data.setRightStamina(ClimberConfig.INSTANCE.getStaminaMax());
            changed = true;
        }

        if (data.isLeftHandGrabbing()) {
            float s = data.getLeftStamina() - ClimberConfig.INSTANCE.getStaminaDrainRate();
            data.setLeftStamina(s);
            if (s <= 0 && !data.isLeftExhausted()) {
                data.setLeftExhausted(true);
                forceReleaseHand(player, data, false);
            }
            changed = true;
        } else if (!data.isLeftExhausted()) {
            float s = data.getLeftStamina() + ClimberConfig.INSTANCE.getStaminaRegenRate();
            data.setLeftStamina(s);
            if (s >= ClimberConfig.INSTANCE.getStaminaMax()) data.setLeftStamina(ClimberConfig.INSTANCE.getStaminaMax());
            changed = true;
        }

        if (changed && player instanceof ServerPlayer sp) {
            syncStamina(sp, data);
        }
    }

    private static void tickStaminaRecovery(Player player, ClimbCapability.IClimbData data) {
        boolean changed = false;

        if (!data.isRightHandGrabbing()) {
            float newR = Math.min(ClimberConfig.INSTANCE.getStaminaMax(), data.getRightStamina() + ClimberConfig.INSTANCE.getStaminaRegenRate());
            data.setRightStamina(newR);
            if (data.isRightExhausted() && newR >= ClimberConfig.INSTANCE.getStaminaMax()) {
                data.setRightExhausted(false);
            }
            changed = true;
        }

        if (!data.isLeftHandGrabbing()) {
            float newL = Math.min(ClimberConfig.INSTANCE.getStaminaMax(), data.getLeftStamina() + ClimberConfig.INSTANCE.getStaminaRegenRate());
            data.setLeftStamina(newL);
            if (data.isLeftExhausted() && newL >= ClimberConfig.INSTANCE.getStaminaMax()) {
                data.setLeftExhausted(false);
            }
            changed = true;
        }

        if (changed && player instanceof ServerPlayer sp) {
            syncStamina(sp, data);
        }
    }

    private static void forceReleaseHand(Player player, ClimbCapability.IClimbData data, boolean right) {
        if (right) {
            data.setRightHandGrabbing(false);
            data.setRightAnchor(null);
        } else {
            data.setLeftHandGrabbing(false);
            data.setLeftAnchor(null);
        }

        if (player instanceof ServerPlayer sp) {
            ClimberMod.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                    new AnchorSyncPacket(sp.getUUID(), data.getRightAnchor(), data.getLeftAnchor()));
            ClimberMod.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                    new ClimbStatePacket(sp.getUUID(), data.isClimbEnabled(),
                            data.isRightHandGrabbing(), data.isLeftHandGrabbing()));
        }
    }

    private static void syncStamina(ServerPlayer player, ClimbCapability.IClimbData data) {
        ClimberMod.PACKET_HANDLER.send(PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new StaminaSyncPacket(
                        player.getUUID(),
                        data.getRightStamina(), data.getLeftStamina(),
                        data.isRightExhausted(), data.isLeftExhausted()));
    }

    // -----------------------------------------------------------------------
    // Physics (unchanged)
    // -----------------------------------------------------------------------

    public static void applyClimbPhysics(Player player, boolean rightGrab, boolean leftGrab,
                                          Vec3 rightAnchor, Vec3 leftAnchor) {
        player.setNoGravity(true);
        player.fallDistance = 0;

        Vec3 pos = player.position().add(0, player.getEyeHeight() * 0.5, 0);
        Vec3 vel = player.getDeltaMovement();

        boolean bothGrabbing = rightGrab && leftGrab && rightAnchor != null && leftAnchor != null;
        Vec3 target = null;

        if (bothGrabbing) {
            target = rightAnchor.add(leftAnchor).scale(0.5);
        } else if (rightGrab && rightAnchor != null) {
            target = rightAnchor;
        } else if (leftGrab && leftAnchor != null) {
            target = leftAnchor;
        }

        if (target != null) {
            Vec3 delta = target.subtract(pos);
            double dist = delta.length();
            Vec3 dir = dist > 0.001 ? delta.normalize() : Vec3.ZERO;

            vel = vel.add(0, -0.06, 0);
            vel = vel.scale(0.94);

            double restDist = bothGrabbing ? 0.6 : 1.5;
            if (dist > restDist) {
                double stretch = dist - restDist;
                vel = vel.add(dir.scale(stretch * 0.08));
            }

            double maxDist = 4.0;
            if (dist > maxDist) {
                double excess = dist - maxDist;
                vel = vel.add(dir.scale(excess * 0.06));

                double radialVel = vel.dot(dir);
                if (radialVel < 0) {
                    vel = vel.subtract(dir.scale(radialVel * 0.98));
                }
            }

            vel = vel.scale(0.96);
        }

        player.setDeltaMovement(vel);
        if (!player.onGround()) {
            player.resetFallDistance();
        }
    }

    // -----------------------------------------------------------------------
    // Jump handling (unchanged logic, packet fix handled in ClimbJumpPacket)
    // -----------------------------------------------------------------------

    public static void handleClimbJump(Player player, boolean bothHands) {
        ClimbCapability.get(player).ifPresent(data -> {
            if (!data.isClimbEnabled()) return;
            if (!data.isRightHandGrabbing() && !data.isLeftHandGrabbing()) return;

            Vec3 impulse;

            if (bothHands
                    && data.isRightHandGrabbing()
                    && data.isLeftHandGrabbing()
                    && data.getRightAnchor() != null
                    && data.getLeftAnchor() != null) {

                Vec3 look = player.getLookAngle();
                float strength = ClimberConfig.INSTANCE.getStrengthJumpBothHands();
                impulse = new Vec3(look.x, look.y + 0.55, look.z).normalize().scale(strength);

            } else {
                Vec3 look = player.getLookAngle();
                float strength = ClimberConfig.INSTANCE.getStrengthJumpOneHand();
                impulse = new Vec3(look.x, look.y + 0.3, look.z).normalize().scale(strength);
            }

            player.setDeltaMovement(impulse);
            player.setNoGravity(false);

            data.setRightHandGrabbing(false);
            data.setLeftHandGrabbing(false);
            data.setRightAnchor(null);
            data.setLeftAnchor(null);
            player.fallDistance = 0;
        });
    }

    public static void handleClimbJump(Player player) {
        handleClimbJump(player, false);
    }
}