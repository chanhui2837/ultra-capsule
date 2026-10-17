package com.ultracapsuleadventure.mixin;

import com.ultracapsuleadventure.medusa.PetrifiedAccess;

import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 석화 동결의 진짜 완성: LivingEntity.tick 취소만으로는 Mob.tick의
 * goalSelector/targetSelector/이동제어가 그대로 돌아서
 * 석화된 애가 가끔 고개를 돌리고 타겟을 잡는다.
 * Mob.tick 자체를 막으면 목표·시선·내비게이션 전부 정지.
 */
@Mixin(Mob.class)
public abstract class MobFreezeMixin {

	@Inject(method = "tick", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$freezeMobTick(CallbackInfo ci) {
		try {
			if ((Object) this instanceof PetrifiedAccess access && access.UltraCapsuleAdventure$isPetrified()) {
				ci.cancel();
			}
		} catch (Exception ignored) {
		}
	}
}


