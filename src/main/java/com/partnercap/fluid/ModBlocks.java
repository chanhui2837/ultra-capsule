package com.partnercap.fluid;

import com.partnercap.PartnerCapMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;

public class ModBlocks {

	public static Block VOID_LIQUID;

	public static void register() {
		ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, PartnerCapMod.id("void_liquid"));
		BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(Blocks.WATER);
		try {
			props.setId(key);
		} catch (Exception ignored) {
		}
		VOID_LIQUID = new LiquidBlock(ModFluids.VOID_STILL, props);
		Registry.register(BuiltInRegistries.BLOCK, key, VOID_LIQUID);
	}
}
