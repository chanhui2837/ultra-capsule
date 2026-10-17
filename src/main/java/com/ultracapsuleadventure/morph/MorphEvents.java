package com.ultracapsuleadventure.morph;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 은신 변신(모프) 총괄.
 * - 황혼의 검 처치 → 은신 캡슐에 등록
 * - 캡슐 우클릭 → 변신/복귀
 * - 변신 중 매 틱: 체력·공격·비행·호흡·면역 복사(이동속도는 플레이어 유지) + 꼭두각시(진짜 몹) 동기화
 * - 같은 종/동족은 선공 전까지 중립 (MobTargetMorphMixin + 여기서 데미지 취소)
 */
public class MorphEvents {

	/**
	 * 서버 권위 변신 상태 (유일한 서버 진실).
	 * Player EntityData에는 빈 슬롯이 없어(최대 21, 바닐라가 전부 사용) 추가할 수 없으니
	 * 클라 동기화는 브로드캐스트 패킷(MorphSyncCache)만 쓴다.
	 */
	private static final Map<UUID, String> SERVER_MORPH = new java.util.concurrent.ConcurrentHashMap<>();

	public static String serverMorphId(UUID playerId) {
		try {
			String v = SERVER_MORPH.get(playerId);
			return v == null ? "" : v;
		} catch (Exception e) {
			return "";
		}
	}

	public static void setServerMorph(UUID playerId, String morphId) {
		try {
			if (morphId == null || morphId.isEmpty()) {
				SERVER_MORPH.remove(playerId);
			} else {
				SERVER_MORPH.put(playerId, morphId);
			}
		} catch (Exception ignored) {
		}
	}

	public static final String TAG_PREFIX = "pmorph:";

	/** 플레이어 → 마지막으로 몹을 때린 시각(ms). 중립 해제 판정용 */
	private static final Map<UUID, Long> LAST_ATTACK = new HashMap<>();
	/** 플레이어 → 웅크리기 누적 (크리퍼 자폭용) */
	private static final Map<UUID, Integer> CROUCH_TICKS = new HashMap<>();
	/** 플레이어 → 마지막 투사체 시각(ms) */
	private static final Map<UUID, Long> LAST_SHOOT = new HashMap<>();
	/** 플레이어 → 크리퍼 자폭 쿨다운 만료 시각(ms) */
	private static final Map<UUID, Long> CREEPER_COOL = new HashMap<>();

	private static class Attrs {
		double maxHealth = 20.0;
		double moveSpeed = 0.1;
		double attack = 1.0;
		double armor = 0.0;
		double knockbackResist = 0.0;
		double jump = 0.42;
		boolean fireImmune = false;
	}

	/** 변신 전 점프력 (복귀 시 복원용) */
	private static final Map<UUID, Double> PRE_JUMP = new HashMap<>();

	private static final Map<String, Attrs> ATTR_CACHE = new HashMap<>();
	/** 축축함(돌고래·아홀로틀 육지 유예) 남은 틱 */
	private static final Map<UUID, Long> MOIST = new HashMap<>();
	private static final long MOIST_MAX = 2400L;

	// ---------- 분류표 (참고 모드 mobsupport + 현 버전 바닐라 동작 기준) ----------

	private static final Set<String> FLYERS = new HashSet<>(Set.of(
			"minecraft:bat", "minecraft:blaze", "minecraft:ghast", "minecraft:happy_ghast",
			"minecraft:phantom", "minecraft:allay", "minecraft:bee",
			"minecraft:parrot", "minecraft:vex", "minecraft:ender_dragon",
			"minecraft:wither"));

	private static final Set<String> AQUATIC = new HashSet<>(Set.of(
			"minecraft:cod", "minecraft:salmon", "minecraft:tropical_fish",
			"minecraft:pufferfish", "minecraft:squid", "minecraft:glow_squid",
			"minecraft:dolphin", "minecraft:axolotl", "minecraft:tadpole",
			"minecraft:guardian", "minecraft:elder_guardian",
			"minecraft:turtle", "minecraft:nautilus"));

	private static final Set<String> WATER_HURT = new HashSet<>(Set.of(
			"minecraft:enderman", "minecraft:blaze", "minecraft:snow_golem",
			"minecraft:strider"));

	private static final Set<String> SUN_HURT = new HashSet<>(Set.of(
			"minecraft:zombie", "minecraft:zombie_villager", "minecraft:skeleton",
			"minecraft:stray", "minecraft:bogged", "minecraft:skeleton_horse",
			"minecraft:zombie_horse", "minecraft:phantom",
			"minecraft:zombie_nautilus"));

	private static final Set<String> UNDEAD = new HashSet<>(Set.of(
			"minecraft:zombie", "minecraft:husk", "minecraft:drowned",
			"minecraft:zombie_villager", "minecraft:zombified_piglin",
			"minecraft:skeleton", "minecraft:stray", "minecraft:bogged",
			"minecraft:wither_skeleton", "minecraft:skeleton_horse",
			"minecraft:zombie_horse", "minecraft:phantom", "minecraft:wither",
			"minecraft:zoglin", "minecraft:zombie_nautilus", "minecraft:camel_husk",
			"minecraft:parched"));

	private static final Set<String> ARTHROPOD = new HashSet<>(Set.of(
			"minecraft:spider", "minecraft:cave_spider", "minecraft:silverfish",
			"minecraft:endermite", "minecraft:bee"));

	private static final Set<String> ILLAGER = new HashSet<>(Set.of(
			"minecraft:pillager", "minecraft:vindicator", "minecraft:evoker",
			"minecraft:vex", "minecraft:ravager", "minecraft:illusioner",
			"minecraft:witch"));

	private static final Set<String> PIGLIN = new HashSet<>(Set.of(
			"minecraft:piglin", "minecraft:piglin_brute", "minecraft:hoglin"));

	/** 추락 데미지 없음 (고양이·오셀롯·개구리·골렘·마그마큐브) */
	private static final Set<String> FALL_NEGATE = new HashSet<>(Set.of(
			"minecraft:cat", "minecraft:ocelot", "minecraft:frog",
			"minecraft:iron_golem", "minecraft:copper_golem", "minecraft:magma_cube"));

	/** 물·용암에서 둥실 뜸 (슬라임·마그마큐브·유황큐브) */
	private static final Set<String> FLOATERS = new HashSet<>(Set.of(
			"minecraft:slime", "minecraft:magma_cube", "minecraft:sulfur_cube"));

	/** 물에서 가라앉음 (철·구리 골렘) */
	private static final Set<String> SINKERS = new HashSet<>(Set.of(
			"minecraft:iron_golem", "minecraft:copper_golem"));

	/** 벽 타기 (거미·동굴거미) */
	private static final Set<String> CLIMBERS = new HashSet<>(Set.of(
			"minecraft:spider", "minecraft:cave_spider"));

	/** 발판 높이 1.0 (말·당나귀·노새·라마·래바저·해골/좀비말) */
	private static final Set<String> STEPPERS = new HashSet<>(Set.of(
			"minecraft:horse", "minecraft:donkey", "minecraft:mule",
			"minecraft:llama", "minecraft:trader_llama", "minecraft:ravager",
			"minecraft:skeleton_horse", "minecraft:zombie_horse"));

	/** 수영 가속 (돌고래의 우아함, 값=배율) */
	private static final Map<String, Float> SWIMMERS = Map.ofEntries(
			Map.entry("minecraft:cod", 3.0f),
			Map.entry("minecraft:salmon", 3.5f),
			Map.entry("minecraft:tropical_fish", 2.0f),
			Map.entry("minecraft:pufferfish", 2.0f),
			Map.entry("minecraft:squid", 2.0f),
			Map.entry("minecraft:glow_squid", 2.0f),
			Map.entry("minecraft:dolphin", 4.0f),
			Map.entry("minecraft:turtle", 2.5f),
			Map.entry("minecraft:nautilus", 3.0f),
			Map.entry("minecraft:guardian", 3.0f),
			Map.entry("minecraft:elder_guardian", 3.0f));

	/** 육지 유예(축축함) 후 건조 피해 (아홀로틀·돌고래) */
	private static final Set<String> MOIST_SKIN = new HashSet<>(Set.of(
			"minecraft:axolotl", "minecraft:dolphin"));

	/** 물속 호흡(육지 질식 없음): 언데드 전체 + 철골렘 */
	private static boolean breathesWater(String typeId) {
		try {
			return UNDEAD.contains(typeId) || "minecraft:iron_golem".equals(typeId)
					|| MobProfiles.hasTag(typeId, "can_breathe_under_water");
		} catch (Exception e) {
			return false;
		}
	}

	/**
	 * 위협 (도망): 변신 종 → 도망치는 몹들. 참고 모드 traitIntimidate와 같은 쌍.
	 * 주민계는 떠는 주민·방랑상인 둘 다.
	 */
	private static final Map<String, Set<String>> INTIMIDATE = Map.ofEntries(
			Map.entry("minecraft:cat", Set.of("minecraft:creeper")),
			Map.entry("minecraft:ocelot", Set.of("minecraft:creeper")),
			Map.entry("minecraft:wolf", Set.of("minecraft:skeleton", "minecraft:fox", "minecraft:rabbit")),
			Map.entry("minecraft:llama", Set.of("minecraft:wolf")),
			Map.entry("minecraft:trader_llama", Set.of("minecraft:wolf")),
			Map.entry("minecraft:polar_bear", Set.of("minecraft:fox")),
			Map.entry("minecraft:guardian", Set.of("minecraft:dolphin")),
			Map.entry("minecraft:elder_guardian", Set.of("minecraft:dolphin")),
			Map.entry("minecraft:zombie", Set.of("minecraft:villager", "minecraft:wandering_trader")),
			Map.entry("minecraft:husk", Set.of("minecraft:villager", "minecraft:wandering_trader")),
			Map.entry("minecraft:drowned", Set.of("minecraft:villager", "minecraft:wandering_trader")),
			Map.entry("minecraft:evoker", Set.of("minecraft:villager", "minecraft:wandering_trader")),
			Map.entry("minecraft:illusioner", Set.of("minecraft:villager", "minecraft:wandering_trader")),
			Map.entry("minecraft:pillager", Set.of("minecraft:villager", "minecraft:wandering_trader")),
			Map.entry("minecraft:ravager", Set.of("minecraft:villager", "minecraft:wandering_trader")),
			Map.entry("minecraft:vex", Set.of("minecraft:villager", "minecraft:wandering_trader")),
			Map.entry("minecraft:vindicator", Set.of("minecraft:villager", "minecraft:wandering_trader")),
			Map.entry("minecraft:zoglin", Set.of("minecraft:villager", "minecraft:wandering_trader")));

