package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.mech.MechRiderAccess;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 메카 탑승자 숨김 플래그를 렌더 상태에 저장 (상태와 함께 제출 단계까지 이동).
 */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityStateRiderMixin implements MechRiderAccess {

	@Unique
	private boolean UltraCapsuleAdventure$hiddenRider = false;

	@Override
	public boolean UltraCapsuleAdventure$isHiddenRider() {
		return UltraCapsuleAdventure$hiddenRider;
	}

	@Override
	public void UltraCapsuleAdventure$setHiddenRider(boolean hidden) {
		UltraCapsuleAdventure$hiddenRider = hidden;
	}
}


