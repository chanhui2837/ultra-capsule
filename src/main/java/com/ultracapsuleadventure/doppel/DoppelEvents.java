package com.ultracapsuleadventure.doppel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * 도플갱어 밤 스토킹 총괄.
 * - 플레이어별 스토킹 일수/임계값/활동체 관리 (서버 메모리 + 변장 태그 이중 저장)
 * - 밤 스폰, 낮 정리, 변장 교체, 매복 돌변, 처치 시 파란 점토 3개 드롭
 */
public class DoppelEvents {

	private static class Stalk {
		int days = 0;
		int threshold = 2;
		UUID activeId = null;
		int ambushIn = -1;
		int disguiseTick = 0;
		boolean initialized = false;
	}

	private static final Map<UUID, Stalk> STALKS = new HashMap<>();

	public static final String TAG_DISGUISE = "doppel_disguise";
	public static final String TAG_VICTIM_PREFIX = "doppel_victim-";

	private static final String[] DISGUISE_TYPES = {
			"minecraft:zombie", "minecraft:skeleton", "minecraft:creeper",
			"minecraft:spider", "minecraft:enderman", "minecraft:villager",
			"minecraft:cow", "minecraft:sheep", "minecraft:pig",
			"minecraft:husk", "minecraft:drowned", "minecraft:witch"
	};

	private static Stalk stalkOf(Player player) {
		Stalk s = STALKS.get(player.getUUID());
		if (s == null) {
			s = new Stalk();
			try {
				s.threshold = 2 + (int) (Math.random() * 2);
			} catch (Exception ignored) {
				s.threshold = 2;
			}
			STALKS.put(player.getUUID(), s);
		}
		return s;
	}

