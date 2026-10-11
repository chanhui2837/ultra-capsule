package com.partnercap.medusa;

/**
 * 석화 플래그를 렌더 상태에 싣기 위한 접근자 (순서 무관, 상태 객체와 함께 이동).
 */
public interface PetrifiedStateAccess {
	boolean partnercap$isPetrifiedState();

	void partnercap$setPetrifiedState(boolean petrified);
}
