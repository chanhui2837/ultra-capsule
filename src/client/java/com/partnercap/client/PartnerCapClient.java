package com.partnercap.client;

import com.partnercap.item.PartnerCapsuleItem;
import com.partnercap.item.SoulCapsuleItem;
import com.partnercap.item.VoidCapsuleItem;
import com.partnercap.network.ModPackets;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.platform.InputConstants;

public class PartnerCapClient implements ClientModInitializer {

	public static KeyMapping openCapKey;
	public static KeyMapping soulToggleKey;
	public static KeyMapping flameBreathKey;
	private static boolean mechAtkDown = false;
	private static boolean mechJmpDown = false;

	@Override
	public void onInitializeClient() {
		openCapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.partnercap.open",
				InputConstants.KEY_C,
				KeyMapping.Category.MISC));
		soulToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.partnercap.soul_toggle",
				InputConstants.KEY_H,
				KeyMapping.Category.MISC));
		flameBreathKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.partnercap.flame_breath",
				InputConstants.KEY_F,
				KeyMapping.Category.MISC));

		try {
			net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(
					com.partnercap.fluid.ModFluids.VOID_STILL,
					com.partnercap.fluid.ModFluids.VOID_FLOWING,
					new net.minecraft.client.renderer.block.FluidModel.Unbaked(
							new net.minecraft.client.resources.model.sprite.Material(
									com.partnercap.PartnerCapMod.id("block/void_liquid_still")),
							new net.minecraft.client.resources.model.sprite.Material(
									com.partnercap.PartnerCapMod.id("block/void_liquid_flow")),
							new net.minecraft.client.resources.model.sprite.Material(
									com.partnercap.PartnerCapMod.id("block/void_liquid_overlay")),
							net.minecraft.client.color.block.BlockTintSources.constant(
									net.minecraft.util.ARGB.opaque(0xFFFFFF))));
		} catch (Exception ignored) {
		}
		try {
			net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
					com.partnercap.client.entity.GloomDevourerRenderer.LAYER,
					com.partnercap.client.entity.GloomDevourerModel::getTexturedModelData);
			net.minecraft.client.renderer.entity.EntityRenderers.register(
					com.partnercap.entity.ModEntityTypes.GLOOM_DEVOURER,
					com.partnercap.client.entity.GloomDevourerRenderer::new);
		} catch (Exception ignored) {
		}
		try {
			net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
					com.partnercap.client.entity.MechRobotRenderer.LAYER,
					com.partnercap.client.entity.MechRobotModel::getTexturedModelData);
			net.minecraft.client.renderer.entity.EntityRenderers.register(
					com.partnercap.entity.ModEntityTypes.MECH_ROBOT,
					com.partnercap.client.entity.MechRobotRenderer::new);
		} catch (Exception ignored) {
		}
		try {
			net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
					com.partnercap.client.entity.MedusaRenderer.LAYER,
					com.partnercap.client.entity.MedusaModel::getTexturedModelData);
			net.minecraft.client.renderer.entity.EntityRenderers.register(
					com.partnercap.entity.ModEntityTypes.MEDUSA,
					com.partnercap.client.entity.MedusaRenderer::new);
			net.minecraft.client.renderer.entity.EntityRenderers.register(
					com.partnercap.entity.ModEntityTypes.STATUE,
					com.partnercap.client.entity.StatueRenderer::new);
		} catch (Exception ignored) {
		}
		// 안대 착용 모델
		try {
			net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
					com.partnercap.client.entity.BlindfoldArmorRenderer.LAYER,
					com.partnercap.client.entity.BlindfoldModel::createLayer);
			net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register(
					context -> new com.partnercap.client.entity.BlindfoldArmorRenderer(context),
					com.partnercap.item.ModItems.BLINDFOLD);
		} catch (Exception ignored) {
		}
		// 화염몽
		try {
			net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
					com.partnercap.client.entity.FlameMongRenderer.LAYER,
					com.partnercap.client.entity.FlameMongModel::getTexturedModelData);
			net.minecraft.client.renderer.entity.EntityRenderers.register(
					com.partnercap.flame.ModFlame.FLAME_MONG,
					com.partnercap.client.entity.FlameMongRenderer::new);
		} catch (Exception ignored) {
		}

		ClientPlayNetworking.registerGlobalReceiver(ModPackets.OpenUtilityGuiPayload.TYPE, (payload, context) -> {			context.client().execute(() -> {
				try {
					if (payload.kind() == 0) {
						Minecraft.getInstance().gui.setScreen(new GambleCapScreen(payload.mainHand()));
					} else {
						Minecraft.getInstance().gui.setScreen(new DisassembleCapScreen(payload.mainHand()));
					}
				} catch (Exception ignored) {
				}
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// 석화체 보간 떨림 + 빨간 플래시 제거 (렌더러 무관, 매 틱 강제)
			try {
				if (client.level != null && client.player != null) {
					var box = client.player.getBoundingBox().inflate(64.0);
					for (var e : client.level.getEntities((net.minecraft.world.entity.Entity) null, box,
							x -> x instanceof net.minecraft.world.entity.LivingEntity)) {
						try {
							if (e instanceof com.partnercap.medusa.PetrifiedAccess access
									&& access.partnercap$isPetrified()) {
								e.setOldPosAndRot();
								if (e instanceof net.minecraft.world.entity.LivingEntity living) {
									living.yBodyRotO = living.yBodyRot;
									living.yHeadRotO = living.yHeadRot;
									living.hurtTime = 0;
									living.deathTime = 0;
								}
							}
						} catch (Exception ignored) {
						}
					}
				}
			} catch (Exception ignored) {
			}
			// 메카 탑승 중 좌클릭=레이저, 스페이스=점프 (서버 패킷)
			try {
				if (client.player != null
						&& client.player.getVehicle() instanceof com.partnercap.mech.MechRobotEntity) {
					boolean atk = false;
					boolean jmp = false;
					try {
						atk = client.options.keyAttack.isDown();
					} catch (Exception ignored) {
					}
					try {
						jmp = client.options.keyJump.isDown();
					} catch (Exception ignored) {
					}
					if (atk && !mechAtkDown) {
						try {
							ClientPlayNetworking.send(new ModPackets.MechFirePayload());
						} catch (Exception ignored) {
						}
					}
					mechAtkDown = atk;
					if (jmp && !mechJmpDown) {
						try {
							ClientPlayNetworking.send(new ModPackets.MechJumpPayload());
						} catch (Exception ignored) {
						}
					}
					mechJmpDown = jmp;
				} else {
					mechAtkDown = false;
					mechJmpDown = false;
				}
			} catch (Exception ignored) {
			}
		if (client.player == null) {
			while (openCapKey.consumeClick()) {
			}
			while (soulToggleKey.consumeClick()) {
			}
			return;
		}
		if (client.gui.screen() != null) {
			while (openCapKey.consumeClick()) {
			}
			while (soulToggleKey.consumeClick()) {
			}
			return;
		}
		// F 홀드: 화염 브레스 (빈손+피 효과일 때 서버가 판정)
		try {
			if (flameBreathKey.isDown() && client.player.getMainHandItem().isEmpty()
					&& client.player.getOffhandItem().isEmpty()) {
				try {
					ClientPlayNetworking.send(new ModPackets.FlameBreathPayload());
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		// H키: 몸속 소울 소환/저장 토글
		while (soulToggleKey.consumeClick()) {
			try {
				ClientPlayNetworking.send(new ModPackets.SoulBodyTogglePayload());
			} catch (Exception ignored) {
			}
		}
			while (openCapKey.consumeClick()) {
				ItemStack main = client.player.getMainHandItem();
				ItemStack off = client.player.getOffhandItem();
				ItemStack cap = null;
				PartnerCapScreen.Mode startMode = PartnerCapScreen.Mode.PARTNER;
				// 캡슐을 손에 들고 있을 때만 창이 뜸
				if (main.getItem() instanceof PartnerCapsuleItem) {
					cap = main;
				} else if (off.getItem() instanceof PartnerCapsuleItem) {
					cap = off;
				} else if (main.getItem() instanceof SoulCapsuleItem) {
					cap = main;
					startMode = PartnerCapScreen.Mode.SOUL;
				} else if (off.getItem() instanceof SoulCapsuleItem) {
					cap = off;
					startMode = PartnerCapScreen.Mode.SOUL;
				} else if (main.getItem() instanceof VoidCapsuleItem) {
					cap = main;
					startMode = PartnerCapScreen.Mode.VOID;
				} else if (off.getItem() instanceof VoidCapsuleItem) {
					cap = off;
					startMode = PartnerCapScreen.Mode.VOID;
				} else if (main.getItem() instanceof com.partnercap.titan.TitanCapsuleItem) {
					cap = main;
					startMode = PartnerCapScreen.Mode.TITAN;
				} else if (off.getItem() instanceof com.partnercap.titan.TitanCapsuleItem) {
					cap = off;
					startMode = PartnerCapScreen.Mode.TITAN;
				} else if (main.getItem() instanceof com.partnercap.item.GambleCapsuleItem) {
					cap = main;
					startMode = PartnerCapScreen.Mode.GAMBLE;
				} else if (off.getItem() instanceof com.partnercap.item.GambleCapsuleItem) {
					cap = off;
					startMode = PartnerCapScreen.Mode.GAMBLE;
				} else if (main.getItem() instanceof com.partnercap.item.DisassembleCapsuleItem) {
					cap = main;
					startMode = PartnerCapScreen.Mode.DIS;
				} else if (off.getItem() instanceof com.partnercap.item.DisassembleCapsuleItem) {
					cap = off;
					startMode = PartnerCapScreen.Mode.DIS;
				} else if (main.getItem() instanceof com.partnercap.mech.MechCapsuleItem) {
					cap = main;
					startMode = PartnerCapScreen.Mode.MECH;
				} else if (off.getItem() instanceof com.partnercap.mech.MechCapsuleItem) {
					cap = off;
					startMode = PartnerCapScreen.Mode.MECH;
				}
				if (cap == null) {
					continue;
				}
				PartnerCapScreen screen = new PartnerCapScreen(cap);
				screen.mode = startMode;
				Minecraft.getInstance().gui.setScreen(screen);
			}
		});
	}
}
