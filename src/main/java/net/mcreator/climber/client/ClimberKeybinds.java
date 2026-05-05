package net.mcreator.climber.client;

import net.mcreator.climber.ClimberConfig;
import net.mcreator.climber.ClimberMod;

import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import org.lwjgl.glfw.GLFW;

@OnlyIn(Dist.CLIENT)
public class ClimberKeybinds {

    public static final KeyMapping KEY_TOGGLE_CLIMB = new KeyMapping(
            "Climb On/Off",
            GLFW.GLFW_KEY_UNKNOWN,
            "The Climber"
    );

    public static final KeyMapping KEY_TOGGLE_STAMINA = new KeyMapping(
            "Stamina On/Off",
            GLFW.GLFW_KEY_UNKNOWN,
            "The Climber"
    );

    public static void registerKeys(IEventBus modBus) {
        modBus.addListener(ClimberKeybinds::onRegisterKeyMappings);
    }

    private static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(KEY_TOGGLE_CLIMB);
        event.register(KEY_TOGGLE_STAMINA);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        while (KEY_TOGGLE_CLIMB.consumeClick()) {
            boolean nowEnabled = !ClimberClientData.climbEnabled;
            mc.player.connection.sendCommand(nowEnabled ? "climb on" : "climb off");
            mc.player.displayClientMessage(
                    Component.literal(nowEnabled ? "§aClimbing enabled!" : "§cClimbing disabled."),
                    true);
        }

        while (KEY_TOGGLE_STAMINA.consumeClick()) {
            boolean nowEnabled = !ClimberConfig.INSTANCE.isStaminaEnabled();
            mc.player.connection.sendCommand(nowEnabled ? "climb stamina on" : "climb stamina off");
            mc.player.displayClientMessage(
                    Component.literal(nowEnabled ? "§aStamina enabled!" : "§cStamina disabled."),
                    true);
        }
    }
}