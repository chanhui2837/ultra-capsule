package com.ultracapsuleadventure.mixin;

import com.ultracapsuleadventure.morph.MorphEvents;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 변신 포션 면역 (참고 모드 UndeadTrait/EffectResistance와 동일).
 * - 언데드 변신: 재생·독 무효
 * - 위더 변신: 모든 효과 무효
 * - 위더 스켈레톤 변신: 시듦 무효
 */
@Mixin(LivingEntity.class)
public abstract class LivingEntityMorphImmuneMixin {

	@Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$morphImmune(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
		try {
			if (!((Object) this instanceof Player player)) {
				return;
			}
			String mid;
			try {
				mid = MorphEvents.morphId(player);
			} catch (Exception e) {
				return;
			}
			if (mid == null || mid.isEmpty() || effect == null) {
				return;
			}
			boolean undead = false;
			try {
				undead = MorphEvents.isUndeadMorph(mid);
			} catch (Exception ignored) {
			}
			if (undead) {
				try {
					var key = effect.getEffect();
					if (key.is(MobEffects.REGENERATION) || key.is(MobEffects.POISON)) {
						cir.setReturnValue(false);
						return;
					}
				} catch (Exception ignored) {
				}
			}
			if ("minecraft:wither".equals(mid)) {
				cir.setReturnValue(false);
				return;
			}
			if ("minecraft:wither_skeleton".equals(mid)) {
				try {
					if (effect.getEffect().is(MobEffects.WITHER)) {
						cir.setReturnValue(false);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}
}


