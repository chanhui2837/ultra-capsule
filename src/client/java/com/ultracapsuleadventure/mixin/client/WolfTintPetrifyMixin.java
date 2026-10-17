package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.medusa.PetrifiedStateAccess;

import net.minecraft.client.renderer.entity.WolfRenderer;
import net.minecraft.client.renderer.entity.state.WolfRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 늑대 본체 틴트는 wetShade 기반이라 젖은 채 석화되면 어둡게 굳는다. 석화 시 흰색 강제.
 */
@Mixin(WolfRenderer.class)
public abstract class WolfTintPetrifyMixin {

	@Inject(method = "getModelTint(Lnet/minecraft/client/renderer/entity/state/WolfRenderState;)I", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$whiteTint(WolfRenderState state, CallbackInfoReturnable<Integer> cir) {
		try {
			if (state instanceof PetrifiedStateAccess access && access.UltraCapsuleAdventure$isPetrifiedState()) {
				cir.setReturnValue(-1);
			}
		} catch (Exception ignored) {
		}
	}
}


