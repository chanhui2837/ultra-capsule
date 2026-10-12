package com.partnercap.mixin.client;

import com.partnercap.medusa.PetrifiedStateAccess;

import net.minecraft.client.renderer.entity.state.SheepRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 양모 레이어는 양모색 틴트를 곱한다. 염색된 양 석화 시 돌 양모가 물들지 않도록 흰색 강제.
 */
@Mixin(SheepRenderState.class)
public abstract class SheepWoolPetrifyMixin {

	@Inject(method = "getWoolColor", at = @At("HEAD"), cancellable = true)
	private void partnercap$whiteWool(CallbackInfoReturnable<Integer> cir) {
		try {
			if ((Object) this instanceof PetrifiedStateAccess access && access.partnercap$isPetrifiedState()) {
				cir.setReturnValue(-1);
			}
		} catch (Exception ignored) {
		}
	}
}
