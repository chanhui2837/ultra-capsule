package com.ultracapsuleadventure.client;

import com.ultracapsuleadventure.item.UltraCapsuleAdventureItem;
import com.ultracapsuleadventure.item.SoulCapsuleItem;
import com.ultracapsuleadventure.item.VoidCapsuleItem;
import com.ultracapsuleadventure.network.ModPackets;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.platform.InputConstants;

public class UltraCapsuleAdventureClient implements ClientModInitializer {

	public static KeyMapping openCapKey;
	public static KeyMapping soulToggleKey;
	public static KeyMapping flameBreathKey;
	public static KeyMapping morphAbilityKey;
	private static boolean mechAtkDown = false;
	private static boolean mechJmpDown = false;
	/**
	 * 탑승 중 좌클릭 꾹 → 캡슐 복귀. 변신 충전({@code MechCapsuleItem.CHARGE_TICKS=60})과 동일한 게이지 UX:
	 * 10틱마다 "캡슐 복귀 ■□□"+음 상승, 60틱 완충 시 복귀 전송, 중간에 떼면 취소. 짧은 레이저連打와 충돌 없음.
	 */
	private static int mechHoldTicks = 0;
	private static boolean mechFoldSent = false;
	private static final int MECH_FOLD_HOLD_TICKS = 60;

	private static boolean rightClickWasDown = false;

	/** 우클릭 하면 바닐라가 상호작용(문·상자·주민 거래 등)을 하는 대상 — 이때는 능력 대신 바닐라 동작이 우선 */
	private static final String[] INTERACT_BLOCKS = { "door", "trapdoor", "gate", "button", "lever", "chest",
			"barrel", "furnace", "smoker", "crafting_table", "enchanting_table", "anvil", "_bed", "shulker_box",
			"hopper", "dropper", "dispenser", "brewing_stand", "beacon", "lectern", "loom", "grindstone",
			"stonecutter", "cartography_table", "smithing_table", "note_block", "jukebox", "repeater",
			"comparator", "daylight_detector", "composter", "cauldron", "bell", "sign", "campfire", "cake",
			"flower_pot", "beehive", "bee_nest", "respawn_anchor", "command_block", "crafter", "decorated_pot",
			"vault", "trial_spawner" };
	private static final java.util.Set<String> INTERACT_ENTITIES = java.util.Set.of("villager",
			"wandering_trader", "horse", "donkey", "mule", "skeleton_horse", "zombie_horse", "camel", "llama",
			"trader_llama", "item_frame", "glow_item_frame", "armor_stand", "allay");

