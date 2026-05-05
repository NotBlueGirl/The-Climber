package net.mcreator.climber.command;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.DoubleArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;

import net.mcreator.climber.ClimberConfig;
import net.mcreator.climber.ClimberMod;
import net.mcreator.climber.capability.ClimbCapability;
import net.mcreator.climber.network.ClimbStatePacket;
import net.mcreator.climber.network.StaminaSyncPacket;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.PacketDistributor;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.registries.ForgeRegistries;

import java.io.File;
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

                    .then(Commands.literal("reset")
                        .executes(ctx -> resetConfig(ctx.getSource())))

                    .then(Commands.literal("save")
                        .executes(ctx -> saveConfig(ctx.getSource())))

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
                                    true))))

                    .then(Commands.literal("StaminaValue")
                        .then(Commands.literal("reset")
                            .executes(ctx -> resetStaminaMax(ctx.getSource())))
                        .then(Commands.argument("value", IntegerArgumentType.integer(1))
                            .executes(ctx -> setStaminaMax(
                                    ctx.getSource(),
                                    IntegerArgumentType.getInteger(ctx, "value")))))

                    .then(Commands.literal("StaminaDecreaseSpeed")
                        .then(Commands.literal("reset")
                            .executes(ctx -> resetStaminaDrain(ctx.getSource())))
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0f))
                            .executes(ctx -> setStaminaDrain(
                                    ctx.getSource(),
                                    FloatArgumentType.getFloat(ctx, "value")))))

                    .then(Commands.literal("StaminaRecoverySpeed")
                        .then(Commands.literal("reset")
                            .executes(ctx -> resetStaminaRegen(ctx.getSource())))
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0f))
                            .executes(ctx -> setStaminaRegen(
                                    ctx.getSource(),
                                    FloatArgumentType.getFloat(ctx, "value")))))

                    .then(Commands.literal("StrengthJumpBothHands")
                        .then(Commands.literal("reset")
                            .executes(ctx -> resetStrengthJumpBothHands(ctx.getSource())))
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0f))
                            .executes(ctx -> setStrengthJumpBothHands(
                                    ctx.getSource(),
                                    FloatArgumentType.getFloat(ctx, "value")))))

                    .then(Commands.literal("StrengthJumpOneHand")
                        .then(Commands.literal("reset")
                            .executes(ctx -> resetStrengthJumpOneHand(ctx.getSource())))
                        .then(Commands.argument("value", FloatArgumentType.floatArg(0f))
                            .executes(ctx -> setStrengthJumpOneHand(
                                    ctx.getSource(),
                                    FloatArgumentType.getFloat(ctx, "value")))))

                    .then(Commands.literal("GrabHitSound")
                        .then(Commands.literal("reset")
                            .executes(ctx -> resetGrabHitSound(ctx.getSource())))
                        .then(Commands.argument("soundId", StringArgumentType.greedyString())
                            .executes(ctx -> setGrabHitSound(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "soundId")))))

                    .then(Commands.literal("GrabJumpSound")
                        .then(Commands.literal("reset")
                            .executes(ctx -> resetGrabJumpSound(ctx.getSource())))
                        .then(Commands.argument("soundId", StringArgumentType.greedyString())
                            .executes(ctx -> setGrabJumpSound(
                                    ctx.getSource(),
                                    StringArgumentType.getString(ctx, "soundId")))))
                )
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

        if (!enabled) {
            source.getServer().getPlayerList().getPlayers().forEach(sp ->
                ClimbCapability.get(sp).ifPresent(data -> {
                    float max = ClimberConfig.INSTANCE.getStaminaMax();
                    data.setRightStamina(max);
                    data.setLeftStamina(max);
                    data.setRightExhausted(false);
                    data.setLeftExhausted(false);
                    ClimberMod.PACKET_HANDLER.send(
                        PacketDistributor.TRACKING_ENTITY_AND_SELF.with(() -> sp),
                        new StaminaSyncPacket(sp.getUUID(), max, max, false, false));
                })
            );
        }

        String msg = enabled
                ? "§aStamina system §lenabled§r§a."
                : "§7Stamina system §ldisabled§r§7. All stamina restored.";
        source.sendSuccess(() -> Component.literal(msg), true);
        return 1;
    }

    private static int resetConfig(CommandSourceStack source) {
        ClimberConfig.INSTANCE.resetAll();
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal("§aAll config values reset to defaults."), true);
        return 1;
    }

    private static int saveConfig(CommandSourceStack source) {
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal("§aConfig saved to config/climber_config.json."), true);
        return 1;
    }

    private static int setStaminaMax(CommandSourceStack source, int value) {
        ClimberConfig.INSTANCE.setStaminaMax(value);
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal("§aStamina max set to §f" + value + "§a."), true);
        return 1;
    }

    private static int resetStaminaMax(CommandSourceStack source) {
        ClimberConfig.INSTANCE.resetStaminaMax();
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal(
                "§aStamina max reset to default (§f" + (int)ClimberConfig.DEFAULT_STAMINA_MAX + "§a)."), true);
        return 1;
    }

    private static int setStaminaDrain(CommandSourceStack source, float value) {
        ClimberConfig.INSTANCE.setStaminaDrainRate(value);
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal("§aStamina decrease speed set to §f" + fmtF(value) + "§a/tick."), true);
        return 1;
    }

    private static int resetStaminaDrain(CommandSourceStack source) {
        ClimberConfig.INSTANCE.resetStaminaDrainRate();
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal(
                "§aStamina decrease speed reset to default (§f" + fmtF(ClimberConfig.DEFAULT_STAMINA_DRAIN_RATE) + "§a/tick)."), true);
        return 1;
    }

    private static int setStaminaRegen(CommandSourceStack source, float value) {
        ClimberConfig.INSTANCE.setStaminaRegenRate(value);
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal("§aStamina recovery speed set to §f" + fmtF(value) + "§a/tick."), true);
        return 1;
    }

    private static int resetStaminaRegen(CommandSourceStack source) {
        ClimberConfig.INSTANCE.resetStaminaRegenRate();
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal(
                "§aStamina recovery speed reset to default (§f" + fmtF(ClimberConfig.DEFAULT_STAMINA_REGEN_RATE) + "§a/tick)."), true);
        return 1;
    }

    private static String fmtF(float v) { return String.format("%.4f", v); }

    private static int setStrengthJumpBothHands(CommandSourceStack source, float value) {
        ClimberConfig.INSTANCE.setStrengthJumpBothHands(value);
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal("§aTwo-hand jump strength set to §f" + fmtF(value) + "§a."), true);
        return 1;
    }

    private static int resetStrengthJumpBothHands(CommandSourceStack source) {
        ClimberConfig.INSTANCE.resetStrengthJumpBothHands();
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal(
                "§aTwo-hand jump strength reset to default (§f" + fmtF(ClimberConfig.DEFAULT_STRENGTH_JUMP_BOTH_HANDS) + "§a)."), true);
        return 1;
    }

    private static int setStrengthJumpOneHand(CommandSourceStack source, float value) {
        ClimberConfig.INSTANCE.setStrengthJumpOneHand(value);
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal("§aOne-hand jump strength set to §f" + fmtF(value) + "§a."), true);
        return 1;
    }

    private static int resetStrengthJumpOneHand(CommandSourceStack source) {
        ClimberConfig.INSTANCE.resetStrengthJumpOneHand();
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal(
                "§aOne-hand jump strength reset to default (§f" + fmtF(ClimberConfig.DEFAULT_STRENGTH_JUMP_ONE_HAND) + "§a)."), true);
        return 1;
    }

    // -----------------------------------------------------------------
    // Sound helpers (files OR registry IDs)
    // -----------------------------------------------------------------

    private static String stripQuotes(String s) {
        if (s.length() >= 2 && ((s.startsWith("\"") && s.endsWith("\""))
                || (s.startsWith("'") && s.endsWith("'")))) {
            return s.substring(1, s.length() - 1);
        }
        return s;
    }

    private static boolean isFilePath(String s) {
        return s.contains("\\")
            || (s.length() > 1 && s.charAt(1) == ':')
            || s.startsWith("/")
            || s.startsWith("./")
            || s.startsWith("../");
    }

    private static int setGrabHitSound(CommandSourceStack source, String rawPath) {
        String path = stripQuotes(rawPath.trim());

        if (path.isEmpty()) {
            source.sendFailure(Component.literal("§cGrabHitSound path cannot be empty."));
            return 0;
        }

        if (isFilePath(path)) {
            File f = new File(path);
            if (!f.exists()) {
                source.sendFailure(Component.literal("§cFile not found: §f" + path));
                return 0;
            }
            String low = path.toLowerCase();
            if (!low.endsWith(".ogg") && !low.endsWith(".wav") && !low.endsWith(".mp3")) {
                source.sendFailure(Component.literal("§cSound file must be .ogg, .wav, or .mp3"));
                return 0;
            }
            ClimberConfig.INSTANCE.setGrabHitSound(path);
            ClimberConfig.INSTANCE.save();
            source.sendSuccess(() -> Component.literal("§aGrab-hit sound set to file: §f" + path), true);
            return 1;
        } else {
            // Registry ID
            try {
                ResourceLocation rl = new ResourceLocation(path);
                if (ForgeRegistries.SOUND_EVENTS.getValue(rl) == null) {
                    source.sendFailure(Component.literal("§cSound §f" + rl + "§c is not registered."));
                    return 0;
                }
                ClimberConfig.INSTANCE.setGrabHitSound(path);
                ClimberConfig.INSTANCE.save();
                source.sendSuccess(() -> Component.literal("§aGrab-hit sound set to: §f" + path), true);
                return 1;
            } catch (Exception e) {
                source.sendFailure(Component.literal("§cInvalid sound ID: §f" + path));
                return 0;
            }
        }
    }

    private static int resetGrabHitSound(CommandSourceStack source) {
        ClimberConfig.INSTANCE.resetGrabHitSound();
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal("§aGrab-hit sound reset to default (block break sound)."), true);
        return 1;
    }

    private static int setGrabJumpSound(CommandSourceStack source, String rawPath) {
        String path = stripQuotes(rawPath.trim());

        if (path.isEmpty()) {
            source.sendFailure(Component.literal("§cGrabJumpSound path cannot be empty."));
            return 0;
        }

        if (isFilePath(path)) {
            File f = new File(path);
            if (!f.exists()) {
                source.sendFailure(Component.literal("§cFile not found: §f" + path));
                return 0;
            }
            String low = path.toLowerCase();
            if (!low.endsWith(".ogg") && !low.endsWith(".wav") && !low.endsWith(".mp3")) {
                source.sendFailure(Component.literal("§cSound file must be .ogg, .wav, or .mp3"));
                return 0;
            }
            ClimberConfig.INSTANCE.setGrabJumpSound(path);
            ClimberConfig.INSTANCE.save();
            source.sendSuccess(() -> Component.literal("§aGrab-jump sound set to file: §f" + path), true);
            return 1;
        } else {
            try {
                ResourceLocation rl = new ResourceLocation(path);
                if (ForgeRegistries.SOUND_EVENTS.getValue(rl) == null) {
                    source.sendFailure(Component.literal("§cSound §f" + rl + "§c is not registered."));
                    return 0;
                }
                ClimberConfig.INSTANCE.setGrabJumpSound(path);
                ClimberConfig.INSTANCE.save();
                source.sendSuccess(() -> Component.literal("§aGrab-jump sound set to: §f" + path), true);
                return 1;
            } catch (Exception e) {
                source.sendFailure(Component.literal("§cInvalid sound ID: §f" + path));
                return 0;
            }
        }
    }

    private static int resetGrabJumpSound(CommandSourceStack source) {
        ClimberConfig.INSTANCE.resetGrabJumpSound();
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal("§aGrab-jump sound reset to default (built-in grab-jump sound)."), true);
        return 1;
    }

    // -----------------------------------------------------------------
    // Idle hand pos & list helpers
    // -----------------------------------------------------------------

    private static int setIdleHandPos(CommandSourceStack source, double x, double y, double z) {
        ClimberConfig.INSTANCE.setIdleHandPos(new Vec3(x, y, z));
        ClimberConfig.INSTANCE.save();
        source.sendSuccess(() -> Component.literal(
                "§aIdle hand position set to (" + fmt(x) + ", " + fmt(y) + ", " + fmt(z) + ")."), true);
        return 1;
    }

    private static int resetIdleHandPos(CommandSourceStack source) {
        ClimberConfig.INSTANCE.resetIdleHandPos();
        ClimberConfig.INSTANCE.save();
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
        } else {
            ClimberConfig.INSTANCE.addExcludes(ids);
        }
        ClimberConfig.INSTANCE.save();

        if (include) {
            source.sendSuccess(() -> Component.literal(
                    "§aAdded to include list: §f" + String.join(", ", ids)), true);
        } else {
            source.sendSuccess(() -> Component.literal(
                    "§aAdded to exclude list: §f" + String.join(", ", ids)), true);
        }
        return 1;
    }

    private static int resetList(CommandSourceStack source, boolean include) {
        if (include) {
            ClimberConfig.INSTANCE.resetIncludeList();
        } else {
            ClimberConfig.INSTANCE.resetExcludeList();
        }
        ClimberConfig.INSTANCE.save();

        if (include) {
            source.sendSuccess(() -> Component.literal("§aInclude list reset to default (all solid blocks)."), true);
        } else {
            source.sendSuccess(() -> Component.literal("§aExclude list reset to default (empty)."), true);
        }
        return 1;
    }

    private static String fmt(double v) {
        return String.format("%.3f", v);
    }
}