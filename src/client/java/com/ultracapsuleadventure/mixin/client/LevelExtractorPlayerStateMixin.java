package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.client.MorphClient;

import net.minecraft.client.renderer.extract.LevelExtractor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 26.3 은 로컬 플레이어를 extractPlayerState 에서 따로 추출하고 AvatarRenderState 가 아니면 예외를 던진다.
 * 그 구간에서는 변신 렌더 스왑을 끄기 위한 플래그만 켠다 (F5/타인 시점 렌더는 그대로 스왑).
 */
@Mixin(LevelExtractor.class)
public abstract class LevelExtractorPlayerStateMixin {

	@Inject(method = "extractPlayerState", at = @At("HEAD"))
	private void UltraCapsuleAdventure$beginPlayerState(CallbackInfo ci) {
		MorphClient.extractingPlayerState = true;
	}

	@Inject(method = "extractPlayerState", at = @At("RETURN"))
	private void UltraCapsuleAdventure$endPlayerState(CallbackInfo ci) {
		MorphClient.extractingPlayerState = false;
	}
}


