package com.ultracapsuleadventure.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 저장 펫 3D 미리보기 공용 헬퍼 (바닐라 인벤토리 인형과 동일 경로).
 */
public class EntityPreview {

	private static final Logger LOGGER = LoggerFactory.getLogger("UltraCapsuleAdventure");
	private static boolean loggedOk = false;
	private static boolean loggedFail = false;
	private static final java.util.concurrent.atomic.AtomicInteger NEXT_PREVIEW_ID = new java.util.concurrent.atomic.AtomicInteger(-1000000);

	/** 저장 태그에서 클라용 표시 엔티티 생성 (월드에 추가하지 않음) */
	public static Entity load(Level level, CompoundTag pet) {
		if (level == null || pet == null) {
			return null;
		}
		try {
			EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.LOAD, true);
			Entity created = EntityType.loadEntityRecursive(pet.copy(), level, request, EntityProcessor.NOP);
			if (created == null) {
				return null;
			}
			try {
				created.setId(NEXT_PREVIEW_ID.getAndDecrement());
			} catch (Exception ignored) {
			}
			try {
				created.setYRot(180.0F);
				created.setXRot(0.0F);
			} catch (Exception ignored) {
			}
			if (created instanceof LivingEntity le) {
				try {
					le.yBodyRot = 180.0F;
					le.yHeadRot = 180.0F;
					le.yHeadRotO = 180.0F;
				} catch (Exception ignored) {
				}
			}
			try {
				com.ultracapsuleadventure.util.TameUtil.applyEquipment(created, pet);
			} catch (Exception ignored) {
			}
			return created;
		} catch (Exception e) {
			return null;
		}
	}

	/** 미리보기 1마리 제출. 실패 시 false (호출자가 무시) */
	@SuppressWarnings({ "unchecked", "rawtypes" })
	public static boolean submit(GuiGraphicsExtractor g, Entity preview, float scalePx, int x, int y, int w, int h, float yawRad) {
		try {
			if (preview == null || g == null) {
				return false;
			}
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null) {
				return false;
			}
			EntityRenderDispatcher dispatcher = mc.getEntityRenderDispatcher();
			EntityRenderer renderer = dispatcher.getRenderer(preview);
			EntityRenderState state = renderer.createRenderState(preview, 1.0f);
			state.shadowPieces.clear();
			float yOff = 0f;
			if (state instanceof LivingEntityRenderState living) {
				if (living.scale != 0) {
					living.boundingBoxWidth = living.boundingBoxWidth / living.scale;
					living.boundingBoxHeight = living.boundingBoxHeight / living.scale;
				}
				living.scale = 1.0f;
				yOff = living.boundingBoxHeight / 2f;
			}
			Quaternionf q = new Quaternionf().rotateZ((float) Math.PI);
			Quaternionf q2 = new Quaternionf().rotateY(yawRad);
			q.mul(q2);
			// 바닐라(대장간 갑옷걸이)처럼 두 번째 회전은 null 전달
			g.entity(state, scalePx, new Vector3f(0, yOff, 0), q, null, x, y, x + w, y + h);
			if (!loggedOk) {
				loggedOk = true;
				LOGGER.info("[UltraCapsuleAdventure] preview submit ok");
			}
			return true;
		} catch (Exception e) {
			if (!loggedFail) {
				loggedFail = true;
				LOGGER.warn("[UltraCapsuleAdventure] preview submit failed", e);
			}
			return false;
		}
	}
}


