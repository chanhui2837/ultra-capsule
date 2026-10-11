package com.partnercap.mixin.client;

import com.partnercap.medusa.PetrifiedStateAccess;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 석화 플래그를 렌더 상태에 저장 (상태와 함께 제출 단계까지 이동).
 */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityStatePetrifyMixin implements PetrifiedStateAccess {

	@Unique
	private boolean partnercap$petrifiedState = false;

	@Override
	public boolean partnercap$isPetrifiedState() {
		return partnercap$petrifiedState;
	}

	@Override
	public void partnercap$setPetrifiedState(boolean petrified) {
		partnercap$petrifiedState = petrified;
	}
}
