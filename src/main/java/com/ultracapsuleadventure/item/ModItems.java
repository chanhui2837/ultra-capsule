package com.ultracapsuleadventure.item;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;

import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;

public class ModItems {

	public static UltraCapsuleAdventureItem ultra_capsule_adventure;
	public static FlameSwordItem FLAME_SWORD;
	public static Item SOUL_SHARD;
	public static Item SOUL_INGOT;
	public static SoulSwordItem SOUL_SWORD;
	public static SoulCapsuleItem SOUL_CAPSULE;
	public static GambleCapsuleItem GAMBLE_CAPSULE;
	public static DisassembleCapsuleItem DISASSEMBLE_CAPSULE;
	public static net.minecraft.world.item.BucketItem VOID_BUCKET;
	public static VoidCapsuleItem VOID_CAPSULE;
	public static net.minecraft.world.item.SpawnEggItem GLOOM_SPAWN_EGG;
	public static com.ultracapsuleadventure.mech.MechCapsuleItem MECH_CAPSULE;
	public static com.ultracapsuleadventure.titan.TitanCapsuleItem TITAN_CAPSULE;
	public static com.ultracapsuleadventure.medusa.MedusaHeadItem MEDUSA_HEAD;
	public static net.minecraft.world.item.SpawnEggItem MEDUSA_SPAWN_EGG;
	public static com.ultracapsuleadventure.medusa.BlindfoldItem BLINDFOLD;

