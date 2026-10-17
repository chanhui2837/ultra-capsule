package com.ultracapsuleadventure.flame;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.SpawnEggItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * 화염몽 콘텐츠 등록: 엔티티+속성 / 심장·지팡이·피병·열관·스폰알.
 */
public class ModFlame {

	public static EntityType<FlameMongEntity> FLAME_MONG;

	public static FlameHeartItem FLAME_HEART;
	public static TranscendWandItem TRANSCEND_WAND;
	public static DragonBloodBottleItem DRAGON_BLOOD;
	public static Block YEOLGWAN_BLOCK;
	public static Item YEOLGWAN_ITEM;
	public static BlockEntityType<YeolgwanBlockEntity> YEOLGWAN_ENTITY;
	public static SpawnEggItem FLAME_MONG_EGG;

	private static ResourceKey<Item> itemKey(String name) {
		return ResourceKey.create(Registries.ITEM, UltraCapsuleAdventureMod.id(name));
	}

	public static void register() {
		// 엔티티
		try {
			ResourceKey<EntityType<?>> id = ResourceKey.create(Registries.ENTITY_TYPE, UltraCapsuleAdventureMod.id("flame_mong"));
			EntityType.Builder<FlameMongEntity> builder = EntityType.Builder
					.<FlameMongEntity>of(FlameMongEntity::new, MobCategory.CREATURE)
					.sized(0.9F, 1.6F);
			FLAME_MONG = Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(id));
		} catch (Exception ignored) {
		}
		try {
			net.fabricmc.fabric.api.object.builder.v1.entity.FabricDefaultAttributeRegistry.register(FLAME_MONG,
					FlameMongEntity.createAttributes());
		} catch (Exception ignored) {
		}
		// 심장
		try {
			ResourceKey<Item> k = itemKey("flame_heart");
			FLAME_HEART = new FlameHeartItem(new Item.Properties().stacksTo(16).setId(k));
			Registry.register(BuiltInRegistries.ITEM, k, FLAME_HEART);
		} catch (Exception ignored) {
		}
		// 초월의 막대
		try {
			ResourceKey<Item> k = itemKey("transcend_wand");
			TRANSCEND_WAND = new TranscendWandItem(new Item.Properties().stacksTo(1).setId(k));
			Registry.register(BuiltInRegistries.ITEM, k, TRANSCEND_WAND);
		} catch (Exception ignored) {
		}
		// 화염룡의 피 유리병
		try {
			ResourceKey<Item> k = itemKey("dragon_blood_bottle");
			DRAGON_BLOOD = new DragonBloodBottleItem(new Item.Properties().stacksTo(16).setId(k));
			Registry.register(BuiltInRegistries.ITEM, k, DRAGON_BLOOD);
		} catch (Exception ignored) {
		}
		// 열관 블록+블록엔티티+블록아이템
		try {
			ResourceKey<Block> bk = ResourceKey.create(Registries.BLOCK, UltraCapsuleAdventureMod.id("yeolgwan"));
			BlockBehaviour.Properties bp = BlockBehaviour.Properties.ofFullCopy(Blocks.FURNACE);
			try {
				bp = bp.strength(3.5F, 6.0F).requiresCorrectToolForDrops().sound(SoundType.STONE);
				bp.lightLevel(s -> 12);
			} catch (Exception ignored) {
			}
			try {
				bp.setId(bk);
			} catch (Exception ignored) {
			}
			YEOLGWAN_BLOCK = new YeolgwanBlock(bp);
			Registry.register(BuiltInRegistries.BLOCK, bk, YEOLGWAN_BLOCK);
		} catch (Exception ignored) {
		}
		try {
			ResourceKey<Item> k = itemKey("yeolgwan");
			YEOLGWAN_ITEM = new BlockItem(YEOLGWAN_BLOCK, new Item.Properties().setId(k));
			Registry.register(BuiltInRegistries.ITEM, k, YEOLGWAN_ITEM);
		} catch (Exception ignored) {
		}
		try {
			ResourceKey<BlockEntityType<?>> ek = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE,
					UltraCapsuleAdventureMod.id("yeolgwan"));
			YEOLGWAN_ENTITY = Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, ek,
					new BlockEntityType<>(YeolgwanBlockEntity::new, java.util.Set.of(YEOLGWAN_BLOCK)));
		} catch (Exception ignored) {
		}
		// 스폰알
		try {
			ResourceKey<Item> k = itemKey("flame_mong_spawn_egg");
			FLAME_MONG_EGG = new SpawnEggItem(new Item.Properties().spawnEgg(FLAME_MONG).setId(k));
			Registry.register(BuiltInRegistries.ITEM, k, FLAME_MONG_EGG);
		} catch (Exception ignored) {
		}
	}

	public static void addToTab(CreativeModeTab.Output output) {
		try {
			output.accept(FLAME_HEART);
			output.accept(TRANSCEND_WAND);
			output.accept(DRAGON_BLOOD);
			output.accept(YEOLGWAN_ITEM);
			output.accept(FLAME_MONG_EGG);
		} catch (Exception ignored) {
		}
	}
}


