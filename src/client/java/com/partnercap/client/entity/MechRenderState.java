package com.partnercap.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class MechRenderState extends LivingEntityRenderState {
	/** 0=로봇, 1=완전 접힘(캡슐볼) */
	public float transform;
	/**
	 * 1인칭으로 직접 탑승 중 (로컬 플레이어 전용): true면 겉모습(머리·몸통·팔다리)을 숨기고
	 * 눈 위치 기준 조종석 내부({@link MechRobotModel} "cockpit" 파트)만 그린다. 밖에서 보는 시점에선 항상 false.
	 */
	public boolean cockpitView;
	/** 조종석 시점용: 탑승자의 실시간 시선 yaw/pitch(도). 몸통 회전은 20Hz 보간이라 지연되므로 카메라 값을 직접 쓴다. */
	public float camYaw;
	public float camPitch;
	/** 렌더링에 쓰이는 메카 몸통 yaw(도, 보간 적용) */
	public float bodyYaw;
}
