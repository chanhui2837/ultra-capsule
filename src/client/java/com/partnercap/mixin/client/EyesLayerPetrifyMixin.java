package com.partnercap.mixin.client;

import com.partnercap.medusa.PetrifiedStateAccess;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 발광 눈(엔더맨/거미/팬텀/브리즈)은 RenderType을 클래스 로드 시 1회 만들어 캐시하므로
 * 텍스처 바꿔치기가 안 먹는다. 석화 시 눈 레이어 자체를 그리지 않는다.
 * (EnderEyesLayer/SpiderEyesLayer/PhantomEyesLayer/BreezeEyesLayer 공통 부모)
 */
@Mixin(EyesLayer.class)
public abstract class EyesLayerPetrifyMixin {

	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/EntityRenderState;FF)V", at = @At("HEAD"), cancellable = true)
	private void partnercap$noGlowEyes(PoseStack pose, SubmitNodeCollector collector, int light,
			EntityRenderState state, float f1, float f2, CallbackInfo ci) {
		try {
			if (state instanceof PetrifiedStateAccess access && access.partnercap$isPetrifiedState()) {
				ci.cancel();
			}
		} catch (Exception ignored) {
		}
	}
}
