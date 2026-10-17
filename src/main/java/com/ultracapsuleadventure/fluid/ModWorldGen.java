package com.ultracapsuleadventure.fluid;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;

import net.fabricmc.fabric.api.biome.v1.BiomeModifications;
import net.fabricmc.fabric.api.biome.v1.BiomeSelectors;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;

public class ModWorldGen {

	public static void register() {
		// 엔드 웅덩이 자연 생성은 제거됨 (먹보 도깨비가 죽을 때 검은 액체를 떨굼).
		// data/.../void_pool.json 은 미사용 상태로 남음.
		// 먹보 도깨비 엔드 자연 스폰 (없으면 공허 액체 수급 불가)
		try {
			net.fabricmc.fabric.api.biome.v1.BiomeModifications.addSpawn(
					BiomeSelectors.foundInTheEnd(),
					net.minecraft.world.entity.MobCategory.MONSTER,
					com.ultracapsuleadventure.entity.ModEntityTypes.GLOOM_DEVOURER,
					8, 1, 2);
		} catch (Exception ignored) {
		}
		try {
			net.minecraft.world.entity.SpawnPlacements.register(
					com.ultracapsuleadventure.entity.ModEntityTypes.GLOOM_DEVOURER,
					net.minecraft.world.entity.SpawnPlacementTypes.ON_GROUND,
					net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
					(type, level, reason, pos, random) -> {
						try {
							return level.getBlockState(pos.below())
									.is(net.minecraft.world.level.block.Blocks.END_STONE);
						} catch (Exception ignored) {
							return false;
						}
					});
		} catch (Exception ignored) {
		}
	}
}


