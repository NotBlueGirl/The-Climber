package net.mcreator.climber.client;

import net.mcreator.climber.ClimberConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@OnlyIn(Dist.CLIENT)
public class ClimberClientData {

    // -----------------------------------------------------------------
    // Local player
    // -----------------------------------------------------------------
    public static boolean climbEnabled  = false;
    public static boolean rightGrabbing = false;
    public static boolean leftGrabbing  = false;

    @Nullable public static Vec3 rightAnchor = null;
    @Nullable public static Vec3 leftAnchor  = null;

    @Nullable public static Vec3 rightAnchorDir = null;
    @Nullable public static Vec3 leftAnchorDir  = null;

    public static float   rightStamina   = ClimberConfig.INSTANCE.getStaminaMax();
    public static float   leftStamina    = ClimberConfig.INSTANCE.getStaminaMax();
    public static boolean rightExhausted = false;
    public static boolean leftExhausted  = false;

    // -----------------------------------------------------------------
    // Remote players
    // -----------------------------------------------------------------
    public static final Map<UUID, RemoteClimbState> REMOTE_STATES = new HashMap<>();

    public static class RemoteClimbState {
        public boolean climbEnabled  = false;
        public boolean rightGrabbing = false;
        public boolean leftGrabbing  = false;
        @Nullable public Vec3 rightAnchor = null;
        @Nullable public Vec3 leftAnchor  = null;
        @Nullable public Vec3 rightAnchorDir = null;
        @Nullable public Vec3 leftAnchorDir  = null;
        public float   rightStamina   = ClimberConfig.INSTANCE.getStaminaMax();
        public float   leftStamina    = ClimberConfig.INSTANCE.getStaminaMax();
        public boolean rightExhausted = false;
        public boolean leftExhausted  = false;
    }

    private static final RemoteClimbState LOCAL_PROXY = new RemoteClimbState();

    @Nullable
    public static RemoteClimbState getState(Player player) {
        if (player == Minecraft.getInstance().player) {
            LOCAL_PROXY.climbEnabled   = climbEnabled;
            LOCAL_PROXY.rightGrabbing  = rightGrabbing;
            LOCAL_PROXY.leftGrabbing   = leftGrabbing;
            LOCAL_PROXY.rightAnchor    = rightAnchor;
            LOCAL_PROXY.leftAnchor     = leftAnchor;
            LOCAL_PROXY.rightAnchorDir = rightAnchorDir;
            LOCAL_PROXY.leftAnchorDir  = leftAnchorDir;
            LOCAL_PROXY.rightStamina   = rightStamina;
            LOCAL_PROXY.leftStamina    = leftStamina;
            LOCAL_PROXY.rightExhausted = rightExhausted;
            LOCAL_PROXY.leftExhausted  = leftExhausted;
            return LOCAL_PROXY;
        }
        return REMOTE_STATES.get(player.getUUID());
    }

    // -----------------------------------------------------------------
    // Packet application
    // -----------------------------------------------------------------
    public static void applyStateFromServer(UUID playerId, boolean enabled, boolean right, boolean left) {
        if (isLocal(playerId)) {
            climbEnabled  = enabled;
            rightGrabbing = right;
            leftGrabbing  = left;
        } else {
            RemoteClimbState s = REMOTE_STATES.computeIfAbsent(playerId, k -> new RemoteClimbState());
            s.climbEnabled = enabled;
            s.rightGrabbing = right;
            s.leftGrabbing = left;
            if (!enabled) {
                s.rightAnchor = null;
                s.leftAnchor = null;
                s.rightAnchorDir = null;
                s.leftAnchorDir = null;
            }
        }
    }

    public static void applyAnchors(UUID playerId, @Nullable Vec3 right, @Nullable Vec3 left) {
        if (isLocal(playerId)) {
            if (right == null) rightAnchorDir = null;
            if (left  == null) leftAnchorDir  = null;
            rightAnchor = right;
            leftAnchor  = left;
        } else {
            RemoteClimbState s = REMOTE_STATES.computeIfAbsent(playerId, k -> new RemoteClimbState());
            if (right == null) s.rightAnchorDir = null;
            if (left  == null) s.leftAnchorDir  = null;
            s.rightAnchor = right;
            s.leftAnchor  = left;
        }
    }

    public static void applyStamina(UUID playerId, float rStam, float lStam,
                                     boolean rExh, boolean lExh) {
        if (isLocal(playerId)) {
            rightStamina   = rStam;
            leftStamina    = lStam;
            rightExhausted = rExh;
            leftExhausted  = lExh;
        } else {
            RemoteClimbState s = REMOTE_STATES.computeIfAbsent(playerId, k -> new RemoteClimbState());
            s.rightStamina   = rStam;
            s.leftStamina    = lStam;
            s.rightExhausted = rExh;
            s.leftExhausted  = lExh;
        }
    }

    // -----------------------------------------------------------------
    // Blush helpers (smoothed to avoid jarring snap)
    // -----------------------------------------------------------------
    public static float smoothBlushRight = 0f;
    public static float smoothBlushLeft  = 0f;
    private static final float BLUSH_LERP = 0.04f; // per-tick lerp speed

    /** Call once per client tick to advance blush smoothing. */
    public static void tickBlush() {
        smoothBlushRight = lerp(smoothBlushRight, getRawBlush(false, rightStamina), BLUSH_LERP);
        smoothBlushLeft  = lerp(smoothBlushLeft,  getRawBlush(false, leftStamina),  BLUSH_LERP);
    }

    private static float lerp(float current, float target, float speed) {
        return current + (target - current) * speed;
    }

    private static float getRawBlush(boolean ignored, float stamina) {
        if (!ClimberConfig.INSTANCE.isStaminaEnabled()) return 0f;
        float threshold = ClimberConfig.INSTANCE.getStaminaMax() * 0.5f;
        if (stamina >= threshold) return 0f;
        return 1f - (stamina / threshold);
    }

    public static float getBlushFactor(boolean rightHand) {
        if (!ClimberConfig.INSTANCE.isStaminaEnabled()) return 0f;
        return rightHand ? smoothBlushRight : smoothBlushLeft;
    }

    public static float getBlushFactor(@Nullable RemoteClimbState state, boolean rightHand) {
        if (!ClimberConfig.INSTANCE.isStaminaEnabled()) return 0f;
        if (state == null) return 0f;
        float stamina = rightHand ? state.rightStamina : state.leftStamina;
        // Remote players use immediate blush (no per-entity smoothing needed)
        float threshold = ClimberConfig.INSTANCE.getStaminaMax() * 0.5f;
        if (stamina >= threshold) return 0f;
        return 1f - (stamina / threshold);
    }

    private static boolean isLocal(UUID id) {
        return Minecraft.getInstance().player != null
                && Minecraft.getInstance().player.getUUID().equals(id);
    }
}