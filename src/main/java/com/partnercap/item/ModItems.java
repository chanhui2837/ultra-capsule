package com.partnercap.item;

import com.partnercap.PartnerCapMod;

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

	public static PartnerCapsuleItem PARTNER_CAPSULE;
	public static FlameSwordItem FLAME_SWORD;
	public static SoulCapsuleItem SOUL_CAPSULE;
	public static GambleCapsuleItem GAMBLE_CAPSULE;
	public static DisassembleCapsuleItem DISASSEMBLE_CAPSULE;
	public static net.minecraft.world.item.BucketItem VOID_BUCKET;
	public static VoidCapsuleItem VOID_CAPSULE;
	public static net.minecraft.world.item.SpawnEggItem GLOOM_SPAWN_EGG;
	public static com.partnercap.mech.MechCapsuleItem MECH_CAPSULE;
	public static com.partnercap.titan.TitanCapsuleItem TITAN_CAPSULE;
	public static com.partnercap.medusa.MedusaHeadItem MEDUSA_HEAD;
	public static net.minecraft.world.item.SpawnEggItem MEDUSA_SPAWN_EGG;
	public static com.partnercap.medusa.BlindfoldItem BLINDFOLD;

	public static final ResourceKey<CreativeModeTab> GROUP_KEY = ResourceKey.create(
			BuiltInRegistries.CREATIVE_MODE_TAB.key(),
			Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "main"));

	public static CreativeModeTab GROUP;

	/** 불길의 검 재질 (네더라이트급, 수리=블레이즈 막대) */
	public static final ToolMaterial FLAME_MATERIAL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_NETHERITE_TOOL,
			2031, 9.0F, 4.0F, 15,
			TagKey.create(Registries.ITEM, Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "flame_repair")));

	public static void register() {
		ResourceKey<Item> capsuleKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "partner_capsule"));
		PARTNER_CAPSULE = new PartnerCapsuleItem(new Item.Properties().stacksTo(1).setId(capsuleKey));
		Registry.register(BuiltInRegistries.ITEM, capsuleKey, PARTNER_CAPSULE);

		ResourceKey<Item> flameKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "flame_sword"));
		Item.Properties flameProps = new Item.Properties().stacksTo(1).setId(flameKey);
		try {
			flameProps = FLAME_MATERIAL.applySwordProperties(flameProps, 5.0F, -2.0F);
		} catch (Exception ignored) {
		}
		FLAME_SWORD = new FlameSwordItem(flameProps);
		Registry.register(BuiltInRegistries.ITEM, flameKey, FLAME_SWORD);

		ResourceKey<Item> soulKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "soul_capsule"));
		SOUL_CAPSULE = new SoulCapsuleItem(new Item.Properties().stacksTo(1)
				.component(DataComponents.ENCHANTABLE, new net.minecraft.world.item.enchantment.Enchantable(15))
				.setId(soulKey));
		Registry.register(BuiltInRegistries.ITEM, soulKey, SOUL_CAPSULE);

		ResourceKey<Item> gambleKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "gamble_capsule"));
		GAMBLE_CAPSULE = new GambleCapsuleItem(new Item.Properties().stacksTo(1)
				.component(DataComponents.ENCHANTABLE, new net.minecraft.world.item.enchantment.Enchantable(15))
				.setId(gambleKey));
		Registry.register(BuiltInRegistries.ITEM, gambleKey, GAMBLE_CAPSULE);

		ResourceKey<Item> disKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "disassemble_capsule"));
		DISASSEMBLE_CAPSULE = new DisassembleCapsuleItem(new Item.Properties().stacksTo(1).setId(disKey));
		Registry.register(BuiltInRegistries.ITEM, disKey, DISASSEMBLE_CAPSULE);

		ResourceKey<Item> voidBucketKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "void_bucket"));
		VOID_BUCKET = new net.minecraft.world.item.BucketItem(com.partnercap.fluid.ModFluids.VOID_STILL,
				new Item.Properties().craftRemainder(net.minecraft.world.item.Items.BUCKET).stacksTo(1).setId(voidBucketKey));
		Registry.register(BuiltInRegistries.ITEM, voidBucketKey, VOID_BUCKET);

		ResourceKey<Item> voidCapKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "void_capsule"));
		VOID_CAPSULE = new VoidCapsuleItem(new Item.Properties().stacksTo(1).setId(voidCapKey));
		Registry.register(BuiltInRegistries.ITEM, voidCapKey, VOID_CAPSULE);

		ResourceKey<Item> gloomEggKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "gloom_devourer_spawn_egg"));
		GLOOM_SPAWN_EGG = new net.minecraft.world.item.SpawnEggItem(
				new Item.Properties().spawnEgg(com.partnercap.entity.ModEntityTypes.GLOOM_DEVOURER).setId(gloomEggKey));
		Registry.register(BuiltInRegistries.ITEM, gloomEggKey, GLOOM_SPAWN_EGG);

		ResourceKey<Item> mechKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "mech_capsule"));
		MECH_CAPSULE = new com.partnercap.mech.MechCapsuleItem(new Item.Properties().stacksTo(1)
				.durability(com.partnercap.mech.MechCapsuleItem.MAX_DURABILITY).setId(mechKey));
		Registry.register(BuiltInRegistries.ITEM, mechKey, MECH_CAPSULE);

		ResourceKey<Item> titanKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "titan_capsule"));
		TITAN_CAPSULE = new com.partnercap.titan.TitanCapsuleItem(new Item.Properties().stacksTo(1)
				.component(DataComponents.ENCHANTABLE, new net.minecraft.world.item.enchantment.Enchantable(15))
				.setId(titanKey));
		Registry.register(BuiltInRegistries.ITEM, titanKey, TITAN_CAPSULE);

		ResourceKey<Item> medusaHeadKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "medusa_head"));
		MEDUSA_HEAD = new com.partnercap.medusa.MedusaHeadItem(new Item.Properties().stacksTo(1)
				.durability(com.partnercap.medusa.MedusaHeadItem.HEAD_MAX_DAMAGE).setId(medusaHeadKey));
		Registry.register(BuiltInRegistries.ITEM, medusaHeadKey, MEDUSA_HEAD);

		ResourceKey<Item> medusaEggKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "medusa_spawn_egg"));
		MEDUSA_SPAWN_EGG = new net.minecraft.world.item.SpawnEggItem(
				new Item.Properties().spawnEgg(com.partnercap.entity.ModEntityTypes.MEDUSA).setId(medusaEggKey));
		Registry.register(BuiltInRegistries.ITEM, medusaEggKey, MEDUSA_SPAWN_EGG);

		ResourceKey<Item> blindfoldKey = ResourceKey.create(Registries.ITEM,
				Identifier.fromNamespaceAndPath(PartnerCapMod.MOD_ID, "blindfold"));
		BLINDFOLD = new com.partnercap.medusa.BlindfoldItem(new Item.Properties().stacksTo(1)
				.equippable(net.minecraft.world.entity.EquipmentSlot.HEAD).setId(blindfoldKey));
		Registry.register(BuiltInRegistries.ITEM, blindfoldKey, BLINDFOLD);

		GROUP = FabricCreativeModeTab.builder()
				.icon(() -> new ItemStack(PARTNER_CAPSULE))
				.title(Component.translatable("itemGroup.partnercap.main"))
				.displayItems((params, output) -> {
					output.accept(PARTNER_CAPSULE);
					output.accept(FLAME_SWORD);
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
					com.partnercap.silicate.ModSilicate.addToTab(output);
				})
				.build();
		Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, GROUP_KEY, GROUP);
	}
}
