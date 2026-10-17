package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.medusa.PetrifiedStateAccess;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.layers.CatCollarLayer;
import net.minecraft.client.renderer.entity.state.CatRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 길들인 고양이 목줄도 석화 시 그리지 않는다 (빨간 틴트 목줄 방지).
 */
@Mixin(CatCollarLayer.class)
public abstract class CatCollarLayerPetrifyMixin {

	@Inject(method = "submit(Lcom/mojang/blaze3d/vertex/PoseStack;Lnet/minecraft/client/renderer/SubmitNodeCollector;ILnet/minecraft/client/renderer/entity/state/CatRenderState;FF)V", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$noCollarOnStatue(PoseStack pose, SubmitNodeCollector collector, int light,
			CatRenderState state, float f1, float f2, CallbackInfo ci) {
		try {
			if (state instanceof PetrifiedStateAccess access && access.UltraCapsuleAdventure$isPetrifiedState()) {
				ci.cancel();
			}
		} catch (Exception ignored) {
		}
	}
}


