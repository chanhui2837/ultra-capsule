package com.ultracapsuleadventure.client;

import net.minecraft.resources.Identifier;

/**
 * 지금 렌더 중인 생물이 석화 상태인지 렌더 스레드 플래그.
 * LivingEntityRenderer 믹스인이 extract(판정)→submit(사용 후 해제) 구간에만 켠다.
 * RenderTypes 믹스인은 이 값만 보고 텍스처를 돌로 바꿔치기한다.
 */
public class StoneSwap {

	public static volatile boolean ACTIVE = false;

	public static final Identifier STONE = Identifier.fromNamespaceAndPath("UltraCapsuleAdventure",
			"textures/entity/statue_stone.png");

	private static final java.util.Map<java.util.UUID, Long> SETTLE = new java.util.HashMap<>();

	/** 석화 시작 시각 (없으면 지금 기록). 다리 모으기 easing 기준점 */
	public static long settleStart(java.util.UUID id, long now) {
		if (id == null) {
			return now;
		}
		try {
			synchronized (SETTLE) {
				Long s = SETTLE.get(id);
				if (s == null) {
					SETTLE.put(id, now);
					if (SETTLE.size() > 500) {
						try {
							SETTLE.remove(SETTLE.keySet().iterator().next());
						} catch (Exception ignored) {
						}
					}
					return now;
				}
				return s;
			}
		} catch (Exception e) {
			return now;
		}
	}

	public static void settleRemove(java.util.UUID id) {
		if (id == null) {
			return;
		}
		try {
			synchronized (SETTLE) {
				SETTLE.remove(id);
			}
		} catch (Exception ignored) {
		}
	}
}


