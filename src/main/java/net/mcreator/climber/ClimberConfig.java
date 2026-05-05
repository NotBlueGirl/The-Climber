package net.mcreator.climber;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.loading.FMLPaths;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

public class ClimberConfig {

    public static final Vec3  DEFAULT_IDLE_HAND_POS           = new Vec3(0.0, 0.0, 0.0);
    public static final float DEFAULT_STAMINA_MAX              = 100f;
    public static final float DEFAULT_STAMINA_DRAIN_RATE       = 0.66f;
    public static final float DEFAULT_STAMINA_REGEN_RATE       = 0.375f;
    public static final float DEFAULT_STRENGTH_JUMP_BOTH_HANDS = 0.85f;
    public static final float DEFAULT_STRENGTH_JUMP_ONE_HAND   = 0.85f;
    /** null = use the default block-break sound */
    public static final String DEFAULT_GRAB_HIT_SOUND  = null;
    /** null = use the built-in grab-jump sound */
    public static final String DEFAULT_GRAB_JUMP_SOUND = null;

    public static final ClimberConfig INSTANCE = new ClimberConfig();
    private ClimberConfig() {}

    // --- Idle hand pos ---
    private Vec3 idleHandPos = DEFAULT_IDLE_HAND_POS;
    public Vec3 getIdleHandPos()       { return idleHandPos != null ? idleHandPos : DEFAULT_IDLE_HAND_POS; }
    public void setIdleHandPos(Vec3 v) { idleHandPos = (v != null) ? v : DEFAULT_IDLE_HAND_POS; }
    public void resetIdleHandPos()     { idleHandPos = DEFAULT_IDLE_HAND_POS; }

    // --- Exclude list ---
    private final Set<String> excludeList = new HashSet<>();
    public Set<String> getExcludeList()             { return excludeList; }
    public void addExcludes(Iterable<String> ids)    { ids.forEach(excludeList::add); }
    public void resetExcludeList()                   { excludeList.clear(); }

    // --- Include list ---
    private final Set<String> includeList = new HashSet<>();
    public Set<String> getIncludeList()             { return includeList; }
    public void addIncludes(Iterable<String> ids)    { ids.forEach(includeList::add); }
    public void resetIncludeList()                   { includeList.clear(); }

    // --- Stamina toggle ---
    private boolean staminaEnabled = false;
    public boolean isStaminaEnabled()           { return staminaEnabled; }
    public void    setStaminaEnabled(boolean v) { staminaEnabled = v; }

    // --- Mutable stamina values ---
    private float staminaMax       = DEFAULT_STAMINA_MAX;
    private float staminaDrainRate = DEFAULT_STAMINA_DRAIN_RATE;
    private float staminaRegenRate = DEFAULT_STAMINA_REGEN_RATE;

    public float getStaminaMax()              { return staminaMax; }
    public void  setStaminaMax(float v)       { staminaMax = Math.max(1f, v); }
    public void  resetStaminaMax()            { staminaMax = DEFAULT_STAMINA_MAX; }

    public float getStaminaDrainRate()        { return staminaDrainRate; }
    public void  setStaminaDrainRate(float v) { staminaDrainRate = Math.max(0f, v); }
    public void  resetStaminaDrainRate()      { staminaDrainRate = DEFAULT_STAMINA_DRAIN_RATE; }

    public float getStaminaRegenRate()        { return staminaRegenRate; }
    public void  setStaminaRegenRate(float v) { staminaRegenRate = Math.max(0f, v); }
    public void  resetStaminaRegenRate()      { staminaRegenRate = DEFAULT_STAMINA_REGEN_RATE; }

    // --- Jump strength ---
    private float strengthJumpBothHands = DEFAULT_STRENGTH_JUMP_BOTH_HANDS;
    private float strengthJumpOneHand   = DEFAULT_STRENGTH_JUMP_ONE_HAND;

    public float getStrengthJumpBothHands()        { return strengthJumpBothHands; }
    public void  setStrengthJumpBothHands(float v) { strengthJumpBothHands = Math.max(0f, v); }
    public void  resetStrengthJumpBothHands()      { strengthJumpBothHands = DEFAULT_STRENGTH_JUMP_BOTH_HANDS; }

    public float getStrengthJumpOneHand()        { return strengthJumpOneHand; }
    public void  setStrengthJumpOneHand(float v) { strengthJumpOneHand = Math.max(0f, v); }
    public void  resetStrengthJumpOneHand()      { strengthJumpOneHand = DEFAULT_STRENGTH_JUMP_ONE_HAND; }

    // --- Custom sound paths (null = use built-in default) ---
    private String grabHitSound  = DEFAULT_GRAB_HIT_SOUND;
    private String grabJumpSound = DEFAULT_GRAB_JUMP_SOUND;

    public String getGrabHitSound()           { return grabHitSound; }
    public void   setGrabHitSound(String v)   { grabHitSound  = (v != null && !v.isBlank()) ? v.trim() : null; }
    public void   resetGrabHitSound()         { grabHitSound  = DEFAULT_GRAB_HIT_SOUND; }

