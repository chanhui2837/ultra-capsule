package com.ultracapsuleadventure.mixin;

import com.ultracapsuleadventure.morph.MorphDims;
import com.ultracapsuleadventure.morph.MorphEvents;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 변신 중 플레이어 크기 교체 (Walkers의 refreshDimensions와 같은 효과).
 * LivingEntity.getDimensions 가 final 이라 Entity.getDimensions 를 가로채는 방식은 플레이어에게 호출되지 않는다.
 * 여기(LivingEntity)서 바꿔야 히트박스·눈높이·카메라가 함께 따라간다.
 */
@Mixin(LivingEntity.class)
public abstract class EntityMorphDimsMixin {

	@Inject(method = "getDimensions", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$morphDims(Pose pose, CallbackInfoReturnable<EntityDimensions> cir) {
		try {
			if ((Object) this instanceof Player player) {
				String mid = MorphEvents.morphId(player);
				if (mid != null && !mid.isEmpty()) {
					EntityDimensions d = MorphDims.dimsFor(mid);
					if (d != null) {
						cir.setReturnValue(d);
					}
				}
			}
		} catch (Exception ignored) {
		}
	}
}


