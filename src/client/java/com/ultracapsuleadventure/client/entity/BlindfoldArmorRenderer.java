package com.ultracapsuleadventure.client.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.ultracapsuleadventure.UltraCapsuleAdventureMod;

import net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;

/**
 * 안대 착용 렌더: 머리 슬롯일 때만 눈 밴드 표시. 기본 스프라이트 렌더는 끈다.
 */
public class BlindfoldArmorRenderer implements ArmorRenderer {

	public static final ModelLayerLocation LAYER = new ModelLayerLocation(UltraCapsuleAdventureMod.id("blindfold"), "main");
	private static final Identifier WORN = UltraCapsuleAdventureMod.id("textures/entity/blindfold_worn.png");

	private final BlindfoldModel model;

	public BlindfoldArmorRenderer(EntityRendererProvider.Context context) {
		this.model = new BlindfoldModel(context.bakeLayer(LAYER));
	}

	@Override
	public void render(PoseStack poseStack, SubmitNodeCollector submitNodeCollector, ItemStack stack,
			HumanoidRenderState humanoidRenderState, EquipmentSlot slot, int light,
			HumanoidModel<HumanoidRenderState> contextModel) {
		if (slot != EquipmentSlot.HEAD) {
			return;
		}
		try {
			model.copyHeadTransform(contextModel.head);
			submitNodeCollector.submitModel(model, humanoidRenderState, poseStack,
					net.minecraft.client.renderer.rendertype.RenderTypes.entityCutout(WORN),
					light, OverlayTexture.NO_OVERLAY, -1);
		} catch (Exception ignored) {
		}
	}

	@Override
	public boolean shouldRenderDefaultHeadItem(LivingEntity entity, ItemStack stack) {
		return false;
	}
}


