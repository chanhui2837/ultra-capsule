package com.ultracapsuleadventure.mixin;

import com.ultracapsuleadventure.soul.SoulAlly;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Enderman;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 아군 소울 엔더맨은 물/비에 면역 (유령이라 젖지 않는다).
 * 물 데미지가 안 들어가면 도망 텔레포트도 안 한다.
 */
@Mixin(Enderman.class)
public abstract class EndermanSoulWaterMixin {

	@Inject(method = "isSensitiveToWater", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$soulNoWater(CallbackInfoReturnable<Boolean> cir) {
		try {
			if (SoulAlly.soulOwnerOf((LivingEntity) (Object) this) != null) {
				cir.setReturnValue(false);
			}
		} catch (Exception ignored) {
		}
	}
}


