package com.ultracapsuleadventure.mixin;

import com.ultracapsuleadventure.medusa.Petrify;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 석화된 엔티티는 누구의 타겟도 될 수 없다 (획득 원천 차단).
 * 1초마다 타겟을 비우는 기존 스위퍼만으로는 엔더맨→엔더마이트 같은
 * 상시 재획득 골을 못 막아서 입 벌린 채 계속 달려든다.
 * setTarget(null) 호출(타겟 해제)은 그대로 허용된다.
 */
@Mixin(Mob.class)
public abstract class MobTargetPetrifyMixin {

	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$noTargetStatue(LivingEntity target, CallbackInfo ci) {
		try {
			if (target != null && Petrify.isPetrified(target)) {
				ci.cancel();
			}
		} catch (Exception ignored) {
		}
	}
}


