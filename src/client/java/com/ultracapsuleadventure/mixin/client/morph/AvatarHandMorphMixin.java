package com.ultracapsuleadventure.mixin.client.morph;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ultracapsuleadventure.client.MorphArmRenderer;
import com.ultracapsuleadventure.morph.MorphEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 1인칭 사람 팔 → 변신 몹의 팔로 교체.
 * 이 믹스인은 required:false 설정 파일에 있어서, 26.3 시그니처가 다르면 적용만 건너뛰고
 * (FirstPersonHandsMorphMixin 이 예전처럼 손을 통째로 숨기는 폴백으로 동작) 게임은 크래시하지 않는다.
 */
@Mixin(targets = "net.minecraft.client.renderer.entity.player.AvatarRenderer")
public abstract class AvatarHandMorphMixin {

	@Inject(method = "renderRightHand", at = @At("HEAD"), cancellable = true, require = 0)
	private void UltraCapsuleAdventure$rightHand(PoseStack pose, SubmitNodeCollector collector, int light,
			Identifier skin, boolean sleeve, CallbackInfo ci) {
		UltraCapsuleAdventure$swapHand(pose, collector, light, true, ci);
	}

	@Inject(method = "renderLeftHand", at = @At("HEAD"), cancellable = true, require = 0)
	private void UltraCapsuleAdventure$leftHand(PoseStack pose, SubmitNodeCollector collector, int light,
			Identifier skin, boolean sleeve, CallbackInfo ci) {
		UltraCapsuleAdventure$swapHand(pose, collector, light, false, ci);
	}

	private static void UltraCapsuleAdventure$swapHand(PoseStack pose, SubmitNodeCollector collector, int light,
			boolean right, CallbackInfo ci) {
		try {
			var mc = Minecraft.getInstance();
			if (mc.player == null) {
				return;
			}
			String mid = MorphEvents.morphId(mc.player);
			if (mid == null || mid.isEmpty()) {
				return;
			}
			MorphArmRenderer.draw(pose, collector, light, right);
			ci.cancel();
		} catch (Exception ignored) {
		}
	}
}


