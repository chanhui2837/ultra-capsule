package com.ultracapsuleadventure.mixin.client;

import net.minecraft.client.gui.Hud;
import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * 메카 차량 체력 하트 비례 표시.
 * 바닐라 {@code Hud.getVehicleMaxHearts}는 30칸(60HP)에서 잘라서 MAX_HP 200인 메카는
 * 반피(100/200)까지도 30칸 풀피로 보임. 여기서 {@code extractVehicleHealth} 안의
 * {@code getHealth()}만 메카에 한해 0~60으로 스케일해 캡슐 내구도와 같은 비율로 보이게 한다.
 */
@Mixin(Hud.class)
public abstract class HudVehicleHealthMixin {

	@Redirect(method = "extractVehicleHealth", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;getHealth()F"))
	private float UltraCapsuleAdventure$scaleMechHealth(LivingEntity vehicle) {
		try {
			float h = 0f;
			try {
				h = vehicle.getHealth();
			} catch (Exception ignored) {
				return 0f;
			}
			try {
				if (vehicle instanceof com.ultracapsuleadventure.mech.MechRobotEntity) {
					float max = 200f;
					try {
						max = vehicle.getMaxHealth();
					} catch (Exception ignored) {
					}
					if (max > 60.01f && max > 0f) {
						return (float) Math.ceil(h / max * 60.0);
					}
				}
			} catch (Exception ignored) {
			}
			return h;
		} catch (Exception ignored) {
			try {
				return vehicle.getHealth();
			} catch (Exception e) {
				return 0f;
			}
		}
	}
}


