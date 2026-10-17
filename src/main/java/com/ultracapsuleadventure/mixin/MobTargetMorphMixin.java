package com.ultracapsuleadventure.mixin;

import com.ultracapsuleadventure.doppel.DoppelEvents;
import com.ultracapsuleadventure.morph.MorphEvents;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 변신 중립 + 도플갱어 변장체 보호.
 * - 꼭두각시(morph_puppet-*)는 누구의 타겟도 될 수 없음 (소품)
 * - 은신 변신 중 같은 종/동족은 선공 전까지 플레이어를 노리지 않음
 * - 도플갱어 변장체는 스토킹 피해자를 노리지 않음
 * setTarget(null) 해제는 그대로 허용.
 */
@Mixin(Mob.class)
public abstract class MobTargetMorphMixin {

	@Inject(method = "setTarget", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$morphNeutral(LivingEntity target, CallbackInfo ci) {
		try {
			if (target == null) {
				return;
			}
			Mob self = (Mob) (Object) this;
			// 꼭두각시는 타겟 불가
			try {
				for (String tag : target.entityTags()) {
					if (tag.startsWith("morph_puppet-")) {
						ci.cancel();
						return;
					}
				}
			} catch (Exception ignored) {
			}
			// 도플갱어 변장체 → 피해자 타겟 금지
			try {
				if (DoppelEvents.isDisguise(self) && DoppelEvents.isMyVictim(self, target)) {
					ci.cancel();
					return;
				}
			} catch (Exception ignored) {
			}
			// 은신 변신 중립
			try {
				if (target instanceof Player player && MorphEvents.isMorphed(player)
						&& !MorphEvents.recentlyAttacked(player)) {
					String mobType = "";
					String morphType = "";
					try {
						mobType = BuiltInRegistries.ENTITY_TYPE.getKey(self.getType()).toString();
					} catch (Exception ignored) {
					}
					try {
						morphType = MorphEvents.morphId(player);
					} catch (Exception ignored) {
					}
					if (!mobType.isEmpty() && !morphType.isEmpty()
							&& MorphEvents.ignoresMorph(self, mobType, morphType)) {
						ci.cancel();
					}
				}
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}
}


