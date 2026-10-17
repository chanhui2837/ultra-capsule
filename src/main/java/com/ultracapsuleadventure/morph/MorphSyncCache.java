package com.ultracapsuleadventure.morph;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * 모프 ID 클라 캐시 (서버→클라 명시적 패킷으로 동기화).
 * EntityData만 믿지 않는 이유: 로컬 플레이어 반영 타이밍·추적 상태에 따라
 * 클라가 못 받는 경우가 있어 렌더 스왑이 침묵 실패할 수 있다.
 * (Walkers의 PlayerShape.sync 패킷과 같은 역할)
 */
public class MorphSyncCache {

	private static final Map<UUID, String> CACHE = new HashMap<>();

	public static void set(UUID playerId, String morphId) {
		try {
			if (morphId == null || morphId.isEmpty()) {
				CACHE.remove(playerId);
			} else {
				if (CACHE.size() > 500) {
					CACHE.clear();
				}
				CACHE.put(playerId, morphId);
			}
		} catch (Exception ignored) {
		}
	}

	public static String get(UUID playerId) {
		try {
			String v = CACHE.get(playerId);
			return v == null ? "" : v;
		} catch (Exception e) {
			return "";
		}
	}

	public static void clear() {
		try {
			CACHE.clear();
		} catch (Exception ignored) {
		}
	}
}


