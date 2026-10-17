package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.medusa.PetrifiedStateAccess;

import net.minecraft.client.renderer.entity.state.EntityRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 석화된 상태에서는 발광 외곽선 없음 (소울 파란 테두리 등 제거).
 */
@Mixin(EntityRenderState.class)
public abstract class LivingEntityStateGlowMixin {

	@Inject(method = "appearsGlowing", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$noGlow(CallbackInfoReturnable<Boolean> cir) {
		try {
			if ((Object) this instanceof PetrifiedStateAccess access && access.UltraCapsuleAdventure$isPetrifiedState()) {
				cir.setReturnValue(false);
			}
		} catch (Exception ignored) {
		}
	}
}


