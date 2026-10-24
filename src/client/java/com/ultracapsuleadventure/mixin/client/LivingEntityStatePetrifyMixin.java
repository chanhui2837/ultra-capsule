package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.medusa.PetrifiedStateAccess;

import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

/**
 * 석화 플래그를 렌더 상태에 저장 (상태와 함께 제출 단계까지 이동).
 */
@Mixin(LivingEntityRenderState.class)
public abstract class LivingEntityStatePetrifyMixin implements PetrifiedStateAccess {

	@Unique
	private boolean UltraCapsuleAdventure$petrifiedState = false;

	@Unique
	private java.util.UUID UltraCapsuleAdventure$renderEntityUuid;

	@Override
	public boolean UltraCapsuleAdventure$isPetrifiedState() {
		return UltraCapsuleAdventure$petrifiedState;
	}

	@Override
	public void UltraCapsuleAdventure$setPetrifiedState(boolean petrified) {
		UltraCapsuleAdventure$petrifiedState = petrified;
	}

	@Override
	public java.util.UUID UltraCapsuleAdventure$getRenderEntityUuid() {
		return UltraCapsuleAdventure$renderEntityUuid;
	}

	@Override
	public void UltraCapsuleAdventure$setRenderEntityUuid(java.util.UUID uuid) {
		UltraCapsuleAdventure$renderEntityUuid = uuid;
	}
}


