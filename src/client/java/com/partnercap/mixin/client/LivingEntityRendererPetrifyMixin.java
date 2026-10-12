package com.partnercap.mixin.client;

import com.partnercap.client.StoneSwap;
import com.partnercap.medusa.PetrifiedAccess;
import com.partnercap.medusa.PetrifiedStateAccess;

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

	@Inject(method = "extractRenderState(Lnet/minecraft/world/entity/LivingEntity;Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)V", at = @At("RETURN"))
	private void partnercap$markPetrified(LivingEntity entity, LivingEntityRenderState state, float tickProgress,
			CallbackInfo ci) {
		boolean p = false;
		try {
			p = entity instanceof PetrifiedAccess access && access.partnercap$isPetrified();
		} catch (Exception ignored) {
		}
		// 메카 탑승자는 몸·갑옷·손·망토·그림자 통째로 숨김 (서버 setInvisible로는 레이어가 남음)
		boolean hidden = false;
		try {
			hidden = entity.getVehicle() instanceof com.partnercap.mech.MechRobotEntity;
		} catch (Exception ignored) {
		}
		try {
			if (state instanceof com.partnercap.mech.MechRiderAccess access) {
				access.partnercap$setHiddenRider(hidden);
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
				com.partnercap.client.StoneSwap.settleRemove(entity.getUUID());
			} catch (Exception ignored) {
			}
			// 상태 객체 풀링 대비: 이전에 석화로 쓰던 state 재사용 시 플래그 오염 방지
			try {
				if (state instanceof PetrifiedStateAccess access) {
					access.partnercap$setPetrifiedState(false);
				}
			} catch (Exception ignored) {
			}
			return;
		}
		try {
			if (state instanceof PetrifiedStateAccess access) {
				access.partnercap$setPetrifiedState(true);
			}
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
		// 클라 복사본의 빨간 플래시도 제거 (서버 동기화 안 됨)
		try {
			entity.hurtTime = 0;
			entity.deathTime = 0;
		} catch (Exception ignored) {
		}
		// 걷기 중 석화 시 0.5초에 걸쳐 다리 나란히 (easing 후 고정)
		try {
			java.util.UUID uid = null;
			try {
				uid = entity.getUUID();
			} catch (Exception ignored) {
			}
			long now = System.currentTimeMillis();
			long start = com.partnercap.client.StoneSwap.settleStart(uid, now);
			float t = Math.max(0f, Math.min(1f, (now - start) / 200f));
			float e = t * t * (3f - 2f * t);
			state.walkAnimationPos = state.walkAnimationPos * (1f - e);
			state.walkAnimationSpeed = state.walkAnimationSpeed * (1f - e);
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
	private void partnercap$markSubmitPetrified(LivingEntityRenderState state, PoseStack pose,
			SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		// 메카 탑승자: 렌더 통째로 스킵 (갑옷·손·이름표·그림자 포함)
		try {
			if (state instanceof com.partnercap.mech.MechRiderAccess access && access.partnercap$isHiddenRider()) {
				StoneSwap.ACTIVE = false;
				ci.cancel();
				return;
			}
		} catch (Exception ignored) {
		}
		boolean p = false;
		try {
			if (state instanceof PetrifiedStateAccess access) {
				p = access.partnercap$isPetrifiedState();
			}
		} catch (Exception ignored) {
		}
		StoneSwap.ACTIVE = p;
		// 시간 기반 유휴 모션 확정 동결 (렌더러 오버라이드와 무관)
		if (p) {
			try {
				state.ageInTicks = 0f;
			} catch (Exception ignored) {
			}
			// submit 시점 최종 강제: extract 이후에 덮어쓰여도 여기서 다시 끈다
			try {
				state.hasRedOverlay = false;
			} catch (Exception ignored) {
			}
			// 공격 팔 내리기 (좀비 피글린 등): 휘두르기 진행도 0 고정
			try {
				if (state instanceof net.minecraft.client.renderer.entity.state.ArmedEntityRenderState armed) {
					armed.swingAnimation = 0f;
				}
			} catch (Exception ignored) {
			}
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
						com.partnercap.PartnerCapMod.LOGGER.warn(
								"[PartnerCap] petrified render: redOverlay=" + red + " outline=" + outline);
					}
				}
			} catch (Exception ignored) {
			}
		}
	}

	@Inject(method = "submit", at = @At("RETURN"))
	private void partnercap$unmarkPetrified(LivingEntityRenderState state, PoseStack pose,
			SubmitNodeCollector collector, CameraRenderState camera, CallbackInfo ci) {
		StoneSwap.ACTIVE = false;
	}

	@Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
	private void partnercap$stoneBody(LivingEntityRenderState state, boolean bodyVisible, boolean translucent,
			boolean glowing, CallbackInfoReturnable<RenderType> cir) {
		try {
			if (state instanceof PetrifiedStateAccess access && access.partnercap$isPetrifiedState()) {
				cir.setReturnValue(RenderTypes.entityCutout(StoneSwap.STONE));
			}
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "getModelTint", at = @At("HEAD"), cancellable = true)
	private void partnercap$whiteTint(LivingEntityRenderState state, CallbackInfoReturnable<Integer> cir) {
		// 석화체는 틴트 없음 (젖은 늑대 wetShade 등). -1 = 흰색(원본색).
		try {
			if (state instanceof PetrifiedStateAccess access && access.partnercap$isPetrifiedState()) {
				cir.setReturnValue(-1);
			}
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "getOverlayCoords(Lnet/minecraft/client/renderer/entity/state/LivingEntityRenderState;F)I", at = @At("HEAD"), cancellable = true)
	private static void partnercap$noRedFlash(LivingEntityRenderState state, float f,
			CallbackInfoReturnable<Integer> cir) {
		try {
			if (state instanceof PetrifiedStateAccess access && access.partnercap$isPetrifiedState()) {
				cir.setReturnValue(
						net.minecraft.client.renderer.texture.OverlayTexture.NO_OVERLAY);
			}
		} catch (Exception ignored) {
		}
	}

}
