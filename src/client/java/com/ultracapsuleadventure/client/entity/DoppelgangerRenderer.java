package com.ultracapsuleadventure.client.entity;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;
import com.ultracapsuleadventure.doppel.DoppelgangerEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class DoppelgangerRenderer extends MobRenderer<DoppelgangerEntity, DoppelgangerRenderState, DoppelgangerModel> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(UltraCapsuleAdventureMod.id("doppelganger"), "main");
	private static final Identifier TEXTURE = UltraCapsuleAdventureMod.id("textures/entity/doppelganger.png");

	public DoppelgangerRenderer(EntityRendererProvider.Context context) {
		super(context, new DoppelgangerModel(context.bakeLayer(LAYER)), 0.6f);
		addLayer(new DoppelgangerEyesLayer(this));
	}

	@Override
	public DoppelgangerRenderState createRenderState() {
		return new DoppelgangerRenderState();
	}

	@Override
	public void extractRenderState(DoppelgangerEntity entity, DoppelgangerRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		try {
			state.phase = entity.getPhase();
		} catch (Exception ignored) {
			state.phase = 0;
		}
		try {
			state.attack = AttackAnimAccess.get(entity, tickProgress);
		} catch (Exception ignored) {
			state.attack = 0f;
		}
	}

	@Override
	public Identifier getTextureLocation(DoppelgangerRenderState state) {
		return TEXTURE;
	}
}


