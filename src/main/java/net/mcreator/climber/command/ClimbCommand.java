package net.mcreator.climber.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.mcreator.climber.ClimberConfig;
import net.mcreator.climber.ClimberMod;
import net.mcreator.climber.capability.ClimbCapability;
import net.mcreator.climber.network.ClimbStatePacket;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Mod.EventBusSubscriber(modid = ClimberMod.MODID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public class ClimbCommand {

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        register(event.getDispatcher());
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("climb")

                .then(Commands.literal("on")
                    .executes(ctx -> setClimb(ctx.getSource(), true)))
                .then(Commands.literal("off")
                    .executes(ctx -> setClimb(ctx.getSource(), false)))

                .then(Commands.literal("stamina")
                    .then(Commands.literal("on")
                        .executes(ctx -> setStamina(ctx.getSource(), true)))
                    .then(Commands.literal("off")
                        .executes(ctx -> setStamina(ctx.getSource(), false))))

                .then(Commands.literal("config")

                    .then(Commands.literal("IdleHandPos")
                        .then(Commands.literal("reset")
                            .executes(ctx -> resetIdleHandPos(ctx.getSource())))
                        .then(Commands.argument("x", DoubleArgumentType.doubleArg())
                            .then(Commands.argument("y", DoubleArgumentType.doubleArg())
                                .then(Commands.argument("z", DoubleArgumentType.doubleArg())
                                    .executes(ctx -> setIdleHandPos(
                                            ctx.getSource(),
                                            DoubleArgumentType.getDouble(ctx, "x"),
                                            DoubleArgumentType.getDouble(ctx, "y"),
                                            DoubleArgumentType.getDouble(ctx, "z")))))))

                    .then(Commands.literal("exclude")
                        .then(Commands.literal("reset")
                            .executes(ctx -> resetList(ctx.getSource(), false)))
                        .then(Commands.argument("blocks", StringArgumentType.greedyString())
                            .executes(ctx -> modifyList(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "blocks"),
                                    false))))

                    .then(Commands.literal("include")
                        .then(Commands.literal("reset")
                            .executes(ctx -> resetList(ctx.getSource(), true)))
                        .then(Commands.argument("blocks", StringArgumentType.greedyString())
                            .executes(ctx -> modifyList(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "blocks"),
                                    true)))))
        );
    }

    private static int setClimb(CommandSourceStack source, boolean enabled) {
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("This command can only be used by a player."));
            return 0;
        }

        ClimbCapability.get(player).ifPresent(data -> {
            data.setClimbEnabled(enabled);

            if (!enabled) {
                data.setRightHandGrabbing(false);
                data.setLeftHandGrabbing(false);
            }

            // >>> FIX: broadcast to tracking players <<<
            ClimberMod.PACKET_HANDLER.send(
                net.minecraftforge.network.PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> player),
                new ClimbStatePacket(player.getUUID(), enabled, false, false)
            );
        });

        String stateMsg = enabled
                ? "§aClimbing enabled! Use RMB/LMB to grab."
                : "§cClimbing disabled.";
        player.sendSystemMessage(Component.literal(stateMsg));
        return 1;
    }

    private static int setStamina(CommandSourceStack source, boolean enabled) {
        ClimberConfig.INSTANCE.setStaminaEnabled(enabled);
        String msg = enabled
                ? "§aStamina system §lenabled§r§a."
                : "§7Stamina system §ldisabled§r§7.";
        source.sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    private static int setIdleHandPos(CommandSourceStack source, double x, double y, double z) {
        ClimberConfig.INSTANCE.setIdleHandPos(new Vec3(x, y, z));
        source.sendSuccess(() -> Component.literal(
                "§aIdle hand position set to (" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")."), true);
        return 1;
    }

    private static int resetIdleHandPos(CommandSourceStack source) {
        ClimberConfig.INSTANCE.resetIdleHandPos();
        source.sendSuccess(() -> Component.literal("§aIdle hand position reset to default."), true);
        return 1;
    }

    private static int modifyList(CommandSourceStack source, String raw, boolean include) {
        List<String> ids = Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());

        List<String> invalid = ids.stream()
                .filter(s -> !s.contains(":"))
                .collect(Collectors.toList());

        if (!invalid.isEmpty()) {
            source.sendFailure(Component.literal(
                    "§cInvalid block IDs (must be like 'minecraft:stone'): " +
                    String.join(", ", invalid)));
            return 0;
        }

        if (include) {
            ClimberConfig.INSTANCE.addIncludes(ids);
            source.sendSuccess(() -> Component.literal(
                    "§aAdded to include list: §f" + String.join(", ", ids)), true);
        } else {
            ClimberConfig.INSTANCE.addExcludes(ids);
            source.sendSuccess(() -> Component.literal(
                    "§aAdded to exclude list: §f" + String.join(", ", ids)), true);
        }
        return 1;
    }

    private static int resetList(CommandSourceStack source, boolean include) {
        if (include) {
            ClimberConfig.INSTANCE.resetIncludeList();
            source.sendSuccess(() -> Component.literal("§aInclude list reset to default (all solid blocks)."), true);
        } else {
            ClimberConfig.INSTANCE.resetExcludeList();
            source.sendSuccess(() -> Component.literal("§aExclude list reset to default (empty)."), true);
        }
        return 1;
    }

    private static String fmt(double v) {
        return String.format("%.3f", v);
    }
}