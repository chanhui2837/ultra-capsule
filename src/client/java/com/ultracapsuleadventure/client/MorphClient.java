package com.ultracapsuleadventure.client;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import net.minecraft.client.Minecraft;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

/**
 * 변신 모습 클라 렌더용 더미 관리 (Walkers의 shape entity와 같은 역할).
 * 서버 전송 없이 각 클라가 동기화된 모프 ID로 직접 생성·갱신한다.
 * EntityPreview와 같은 생성 경로라 GUI 미리보기와 완전히 같은 모습이 나온다.
 */
public class MorphClient {

	/** LevelExtractor.extractPlayerState 실행 중 (여기선 반드시 AvatarRenderState 가 필요해 스왑 금지) */
	public static volatile boolean extractingPlayerState = false;

	/** 인벤토리 인형용 createRenderState 가로채기 재진입 방지 */
	public static volatile boolean inGuiSwap = false;

	private static Boolean armHook = null;

	/**
	 * 1인칭 팔 교체 믹스인(AvatarHandMorphMixin)이 실제로 적용됐는지 (병합된 핸들러 메서드 존재로 확인).
	 * 미적용이면 FirstPersonHandsMorphMixin 이 옛 방식(손 통째로 숨김)으로 폴백한다.
	 */
	public static boolean armHookApplied() {
		if (armHook == null) {
			boolean ok = false;
			try {
				Class<?> c = Class.forName("net.minecraft.client.renderer.entity.player.AvatarRenderer");
				for (java.lang.reflect.Method m : c.getDeclaredMethods()) {
					if (m.getName().contains("UltraCapsuleAdventure$")) {
						ok = true;
						break;
					}
				}
			} catch (Throwable ignored) {
			}
			armHook = ok;
			com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.info("[Morph] 1인칭 팔 교체 훅 적용=" + ok
					+ (ok ? "" : " (미적용: 1인칭 손 전체 숨김 폴백)"));
		}
		return armHook;
	}

	private static final Map<UUID, Entity> DUMMIES = new HashMap<>();
	private static final Map<UUID, String> TYPES = new HashMap<>();
	private static final AtomicInteger NEXT_ID = new AtomicInteger(-2000000);
	private static final Map<String, Long> FAIL_WARN = new HashMap<>();
	private static final Map<String, Boolean> SWAP_LOGGED = new HashMap<>();

	private static java.lang.reflect.Field SWING_STATE = null;
	private static boolean swingSearched = false;
	private static final Map<String, Long> ACTIVE_WARN = new HashMap<>();
	private static final Map<UUID, Integer> SWING_TICK = new HashMap<>();
	private static java.lang.reflect.Field[] WALK_FIELDS = null;

	/** 렌더 직전 호출: 더미를 가져오고(없으면 생성) 플레이어 동작을 복사한다 */
	public static Entity dummyFor(Player player, String typeId) {
		Minecraft mc = Minecraft.getInstance();
		if (mc.level == null || typeId == null || typeId.isEmpty()) {
			return null;
		}
		UUID uuid = player.getUUID();
		Entity dummy = DUMMIES.get(uuid);
		String known = TYPES.get(uuid);
		boolean stale = dummy == null || !typeId.equals(known) || dummy.isRemoved();
		try {
			if (!stale && dummy.level() != mc.level) {
				stale = true;
			}
		} catch (Exception ignored) {
		}
		if (stale) {
			dummy = create(mc.level, typeId);
			if (dummy == null) {
				try {
					long now = System.currentTimeMillis();
					Long last = FAIL_WARN.get(typeId);
					if (last == null || now - last > 15000L) {
						FAIL_WARN.put(typeId, now);
						com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.warn("[Morph] dummy 생성 실패 type=" + typeId);
					}
				} catch (Exception ignored) {
				}
				return null;
			}
			DUMMIES.put(uuid, dummy);
			TYPES.put(uuid, typeId);
		try {
			if (!SWAP_LOGGED.containsKey(typeId)) {
				SWAP_LOGGED.put(typeId, Boolean.TRUE);
				com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.info("[Morph] render swap OK type=" + typeId);
			}
			long now = System.currentTimeMillis();
			Long last = ACTIVE_WARN.get(typeId);
			if (last == null || now - last > 60000L) {
				ACTIVE_WARN.put(typeId, now);
				com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.info("[Morph] render swap active type=" + typeId);
			}
		} catch (Exception ignored) {
		}
	}
		try {
			syncFrom(dummy, player);
		} catch (Exception ignored) {
		}
		return dummy;
	}

