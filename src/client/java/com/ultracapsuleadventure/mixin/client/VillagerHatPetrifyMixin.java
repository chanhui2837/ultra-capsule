package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.medusa.PetrifiedStateAccess;

import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.npc.VillagerModel;
import net.minecraft.client.renderer.entity.state.VillagerRenderState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * 석화된 주민 석상에 모자(hat + hat_rim)가 생기는 문제 수정.
 * 주민 모델의 모자 파트는 root가 아니라 head 아래(root/head/hat/hat_rim)에 있다.
 * 예전 코드는 root.getChild("hat")만 찾아서 항상 실패했기 때문에 모자가 숨겨지지 않았다.
 * 석화 시 돌 텍스처로 바꿔치기되면 투명이던 모자 영역이 불투명 돌모자가 되고,
 * 직업 레이어(농부 짚모자 등)도 같은 모델의 모자 파트를 그리므로 이 파트를 숨기면 둘 다 사라진다.
 * 직업/바이옴 텍스처에 실제 모자가 있는 주민(농부 등)은 모자를 그대로 두고, 모자 없는 주민만 숨긴다.
 * 떠돌이 상인은 후드가 모자 파트라서 제외한다.
 * 26.x submit 파이프라인은 visible 플래그를 읽지 않으므로 스케일 0으로도 숨긴다.
 * 모델은 렌더러 공용 싱글톤이라 매 프레임 양방향 갱신. BabyVillagerModel도 함께 적용된다.
 */
@Mixin(VillagerModel.class)
public abstract class VillagerHatPetrifyMixin {

	@Unique
	private ModelPart UltraCapsuleAdventure$root;

	@Unique
	private ModelPart UltraCapsuleAdventure$hat;

	@Inject(method = "<init>(Lnet/minecraft/client/model/geom/ModelPart;)V", at = @At("RETURN"))
	private void UltraCapsuleAdventure$captureRoot(ModelPart root, CallbackInfo ci) {
		try {
			UltraCapsuleAdventure$root = root;
		} catch (Exception ignored) {
		}
	}

