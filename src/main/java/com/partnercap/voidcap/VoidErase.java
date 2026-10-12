package com.partnercap.voidcap;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Set;
import java.util.UUID;

import com.partnercap.item.PartnerCapsuleItem;
import com.partnercap.util.TameUtil;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.npc.villager.AbstractVillager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.Vec3;

/**
 * 존재 삭제: 피해 기록 되돌리기 + 자식 연쇄 소멸 + 새끼-부모 연결 추적.
 */
public class VoidErase {

	private record Dmg(UUID attacker, UUID victim, float amount, long time) {
	}

	private record DeathSnap(CompoundTag tag, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,
			Vec3 pos, UUID killer, long time) {
	}

	/** 존재 삭제 되돌림 유효기간: 살해→부활, 피해 회복, 거래 되돌림, 액체 회수 전부 2시간 */
	private static final long REWIND_WINDOW_MS = 7200000L;

	private static final List<Dmg> DMG_LOG = new ArrayList<>();
	private static final Map<UUID, DeathSnap> DEATH_SNAPS = new HashMap<>();
	private static final Set<UUID> KNOWN_BABIES = new LinkedHashSet<>();
	private static final Map<UUID, List<UUID>> BABY_PARENTS = new HashMap<>();

	private record TradeSession(UUID trader, Map<String, Integer> snap) {
	}

	private record TradeRecord(UUID player, Map<String, Integer> delta, long time) {
	}

	private static final Map<UUID, TradeSession> SESSIONS = new HashMap<>();
	private static final Map<UUID, List<TradeRecord>> TRADE_LOG = new HashMap<>();
	private static final Map<UUID, Long> ZV_SEEN = new HashMap<>();

	private record DevourLiquid(net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,
			net.minecraft.core.BlockPos pos, long time) {
	}

	private static final Map<UUID, List<DevourLiquid>> DEVOUR_LIQUID = new HashMap<>();
	private static final Map<UUID, Integer> LAST_LIQUID_CLEARED = new HashMap<>();

	private record PendingDrops(UUID victim, net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim,
			Vec3 pos, Set<UUID> keep, long due) {
	}

	private static final List<PendingDrops> PENDING_DROPS = new ArrayList<>();
	private static final Map<UUID, List<UUID>> VICTIM_DROPS = new HashMap<>();
	private static final Map<UUID, Integer> LAST_DROPS_CLEARED = new HashMap<>();

	/** 사망 직후 기존 드롭 제외하고 피해자 드롭만 확정 (1초 뒤 판정) */
	private static void queueDropCapture(UUID vid,
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim, Vec3 pos, Set<UUID> keep) {
		try {
			synchronized (PENDING_DROPS) {
				PENDING_DROPS.add(new PendingDrops(vid, dim, pos, keep, System.currentTimeMillis() + 1000L));
				while (PENDING_DROPS.size() > 100) {
					PENDING_DROPS.remove(0);
				}
			}
		} catch (Exception ignored) {
		}
	}