	private static Entity create(Level level, String typeId) {
		try {
			if (BuiltInRegistries.ENTITY_TYPE
					.get(Identifier.parse(typeId)).map(h -> h.value()).orElse(null) == null) {
				return null;
			}
			CompoundTag tag = new CompoundTag();
			tag.putString("id", typeId);
			EntitySpawnRequest req = new EntitySpawnRequest(EntitySpawnReason.LOAD, false);
			Entity created = EntityType.loadEntityRecursive(tag, level, req, EntityProcessor.NOP);
			if (created == null) {
				return null;
			}
			try {
				created.setId(NEXT_ID.getAndDecrement());
			} catch (Exception ignored) {
			}
			return created;
		} catch (Exception e) {
			return null;
		}
	}

	/** Walkers의 EntityUpdater와 같은 역할: 플레이어 움직임을 더미에 복사 */
	static void syncFrom(Entity dummy, Player player) {
		tickDummy(dummy, player);
		try {
			dummy.setPos(player.getX(), player.getY(), player.getZ());
			// 더미는 tick을 안 돌려 xOld/yOld/zOld가 생성 위치(0,0,0)에 고정돼 있었다.
			// 렌더러는 lerp(xOld, x, partialTick)로 위치를 계산하므로 프레임마다 위치가 튀어 모습이 끊겨 보였다.
			dummy.xOld = player.xOld;
			dummy.yOld = player.yOld;
			dummy.zOld = player.zOld;
		} catch (Exception ignored) {
		}
		try {
			dummy.setYRot(player.getYRot());
			dummy.setXRot(player.getXRot());
			dummy.yRotO = player.yRotO;
			dummy.xRotO = player.xRotO;
		} catch (Exception ignored) {
		}
		try {
			dummy.tickCount = player.tickCount;
		} catch (Exception ignored) {
		}
		try {
			if (dummy instanceof LivingEntity dl2) {
				copyWalk(player, dl2);
			}
		} catch (Exception ignored) {
		}
		try {
			dummy.setPose(player.getPose());
		} catch (Exception ignored) {
		}
		try {
			dummy.setRemainingFireTicks(player.getRemainingFireTicks());
		} catch (Exception ignored) {
		}
		try {
			dummy.setSprinting(player.isSprinting());
		} catch (Exception ignored) {
		}
		try {
			dummy.setShiftKeyDown(player.isCrouching());
		} catch (Exception ignored) {
		}
		if (dummy instanceof LivingEntity dl) {
			try {
				dl.yHeadRot = player.getYHeadRot();
				dl.yBodyRot = player.yBodyRot;
				dl.yHeadRotO = player.yHeadRotO;
				dl.yBodyRotO = player.yBodyRotO;
			} catch (Exception ignored) {
			}
			try {
				dl.hurtTime = player.hurtTime;
				dl.deathTime = player.deathTime;
			} catch (Exception ignored) {
			}
			// 손에 든 것 복사 (좀비가 든 검도 보이게)
			try {
				if (player instanceof LivingEntity pl) {
					try {
						dl.setItemSlot(net.minecraft.world.entity.EquipmentSlot.MAINHAND,
								pl.getMainHandItem().copy());
					} catch (Exception ignored) {
					}
					try {
						dl.setItemSlot(net.minecraft.world.entity.EquipmentSlot.OFFHAND,
								pl.getOffhandItem().copy());
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			// 발광·투명 복사
			try {
				dummy.setGlowingTag(player.hasGlowingTag());
			} catch (Exception ignored) {
			}
			try {
				dummy.setInvisible(player.isInvisible());
			} catch (Exception ignored) {
			}
			copySwing(player, dl);
		}
	}

	private static final Map<UUID, Integer> DUMMY_TICK = new HashMap<>();

	/**
	 * 더미를 게임 틱당 1회 돌려 몹 고유 애니메이션(닭 날갯짓, 박쥐, 오징어, 슬라임 찌그러짐, 입자 등)을 살린다.
	 * 물리는 끄고(noPhysics) 이동·접지 상태는 플레이어 값을 그대로 쓴다.
	 */
	private static void tickDummy(Entity dummy, Player player) {
		try {
			Integer last = DUMMY_TICK.get(player.getUUID());
			if (last != null && last == player.tickCount) {
				return;
			}
			DUMMY_TICK.put(player.getUUID(), player.tickCount);
			dummy.noPhysics = true;
			dummy.setOnGround(player.onGround());
			dummy.setDeltaMovement(player.getDeltaMovement());
			dummy.tick();
			// 토끼는 점프 이동: 땅에서 움직이면 깡충 모션 시작 (진행 중이면 다시 시작하지 않음)
			if (dummy instanceof net.minecraft.world.entity.animal.rabbit.Rabbit rabbit) {
				net.minecraft.world.phys.Vec3 dm = player.getDeltaMovement();
				boolean moving = dm.x * dm.x + dm.z * dm.z > 1.0E-4;
				if (player.onGround() && moving && rabbit.getJumpCompletion(0.99f) <= 0f) {
					rabbit.startJumping();
				}
			}
		} catch (Exception ignored) {
		}
	}

	/** 걷기 애니메이션(speed/speedOld/position/positionScale)을 플레이어 값 그대로 복사 */
	private static void copyWalk(LivingEntity player, LivingEntity dummy) {
		try {
			if (WALK_FIELDS == null) {
				java.util.List<java.lang.reflect.Field> list = new java.util.ArrayList<>();
				for (java.lang.reflect.Field f : player.walkAnimation.getClass().getDeclaredFields()) {
					if (!java.lang.reflect.Modifier.isStatic(f.getModifiers()) && f.getType() == float.class) {
						f.setAccessible(true);
						list.add(f);
					}
				}
				WALK_FIELDS = list.toArray(new java.lang.reflect.Field[0]);
			}
			for (java.lang.reflect.Field f : WALK_FIELDS) {
				f.setFloat(dummy.walkAnimation, f.getFloat(player.walkAnimation));
			}
		} catch (Exception ignored) {
		}
	}

	/**
	 * 공격 스윙 복사 (26.3 SwingState). 더미는 tick을 안 돌리므로
	 * 스윙 시작 mirrored + 상태 전진을 여기서 직접 수행한다.
	 */
	private static void copySwing(Entity player, LivingEntity dummy) {
		try {
			if (!(player instanceof LivingEntity pl)) {
				return;
			}
			boolean ps = false;
			try {
				ps = pl.isSwinging();
			} catch (Exception ignored) {
			}
			boolean ds = false;
			try {
				ds = dummy.isSwinging();
			} catch (Exception ignored) {
			}
			if (ps && !ds) {
				try {
					var cur = pl.getCurrentSwing();
					if (cur != null) {
						dummy.swing(cur.hand(), cur.animation(), false);
					}
				} catch (Exception ignored) {
				}
			}
			// 스윙 상태 전진 (private swingState에 리플렉션 접근) - 렌더 프레임이 아니라 게임 틱당 1회만
			try {
				Integer lastTick = SWING_TICK.get(player.getUUID());
				if (lastTick != null && lastTick == player.tickCount) {
					return;
				}
				SWING_TICK.put(player.getUUID(), player.tickCount);
				if (!swingSearched) {
					swingSearched = true;
					try {
						java.lang.reflect.Field f = LivingEntity.class.getDeclaredField("swingState");
						f.setAccessible(true);
						SWING_STATE = f;
					} catch (Exception ignored) {
					}
				}
				if (SWING_STATE != null) {
					Object st = SWING_STATE.get(dummy);
					if (st != null) {
						try {
							st.getClass().getMethod("tick").invoke(st);
						} catch (Exception ignored) {
						}
					}
				}
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	/** 메모리 정리 (월드 변경 시 호출) */
	public static void clear() {
		MorphArmRenderer.clear();
		DUMMIES.clear();
		TYPES.clear();
		SWING_TICK.clear();
		DUMMY_TICK.clear();
	}

	/** 한 플레이어의 잔상 더미만 제거 */
	public static void clearPlayer(UUID uuid) {
		try {
			if (uuid == null) {
				return;
			}
			DUMMIES.remove(uuid);
			TYPES.remove(uuid);
		} catch (Exception ignored) {
		}
	}
}


