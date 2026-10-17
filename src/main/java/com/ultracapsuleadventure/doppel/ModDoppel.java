package com.ultracapsuleadventure.doppel;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;
import com.ultracapsuleadventure.morph.MorphEvents;
import com.ultracapsuleadventure.morph.StealthCapsuleItem;
import com.ultracapsuleadventure.morph.TwilightSwordItem;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;

/**
 * 도플갱어/은신 콘텐츠 등록: 파란 점토·은신 캡슐·황혼의 검·스폰알 + 이벤트.
 */
public class ModDoppel {

	public static Item BLUE_CLAY;
	public static StealthCapsuleItem STEALTH_CAPSULE;
	public static TwilightSwordItem TWILIGHT_SWORD;
	public static net.minecraft.world.item.SpawnEggItem DOPPEL_SPAWN_EGG;

	public static final TagKey<Item> TWILIGHT_REPAIR = TagKey.create(Registries.ITEM,
			Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "twilight_repair"));

	public static final ToolMaterial TWILIGHT_MATERIAL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
			2031, 9.0F, 5.0F, 18, TWILIGHT_REPAIR);

	public static void register() {
		ResourceKey<Item> clayKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "blue_clay"));
		BLUE_CLAY = new Item(new Item.Properties().setId(clayKey));
		Registry.register(BuiltInRegistries.ITEM, clayKey, BLUE_CLAY);

		ResourceKey<Item> stealthKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "stealth_capsule"));
		STEALTH_CAPSULE = new StealthCapsuleItem(new Item.Properties().stacksTo(1).setId(stealthKey));
		Registry.register(BuiltInRegistries.ITEM, stealthKey, STEALTH_CAPSULE);

		ResourceKey<Item> twilightKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "twilight_sword"));
		Item.Properties twilightProps = new Item.Properties().stacksTo(1).setId(twilightKey);
		try {
			twilightProps = TWILIGHT_MATERIAL.applySwordProperties(twilightProps, 5.0F, -2.2F);
		} catch (Exception ignored) {
		}
		TWILIGHT_SWORD = new TwilightSwordItem(twilightProps);
		Registry.register(BuiltInRegistries.ITEM, twilightKey, TWILIGHT_SWORD);

		try {
			ResourceKey<Item> eggKey = ResourceKey.create(Registries.ITEM,
					Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "doppelganger_spawn_egg"));
			DOPPEL_SPAWN_EGG = new net.minecraft.world.item.SpawnEggItem(
					new Item.Properties().spawnEgg(com.ultracapsuleadventure.entity.ModEntityTypes.DOPPELGANGER).setId(eggKey));
			Registry.register(BuiltInRegistries.ITEM, eggKey, DOPPEL_SPAWN_EGG);
		} catch (Exception ignored) {
		}

		try {
			DoppelEvents.register();
		} catch (Exception ignored) {
		}
		try {
			MorphEvents.register();
		} catch (Exception ignored) {
		}
	}

	public static void addToTab(CreativeModeTab.Output output) {
		try {
			output.accept(BLUE_CLAY);
		} catch (Exception ignored) {
		}
		try {
			output.accept(STEALTH_CAPSULE);
		} catch (Exception ignored) {
		}
		try {
			output.accept(TWILIGHT_SWORD);
		} catch (Exception ignored) {
		}
		try {
			if (DOPPEL_SPAWN_EGG != null) {
				output.accept(DOPPEL_SPAWN_EGG);
			}
		} catch (Exception ignored) {
		}
	}
}


