package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.client.MorphClient;
import com.ultracapsuleadventure.morph.MorphEvents;

import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 변신 렌더 스왑 (Walkers/Identity와 같은 방식).
 * 플레이어 렌더 상태를 만드는 대신, 동기화된 모프 ID의 더미로 상태를 만들어
 * 파이프라인(렌더러 조회→제출)이 알아서 해당 몹 렌더러·모델·그림자로 그리게 한다.
 * - 서버 텔레포트 꼭두각시가 없으니 떨림이 없고, 1인칭 시야를 가리는 실체도 없다
 * - 본인·타인·F5 모두 같은 모습 (ID는 EntityData로 전 클라에 동기화됨)
 */
@Mixin(EntityRenderDispatcher.class)
public abstract class EntityRenderDispatcherMorphMixin {

	@Inject(method = "extractEntity", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$swapMorph(Entity entity, float tickProgress,
			CallbackInfoReturnable<EntityRenderState> cir) {
		try {
			if (!(entity instanceof Player player)) {
				return;
			}
			// 로컬 플레이어 상태 추출(1인칭 손 등)은 AvatarRenderState 를 요구 → 스왑하면 크래시
			if (MorphClient.extractingPlayerState) {
				return;
			}
			String mid;
			try {
				mid = MorphEvents.morphId(player);
			} catch (Exception e) {
				return;
			}
			if (mid == null || mid.isEmpty()) {
				// 잔상 더미가 남아 있어도 절대 그리지 않게 즉시 제거
				try {
					MorphClient.clearPlayer(player.getUUID());
				} catch (Exception ignored) {
				}
				return;
			}
			Entity dummy;
			try {
				dummy = MorphClient.dummyFor(player, mid);
			} catch (Exception e) {
				return;
			}
			if (dummy == null) {
				return;
			}
			try {
				EntityRenderDispatcher self = (EntityRenderDispatcher) (Object) this;
				EntityRenderState st = self.extractEntity(dummy, tickProgress);
				// 밝기는 더미 위치가 아니라 실제 플레이어 위치 기준으로 (더미 위치가 틀어지면 몹이 까맣게 보임)
				try {
					boolean prev = MorphClient.extractingPlayerState;
					MorphClient.extractingPlayerState = true;
					try {
						EntityRenderState ps = self.extractEntity(player, tickProgress);
						if (ps != null && st != null) {
							st.lightCoords = ps.lightCoords;
						}
					} finally {
						MorphClient.extractingPlayerState = prev;
					}
				} catch (Exception ignored) {
				}
				cir.setReturnValue(st);
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}
}


