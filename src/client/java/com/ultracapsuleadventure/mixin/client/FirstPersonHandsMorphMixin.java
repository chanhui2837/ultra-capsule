package com.ultracapsuleadventure.mixin.client;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.FirstPersonHandsAndItemsRenderer;
import net.minecraft.client.renderer.state.level.FirstPersonHandsAndItemsRenderState;
import net.minecraft.client.renderer.state.level.PlayerRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 변신 중 1인칭 인간 손 숨김 (폴백 전용: 팔 교체 훅이 없을 때만 손·아이템 전체를 숨김).
 * 참고 모드는 변신체 손을 그려주지만, 매핑 없는 모델은 손을 지우고 끝낸다.
 * 여기서는 변신 중 바닐라 인간 손을 그리지 않는다 (F5·타인 시점은 더미 스왑이 담당).
 */
@Mixin(FirstPersonHandsAndItemsRenderer.class)
public abstract class FirstPersonHandsMorphMixin {

	@Inject(method = "submitHandsWithItems", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$hideHands(float partialTick, PoseStack poseStack,
			SubmitNodeCollector collector, PlayerRenderState playerState,
			FirstPersonHandsAndItemsRenderState handsState, CallbackInfo ci) {
		try {
			var mc = Minecraft.getInstance();
			if (mc.player == null) {
				return;
			}
			String mid = "";
			try {
				mid = com.ultracapsuleadventure.morph.MorphEvents.morphId(mc.player);
			} catch (Exception ignored) {
			}
			// 팔 교체 훅이 적용돼 있으면 취소하지 않는다 → 든 아이템은 바닐라가 그대로 그리고,
			// 빈손일 때의 팔은 AvatarHandMorphMixin 이 변신 몹의 팔로 대체한다.
			if (mid != null && !mid.isEmpty() && !com.ultracapsuleadventure.client.MorphClient.armHookApplied()) {
				ci.cancel();
			}
		} catch (Exception ignored) {
		}
	}
}


