package com.partnercap.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class MechRenderState extends LivingEntityRenderState {
	/** 0=로봇, 1=완전 접힘(캡슐볼) */
	public float transform;
	/** 1인칭 탑승 중: 카메라가 머리 안에 있어 머리 파트 숨김 (바닐라 1인칭 머리 숨김과 동일) */
	public boolean hideHead;
}