	private static boolean intimidates(String morphType, String mobType) {
		try {
			Set<String> prey = INTIMIDATE.get(morphType);
			return prey != null && prey.contains(mobType);
		} catch (Exception e) {
			return false;
		}
	}

	// ---------- 상태 조회 ----------

	/**
	 * 서버: 서버맵만 신뢰 / 클라: 동기화 캐시만 신뢰.
	 * (싱글·LAN 통합서버는 클라와 서버가 같은 JVM이라 static 캐시를 공유한다.
	 * 서버에서 클라 캐시까지 폴백하면 해제 후에도 옛 ID가 남아 "해제 메시지만 뜨고
	 * 안 풀리는" 문제가 생긴다.)
	 */
	public static String morphId(Player player) {
		try {
			if (!player.level().isClientSide()) {
				String sv = serverMorphId(player.getUUID());
				if (sv != null && !sv.isEmpty()) {
					return sv;
				}
				// 구버전 태그 폴백 (서버 전용)
				try {
					for (String tag : player.entityTags()) {
						if (tag.startsWith(TAG_PREFIX)) {
							return tag.substring(TAG_PREFIX.length());
						}
					}
				} catch (Exception ignored) {
				}
				return "";
			}
		} catch (Exception ignored) {
		}
		// 클라이언트: 서버가 보낸 동기화 캐시
		try {
			String cached = MorphSyncCache.get(player.getUUID());
			if (cached != null && !cached.isEmpty()) {
				return cached;
			}
		} catch (Exception ignored) {
		}
		return "";
	}

