package com.ultracapsuleadventure.client.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;

/**
 * 안대 착용 모델: 머리 파트를 그대로 따라다니는 눈 밴드.
 * 회전·위치는 렌더러가 실제 머리 파트에서 매 프레임 복사 (눈에 딱 맞음).
 */
public class BlindfoldModel extends EntityModel<HumanoidRenderState> {

	private final ModelPart head;

	public BlindfoldModel(ModelPart root) {
		super(root);
		this.head = root.getChild("head");
	}

	public static LayerDefinition createLayer() {
		MeshDefinition mesh = new MeshDefinition();
		PartDefinition root = mesh.getRoot();
		PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create(), PartPose.ZERO);
		// 눈 높이 밴드 (바닐라 머리 박스 -4..4/-8..0 기준, 0.5px 여유로 z파이팅 방지)
		head.addOrReplaceChild("band",
				CubeListBuilder.create().texOffs(0, 0).addBox(-4.5F, -5.5F, -4.5F, 9.0F, 3.0F, 9.0F),
				PartPose.ZERO);
		return LayerDefinition.create(mesh, 16, 16);
	}

	/** 실제 머리 파트의 위치·회전을 그대로 복사 */
	public void copyHeadTransform(ModelPart src) {
		try {
			head.setPos(src.x, src.y, src.z);
			head.setRotation(src.xRot, src.yRot, src.zRot);
		} catch (Exception ignored) {
		}
	}

	@Override
	public void setupAnim(HumanoidRenderState state) {
		super.setupAnim(state);
	}
}


