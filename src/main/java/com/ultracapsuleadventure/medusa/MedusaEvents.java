package com.ultracapsuleadventure.medusa;

import com.ultracapsuleadventure.entity.ModEntityTypes;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 메두사 자연 생성(비 + 주변 + 40%) + 머리 드롭 + 석상 보호.
 */
public class MedusaEvents {

	public static void register() {
		// 석상: 어떤 피해도 받지 않음 + 죽지 않음 (곡괭이 채굴만 허용 → 조약돌 2개)
		try {
			ServerLivingEntityEvents.ALLOW_DAMAGE.register((LivingEntity victim, DamageSource source, float amount) -> {
				try {
					// 석상(구 플레이어 석상 포함): 곡괭이 좌클릭으로도 캐진다. 그 외 피해는 전부 차단.
					// isInvulnerable=false이므로 공격이 여기까지 도달한다.
					if (victim instanceof StatueEntity) {
						Entity attacker = null;
						try {
							attacker = source.getEntity();
						} catch (Exception ignored) {
						}
						if (attacker instanceof ServerPlayer sp) {
							try {
								Petrify.tryMinePetrified(sp, victim);
							} catch (Exception ignored) {
							}
						}
						return false;
					}
					if (Petrify.isPetrified(victim)) {
						Entity attacker = null;
						try {
							attacker = source.getEntity();
						} catch (Exception ignored) {
						}
						if (attacker instanceof ServerPlayer sp) {
							try {
								Petrify.tryMinePetrified(sp, victim);
							} catch (Exception ignored) {
							}
						}
						return false;
					}
				} catch (Exception ignored) {
				}
				return true;
			});
		} catch (Exception ignored) {
		}
		try {
			ServerLivingEntityEvents.ALLOW_DEATH.register((LivingEntity entity, DamageSource source, float amount) -> {
				try {
					if (entity instanceof StatueEntity) {
						return false;
					}
					if (Petrify.isPetrified(entity)) {
						return false;
					}
				} catch (Exception ignored) {
				}
				return true;
			});
		} catch (Exception ignored) {
		}
		// 석화된 대상을 노리는 몹의 타겟 해제 (공격 대상에서 완전히 제외)
		// + 빨간 피격 플래시 원천 차단 (hurtTime/deathTime 지속 0)
		try {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				try {
					if (server.getTickCount() % 20 != 0) {
						return;
					}
					for (ServerLevel level : server.getAllLevels()) {
						try {
							for (Entity e : level.getAllEntities()) {
								try {
									if (e instanceof LivingEntity living
											&& Petrify.isPetrified(living)) {
										// 석화된 메카에 탑승자가 남아 있으면 강제 하차 (NBT 로드 등 예외 경로 안전망)
										try {
											if (living.isVehicle()) {
												living.ejectPassengers();
											}
										} catch (Exception ignored) {
										}
										try {
											for (Entity passenger : new java.util.ArrayList<>(
													living.getPassengers())) {
												try {
													passenger.stopRiding();
												} catch (Exception ignored) {
												}
											}
										} catch (Exception ignored) {
										}
										try {
											living.hurtTime = 0;
											living.deathTime = 0;
										} catch (Exception ignored) {
										}
										try {
											living.clearFire();
										} catch (Exception ignored) {
										}
										// 지속 피해(독/위더) 원천 제거: 바닐라 경로 우회 피해 차단
										try {
											living.removeEffect(net.minecraft.world.effect.MobEffects.POISON);
										} catch (Exception ignored) {
										}
										try {
											living.removeEffect(net.minecraft.world.effect.MobEffects.WITHER);
										} catch (Exception ignored) {
										}
									}
								} catch (Exception ignored) {
								}
								if (!(e instanceof net.minecraft.world.entity.Mob mob)) {
									continue;
								}
								try {
									LivingEntity t = mob.getTarget();
									if (t != null && t.isAlive() && Petrify.isPetrified(t)) {
										mob.setTarget(null);
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
		// 메두사 사망 → 머리 드롭 (무조건 1개)
		try {
			ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity dead, DamageSource source) -> {
				try {
					if (!MedusaEntity.isMedusa(dead)) {
						return;
					}
					if (!(dead.level() instanceof ServerLevel level)) {
						return;
					}
					ItemStack head = ItemStack.EMPTY;
					try {
						head = new ItemStack(com.ultracapsuleadventure.item.ModItems.MEDUSA_HEAD);
					} catch (Exception e) {
						return;
					}
					if (head.isEmpty()) {
						return;
					}
					try {
						net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
								level, dead.getX(), dead.getY() + 0.8, dead.getZ(), head);
						level.addFreshEntity(drop);
					} catch (Exception ignored) {
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
		// 비 오는 날 플레이어 주변 40% 자연 생성
		try {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				try {
					if (server.getTickCount() % 600 != 0) {
						return;
					}
					tickSpawn(server);
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
	}

	private static void tickSpawn(MinecraftServer server) {
		try {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				try {
					if (player.isSpectator()) {
						continue;
					}
					try {
						if (player.isCreative()) {
							continue;
						}
					} catch (Exception ignored) {
					}
					if (!(player.level() instanceof ServerLevel level)) {
						continue;
					}
					boolean raining = false;
					try {
						raining = level.isRaining();
					} catch (Exception ignored) {
					}
					if (!raining) {
						continue;
					}
					try {
						if (level.getDifficulty() == Difficulty.PEACEFUL) {
							continue;
						}
					} catch (Exception ignored) {
					}
					float roll = 0f;
					try {
						roll = level.getRandom().nextFloat();
					} catch (Exception e) {
						continue;
					}
					if (roll > 0.4f) {
						continue;
					}
					// 주변 메두사 2마리 상한
					int near = 0;
					try {
						for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(64.0),
								x -> MedusaEntity.isMedusa(x))) {
							near++;
						}
					} catch (Exception ignored) {
					}
					if (near >= 2) {
						continue;
					}
					double angle = 0;
					double dist = 32;
					try {
						angle = level.getRandom().nextDouble() * Math.PI * 2.0;
						dist = 24.0 + level.getRandom().nextDouble() * 16.0;
					} catch (Exception ignored) {
					}
					int x = (int) Math.floor(player.getX() + Math.cos(angle) * dist);
					int z = (int) Math.floor(player.getZ() + Math.sin(angle) * dist);
					int y = 0;
					try {
						y = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z) + 1;
					} catch (Exception e) {
						continue;
					}
					MedusaEntity medusa = null;
					try {
						medusa = new MedusaEntity(ModEntityTypes.MEDUSA, level);
						medusa.setPos(x + 0.5, y, z + 0.5);
						level.addFreshEntityWithPassengers(medusa);
					} catch (Exception ignored) {
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}
}


