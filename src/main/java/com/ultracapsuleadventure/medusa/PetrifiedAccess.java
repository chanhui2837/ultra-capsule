package com.ultracapsuleadventure.medusa;

/**
 * LivingEntity 믹스인이 구현하는 석화 플래그 접근자 (서버·클라 공용, 자동 동기화).
 */
public interface PetrifiedAccess {
	boolean UltraCapsuleAdventure$isPetrified();

	void UltraCapsuleAdventure$setPetrified(boolean petrified);

	/**
	 * 석화 당시 저장해 둔 머리/몸통 방향을 엔티티에 다시 적용한다.
	 * 월드 재입장·청크 재로딩으로 클라 엔티티가 새로 생성되면 몸통 방향이 머리 방향으로 초기화되므로 필요하다.
	 */
	void UltraCapsuleAdventure$applyFrozenRotation();

	/** Frozen melee swing progress captured at petrification. */
	float UltraCapsuleAdventure$getFrozenAttackAnim();
}


