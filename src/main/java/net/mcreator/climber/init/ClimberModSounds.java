
/*
 *    MCreator note: This file will be REGENERATED on each build.
 */
package net.mcreator.climber.init;

import net.minecraftforge.registries.RegistryObject;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.DeferredRegister;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.resources.ResourceLocation;

import net.mcreator.climber.ClimberMod;

public class ClimberModSounds {
	public static final DeferredRegister<SoundEvent> REGISTRY = DeferredRegister.create(ForgeRegistries.SOUND_EVENTS, ClimberMod.MODID);
	public static final RegistryObject<SoundEvent> GRAB_HIT = REGISTRY.register("grab-hit", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("climber", "grab-hit")));
	public static final RegistryObject<SoundEvent> GRAB_JUMP = REGISTRY.register("grab-jump", () -> SoundEvent.createVariableRangeEvent(new ResourceLocation("climber", "grab-jump")));
}