	public static void register() {
		// 처치 드롭: 도플갱어 본체 or 변장체를 죽이면 파란 점토 3개
		try {
			ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity dead, DamageSource source) -> {
				try {
					boolean isDoppel = dead instanceof DoppelgangerEntity;
					boolean isDisguise = false;
					try {
						isDisguise = dead.entityTags().contains(TAG_DISGUISE);
					} catch (Exception ignored) {
					}
					if (!isDoppel && !isDisguise) {
						// 피해자 사망 시 스토킹 활동체만 정리 (일수는 유지)
						if (dead instanceof ServerPlayer sp) {
							try {
								Stalk s = STALKS.get(sp.getUUID());
								if (s != null) {
									s.activeId = null;
									s.ambushIn = -1;
								}
							} catch (Exception ignored) {
							}
						}
						return;
					}
					if (!(dead.level() instanceof ServerLevel sl)) {
						return;
					}
					// 파란 점토 3개 드롭
					for (int i = 0; i < 3; i++) {
						try {
							ItemStack clay = new ItemStack(ModDoppel.BLUE_CLAY);
							ItemEntity drop = new ItemEntity(sl,
									dead.getX() + (sl.getRandom().nextDouble() - 0.5),
									dead.getY() + 0.8,
									dead.getZ() + (sl.getRandom().nextDouble() - 0.5), clay);
							drop.setDefaultPickUpDelay();
							sl.addFreshEntity(drop);
						} catch (Exception ignored) {
						}
					}
					try {
						sl.sendParticles(ParticleTypes.SOUL, dead.getX(), dead.getY() + 1, dead.getZ(),
								20, 0.4, 0.8, 0.4, 0.05);
						sl.playSound(null, dead.blockPosition(), SoundEvents.ENDERMAN_DEATH,
								SoundSource.HOSTILE, 1.2f, 0.5f);
					} catch (Exception ignored) {
					}
				// 스토킹 종료 (새 스토커가 나중에 옴)
				// 단, 추적 중인 본체가 맞을 때만 제거: 소울 아군 도플 사망이
				// 주인의 진행 중 스토킹을 날려버리면 안 된다.
				try {
					UUID victim = victimOf(dead);
					if (victim == null && dead instanceof DoppelgangerEntity dop) {
						victim = dop.getVictim();
					}
					boolean tracked = false;
					try {
						Stalk s = victim == null ? null : STALKS.get(victim);
						tracked = s != null && s.activeId != null && s.activeId.equals(dead.getUUID());
					} catch (Exception ignored) {
					}
					if (victim != null && tracked) {
						STALKS.remove(victim);
					} else if (victim == null) {
							// 못 찾으면 전수 탐색
							for (var en : new ArrayList<>(STALKS.entrySet())) {
								try {
									if (en.getValue() != null && dead.getUUID().equals(en.getValue().activeId)) {
										STALKS.remove(en.getKey());
										break;
									}
								} catch (Exception ignored) {
								}
							}
						}
					} catch (Exception ignored) {
					}
					try {
						Entity killer = source.getEntity();
						if (killer instanceof ServerPlayer sp) {
							sp.sendSystemMessage(Component.literal("도플갱어를 처치했다! 파란 점토 3개를 얻었다."));
						}
					} catch (Exception ignored) {
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}

		try {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				try {
					int tick = server.getTickCount();
					boolean slow = (tick % 100 == 0);
					boolean follow = (tick % 20 == 0);
					for (ServerPlayer player : server.getPlayerList().getPlayers()) {
						try {
							if (player.isCreative() || player.isSpectator() || !player.isAlive()) {
								continue;
							}
							if (!(player.level() instanceof ServerLevel sl)) {
								continue;
							}
							boolean overworld = false;
							try {
								overworld = sl.dimension() == Level.OVERWORLD;
							} catch (Exception ignored) {
							}
							if (!overworld) {
								continue;
							}
							Stalk s = stalkOf(player);
							// 활동체 생존 확인
							Entity active = findActive(server, s);
							if (slow) {
								tickSlow(server, sl, player, s, active);
							}
							if (follow) {
								tickFollow(sl, player, s, active);
							}
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
	}

	private static Entity findActive(net.minecraft.server.MinecraftServer server, Stalk s) {
		if (s == null || s.activeId == null) {
			return null;
		}
		try {
			for (ServerLevel level : server.getAllLevels()) {
				try {
					for (Entity e : level.getAllEntities()) {
						try {
							if (e.getUUID().equals(s.activeId) && e.isAlive() && !e.isRemoved()) {
								return e;
							}
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	/** 도플갱어 출현 최소 월드 일수 (마크 시간 기준) */
	public static final long WORLD_DAYS_REQUIRED = 20L;
	private static final long TICKS_PER_DAY = 24000L;

	/** 26.3 Clock API 기준 오버월드 시간 (구 getDayTime 대체) */
	private static long dayTime(ServerLevel level) {
		try {
			return level.getOverworldClockTime();
		} catch (Exception ignored) {
		}
		try {
			return level.getDefaultClockTime();
		} catch (Exception ignored) {
		}
		return 6000L;
	}

	/** 월드 일수 (0부터, 20일 = 480000틱) */
	public static long worldDay(ServerLevel level) {
		try {
			return Math.floorDiv(dayTime(level), TICKS_PER_DAY);
		} catch (Exception e) {
			return 0L;
		}
	}

	private static boolean isNight(ServerLevel sl) {
		try {
			long t = Math.floorMod(dayTime(sl), TICKS_PER_DAY);
			return t >= 13000L && t <= 23000L;
		} catch (Exception e) {
			return false;
		}
	}

	private static void tickSlow(net.minecraft.server.MinecraftServer server, ServerLevel sl,
			ServerPlayer player, Stalk s, Entity active) {
		boolean night = isNight(sl);
		if (!night) {
			// 낮: 스토크 단계 활동체는 일수+1 후 소멸, 매복 중은 계속 싸움
			if (active != null) {
				boolean ambush = false;
				try {
					if (active instanceof DoppelgangerEntity dop) {
						ambush = dop.isAmbush();
					}
				} catch (Exception ignored) {
				}
				// 변장체는 항상 스토크 취급
				if (!ambush) {
					try {
						long t = Math.floorMod(dayTime(sl), TICKS_PER_DAY);
						if (t < 500L) {
							s.days++;
							s.ambushIn = -1;
							s.activeId = null;
							try {
								sl.sendParticles(ParticleTypes.POOF, active.getX(), active.getY() + 1,
										active.getZ(), 12, 0.4, 0.8, 0.4, 0.05);
							} catch (Exception ignored) {
							}
							try {
								active.discard();
							} catch (Exception ignored) {
							}
							if (s.days >= 1) {
								try {
									player.sendSystemMessage(Component.literal(
											"어젯밤 누군가 지켜보는 기분이 들었다... (" + s.days + "일째)"));
								} catch (Exception ignored) {
								}
							}
						}
					} catch (Exception ignored) {
					}
				}
			}
			return;
		}
		// 밤 + 월드 20일 이상일 때만 출현
		if (active == null) {
			try {
				if (worldDay(sl) < WORLD_DAYS_REQUIRED) {
					return;
				}
			} catch (Exception ignored) {
				return;
			}
			// 스폰 확률: 첫날 25%/5초, 스토킹 중이면 확정 스폰
			boolean shouldSpawn = false;
			try {
				if (s.days <= 0) {
					shouldSpawn = sl.getRandom().nextFloat() < 0.25f;
				} else {
					shouldSpawn = sl.getRandom().nextFloat() < 0.85f;
				}
			} catch (Exception ignored) {
			}
			if (!shouldSpawn) {
				return;
			}
			// 너무 밝은 곳(침대 근처 낮?)이 아니라면 스폰
			DoppelgangerEntity dop = spawnStalker(sl, player);
			if (dop != null) {
				s.activeId = dop.getUUID();
				s.disguiseTick = 0;
				if (s.days >= s.threshold) {
					s.ambushIn = 600 + sl.getRandom().nextInt(1200);
				} else {
					s.ambushIn = -1;
				}
			}
			return;
		}
		// 활동체 있음
		boolean isDisguise = false;
		try {
			isDisguise = active.entityTags().contains(TAG_DISGUISE);
		} catch (Exception ignored) {
		}
		boolean ambush = false;
		try {
			if (active instanceof DoppelgangerEntity dop) {
				ambush = dop.isAmbush();
			}
		} catch (Exception ignored) {
		}
		if (ambush) {
			return;
		}
		// 석화된 활동체는 변장 교체하지 않는다 (동결 유지)
		try {
			if (com.ultracapsuleadventure.medusa.Petrify.isPetrified(active)) {
				return;
			}
		} catch (Exception ignored) {
		}
		// 매복 카운트다운
		if (s.days >= s.threshold && s.ambushIn > 0) {
			s.ambushIn -= 100;
			if (s.ambushIn <= 0) {
				triggerAmbush(server, sl, player, s, active);
				return;
			}
		}
		// 변장 교체 (스토크 중에만)
		s.disguiseTick += 100;
		if (s.disguiseTick < 600) {
			return;
		}
		s.disguiseTick = 0;
		try {
			float r = sl.getRandom().nextFloat();
			if (isDisguise) {
				// 30%로 정체 공개 (다시 도플갱어로)
				if (r < 0.30f) {
					undisguise(server, sl, player, s, active);
				}
			} else {
				// 45%로 변장
				if (r < 0.45f) {
					disguise(server, sl, player, s, active);
				}
			}
		} catch (Exception ignored) {
		}
	}

	/** 변장체/스토커의 매 틱 추적 유지 (20틱마다): 너무 멀면 몰래 순간이동 */
	private static void tickFollow(ServerLevel sl, ServerPlayer player, Stalk s, Entity active) {
		if (active == null || !active.isAlive()) {
			return;
		}
		// 석화된 스토커는 건드리지 않는다 (동결 유지, 순간이동 금지)
		try {
			if (com.ultracapsuleadventure.medusa.Petrify.isPetrified(active)) {
				return;
			}
		} catch (Exception ignored) {
		}
		try {
			boolean ambush = active instanceof DoppelgangerEntity dop && dop.isAmbush();
			if (ambush) {
				return;
			}
			double d2 = -1;
			try {
				d2 = active.distanceToSqr(player);
			} catch (Exception ignored) {
				return;
			}
			// 변장체는 AI가 엉뚱한 곳으로 가므로 24+ 면 몰래 재배치, 10- 유지
			if (d2 > 576.0) {
				BlockPos p = pickNear(player, sl, 10, 18);
				if (p != null) {
					try {
						sl.sendParticles(ParticleTypes.PORTAL, active.getX(), active.getY() + 1,
								active.getZ(), 10, 0.3, 0.6, 0.3, 0.05);
					} catch (Exception ignored) {
					}
					try {
						active.teleportTo(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
					} catch (Exception ignored) {
					}
				}
			}
			// 변장체가 피해자를 공격하려 들면 목표 해제
			try {
				if (active instanceof Mob mob) {
					try {
						if (mob.getTarget() == player) {
							mob.setTarget(null);
						}
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	private static DoppelgangerEntity spawnStalker(ServerLevel sl, ServerPlayer player) {
		try {
			BlockPos p = pickNear(player, sl, 14, 22);
			if (p == null) {
				return null;
			}
			EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.EVENT, true);
			Entity created = null;
			try {
				created = com.ultracapsuleadventure.entity.ModEntityTypes.DOPPELGANGER.create(sl, request);
			} catch (Exception ignored) {
			}
			if (!(created instanceof DoppelgangerEntity dop)) {
				return null;
			}
			try {
				dop.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
				dop.setVictim(player.getUUID());
				dop.setPhase(DoppelgangerEntity.STALK);
			} catch (Exception ignored) {
			}
			try {
				dop.setPersistenceRequired();
			} catch (Exception ignored) {
			}
			try {
				sl.addFreshEntityWithPassengers(dop);
			} catch (Exception e) {
				return null;
			}
			try {
				sl.sendParticles(ParticleTypes.PORTAL, dop.getX(), dop.getY() + 1, dop.getZ(),
						16, 0.4, 0.8, 0.4, 0.08);
				sl.playSound(null, dop.blockPosition(), SoundEvents.ENDERMAN_STARE,
						SoundSource.HOSTILE, 0.9f, 0.5f);
			} catch (Exception ignored) {
			}
			return dop;
		} catch (Exception e) {
			return null;
		}
	}

	private static BlockPos pickNear(ServerPlayer player, ServerLevel sl, int minR, int maxR) {
		try {
			for (int i = 0; i < 16; i++) {
				double a = sl.getRandom().nextDouble() * Math.PI * 2.0;
				double r = minR + sl.getRandom().nextDouble() * (maxR - minR);
				int x = (int) Math.floor(player.getX() + Math.cos(a) * r);
				int z = (int) Math.floor(player.getZ() + Math.sin(a) * r);
				int y = (int) Math.floor(player.getY());
				for (int dy = 6; dy >= -6; dy--) {
					BlockPos p = new BlockPos(x, y + dy, z);
					try {
						BlockState below = sl.getBlockState(p.below());
						if (!below.isSolidRender()) {
							continue;
						}
						// 도플갱어 키 2.3: 3칸 확보해야 질식 데미지가 안 들어간다
						if (!sl.getBlockState(p).isAir() || !sl.getBlockState(p.above()).isAir()
								|| !sl.getBlockState(p.above().above()).isAir()) {
							continue;
						}
						return p;
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	/** 스토커 → 주변 흔한 몹으로 변장 (진짜 바닐라 엔티티로 교체) */
	private static void disguise(net.minecraft.server.MinecraftServer server, ServerLevel sl,
			ServerPlayer player, Stalk s, Entity active) {
		try {
			String typeId = DISGUISE_TYPES[sl.getRandom().nextInt(DISGUISE_TYPES.length)];
			EntityType<?> type = null;
			try {
				type = BuiltInRegistries.ENTITY_TYPE
						.get(Identifier.parse(typeId)).map(h -> h.value()).orElse(null);
			} catch (Exception ignored) {
			}
			if (type == null) {
				return;
			}
			EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.EVENT, true);
			Entity created = null;
			try {
				created = type.create(sl, request);
			} catch (Exception ignored) {
			}
			if (!(created instanceof LivingEntity living)) {
				return;
			}
			try {
				created.setPos(active.getX(), active.getY(), active.getZ());
			} catch (Exception ignored) {
			}
			try {
				created.addTag(TAG_DISGUISE);
				created.addTag(TAG_VICTIM_PREFIX + player.getUUID().toString());
			} catch (Exception ignored) {
			}
			try {
				if (created instanceof Mob mob) {
					mob.setPersistenceRequired();
					mob.setTarget(null);
				}
			} catch (Exception ignored) {
			}
			try {
				sl.addFreshEntityWithPassengers(created);
			} catch (Exception e) {
				return;
			}
			try {
				sl.sendParticles(ParticleTypes.POOF, active.getX(), active.getY() + 1, active.getZ(),
						10, 0.3, 0.6, 0.3, 0.05);
			} catch (Exception ignored) {
			}
			try {
				active.discard();
			} catch (Exception ignored) {
			}
			s.activeId = created.getUUID();
		} catch (Exception ignored) {
		}
	}

	private static void undisguise(net.minecraft.server.MinecraftServer server, ServerLevel sl,
			ServerPlayer player, Stalk s, Entity active) {
		try {
			BlockPos p = pickNear(player, sl, 10, 16);
			if (p == null) {
				p = active.blockPosition();
			}
			EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.EVENT, true);
			Entity created = null;
			try {
				created = com.ultracapsuleadventure.entity.ModEntityTypes.DOPPELGANGER.create(sl, request);
			} catch (Exception ignored) {
			}
			if (!(created instanceof DoppelgangerEntity dop)) {
				return;
			}
			try {
				dop.setPos(p.getX() + 0.5, p.getY(), p.getZ() + 0.5);
				dop.setVictim(player.getUUID());
			} catch (Exception ignored) {
			}
			try {
				dop.setPersistenceRequired();
			} catch (Exception ignored) {
			}
			try {
				sl.addFreshEntityWithPassengers(dop);
			} catch (Exception e) {
				return;
			}
			try {
				active.discard();
			} catch (Exception ignored) {
			}
			s.activeId = dop.getUUID();
		} catch (Exception ignored) {
		}
	}

	/** 돌변: 변장 해제 후 플레이어 정면 3블록 앞에 본체 소환해 공격 개시 */
	private static void triggerAmbush(net.minecraft.server.MinecraftServer server, ServerLevel sl,
			ServerPlayer player, Stalk s, Entity active) {
		try {
			try {
				if (active != null) {
					active.discard();
				}
			} catch (Exception ignored) {
			}
			// 정면 3블록
			Vec3 look = player.getLookAngle();
			double fx = player.getX() + look.x * 3.0;
			double fz = player.getZ() + look.z * 3.0;
			BlockPos front = null;
			for (int dy = 3; dy >= -3; dy--) {
				BlockPos p = new BlockPos((int) Math.floor(fx), (int) Math.floor(player.getY()) + dy,
						(int) Math.floor(fz));
				try {
					BlockState below = sl.getBlockState(p.below());
					if (!below.isSolidRender()) {
						continue;
					}
					if (!sl.getBlockState(p).isAir() || !sl.getBlockState(p.above()).isAir()
							|| !sl.getBlockState(p.above().above()).isAir()) {
						continue;
					}
					front = p;
					break;
				} catch (Exception ignored) {
				}
			}
			if (front == null) {
				BlockPos p = pickNear(player, sl, 3, 5);
				front = p != null ? p : player.blockPosition();
			}
			EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.EVENT, true);
			Entity created = null;
			try {
				created = com.ultracapsuleadventure.entity.ModEntityTypes.DOPPELGANGER.create(sl, request);
			} catch (Exception ignored) {
			}
			if (!(created instanceof DoppelgangerEntity dop)) {
				return;
			}
			try {
				dop.setPos(front.getX() + 0.5, front.getY(), front.getZ() + 0.5);
				dop.setVictim(player.getUUID());
			} catch (Exception ignored) {
			}
			try {
				dop.setPersistenceRequired();
			} catch (Exception ignored) {
			}
			try {
				sl.addFreshEntityWithPassengers(dop);
			} catch (Exception e) {
				return;
			}
			dop.beginAmbush();
			try {
				if (dop instanceof Mob mob) {
					mob.setTarget(player);
				}
			} catch (Exception ignored) {
			}
			s.activeId = dop.getUUID();
			s.ambushIn = -1;
			try {
				player.sendSystemMessage(Component.literal("§4§l도플갱어가 돌변했다! 정면을 봐라!"));
				player.sendOverlayMessage(Component.literal("§c도망칠 수 없다"));
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	public static UUID victimOf(Entity e) {
		try {
			for (String tag : e.entityTags()) {
				if (tag.startsWith(TAG_VICTIM_PREFIX)) {
					try {
						return UUID.fromString(tag.substring(TAG_VICTIM_PREFIX.length()));
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	public static boolean isDisguise(Entity e) {
		try {
			return e != null && e.entityTags().contains(TAG_DISGUISE);
		} catch (Exception ex) {
			return false;
		}
	}

	/** 변장체가 스토킹 피해자를 노리지 못하게 (믹스인에서 사용) */
	public static boolean isMyVictim(Entity mob, LivingEntity target) {
		try {
			UUID v = victimOf(mob);
			if (v == null || target == null) {
				return false;
			}
			return v.equals(target.getUUID());
		} catch (Exception e) {
			return false;
		}
	}

	/** 디버그/관리용: 현재 스토킹 일수 */
	public static int stalkDays(Player player) {
		try {
			Stalk s = STALKS.get(player.getUUID());
			return s == null ? 0 : s.days;
		} catch (Exception e) {
			return 0;
		}
	}
}


