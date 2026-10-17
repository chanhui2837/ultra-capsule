package com.ultracapsuleadventure.medusa;

/**
 * LivingEntity 믹스인이 구현하는 석화 플래그 접근자 (서버·클라 공용, 자동 동기화).
 */
public interface PetrifiedAccess {
	boolean UltraCapsuleAdventure$isPetrified();

	void UltraCapsuleAdventure$setPetrified(boolean petrified);
}


