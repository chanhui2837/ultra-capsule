package com.ultracapsuleadventure.client.entity;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;
import com.ultracapsuleadventure.flame.FlameMongEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class FlameMongRenderer extends MobRenderer<FlameMongEntity, FlameMongRenderState, FlameMongModel> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(UltraCapsuleAdventureMod.id("flame_mong"), "main");
	private static final Identifier TEXTURE = UltraCapsuleAdventureMod.id("textures/entity/flame_mong.png");

	public FlameMongRenderer(EntityRendererProvider.Context context) {
		super(context, new FlameMongModel(context.bakeLayer(LAYER)), 0.6f);
		try {
			addLayer(new net.minecraft.client.renderer.entity.layers.ItemInHandLayer<>(this));
		} catch (Exception ignored) {
		}
	}

	@Override
	public FlameMongRenderState createRenderState() {
		return new FlameMongRenderState();
	}

	@Override
	public void extractRenderState(FlameMongEntity entity, FlameMongRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		// 손에 든 초월의 막대 렌더용 (스폰알 소환분 포함, 손이 비어도 상태만 갱신)
		try {
			net.minecraft.client.renderer.entity.state.ArmedEntityRenderState.extractArmedEntityRenderState(
					entity, state, this.itemModelResolver, tickProgress);
		} catch (Exception ignored) {
		}
		try {
			state.mouthOpen = entity.getMouthOpen();
		} catch (Exception ignored) {
			state.mouthOpen = 0;
		}
		try {
			// 벽에 붙어 공중에 있을 때만 등반 자세 (땅에서 벽에 부딪히는 것만으로는 팔을 들지 않음)
			state.climbing = entity.horizontalCollision && !entity.isInLava() && !entity.onGround();
		} catch (Exception ignored) {
			state.climbing = false;
		}
		try {
			state.attack = AttackAnimAccess.get(entity, tickProgress);
		} catch (Exception ignored) {
			state.attack = 0f;
		}
		try {
			state.sleeping = entity.isSleeping();
		} catch (Exception ignored) {
			state.sleeping = false;
		}
	}

	@Override
	public Identifier getTextureLocation(FlameMongRenderState state) {
		return TEXTURE;
	}
}


