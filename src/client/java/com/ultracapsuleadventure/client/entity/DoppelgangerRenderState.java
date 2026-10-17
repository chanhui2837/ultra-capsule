package com.ultracapsuleadventure.client.entity;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

public class DoppelgangerRenderState extends LivingEntityRenderState {
	/** 0=스토크(굽음), 1=매복(펼침) */
	public int phase;
	/** 근접 공격 스윙 진행도 (0~1) */
	public float attack;
}


