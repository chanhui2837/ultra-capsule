package com.partnercap.silicate;

import com.partnercap.PartnerCapMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ToolMaterial;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 규산 콘텐츠 등록: 규산 블록(운석) → 규산 파편(3개 드롭) → 규산 주괴(파편 9개) → 도구/갑옷.
 * 성능: 다이아몬드 &lt; 규산 &lt; 네더라이트
 *  - 도구: 내구 1800(1561/2031), 채굴속도 8.5(8/9), 공격 보너스 3.5(3/4), 인챈트 12(10/15)
 *  - 방어구: 방어력은 다이아·네더라이트와 동일(3/8/6/3), 내구 배수 35(33/37), 강인함 2.5(2/3), 밀치기 저항 0.05(0/0.1)
 */
public class ModSilicate {

	public static Block SILICATE_BLOCK;
	public static Item SILICATE_BLOCK_ITEM;
	public static Item SILICATE_SHARD;
	public static Item SILICATE_INGOT;
	public static Item PICKAXE;
	public static Item AXE;
	public static Item SWORD;
	public static Item SHOVEL;
	public static Item HOE;
	public static Item HELMET;
	public static Item CHESTPLATE;
	public static Item LEGGINGS;
	public static Item BOOTS;

	/** 수리 재료 태그 (data/partnercap/tags/item/silicate_repair.json = 규산 주괴) */
	public static final TagKey<Item> REPAIR = TagKey.create(Registries.ITEM, PartnerCapMod.id("silicate_repair"));

	public static final ToolMaterial MATERIAL = new ToolMaterial(
			BlockTags.INCORRECT_FOR_DIAMOND_TOOL, 1800, 8.5F, 3.5F, 12, REPAIR);

	private static ResourceKey<Item> itemKey(String name) {
		return ResourceKey.create(Registries.ITEM, PartnerCapMod.id(name));
	}

	private static Item register(String name, java.util.function.Function<Item.Properties, Item> factory, boolean stack1) {
		ResourceKey<Item> key = itemKey(name);
		Item.Properties props = new Item.Properties();
		if (stack1) {
			props = props.stacksTo(1);
		}
		props = props.setId(key);
		Item item = factory.apply(props);
		Registry.register(BuiltInRegistries.ITEM, key, item);
		return item;
	}

	public static void register() {
		// 블록 (다이아 곡괭이 이상만 채굴: 태그는 data/minecraft/tags/block/needs_diamond_tool.json)
		ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, PartnerCapMod.id("silicate_block"));
		BlockBehaviour.Properties bp = BlockBehaviour.Properties.ofFullCopy(Blocks.OBSIDIAN);
		try {
			bp = bp.strength(8.0F, 30.0F).requiresCorrectToolForDrops().sound(SoundType.AMETHYST);
		} catch (Exception ignored) {
		}
		try {
			bp.setId(blockKey);
		} catch (Exception ignored) {
		}
		SILICATE_BLOCK = new SilicateBlock(bp);
		Registry.register(BuiltInRegistries.BLOCK, blockKey, SILICATE_BLOCK);
		ResourceKey<Item> blockItemKey = itemKey("silicate_block");
		SILICATE_BLOCK_ITEM = new BlockItem(SILICATE_BLOCK, new Item.Properties().setId(blockItemKey));
		Registry.register(BuiltInRegistries.ITEM, blockItemKey, SILICATE_BLOCK_ITEM);

		SILICATE_SHARD = register("silicate_shard", SilicateShardItem::new, false);
		SILICATE_INGOT = register("silicate_ingot", Item::new, false);

		PICKAXE = register("silicate_pickaxe", p -> SilicateGear.tool("pickaxe", MATERIAL, p), true);
		AXE = register("silicate_axe", p -> SilicateGear.tool("axe", MATERIAL, p), true);
		SWORD = register("silicate_sword", p -> SilicateGear.tool("sword", MATERIAL, p), true);
		SHOVEL = register("silicate_shovel", p -> SilicateGear.tool("shovel", MATERIAL, p), true);
		HOE = register("silicate_hoe", p -> SilicateGear.tool("hoe", MATERIAL, p), true);

		HELMET = register("silicate_helmet", p -> SilicateGear.armor("HELMET", p), true);
		CHESTPLATE = register("silicate_chestplate", p -> SilicateGear.armor("CHESTPLATE", p), true);
		LEGGINGS = register("silicate_leggings", p -> SilicateGear.armor("LEGGINGS", p), true);
		BOOTS = register("silicate_boots", p -> SilicateGear.armor("BOOTS", p), true);
	}

	/** 크리에이티브 탭에 추가 */
	public static void addToTab(CreativeModeTab.Output output) {
		output.accept(SILICATE_BLOCK_ITEM);
		output.accept(SILICATE_SHARD);
		output.accept(SILICATE_INGOT);
		output.accept(PICKAXE);
		output.accept(AXE);
		output.accept(SHOVEL);
		output.accept(HOE);
		output.accept(SWORD);
		output.accept(HELMET);
		output.accept(CHESTPLATE);
		output.accept(LEGGINGS);
		output.accept(BOOTS);
	}
}
