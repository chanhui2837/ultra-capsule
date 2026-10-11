package com.partnercap.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class MedusaRenderState extends LivingEntityRenderState {
	/** 0=평상시, >0=석화 섬광 */
	public int gazeFlash;
	/** 근접 공격 스윙 진행도 (0~1) */
	public float attack;
}