	public static boolean isMorphed(Player player) {
		try {
			return !morphId(player).isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	/** 구버전 태그 제거 (EntityData로 이관済) */
	private static void clearLegacyTags(Player player) {
		try {
			for (String tag : new java.util.ArrayList<>(player.entityTags())) {
				if (tag.startsWith(TAG_PREFIX)) {
					try {
						player.removeTag(tag);
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	/** 특정 대상에게만 해당 플레이어의 현재 상태 전송 (비어 있어도 전송해 클라 잔상 제거) */
	public static void sendStateTo(ServerPlayer target, ServerPlayer subject) {
		try {
			String id = morphId(subject);
			net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
					target,
					new com.ultracapsuleadventure.network.ModPackets.MorphSyncPayload(
							subject.getUUID().toString(), id == null ? "" : id));
		} catch (Exception ignored) {
		}
	}

	/** 해당 플레이어의 모프 상태를 접속 전원에게 즉시 전파 */
	public static void broadcastMorph(ServerPlayer player) {
		try {
			String id = "";
			try {
				id = morphId(player);
			} catch (Exception ignored) {
			}
			if (id == null) {
				id = "";
			}
			com.ultracapsuleadventure.network.ModPackets.MorphSyncPayload payload =
					new com.ultracapsuleadventure.network.ModPackets.MorphSyncPayload(
							player.getUUID().toString(), id);
			net.minecraft.server.MinecraftServer server = player.level().getServer();
			if (server == null) {
				return;
			}
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				try {
					net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(p, payload);
				} catch (Exception e) {
					com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.error("[Morph] 동기화 패킷 전송 실패 → " + p.getName().getString(), e);
				}
			}
		} catch (Exception ignored) {
		}
	}

	public static boolean isFlyer(String typeId) {
		return typeId != null && FLYERS.contains(typeId);
	}

	public static boolean isAquatic(String typeId) {
		return typeId != null && AQUATIC.contains(typeId);
	}

	/** 언데드 변신인지 (재생·독 면역, 수중 호흡) */
	public static boolean isUndeadMorph(String morphType) {
		try {
			return morphType != null && UNDEAD.contains(morphType);
		} catch (Exception e) {
			return false;
		}
	}

	/** 같은 종 or 동족이면 중립 대상 */
	public static boolean sameFaction(String mobType, String morphType) {
		if (mobType == null || morphType == null) {
			return false;
		}
		if (mobType.equals(morphType)) {
			return true;
		}
		if (UNDEAD.contains(mobType) && UNDEAD.contains(morphType)) {
			return true;
		}
		if (ARTHROPOD.contains(mobType) && ARTHROPOD.contains(morphType)) {
			return true;
		}
		if (ILLAGER.contains(mobType) && ILLAGER.contains(morphType)) {
			return true;
		}
		if (PIGLIN.contains(mobType) && PIGLIN.contains(morphType)) {
			return true;
		}
		// 26.3 몹 프로필의 동료 목록 (같은 종 + 진영/태그)
		return MobProfiles.isAlly(mobType, morphType);
	}

	/**
	 * 변신한 플레이어를 이 몹이 무시해야 하는가: 동족이거나, 적대 몹(Enemy)인데 변신체가 그 몹의 먹잇감이 아닐 때.
	 * (예: 돼지·소·닭으로 변신하면 좀비·스켈레톤·크리퍼는 노리지 않는다)
	 */
	public static boolean ignoresMorph(Mob mob, String mobType, String morphType) {
		try {
			if (sameFaction(mobType, morphType)) {
				return true;
			}
			return mob instanceof net.minecraft.world.entity.monster.Enemy && !MobProfiles.targetsMorph(mobType, morphType);
		} catch (Exception e) {
			return false;
		}
	}

	public static boolean recentlyAttacked(Player player) {
		try {
			Long t = LAST_ATTACK.get(player.getUUID());
			if (t == null) {
				return false;
			}
			return System.currentTimeMillis() - t < 10000L;
		} catch (Exception e) {
			return false;
		}
	}

	public static void noteAttack(Player player) {
		try {
			LAST_ATTACK.put(player.getUUID(), System.currentTimeMillis());
		} catch (Exception ignored) {
		}
	}

	/**
	 * 변신한 플레이어를 노리는 동족 몹의 타겟 해제.
	 * setTarget 믹스인은 신규 획득만 막고, 변신 전부터 물고 있던 타겟은 유지되므로
	 * 변신 직후 + 주기 스위퍼에서 직접 비운다. 선공 후 10초는 중립 해제(그대로 둠).
	 */
	public static void clearMorphTargets(ServerPlayer player, String morphType) {
		try {
			if (player == null || morphType == null || morphType.isEmpty()) {
				return;
			}
			if (recentlyAttacked(player)) {
				return;
			}
			if (!(player.level() instanceof ServerLevel sl)) {
				return;
			}
			net.minecraft.world.phys.AABB box;
			try {
				box = player.getBoundingBox().inflate(48.0);
			} catch (Exception e) {
				return;
			}
			for (Entity e : sl.getEntities(player, box, x -> x instanceof Mob)) {
				try {
					Mob mob = (Mob) e;
					LivingEntity t = null;
					try {
						t = mob.getTarget();
					} catch (Exception ignored) {
					}
					if (t != player) {
						continue;
					}
					String mobType = "";
					try {
						mobType = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
					} catch (Exception ignored) {
					}
					if (!mobType.isEmpty() && ignoresMorph(mob, mobType, morphType)) {
						try {
							mob.setTarget(null);
						} catch (Exception ignored) {
						}
						// 브레인 기반 몹(피글린·호글린·워든·크리킹)은 기억에서도 지움
						try {
							mob.getBrain().eraseMemory(net.minecraft.world.entity.ai.memory.MemoryModuleType.ATTACK_TARGET);
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	// ---------- 변신/복귀 ----------

	/** 실패 시 사유 문자열, 성공 시 null */
	public static String morph(ServerPlayer player, String typeId) {
		try {
			if (isMorphed(player)) {
				// 같은 모습 재요청은 낀 상태 복구로 보고 성공 처리 (재적용+재전파)
				try {
					if (typeId != null && typeId.equals(morphId(player))) {
						SERVER_MORPH.put(player.getUUID(), typeId);
						clearLegacyTags(player);
						applyNow(player, typeId);
						player.refreshDimensions();
						broadcastMorph(player);
						try {
							com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.info("[Morph] reapply " + player.getName().getString()
									+ " -> " + typeId);
						} catch (Exception ignored) {
						}
						return null;
					}
				} catch (Exception ignored) {
				}
				return "이미 변신 중입니다.";
			}
			if (typeId == null || !typeId.contains(":")) {
				return "등록된 엔티티가 없습니다.";
			}
			if (typeId.equals("minecraft:player") || typeId.equals("minecraft:mannequin")) {
				return "플레이어로는 변신할 수 없습니다.";
			}
			if (player.isSpectator()) {
				return "관전 모드에서는 변신할 수 없습니다.";
			}
			// 살아있는 엔티티인지 검증 (임시 생성)
			EntityType<?> type = null;
			try {
				type = BuiltInRegistries.ENTITY_TYPE
						.get(Identifier.parse(typeId)).map(h -> h.value()).orElse(null);
			} catch (Exception ignored) {
			}
			if (type == null) {
				return "알 수 없는 엔티티: " + typeId;
			}
			boolean living = false;
			try {
				EntitySpawnRequest req = new EntitySpawnRequest(EntitySpawnReason.LOAD, false);
				Entity tmp = type.create(player.level(), req);
				if (tmp instanceof LivingEntity) {
					living = true;
				}
				if (tmp != null) {
					try {
						tmp.discard();
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			if (!living) {
				return "살아있는 엔티티가 아닙니다: " + typeId;
			}
			SERVER_MORPH.put(player.getUUID(), typeId);
			clearLegacyTags(player);
			// 변신 전부터 노리던 동족의 타겟 해제 (setTarget 차단은 신규 획득만 막으므로)
			try {
				clearMorphTargets(player, typeId);
			} catch (Exception ignored) {
			}
			try {
				applyNow(player, typeId);
			} catch (Exception ignored) {
			}
			// 히트박스·눈높이 즉시 갱신 (클라는 동기화된 ID로 자동 갱신)
			try {
				player.refreshDimensions();
			} catch (Exception ignored) {
			}
			// 명시적 동기화 패킷 즉시 전파 (EntityData만 믿지 않음)
			try {
				broadcastMorph(player);
			} catch (Exception ignored) {
			}
			try {
				player.sendSystemMessage(Component.literal(
						MorphData.shortName(typeId) + "(으)로 변신했다! (캡슐 우클릭: 복귀)"));
			} catch (Exception ignored) {
			}
			try {
				com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.info("[Morph] morph " + player.getName().getString()
						+ " -> " + typeId);
			} catch (Exception ignored) {
			}
			try {
				if (player.level() instanceof ServerLevel sl) {
					sl.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1, player.getZ(),
							24, 0.4, 0.8, 0.4, 0.1);
					sl.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT,
							SoundSource.PLAYERS, 0.9f, 1.3f);
				}
			} catch (Exception ignored) {
			}
			return null;
		} catch (Exception e) {
			return "변신 실패!";
		}
	}

	public static void unmorph(ServerPlayer player, String message) {
		try {
			String id = morphId(player);
			SERVER_MORPH.remove(player.getUUID());
			try {
				com.ultracapsuleadventure.UltraCapsuleAdventureMod.LOGGER.info("[Morph] unmorph "
						+ player.getName().getString() + " was=" + id);
			} catch (Exception ignored) {
			}
			// 변신 ID 제거 (EntityData + 구버전 태그)
			try {
				if (player instanceof MorphAccess a) {
					a.UltraCapsuleAdventure$setMorphId("");
				}
			} catch (Exception ignored) {
			}
			clearLegacyTags(player);
			// 속성 복원
			try {
				restoreDefaults(player);
			} catch (Exception ignored) {
			}
			// 효과 제거
			try {
				player.removeEffect(MobEffects.INVISIBILITY);
			} catch (Exception ignored) {
			}
			try {
				player.removeEffect(MobEffects.FIRE_RESISTANCE);
			} catch (Exception ignored) {
			}
			try {
				player.removeEffect(MobEffects.WATER_BREATHING);
			} catch (Exception ignored) {
			}
			try {
				player.removeEffect(MobEffects.SLOW_FALLING);
			} catch (Exception ignored) {
			}
			// 비행 복원 (크리에이티브 제외)
			try {
				if (!player.isCreative() && !player.isSpectator()) {
					player.getAbilities().mayfly = false;
					player.getAbilities().flying = false;
					player.onUpdateAbilities();
				}
			} catch (Exception ignored) {
			}
			// 공기 복원
			try {
				player.setAirSupply(player.getMaxAirSupply());
			} catch (Exception ignored) {
			}
			// 꼭두각시 제거
			try {
				removePuppet(player);
			} catch (Exception ignored) {
			}
			try {
				CROUCH_TICKS.remove(player.getUUID());
			} catch (Exception ignored) {
			}
			try {
				player.refreshDimensions();
			} catch (Exception ignored) {
			}
			try {
				broadcastMorph(player);
			} catch (Exception ignored) {
			}
			if (message != null && !message.isEmpty()) {
				try {
					player.sendSystemMessage(Component.literal(message));
				} catch (Exception ignored) {
				}
			}
			try {
				if (player.level() instanceof ServerLevel sl && !id.isEmpty()) {
					sl.sendParticles(ParticleTypes.POOF, player.getX(), player.getY() + 1, player.getZ(),
							12, 0.3, 0.6, 0.3, 0.05);
				}
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	private static void restoreDefaults(ServerPlayer player) {
		try {
			var hp = player.getAttribute(Attributes.MAX_HEALTH);
			if (hp != null) {
				hp.setBaseValue(20.0);
				try {
					if (player.getHealth() > 20.0f) {
						player.setHealth(20.0f);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		try {
			var sp = player.getAttribute(Attributes.MOVEMENT_SPEED);
			if (sp != null) {
				sp.setBaseValue(0.1);
			}
		} catch (Exception ignored) {
		}
		try {
			var atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
			if (atk != null) {
				atk.setBaseValue(1.0);
			}
		} catch (Exception ignored) {
		}
		try {
			var arm = player.getAttribute(Attributes.ARMOR);
			if (arm != null) {
				arm.setBaseValue(0.0);
			}
		} catch (Exception ignored) {
		}
		try {
			var kb = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
			if (kb != null) {
				kb.setBaseValue(0.0);
			}
		} catch (Exception ignored) {
		}
		try {
			var st = player.getAttribute(Attributes.STEP_HEIGHT);
			if (st != null) {
				st.setBaseValue(0.6);
			}
		} catch (Exception ignored) {
		}
		try {
			var jp = player.getAttribute(Attributes.JUMP_STRENGTH);
			if (jp != null) {
				Double pre = PRE_JUMP.remove(player.getUUID());
				jp.setBaseValue(pre == null ? 0.42 : pre);
			}
		} catch (Exception ignored) {
		}
		try {
			MOIST.remove(player.getUUID());
		} catch (Exception ignored) {
		}
	}

	private static Attrs attrsOf(ServerLevel sl, String typeId) {
		Attrs cached = ATTR_CACHE.get(typeId);
		if (cached != null) {
			return cached;
		}
		Attrs a = new Attrs();
		try {
			EntityType<?> type = BuiltInRegistries.ENTITY_TYPE
					.get(Identifier.parse(typeId)).map(h -> h.value()).orElse(null);
			if (type != null) {
				EntitySpawnRequest req = new EntitySpawnRequest(EntitySpawnReason.LOAD, false);
				Entity tmp = null;
				try {
					tmp = type.create(sl, req);
				} catch (Exception ignored) {
				}
				if (tmp instanceof LivingEntity living) {
					try {
						a.maxHealth = living.getMaxHealth();
					} catch (Exception ignored) {
					}
					try {
						var atk = living.getAttribute(Attributes.ATTACK_DAMAGE);
						if (atk != null) {
							a.attack = atk.getBaseValue();
						} else {
							// 공격 속성이 없으면 체력 비례 추정 (슬라임 등)
							a.attack = Math.max(1.0, a.maxHealth / 8.0);
						}
					} catch (Exception ignored) {
					}
					try {
						var spd = living.getAttribute(Attributes.MOVEMENT_SPEED);
						if (spd != null) {
							a.moveSpeed = spd.getBaseValue();
						}
					} catch (Exception ignored) {
					}
					try {
						var arm = living.getAttribute(Attributes.ARMOR);
						if (arm != null) {
							a.armor = Math.max(0.0, arm.getBaseValue());
						}
					} catch (Exception ignored) {
					}
					try {
						var kb = living.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
						if (kb != null) {
							a.knockbackResist = Math.max(0.0, Math.min(1.0, kb.getBaseValue()));
						}
					} catch (Exception ignored) {
					}
					try {
						var jp = living.getAttribute(Attributes.JUMP_STRENGTH);
						if (jp != null) {
							a.jump = Math.max(0.0, Math.min(2.0, jp.getBaseValue()));
						}
					} catch (Exception ignored) {
					}
					try {
						a.fireImmune = living.fireImmune();
					} catch (Exception ignored) {
					}
				}
				if (tmp != null) {
					try {
						tmp.discard();
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
		if (a.maxHealth < 1.0 || a.maxHealth > 1000.0) {
			a.maxHealth = 20.0;
		}
		if (a.moveSpeed < 0.01 || a.moveSpeed > 2.0) {
			a.moveSpeed = 0.1;
		}
		if (a.attack < 0.0 || a.attack > 100.0) {
			a.attack = 1.0;
		}
		if (ATTR_CACHE.size() > 200) {
			ATTR_CACHE.clear();
		}
		ATTR_CACHE.put(typeId, a);
		return a;
	}

	/**
	 * 변신 즉시 1회 적용. 참고 모드 기본 설정과 동일:
	 * 체력 상한 20·이동속도 상한 0.1(느린 몹은 그대로 느려짐), 공격·방어·넉백저항은 복사.
	 */
	private static void applyNow(ServerPlayer player, String typeId) {
		try {
			if (!(player.level() instanceof ServerLevel sl)) {
				return;
			}
			Attrs a = attrsOf(sl, typeId);
			double wantHp = Math.min(20.0, Math.max(1.0, a.maxHealth));
			double wantSpeed = walkSpeedFor(a.moveSpeed, typeId);
			try {
				var hp = player.getAttribute(Attributes.MAX_HEALTH);
				if (hp != null) {
					double before = hp.getBaseValue();
					hp.setBaseValue(wantHp);
					// 변신 시 체력 비율 유지
					try {
						float ratio = (float) (player.getHealth() / Math.max(1.0, before));
						player.setHealth((float) (wantHp * Math.max(0.2f, Math.min(1.0f, ratio))));
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			try {
				var sp = player.getAttribute(Attributes.MOVEMENT_SPEED);
				if (sp != null) {
					sp.setBaseValue(wantSpeed);
				}
			} catch (Exception ignored) {
			}
			try {
				var atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
				if (atk != null) {
					atk.setBaseValue(Math.max(0.5, a.attack));
				}
			} catch (Exception ignored) {
			}
			try {
				var arm = player.getAttribute(Attributes.ARMOR);
				if (arm != null) {
					arm.setBaseValue(Math.max(0.0, a.armor));
				}
			} catch (Exception ignored) {
			}
			try {
				var kb = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
				if (kb != null) {
					kb.setBaseValue(Math.max(0.0, Math.min(1.0, a.knockbackResist)));
				}
			} catch (Exception ignored) {
			}
			try {
				var st = player.getAttribute(Attributes.STEP_HEIGHT);
				if (st != null) {
					st.setBaseValue(STEPPERS.contains(typeId) ? 1.0 : 0.6);
				}
			} catch (Exception ignored) {
			}
			try {
				var jp = player.getAttribute(Attributes.JUMP_STRENGTH);
				if (jp != null) {
					try {
						PRE_JUMP.put(player.getUUID(), jp.getBaseValue());
					} catch (Exception ignored) {
					}
					jp.setBaseValue(a.jump);
				}
			} catch (Exception ignored) {
			}
			// 렌더는 클라가 플레이어 자리에 몹을 직접 그리므로 투명화 불필요
		} catch (Exception ignored) {
		}
	}

	// ---------- 등록 ----------

	/** 서버 종료 시 메모리 상태 전부 정리 (다른 월드 재사용 시 유령 변신 방지) */
	public static void clearAll() {
		ABILITY_COOL.clear();
		try {
			SERVER_MORPH.clear();
		} catch (Exception ignored) {
		}
		try {
			LAST_ATTACK.clear();
		} catch (Exception ignored) {
		}
		try {
			CROUCH_TICKS.clear();
		} catch (Exception ignored) {
		}
		try {
			LAST_SHOOT.clear();
		} catch (Exception ignored) {
		}
		try {
			CREEPER_COOL.clear();
		} catch (Exception ignored) {
		}
		try {
			MOIST.clear();
		} catch (Exception ignored) {
		}
		try {
			PRE_JUMP.clear();
		} catch (Exception ignored) {
		}
	}

	public static void register() {
		// 서버 종료 시 정리 (통합 서버 월드 교체 대비)
		try {
			net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents.SERVER_STOPPED.register(
					server -> clearAll());
		} catch (Exception ignored) {
		}
		// 입장 시 현재 모프 상태를 입장자에게 즉시 전송 (캐시 예열).
		// 본인은 비어 있어도 반드시 전송: 클라에 남은 옛 캐시(다른 월드 유령 변신)를 지운다.
		try {
			net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents.JOIN.register(
					(handler, sender, server) -> {
						try {
							ServerPlayer joiner = handler.getPlayer();
							server.execute(() -> {
								try {
									for (ServerPlayer p : server.getPlayerList().getPlayers()) {
										try {
											String id = morphId(p);
											if (p.getUUID().equals(joiner.getUUID())) {
												try {
													net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
															joiner,
															new com.ultracapsuleadventure.network.ModPackets.MorphSyncPayload(
																	p.getUUID().toString(),
																	id == null ? "" : id));
												} catch (Exception ignored) {
												}
												continue;
											}
											if (id != null && !id.isEmpty()) {
												try {
													net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking.send(
															joiner,
															new com.ultracapsuleadventure.network.ModPackets.MorphSyncPayload(
																	p.getUUID().toString(), id));
												} catch (Exception ignored) {
												}
											}
										} catch (Exception ignored) {
										}
									}
								} catch (Exception ignored) {
								}
							});
						} catch (Exception ignored) {
						}
					});
		} catch (Exception ignored) {
		}
		// 황혼의 검 처치 → 은신 캡슐 등록
		try {
			ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity dead, DamageSource source) -> {
				try {
					// 변신 중 사망 시 해제
					if (dead instanceof ServerPlayer sp && isMorphed(sp)) {
						try {
							unmorph(sp, null);
						} catch (Exception ignored) {
						}
					}
					if (dead instanceof Player) {
						return;
					}
					// 꼭두각시 처치는 등록 제외
					try {
						for (String tag : dead.entityTags()) {
							if (tag.startsWith("morph_puppet-")) {
								return;
							}
						}
					} catch (Exception ignored) {
					}
					Entity killerEnt = null;
					try {
						killerEnt = source.getEntity();
					} catch (Exception ignored) {
					}
					if (!(killerEnt instanceof ServerPlayer killer)) {
						return;
					}
					// 황혼의 검 확인
					boolean twilight = false;
					try {
						ItemStack main = killer.getMainHandItem();
						ItemStack off = killer.getOffhandItem();
						if (main.getItem() instanceof TwilightSwordItem
								|| off.getItem() instanceof TwilightSwordItem) {
							twilight = true;
						}
					} catch (Exception ignored) {
					}
					if (!twilight) {
						return;
					}
					// 은신 캡슐 탐색 (손 우선)
					ItemStack cap = ItemStack.EMPTY;
					try {
						for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
							ItemStack s = killer.getItemInHand(hand);
							if (s.getItem() instanceof StealthCapsuleItem) {
								cap = s;
								break;
							}
						}
						if (cap.isEmpty()) {
							for (int i = 0; i < killer.getInventory().getContainerSize(); i++) {
								ItemStack s = killer.getInventory().getItem(i);
								if (s.getItem() instanceof StealthCapsuleItem) {
									cap = s;
									break;
								}
							}
						}
					} catch (Exception ignored) {
					}
					if (cap.isEmpty()) {
						try {
							killer.sendOverlayMessage(Component.literal("은신 캡슐이 없어 등록 실패!"));
						} catch (Exception ignored) {
						}
						return;
					}
					String typeId;
					try {
						typeId = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
					} catch (Exception e) {
						return;
					}
					if (typeId.equals("minecraft:player")) {
						return;
					}
				// 중복 등록 방지: 이미 있으면 선택만 옮기고 끝낸다 (무한 등록, 중복 없음)
				try {
					if (MorphData.getAll(cap).contains(typeId)) {
						MorphData.addMorph(cap, typeId);
						try {
							com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(killer);
						} catch (Exception ignored) {
						}
						try {
							killer.sendOverlayMessage(Component.literal(
									MorphData.shortName(typeId) + "(은)는 이미 등록됨 (선택만 이동)"));
						} catch (Exception ignored) {
						}
						return;
					}
				} catch (Exception ignored) {
				}
				boolean added = false;
				try {
					added = MorphData.addMorph(cap, typeId);
				} catch (Exception ignored) {
				}
				if (!added) {
					try {
						killer.sendOverlayMessage(Component.literal("등록 실패!"));
					} catch (Exception ignored) {
					}
					return;
				}
					try {
						com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(killer);
					} catch (Exception ignored) {
					}
					if (dead.level() instanceof ServerLevel sl) {
						try {
							Vec3 from = dead.position().add(0, 1.0, 0);
							Vec3 to = killer.position().add(0, 1.2, 0);
							for (int i = 0; i <= 12; i++) {
								Vec3 p = from.lerp(to, i / 12.0);
								sl.sendParticles(ParticleTypes.PORTAL, p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
							}
							sl.playSound(null, killer.blockPosition(),
									SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.7f, 0.6f);
						} catch (Exception ignored) {
						}
					}
					String shortType = MorphData.shortName(typeId);
					try {
						killer.sendSystemMessage(Component.literal(
								shortType + "(을)를 은신 캡슐에 등록했다! (총 "
										+ MorphData.getAll(cap).size() + "종) C키 관리창에서 변신"));
					} catch (Exception ignored) {
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}

		// 공격 기록 + 원거리 교체 + 접촉 효과 + 중립 보호
		try {
			ServerLivingEntityEvents.ALLOW_DAMAGE.register((LivingEntity victim, DamageSource source, float amount) -> {
				try {
					Entity atkEnt = null;
					try {
						atkEnt = source.getEntity();
					} catch (Exception ignored) {
					}
					if (atkEnt == null) {
						try {
							atkEnt = source.getDirectEntity();
						} catch (Exception ignored) {
						}
					}
					// A) 변신 플레이어가 때림 → 기록 + 접촉 효과 + 원거리 교체
					if (atkEnt instanceof ServerPlayer sp && isMorphed(sp) && victim != sp) {
						String mid = morphId(sp);
						noteAttack(sp);
						// 원거리형: 3블록 밖은 투사체로 교체
						try {
							String proj = projectileOf(mid);
							if (proj != null && victim.isAlive()) {
								double d2 = -1;
								try {
									d2 = sp.distanceToSqr(victim);
								} catch (Exception ignored) {
								}
								if (d2 > 6.25) {
									long now = System.currentTimeMillis();
									Long last = LAST_SHOOT.get(sp.getUUID());
									if (last == null || now - last > 700L) {
										LAST_SHOOT.put(sp.getUUID(), now);
										shootProjectile(sp, mid, victim);
										return false;
									}
								}
							}
						} catch (Exception ignored) {
						}
						// 접촉 효과
						try {
							applyContactEffect(sp, mid, victim);
						} catch (Exception ignored) {
						}
						return true;
					}
					// B) 꼭두각시는 무적
					try {
						if (victim.entityTags().stream().anyMatch(t -> t.startsWith("morph_puppet-"))) {
							return false;
						}
					} catch (Exception ignored) {
					}
					// C) 변신 플레이어가 맞음 → 특성 경감/면역
					if (victim instanceof ServerPlayer sp && isMorphed(sp)) {
						String mid = morphId(sp);
						String msgId = "";
						try {
							msgId = source.getMsgId();
						} catch (Exception ignored) {
						}
						// 낙하: 비행형·닭형 면역/경감
						try {
							if (msgId.contains("fall")) {
								if (isFlyer(mid)) {
									return false;
								}
								if (mid.equals("minecraft:chicken")) {
									return false;
								}
							}
						} catch (Exception ignored) {
						}
						// 화염/용암: 내성형 면역
						try {
							if (msgId.contains("fire") || msgId.contains("lava")
									|| msgId.contains("hot_floor") || msgId.contains("inFire")
									|| msgId.contains("onFire")) {
								Attrs a = null;
								try {
									if (sp.level() instanceof ServerLevel sl) {
										a = attrsOf(sl, mid);
									}
								} catch (Exception ignored) {
								}
								if (a != null && a.fireImmune) {
									return false;
								}
							}
						} catch (Exception ignored) {
						}
						// 익사: 수생형 면역
						try {
							if (msgId.contains("drown") && isAquatic(mid)) {
								return false;
							}
						} catch (Exception ignored) {
						}
						// 동족 보호: 같은 종/동족 몹의 공격 취소 (선공 후 10초 제외)
						try {
							if (atkEnt instanceof Mob mob) {
								String mobType = "";
								try {
									mobType = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
								} catch (Exception ignored) {
								}
								if (ignoresMorph(mob, mobType, mid) && !recentlyAttacked(sp)) {
									return false;
								}
							}
						} catch (Exception ignored) {
						}
					}
				} catch (Exception e) {
					return true;
				}
				return true;
			});
		} catch (Exception ignored) {
		}

		// 매 틱 특성 적용
		try {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				try {
					if (server.getTickCount() % 100 == 0) {
						try {
							clearStrayPuppets(server);
						} catch (Exception ignored) {
						}
						// 모프 상태 재전파 (차원 이동·추적 시작 등 누락 복구)
						try {
							for (ServerPlayer p : server.getPlayerList().getPlayers()) {
								try {
									if (isMorphed(p)) {
										broadcastMorph(p);
									}
								} catch (Exception ignored) {
								}
							}
						} catch (Exception ignored) {
						}
					}
					// 동족 중립·위협 스위퍼 (1초마다): 변신 전부터 물고 있던 타겟 + 새로고침 누락분 해제
					try {
						if (server.getTickCount() % 20 == 0) {
							for (ServerPlayer p : server.getPlayerList().getPlayers()) {
								try {
									String mid = morphId(p);
									if (!mid.isEmpty() && p.isAlive()) {
										clearMorphTargets(p, mid);
										scarePrey(p, mid);
										huntMorphed(p, mid);
									}
								} catch (Exception ignored) {
								}
							}
						}
					} catch (Exception ignored) {
					}
					// 이동 특성 (매 틱): 등반·둥실·가라앉음
					try {
						for (ServerPlayer p : server.getPlayerList().getPlayers()) {
							try {
								String mid = morphId(p);
								if (!mid.isEmpty() && p.isAlive() && !p.isSpectator()) {
									tickMorphMove(p, mid);
								}
							} catch (Exception ignored) {
							}
						}
					} catch (Exception ignored) {
					}
					if (server.getTickCount() % 5 != 0) {
						return;
					}
					for (ServerPlayer player : server.getPlayerList().getPlayers()) {
						try {
							if (!isMorphed(player) || !player.isAlive()) {
								continue;
							}
							// 관전 모드로 바뀌면 변신 해제 (꼭두각시가 어긋나지 않게)
							if (player.isSpectator()) {
								try {
									unmorph(player, "관전 모드에서는 변신이 해제됩니다.");
								} catch (Exception ignored) {
								}
								continue;
							}
							tickMorph(player);
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
	}

	// ---------- 틱 ----------

	/** 매 틱 이동 특성: 거미 벽타기, 큐브 둥실뜸, 골렘 가라앉음 (참고 모드와 같은 수치) */
	/** 물 밖에서 마른 시간(틱) - 물고기류 질식용 */
	private static final Map<UUID, Integer> DRY = new HashMap<>();

	/**
	 * 매 틱 처리.
	 * - 물에 민감한 몹(엔더맨·블레이즈·눈골렘·스트라이더): 물·비에 닿으면 1 피해(무적 시간 때문에 약 0.5초마다 1).
	 *   엔더맨은 바닐라처럼 피해를 받으면 90% 확률로 무작위 순간이동.
	 * - 물고기류: 물 밖에서 15초(300틱) 지나면 1초마다 2 피해. 공기 게이지도 함께 줄어듦.
	 *   (플레이어 기본 틱이 공기를 틱당 +4씩 채워서 예전 5틱 처리로는 절대 질식하지 않았음)
	 */
	private static void tickDryAndWater(ServerPlayer player, String mid) {
		try {
			if (!(player.level() instanceof ServerLevel sl)) {
				return;
			}
			// 면역 계열 (26.3 태그 기준): 가루눈 동결 / 독·재생 / 위더 효과
			try {
				if (MobProfiles.hasTag(mid, "freeze_immune_entity_types") && player.getTicksFrozen() > 0) {
					player.setTicksFrozen(0);
				}
				if (MobProfiles.hasTag(mid, "ignores_poison_and_regen")) {
					if (player.hasEffect(MobEffects.POISON)) {
						player.removeEffect(MobEffects.POISON);
					}
					if (player.hasEffect(MobEffects.REGENERATION)) {
						player.removeEffect(MobEffects.REGENERATION);
					}
				}
				if ((mid.equals("minecraft:wither_skeleton") || mid.equals("minecraft:wither"))
						&& player.hasEffect(MobEffects.WITHER)) {
					player.removeEffect(MobEffects.WITHER);
				}
				// 스트라이더: 용암 위에 뜸
				if (mid.equals("minecraft:strider") && player.isInLava() && !player.getAbilities().flying) {
					player.addDeltaMovement(new Vec3(0, 0.07, 0));
				}
			} catch (Exception ignored) {
			}
			if (player.isCreative() || player.isSpectator()) {
				DRY.remove(player.getUUID());
				return;
			}
			if (WATER_HURT.contains(mid) && player.isInWaterOrRain()) {
				boolean hurt = player.hurtServer(sl, sl.damageSources().drown(), 1.0F);
				if (hurt && mid.equals("minecraft:enderman") && sl.getRandom().nextInt(10) != 0) {
					randomEnderTeleport(sl, player);
				}
			}
			boolean fishLike = isAquatic(mid) && !MOIST_SKIN.contains(mid) && !"minecraft:turtle".equals(mid);
			if (fishLike) {
				if (player.isInWater()) {
					DRY.remove(player.getUUID());
				} else {
					int dry = DRY.merge(player.getUUID(), 1, Integer::sum);
					player.setAirSupply(Math.max(0, 300 - dry));
					if (dry >= 300 && (dry - 300) % 20 == 0) {
						player.hurtServer(sl, sl.damageSources().drown(), 2.0F);
					}
				}
			} else {
				DRY.remove(player.getUUID());
			}
		} catch (Exception ignored) {
		}
	}

	/** 주변 16칸 안 물 없는 안전한 곳으로 무작위 순간이동 */
	private static boolean randomEnderTeleport(ServerLevel sl, ServerPlayer p) {
		try {
			for (int i = 0; i < 16; i++) {
				double fx = p.getX() + (sl.getRandom().nextDouble() - 0.5) * 32.0;
				double fz = p.getZ() + (sl.getRandom().nextDouble() - 0.5) * 32.0;
				double fy = p.getY() + sl.getRandom().nextInt(16) - 8;
				net.minecraft.world.phys.AABB box = p.getBoundingBox().move(fx - p.getX(), fy - p.getY(), fz - p.getZ());
				if (!sl.noCollision(p, box)) {
					continue;
				}
				int drop = 0;
				while (drop < 24 && sl.noCollision(p, box.move(0, -0.5, 0))) {
					box = box.move(0, -0.5, 0);
					fy -= 0.5;
					drop++;
				}
				if (drop >= 24) {
					continue;
				}
				var fs = sl.getFluidState(BlockPos.containing(fx, fy + 0.1, fz));
				if (fs.is(net.minecraft.tags.FluidTags.WATER) || fs.is(net.minecraft.tags.FluidTags.LAVA)) {
					continue;
				}
				sl.sendParticles(ParticleTypes.PORTAL, p.getX(), p.getY() + 1.0, p.getZ(), 32, 0.4, 0.8, 0.4, 0.2);
				p.teleportTo(fx, fy, fz);
				p.fallDistance = 0f;
				sl.sendParticles(ParticleTypes.PORTAL, fx, fy + 1.0, fz, 32, 0.4, 0.8, 0.4, 0.2);
				sl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
				return true;
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	private static void tickMorphMove(ServerPlayer player, String mid) {
		try {
			if (!(player.level() instanceof ServerLevel)) {
				return;
			}
		} catch (Exception e) {
			return;
		}
		tickDryAndWater(player, mid);
		// 벽 타기: 벽에 붙으면 상승 (웅크리면 하강)
		if (CLIMBERS.contains(mid)) {
			try {
				if (player.horizontalCollision) {
					Vec3 m = player.getDeltaMovement();
					double mx = Math.max(-0.15, Math.min(0.15, m.x));
					double mz = Math.max(-0.15, Math.min(0.15, m.z));
					double my = m.y;
					boolean sneak = false;
					try {
						sneak = player.isCrouching();
					} catch (Exception ignored) {
					}
					if (sneak) {
						my -= my;
					} else {
						my -= (my - 0.2);
					}
					player.setDeltaMovement(mx, my, mz);
					player.fallDistance = 0f;
				}
			} catch (Exception ignored) {
			}
		}
		// 둥실뜸: 물·용암에서 상승 (날지 않을 때)
		if (FLOATERS.contains(mid)) {
			try {
				boolean inFluid = false;
				try {
					inFluid = player.isInWater() || player.isInLava();
				} catch (Exception ignored) {
				}
				boolean flying = false;
				try {
					flying = player.getAbilities().flying;
				} catch (Exception ignored) {
				}
				if (inFluid && !flying) {
					player.addDeltaMovement(new Vec3(0, 0.07, 0));
				}
			} catch (Exception ignored) {
			}
		}
		// 가라앉음: 물에서 하강 (벽에 붙으면 상승해 빠져나옴)
		if (SINKERS.contains(mid)) {
			try {
				boolean inWater = false;
				try {
					inWater = player.isInWater();
				} catch (Exception ignored) {
				}
				boolean flying = false;
				try {
					flying = player.getAbilities().flying;
				} catch (Exception ignored) {
				}
				if (inWater) {
					Vec3 m = player.getDeltaMovement();
					if (player.horizontalCollision) {
						player.setDeltaMovement(m.x, 0.07, m.z);
					} else if (m.y > -0.07 && !flying) {
						player.addDeltaMovement(new Vec3(0, -0.07, 0));
					}
				}
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * 위협 (도망): 포식자 변신 근처 6m의 먹이는 도망가고, 물고 있던 타겟도 놓는다.
	 * 반격당한 뒤 10초(선공 기록)는 제외.
	 */
	private static void scarePrey(ServerPlayer player, String morphType) {
		try {
			Set<String> prey = INTIMIDATE.get(morphType);
			if ((prey == null || prey.isEmpty()) && !MobProfiles.hasScares(morphType)) {
				return;
			}
			if (recentlyAttacked(player)) {
				return;
			}
			if (!(player.level() instanceof ServerLevel sl)) {
				return;
			}
			net.minecraft.world.phys.AABB box;
			try {
				box = player.getBoundingBox().inflate(8.0);
			} catch (Exception e) {
				return;
			}
			for (Entity e : sl.getEntities(player, box, x -> x instanceof Mob)) {
				try {
					Mob mob = (Mob) e;
					String mobType = "";
					try {
						mobType = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
					} catch (Exception ignored) {
					}
					if (!(prey != null && prey.contains(mobType)) && !MobProfiles.isScared(mobType, morphType)) {
						continue;
					}
					// 타겟 해제
					try {
						if (mob.getTarget() == player) {
							mob.setTarget(null);
						}
					} catch (Exception ignored) {
					}
					// 도망: 6m 안이면 반대 방향으로 이동
					try {
						double d2 = mob.distanceToSqr(player);
						if (d2 < 36.0) {
							boolean redirect = false;
							try {
								redirect = mob.getNavigation().isDone();
							} catch (Exception ignored) {
								redirect = true;
							}
							if (redirect) {
								Vec3 away = new Vec3(mob.getX() - player.getX(), 0, mob.getZ() - player.getZ());
								double len = away.length();
								if (len > 1e-4) {
									away = away.scale(10.0 / len);
									try {
										mob.getNavigation().moveTo(
												mob.getX() + away.x, mob.getY(), mob.getZ() + away.z, 1.2);
									} catch (Exception ignored) {
									}
								}
							}
						}
					} catch (Exception ignored) {
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	/**
	 * 몹별 걷는 속도: 좀비(0.23)를 플레이어 기본(0.1)으로 보고 바닐라 속도에 비례.
	 * 거미·늑대는 약 1.3배, 소·양은 약 0.9배, 스니퍼는 약 0.4배. 범위 0.03~0.14.
	 * 0.6 이상(수중 전용 값)은 기본 0.1, 주민류는 AI가 느리게 걸으므로 0.06.
	 */
	private static double walkSpeedFor(double vanilla, String typeId) {
		if ("minecraft:villager".equals(typeId) || "minecraft:wandering_trader".equals(typeId)) {
			return 0.06;
		}
		if (vanilla >= 0.6) {
			return 0.1;
		}
		// 속도 속성이 따로 없는 몹(물고기·박쥐·팬텀 등)은 생물 기본값 0.1 → 플레이어 기본 속도 유지
		boolean slowOnPurpose = "minecraft:sniffer".equals(typeId) || "minecraft:camel".equals(typeId)
				|| "minecraft:camel_husk".equals(typeId) || "minecraft:happy_ghast".equals(typeId);
		if (!slowOnPurpose && Math.abs(vanilla - 0.1) < 0.0011) {
			return 0.1;
		}
		return Math.min(0.14, Math.max(0.03, 0.1 * (vanilla / 0.23)));
	}

	private static void tickMorph(ServerPlayer player) {
		String mid = morphId(player);
		if (mid.isEmpty()) {
			return;
		}
		if (!(player.level() instanceof ServerLevel sl)) {
			return;
		}
		Attrs a = attrsOf(sl, mid);
		// 속성 유지 (버프 등으로 바뀌면 복구). 상한은 참고 모드 기본 설정과 동일.
		double wantHp = Math.min(20.0, Math.max(1.0, a.maxHealth));
		double wantSpeed = walkSpeedFor(a.moveSpeed, mid);
		try {
			var sp = player.getAttribute(Attributes.MOVEMENT_SPEED);
			if (sp != null && Math.abs(sp.getBaseValue() - wantSpeed) > 0.0001) {
				sp.setBaseValue(wantSpeed);
			}
		} catch (Exception ignored) {
		}
		try {
			var hp = player.getAttribute(Attributes.MAX_HEALTH);
			if (hp != null && Math.abs(hp.getBaseValue() - wantHp) > 0.01) {
				hp.setBaseValue(wantHp);
			}
		} catch (Exception ignored) {
		}
		try {
			var atk = player.getAttribute(Attributes.ATTACK_DAMAGE);
			if (atk != null && Math.abs(atk.getBaseValue() - Math.max(0.5, a.attack)) > 0.01) {
				atk.setBaseValue(Math.max(0.5, a.attack));
			}
		} catch (Exception ignored) {
		}
		try {
			var arm = player.getAttribute(Attributes.ARMOR);
			if (arm != null && Math.abs(arm.getBaseValue() - Math.max(0.0, a.armor)) > 0.01) {
				arm.setBaseValue(Math.max(0.0, a.armor));
			}
		} catch (Exception ignored) {
		}
		try {
			var kb = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
			double wantKb = Math.max(0.0, Math.min(1.0, a.knockbackResist));
			if (kb != null && Math.abs(kb.getBaseValue() - wantKb) > 0.001) {
				kb.setBaseValue(wantKb);
			}
		} catch (Exception ignored) {
		}
		try {
			var st = player.getAttribute(Attributes.STEP_HEIGHT);
			double wantSt = STEPPERS.contains(mid) ? 1.0 : 0.6;
			if (st != null && Math.abs(st.getBaseValue() - wantSt) > 0.001) {
				st.setBaseValue(wantSt);
			}
		} catch (Exception ignored) {
		}
		try {
			var jp = player.getAttribute(Attributes.JUMP_STRENGTH);
			if (jp != null && Math.abs(jp.getBaseValue() - a.jump) > 0.001) {
				jp.setBaseValue(a.jump);
			}
		} catch (Exception ignored) {
		}
		// 화염 내성
		if (a.fireImmune) {
			try {
				if (!player.hasEffect(MobEffects.FIRE_RESISTANCE)) {
					player.addEffect(new MobEffectInstance(MobEffects.FIRE_RESISTANCE, 999999, 0, false, false, false));
				}
			} catch (Exception ignored) {
			}
			try {
				if (player.isOnFire()) {
					player.clearFire();
				}
			} catch (Exception ignored) {
			}
		}
		// 비행
		if (isFlyer(mid)) {
			try {
				if (!player.getAbilities().mayfly) {
					player.getAbilities().mayfly = true;
					player.onUpdateAbilities();
				}
			} catch (Exception ignored) {
			}
			try {
				if (!player.onGround() && !player.getAbilities().flying && !player.isInWater()) {
					player.getAbilities().flying = true;
					player.onUpdateAbilities();
				}
			} catch (Exception ignored) {
			}
			try {
				player.fallDistance = 0f;
			} catch (Exception ignored) {
			}
		}
		// 닭: 느린 낙하
		if (mid.equals("minecraft:chicken")) {
			try {
				if (!player.hasEffect(MobEffects.SLOW_FALLING) && !player.onGround()) {
					player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 100, 0, false, false, false));
				}
			} catch (Exception ignored) {
			}
		}
		// 추락 무시 (고양이·개구리·골렘·마그마큐브)
		if (FALL_NEGATE.contains(mid) || MobProfiles.hasTag(mid, "fall_damage_immune")) {
			try {
				player.fallDistance = 0f;
			} catch (Exception ignored) {
			}
		}
		// 수생: 물에서 호흡, 밖에서 질식 (단 축축함 종은 유예 후 건조 피해)
		if (isAquatic(mid)) {
			try {
				if (!player.hasEffect(MobEffects.WATER_BREATHING)) {
					player.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 999999, 0, false, false, false));
				}
			} catch (Exception ignored) {
			}
			boolean inWater = false;
			try {
				inWater = player.isInWater();
			} catch (Exception ignored) {
			}
			boolean wet = false;
			try {
				wet = inWater || player.isInWaterOrRain();
			} catch (Exception ignored) {
				wet = inWater;
			}
			// 수영 가속 (돌고래의 우아함)
			try {
				Float mult = SWIMMERS.get(mid);
				if (mult != null && mult > 1.0f && inWater) {
					int amp = mult >= 4.0f ? 1 : 0;
					if (!player.hasEffect(MobEffects.DOLPHINS_GRACE)) {
						player.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 100, amp, false, false, false));
					}
				} else if (mult == null || mult <= 1.0f) {
					try {
						player.removeEffect(MobEffects.DOLPHINS_GRACE);
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			try {
				if (inWater) {
					player.setAirSupply(player.getMaxAirSupply());
					try {
						MOIST.put(player.getUUID(), MOIST_MAX);
					} catch (Exception ignored) {
					}
				} else if (MOIST_SKIN.contains(mid)) {
					// 축축함: 젖어 있으면 회복, 마르면 감소 → 0에서 건조 피해
					long moist = MOIST_MAX;
					try {
						moist = MOIST.getOrDefault(player.getUUID(), MOIST_MAX);
					} catch (Exception ignored) {
					}
					if (wet) {
						moist = MOIST_MAX;
					} else {
						moist -= 5;
					}
					try {
						MOIST.put(player.getUUID(), moist);
					} catch (Exception ignored) {
					}
					if (!wet && moist <= 0 && !player.isCreative()) {
						try {
							player.hurtServer(sl, sl.damageSources().dryOut(), 1.0F);
						} catch (Exception ignored) {
						}
						try {
							MOIST.put(player.getUUID(), 100L);
						} catch (Exception ignored) {
						}
					}
				}
			} catch (Exception ignored) {
			}
		}
		// 언데드·철골렘: 물속 호흡 (육지 질식 없음)
		if (breathesWater(mid) && !isAquatic(mid)) {
			try {
				boolean eyesWet = false;
				try {
					eyesWet = player.isEyeInFluid(net.minecraft.tags.FluidTags.WATER);
				} catch (Exception e1) {
					try {
						eyesWet = player.isInWater();
					} catch (Exception ignored) {
					}
				}
				if (eyesWet) {
					player.setAirSupply(player.getMaxAirSupply());
				}
			} catch (Exception ignored) {
			}
		}
		// 물 피해는 매 틱 처리(tickDryAndWater)
		// 햇빛 화상 (좀비·스켈레톤 등). 투구 착용 시 투구가 대신 닳음
		if (SUN_HURT.contains(mid) || MobProfiles.hasTag(mid, "burn_in_daylight")) {
			try {
				if (sl.isBrightOutside() && sl.canSeeSky(player.blockPosition())
						&& !player.isInWaterOrRain() && player.tickCount % 20 < 5) {
					boolean helmeted = false;
					try {
						ItemStack head = player.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.HEAD);
						if (head != null && !head.isEmpty()) {
							helmeted = true;
							if (head.isDamageableItem()) {
								try {
									head.hurtAndBreak(1, player,
											net.minecraft.world.entity.EquipmentSlot.HEAD);
								} catch (Exception ignored) {
								}
							}
						}
					} catch (Exception ignored) {
					}
					if (!helmeted) {
						try {
							player.hurtServer(sl, sl.damageSources().inFire(), 1.0F);
						} catch (Exception ignored) {
						}
					}
				}
			} catch (Exception ignored) {
			}
		}
		// 크리퍼: 1.5초 웅크리면 자폭 (블록 파괴 없음)
		if (mid.equals("minecraft:creeper")) {
			try {
				boolean crouch = player.isCrouching();
				int n = CROUCH_TICKS.getOrDefault(player.getUUID(), 0);
				if (crouch) {
					n += 5;
				} else {
					n = 0;
				}
				CROUCH_TICKS.put(player.getUUID(), n);
				long now = System.currentTimeMillis();
				Long cool = CREEPER_COOL.get(player.getUUID());
				if (n >= 30 && (cool == null || now > cool)) {
					CREEPER_COOL.put(player.getUUID(), now + 5000L);
					CROUCH_TICKS.put(player.getUUID(), 0);
					creeperBlast(sl, player);
				} else if (n > 0 && n % 10 == 0) {
					try {
						player.sendOverlayMessage(Component.literal("자폭 충전 " + (n * 100 / 30) + "% (웅크리기 유지)"));
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
		}
		// 모습 렌더는 각 클라가 플레이어 자리에 직접 수행 (서버 꼭두각시 없음)
	}

	private static void creeperBlast(ServerLevel sl, ServerPlayer player) {
		try {
			Vec3 center = player.position().add(0, 1.0, 0);
			sl.sendParticles(ParticleTypes.EXPLOSION, center.x, center.y, center.z, 10, 0.5, 0.5, 0.5, 0.1);
			sl.sendParticles(ParticleTypes.SMOKE, center.x, center.y, center.z, 20, 0.5, 0.8, 0.5, 0.05);
			sl.playSound(null, player.blockPosition(), SoundEvents.GENERIC_EXPLODE.value(),
					SoundSource.PLAYERS, 1.0f, 1.0f);
			for (Entity e : sl.getEntities(player, player.getBoundingBox().inflate(4.0),
					x -> x instanceof LivingEntity living && living.isAlive() && x != player)) {
				try {
					((LivingEntity) e).hurtServer(sl, sl.damageSources().explosion(null, player), 9.0F);
				} catch (Exception ignored) {
				}
			}
			try {
				player.sendSystemMessage(Component.literal("크리퍼 자폭! (블록 파괴 없음)"));
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	// ---------- morph puppet cleanup (legacy) ----------
	// 새 방식은 서버 꼭두각시 없이 각 클라가 플레이어 자리에 직접 렌더한다.

	/** 전 차원에 남은 구버전 꼭두각시 전부 제거 */
	public static void clearStrayPuppets(net.minecraft.server.MinecraftServer server) {
		try {
			for (ServerLevel level : server.getAllLevels()) {
				try {
					for (Entity e : level.getAllEntities()) {
						try {
							for (String tag : e.entityTags()) {
								if (tag.startsWith("morph_puppet-")) {
									e.discard();
									break;
								}
							}
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	public static void removePuppet(ServerPlayer player) {
		try {
			try {
				for (ServerLevel level : player.level().getServer().getAllLevels()) {
					for (Entity e : level.getAllEntities()) {
						try {
							for (String tag : e.entityTags()) {
								if (tag.equals("morph_puppet-" + player.getUUID().toString())) {
									e.discard();
									break;
								}
							}
						} catch (Exception ignored) {
						}
					}
				}
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	// ---------- 공격 방식 ----------

	/** 원거리형의 투사체 종류 (근접형은 null) */
	private static String projectileOf(String typeId) {
		if (typeId == null) {
			return null;
		}
		return switch (typeId) {
			case "minecraft:skeleton", "minecraft:stray", "minecraft:bogged",
					"minecraft:pillager", "minecraft:parched", "minecraft:illusioner" -> "arrow";
			case "minecraft:breeze" -> "wind_charge";
		// 드라우너드는 바닐라에서 삼지창을 던진다 (화살 아님)
		case "minecraft:drowned" -> "trident";
			case "minecraft:blaze" -> "small_fireball";
			case "minecraft:ghast" -> "fireball";
			case "minecraft:snow_golem" -> "snowball";
			case "minecraft:llama", "minecraft:trader_llama" -> "spit";
			case "minecraft:shulker" -> "shulker_bullet";
			case "minecraft:ender_dragon" -> "dragon_fireball";
			case "minecraft:wither" -> "wither_skull";
			default -> null;
		};
	}

	/** 투사체 타입을 레지스트리에서 조회 (26.3에서 EntityType 상수 제거됨) */
	private static EntityType<?> projType(String kind) {
		String id = switch (kind) {
			case "arrow" -> "minecraft:arrow";
			case "trident" -> "minecraft:trident";
			case "wind_charge" -> "minecraft:wind_charge";
			case "small_fireball" -> "minecraft:small_fireball";
			case "fireball" -> "minecraft:fireball";
			case "snowball" -> "minecraft:snowball";
			case "spit" -> "minecraft:llama_spit";
			case "shulker_bullet" -> "minecraft:shulker_bullet";
			case "dragon_fireball" -> "minecraft:dragon_fireball";
			case "wither_skull" -> "minecraft:wither_skull";
			default -> "";
		};
		if (id.isEmpty()) {
			return null;
		}
		try {
			return BuiltInRegistries.ENTITY_TYPE
					.get(Identifier.parse(id)).map(h -> h.value()).orElse(null);
		} catch (Exception e) {
			return null;
		}
	}

	private static void shootProjectile(ServerPlayer player, String morphId, LivingEntity target) {
		shootProjectile(player, morphId, target, null);
	}

	/** forcedDir 가 있으면 그 방향(시선)으로, 없으면 target 방향으로 발사 */
	private static void shootProjectile(ServerPlayer player, String morphId, LivingEntity target, Vec3 forcedDir) {
		try {
			if (!(player.level() instanceof ServerLevel sl)) {
				return;
			}
			String kind = projectileOf(morphId);
			if (kind == null) {
				return;
			}
			EntityType<?> type = projType(kind);
			if (type == null) {
				return;
			}
			EntitySpawnRequest req = new EntitySpawnRequest(EntitySpawnReason.MOB_SUMMONED, true);
			Entity proj = null;
			try {
				proj = type.create(sl, req);
			} catch (Exception ignored) {
			}
			if (proj == null) {
				return;
			}
			Vec3 eye;
			try {
				eye = player.getEyePosition();
			} catch (Exception e) {
				eye = player.position().add(0, 1.6, 0);
			}
			try {
				proj.setPos(eye.x, eye.y - 0.2, eye.z);
			} catch (Exception ignored) {
			}
			Vec3 dir;
			if (forcedDir != null) {
				dir = forcedDir.normalize();
				try {
					// 시선 방향으로 약간 앞에서 생성 (자기 히트박스와 겹침 방지)
					proj.setPos(eye.x + dir.x * 0.6, eye.y - 0.2 + dir.y * 0.6, eye.z + dir.z * 0.6);
				} catch (Exception ignored) {
				}
			} else {
				try {
					dir = new Vec3(target.getX() - eye.x, (target.getY() + target.getBbHeight() * 0.5) - eye.y,
							target.getZ() - eye.z).normalize();
				} catch (Exception e) {
					dir = player.getLookAngle();
				}
			}
			float speed = kind.equals("arrow") ? 2.2f : 1.6f;
			try {
				if (proj instanceof net.minecraft.world.entity.projectile.Projectile p) {
					try {
						p.setOwner(player);
					} catch (Exception ignored2) {
					}
					try {
						p.shoot(dir.x, dir.y, dir.z, speed, 1.0f);
					} catch (Exception ignored3) {
						try {
							proj.setDeltaMovement(dir.scale(speed));
						} catch (Exception ignored4) {
						}
					}
				} else {
					proj.setDeltaMovement(dir.scale(speed));
				}
			} catch (Exception ignored5) {
			}
			try {
				sl.addFreshEntity(proj);
			} catch (Exception ignored) {
			}
			try {
				sl.playSound(null, player.blockPosition(),
						kind.contains("fire") || kind.contains("skull")
								? SoundEvents.BLAZE_SHOOT
								: SoundEvents.ARROW_SHOOT,
						SoundSource.PLAYERS, 0.8f, 1.0f);
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}


	// ---------- 능력 키 (R) ----------

	/** 플레이어 → 능력 재사용 가능 게임틱 */
	private static final Map<UUID, Long> ABILITY_COOL = new HashMap<>();

	private static int abilityCooldown(String kind) {
		return switch (kind) {
			case "small_fireball", "snowball" -> 6;
			case "arrow" -> 15;
			case "wind_charge", "spit", "wither_skull" -> 20;
			case "trident" -> 30;
			case "fireball", "dragon_fireball", "shulker_bullet" -> 40;
			default -> 20;
		};
	}

	/**
	 * 능력 키: 변신 몹의 고유 능력을 시선 방향으로 사용.
	 * - 엔더맨: 시선 끝(최대 32블록)으로 순간이동
	 * - 블레이즈·가스트·스켈레톤·눈골렘·위더 등 원거리형: 투사체 발사 (쿨다운은 종류별)
	 */
	public static void useAbility(ServerPlayer player) {
		try {
			String mid = morphId(player);
			if (mid == null || mid.isEmpty() || player.isSpectator()) {
				return;
			}
			if (!(player.level() instanceof ServerLevel sl)) {
				return;
			}
			long now = sl.getGameTime();
			Long next = ABILITY_COOL.get(player.getUUID());
			if (next != null && now < next) {
				return;
			}
			if (mid.equals("minecraft:enderman")) {
				if (enderTeleport(sl, player)) {
					ABILITY_COOL.put(player.getUUID(), now + 10);
				}
				return;
			}
			if (specialAbility(sl, player, mid, now)) {
				return;
			}
			String kind = projectileOf(mid);
			if (kind == null) {
				return;
			}
			ABILITY_COOL.put(player.getUUID(), now + abilityCooldown(kind));
			shootProjectile(player, mid, null, player.getLookAngle());
		} catch (Exception ignored) {
		}
	}

	/** 시선 방향 range칸 안의 가장 가까운 생물 (시야가 확보된 것만) */
	private static LivingEntity lookTarget(ServerLevel sl, ServerPlayer p, double range) {
		try {
			Vec3 eye = p.getEyePosition();
			Vec3 look = p.getLookAngle();
			net.minecraft.world.phys.AABB box = p.getBoundingBox().expandTowards(look.scale(range)).inflate(1.0);
			LivingEntity best = null;
			double bestT = range;
			for (Entity e : sl.getEntities(p, box, x -> x instanceof LivingEntity le && le.isAlive() && !x.isSpectator())) {
				Vec3 c = e.getBoundingBox().getCenter();
				double t = c.subtract(eye).dot(look);
				if (t < 0 || t > range) {
					continue;
				}
				if (eye.add(look.scale(t)).distanceTo(c) > Math.max(e.getBbWidth(), e.getBbHeight()) * 0.6 + 0.4) {
					continue;
				}
				if (!p.hasLineOfSight(e)) {
					continue;
				}
				if (t < bestT) {
					bestT = t;
					best = (LivingEntity) e;
				}
			}
			return best;
		} catch (Exception e) {
			return null;
		}
	}

	/** 투사체가 아닌 고유 능력. 처리했으면 true(대상이 없어 실패해도 true) */
	private static boolean specialAbility(ServerLevel sl, ServerPlayer p, String mid, long now) {
		try {
			switch (mid) {
				case "minecraft:guardian", "minecraft:elder_guardian" -> {
					LivingEntity t = lookTarget(sl, p, 15.0);
					if (t == null) {
						return true;
					}
					float dmg = mid.equals("minecraft:elder_guardian") ? 8.0f : 6.0f;
					t.hurtServer(sl, sl.damageSources().indirectMagic(p, p), dmg);
					beamParticles(sl, p.getEyePosition(), t.getBoundingBox().getCenter());
					sl.playSound(null, p.blockPosition(), SoundEvents.GUARDIAN_ATTACK, SoundSource.PLAYERS, 1.0f, 1.0f);
					ABILITY_COOL.put(p.getUUID(), now + 40);
					return true;
				}
				case "minecraft:warden" -> {
					LivingEntity t = lookTarget(sl, p, 15.0);
					if (t == null) {
						return true;
					}
					t.hurtServer(sl, sl.damageSources().sonicBoom(p), 10.0f);
					sl.sendParticles(ParticleTypes.SONIC_BOOM, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 1, 0, 0, 0, 0);
					sl.playSound(null, p.blockPosition(), SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.5f, 1.0f);
					ABILITY_COOL.put(p.getUUID(), now + 100);
					return true;
				}
				case "minecraft:witch" -> {
					LivingEntity t = lookTarget(sl, p, 12.0);
					if (t == null) {
						return true;
					}
					switch (sl.getRandom().nextInt(4)) {
						case 0 -> t.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0, false, true, true));
						case 1 -> t.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 200, 0, false, true, true));
						case 2 -> t.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0, false, true, true));
						default -> t.addEffect(new MobEffectInstance(MobEffects.INSTANT_DAMAGE, 1, 0, false, true, true));
					}
					sl.sendParticles(ParticleTypes.WITCH, t.getX(), t.getY() + t.getBbHeight() * 0.5, t.getZ(), 20, 0.3, 0.4, 0.3, 0.05);
					sl.playSound(null, p.blockPosition(), SoundEvents.WITCH_THROW, SoundSource.PLAYERS, 1.0f, 1.0f);
					ABILITY_COOL.put(p.getUUID(), now + 40);
					return true;
				}
				case "minecraft:evoker" -> {
					Vec3 look = p.getLookAngle();
					Vec3 flat = new Vec3(look.x, 0, look.z);
					if (flat.lengthSqr() < 1.0E-4) {
						return true;
					}
					flat = flat.normalize();
					for (int i = 1; i <= 6; i++) {
						double fx = p.getX() + flat.x * (1.0 + i * 1.4);
						double fz = p.getZ() + flat.z * (1.0 + i * 1.4);
						sl.addFreshEntity(new net.minecraft.world.entity.projectile.EvokerFangs(sl, fx, p.getY(), fz,
								p.getYRot(), i * 2, p));
					}
					sl.playSound(null, p.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 1.0f, 1.0f);
					ABILITY_COOL.put(p.getUUID(), now + 40);
					return true;
				}
				case "minecraft:ravager" -> {
					net.minecraft.world.phys.AABB box = p.getBoundingBox().inflate(5.0);
					for (Entity e : sl.getEntities(p, box, x -> x instanceof LivingEntity le && le.isAlive())) {
						try {
							LivingEntity le = (LivingEntity) e;
							le.hurtServer(sl, sl.damageSources().mobAttack(p), 6.0f);
							Vec3 away = new Vec3(e.getX() - p.getX(), 0, e.getZ() - p.getZ());
							if (away.lengthSqr() > 1.0E-4) {
								away = away.normalize();
								e.setDeltaMovement(e.getDeltaMovement().add(away.x * 0.9, 0.4, away.z * 0.9));
								e.needsSync = true;
							}
						} catch (Exception ignored) {
						}
					}
					sl.sendParticles(ParticleTypes.POOF, p.getX(), p.getY() + 0.5, p.getZ(), 30, 1.5, 0.3, 1.5, 0.05);
					sl.playSound(null, p.blockPosition(), SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0f, 1.0f);
					ABILITY_COOL.put(p.getUUID(), now + 60);
					return true;
				}
				default -> {
					return false;
				}
			}
		} catch (Exception e) {
			return true;
		}
	}

	private static void beamParticles(ServerLevel sl, Vec3 from, Vec3 to) {
		try {
			Vec3 d = to.subtract(from);
			int n = Math.max(2, (int) (d.length() * 2));
			for (int i = 0; i <= n; i++) {
				Vec3 pt = from.add(d.scale(i / (double) n));
				sl.sendParticles(ParticleTypes.CRIT, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
			}
		} catch (Exception ignored) {
		}
	}

	/** 변신체를 사냥하는 몹(프로필의 "적")이 근처에 있으면 플레이어를 타겟으로 지정 (1초마다) */
	private static void huntMorphed(ServerPlayer player, String morphType) {
		try {
			if (!MobProfiles.hasHunters(morphType) || player.isCreative() || player.isSpectator()) {
				return;
			}
			if (!(player.level() instanceof ServerLevel sl)) {
				return;
			}
			for (Entity e : sl.getEntities(player, player.getBoundingBox().inflate(16.0), x -> x instanceof Mob)) {
				try {
					Mob mob = (Mob) e;
					if (mob.getTarget() != null || !mob.isAlive()) {
						continue;
					}
					String mobType = BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()).toString();
					if (sameFaction(mobType, morphType)) {
						continue;
					}
					if (MobProfiles.isHunter(mobType, morphType) && mob.canAttack(player)) {
						mob.setTarget(player);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	/** 시선 방향 최대 32블록 안에서 서 있을 수 있는 가장 먼 지점으로 이동 */
	private static boolean enderTeleport(ServerLevel sl, ServerPlayer p) {
		try {
			Vec3 eye = p.getEyePosition();
			Vec3 look = p.getLookAngle();
			double range = 32.0;
			Vec3 end = eye.add(look.scale(range));
			net.minecraft.world.phys.BlockHitResult hit = sl.clip(new net.minecraft.world.level.ClipContext(eye, end,
					net.minecraft.world.level.ClipContext.Block.COLLIDER,
					net.minecraft.world.level.ClipContext.Fluid.NONE, p));
			double reach = range;
			if (hit != null && hit.getType() != net.minecraft.world.phys.HitResult.Type.MISS) {
				reach = Math.max(0.0, hit.getLocation().distanceTo(eye) - 0.4);
			}
			double eyeOff = eye.y - p.getY();
			net.minecraft.world.phys.AABB base = p.getBoundingBox();
			Vec3 from = p.position();
			for (double t = reach; t >= 1.0; t -= 1.0) {
				Vec3 c = eye.add(look.scale(t));
				double fx = c.x;
				double fy = c.y - eyeOff;
				double fz = c.z;
				net.minecraft.world.phys.AABB box = base.move(fx - from.x, fy - from.y, fz - from.z);
				if (!sl.noCollision(p, box)) {
					continue;
				}
				// 공중이면 바닥까지 내려감 (최대 12블록)
				int drop = 0;
				while (drop < 24 && sl.noCollision(p, box.move(0, -0.5, 0))) {
					box = box.move(0, -0.5, 0);
					fy -= 0.5;
					drop++;
				}
				if (drop >= 24) {
					continue;
				}
				if (sl.getFluidState(BlockPos.containing(fx, fy + 0.1, fz)).is(net.minecraft.tags.FluidTags.LAVA)) {
					continue;
				}
				sl.sendParticles(ParticleTypes.PORTAL, from.x, from.y + 1.0, from.z, 32, 0.4, 0.8, 0.4, 0.2);
				sl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
				p.teleportTo(fx, fy, fz);
				p.fallDistance = 0f;
				sl.sendParticles(ParticleTypes.PORTAL, fx, fy + 1.0, fz, 32, 0.4, 0.8, 0.4, 0.2);
				sl.playSound(null, p.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0f, 1.0f);
				return true;
			}
		} catch (Exception ignored) {
		}
		return false;
	}

	/**
	 * 근접 접촉 효과. 참고 모드 EffectAttack 보유 몹과 동일:
	 * 동굴거미 독7초·허스크 기아7초·위더스켈레톤/위더 위더10초·복어 독3초.
	 */
	private static void applyContactEffect(ServerPlayer attacker, String morphId, LivingEntity victim) {
		if (morphId == null || victim == null || !victim.isAlive()) {
			return;
		}
		try {
			switch (morphId) {
				case "minecraft:cave_spider" -> victim.addEffect(
						new MobEffectInstance(MobEffects.POISON, 140, 0, false, true, true));
				case "minecraft:husk" -> victim.addEffect(
						new MobEffectInstance(MobEffects.HUNGER, 140, 0, false, true, true));
				case "minecraft:wither_skeleton", "minecraft:wither" -> victim.addEffect(
						new MobEffectInstance(MobEffects.WITHER, 200, 0, false, true, true));
				case "minecraft:bee" -> victim.addEffect(
						new MobEffectInstance(MobEffects.POISON, 200, 0, false, true, true));
				case "minecraft:pufferfish" -> victim.addEffect(
						new MobEffectInstance(MobEffects.POISON, 60, 0, false, true, true));
				default -> {
				}
			}
		} catch (Exception ignored) {
		}
	}
}



