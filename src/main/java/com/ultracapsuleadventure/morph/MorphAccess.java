package com.ultracapsuleadventure.morph;

/**
 * 플레이어 변신 상태.
 * 서버는 SERVER_MORPH 맵 권위, 클라는 브로드캐스트 패킷 캐시(MorphSyncCache).
 * (Player EntityData에는 빈 슬롯이 없어 EntityData 동기화 불가)
 */
public interface MorphAccess {
	String UltraCapsuleAdventure$getMorphId();

	void UltraCapsuleAdventure$setMorphId(String id);
}


