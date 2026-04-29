package net.mcreator.climber.client;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;

import net.mcreator.climber.ClimberConfig;

import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RenderHandEvent;
import net.minecraftforge.client.event.RenderLevelStageEvent;
import net.minecraftforge.client.event.RenderPlayerEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import java.util.IdentityHashMap;
import java.util.Map;

@OnlyIn(Dist.CLIENT)
public class ClimberArmRenderer {

    // -----------------------------------------------------------------
    // World-space pinned-arm render (third-person / other players)
    // -----------------------------------------------------------------
    @SubscribeEvent
    public void onRenderWorld(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_ENTITIES) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;

        Camera camera = mc.gameRenderer.getMainCamera();
        Vec3 camPos = camera.getPosition();
        PoseStack ps = event.getPoseStack();
        MultiBufferSource.BufferSource buf = mc.renderBuffers().bufferSource();

        // Clean up disconnected players
        ClimberClientData.REMOTE_STATES.keySet().removeIf(id -> mc.level.getPlayerByUUID(id) == null);

        for (Player p : mc.level.players()) {
            if (!(p instanceof AbstractClientPlayer player)) continue;

            ClimberClientData.RemoteClimbState state = ClimberClientData.getState(player);
            if (state == null || !state.climbEnabled) continue;

            Vec3 shoulder = player.position().add(0, player.getEyeHeight() * 0.5, 0);

            if (state.rightGrabbing && state.rightAnchor != null) {
                // Lazy-init: freeze direction once, never recompute
                if (state.rightAnchorDir == null) {
                    Vec3 toShoulder = shoulder.subtract(state.rightAnchor);
                    double len = toShoulder.length();
                    state.rightAnchorDir = len > 0.001 ? toShoulder.scale(1.0 / len) : new Vec3(0, 1, 0);
                }
                renderPinnedHand(ps, buf, mc, camPos, player,
                        state.rightAnchor, state.rightAnchorDir, HumanoidArm.RIGHT, state);
            }
            if (state.leftGrabbing && state.leftAnchor != null) {
                if (state.leftAnchorDir == null) {
                    Vec3 toShoulder = shoulder.subtract(state.leftAnchor);
                    double len = toShoulder.length();
                    state.leftAnchorDir = len > 0.001 ? toShoulder.scale(1.0 / len) : new Vec3(0, 1, 0);
                }
                renderPinnedHand(ps, buf, mc, camPos, player,
                        state.leftAnchor, state.leftAnchorDir, HumanoidArm.LEFT, state);
            }
        }
    }

    private void renderPinnedHand(PoseStack ps, MultiBufferSource buf, Minecraft mc,
                                   Vec3 camPos, AbstractClientPlayer player, Vec3 anchor,
                                   Vec3 frozenDir, HumanoidArm arm,
                                   ClimberClientData.RemoteClimbState state) {
        ps.pushPose();

        Vec3 shoulderRenderPos = anchor.add(frozenDir.scale(0.75));
        ps.translate(
                shoulderRenderPos.x - camPos.x,
                shoulderRenderPos.y - camPos.y,
                shoulderRenderPos.z - camPos.z
        );

        Vec3 down = frozenDir.scale(-1);
        float yaw   = (float) Math.toDegrees(Math.atan2(down.x, down.z));
        float pitch = (float) Math.toDegrees(Math.asin(Math.max(-1.0, Math.min(1.0, -down.y))));

        ps.mulPose(Axis.YP.rotationDegrees(yaw));
        ps.mulPose(Axis.XP.rotationDegrees(pitch + 90f));

        var renderer = (net.minecraft.client.renderer.entity.player.PlayerRenderer)
                mc.getEntityRenderDispatcher().getRenderer(player);
        var model = renderer.getModel();
        model.setupAnim(player, 0, 0, 0, 0, 0);

        // Save visibility
        boolean headVisible     = model.head.visible;
        boolean hatVisible      = model.hat.visible;
        boolean bodyVisible     = model.body.visible;
        boolean rightLegVisible = model.rightLeg.visible;
        boolean leftLegVisible  = model.leftLeg.visible;
        boolean rightArmVisible = model.rightArm.visible;
        boolean leftArmVisible  = model.leftArm.visible;
        boolean rightSleeveVisible = model.rightSleeve.visible;
        boolean leftSleeveVisible  = model.leftSleeve.visible;

        boolean isRight = (arm == HumanoidArm.RIGHT);
        float blush = ClimberClientData.getBlushFactor(state, isRight);
        int overlay = blush > 0.01f
                ? OverlayTexture.pack(0, true)
                : OverlayTexture.NO_OVERLAY;

        int brightLight = 15728880;
        int light = blush > 0.5f
                ? (int)(brightLight * (1f - blush * 0.3f))
                : brightLight;

        VertexConsumer vcSolid  = buf.getBuffer(RenderType.entitySolid(player.getSkinTextureLocation()));
        VertexConsumer vcCutout = buf.getBuffer(RenderType.entityCutoutNoCull(player.getSkinTextureLocation()));

        if (isRight) {
            hideAllExcept(model, true);
            model.rightArm.render(ps, vcSolid, light, overlay);
            model.rightSleeve.visible = true;
            model.rightSleeve.render(ps, vcCutout, light, overlay);
        } else {
            hideAllExcept(model, false);
            model.leftArm.render(ps, vcSolid, light, overlay);
            model.leftSleeve.visible = true;
            model.leftSleeve.render(ps, vcCutout, light, overlay);
        }

        // Restore
        model.head.visible     = headVisible;
        model.hat.visible      = hatVisible;
        model.body.visible     = bodyVisible;
        model.rightLeg.visible = rightLegVisible;
        model.leftLeg.visible  = leftLegVisible;
        model.rightArm.visible = rightArmVisible;
        model.leftArm.visible  = leftArmVisible;
        model.rightSleeve.visible = rightSleeveVisible;
        model.leftSleeve.visible  = leftSleeveVisible;

        ps.popPose();
    }

    // -----------------------------------------------------------------
    // Hide pinned arms from normal player renderer
    // -----------------------------------------------------------------
    private final Map<Player, ItemStack> suppressedMainItems = new IdentityHashMap<>();
    private final Map<Player, ItemStack> suppressedOffItems  = new IdentityHashMap<>();

    @SubscribeEvent
    public void onRenderPlayerPre(RenderPlayerEvent.Pre event) {
        Player player = event.getEntity();
        PlayerModel<?> model = event.getRenderer().getModel();

        ClimberClientData.RemoteClimbState state = ClimberClientData.getState(player);
        if (state == null || !state.climbEnabled) return;

        if (state.rightGrabbing) {
            model.rightArm.visible    = false;
            model.rightSleeve.visible = false;

            boolean rightIsMain = (player.getMainArm() == HumanoidArm.RIGHT);
            if (rightIsMain) {
                suppressedMainItems.put(player, player.getMainHandItem().copy());
                player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
            } else {
                suppressedOffItems.put(player, player.getOffhandItem().copy());
                player.getInventory().offhand.set(0, ItemStack.EMPTY);
            }
        }

        if (state.leftGrabbing) {
            model.leftArm.visible    = false;
            model.leftSleeve.visible = false;

            boolean leftIsMain = (player.getMainArm() == HumanoidArm.LEFT);
            if (leftIsMain) {
                suppressedMainItems.put(player, player.getMainHandItem().copy());
                player.getInventory().items.set(player.getInventory().selected, ItemStack.EMPTY);
            } else {
                suppressedOffItems.put(player, player.getOffhandItem().copy());
                player.getInventory().offhand.set(0, ItemStack.EMPTY);
            }
        }
    }

    @SubscribeEvent
    public void onRenderPlayerPost(RenderPlayerEvent.Post event) {
        Player player = event.getEntity();
        PlayerModel<?> model = event.getRenderer().getModel();

        model.rightArm.visible    = true;
        model.leftArm.visible     = true;
        model.rightSleeve.visible = true;
        model.leftSleeve.visible  = true;

        if (suppressedMainItems.containsKey(player)) {
            player.getInventory().items.set(player.getInventory().selected, suppressedMainItems.remove(player));
        }
        if (suppressedOffItems.containsKey(player)) {
            player.getInventory().offhand.set(0, suppressedOffItems.remove(player));
        }
    }

    // -----------------------------------------------------------------
    // First-person hand rendering
    // -----------------------------------------------------------------
    @SubscribeEvent(priority = EventPriority.HIGH)
    public void onRenderHand(RenderHandEvent event) {
        if (!ClimberClientData.climbEnabled) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        boolean isRight = (event.getHand() == InteractionHand.MAIN_HAND);
        boolean grabbing = isRight ? ClimberClientData.rightGrabbing : ClimberClientData.leftGrabbing;

        if (grabbing) {
            event.setCanceled(true);
            return;
        }

        PoseStack ps  = event.getPoseStack();
        MultiBufferSource buf = event.getMultiBufferSource();
        int light = event.getPackedLight();

        float baseSide  = isRight ?  0.56f : -0.56f;
        float baseDown  = -0.52f;
        float baseDepth = -0.72f;

        Vec3 offset = ClimberConfig.INSTANCE.getIdleHandPos();
        float offX = (float) offset.x * (isRight ? 1 : -1);
        float offY = (float) offset.y;
        float offZ = (float) offset.z;

        ps.pushPose();
        ps.translate(baseSide + offX, baseDown + offY, baseDepth + offZ);
        ps.mulPose(Axis.ZP.rotationDegrees(isRight ? -18f : 18f));
        ps.mulPose(Axis.XP.rotationDegrees(-35f));

        var renderer = (net.minecraft.client.renderer.entity.player.PlayerRenderer)
                mc.getEntityRenderDispatcher().getRenderer(mc.player);
        var model = renderer.getModel();
        model.setupAnim(mc.player, 0, 0, 0, 0, 0);

        boolean exhausted = isRight ? ClimberClientData.rightExhausted : ClimberClientData.leftExhausted;
        int overlay = exhausted ? OverlayTexture.pack(0, true) : OverlayTexture.NO_OVERLAY;

        var vcSolid  = buf.getBuffer(RenderType.entitySolid(mc.player.getSkinTextureLocation()));
        var vcCutout = buf.getBuffer(RenderType.entityCutoutNoCull(mc.player.getSkinTextureLocation()));
        if (isRight) {
            hideAllExcept(model, true);
            model.rightArm.render(ps, vcSolid, light, overlay);
            model.rightSleeve.visible = true;
            model.rightSleeve.render(ps, vcCutout, light, overlay);
            model.rightSleeve.visible = false;
        } else {
            hideAllExcept(model, false);
            model.leftArm.render(ps, vcSolid, light, overlay);
            model.leftSleeve.visible = true;
            model.leftSleeve.render(ps, vcCutout, light, overlay);
            model.leftSleeve.visible = false;
        }

        ps.popPose();
        event.setCanceled(true);
    }

    private void hideAllExcept(PlayerModel<?> model, boolean rightArm) {
        model.head.visible     = false;
        model.hat.visible      = false;
        model.body.visible     = false;
        model.rightLeg.visible = false;
        model.leftLeg.visible  = false;
        model.rightArm.visible = rightArm;
        model.leftArm.visible  = !rightArm;
    }
}