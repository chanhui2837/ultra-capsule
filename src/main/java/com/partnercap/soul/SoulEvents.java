package com.partnercap.soul;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import com.partnercap.item.FlameSwordItem;
import com.partnercap.item.ModItems;
import com.partnercap.item.PartnerCapsuleItem;
import com.partnercap.item.SoulCapsuleItem;
import com.partnercap.util.TameUtil;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.cubemob.AbstractCubeMob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 불길의 검 처치 시 영혼 흡수 (소울 캡슐에 저장 + 흡수 모션).
 */
public class SoulEvents {

	private record Cleanup(ServerLevel level, Vec3 pos, Set<UUID> keep, int tick) {
	}

	private static final Map<UUID, Set<UUID>> PRE_DROP = new HashMap<>();
	private static final List<Cleanup> PENDING = new ArrayList<>();

	private record SplitNote(ServerLevel level, Vec3 pos, String typeId, UUID owner, int untilTick) {
	}

	private static final List<SplitNote> SPLITS = new ArrayList<>();

	private static final Map<UUID, net.minecraft.resources.ResourceKey<Level>> LAST_DIM = new HashMap<>();

	public static void register() {
		// 리스폰: 사망 때 회수된 소울 자동 소환
		try {
			ServerPlayerEvents.AFTER_RESPAWN.register((ServerPlayer oldP, ServerPlayer newP, boolean alive) -> {
				try {
					for (int i = 0; i < newP.getInventory().getContainerSize(); i++) {
						ItemStack cap;
						try {
							cap = newP.getInventory().getItem(i);
						} catch (Exception e) {
							continue;
						}
						if (!(cap.getItem() instanceof SoulCapsuleItem)) {
							continue;
						}
						boolean auto = false;
						try {
							auto = SoulData.getAutoOut(cap);
						} catch (Exception ignored) {
						}
						if (!auto) {
							continue;
						}
						try {
							SoulData.setAutoOut(cap, false);
						} catch (Exception ignored) {
						}
						try {
							SoulCapsuleItem.summonAll(newP.level(), newP, cap);
						} catch (Exception ignored) {
						}
					}
					PartnerCapsuleItem.syncCapsule(newP);
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
		// 유령(아군 소울)은 햇빛에 타지 않는다 (5틱마다 소화 — 화염 피해틱보다 빠름)
		try {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				try {
					if (server.getTickCount() % 5 != 0) {
						return;
					}
					for (ServerLevel level : server.getAllLevels()) {
						try {
							for (Entity e : level.getAllEntities()) {
								try {
									if (!(e instanceof LivingEntity living)) {
										continue;
									}
									UUID owner;
									try {
										owner = SoulAlly.soulOwnerOf(living);
									} catch (Exception ex) {
										continue;
									}
									if (owner == null) {
										continue;
									}
									// 햇빛 소화: 낮에 하늘 아래서 타고 있으면 끈다
									// (isSunBurnTick은 private이라 동등 조건 직접 판정)
									try {
										if (living.isOnFire()
												&& !living.isInWaterOrRain()
												&& !living.isInLava()) {
											boolean bright = false;
											try {
												bright = level.isBrightOutside();
											} catch (Exception ignored) {
											}
											boolean sky = false;
											try {
												sky = level.canSeeSky(living.blockPosition());
											} catch (Exception ignored) {
											}
											if (bright && sky) {
												try {
													living.clearFire();
												} catch (Exception ignored) {
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
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
		// 재시작 등으로 골이 초기화된 소울을 주기적으로 재적용 + 사망 드롭 정리 실행
		try {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				try {
					SoulAlly.tickMaintain(server);
				} catch (Exception ignored) {
				}
				// 분열 새끼 소울 편입
				try {
					if (!SPLITS.isEmpty()) {
						int now = server.getTickCount();
						Iterator<SplitNote> it = SPLITS.iterator();
						while (it.hasNext()) {
							SplitNote n = it.next();
							if (now > n.untilTick()) {
								it.remove();
								continue;
							}
							int done = 0;
							try {
								for (Entity e : n.level().getAllEntities()) {
									if (!(e instanceof LivingEntity)) {
										continue;
									}
									String tid;
									try {
										tid = BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString();
									} catch (Exception ex) {
										continue;
									}
									if (!tid.equals(n.typeId())) {
										continue;
									}
									if (SoulAlly.soulOwnerOf(e) != null) {
										continue;
									}
									boolean small = true;
									try {
										if (e instanceof AbstractCubeMob cube) {
											small = cube.getSize() <= 1;
										}
									} catch (Exception ignored) {
									}
									if (!small) {
										continue;
									}
									double dx, dy, dz;
									try {
										dx = e.getX() - n.pos().x;
										dy = e.getY() - n.pos().y;
										dz = e.getZ() - n.pos().z;
									} catch (Exception ex) {
										continue;
									}
									if (dx * dx + dy * dy + dz * dz > 64.0) {
										continue;
									}
									try {
										SoulAlly.makeAlly(n.level(), e, n.owner());
										done++;
									} catch (Exception ignored) {
									}
								}
							} catch (Exception ignored) {
							}
							if (done > 0) {
								it.remove();
							}
						}
					}
				} catch (Exception ignored) {
				}
				// 차원 이동 시 소울 자동 추적
				try {
					if (server.getTickCount() % 20 == 0) {
						for (ServerPlayer p : server.getPlayerList().getPlayers()) {
							try {
								net.minecraft.resources.ResourceKey<Level> dim = p.level().dimension();
								UUID id = p.getUUID();
								net.minecraft.resources.ResourceKey<Level> last = LAST_DIM.get(id);
								if (last != null && !last.equals(dim)) {
									int n = SoulAlly.recallSoulsTo(p);
									if (n > 0) {
										try {
											p.sendSystemMessage(Component.literal("소울 " + n + "기가 차원을 넘어 따라왔다!"));
										} catch (Exception ignored) {
										}
									}
								}
								LAST_DIM.put(id, dim);
							} catch (Exception ignored) {
							}
						}
					}
				} catch (Exception ignored) {
				}
				try {
					if (!PENDING.isEmpty()) {
						int now = server.getTickCount();
						Iterator<Cleanup> it = PENDING.iterator();
						while (it.hasNext()) {
							Cleanup c = it.next();
							if (now < c.tick()) {
								continue;
							}
							it.remove();
							try {
								AABB box = new AABB(c.pos().x - 5, c.pos().y - 5, c.pos().z - 5,
										c.pos().x + 5, c.pos().y + 5, c.pos().z + 5);
								for (Entity e : c.level().getEntities((Entity) null, box,
										x -> (x instanceof ItemEntity || x instanceof ExperienceOrb)
												&& !c.keep().contains(x.getUUID()))) {
									try {
										e.discard();
									} catch (Exception ignored) {
									}
								}
							} catch (Exception ignored) {
							}
						}
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
		// 치명타 직전 주변 드롭물 스냅샷 (소울 사망 시 새로 생긴 것만 제거)
		try {
			ServerLivingEntityEvents.ALLOW_DEATH.register((LivingEntity entity, DamageSource source, float amount) -> {
				try {
					if (SoulAlly.soulOwnerOf(entity) != null && entity.level() instanceof ServerLevel level) {
						Set<UUID> keep = new HashSet<>();
						try {
							for (Entity e : level.getEntities(entity, entity.getBoundingBox().inflate(5.0),
									x -> x instanceof ItemEntity || x instanceof ExperienceOrb)) {
								keep.add(e.getUUID());
							}
						} catch (Exception ignored) {
						}
						PRE_DROP.put(entity.getUUID(), keep);
					}
				} catch (Exception ignored) {
				}
				return true;
			});
		} catch (Exception ignored) {
		}
		// 아군 오폭 취소: 소울이 낸 피해는 주인/동료 소울에게 안 들어감
		try {
			ServerLivingEntityEvents.ALLOW_DAMAGE.register((LivingEntity victim, DamageSource source, float amount) -> {
				try {
					boolean victimSide = false;
					UUID victimOwner = null;
					try {
						if (victim instanceof ServerPlayer) {
							victimSide = true;
						} else {
							victimOwner = SoulAlly.soulOwnerOf(victim);
							victimSide = victimOwner != null;
						}
					} catch (Exception ignored) {
					}
					if (!victimSide) {
						return true;
					}
					UUID so = null;
					try {
						Entity direct = null;
						try {
							direct = source.getDirectEntity();
						} catch (Exception ignored) {
						}
						if (direct != null) {
							so = SoulAlly.soulOwnerOf(direct);
						}
						if (so == null) {
							Entity causing = null;
							try {
								causing = source.getEntity();
							} catch (Exception ignored) {
							}
							if (causing != null) {
								so = SoulAlly.soulOwnerOf(causing);
							}
						}
					} catch (Exception ignored) {
					}
					if (so == null) {
						return true;
					}
					if (victim instanceof Player vp) {
						return !vp.getUUID().equals(so);
					}
					return victimOwner == null || !victimOwner.equals(so);
				} catch (Exception e) {
					return true;
				}
			});
		} catch (Exception ignored) {
		}
		ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity dead, DamageSource source) -> {
			try {
				if (!(dead.level() instanceof ServerLevel level)) {
					return;
				}
				// 플레이어 사망: 따라오던 소울 전원 캡슐 회수 (리스폰 때 자동 소환)
				if (dead instanceof ServerPlayer dsp) {
					try {
						List<LivingEntity> souls = SoulAlly.findOwnedSouls(dsp, 512.0);
						if (!souls.isEmpty()) {
							ItemStack cap = findSoulCapsule(dsp);
							boolean fresh = false;
							if (cap.isEmpty()) {
								cap = new ItemStack(ModItems.SOUL_CAPSULE);
								fresh = true;
							}
							int n = 0;
							for (LivingEntity s : souls) {
								try {
									String tid = BuiltInRegistries.ENTITY_TYPE.getKey(s.getType()).toString();
									SoulData.addSoul(cap, SoulData.captureFrom(s, tid));
									s.discard();
									n++;
								} catch (Exception ignored) {
								}
							}
							if (n > 0) {
								SoulData.setAutoOut(cap, true);
								if (fresh) {
									TameUtil.giveToPlayer(dsp, cap);
								} else {
									PartnerCapsuleItem.syncCapsule(dsp);
								}
								try {
									dsp.sendSystemMessage(Component.literal("사망: 소울 " + n + "기를 캡슐에 회수함"));
								} catch (Exception ignored) {
								}
							}
						}
					} catch (Exception ignored) {
					}
					// 사망: 몸속에 저장했던 영혼도 캡슐로 옮긴다 (소실 방지)
					try {
						java.util.List<CompoundTag> body = SoulBody.takeAll(dsp);
						if (!body.isEmpty()) {
							ItemStack cap = findSoulCapsule(dsp);
							boolean fresh = false;
							if (cap.isEmpty()) {
								cap = new ItemStack(ModItems.SOUL_CAPSULE);
								fresh = true;
							}
							for (CompoundTag s : body) {
								try {
									SoulData.addSoul(cap, s);
								} catch (Exception ignored) {
								}
							}
							if (fresh) {
								TameUtil.giveToPlayer(dsp, cap);
							} else {
								PartnerCapsuleItem.syncCapsule(dsp);
							}
							try {
								dsp.sendSystemMessage(Component.literal("사망: 몸속 영혼 " + body.size() + "기를 캡슐에 옮겼다"));
							} catch (Exception ignored) {
							}
						}
					} catch (Exception ignored) {
					}
				}
				// 아군 소울 사망: 전리품/경험치 없이 조용히 소멸
				UUID wasSoul;
				try {
					wasSoul = SoulAlly.soulOwnerOf(dead);
				} catch (Exception e) {
					wasSoul = null;
				}
				if (wasSoul != null) {
					Set<UUID> keep = PRE_DROP.remove(dead.getUUID());
					if (keep == null) {
						keep = Set.of();
					}
					PENDING.add(new Cleanup(level, dead.position(), keep, level.getServer().getTickCount() + 5));
					// 분열몹(마그마/슬라임) 소울 사망: 갈라진 새끼도 아군으로 편입
					try {
						String deadType = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
						if (deadType.endsWith("magma_cube") || deadType.endsWith(":slime")) {
							SPLITS.add(new SplitNote(level, dead.position(), deadType, wasSoul,
									level.getServer().getTickCount() + 200));
						}
					} catch (Exception ignored) {
					}
					return;
				}
				// 먹보 도깨비 사망: 검은 액체를 흩뿌림 (공허 캡슐 재료)
				try {
					if (com.partnercap.entity.ModEntityTypes.isDevourer(dead)) {
						net.minecraft.world.level.block.state.BlockState liquid = com.partnercap.fluid.ModBlocks.VOID_LIQUID
								.defaultBlockState();
						int placed = 0;
						for (int i = 0; i < 24 && placed < 5; i++) {
							double x = dead.getX() + (level.getRandom().nextDouble() - 0.5) * 5.0;
							double y = dead.getY() + level.getRandom().nextInt(2);
							double z = dead.getZ() + (level.getRandom().nextDouble() - 0.5) * 5.0;
							net.minecraft.core.BlockPos p = net.minecraft.core.BlockPos.containing(x, y, z);
							try {
								if (level.getBlockState(p).isAir()) {
									level.setBlock(p, liquid, 3);
									placed++;
									try {
										com.partnercap.voidcap.VoidErase.noteDevourerLiquid(level, dead.getUUID(), p);
									} catch (Exception ignored) {
									}
								}
							} catch (Exception ignored) {
							}
						}
					}
				} catch (Exception ignored) {
				}
				if (dead instanceof Player) {
					return;
				}
				Entity killerEnt = source.getEntity();
				ServerPlayer killer = null;
				boolean needSword = true;
				if (killerEnt instanceof ServerPlayer sp) {
					killer = sp;
				} else if (killerEnt instanceof LivingEntity soulKiller) {
					// 아군 소울의 킬도 흡수 (무기 무관)
					UUID oid = null;
					try {
						oid = SoulAlly.soulOwnerOf(soulKiller);
					} catch (Exception e) {
						return;
					}
					if (oid == null) {
						return;
					}
					try {
						for (ServerPlayer p : level.players()) {
							if (p.getUUID().equals(oid)) {
								killer = p;
								break;
							}
						}
					} catch (Exception e) {
						return;
					}
					if (killer == null) {
						return;
					}
					needSword = false;
				} else {
					return;
				}
				if (needSword) {
					ItemStack weapon = killer.getMainHandItem();
					if (!(weapon.getItem() instanceof FlameSwordItem)) {
						weapon = killer.getOffhandItem();
						if (!(weapon.getItem() instanceof FlameSwordItem)) {
							return;
						}
					}
				}
				ItemStack cap = findSoulCapsule(killer);
				if (cap.isEmpty()) {
					return;
				}
				String typeId;
				try {
					typeId = BuiltInRegistries.ENTITY_TYPE.getKey(dead.getType()).toString();
				} catch (Exception e) {
					return;
				}
			CompoundTag soul = SoulData.captureFrom(dead, typeId);
			// 인챈트 연동: 소울 캡슐 약탈 Lv당 흡수 +1개 (기본 1개, 최대 4개)
			int soulCount = 1;
			try {
				soulCount = Math.max(1,
						Math.min(4, 1 + com.partnercap.util.EnchantLevels.looting(level.registryAccess(), cap)));
			} catch (Exception ignored) {
			}
			for (int i = 0; i < soulCount; i++) {
				try {
					SoulData.addSoul(cap, soul);
				} catch (Exception ignored) {
				}
			}
			PartnerCapsuleItem.syncCapsule(killer);

				// 영혼 흡수 모션: 시체에서 플레이어로 혼 파티클 줄기 + 흡수 사운드
				Vec3 from = dead.position().add(0, 1.0, 0);
				Vec3 to = killer.position().add(0, 1.2, 0);
				try {
					for (int i = 0; i <= 14; i++) {
						Vec3 p = from.lerp(to, i / 14.0);
						level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL,
								p.x, p.y, p.z, 2, 0.05, 0.05, 0.05, 0.0);
					}
					level.sendParticles(net.minecraft.core.particles.ParticleTypes.SOUL_FIRE_FLAME,
							to.x, to.y, to.z, 10, 0.25, 0.4, 0.25, 0.02);
				} catch (Exception ignored) {
				}
				try {
					level.playSound(null, killer.blockPosition(),
							net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
							net.minecraft.sounds.SoundSource.PLAYERS, 0.7f, 1.3f);
				} catch (Exception ignored) {
				}
			String shortType = typeId.contains(":") ? typeId.substring(typeId.indexOf(':') + 1) : typeId;
			try {
				if (soulCount > 1) {
					killer.sendSystemMessage(Component.literal(shortType + "의 영혼을 " + soulCount + "개 흡수했다! (약탈)"));
				} else {
					killer.sendSystemMessage(Component.literal(shortType + "의 영혼을 흡수했다! (소울 캡슐)"));
				}
			} catch (Exception ignored) {
			}
			} catch (Exception ignored) {
			}
		});
	}

	static ItemStack findSoulCapsule(ServerPlayer player) {
		try {
			for (net.minecraft.world.InteractionHand hand : net.minecraft.world.InteractionHand.values()) {
				ItemStack s = player.getItemInHand(hand);
				if (s.getItem() instanceof SoulCapsuleItem) {
					return s;
				}
			}
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack s = player.getInventory().getItem(i);
				if (s.getItem() instanceof SoulCapsuleItem) {
					return s;
				}
			}
		} catch (Exception ignored) {
		}
		return ItemStack.EMPTY;
	}

	/** 영혼 해제(방출) 시 사용 예정. 현재 소환된 아군은 월드에 남음. */
	@SuppressWarnings("unused")
	public static void releaseSouls(Level level, Player player, ItemStack stack) {
		if (level.isClientSide()) {
			return;
		}
		SoulData.clearSouls(stack);
		if (player instanceof ServerPlayer sp) {
			PartnerCapsuleItem.syncCapsule(sp);
		}
	}
}
