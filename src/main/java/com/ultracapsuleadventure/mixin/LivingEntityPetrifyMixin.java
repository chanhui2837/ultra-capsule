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

	@Inject(method = "defineSynchedData", at = @At("TAIL"))
	private void UltraCapsuleAdventure$definePetrified(SynchedEntityData.Builder builder, CallbackInfo ci) {
		try {
			builder.define(UltraCapsuleAdventure_PETRIFIED, false);
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
				ci.cancel();
			}
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void UltraCapsuleAdventure$writePetrified(ValueOutput out, CallbackInfo ci) {
		try {
			out.putBoolean("Petrified", UltraCapsuleAdventure$isPetrified());
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void UltraCapsuleAdventure$readPetrified(ValueInput in, CallbackInfo ci) {
		try {
			if (in.getBooleanOr("Petrified", false)) {
				UltraCapsuleAdventure$setPetrified(true);
			}
		} catch (Exception ignored) {
		}
	}
}


