package com.ultracapsuleadventure.client.entity;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;

import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.EyesLayer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/** 도플갱어 발광 눈 (밝기 무관, 눈 픽셀만 불투명한 별도 텍스처) */
public class DoppelgangerEyesLayer extends EyesLayer<DoppelgangerRenderState, DoppelgangerModel> {

	private static final RenderType EYES = RenderTypes
			.eyes(Identifier.fromNamespaceAndPath(UltraCapsuleAdventureMod.MOD_ID, "textures/entity/doppelganger_eyes.png"));

	public DoppelgangerEyesLayer(RenderLayerParent<DoppelgangerRenderState, DoppelgangerModel> parent) {
		super(parent);
	}

	@Override
	public RenderType renderType() {
		return EYES;
	}
}