	private static void sweepPendingDrops(MinecraftServer server) {
		try {
			List<PendingDrops> due = new ArrayList<>();
			synchronized (PENDING_DROPS) {
				long now = System.currentTimeMillis();
				var it = PENDING_DROPS.iterator();
				while (it.hasNext()) {
					PendingDrops p = it.next();
					if (now >= p.due()) {
						due.add(p);
						it.remove();
					}
				}
			}
			for (PendingDrops p : due) {
				try {
					ServerLevel dest = null;
					for (ServerLevel l : server.getAllLevels()) {
						try {
							if (l.dimension().equals(p.dim())) {
								dest = l;
								break;
							}
						} catch (Exception ignored) {
						}
					}
					if (dest == null) {
						continue;
					}
					net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
							p.pos().x - 6, p.pos().y - 6, p.pos().z - 6,
							p.pos().x + 6, p.pos().y + 6, p.pos().z + 6);
					List<UUID> fresh = new ArrayList<>();
					for (Entity e : dest.getEntities((Entity) null, box,
							x -> (x instanceof net.minecraft.world.entity.item.ItemEntity
									|| x instanceof net.minecraft.world.entity.ExperienceOrb))) {
						try {
							if (!p.keep().contains(e.getUUID())) {
								fresh.add(e.getUUID());
							}
						} catch (Exception ignored) {
						}
					}
					if (!fresh.isEmpty()) {
						synchronized (VICTIM_DROPS) {
							VICTIM_DROPS.put(p.victim(), fresh);
							while (VICTIM_DROPS.size() > 200) {
								try {
									VICTIM_DROPS.remove(VICTIM_DROPS.keySet().iterator().next());
								} catch (Exception ex) {
									break;
								}
							}
						}
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	private static int clearVictimDrops(MinecraftServer server, UUID vid) {
		List<UUID> ids;
		synchronized (VICTIM_DROPS) {
			ids = VICTIM_DROPS.remove(vid);
		}
		if (server == null || ids == null || ids.isEmpty()) {
			return 0;
		}
		int n = 0;
		for (UUID id : ids) {
			try {
				Entity e = findEntity(server, id);
				if (e instanceof net.minecraft.world.entity.item.ItemEntity
						|| e instanceof net.minecraft.world.entity.ExperienceOrb) {
					e.discard();
					n++;
				}
			} catch (Exception ignored) {
			}
		}
		return n;
	}

	public static int takeDropsCleared(UUID killerId) {
		synchronized (LAST_DROPS_CLEARED) {
			Integer v = LAST_DROPS_CLEARED.remove(killerId);
			return v == null ? 0 : v;
		}
	}

	/** 먹보 사망 시 뿌린 검은 액체 기록 (부활 때 같이 회수용) */
	public static void noteDevourerLiquid(ServerLevel level, UUID devourerId, net.minecraft.core.BlockPos pos) {
		if (level == null || devourerId == null || pos == null) {
			return;
		}
		try {
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim = level.dimension();
			synchronized (DEVOUR_LIQUID) {
				List<DevourLiquid> list = DEVOUR_LIQUID.computeIfAbsent(devourerId, k -> new ArrayList<>());
				list.add(new DevourLiquid(dim, pos.immutable(), System.currentTimeMillis()));
				while (list.size() > 12) {
					list.remove(0);
				}
				while (DEVOUR_LIQUID.size() > 500) {
					try {
						DEVOUR_LIQUID.remove(DEVOUR_LIQUID.keySet().iterator().next());
					} catch (Exception ex) {
						break;
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	private static int clearDevourerLiquid(MinecraftServer server, UUID devourerId) {
		List<DevourLiquid> list;
		synchronized (DEVOUR_LIQUID) {
			list = DEVOUR_LIQUID.remove(devourerId);
		}
		if (server == null || list == null || list.isEmpty()) {
			return 0;
		}
		int n = 0;
		long now = System.currentTimeMillis();
		for (DevourLiquid d : list) {
			try {
				if (now - d.time() > REWIND_WINDOW_MS) {
					continue;
				}
				ServerLevel dest = null;
				for (ServerLevel l : server.getAllLevels()) {
					try {
						if (l.dimension().equals(d.dim())) {
							dest = l;
							break;
						}
					} catch (Exception ignored) {
					}
				}
				if (dest == null) {
					continue;
				}
				boolean isVoid = false;
				try {
					isVoid = dest.getBlockState(d.pos())
							.is(com.partnercap.fluid.ModBlocks.VOID_LIQUID);
				} catch (Exception ignored) {
				}
				if (!isVoid) {
					continue;
				}
				try {
					dest.setBlock(d.pos(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
					n++;
				} catch (Exception ignored) {
				}
			} catch (Exception ignored) {
			}
		}
		return n;
	}

	public static int takeLiquidCleared(UUID killerId) {
		synchronized (LAST_LIQUID_CLEARED) {
			Integer v = LAST_LIQUID_CLEARED.remove(killerId);
			return v == null ? 0 : v;
		}
	}

	public static void register() {
		try {
			ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity dead, DamageSource source) -> {
				try {
					if (dead instanceof Player) {
						return;
					}
					if (!(dead.level() instanceof ServerLevel level)) {
						return;
					}
					UUID killer = null;
					try {
						Entity a = source.getEntity();
						if (a != null) {
							killer = a.getUUID();
						}
					} catch (Exception ignored) {
					}
					CompoundTag tag = null;
					try {
						tag = com.partnercap.util.TameUtil.savePet(dead);
					} catch (Exception ignored) {
					}
					if (tag == null) {
						return;
					}
					UUID vid;
					try {
						vid = dead.getUUID();
					} catch (Exception e) {
						return;
					}
					net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim;
					try {
						dim = level.dimension();
					} catch (Exception e) {
						return;
					}
					Vec3 pos;
					try {
						pos = dead.position();
					} catch (Exception e) {
						return;
					}
					synchronized (DEATH_SNAPS) {
						DEATH_SNAPS.put(vid, new DeathSnap(tag, dim, pos, killer, System.currentTimeMillis()));
						while (DEATH_SNAPS.size() > 200) {
							try {
								DEATH_SNAPS.remove(DEATH_SNAPS.keySet().iterator().next());
							} catch (Exception ex) {
								break;
							}
						}
					}
					// 부활 때 같이 치울 드롭 확정용: 사망 전부터 있던 드롭은 제외
					try {
						Set<UUID> keep = new HashSet<>();
						try {
							net.minecraft.world.phys.AABB box = new net.minecraft.world.phys.AABB(
									pos.x - 6, pos.y - 6, pos.z - 6, pos.x + 6, pos.y + 6, pos.z + 6);
							for (Entity e : level.getEntities((Entity) null, box,
									x -> (x instanceof net.minecraft.world.entity.item.ItemEntity
											|| x instanceof net.minecraft.world.entity.ExperienceOrb))) {
								keep.add(e.getUUID());
							}
						} catch (Exception ignored) {
						}
						queueDropCapture(vid, dim, pos, keep);
					} catch (Exception ignored) {
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
		try {
			ServerLivingEntityEvents.AFTER_DAMAGE.register(
					(LivingEntity entity, DamageSource source, float baseDamage, float taken, boolean blocked) -> {
						try {
							if (taken <= 0) {
								return;
							}
							Entity a = null;
							try {
								a = source.getEntity();
							} catch (Exception ignored) {
							}
							UUID au = null;
							try {
								if (a != null) {
									au = a.getUUID();
								}
							} catch (Exception ignored) {
							}
							synchronized (DMG_LOG) {
								DMG_LOG.add(new Dmg(au, entity.getUUID(), taken, System.currentTimeMillis()));
								if (DMG_LOG.size() > 3000) {
									DMG_LOG.subList(0, 1000).clear();
								}
							}
						} catch (Exception ignored) {
						}
					});
		} catch (Exception ignored) {
		}
		try {
			net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
				try {
					if (entity instanceof AgeableMob baby) {
						linkBaby(baby, level);
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
		try {
			ServerTickEvents.END_SERVER_TICK.register(VoidErase::tickCensus);
		} catch (Exception ignored) {
		}
		try {
			ServerTickEvents.END_SERVER_TICK.register(VoidErase::sweepPendingDrops);
		} catch (Exception ignored) {
		}
	}

	private static void linkBaby(AgeableMob baby, ServerLevel level) {
		UUID id;
		try {
			id = baby.getUUID();
		} catch (Exception e) {
			return;
		}
		boolean isBaby = false;
		try {
			isBaby = baby.isBaby();
		} catch (Exception ignored) {
			return;
		}
		if (!isBaby) {
			return;
		}
		boolean known;
		synchronized (KNOWN_BABIES) {
			known = !KNOWN_BABIES.add(id);
			if (KNOWN_BABIES.size() > 2000) {
				IteratorPurge.purge(KNOWN_BABIES, 1000);
			}
		}
		if (known) {
			return;
		}
		try {
			AABB box = baby.getBoundingBox().inflate(6.0);
			List<UUID> parents = new ArrayList<>();
			for (Entity e : level.getEntities(baby, box, x -> x instanceof AgeableMob)) {
				AgeableMob a = (AgeableMob) e;
				boolean adult = true;
				try {
					adult = !a.isBaby();
				} catch (Exception ignored) {
				}
				if (!adult) {
					continue;
				}
				try {
					if (a.getType() == baby.getType()) {
						parents.add(a.getUUID());
					}
				} catch (Exception ignored) {
				}
			}
			if (!parents.isEmpty()) {
				synchronized (BABY_PARENTS) {
					BABY_PARENTS.put(id, parents);
				}
			}
		} catch (Exception ignored) {
		}
	}

	private static void tickCensus(MinecraftServer server) {
		try {
			tickTrades(server);
		} catch (Exception ignored) {
		}
		try {
			if (server.getTickCount() % 100 != 0) {
				return;
			}
			for (ServerLevel level : server.getAllLevels()) {
				List<AgeableMob> babies = new ArrayList<>();
				try {
					for (Entity e : level.getAllEntities()) {
						if (e instanceof AgeableMob baby) {
							boolean isBaby = false;
							try {
								isBaby = baby.isBaby();
							} catch (Exception ignored) {
							}
							if (!isBaby) {
								continue;
							}
							babies.add(baby);
						}
						try {
							if (e instanceof net.minecraft.world.entity.monster.zombie.ZombieVillager zv) {
								UUID zid = null;
								try {
									zid = zv.getUUID();
								} catch (Exception ignored) {
								}
								if (zid != null) {
									boolean isNew = false;
									synchronized (ZV_SEEN) {
										if (!ZV_SEEN.containsKey(zid)) {
											ZV_SEEN.put(zid, System.currentTimeMillis());
											isNew = true;
										}
										if (ZV_SEEN.size() > 1000) {
											long cutoff = System.currentTimeMillis() - REWIND_WINDOW_MS;
											ZV_SEEN.entrySet().removeIf(en -> en.getValue() < cutoff);
										}
									}
									// 새로 전향된 좀비주민: 같은 사건의 주민 사망 스냅샷은 치유로 처리하므로 부활 명단에서 제거 (중복 방지)
									if (isNew) {
										try {
											double zx = zv.getX();
											double zy = zv.getY();
											double zz = zv.getZ();
											long now = System.currentTimeMillis();
											synchronized (DEATH_SNAPS) {
												var it = DEATH_SNAPS.entrySet().iterator();
												while (it.hasNext()) {
													var en = it.next();
													try {
														DeathSnap s = en.getValue();
														if (now - s.time() > 180000L) {
															continue;
														}
														String sid = "";
														try {
															sid = s.tag().getString("id").orElse("");
														} catch (Exception ignored) {
														}
														if (!sid.equals("minecraft:villager")) {
															continue;
														}
														double dx = s.pos().x - zx;
														double dy = s.pos().y - zy;
														double dz = s.pos().z - zz;
														if (dx * dx + dy * dy + dz * dz < 1600.0) {
															it.remove();
														}
													} catch (Exception ignored) {
													}
												}
											}
										} catch (Exception ignored) {
										}
									}
								}
							}
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
					continue;
				}
				for (AgeableMob baby : babies) {
					UUID id;
					try {
						id = baby.getUUID();
					} catch (Exception e) {
						continue;
					}
					boolean known;
					synchronized (KNOWN_BABIES) {
						known = !KNOWN_BABIES.add(id);
						if (KNOWN_BABIES.size() > 2000) {
							IteratorPurge.purge(KNOWN_BABIES, 1000);
						}
					}
					if (known) {
						continue;
					}
					// 새로 태어난 새끼: 주변 동종 성체와 연결
					try {
						AABB box = baby.getBoundingBox().inflate(6.0);
						List<UUID> parents = new ArrayList<>();
						for (Entity e : level.getEntities(baby, box,
								x -> x instanceof AgeableMob)) {
							AgeableMob a = (AgeableMob) e;
							boolean adult = true;
							try {
								adult = !a.isBaby();
							} catch (Exception ignored) {
							}
							if (!adult) {
								continue;
							}
							try {
								if (a.getType() == baby.getType()) {
									parents.add(a.getUUID());
								}
							} catch (Exception ignored) {
							}
						}
						if (!parents.isEmpty()) {
							synchronized (BABY_PARENTS) {
								BABY_PARENTS.put(id, parents);
							}
						}
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	private static Map<String, Integer> invSnapshot(ServerPlayer player) {
		Map<String, Integer> out = new HashMap<>();
		try {
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack s;
				try {
					s = player.getInventory().getItem(i);
				} catch (Exception e) {
					continue;
				}
				if (s.isEmpty()) {
					continue;
				}
				String id;
				try {
					id = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
				} catch (Exception e) {
					continue;
				}
				int c = 0;
				try {
					c = s.getCount();
				} catch (Exception e) {
					continue;
				}
				out.put(id, out.getOrDefault(id, 0) + c);
			}
		} catch (Exception ignored) {
		}
		return out;
	}

	private static UUID openTrader(ServerPlayer player) {
		try {
			if (!(player.containerMenu instanceof MerchantMenu)) {
				return null;
			}
			AABB box = player.getBoundingBox().inflate(8.0);
			for (Entity e : player.level().getEntities(player, box, x -> x instanceof AbstractVillager)) {
				try {
					AbstractVillager v = (AbstractVillager) e;
					Player tp = null;
					try {
						tp = v.getTradingPlayer();
					} catch (Exception ignored) {
					}
					if (tp == player) {
						return v.getUUID();
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	/** 상인 메뉴 열림/닫힘 추적: 순수 거래 증감만 기록 */
	private static void tickTrades(MinecraftServer server) {
		java.util.Set<UUID> online = new HashSet<>();
		List<ServerPlayer> players;
		try {
			players = server.getPlayerList().getPlayers();
		} catch (Exception e) {
			return;
		}
		for (ServerPlayer p : players) {
			UUID id;
			try {
				id = p.getUUID();
			} catch (Exception e) {
				continue;
			}
			online.add(id);
			UUID trader = null;
			try {
				trader = openTrader(p);
			} catch (Exception ignored) {
			}
			TradeSession cur = null;
			try {
				cur = SESSIONS.get(id);
			} catch (Exception ignored) {
			}
			UUID curTrader = cur == null ? null : cur.trader();
			boolean same = trader != null && trader.equals(curTrader);
			if (!same) {
				if (cur != null) {
					try {
						Map<String, Integer> now = invSnapshot(p);
						Map<String, Integer> delta = new HashMap<>();
						java.util.Set<String> keys = new HashSet<>(cur.snap().keySet());
						keys.addAll(now.keySet());
						for (String k : keys) {
							int d = now.getOrDefault(k, 0) - cur.snap().getOrDefault(k, 0);
							if (d != 0) {
								delta.put(k, d);
							}
						}
						if (!delta.isEmpty() && curTrader != null) {
							List<TradeRecord> list = TRADE_LOG.get(curTrader);
							if (list == null) {
								list = new ArrayList<>();
								TRADE_LOG.put(curTrader, list);
							}
							list.add(new TradeRecord(id, delta, System.currentTimeMillis()));
							int total = 0;
							for (List<TradeRecord> l : TRADE_LOG.values()) {
								total += l.size();
							}
							if (total > 300) {
								for (List<TradeRecord> l : TRADE_LOG.values()) {
									while (!l.isEmpty() && total > 200) {
										l.remove(0);
										total--;
									}
								}
							}
						}
					} catch (Exception ignored) {
					}
					try {
						SESSIONS.remove(id);
					} catch (Exception ignored) {
					}
				}
				if (trader != null) {
					try {
						SESSIONS.put(id, new TradeSession(trader, invSnapshot(p)));
					} catch (Exception ignored) {
					}
				}
			}
		}
		try {
			SESSIONS.keySet().removeIf(k -> !online.contains(k));
		} catch (Exception ignored) {
		}
	}

	private static net.minecraft.world.item.Item itemById(String id) {
		try {
			int i = id.indexOf(':');
			if (i < 0) {
				return null;
			}
			net.minecraft.resources.Identifier rid = net.minecraft.resources.Identifier.fromNamespaceAndPath(
					id.substring(0, i), id.substring(i + 1));
			Object got = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(rid).orElse(null);
			if (got instanceof net.minecraft.world.item.Item item) {
				return item;
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private static int takeFromPlayer(ServerPlayer player, String id, int count) {
		int left = count;
		try {
			for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
				ItemStack s;
				try {
					s = player.getInventory().getItem(i);
				} catch (Exception e) {
					continue;
				}
				if (s.isEmpty()) {
					continue;
				}
				String sid;
				try {
					sid = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
				} catch (Exception e) {
					continue;
				}
				if (!sid.equals(id)) {
					continue;
				}
				int take = 0;
				try {
					take = Math.min(left, s.getCount());
					s.shrink(take);
				} catch (Exception e) {
					continue;
				}
				left -= take;
			}
		} catch (Exception ignored) {
		}
		return count - left;
	}

	/** 해당 주민과의 거래 되돌리기. 반환: 되돌린 거래 건수 */
	private static int revertTrades(ServerPlayer player, UUID villagerId) {
		List<TradeRecord> list;
		try {
			list = TRADE_LOG.remove(villagerId);
		} catch (Exception e) {
			return 0;
		}
		if (list == null || list.isEmpty()) {
			return 0;
		}
		int n = 0;
		for (TradeRecord r : list) {
			try {
				if (!r.player().equals(player.getUUID())) {
					continue;
				}
				if (applyTradeRecord(player, r)) {
					n++;
				}
			} catch (Exception ignored) {
			}
		}
		return n;
	}

	private static boolean applyTradeRecord(ServerPlayer player, TradeRecord r) {
		boolean touched = false;
		for (var e : r.delta().entrySet()) {
			int d = e.getValue();
			if (d > 0) {
				int took = takeFromPlayer(player, e.getKey(), d);
				if (took > 0) {
					touched = true;
				}
			} else if (d < 0) {
				net.minecraft.world.item.Item item = itemById(e.getKey());
				if (item == null) {
					continue;
				}
				int left = -d;
				while (left > 0) {
					int m = Math.min(left, 64);
					ItemStack back = new ItemStack(item);
					try {
						back.setCount(m);
					} catch (Exception ex) {
						break;
					}
					try {
						com.partnercap.util.TameUtil.giveToPlayer(player, back);
						touched = true;
					} catch (Exception ex) {
						break;
					}
					left -= m;
				}
			}
		}
		return touched;
	}

	private static ServerPlayer findPlayer(MinecraftServer server, UUID id) {		try {
			for (ServerPlayer p : server.getPlayerList().getPlayers()) {
				try {
					if (p.getUUID().equals(id)) {
						return p;
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	/** 거래 상대가 오프라인이어도 되는 버전 (먹보 도깨비 등 플레이어 없이 호출) */
	private static int revertTradesFor(MinecraftServer server, UUID villagerId) {
		List<TradeRecord> list;
		try {
			list = TRADE_LOG.get(villagerId);
		} catch (Exception e) {
			return 0;
		}
		if (list == null || list.isEmpty()) {
			return 0;
		}
		int n = 0;
		List<TradeRecord> rest = new ArrayList<>();
		for (TradeRecord r : new ArrayList<>(list)) {
			ServerPlayer p = null;
			try {
				p = findPlayer(server, r.player());
			} catch (Exception ignored) {
			}
			if (p == null) {
				rest.add(r);
				continue;
			}
			boolean ok = false;
			try {
				ok = applyTradeRecord(p, r);
			} catch (Exception ignored) {
			}
			if (ok) {
				n++;
			} else {
				rest.add(r);
			}
		}
		try {
			if (rest.isEmpty()) {
				TRADE_LOG.remove(villagerId);
			} else {
				TRADE_LOG.put(villagerId, rest);
			}
		} catch (Exception ignored) {
		}
		return n;
	}

	private static class IteratorPurge {
		static void purge(Set<UUID> set, int n) {
			try {
				var it = set.iterator();
				for (int i = 0; i < n && it.hasNext(); i++) {
					it.next();
					it.remove();
				}
			} catch (Exception ignored) {
			}
		}
	}

	private static void clearHarmful(LivingEntity living) {
		try {
			living.removeEffect(MobEffects.POISON);
		} catch (Exception ignored) {
		}
		try {
			living.removeEffect(MobEffects.WITHER);
		} catch (Exception ignored) {
		}
		try {
			living.removeEffect(MobEffects.WEAKNESS);
		} catch (Exception ignored) {
		}
		try {
			living.removeEffect(MobEffects.SLOWNESS);
		} catch (Exception ignored) {
		}
		try {
			living.removeEffect(MobEffects.MINING_FATIGUE);
		} catch (Exception ignored) {
		}
		try {
			living.removeEffect(MobEffects.UNLUCK);
		} catch (Exception ignored) {
		}
		try {
			living.removeEffect(MobEffects.BLINDNESS);
		} catch (Exception ignored) {
		}
		try {
			living.removeEffect(MobEffects.NAUSEA);
		} catch (Exception ignored) {
		}
		try {
			living.removeEffect(MobEffects.HUNGER);
		} catch (Exception ignored) {
		}
		try {
			living.removeEffect(MobEffects.LEVITATION);
		} catch (Exception ignored) {
		}
		try {
			living.removeEffect(MobEffects.DARKNESS);
		} catch (Exception ignored) {
		}
		try {
			living.removeEffect(MobEffects.BAD_OMEN);
		} catch (Exception ignored) {
		}
	}

	static Entity findEntity(MinecraftServer server, UUID id) {
		try {
			for (ServerLevel level : server.getAllLevels()) {
				try {
					for (Entity e : level.getAllEntities()) {
						try {
							if (e.getUUID().equals(id)) {
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

	static UUID tagUuid(CompoundTag tag) {
		try {
			int[] arr = tag.getIntArray("UUID").orElse(null);
			if (arr == null || arr.length != 4) {
				return null;
			}
			return UUIDUtil.uuidFromIntArray(arr);
		} catch (Exception e) {
			return null;
		}
	}

	/** 캡슐 index번 항목 존재 삭제. 피해 되돌림 + 자식 연쇄 소멸. */
	public static float healVictims(MinecraftServer server, UUID targetId) {
		float healed = 0f;
		try {
			long now = System.currentTimeMillis();
			List<Dmg> hits = new ArrayList<>();
			synchronized (DMG_LOG) {
				for (Dmg d : DMG_LOG) {
					if (targetId.equals(d.attacker()) && now - d.time() < REWIND_WINDOW_MS) {
						hits.add(d);
					}
				}
			}
			java.util.Map<UUID, Float> perVictim = new java.util.HashMap<>();
			for (Dmg d : hits) {
				perVictim.put(d.victim(), perVictim.getOrDefault(d.victim(), 0f) + d.amount());
			}
			for (var e : perVictim.entrySet()) {
				try {
					Entity v = findEntity(server, e.getKey());
					if (v instanceof LivingEntity living && living.isAlive()) {
						float max;
						float oldH;
						try {
							max = living.getMaxHealth();
							oldH = living.getHealth();
						} catch (Exception ex) {
							continue;
						}
						float nhp = Math.min(max, oldH + e.getValue());
						try {
							living.setHealth(nhp);
							if (nhp > oldH + 0.01f) {
								healed += (nhp - oldH);
								clearHarmful(living);
							}
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		return healed;
	}

	public static int removeOffspring(MinecraftServer server, UUID targetId) {
		int removedKids = 0;
		try {
			Set<UUID> queue = new LinkedHashSet<>();
			queue.add(targetId);
			Set<UUID> done = new HashSet<>();
			while (!queue.isEmpty()) {
				UUID cur;
				try {
					cur = queue.iterator().next();
					queue.remove(cur);
				} catch (Exception e) {
					break;
				}
				if (!done.add(cur)) {
					continue;
				}
				List<UUID> kids = new ArrayList<>();
				synchronized (BABY_PARENTS) {
					for (var en : BABY_PARENTS.entrySet()) {
						try {
							if (en.getValue().contains(cur)) {
								kids.add(en.getKey());
							}
						} catch (Exception ignored) {
						}
					}
					for (UUID k : kids) {
						BABY_PARENTS.remove(k);
					}
				}
				synchronized (KNOWN_BABIES) {
					KNOWN_BABIES.removeAll(kids);
				}
				for (UUID kid : kids) {
					try {
						Entity ke = findEntity(server, kid);
						if (ke != null) {
							ke.discard();
							removedKids++;
						}
					} catch (Exception ignored) {
					}
					queue.add(kid);
				}
			}
		} catch (Exception ignored) {
		}
		return removedKids;
	}

	public static List<ItemStack> collectGear(CompoundTag tag) {
		List<ItemStack> out = new ArrayList<>();
		try {
			String equip = tag.getString("VoidEquip").orElse("");
			if (!equip.isEmpty()) {
				for (String ent : equip.split(";")) {
					int ci = ent.lastIndexOf(',');
					if (ci < 0) {
						continue;
					}
					Item item = itemById(ent.substring(0, ci));
					if (item == null) {
						continue;
					}
					int cnt = 1;
					try {
						cnt = Math.max(1, Math.min(64, Integer.parseInt(ent.substring(ci + 1))));
					} catch (Exception ignored) {
					}
					ItemStack back = new ItemStack(item);
					try {
						back.setCount(cnt);
					} catch (Exception ignored) {
					}
					out.add(back);
				}
			}
		} catch (Exception ignored) {
		}
		try {
			CompoundTag carried = tag.getCompound("carried").orElse(null);
			if (carried != null && !carried.isEmpty()) {
				String bname = carried.getString("Name").orElse("");
				if (!bname.isEmpty()) {
					Object bobj = null;
					try {
						int ci2 = bname.indexOf(':');
						if (ci2 >= 0) {
							bobj = net.minecraft.core.registries.BuiltInRegistries.BLOCK
									.getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath(
											bname.substring(0, ci2), bname.substring(ci2 + 1)))
									.orElse(null);
						}
					} catch (Exception ignored) {
					}
					if (bobj instanceof net.minecraft.world.level.block.Block block) {
						try {
							Item bi = block.asItem();
							if (bi != null && bi != net.minecraft.world.item.Items.AIR) {
								out.add(new ItemStack(bi));
							}
						} catch (Exception ignored) {
						}
					}
				}
			}
		} catch (Exception ignored) {
		}
		return out;
	}

	/** 먹보 도깨비 등이 삼킨 경우: 같은 되돌리기를 월드에 직접 적용 */
	public static void devourRewind(ServerLevel level, LivingEntity victim) {
		devourRewind(level, victim, null);
	}

	public static void devourRewind(ServerLevel level, LivingEntity victim, UUID devourerId) {
		MinecraftServer server = null;
		UUID vid = null;
		try {
			server = level.getServer();
			vid = victim.getUUID();
		} catch (Exception e) {
			return;
		}
		if (server == null || vid == null) {
			return;
		}
		try {
			healVictims(server, vid);
		} catch (Exception ignored) {
		}
		// 먹힌 자가 석화시킨 애들은 즉시 해제 (공허 존재 삭제와 동일 취급)
		try {
			com.partnercap.medusa.Petrify.releaseByErased(server, vid);
		} catch (Exception ignored) {
		}
		// 먹힌 피해자의 새끼는 건드리지 않음: 먹힘은 살해이지 존재 삭제가 아니라서
		// 자식 연쇄 소멸은 공허 존재 삭제(erase)에서만 수행. 여기서 지우면 DeathSnap 없이
		// discard되어 devourer를 지워도 새끼가 부활하지 않는 버그가 됨.
		try {
			revertTradesFor(server, vid);
		} catch (Exception ignored) {
		}
		try {
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				ItemStack s;
				try {
					s = victim.getItemBySlot(slot);
				} catch (Exception e) {
					continue;
				}
				if (s == null || s.isEmpty()) {
					continue;
				}
				try {
					net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
							level, victim.getX(), victim.getY() + 1, victim.getZ(), s.copy());
					level.addFreshEntity(drop);
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		CompoundTag tag = null;
		try {
			tag = TameUtil.savePet(victim);
		} catch (Exception ignored) {
		}
		if (tag != null) {
			try {
				for (ItemStack gear : collectGear(tag)) {
					try {
						net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
								level, victim.getX(), victim.getY() + 1, victim.getZ(), gear);
						level.addFreshEntity(drop);
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
		}
	}

	/** 좀비계열 삭제 시 주변 좀비주민 치료 (원래 주민으로 복귀, 데이터 유지) */
	private static int cureNearbyZombies(MinecraftServer server, ServerPlayer player, String erasedTypeId) {
		String t = erasedTypeId == null ? "" : erasedTypeId.toLowerCase();
		boolean zombieFam = t.contains("zomb") || t.contains("husk") || t.contains("drowned");
		if (!zombieFam) {
			return 0;
		}
		long now = System.currentTimeMillis();
		List<UUID> cands = new ArrayList<>();
		synchronized (ZV_SEEN) {
			for (var e : ZV_SEEN.entrySet()) {
				if (now - e.getValue() < REWIND_WINDOW_MS) {
					cands.add(e.getKey());
				}
			}
		}
		int n = 0;
		for (UUID id : cands) {
			try {
				Entity e = findEntity(server, id);
				if (!(e instanceof net.minecraft.world.entity.monster.zombie.ZombieVillager zv)) {
					continue;
				}
				try {
					if (zv.isConverting()) {
						continue;
					}
				} catch (Exception ignored) {
				}
				boolean inRange = false;
				try {
					inRange = zv.distanceToSqr(player) < 2304.0;
				} catch (Exception ignored) {
				}
				if (!inRange) {
					continue;
				}
				try {
					java.lang.reflect.Method m = net.minecraft.world.entity.monster.zombie.ZombieVillager.class
							.getDeclaredMethod("startConverting", UUID.class, int.class);
					m.setAccessible(true);
					m.invoke(zv, player.getUUID(), 1);
					n++;
				} catch (Exception ignored) {
				}
			} catch (Exception ignored) {
			}
		}
		return n;
	}

	/** 지운 놈에게 죽은 피해자 부활 (되돌림 유효기간 이내) */
	public static int reviveVictims(MinecraftServer server, UUID killerId) {
		int n = 0;
		int liquidGone = 0;
		int dropsGone = 0;
		List<UUID> matched = new ArrayList<>();
		synchronized (DEATH_SNAPS) {
			long now = System.currentTimeMillis();
			for (var e : DEATH_SNAPS.entrySet()) {
				try {
					DeathSnap s = e.getValue();
					if (now - s.time() > REWIND_WINDOW_MS) {
						continue;
					}
					if (s.killer() != null && s.killer().equals(killerId)) {
						matched.add(e.getKey());
					}
				} catch (Exception ignored) {
				}
			}
		}
		for (UUID vid : matched) {
			DeathSnap s;
			synchronized (DEATH_SNAPS) {
				s = DEATH_SNAPS.get(vid);
			}
			if (s == null) {
				continue;
			}
			try {
				if (findEntity(server, vid) != null) {
					synchronized (DEATH_SNAPS) {
						DEATH_SNAPS.remove(vid);
					}
					continue;
				}
			} catch (Exception ignored) {
			}
			ServerLevel dest = null;
			try {
				for (ServerLevel l : server.getAllLevels()) {
					try {
						if (l.dimension().equals(s.dim())) {
							dest = l;
							break;
						}
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			if (dest == null) {
				continue;
			}
			try {
				CompoundTag tag = s.tag().copy();
				try {
					tag.putShort("DeathTime", (short) 0);
				} catch (Exception ignored) {
				}
				try {
					tag.putShort("HurtTime", (short) 0);
				} catch (Exception ignored) {
				}
				EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.LOAD, true);
				Entity created = EntityType.loadEntityRecursive(tag, dest, request, EntityProcessor.NOP);
				if (created == null) {
					continue;
				}
				created.setPos(s.pos().x, s.pos().y, s.pos().z);
				if (created instanceof LivingEntity living) {
					try {
						living.setHealth(living.getMaxHealth());
					} catch (Exception ignored) {
					}
				}
				dest.addFreshEntityWithPassengers(created);
				synchronized (DEATH_SNAPS) {
					DEATH_SNAPS.remove(vid);
				}
				n++;
				try {
					liquidGone += clearDevourerLiquid(server, vid);
				} catch (Exception ignored) {
				}
				try {
					dropsGone += clearVictimDrops(server, vid);
				} catch (Exception ignored) {
				}
			} catch (Exception ignored) {
			}
		}
		if (liquidGone > 0 && killerId != null) {
			synchronized (LAST_LIQUID_CLEARED) {
				LAST_LIQUID_CLEARED.put(killerId,
						LAST_LIQUID_CLEARED.getOrDefault(killerId, 0) + liquidGone);
			}
		}
		if (dropsGone > 0 && killerId != null) {
			synchronized (LAST_DROPS_CLEARED) {
				LAST_DROPS_CLEARED.put(killerId,
						LAST_DROPS_CLEARED.getOrDefault(killerId, 0) + dropsGone);
			}
		}
		return n;
	}

	public static void noteDeath(ServerLevel level, LivingEntity victim, UUID killerId) {
		try {
			if (victim instanceof Player) {
				return;
			}
			CompoundTag tag;
			try {
				tag = TameUtil.savePet(victim);
			} catch (Exception e) {
				return;
			}
			if (tag == null) {
				return;
			}
			UUID vid;
			Vec3 pos;
			net.minecraft.resources.ResourceKey<net.minecraft.world.level.Level> dim;
			try {
				vid = victim.getUUID();
				pos = victim.position();
				dim = level.dimension();
			} catch (Exception e) {
				return;
			}
			synchronized (DEATH_SNAPS) {
				DEATH_SNAPS.put(vid, new DeathSnap(tag, dim, pos, killerId, System.currentTimeMillis()));
				while (DEATH_SNAPS.size() > 200) {
					try {
						DEATH_SNAPS.remove(DEATH_SNAPS.keySet().iterator().next());
					} catch (Exception ex) {
						break;
					}
				}
			}
		} catch (Exception ignored) {
		}
	}

	public static String erase(ServerPlayer player, ItemStack cap, int index) {
		List<CompoundTag> voids = VoidData.getVoids(cap);
		if (index < 0 || index >= voids.size()) {
			return "잘못된 대상";
		}
		CompoundTag tag = voids.get(index);
		UUID targetId = tagUuid(tag);
		String typeName = "?";
		try {
			String tid = com.partnercap.data.PartnerData.getTypeId(tag);
			typeName = tid.contains(":") ? tid.substring(tid.indexOf(':') + 1) : tid;
		} catch (Exception ignored) {
		}
		voids.remove(index);
		VoidData.setVoids(cap, voids);
		PartnerCapsuleItem.syncCapsule(player);

		float healed = 0f;
		int removedKids = 0;
		MinecraftServer server = null;
		try {
			server = player.level().getServer();
		} catch (Exception ignored) {
		}
		if (server != null && targetId != null) {
			long now = System.currentTimeMillis();
			List<Dmg> hits = new ArrayList<>();
			synchronized (DMG_LOG) {
				for (Dmg d : DMG_LOG) {
					if (targetId.equals(d.attacker()) && now - d.time() < REWIND_WINDOW_MS) {
						hits.add(d);
					}
				}
			}
			java.util.Map<UUID, Float> perVictim = new java.util.HashMap<>();
			for (Dmg d : hits) {
				perVictim.put(d.victim(), perVictim.getOrDefault(d.victim(), 0f) + d.amount());
			}
			for (var e : perVictim.entrySet()) {
				try {
					Entity v = findEntity(server, e.getKey());
					if (v instanceof LivingEntity living && living.isAlive()) {
						float max;
						float oldH;
						try {
							max = living.getMaxHealth();
							oldH = living.getHealth();
						} catch (Exception ex) {
							continue;
						}
						float nhp = Math.min(max, oldH + e.getValue());
						try {
							living.setHealth(nhp);
							if (nhp > oldH + 0.01f) {
								healed += (nhp - oldH);
								clearHarmful(living);
							}
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			}
			// 자식 연쇄 소멸
			Set<UUID> queue = new LinkedHashSet<>();
			queue.add(targetId);
			Set<UUID> done = new HashSet<>();
			while (!queue.isEmpty()) {
				UUID cur;
				try {
					cur = queue.iterator().next();
					queue.remove(cur);
				} catch (Exception e) {
					break;
				}
				if (!done.add(cur)) {
					continue;
				}
				List<UUID> kids = new ArrayList<>();
				synchronized (BABY_PARENTS) {
					for (var en : BABY_PARENTS.entrySet()) {
						try {
							if (en.getValue().contains(cur)) {
								kids.add(en.getKey());
							}
						} catch (Exception ignored) {
						}
					}
					for (UUID k : kids) {
						BABY_PARENTS.remove(k);
					}
				}
				synchronized (KNOWN_BABIES) {
					KNOWN_BABIES.removeAll(kids);
				}
				for (UUID kid : kids) {
					try {
						Entity ke = findEntity(server, kid);
						if (ke != null) {
							ke.discard();
							removedKids++;
						}
					} catch (Exception ignored) {
					}
					queue.add(kid);
				}
			}
		}
		String msg = typeName + " 존재 삭제 완료";
		if (healed > 0.5f) {
			msg += " (체력 " + String.format("%.1f", healed) + " 회복+해로운 효과 제거)";
		}
		if (removedKids > 0) {
			msg += " (여파 " + removedKids + "마리 소멸)";
		}
		if (server != null && targetId != null) {
			int revived = 0;
			try {
				revived = reviveVictims(server, targetId);
			} catch (Exception ignored) {
			}
			if (revived > 0) {
				msg += " (" + revived + "마리 부활)";
			}
			int liquidGone = 0;
			try {
				liquidGone = takeLiquidCleared(targetId);
			} catch (Exception ignored) {
			}
			if (liquidGone > 0) {
				msg += " (검은 액체 " + liquidGone + "칸 회수)";
			}
			int dropsGone = 0;
			try {
				dropsGone = takeDropsCleared(targetId);
			} catch (Exception ignored) {
			}
			if (dropsGone > 0) {
				msg += " (드롭 " + dropsGone + "개 회수)";
			}
			int unpetrified = 0;
			try {
				unpetrified = com.partnercap.medusa.Petrify.releaseByErased(server, targetId);
			} catch (Exception ignored) {
			}
			if (unpetrified > 0) {
				msg += " (" + unpetrified + "마리 석화 해제)";
			}
		}
		int gearBack = 0;
		try {
			String equip = tag.getString("VoidEquip").orElse("");
			if (!equip.isEmpty()) {
				for (String ent : equip.split(";")) {
					int ci = ent.lastIndexOf(',');
					if (ci < 0) {
						continue;
					}
					String iid = ent.substring(0, ci);
					int cnt = 1;
					try {
						cnt = Math.max(1, Math.min(64, Integer.parseInt(ent.substring(ci + 1))));
					} catch (Exception ignored) {
					}
					Item item = itemById(iid);
					if (item == null) {
						continue;
					}
					ItemStack back = new ItemStack(item);
					try {
						back.setCount(cnt);
					} catch (Exception ignored) {
					}
					try {
						TameUtil.giveToPlayer(player, back);
						gearBack++;
					} catch (Exception ignored) {
					}
				}
			}
		} catch (Exception ignored) {
		}
		try {
			CompoundTag carried = tag.getCompound("carried").orElse(null);
			if (carried != null && !carried.isEmpty()) {
				String bname = carried.getString("Name").orElse("");
				if (!bname.isEmpty()) {
					Object bobj = null;
					try {
						int ci2 = bname.indexOf(':');
						if (ci2 >= 0) {
							bobj = net.minecraft.core.registries.BuiltInRegistries.BLOCK
									.getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath(
											bname.substring(0, ci2), bname.substring(ci2 + 1)))
									.orElse(null);
						}
					} catch (Exception ignored) {
					}
					if (bobj instanceof net.minecraft.world.level.block.Block block) {
						try {
							Item bi = block.asItem();
							if (bi != null && bi != net.minecraft.world.item.Items.AIR) {
								TameUtil.giveToPlayer(player, new ItemStack(bi));
								gearBack++;
							}
						} catch (Exception ignored) {
						}
					}
				}
			}
		} catch (Exception ignored) {
		}
		if (gearBack > 0) {
			msg += " (휴대품 " + gearBack + "종 반환)";
		}
		if (targetId != null) {
			int trades = 0;
			try {
				if (server != null) {
					trades = revertTradesFor(server, targetId);
				} else {
					trades = revertTrades(player, targetId);
				}
			} catch (Exception ignored) {
			}
			if (trades > 0) {
				msg += " (거래 " + trades + "건 무효)";
			}
		}
		if (server != null) {
			try {
				cureNearbyZombies(server, player, typeName);
			} catch (Exception ignored) {
			}
		}
		try {
			player.sendSystemMessage(Component.literal(msg));
		} catch (Exception ignored) {
		}
		return msg;
	}

	/** 저장 태그에서 풀어주기 (일반 엔티티로 소환) */
	public static boolean release(ServerPlayer player, ItemStack cap, int index) {
		if (!(player.level() instanceof ServerLevel serverLevel)) {
			return false;
		}
		CompoundTag tag = VoidData.removeVoid(cap, index);
		if (tag == null) {
			return false;
		}
		try {
			net.minecraft.world.entity.EntitySpawnRequest request = new net.minecraft.world.entity.EntitySpawnRequest(
					net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED, true);
			Entity created = net.minecraft.world.entity.EntityType.loadEntityRecursive(tag, serverLevel, request,
					net.minecraft.world.entity.EntityProcessor.NOP);
			if (created == null) {
				VoidData.addVoid(cap, tag);
				return false;
			}
			created.setPos(player.getX() + 1, player.getY() + 1, player.getZ() + 1);
			serverLevel.addFreshEntityWithPassengers(created);
			PartnerCapsuleItem.syncCapsule(player);
			try {
				player.sendSystemMessage(Component.literal("풀어줬다!"));
			} catch (Exception ignored) {
			}
			return true;
		} catch (Exception e) {
			try {
				VoidData.addVoid(cap, tag);
			} catch (Exception ignored) {
			}
			return false;
		}
	}

	/** 캡슐에 빨아들이기 (클릭한 1마리만) */
	public static boolean capture(ServerPlayer player, ItemStack cap, LivingEntity target) {
		if (target == null || !target.isAlive() || target.isRemoved()) {
			return false;
		}
		if (target instanceof com.partnercap.medusa.StatueEntity) {
			return false;
		}
		if (com.partnercap.medusa.Petrify.isPetrified(target)) {
			return false;
		}
		if (target instanceof Player) {
			return false;
		}
		try {
			// 빨아들이는 모션: 엔티티 → 플레이어 검은 포털 줄기
			ServerLevel level = (ServerLevel) player.level();
			net.minecraft.world.phys.Vec3 from = target.position().add(0, 1.0, 0);
			net.minecraft.world.phys.Vec3 to = player.position().add(0, 1.2, 0);
			for (int i = 0; i <= 14; i++) {
				net.minecraft.world.phys.Vec3 p = from.lerp(to, i / 14.0);
				level.sendParticles(net.minecraft.core.particles.ParticleTypes.PORTAL,
						p.x, p.y, p.z, 3, 0.05, 0.05, 0.05, 0.0);
			}
			level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
					to.x, to.y, to.z, 8, 0.25, 0.4, 0.25, 0.02);
			level.playSound(null, player.blockPosition(),
					net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
					net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 0.6f);
		} catch (Exception ignored) {
		}
		CompoundTag tag;
		try {
			tag = com.partnercap.util.TameUtil.savePet(target);
		} catch (Exception e) {
			return false;
		}
		// 착용/휴대 장비 기록 (삭제 시 반환용)
		try {
			List<String> gear = new ArrayList<>();
			for (EquipmentSlot slot : EquipmentSlot.values()) {
				ItemStack s;
				try {
					s = target.getItemBySlot(slot);
				} catch (Exception e) {
					continue;
				}
				if (s == null || s.isEmpty()) {
					continue;
				}
				String gid;
				int cnt = 1;
				try {
					gid = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(s.getItem()).toString();
					cnt = Math.max(1, Math.min(64, s.getCount()));
				} catch (Exception e) {
					continue;
				}
				gear.add(gid + "," + cnt);
			}
			if (!gear.isEmpty()) {
				tag.putString("VoidEquip", String.join(";", gear));
			}
		} catch (Exception ignored) {
		}
		try {
			target.discard();
		} catch (Exception ignored) {
		}
		VoidData.addVoid(cap, tag);
		PartnerCapsuleItem.syncCapsule(player);
		try {
			player.sendSystemMessage(Component.literal("공허에 가두었다! (C키로 확인)"));
		} catch (Exception ignored) {
		}
		return true;
	}
}
