package com.ultracapsuleadventure.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class GloomDevourerRenderState extends LivingEntityRenderState {
	public int devourPhase;
	public float devourTick;
	/** 단계별 전환 가중치 (0~1, 렌더러에서 개체별로 부드럽게 보간) */
	public float blend1;
	public float blend2;
	public float blend3;
	/** 근접 공격 스윙 진행도 (0~1) */
	public float attack;
}