	/** 맨손 우클릭으로 능력을 써도 되는 상황인가 */
	private static boolean canRightClickAbility(Minecraft client) {
		try {
			if (client.player == null || client.gui.screen() != null) {
				return false;
			}
			if (!client.player.getMainHandItem().isEmpty() || !client.player.getOffhandItem().isEmpty()) {
				return false;
			}
			if (client.player.isPassenger() || client.player.isUsingItem()) {
				return false;
			}
			var hit = client.hitResult;
			if (hit instanceof net.minecraft.world.phys.BlockHitResult bhr
					&& hit.getType() == net.minecraft.world.phys.HitResult.Type.BLOCK && client.level != null) {
				String path = net.minecraft.core.registries.BuiltInRegistries.BLOCK
						.getKey(client.level.getBlockState(bhr.getBlockPos()).getBlock()).getPath();
				for (String n : INTERACT_BLOCKS) {
					if (path.contains(n)) {
						return false;
					}
				}
			} else if (hit instanceof net.minecraft.world.phys.EntityHitResult ehr) {
				var e = ehr.getEntity();
				String path = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE
						.getKey(e.getType()).getPath();
				if (INTERACT_ENTITIES.contains(path) || path.contains("boat") || path.contains("raft")
						|| path.contains("minecart")) {
					return false;
				}
				if (e instanceof net.minecraft.world.entity.TamableAnimal t && t.isTame()) {
					return false;
				}
			}
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public void onInitializeClient() {
		openCapKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.UltraCapsuleAdventure.open",
				InputConstants.KEY_C,
				KeyMapping.Category.MISC));
		soulToggleKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.UltraCapsuleAdventure.soul_toggle",
				InputConstants.KEY_H,
				KeyMapping.Category.MISC));
		flameBreathKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.UltraCapsuleAdventure.flame_breath",
				InputConstants.KEY_F,
				KeyMapping.Category.MISC));
		morphAbilityKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
				"key.UltraCapsuleAdventure.morph_ability",
				InputConstants.KEY_R,
				KeyMapping.Category.MISC));

		try {
			net.fabricmc.fabric.api.client.render.fluid.v1.FluidRenderingRegistry.register(
					com.ultracapsuleadventure.fluid.ModFluids.VOID_STILL,
					com.ultracapsuleadventure.fluid.ModFluids.VOID_FLOWING,
					new net.minecraft.client.renderer.block.FluidModel.Unbaked(
							new net.minecraft.client.resources.model.sprite.Material(
									com.ultracapsuleadventure.UltraCapsuleAdventureMod.id("block/void_liquid_still")),
							new net.minecraft.client.resources.model.sprite.Material(
									com.ultracapsuleadventure.UltraCapsuleAdventureMod.id("block/void_liquid_flow")),
							new net.minecraft.client.resources.model.sprite.Material(
									com.ultracapsuleadventure.UltraCapsuleAdventureMod.id("block/void_liquid_overlay")),
							net.minecraft.client.color.block.BlockTintSources.constant(
									net.minecraft.util.ARGB.opaque(0xFFFFFF))));
		} catch (Exception ignored) {
		}
		try {
			net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
					com.ultracapsuleadventure.client.entity.GloomDevourerRenderer.LAYER,
					com.ultracapsuleadventure.client.entity.GloomDevourerModel::getTexturedModelData);
			net.minecraft.client.renderer.entity.EntityRenderers.register(
					com.ultracapsuleadventure.entity.ModEntityTypes.GLOOM_DEVOURER,
					com.ultracapsuleadventure.client.entity.GloomDevourerRenderer::new);
		} catch (Exception ignored) {
		}
		try {
			net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
					com.ultracapsuleadventure.client.entity.MechRobotRenderer.LAYER,
					com.ultracapsuleadventure.client.entity.MechRobotModel::getTexturedModelData);
			net.minecraft.client.renderer.entity.EntityRenderers.register(
					com.ultracapsuleadventure.entity.ModEntityTypes.MECH_ROBOT,
					com.ultracapsuleadventure.client.entity.MechRobotRenderer::new);
		} catch (Exception ignored) {
		}
		try {
			net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
					com.ultracapsuleadventure.client.entity.MedusaRenderer.LAYER,
					com.ultracapsuleadventure.client.entity.MedusaModel::getTexturedModelData);
			net.minecraft.client.renderer.entity.EntityRenderers.register(
					com.ultracapsuleadventure.entity.ModEntityTypes.MEDUSA,
					com.ultracapsuleadventure.client.entity.MedusaRenderer::new);
			net.minecraft.client.renderer.entity.EntityRenderers.register(
					com.ultracapsuleadventure.entity.ModEntityTypes.STATUE,
					com.ultracapsuleadventure.client.entity.StatueRenderer::new);
		} catch (Exception ignored) {
		}
		// 안대 착용 모델
		try {
			net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
					com.ultracapsuleadventure.client.entity.BlindfoldArmorRenderer.LAYER,
					com.ultracapsuleadventure.client.entity.BlindfoldModel::createLayer);
			net.fabricmc.fabric.api.client.rendering.v1.ArmorRenderer.register(
					context -> new com.ultracapsuleadventure.client.entity.BlindfoldArmorRenderer(context),
					com.ultracapsuleadventure.item.ModItems.BLINDFOLD);
		} catch (Exception ignored) {
		}
		// 화염몽
		try {
			net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
					com.ultracapsuleadventure.client.entity.FlameMongRenderer.LAYER,
					com.ultracapsuleadventure.client.entity.FlameMongModel::getTexturedModelData);
			net.minecraft.client.renderer.entity.EntityRenderers.register(
					com.ultracapsuleadventure.flame.ModFlame.FLAME_MONG,
					com.ultracapsuleadventure.client.entity.FlameMongRenderer::new);
		} catch (Exception ignored) {
		}
		// 도플갱어
		try {
			net.fabricmc.fabric.api.client.rendering.v1.ModelLayerRegistry.registerModelLayer(
					com.ultracapsuleadventure.client.entity.DoppelgangerRenderer.LAYER,
					com.ultracapsuleadventure.client.entity.DoppelgangerModel::getTexturedModelData);
			net.minecraft.client.renderer.entity.EntityRenderers.register(
					com.ultracapsuleadventure.entity.ModEntityTypes.DOPPELGANGER,
					com.ultracapsuleadventure.client.entity.DoppelgangerRenderer::new);
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

		// 서버 접속 해제 시 모프 캐시·더미 전부 정리 (다른 월드 유령 변신 방지)
		try {
			net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents.DISCONNECT.register(
					(handler, client) -> {
						try {
							com.ultracapsuleadventure.morph.MorphSyncCache.clear();
						} catch (Exception ignored) {
						}
						try {
							com.ultracapsuleadventure.client.MorphClient.clear();
						} catch (Exception ignored) {
						}
					});
		} catch (Exception ignored) {
		}

		// 모프 상태 수신 → 클라 캐시 (렌더 스왑·치수 믹스인이 참조)
		ClientPlayNetworking.registerGlobalReceiver(ModPackets.MorphSyncPayload.TYPE, (payload, context) -> {
			String suuid = payload.playerUuid();
			String mid = payload.morphId();
			context.client().execute(() -> {
				try {
					java.util.UUID id = java.util.UUID.fromString(suuid);
					String prev = com.ultracapsuleadventure.morph.MorphSyncCache.get(id);
					String a = prev == null ? "" : prev;
					String b = mid == null ? "" : mid;
					com.ultracapsuleadventure.morph.MorphSyncCache.set(id, mid);
					// 변신/해제 즉시 히트박스·눈높이 갱신 (기존엔 20틱마다, 해제 땐 아예 갱신 안 함)
					if (!a.equals(b)) {
						try {
							var lvl = context.client().level;
							if (lvl != null) {
								for (var pl : lvl.players()) {
									if (pl.getUUID().equals(id)) {
										pl.refreshDimensions();
									}
								}
							}
							if (b.isEmpty()) {
								com.ultracapsuleadventure.client.MorphClient.clearPlayer(id);
							}
						} catch (Exception ignored) {
						}
					}
					if (!a.equals(b)) {
						com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.info("[Morph] sync player=" + suuid + " morph=" + b);
					}
				} catch (Exception ignored) {
				}
			});
		});

		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			// 변신 중 히트박스/눈높이 클라 동기화 (브로드캐스트 캐시 → getDimensions 믹스인)
			try {
				if (client.level != null && client.player != null && client.player.tickCount % 20 == 0) {
					for (var p : client.level.players()) {
						try {
							if (!com.ultracapsuleadventure.morph.MorphEvents.morphId(p).isEmpty()) {
								p.refreshDimensions();
							}
						} catch (Exception ignored) {
						}
					}
				}
			} catch (Exception ignored) {
			}
			// 석화체 보간 떨림 + 빨간 플래시 제거 (렌더러 무관, 매 틱 강제)
			try {
				if (client.level != null && client.player != null) {
					var box = client.player.getBoundingBox().inflate(64.0);
					for (var e : client.level.getEntities((net.minecraft.world.entity.Entity) null, box,
							x -> x instanceof net.minecraft.world.entity.LivingEntity)) {
						try {
							if (e instanceof com.ultracapsuleadventure.medusa.PetrifiedAccess access
									&& access.UltraCapsuleAdventure$isPetrified()) {
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
			// + 좌클릭 꾹 60틱=캡슐 복귀. 변신 충전(MechCapsuleItem.onUseTick)과 동일한 게이지 UX:
			// 10틱마다 "캡슐 복귀 ■□□"+음 상승, 완충 시 복귀, 중간에 떼면 취소.
			try {
				if (client.player != null
						&& client.player.getVehicle() instanceof com.ultracapsuleadventure.mech.MechRobotEntity) {
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
					try {
						if (atk && !mechFoldSent) {
							mechHoldTicks++;
							// 변신 충전과 동일: 10틱마다 게이지+음 상승 (60틱 미만까지만)
							if (mechHoldTicks % 10 == 0 && mechHoldTicks < MECH_FOLD_HOLD_TICKS) {
								try {
									int k = (int) (12.0 * mechHoldTicks / MECH_FOLD_HOLD_TICKS);
									StringBuilder bar = new StringBuilder("캡슐 복귀 ");
									for (int i = 0; i < 12; i++) {
										bar.append(i < k ? "■" : "□");
									}
									client.player.sendOverlayMessage(
											net.minecraft.network.chat.Component.literal(bar.toString()));
								} catch (Exception ignored) {
								}
								try {
									float pitch = 0.5f + 1.0f * (mechHoldTicks / (float) MECH_FOLD_HOLD_TICKS);
									client.player.playSound(
											net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, 0.8f, pitch);
								} catch (Exception ignored) {
								}
							}
							if (mechHoldTicks >= MECH_FOLD_HOLD_TICKS) {
								try {
									ClientPlayNetworking.send(new ModPackets.MechFoldPayload());
								} catch (Exception ignored) {
								}
								mechFoldSent = true;
							}
						} else if (!atk) {
							// 변신 충전 취소(releaseUsing "충전 취소됨")와 동일: 게이지 띄웠는데 완충 전이면 취소 표시.
							// 짧은 레이저 클릭(10틱 미만)은 스팸 방지로 표시 안 함.
							try {
								if (!mechFoldSent && mechHoldTicks >= 10
										&& mechHoldTicks < MECH_FOLD_HOLD_TICKS && client.player != null) {
									client.player.sendOverlayMessage(
											net.minecraft.network.chat.Component.literal("복귀 취소됨"));
								}
							} catch (Exception ignored) {
							}
							mechHoldTicks = 0;
							mechFoldSent = false;
						}
					} catch (Exception ignored) {
					}
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
					mechHoldTicks = 0;
					mechFoldSent = false;
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
		// R키: 변신 능력 (엔더맨 순간이동은 누를 때 1회, 투사체형은 꾹 누르면 연사 - 쿨다운은 서버 판정)
		try {
			String mm = com.ultracapsuleadventure.morph.MorphEvents.morphId(client.player);
			// 우클릭: 양손이 비어 있으면 R키와 같은 변신 능력 사용 (엔더맨은 누를 때 1회, 나머지는 꾹 누르면 연사)
			boolean useDown = false;
			try {
				useDown = client.options.keyUse.isDown();
			} catch (Exception ignored) {
			}
			if (mm != null && !mm.isEmpty() && useDown && canRightClickAbility(client)) {
				if (!mm.equals("minecraft:enderman") || !rightClickWasDown) {
					ClientPlayNetworking.send(new ModPackets.MorphAbilityPayload());
				}
			}
			rightClickWasDown = useDown;
			if (mm != null && !mm.isEmpty()) {
				if (mm.equals("minecraft:enderman")) {
					while (morphAbilityKey.consumeClick()) {
						ClientPlayNetworking.send(new ModPackets.MorphAbilityPayload());
					}
				} else {
					while (morphAbilityKey.consumeClick()) {
					}
					if (morphAbilityKey.isDown()) {
						ClientPlayNetworking.send(new ModPackets.MorphAbilityPayload());
					}
				}
			} else {
				while (morphAbilityKey.consumeClick()) {
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
				UltraCapsuleAdventureScreen.Mode startMode = UltraCapsuleAdventureScreen.Mode.PARTNER;
				// 캡슐을 손에 들고 있을 때만 창이 뜸
				if (main.getItem() instanceof UltraCapsuleAdventureItem) {
					cap = main;
				} else if (off.getItem() instanceof UltraCapsuleAdventureItem) {
					cap = off;
				} else if (main.getItem() instanceof SoulCapsuleItem) {
					cap = main;
					startMode = UltraCapsuleAdventureScreen.Mode.SOUL;
				} else if (off.getItem() instanceof SoulCapsuleItem) {
					cap = off;
					startMode = UltraCapsuleAdventureScreen.Mode.SOUL;
				} else if (main.getItem() instanceof VoidCapsuleItem) {
					cap = main;
					startMode = UltraCapsuleAdventureScreen.Mode.VOID;
				} else if (off.getItem() instanceof VoidCapsuleItem) {
					cap = off;
					startMode = UltraCapsuleAdventureScreen.Mode.VOID;
				} else if (main.getItem() instanceof com.ultracapsuleadventure.titan.TitanCapsuleItem) {
					cap = main;
					startMode = UltraCapsuleAdventureScreen.Mode.TITAN;
				} else if (off.getItem() instanceof com.ultracapsuleadventure.titan.TitanCapsuleItem) {
					cap = off;
					startMode = UltraCapsuleAdventureScreen.Mode.TITAN;
				} else if (main.getItem() instanceof com.ultracapsuleadventure.item.GambleCapsuleItem) {
					cap = main;
					startMode = UltraCapsuleAdventureScreen.Mode.GAMBLE;
				} else if (off.getItem() instanceof com.ultracapsuleadventure.item.GambleCapsuleItem) {
					cap = off;
					startMode = UltraCapsuleAdventureScreen.Mode.GAMBLE;
				} else if (main.getItem() instanceof com.ultracapsuleadventure.item.DisassembleCapsuleItem) {
					cap = main;
					startMode = UltraCapsuleAdventureScreen.Mode.DIS;
				} else if (off.getItem() instanceof com.ultracapsuleadventure.item.DisassembleCapsuleItem) {
					cap = off;
					startMode = UltraCapsuleAdventureScreen.Mode.DIS;
				} else if (main.getItem() instanceof com.ultracapsuleadventure.mech.MechCapsuleItem) {
					cap = main;
					startMode = UltraCapsuleAdventureScreen.Mode.MECH;
				} else if (off.getItem() instanceof com.ultracapsuleadventure.mech.MechCapsuleItem) {
					cap = off;
					startMode = UltraCapsuleAdventureScreen.Mode.MECH;
				} else if (main.getItem() instanceof com.ultracapsuleadventure.morph.StealthCapsuleItem) {
					cap = main;
					startMode = UltraCapsuleAdventureScreen.Mode.STEALTH;
				} else if (off.getItem() instanceof com.ultracapsuleadventure.morph.StealthCapsuleItem) {
					cap = off;
					startMode = UltraCapsuleAdventureScreen.Mode.STEALTH;
				}
				if (cap == null) {
					continue;
				}
				UltraCapsuleAdventureScreen screen = new UltraCapsuleAdventureScreen(cap);
				screen.mode = startMode;
				Minecraft.getInstance().gui.setScreen(screen);
			}
		});
	}
}