	public static final ResourceKey<CreativeModeTab> GROUP_KEY = ResourceKey.create(
			BuiltInRegistries.CREATIVE_MODE_TAB.key(),
			Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "main"));

	public static CreativeModeTab GROUP;

	/** 불길의 검 재질 (네더라이트급, 수리=블레이즈 막대) */
	public static final ToolMaterial FLAME_MATERIAL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
			2031, 9.0F, 4.0F, 15,
			TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "flame_repair")));

	public static void register() {
		ResourceKey<Item> capsuleKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "ultra_capsule_adventure"));
		ultra_capsule_adventure = new UltraCapsuleAdventureItem(new Item.Properties().stacksTo(1).setId(capsuleKey));
		Registry.register(BuiltInRegistries.ITEM, capsuleKey, ultra_capsule_adventure);

		ResourceKey<Item> flameKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "flame_sword"));
		Item.Properties flameProps = new Item.Properties().stacksTo(1).setId(flameKey);
		try {
			flameProps = FLAME_MATERIAL.applySwordProperties(flameProps, 5.0F, -2.0F);
		} catch (Exception ignored) {
		}
		FLAME_SWORD = new FlameSwordItem(flameProps);
		Registry.register(BuiltInRegistries.ITEM, flameKey, FLAME_SWORD);

		ResourceKey<Item> soulShardKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "soul_shard"));
		SOUL_SHARD = new Item(new Item.Properties().stacksTo(64).setId(soulShardKey));
		Registry.register(BuiltInRegistries.ITEM, soulShardKey, SOUL_SHARD);

		ResourceKey<Item> soulIngotKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "soul_ingot"));
		SOUL_INGOT = new Item(new Item.Properties().stacksTo(64).setId(soulIngotKey));
		Registry.register(BuiltInRegistries.ITEM, soulIngotKey, SOUL_INGOT);

		ResourceKey<Item> soulSwordKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "soul_sword"));
		Item.Properties soulSwordProps = new Item.Properties().stacksTo(1).setId(soulSwordKey);
		soulSwordProps = FLAME_MATERIAL.applySwordProperties(soulSwordProps, 6.0F, -2.4F);
		SOUL_SWORD = new SoulSwordItem(soulSwordProps);
		Registry.register(BuiltInRegistries.ITEM, soulSwordKey, SOUL_SWORD);
		ResourceKey<Item> soulKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "soul_capsule"));
		SOUL_CAPSULE = new SoulCapsuleItem(new Item.Properties().stacksTo(1)
				.component(DataComponents.ENCHANTABLE, new net.minecraft.world.item.enchantment.Enchantable(15))
				.setId(soulKey));
		Registry.register(BuiltInRegistries.ITEM, soulKey, SOUL_CAPSULE);

		ResourceKey<Item> gambleKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "gamble_capsule"));
		GAMBLE_CAPSULE = new GambleCapsuleItem(new Item.Properties().stacksTo(1)
				.component(DataComponents.ENCHANTABLE, new net.minecraft.world.item.enchantment.Enchantable(15))
				.setId(gambleKey));
		Registry.register(BuiltInRegistries.ITEM, gambleKey, GAMBLE_CAPSULE);

		ResourceKey<Item> disKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "disassemble_capsule"));
		DISASSEMBLE_CAPSULE = new DisassembleCapsuleItem(new Item.Properties().stacksTo(1).setId(disKey));
		Registry.register(BuiltInRegistries.ITEM, disKey, DISASSEMBLE_CAPSULE);

		ResourceKey<Item> voidBucketKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "void_bucket"));
		VOID_BUCKET = new net.minecraft.world.item.BucketItem(com.ultracapsuleadventure.fluid.ModFluids.VOID_STILL,
				new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1).setId(voidBucketKey));
		Registry.register(BuiltInRegistries.ITEM, voidBucketKey, VOID_BUCKET);

		ResourceKey<Item> voidCapKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "void_capsule"));
		VOID_CAPSULE = new VoidCapsuleItem(new Item.Properties().stacksTo(1).setId(voidCapKey));
		Registry.register(BuiltInRegistries.ITEM, voidCapKey, VOID_CAPSULE);

		ResourceKey<Item> gloomEggKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "gloom_devourer_spawn_egg"));
		GLOOM_SPAWN_EGG = new net.minecraft.world.item.SpawnEggItem(
				new Item.Properties().spawnEgg(com.ultracapsuleadventure.entity.ModEntityTypes.GLOOM_DEVOURER).setId(gloomEggKey));
		Registry.register(BuiltInRegistries.ITEM, gloomEggKey, GLOOM_SPAWN_EGG);

		ResourceKey<Item> mechKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "mech_capsule"));
		MECH_CAPSULE = new com.ultracapsuleadventure.mech.MechCapsuleItem(new Item.Properties().stacksTo(1)
				.durability(com.ultracapsuleadventure.mech.MechCapsuleItem.MAX_DURABILITY).setId(mechKey));
		Registry.register(BuiltInRegistries.ITEM, mechKey, MECH_CAPSULE);

		ResourceKey<Item> titanKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "titan_capsule"));
		TITAN_CAPSULE = new com.ultracapsuleadventure.titan.TitanCapsuleItem(new Item.Properties().stacksTo(1)
				.component(DataComponents.ENCHANTABLE, new net.minecraft.world.item.enchantment.Enchantable(15))
				.setId(titanKey));
		Registry.register(BuiltInRegistries.ITEM, titanKey, TITAN_CAPSULE);

		ResourceKey<Item> medusaHeadKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "medusa_head"));
		MEDUSA_HEAD = new com.ultracapsuleadventure.medusa.MedusaHeadItem(new Item.Properties().stacksTo(1)
				.durability(com.ultracapsuleadventure.medusa.MedusaHeadItem.HEAD_MAX_DAMAGE).setId(medusaHeadKey));
		Registry.register(BuiltInRegistries.ITEM, medusaHeadKey, MEDUSA_HEAD);

		ResourceKey<Item> medusaEggKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "medusa_spawn_egg"));
		MEDUSA_SPAWN_EGG = new net.minecraft.world.item.SpawnEggItem(
				new Item.Properties().spawnEgg(com.ultracapsuleadventure.entity.ModEntityTypes.MEDUSA).setId(medusaEggKey));
		Registry.register(BuiltInRegistries.ITEM, medusaEggKey, MEDUSA_SPAWN_EGG);

		ResourceKey<Item> blindfoldKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "blindfold"));
		BLINDFOLD = new com.ultracapsuleadventure.medusa.BlindfoldItem(new Item.Properties().stacksTo(1)
				.equippable(net.minecraft.world.entity.EquipmentSlot.HEAD).setId(blindfoldKey));
		Registry.register(BuiltInRegistries.ITEM, blindfoldKey, BLINDFOLD);

		GROUP = FabricCreativeModeTab.builder()
				.icon(() -> new ItemStack(ultra_capsule_adventure))
				.title(Component.translatable("itemGroup.UltraCapsuleAdventure.main"))
				.displayItems((params, output) -> {
					output.accept(ultra_capsule_adventure);
				output.accept(FLAME_SWORD);
				output.accept(SOUL_SHARD);
				output.accept(SOUL_INGOT);
				output.accept(SOUL_SWORD);
				output.accept(SOUL_CAPSULE);
					output.accept(GAMBLE_CAPSULE);
					output.accept(DISASSEMBLE_CAPSULE);
					output.accept(VOID_BUCKET);
					output.accept(VOID_CAPSULE);
					output.accept(GLOOM_SPAWN_EGG);
					output.accept(MECH_CAPSULE);
					output.accept(TITAN_CAPSULE);
					output.accept(MEDUSA_HEAD);
					output.accept(MEDUSA_SPAWN_EGG);
				output.accept(BLINDFOLD);
				com.ultracapsuleadventure.silicate.ModSilicate.addToTab(output);
				com.ultracapsuleadventure.flame.ModFlame.addToTab(output);
				com.ultracapsuleadventure.doppel.ModDoppel.addToTab(output);
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, GROUP_KEY, GROUP);
	}
}



