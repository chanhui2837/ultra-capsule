package com.partnercap.fluid;

import com.partnercap.PartnerCapMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FlowingFluid;

public class ModFluids {

	public static FlowingFluid VOID_STILL;
	public static FlowingFluid VOID_FLOWING;

	public static final TagKey<Fluid> VOID_TAG = TagKey.create(Registries.FLUID, PartnerCapMod.id("void_liquid"));

	public static void register() {
		VOID_STILL = Registry.register(BuiltInRegistries.FLUID,
				ResourceKey.create(Registries.FLUID, PartnerCapMod.id("void_liquid")), new VoidFluid.Still());
		VOID_FLOWING = Registry.register(BuiltInRegistries.FLUID,
				ResourceKey.create(Registries.FLUID, PartnerCapMod.id("flowing_void_liquid")), new VoidFluid.Flowing());
	}
}
