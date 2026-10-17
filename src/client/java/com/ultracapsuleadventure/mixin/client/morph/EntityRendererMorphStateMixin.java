package com.ultracapsuleadventure.mixin.client.morph;

import com.ultracapsuleadventure.client.MorphClient;
import com.ultracapsuleadventure.morph.MorphEvents;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * E키 인벤토리 / 크리에이티브 인벤토리의 플레이어 인형 변신 적용.
 * 바닐라 인벤토리 인형은 EntityRenderDispatcher.extractEntity 를 거치지 않고
 * renderer.createRenderState(player) 를 직접 호출해서 기존 렌더 스왑이 우회됐다.
 * 월드 렌더(extractEntity)와 1인칭 추출(extractingPlayerState)은 플래그로 제외한다.
 */
@Mixin(EntityRenderer.class)
public abstract class EntityRendererMorphStateMixin {

	@Inject(method = "createRenderState(Lnet/minecraft/world/entity/Entity;F)Lnet/minecraft/client/renderer/entity/state/EntityRenderState;",
			at = @At("HEAD"), cancellable = true, require = 0)
	@SuppressWarnings({ "unchecked", "rawtypes" })
	private void UltraCapsuleAdventure$guiMorph(Entity entity, float partial, CallbackInfoReturnable<EntityRenderState> cir) {
		try {
			if (!(entity instanceof Player player) || MorphClient.extractingPlayerState || MorphClient.inGuiSwap) {
				return;
			}
			if (!player.level().isClientSide()) {
				return;
			}
			String mid = MorphEvents.morphId(player);
			if (mid == null || mid.isEmpty()) {
				return;
			}
			Entity dummy = MorphClient.dummyFor(player, mid);
			if (dummy == null) {
				return;
			}
			MorphClient.inGuiSwap = true;
			try {
				EntityRenderer dr = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(dummy);
				EntityRenderState st = dr.createRenderState(dummy, partial);
				if (st != null) {
					st.shadowPieces.clear();
					cir.setReturnValue(st);
				}
			} finally {
				MorphClient.inGuiSwap = false;
			}
		} catch (Exception ignored) {
		}
	}
}


