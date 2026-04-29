package net.mcreator.climber.client.screens;

import net.mcreator.climber.ClimberConfig;
import net.mcreator.climber.client.ClimberClientData;

import net.minecraftforge.client.event.RenderGuiEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.api.distmarker.Dist;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.resources.ResourceLocation;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;

@Mod.EventBusSubscriber(Dist.CLIENT)
public class ClimbStatusUIOverlay {

    // Stamina thresholds (matching the spec: >51% = tier 1, 20–50% = tier 2, <20% = tier 3)
    private static final float THRESHOLD_HIGH = 51f;
    private static final float THRESHOLD_LOW  = 20f;

    @SubscribeEvent(priority = EventPriority.NORMAL)
    public static void eventHandler(RenderGuiEvent.Pre event) {
        // Only draw when climbing is active and at least one hand is grabbing
        if (!ClimberClientData.climbEnabled) return;
        if (!ClimberClientData.leftGrabbing && !ClimberClientData.rightGrabbing) return;

        int w = event.getWindow().getGuiScaledWidth();
        int h = event.getWindow().getGuiScaledHeight();

        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA, GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,       GlStateManager.DestFactor.ZERO);
        RenderSystem.setShaderColor(1, 1, 1, 1);

        // ---- Left hand icon (shown only while left hand is grabbing) ----
        if (ClimberClientData.leftGrabbing) {
            String leftTex = selectTexture("left_hand_climbed", ClimberClientData.leftStamina);
            blit(event, new ResourceLocation("climber:textures/screens/" + leftTex + ".png"),
                    w / 2 - 20, h / 2 + 5);
        }

        // ---- Right hand icon (shown only while right hand is grabbing) ----
        if (ClimberClientData.rightGrabbing) {
            String rightTex = selectTexture("right_hand_climbed", ClimberClientData.rightStamina);
            blit(event, new ResourceLocation("climber:textures/screens/" + rightTex + ".png"),
                    w / 2 + 5, h / 2 + 5);
        }

        RenderSystem.depthMask(true);
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
        RenderSystem.setShaderColor(1, 1, 1, 1);
    }

    /**
     * Returns the texture filename (without path/extension) for the given hand prefix
     * based on the current stamina value.
     *
     * Tier 1 (_1): stamina > 51 %
     * Tier 2 (_2): stamina > 20 % AND <= 50 %
     * Tier 3 (_3): stamina <= 20 %
     */
    private static String selectTexture(String prefix, float stamina) {
        // Normalise to 0-100 range regardless of STAMINA_MAX value
        float pct = (stamina / ClimberConfig.STAMINA_MAX) * 100f;

        if (pct > THRESHOLD_HIGH) {
            return prefix + "_1";
        } else if (pct > THRESHOLD_LOW) {
            return prefix + "_2";
        } else {
            return prefix + "_3";
        }
    }

    /** Helper: blit a 16×16 texture at the given screen position. */
    private static void blit(RenderGuiEvent.Pre event, ResourceLocation texture, int x, int y) {
        event.getGuiGraphics().blit(texture, x, y, 0, 0, 16, 16, 16, 16);
    }
}