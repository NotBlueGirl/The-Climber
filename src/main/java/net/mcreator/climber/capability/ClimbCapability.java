package net.mcreator.climber.capability;

import net.mcreator.climber.ClimberConfig;
import net.mcreator.climber.ClimberMod;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityManager;
import net.minecraftforge.common.capabilities.CapabilityToken;
import net.minecraftforge.common.capabilities.ICapabilitySerializable;
import net.minecraftforge.common.capabilities.RegisterCapabilitiesEvent;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.eventbus.api.IEventBus;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ClimbCapability {

    public static final Capability<IClimbData> CLIMB_CAP = CapabilityManager.get(new CapabilityToken<>() {});
    public static final ResourceLocation ID = new ResourceLocation(ClimberMod.MODID, "climb_data");

    public static void register(IEventBus modBus) {
        modBus.addListener(ClimbCapability::onRegisterCapabilities);
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.addGenericListener(
                net.minecraft.world.entity.Entity.class,
                ClimbCapability::onAttachCapabilities
        );
    }

    private static void onRegisterCapabilities(RegisterCapabilitiesEvent event) {
        event.register(IClimbData.class);
    }

    private static void onAttachCapabilities(AttachCapabilitiesEvent<net.minecraft.world.entity.Entity> event) {
        if (event.getObject() instanceof Player) {
            event.addCapability(ID, new ClimbDataProvider());
        }
    }

    // ---- Interface ----

    public interface IClimbData {
        boolean isClimbEnabled();
        void setClimbEnabled(boolean enabled);

        boolean isRightHandGrabbing();
        void setRightHandGrabbing(boolean grabbing);

        boolean isLeftHandGrabbing();
        void setLeftHandGrabbing(boolean grabbing);

        @Nullable Vec3 getRightAnchor();
        void setRightAnchor(@Nullable Vec3 pos);

        @Nullable Vec3 getLeftAnchor();
        void setLeftAnchor(@Nullable Vec3 pos);

        // ---- Stamina ----
        float getRightStamina();
        void  setRightStamina(float v);

        float getLeftStamina();
        void  setLeftStamina(float v);

        boolean isRightExhausted();
        void    setRightExhausted(boolean v);

        boolean isLeftExhausted();
        void    setLeftExhausted(boolean v);

        void saveNBT(CompoundTag tag);
        void loadNBT(CompoundTag tag);
    }

    // ---- Implementation ----

    public static class ClimbData implements IClimbData {
        private boolean climbEnabled      = false;
        private boolean rightHandGrabbing = false;
        private boolean leftHandGrabbing  = false;
        @Nullable private Vec3 rightAnchor = null;
        @Nullable private Vec3 leftAnchor  = null;

        private float   rightStamina   = ClimberConfig.STAMINA_MAX;
        private float   leftStamina    = ClimberConfig.STAMINA_MAX;
        private boolean rightExhausted = false;
        private boolean leftExhausted  = false;

        @Override public boolean isClimbEnabled()               { return climbEnabled; }
        @Override public void setClimbEnabled(boolean v)        { climbEnabled = v; }

        @Override public boolean isRightHandGrabbing()          { return rightHandGrabbing; }
        @Override public void setRightHandGrabbing(boolean v)   { rightHandGrabbing = v; }

        @Override public boolean isLeftHandGrabbing()           { return leftHandGrabbing; }
        @Override public void setLeftHandGrabbing(boolean v)    { leftHandGrabbing = v; }

        @Override public @Nullable Vec3 getRightAnchor()        { return rightAnchor; }
        @Override public void setRightAnchor(@Nullable Vec3 v)  { rightAnchor = v; }

        @Override public @Nullable Vec3 getLeftAnchor()         { return leftAnchor; }
        @Override public void setLeftAnchor(@Nullable Vec3 v)   { leftAnchor = v; }

        @Override public float getRightStamina()                { return rightStamina; }
        @Override public void  setRightStamina(float v)         { rightStamina = Math.max(0, Math.min(ClimberConfig.STAMINA_MAX, v)); }

        @Override public float getLeftStamina()                 { return leftStamina; }
        @Override public void  setLeftStamina(float v)          { leftStamina  = Math.max(0, Math.min(ClimberConfig.STAMINA_MAX, v)); }

        @Override public boolean isRightExhausted()             { return rightExhausted; }
        @Override public void    setRightExhausted(boolean v)   { rightExhausted = v; }

        @Override public boolean isLeftExhausted()              { return leftExhausted; }
        @Override public void    setLeftExhausted(boolean v)    { leftExhausted = v; }

        @Override
        public void saveNBT(CompoundTag tag) {
            tag.putBoolean("climbEnabled", climbEnabled);
            tag.putBoolean("rightHandGrabbing", rightHandGrabbing);
            tag.putBoolean("leftHandGrabbing", leftHandGrabbing);
            saveVec(tag, "rightAnchor", rightAnchor);
            saveVec(tag, "leftAnchor",  leftAnchor);
            tag.putFloat("rightStamina",     rightStamina);
            tag.putFloat("leftStamina",      leftStamina);
            tag.putBoolean("rightExhausted", rightExhausted);
            tag.putBoolean("leftExhausted",  leftExhausted);
        }

        @Override
        public void loadNBT(CompoundTag tag) {
            climbEnabled      = tag.getBoolean("climbEnabled");
            rightHandGrabbing = tag.getBoolean("rightHandGrabbing");
            leftHandGrabbing  = tag.getBoolean("leftHandGrabbing");
            rightAnchor = loadVec(tag, "rightAnchor");
            leftAnchor  = loadVec(tag, "leftAnchor");
            rightStamina   = tag.contains("rightStamina")   ? tag.getFloat("rightStamina")   : ClimberConfig.STAMINA_MAX;
            leftStamina    = tag.contains("leftStamina")    ? tag.getFloat("leftStamina")    : ClimberConfig.STAMINA_MAX;
            rightExhausted = tag.contains("rightExhausted") && tag.getBoolean("rightExhausted");
            leftExhausted  = tag.contains("leftExhausted")  && tag.getBoolean("leftExhausted");
        }

        private static void saveVec(CompoundTag tag, String key, @Nullable Vec3 v) {
            if (v == null) return;
            CompoundTag sub = new CompoundTag();
            sub.putDouble("x", v.x); sub.putDouble("y", v.y); sub.putDouble("z", v.z);
            tag.put(key, sub);
        }

        @Nullable
        private static Vec3 loadVec(CompoundTag tag, String key) {
            if (!tag.contains(key)) return null;
            CompoundTag sub = tag.getCompound(key);
            return new Vec3(sub.getDouble("x"), sub.getDouble("y"), sub.getDouble("z"));
        }
    }

    // ---- Provider ----

    public static class ClimbDataProvider implements ICapabilitySerializable<CompoundTag> {
        private final ClimbData data = new ClimbData();
        private final LazyOptional<IClimbData> optional = LazyOptional.of(() -> data);

        @Override
        public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side) {
            return cap == CLIMB_CAP ? optional.cast() : LazyOptional.empty();
        }

        @Override public CompoundTag serializeNBT()          { CompoundTag t = new CompoundTag(); data.saveNBT(t); return t; }
        @Override public void deserializeNBT(CompoundTag t)  { data.loadNBT(t); }
    }

    // ---- Helper ----

    public static LazyOptional<IClimbData> get(Player player) {
        return player.getCapability(CLIMB_CAP);
    }
}
