package com.ultracapsuleadventure.mixin;

import com.ultracapsuleadventure.medusa.PetrifiedAccess;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 석화 = 원본 엔티티를 그 자리에서 동결.
 * - tick 전체 취소 → 위치·자세·애니메이션 그대로 고정 (클라·서버 양쪽)
 * - 플래그는 SynchedEntityData로 자동 동기화 + NBT 저장
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityPetrifyMixin implements PetrifiedAccess {

	@Unique
	private static final EntityDataAccessor<Boolean> UltraCapsuleAdventure_PETRIFIED = SynchedEntityData.defineId(
			LivingEntity.class, EntityDataSerializers.BOOLEAN);
	@Unique
	private static final EntityDataAccessor<Float> UltraCapsuleAdventure_HEAD_ROT = SynchedEntityData.defineId(
			LivingEntity.class, EntityDataSerializers.FLOAT);
	@Unique
	private static final EntityDataAccessor<Float> UltraCapsuleAdventure_BODY_ROT = SynchedEntityData.defineId(
			LivingEntity.class, EntityDataSerializers.FLOAT);
	@Unique
	private static final EntityDataAccessor<Float> UltraCapsuleAdventure_ATTACK_ANIM = SynchedEntityData.defineId(
			LivingEntity.class, EntityDataSerializers.FLOAT);

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void UltraCapsuleAdventure$definePetrified(SynchedEntityData.Builder builder, CallbackInfo ci) {
		try {
			builder.define(UltraCapsuleAdventure_PETRIFIED, false);
			builder.define(UltraCapsuleAdventure_HEAD_ROT, 0f);
			builder.define(UltraCapsuleAdventure_BODY_ROT, 0f);
			builder.define(UltraCapsuleAdventure_ATTACK_ANIM, 0f);
		} catch (Exception ignored) {
		}
	}

	@Override
	public boolean UltraCapsuleAdventure$isPetrified() {
		try {
			LivingEntity self = (LivingEntity) (Object) this;
			return self.getEntityData().get(UltraCapsuleAdventure_PETRIFIED);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void UltraCapsuleAdventure$setPetrified(boolean petrified) {
		try {
			LivingEntity self = (LivingEntity) (Object) this;
			self.getEntityData().set(UltraCapsuleAdventure_PETRIFIED, petrified);
			if (petrified) {
				self.getEntityData().set(UltraCapsuleAdventure_HEAD_ROT, self.yHeadRot);
				self.getEntityData().set(UltraCapsuleAdventure_BODY_ROT, self.yBodyRot);
				// Minecraft 26.3 LivingEntity does not expose an attack-animation getter.
				// The render-state/model pose is frozen client-side; keep this synced fallback neutral.
				self.getEntityData().set(UltraCapsuleAdventure_ATTACK_ANIM, 0.0f);
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	public float UltraCapsuleAdventure$getFrozenAttackAnim() {
		try {
			return ((LivingEntity) (Object) this).getEntityData().get(UltraCapsuleAdventure_ATTACK_ANIM);
		} catch (Exception e) {
			return 0f;
		}
	}

	@Override
	public void UltraCapsuleAdventure$applyFrozenRotation() {
		try {
			LivingEntity self = (LivingEntity) (Object) this;
			if (!self.getEntityData().get(UltraCapsuleAdventure_PETRIFIED)) {
				return;
			}
			float head = self.getEntityData().get(UltraCapsuleAdventure_HEAD_ROT);
			float body = self.getEntityData().get(UltraCapsuleAdventure_BODY_ROT);
			self.yHeadRot = head;
			self.yHeadRotO = head;
			self.yBodyRot = body;
			self.yBodyRotO = body;
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$freezeTick(CallbackInfo ci) {
		try {
			if (UltraCapsuleAdventure$isPetrified()) {
				// tick 전체를 얼리면 hurtTime--도 안 돌아서 맞던 중 석화 시 빨강이 영구 고착됨.
				// 동결 전에만 빨강 타이머를 0으로 비운다 (석화체는 무적이라 무적프레임 문제 없음).
				try {
					LivingEntity self = (LivingEntity) (Object) this;
					self.hurtTime = 0;
					self.deathTime = 0;
				} catch (Exception ignored) {
				}
				// 클라 스폰/월드 재입장 시 yHeadRot·yBodyRot이 기본값(yRot)으로 초기화되는 문제 대응.
				// 서버에서 SynchedEntityData로 동기화된 값을 클라 엔티티에 적용해 렌더 직전 고정.
				try {
					LivingEntity self = (LivingEntity) (Object) this;
					var data = self.getEntityData();
					self.yHeadRot = data.get(UltraCapsuleAdventure_HEAD_ROT);
					self.yHeadRotO = self.yHeadRot;
					self.yBodyRot = data.get(UltraCapsuleAdventure_BODY_ROT);
					self.yBodyRotO = self.yBodyRot;
				} catch (Exception ignored) {
				}
				ci.cancel();
			}
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void UltraCapsuleAdventure$writePetrified(ValueOutput out, CallbackInfo ci) {
		try {
			out.putBoolean("Petrified", UltraCapsuleAdventure$isPetrified());
			if (UltraCapsuleAdventure$isPetrified()) {
				LivingEntity self = (LivingEntity) (Object) this;
				out.putFloat("UCA_yRot", self.getYRot());
				out.putFloat("UCA_xRot", self.getXRot());
				out.putFloat("UCA_yHeadRot", self.yHeadRot);
				out.putFloat("UCA_yBodyRot", self.yBodyRot);
				out.putFloat("UCA_yHeadRotO", self.yHeadRotO);
				out.putFloat("UCA_yBodyRotO", self.yBodyRotO);
				out.putFloat("UCA_AttackAnim", UltraCapsuleAdventure$getFrozenAttackAnim());
				out.putString("UCA_Pose", self.getPose().name());
				out.putBoolean("UCA_Aggressive", self instanceof net.minecraft.world.entity.Mob mob && mob.isAggressive());
				out.putBoolean("UCA_UsingItem", self.isUsingItem());
				out.putString("UCA_UsedHand", self.getUsedItemHand().name());
			}
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void UltraCapsuleAdventure$readPetrified(ValueInput in, CallbackInfo ci) {
		try {
			if (in.getBooleanOr("Petrified", false)) {
				UltraCapsuleAdventure$setPetrified(true);
				LivingEntity self = (LivingEntity) (Object) this;
				float yaw = in.getFloatOr("UCA_yRot", self.getYRot());
				float pitch = in.getFloatOr("UCA_xRot", self.getXRot());
				self.setYRot(yaw);
				self.setXRot(pitch);
				self.yHeadRot = in.getFloatOr("UCA_yHeadRot", yaw);
				self.yBodyRot = in.getFloatOr("UCA_yBodyRot", yaw);
				self.yHeadRotO = in.getFloatOr("UCA_yHeadRotO", yaw);
				self.yBodyRotO = in.getFloatOr("UCA_yBodyRotO", yaw);
				try {
					self.getEntityData().set(UltraCapsuleAdventure_HEAD_ROT, self.yHeadRot);
					self.getEntityData().set(UltraCapsuleAdventure_BODY_ROT, self.yBodyRot);
					self.getEntityData().set(UltraCapsuleAdventure_ATTACK_ANIM, in.getFloatOr("UCA_AttackAnim", 0f));
				} catch (Exception ignored) {
				}
				try {
					self.setPose(net.minecraft.world.entity.Pose.valueOf(in.getString("UCA_Pose").orElse("STANDING")));
				} catch (Exception ignored) {
				}
				if (self instanceof net.minecraft.world.entity.Mob mob) {
					mob.setAggressive(in.getBooleanOr("UCA_Aggressive", mob.isAggressive()));
				}
				if (in.getBooleanOr("UCA_UsingItem", false)) {
					try {
						net.minecraft.world.InteractionHand hand = net.minecraft.world.InteractionHand.valueOf(in.getString("UCA_UsedHand").orElse("MAIN_HAND"));
						if (!self.getItemInHand(hand).isEmpty()) self.startUsingItem(hand);
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
	}
}


