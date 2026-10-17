package com.ultracapsuleadventure.client.entity;

import net.minecraft.client.renderer.entity.state.ArmedEntityRenderState;

public class FlameMongRenderState extends ArmedEntityRenderState {
	public int mouthOpen = 0;
	public float climbAmount = 0f;
	public boolean climbing = false;
	/** 근접 공격 스윙 진행도 0~1 (AttackAnimAccess) */
	public float attack = 0f;
	/** 밤 수면 중 (누운 자세) */
	public boolean sleeping = false;
}


