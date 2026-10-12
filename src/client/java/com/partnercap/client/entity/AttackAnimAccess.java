package com.partnercap.client.entity;

import java.lang.reflect.Field;

import net.minecraft.world.entity.LivingEntity;

/**
 * 근접 공격 스윙 진행도(0~1) 읽기.
 * 26.3에서 getAttackAnim(float) 메서드가 없어 컴파일이 안 되므로, 필드(attackAnim/oAttackAnim)를 리플렉션으로 시도한다.
 * 필드도 없으면 0을 반환 (공격 스윙만 생략되고 나머지 애니메이션은 정상).
 */
final class AttackAnimAccess {

	private static boolean init;
	private static Field cur;
	private static Field old;

	private AttackAnimAccess() {
	}

	static float get(LivingEntity entity, float partialTick) {
		if (!init) {
			init = true;
			cur = find("attackAnim");
			old = find("oAttackAnim");
		}
		try {
			if (cur == null) {
				return 0f;
			}
			float c = cur.getFloat(entity);
			float o = old != null ? old.getFloat(entity) : c;
			return o + (c - o) * partialTick;
		} catch (Exception e) {
			return 0f;
		}
	}

	private static Field find(String name) {
		for (Class<?> k = LivingEntity.class; k != null; k = k.getSuperclass()) {
			try {
				Field f = k.getDeclaredField(name);
				if (f.getType() == float.class) {
					f.setAccessible(true);
					return f;
				}
			} catch (Exception ignored) {
			}
		}
		return null;
	}
}
