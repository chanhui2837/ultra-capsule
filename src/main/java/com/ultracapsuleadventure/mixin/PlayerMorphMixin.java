package com.ultracapsuleadventure.mixin;

import com.ultracapsuleadventure.morph.MorphAccess;
import com.ultracapsuleadventure.morph.MorphEvents;
import com.ultracapsuleadventure.morph.MorphSyncCache;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 플레이어 변신 ID 동기화.
 * Player EntityData에는 빈 슬롯이 없어(최대 21, 바닐라가 전부 사용) defineId가
 * "Data value id is too big"으로 실패하므로 EntityData를 쓰지 않는다.
 * 대신 서버는 SERVER_MORPH 맵 권위 + 브로드캐스트 패킷(MorphSyncCache) 동기화.
 * NBT 저장은 유지하고, 재접속 시 서버 맵으로 복원한다.
 */
@Mixin(Player.class)
public abstract class PlayerMorphMixin extends LivingEntity implements MorphAccess {

	protected PlayerMorphMixin(EntityType<? extends LivingEntity> type, Level level) {
		super(type, level);
	}

	@Override
	public String UltraCapsuleAdventure$getMorphId() {
		try {
			Player self = (Player) (Object) this;
			if (!self.level().isClientSide()) {
				return MorphEvents.serverMorphId(self.getUUID());
			}
			return MorphSyncCache.get(self.getUUID());
		} catch (Exception e) {
			return "";
		}
	}

	@Override
	public void UltraCapsuleAdventure$setMorphId(String id) {
		try {
			Player self = (Player) (Object) this;
			if (!self.level().isClientSide()) {
				MorphEvents.setServerMorph(self.getUUID(), id);
			} else {
				MorphSyncCache.set(self.getUUID(), id);
			}
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
	private void UltraCapsuleAdventure$saveMorph(ValueOutput out, CallbackInfo ci) {
		try {
			out.putString("UltraCapsuleAdventureMorph", UltraCapsuleAdventure$getMorphId());
		} catch (Exception ignored) {
		}
	}

	@Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
	private void UltraCapsuleAdventure$loadMorph(ValueInput in, CallbackInfo ci) {
		try {
			String v = in.getString("UltraCapsuleAdventureMorph").orElse("");
			if (v == null || v.isEmpty()) {
				return;
			}
			Player self = (Player) (Object) this;
			// 클라 복사본은 패킷으로 받으므로 서버만 복원 (재접속해도 변신 유지)
			if (self.level().isClientSide()) {
				return;
			}
			if (!(self instanceof net.minecraft.server.level.ServerPlayer sp)) {
				return;
			}
			try {
				MorphEvents.setServerMorph(sp.getUUID(), v);
				com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.info("[Morph] restored "
						+ sp.getName().getString() + " -> " + v);
			} catch (Exception ignored) {
			}
			// 본인 캐시 + 전체 전파는 JOIN 핸들러(모든 변신자 전송)가 담당
		} catch (Exception ignored) {
		}
	}
}


