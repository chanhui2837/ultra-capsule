package com.partnercap.client.entity;

import com.partnercap.PartnerCapMod;
import com.partnercap.medusa.MedusaEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class MedusaRenderer extends MobRenderer<MedusaEntity, MedusaRenderState, MedusaModel> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(PartnerCapMod.id("medusa"), "main");
	private static final Identifier TEXTURE = PartnerCapMod.id("textures/entity/medusa.png");

	public MedusaRenderer(EntityRendererProvider.Context context) {
		super(context, new MedusaModel(context.bakeLayer(LAYER)), 0.5f);
	}

	@Override
	public MedusaRenderState createRenderState() {
		return new MedusaRenderState();
	}

	@Override
	public void extractRenderState(MedusaEntity entity, MedusaRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		try {
			state.gazeFlash = entity.getGazeFlash();
		} catch (Exception ignored) {
			state.gazeFlash = 0;
		}
		try {
			state.attack = AttackAnimAccess.get(entity, tickProgress);
		} catch (Exception ignored) {
			state.attack = 0f;
		}
	}

	@Override
	public Identifier getTextureLocation(MedusaRenderState state) {
		return TEXTURE;
	}
}
