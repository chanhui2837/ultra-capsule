package com.ultracapsuleadventure.mech;

/**
 * 메카 탑승자 렌더 숨김 플래그 (extract→submit 전달용).
 * 서버 setInvisible로는 갑옷·손·망토 레이어가 그대로 보이므로, 클라에서 통째로 스킵한다.
 */
public interface MechRiderAccess {
	boolean UltraCapsuleAdventure$isHiddenRider();

	void UltraCapsuleAdventure$setHiddenRider(boolean hidden);
}


