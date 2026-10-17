package com.ultracapsuleadventure.morph;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;

/**
 * 변신 중 플레이어 히트박스/눈높이 계산.
 * Walkers의 DimensionsRefresher와 같은 역할: getDimensions를 가로채
 * 변신한 몹 크기로 반환하면 히트박스·카메라(눈높이)가 따라간다.
 */
public class MorphDims {

	private static final Map<String, EntityDimensions> CACHE = new HashMap<>();

	/** 몹 원본 크기 (특대형은 플레이 가능하도록 균등 축소) */
	public static EntityDimensions dimsFor(String typeId) {
		if (typeId == null || typeId.isEmpty()) {
			return null;
		}
		EntityDimensions cached = CACHE.get(typeId);
		if (cached != null) {
			return cached;
		}
		EntityDimensions d = null;
		try {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
					.get(Identifier.parse(typeId)).map(h -> h.value()).orElse(null);
			if (type != null) {
				d = type.getDimensions();
			}
		} catch (Exception ignored) {
		}
		if (d == null) {
			return null;
		}
		try {
			float f = 1.0f;
			if (d.width() > 3.0f) {
				f = Math.min(f, 3.0f / d.width());
			}
			if (d.height() > 4.0f) {
				f = Math.min(f, 4.0f / d.height());
			}
			if (f < 1.0f) {
				d = d.scale(f);
			}
		} catch (Exception ignored) {
		}
		if (CACHE.size() > 200) {
			CACHE.clear();
		}
		CACHE.put(typeId, d);
		return d;
	}
}