	@Unique
	private static ModelPart UltraCapsuleAdventure$child(ModelPart parent, String name) {
		try {
			if (parent != null && parent.hasChild(name)) {
				return parent.getChild(name);
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	@Unique
	private ModelPart UltraCapsuleAdventure$findHat() {
		if (UltraCapsuleAdventure$hat != null) {
			return UltraCapsuleAdventure$hat;
		}
		ModelPart root = UltraCapsuleAdventure$root;
		if (root == null) {
			return null;
		}
		ModelPart hat = UltraCapsuleAdventure$child(UltraCapsuleAdventure$child(root, "head"), "hat");
		if (hat == null) {
			hat = UltraCapsuleAdventure$child(root, "hat");
		}
		UltraCapsuleAdventure$hat = hat;
		return hat;
	}

	/**
	 * 렌더 상태의 VillagerData 문자열. 떠돌이 상인처럼 데이터가 없으면 null, 접근 수단을 못 찾으면 ""(주민으로 간주).
	 */
	@Unique
	private static String UltraCapsuleAdventure$villagerDataString(Object state) {
		try {
			Object data;
			try {
				java.lang.reflect.Method m = state.getClass().getMethod("getVillagerData");
				data = m.invoke(state);
			} catch (NoSuchMethodException e) {
				java.lang.reflect.Field f = state.getClass().getField("villagerData");
				data = f.get(state);
			}
			return data == null ? null : String.valueOf(data);
		} catch (Exception ignored) {
		}
		return "";
	}

	@Unique
	private static final java.util.Map<String, Boolean> UltraCapsuleAdventure$HAT_CACHE = new java.util.HashMap<>();

	@Unique
	private static String UltraCapsuleAdventure$regId(String data, String registry) {
		java.util.regex.Matcher m = java.util.regex.Pattern
				.compile(registry + "\\s*/\\s*([a-z0-9_.\\-]+:)?([a-z0-9_./\\-]+)").matcher(data);
		if (!m.find()) {
			return null;
		}
		String ns = m.group(1) == null ? "minecraft:" : m.group(1);
		return ns + m.group(2);
	}

	/**
	 * 이 주민이 실제로 모자를 쓰는지: 직업/바이옴 텍스처의 모자 영역(hat: x32~64,y0~18 / hat_rim: x30~64,y47~64)에
	 * 불투명 픽셀이 있으면 모자 있음 (농부 짚모자 등). 판별 실패 시 모자 없음으로 처리해 숨긴다.
	 */
	@Unique
	private static boolean UltraCapsuleAdventure$hasRealHat(String data) {
		try {
			if (data == null || data.isEmpty()) {
				return false;
			}
			String prof = UltraCapsuleAdventure$regId(data, "villager_profession");
			String type = UltraCapsuleAdventure$regId(data, "villager_type");
			String key = prof + "|" + type;
			synchronized (UltraCapsuleAdventure$HAT_CACHE) {
				Boolean c = UltraCapsuleAdventure$HAT_CACHE.get(key);
				if (c != null) {
					return c;
				}
			}
			boolean has = UltraCapsuleAdventure$textureHasHat(prof, "profession")
					|| UltraCapsuleAdventure$textureHasHat(type, "type");
			synchronized (UltraCapsuleAdventure$HAT_CACHE) {
				UltraCapsuleAdventure$HAT_CACHE.put(key, has);
			}
			return has;
		} catch (Exception ignored) {
		}
		return false;
	}

	@Unique
	private static boolean UltraCapsuleAdventure$textureHasHat(String id, String folder) {
		try {
			if (id == null) {
				return false;
			}
			int i = id.indexOf(':');
			String ns = i < 0 ? "minecraft" : id.substring(0, i);
			String path = i < 0 ? id : id.substring(i + 1);
			net.minecraft.resources.Identifier tex = net.minecraft.resources.Identifier.fromNamespaceAndPath(ns,
					"textures/entity/villager/" + folder + "/" + path + ".png");
			java.util.Optional<net.minecraft.server.packs.resources.Resource> res = net.minecraft.client.Minecraft
					.getInstance().getResourceManager().getResource(tex);
			if (res.isEmpty()) {
				return false;
			}
			try (java.io.InputStream in = res.get().open()) {
				java.awt.image.BufferedImage img = javax.imageio.ImageIO.read(in);
				if (img == null) {
					return false;
				}
				int w = img.getWidth();
				int h = img.getHeight();
				// 64x64 기준 좌표를 실제 해상도에 맞춰 환산
				int sx = Math.max(1, w / 64);
				int sy = Math.max(1, h / 64);
				int[][] boxes = { { 32, 0, 64, 18 }, { 30, 47, 64, 64 } };
				for (int[] b : boxes) {
					for (int y = b[1] * sy; y < Math.min(h, b[3] * sy); y++) {
						for (int x = b[0] * sx; x < Math.min(w, b[2] * sx); x++) {
							if (((img.getRGB(x, y) >>> 24) & 0xFF) > 8) {
								return true;
							}
						}
					}
				}
			}
		} catch (Throwable ignored) {
		}
		return false;
	}

	@Inject(method = "setupAnim(Lnet/minecraft/client/renderer/entity/state/VillagerRenderState;)V", at = @At("TAIL"))
	private void UltraCapsuleAdventure$hideHatOnStatue(VillagerRenderState state, CallbackInfo ci) {
		try {
			boolean p = false;
			if (state instanceof PetrifiedStateAccess access && access.UltraCapsuleAdventure$isPetrifiedState()) {
				String data = UltraCapsuleAdventure$villagerDataString(state);
				// 떠돌이 상인(data == null)은 제외, 실제 모자를 쓰는 주민(농부 등)은 모자 유지
				p = data != null && !UltraCapsuleAdventure$hasRealHat(data);
			}
			ModelPart hat = UltraCapsuleAdventure$findHat();
			if (hat == null) {
				return;
			}
			if (p) {
				hat.visible = false;
			}
			// submit 파이프라인은 visible을 무시하므로 스케일 0으로 함께 숨긴다 (hat_rim은 hat의 자식)
			float s = p ? 0f : 1f;
			try {
				hat.xScale = s;
				hat.yScale = s;
				hat.zScale = s;
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}
}
