package com.ultracapsuleadventure.mixin;

import com.ultracapsuleadventure.medusa.Petrify;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 석화된 몬스터는 석상이므로 침대 수면을 방해하지 않는다
 * ("근처에 몬스터가 있어서 잘 수 없습니다" 판정에서 제외).
 * 바닐라는 침대 주변 몬스터에게 isPreventingPlayerRest를 물어 하나라도 true면 수면을 막는다.
 * 메서드 시그니처가 다르면 이 주입만 조용히 건너뛴다(require = 0).
 */
@Mixin(Monster.class)
public abstract class MonsterRestPetrifyMixin {

	@Inject(method = "isPreventingPlayerRest(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/entity/player/Player;)Z", at = @At("HEAD"), cancellable = true, require = 0)
	private void UltraCapsuleAdventure$statueDoesNotBlockSleep(ServerLevel level, Player player,
			CallbackInfoReturnable<Boolean> cir) {
		try {
			if (Petrify.isPetrified((Entity) (Object) this)) {
				cir.setReturnValue(false);
			}
		} catch (Exception ignored) {
		}
	}
}
