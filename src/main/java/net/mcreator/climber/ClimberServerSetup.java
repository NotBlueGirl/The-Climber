package net.mcreator.climber;

import net.mcreator.climber.event.ClimberServerEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;

public class ClimberServerSetup {

    public static void init(IEventBus modBus) {
        modBus.addListener(ClimberServerSetup::onCommonSetup);
    }

    private static void onCommonSetup(FMLCommonSetupEvent event) {
        MinecraftForge.EVENT_BUS.register(new ClimberServerEvents());
        // REMOVED: ClimberClientPhysics registration (moved to ClimberClientSetup)
    }
}