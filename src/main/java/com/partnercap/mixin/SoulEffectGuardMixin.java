package com.partnercap.mixin;

import java.util.UUID;

import com.partnercap.soul.SoulAlly;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 아군 오폭 이펙트 차단: 소울이 뿌린 포션(스플래시 묻힘 포함)은
 * 같은 주인의 플레이어/동료 소울에게 적용되지 않는다.
 * (데미지는 SoulEvents ALLOW_DAMAGE가, 상태이상은 여기서 차단)
 */
@Mixin(LivingEntity.class)
public abstract class SoulEffectGuardMixin {

	@Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"), cancellable = true)
	private void partnercap$noFriendlySplash(MobEffectInstance effect, Entity source,
			CallbackInfoReturnable<Boolean> cir) {
		try {
			if (source == null) {
				return;
			}
			UUID so = null;
			try {
				so = SoulAlly.soulOwnerOf(source);
			} catch (Exception ignored) {
			}
			if (so == null) {
				return;
			}
			LivingEntity self = (LivingEntity) (Object) this;
			if (self instanceof Player vp) {
				try {
					if (vp.getUUID().equals(so)) {
						cir.setReturnValue(false);
					}
				} catch (Exception ignored) {
				}
				return;
			}
			UUID vo = null;
			try {
				vo = SoulAlly.soulOwnerOf(self);
			} catch (Exception ignored) {
			}
			if (vo != null && vo.equals(so)) {
				cir.setReturnValue(false);
			}
		} catch (Exception ignored) {
		}
	}
}
