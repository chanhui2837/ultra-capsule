package com.partnercap.client.entity;

import com.partnercap.PartnerCapMod;
import com.partnercap.flame.FlameMongEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class FlameMongRenderer extends MobRenderer<FlameMongEntity, FlameMongRenderState, FlameMongModel> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(PartnerCapMod.id("flame_mong"), "main");
	private static final Identifier TEXTURE = PartnerCapMod.id("textures/entity/flame_mong.png");

	public FlameMongRenderer(EntityRendererProvider.Context context) {
		super(context, new FlameMongModel(context.bakeLayer(LAYER)), 0.6f);
	}

	@Override
	public FlameMongRenderState createRenderState() {
		return new FlameMongRenderState();
	}

	@Override
	public void extractRenderState(FlameMongEntity entity, FlameMongRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		try {
			state.mouthOpen = entity.getMouthOpen();
		} catch (Exception ignored) {
			state.mouthOpen = 0;
		}
	}

	@Override
	public Identifier getTextureLocation(FlameMongRenderState state) {
		return TEXTURE;
	}
}
