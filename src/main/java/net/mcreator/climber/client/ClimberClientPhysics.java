package net.mcreator.climber.client;

import net.mcreator.climber.event.ClimberServerEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

@OnlyIn(Dist.CLIENT)
public class ClimberClientPhysics {

    @SubscribeEvent
    public void onClientTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        Player player = event.player;
        Minecraft mc = Minecraft.getInstance();
        
        // Only apply to our local player
        if (player != mc.player) return;

        if (!ClimberClientData.climbEnabled || (!ClimberClientData.rightGrabbing && !ClimberClientData.leftGrabbing)) {
            player.setNoGravity(false);
            return;
        }

        // Run the exact same math locally using the ClientData!
        ClimberServerEvents.applyClimbPhysics(
                player,
                ClimberClientData.rightGrabbing,
                ClimberClientData.leftGrabbing,
                ClimberClientData.rightAnchor,
                ClimberClientData.leftAnchor
        );
    }
}