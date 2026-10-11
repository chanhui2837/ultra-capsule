package com.partnercap.client.entity;

import java.util.HashMap;
import java.util.Map;

import com.partnercap.PartnerCapMod;
import com.partnercap.entity.GloomDevourerEntity;

import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.Identifier;

public class GloomDevourerRenderer extends MobRenderer<GloomDevourerEntity, GloomDevourerRenderState, GloomDevourerModel> {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(PartnerCapMod.id("gloom_devourer"), "main");
	private static final Identifier TEXTURE = PartnerCapMod.id("textures/entity/gloom_devourer.png");

	/** 개체별 단계 전환 보간 상태: {w1, w2, w3, 마지막 ageInTicks} (모델은 공유되므로 여기서 관리) */
	private final Map<Integer, float[]> easing = new HashMap<>();

	public GloomDevourerRenderer(EntityRendererProvider.Context context) {
		super(context, new GloomDevourerModel(context.bakeLayer(LAYER)), 0.7f);
	}

	@Override
	public GloomDevourerRenderState createRenderState() {
		return new GloomDevourerRenderState();
	}

	@Override
	public void extractRenderState(GloomDevourerEntity entity, GloomDevourerRenderState state, float tickProgress) {
		super.extractRenderState(entity, state, tickProgress);
		int ph;
		try {
			ph = entity.getPhase();
		} catch (Exception ignored) {
			ph = 0;
		}
		state.devourPhase = ph;
		try {
			state.attack = AttackAnimAccess.get(entity, tickProgress);
		} catch (Exception ignored) {
			state.attack = 0f;
		}
		if (this.easing.size() > 128) {
			this.easing.clear();
		}
		float[] e = this.easing.get(entity.getId());
		if (e == null) {
			e = new float[] {ph == 1 ? 1f : 0f, ph == 2 ? 1f : 0f, ph == 3 ? 1f : 0f, state.ageInTicks};
			this.easing.put(entity.getId(), e);
		}
		float dt = state.ageInTicks - e[3];
		if (dt < 0f || dt > 10f) {
			dt = 0f;
		}
		e[3] = state.ageInTicks;
		float k = 1f - (float) Math.exp(-0.3f * dt);
		for (int i = 0; i < 3; i++) {
			float target = (ph == i + 1) ? 1f : 0f;
			e[i] += (target - e[i]) * k;
		}
		state.blend1 = e[0];
		state.blend2 = e[1];
		state.blend3 = e[2];
	}

	@Override
	public Identifier getTextureLocation(GloomDevourerRenderState state) {
		return TEXTURE;
	}
}