    public String getGrabJumpSound()          { return grabJumpSound; }
    public void   setGrabJumpSound(String v)  { grabJumpSound = (v != null && !v.isBlank()) ? v.trim() : null; }
    public void   resetGrabJumpSound()        { grabJumpSound = DEFAULT_GRAB_JUMP_SOUND; }

    // (ClimberClientData and ClimbCapability use this field)

    // --- Reset all ---
    public void resetAll() {
        resetIdleHandPos();
        resetExcludeList();
        resetIncludeList();
        staminaEnabled       = false;
        staminaMax           = DEFAULT_STAMINA_MAX;
        staminaDrainRate     = DEFAULT_STAMINA_DRAIN_RATE;
        staminaRegenRate     = DEFAULT_STAMINA_REGEN_RATE;
        strengthJumpBothHands = DEFAULT_STRENGTH_JUMP_BOTH_HANDS;
        strengthJumpOneHand   = DEFAULT_STRENGTH_JUMP_ONE_HAND;
        grabHitSound          = DEFAULT_GRAB_HIT_SOUND;
        grabJumpSound         = DEFAULT_GRAB_JUMP_SOUND;
    }

    // --- Grabbable check ---
    public boolean isGrabbable(String registryName) {
        if (!includeList.isEmpty() && !includeList.contains(registryName)) return false;
        if (excludeList.contains(registryName)) return false;
        return true;
    }

    // --- Persistence ---
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("climber_config.json");
    }

    public void save() {
        JsonObject obj = new JsonObject();
        obj.addProperty("staminaEnabled",        staminaEnabled);
        obj.addProperty("staminaMax",             staminaMax);
        obj.addProperty("staminaDrainRate",       staminaDrainRate);
        obj.addProperty("staminaRegenRate",       staminaRegenRate);
        obj.addProperty("strengthJumpBothHands",  strengthJumpBothHands);
        obj.addProperty("strengthJumpOneHand",    strengthJumpOneHand);
        if (grabHitSound  != null) obj.addProperty("grabHitSound",  grabHitSound);
        if (grabJumpSound != null) obj.addProperty("grabJumpSound", grabJumpSound);

        Vec3 hp = getIdleHandPos();
        JsonObject hand = new JsonObject();
        hand.addProperty("x", hp.x);
        hand.addProperty("y", hp.y);
        hand.addProperty("z", hp.z);
        obj.add("idleHandPos", hand);

        com.google.gson.JsonArray excl = new com.google.gson.JsonArray();
        excludeList.forEach(excl::add);
        obj.add("excludeList", excl);

        com.google.gson.JsonArray incl = new com.google.gson.JsonArray();
        includeList.forEach(incl::add);
        obj.add("includeList", incl);

        try (Writer w = new FileWriter(configPath().toFile())) {
            GSON.toJson(obj, w);
        } catch (IOException e) {
            ClimberMod.LOGGER.error("[Climber] Failed to save config: {}", e.getMessage());
        }
    }

    public void load() {
        Path path = configPath();
        if (!Files.exists(path)) return;
        try (Reader r = new FileReader(path.toFile())) {
            JsonObject obj = GSON.fromJson(r, JsonObject.class);
            if (obj == null) return;
            if (obj.has("staminaEnabled"))       staminaEnabled       = obj.get("staminaEnabled").getAsBoolean();
            if (obj.has("staminaMax"))            staminaMax           = obj.get("staminaMax").getAsFloat();
            if (obj.has("staminaDrainRate"))      staminaDrainRate     = obj.get("staminaDrainRate").getAsFloat();
            if (obj.has("staminaRegenRate"))      staminaRegenRate     = obj.get("staminaRegenRate").getAsFloat();
            if (obj.has("strengthJumpBothHands")) strengthJumpBothHands = obj.get("strengthJumpBothHands").getAsFloat();
            if (obj.has("strengthJumpOneHand"))   strengthJumpOneHand   = obj.get("strengthJumpOneHand").getAsFloat();
            if (obj.has("grabHitSound"))          grabHitSound          = obj.get("grabHitSound").getAsString();
            if (obj.has("grabJumpSound"))         grabJumpSound         = obj.get("grabJumpSound").getAsString();
            if (obj.has("idleHandPos")) {
                JsonObject hp = obj.getAsJsonObject("idleHandPos");
                idleHandPos = new Vec3(hp.get("x").getAsDouble(), hp.get("y").getAsDouble(), hp.get("z").getAsDouble());
            }
            excludeList.clear();
            if (obj.has("excludeList")) obj.getAsJsonArray("excludeList").forEach(e -> excludeList.add(e.getAsString()));
            includeList.clear();
            if (obj.has("includeList")) obj.getAsJsonArray("includeList").forEach(e -> includeList.add(e.getAsString()));
            ClimberMod.LOGGER.info("[Climber] Config loaded from {}", path);
        } catch (Exception e) {
            ClimberMod.LOGGER.error("[Climber] Failed to load config: {}", e.getMessage());
        }
    }
}