package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.medusa.PetrifiedStateAccess;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 주민 주 모델(VILLAGER)은 모자 지오메트리(hat + hat_rim)를 항상 포함한다.
 * 평소엔 기본 텍스처의 모자 영역이 투명이라 안 보이지만,
 * 석화 시 돌 텍스처로 바꿔치기되면 불투명 돌모자가 생긴다 (무직 주민 석상 모자 버그).
 * 실제 모자(농부 짚모자 등)는 직업 레이어가 담당하므로 주 모델 모자는 석화 시 항상 숨긴다.
 * 주의: 26.x submit 파이프라인(visit→compile)은 visible 플래그를 읽지 않으므로
 * 스케일 0으로 찌그러뜨려 숨긴다. 모델은 렌더러 공용 싱글톤이라 매 프레임 양방향 갱신.
 * BabyVillagerModel은 VillagerModel을 상속하므로 함께 적용된다.
 */
@Mixin(VillagerModel.class)
public abstract class VillagerHatPetrifyMixin {

	@Unique
	private ModelPart UltraCapsuleAdventure$root;

	@Inject(method = "<init>(Lnet/minecraft/client/model/geom/ModelPart;)V", at = @At("RETURN"))
	private void UltraCapsuleAdventure$captureRoot(ModelPart root, CallbackInfo ci) {
		try {
			UltraCapsuleAdventure$root = root;
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;)V", at = @At("TAIL"))
	private void UltraCapsuleAdventure$hideHatOnStatue(VillagerRenderState state, CallbackInfo ci) {
		try {
			boolean p = state instanceof PetrifiedStateAccess access && access.UltraCapsuleAdventure$isPetrifiedState();
			ModelPart root = UltraCapsuleAdventure$root;
			if (root == null) {
				return;
			}
			ModelPart hat = null;
			try {
				if (root.hasChild("hat")) {
					hat = root.getChild("hat");
				}
			} catch (Exception ignored) {
			}
			if (hat != null) {
				hat.visible = !p;
				// submit 파이프라인은 visible을 무시하므로 스케일 0으로 함께 숨긴다
				float s = p ? 0f : 1f;
				try {
					hat.xScale = s;
					hat.yScale = s;
					hat.zScale = s;
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}
}


