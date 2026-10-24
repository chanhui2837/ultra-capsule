package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.client.StoneSwap;
import com.ultracapsuleadventure.medusa.PetrifiedAccess;
import com.ultracapsuleadventure.medusa.PetrifiedStateAccess;

import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

import com.mojang.blaze3d.vertex.PoseStack;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 석화 플래그를 상태 객체에 실어 나름 (추출→제출 순서와 무관하게 정확).
 * 몸통은 getRenderType에서 직접 돌로 교체. 갑옷·발광 레이어는 팩토리 스왑이 담당.
 */
@Mixin(LivingEntityRenderer.class)
public abstract class LivingEntityRendererPetrifyMixin {

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("HEAD"))
	private void UltraCapsuleAdventure$restoreFrozenRotation(LivingEntity entity, LivingEntityRenderState state,
			float tickProgress, CallbackInfo ci) {
		// 월드 재입장 시 클라 엔티티의 몸통 방향이 머리 방향으로 초기화되는 문제 방지:
		// 렌더 상태 추출 전에 석화 당시의 머리/몸통 방향을 항상 복원한다.
		try {
			if (entity instanceof PetrifiedAccess access && access.UltraCapsuleAdventure$isPetrified()) {
				access.UltraCapsuleAdventure$applyFrozenRotation();
			}
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
	private void UltraCapsuleAdventure$markPetrified(LivingEntity entity, LivingEntityRenderState state, float tickProgress,
			CallbackInfo ci) {
		// Keep a stable entity key: render-state objects may be reused for different entities/frames.
		try {
			if (state instanceof PetrifiedStateAccess access) {
				access.UltraCapsuleAdventure$setRenderEntityUuid(entity.getUUID());
			}
		} catch (Exception ignored) {
		}
		boolean p = false;
		try {
			p = entity instanceof PetrifiedAccess access && access.UltraCapsuleAdventure$isPetrified();
		} catch (Exception ignored) {
		}
		// 메카 탑승자는 몸·갑옷·손·망토·그림자 통째로 숨김 (서버 setInvisible로는 레이어가 남음)
		boolean hidden = false;
		try {
			hidden = entity.getVehicle() instanceof com.ultracapsuleadventure.mech.MechRobotEntity;
		} catch (Exception ignored) {
		}
		try {
			if (state instanceof com.ultracapsuleadventure.mech.MechRiderAccess access) {
				access.UltraCapsuleAdventure$setHiddenRider(hidden);
			}
		} catch (Exception ignored) {
		}
		if (hidden) {
			try {
				state.shadowPieces.clear();
			} catch (Exception ignored) {
			}
		}
		if (!p) {
			try {
				com.ultracapsuleadventure.client.StoneSwap.settleRemove(entity.getUUID());
			} catch (Exception ignored) {
			}
			// 상태 객체 풀링 대비: 이전에 석화로 쓰던 state 재사용 시 플래그 오염 방지
			try {
				if (state instanceof PetrifiedStateAccess access) {
					access.UltraCapsuleAdventure$setPetrifiedState(false);
				}
			} catch (Exception ignored) {
			}
			return;
		}
		try {
			if (state instanceof PetrifiedStateAccess access) {
				access.UltraCapsuleAdventure$setPetrifiedState(true);
			}
		} catch (Exception ignored) {
		}
		// 석화체는 발광 외곽선(영혼의 파란 테두리 등) 제거: 외곽선 색은 state.outlineColor로 직접 그려진다.
		try {
			state.outlineColor = net.minecraft.client.renderer.entity.state.EntityRenderState.NO_OUTLINE;
		} catch (Exception ignored) {
		}
		// 빨강 오버레이 원천 제거: extract 시점에 state에 구워진 값을 직접 끈다.
		// entity.hurtTime=0만으로는 이미 구워진 state.hasRedOverlay가 그대로라서 빨갛게 됨.
		try {
			state.hasRedOverlay = false;
		} catch (Exception ignored) {
		}
		// 시간 기반 유휴 모션 정지
		try {
			int frozen = 0;
			try {
				frozen = entity.tickCount;
			} catch (Exception ignored) {
			}
			state.ageInTicks = frozen;
		} catch (Exception ignored) {
		}
		// 석화 순간의 근접 공격 애니메이션을 서버 저장값에서 복원한다.
		// 좀비/좀비 피글린 등이 팔을 올린 프레임 그대로 유지된다.
		try {
			if (state instanceof net.minecraft.client.renderer.entity.state.ArmedEntityRenderState armed
					&& entity instanceof PetrifiedAccess access) {
				armed.swingAnimation = access.UltraCapsuleAdventure$getFrozenAttackAnim();
			}
		} catch (Exception ignored) {
		}
		// 클라 복사본의 빨간 플래시도 제거 (서버 동기화 안 됨)
		try {
			entity.hurtTime = 0;
			entity.deathTime = 0;
		} catch (Exception ignored) {
		}
		// 석화된 엔티티의 걷기 입력은 렌더 프레임마다 다시 계산되므로
		// 추출된 상태값을 반복 감쇠시키지 않고 즉시 0으로 고정한다.
		// 이전 방식은 풀링된 RenderState에 매 프레임 감쇠를 누적해
		// 모델 애니메이션 입력이 계속 변하는 원인이 될 수 있었다.
		try {
			state.walkAnimationPos = 0.0f;
			state.walkAnimationSpeed = 0.0f;
		} catch (Exception ignored) {
		}
		// 걷기/회전 중 석화 시 보간 떨림 제거
		try {
			entity.setOldPosAndRot();
		} catch (Exception ignored) {
		}
		try {
			entity.yBodyRotO = entity.yBodyRot;
			entity.yHeadRotO = entity.yHeadRot;
		} catch (Exception ignored) {
		}
	}

	private static final java.util.Set<String> DIAG_DONE = new java.util.HashSet<>();

	@Inject(method = "submit", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$markSubmitPetrified(LivingEntityRenderState state, PoseStack pose,
			SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		// 메카 탑승자: 렌더 통째로 스킵 (갑옷·손·이름표·그림자 포함)
		try {
			if (state instanceof com.ultracapsuleadventure.mech.MechRiderAccess access && access.UltraCapsuleAdventure$isHiddenRider()) {
				StoneSwap.ACTIVE = false;
				ci.cancel();
				return;
			}
		} catch (Exception ignored) {
		}
		boolean p = false;
		try {
			if (state instanceof PetrifiedStateAccess access) {
				p = access.UltraCapsuleAdventure$isPetrifiedState();
			}
		} catch (Exception ignored) {
		}
		StoneSwap.ACTIVE = p;
		// 시간 기반 유휴 모션 확정 동결 (렌더러 오버라이드와 무관)
		if (p) {
			// 회전·촉수·팔 흔들림 등 ageInTicks 기반 유휴 애니메이션을 완전히 정지한다.
			// v3에서 빠져 있던 기존 동결 처리를 복구한다.
			try {
				state.ageInTicks = 0.0f;
			} catch (Exception ignored) {
			}
			// 외곽선도 submit 시점에 한 번 더 끈다
			try {
				state.outlineColor = net.minecraft.client.renderer.entity.state.EntityRenderState.NO_OUTLINE;
			} catch (Exception ignored) {
			}
			// submit 시점 최종 강제: extract 이후에 덮어쓰여도 여기서 다시 끈다
			try {
				state.hasRedOverlay = false;
			} catch (Exception ignored) {
			}
			// swingAnimation was restored from the persisted snapshot during extraction.
			// Do not reset it here: doing so drops zombie arms after the first render.
			// 피격 흔들림 고정
			try {
				state.ticksSinceKineticHitFeedback = 0f;
			} catch (Exception ignored) {
			}
			// 팬텀 날개 고정 (펼친 채 굳지 않고 수평으로)
			try {
				if (state instanceof net.minecraft.client.renderer.entity.state.PhantomRenderState phantom) {
					phantom.flapTime = (float) (Math.PI / 2.0);
				}
			} catch (Exception ignored) {
			}
			// 진단: 빨강 원인 특정용 1회 로그
			try {
				String key = "stone";
				synchronized (DIAG_DONE) {
					if (!DIAG_DONE.contains(key)) {
						DIAG_DONE.add(key);
						boolean red = false;
						try {
							red = state.hasRedOverlay;
						} catch (Exception ignored) {
						}
						int outline = -999;
						try {
							outline = state.outlineColor;
						} catch (Exception ignored) {
						}
						com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.warn(
								"[UltraCapsuleAdventure] petrified render: redOverlay=" + red + " outline=" + outline);
					}
				}
			} catch (Exception ignored) {
			}
		}
	}

	@Inject(method = "submit", at = @At("RETURN"))
	private void UltraCapsuleAdventure$unmarkPetrified(LivingEntityRenderState state, PoseStack pose,
			SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		StoneSwap.ACTIVE = false;
	}

	@Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$stoneBody(LivingEntityRenderState state, boolean bodyVisible, boolean translucent,
			boolean glowing, CallbackInfoReturnable<RenderType> cir) {
		try {
			if (state instanceof PetrifiedStateAccess access && access.UltraCapsuleAdventure$isPetrifiedState()) {
				cir.setReturnValue(RenderTypes.entityCutout(StoneSwap.STONE));
			}
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$whiteTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
		// 석화체는 틴트 없음 (젖은 늑대 wetShade 등). -1 = 흰색(원본색).
		try {
			if (state instanceof PetrifiedStateAccess access && access.UltraCapsuleAdventure$isPetrifiedState()) {
				cir.setReturnValue(-1);
			}
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "getOverlayCoords(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)I", at = @At("HEAD"), cancellable = true)
	private static void UltraCapsuleAdventure$noRedFlash(LivingEntityRenderState state, float f,
			CallbackInfoReturnable<Integer> cir) {
		try {
			if (state instanceof PetrifiedStateAccess access && access.UltraCapsuleAdventure$isPetrifiedState()) {
				cir.setReturnValue(
						net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
			}
		} catch (Exception ignored) {
		}
	}

}


