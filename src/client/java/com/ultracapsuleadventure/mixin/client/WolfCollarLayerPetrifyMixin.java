package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.medusa.PetrifiedStateAccess;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.WolfCollarLayer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 길들인 늑대의 목줄 레이어는 전신 늑대 모델을 목줄 텍스처+빨간 틴트로 통째로 다시 그린다.
 * 석화 시 돌 텍스처로 바꿔치기되면 불투명 돌 전신이 빨갛게 칠해져서 야생 늑대와 달리 빨갛게 보인다.
 * 석상에는 목줄을 그리지 않는다 (submit 취소).
 */
@Mixin(WolfCollarLayer.class)
public abstract class WolfCollarLayerPetrifyMixin {

	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/WolfRenderState;FF)V", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$noCollarOnStatue(PoseStack pose, SubmitNodeCollector collector, int light,
			WolfRenderState state, float f1, float f2, CallbackInfo ci) {
		try {
			if (state instanceof PetrifiedStateAccess access && access.UltraCapsuleAdventure$isPetrifiedState()) {
				ci.cancel();
			}
		} catch (Exception ignored) {
		}
	}
}


