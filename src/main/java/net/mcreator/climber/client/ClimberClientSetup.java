package net.mcreator.climber.client;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

@OnlyIn(Dist.CLIENT)
public class ClimberClientSetup {

    public static void init(IEventBus modBus) {
        modBus.addListener(ClimberClientSetup::onClientSetup);
    }

    private static void onClientSetup(FMLClientSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new ClimberInputHandler());
        MinecraftForge.EVENT_BUS.register(new ClimberKeyInputHandler());
        MinecraftForge.EVENT_BUS.register(new ClimberArmRenderer());
        // BUG FIX: Client physics must be registered on the client side only
        MinecraftForge.EVENT_BUS.register(new ClimberClientPhysics());
    }
}