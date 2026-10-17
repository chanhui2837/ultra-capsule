package com.ultracapsuleadventure.flame;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

/**
 * 화염몽 자연 스폰(사막, 소량) + 사망 드롭(심장+초월의 막대) + 수호 전투(화염+지팡이).
 */
public class FlameMongEvents {

	public static void register() {
		// 사망 → 심장 1개 + 초월의 막대 1개 확정 드롭
		try {
			ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity dead, DamageSource source) -> {
				try {
					if (!FlameMongEntity.isFlameMong(dead)) {
						return;
					}
					if (!(dead.level() instanceof ServerLevel level)) {
						return;
					}
					try {
						ItemStack heart = new ItemStack(ModFlame.FLAME_HEART);
						if (!heart.isEmpty()) {
							level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
									level, dead.getX(), dead.getY() + 0.8, dead.getZ(), heart));
						}
					} catch (Exception ignored) {
					}
					try {
						ItemStack wand = new ItemStack(ModFlame.TRANSCEND_WAND);
						if (!wand.isEmpty()) {
							level.addFreshEntity(new net.minecraft.world.entity.item.ItemEntity(
									level, dead.getX(), dead.getY() + 0.8, dead.getZ(), wand));
						}
					} catch (Exception ignored) {
					}
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
		// 수호 전투 틱: 집 근처 적이 있으면 화염+지팡이 (1초마다)
		try {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				try {
					if (server.getTickCount() % 20 != 0) {
						return;
					}
					tickCombat(server);
				} catch (Exception ignored) {
				}
			});
		} catch (Exception ignored) {
		}
		// 사막 자연 스폰 (30초마다 25%, 64m 내 3마리 상한)
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

	private static void tickCombat(MinecraftServer server) {
		try {
			for (ServerLevel level : server.getAllLevels()) {
				try {
					for (Entity e : level.getAllEntities()) {
						try {
							if (!(e instanceof FlameMongEntity mong) || !mong.isAlive()) {
								continue;
							}
							try {
								if (mong.isSleeping()) {
									continue;
								}
							} catch (Exception ignored) {
							}
							LivingEntity target = null;
							try {
								target = mong.getTarget();
							} catch (Exception ignored) {
							}
							if (target == null || !target.isAlive()) {
								continue;
							}
							// 지팡이 먼저 (쿨 있을 땐 스킵)
							try {
								mong.waveWand(target);
							} catch (Exception ignored) {
							}
							// 화염 브레스 (3초마다). 소울은 주인/동료에게 쏘지 않음
							try {
								if (mong.tickCount % 60 == 0) {
									boolean skip = false;
									try {
										java.util.UUID owner = mong.soulOwnerId();
										if (owner != null) {
											if (target.getUUID().equals(owner)) {
												skip = true;
											} else {
												for (String tag : target.entityTags()) {
													if (tag.equals(com.ultracapsuleadventure.soul.SoulAlly.TAG_PREFIX + owner.toString())) {
														skip = true;
														break;
													}
												}
											}
										}
									} catch (Exception ignored) {
									}
									if (!skip) {
										Vec3 dir = new Vec3(target.getX() - mong.getX(),
												(target.getY() + 1) - mong.getEyeY(), target.getZ() - mong.getZ());
										mong.breatheFire(dir);
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
	}

	private static void tickSpawn(MinecraftServer server) {
		try {
			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				try {
					if (player.isSpectator() || player.isCreative()) {
						continue;
					}
					if (!(player.level() instanceof ServerLevel level)) {
						continue;
					}
					try {
						if (!level.dimension().equals(net.minecraft.world.level.Level.OVERWORLD)) {
							continue;
						}
					} catch (Exception ignored) {
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
					if (roll > 0.25f) {
						continue;
					}
					int near = 0;
					try {
						for (Entity e : level.getEntities(player, player.getBoundingBox().inflate(64.0),
								FlameMongEntity::isFlameMong)) {
							near++;
						}
					} catch (Exception ignored) {
					}
					if (near >= 3) {
						continue;
					}
					double angle = 0;
					double dist = 28;
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
					BlockPos at = new BlockPos(x, y, z);
					// 사막 바이옴에서만
					try {
						if (!level.getBiome(at).is(net.minecraft.world.level.biome.Biomes.DESERT)) {
							continue;
						}
					} catch (Exception e) {
						continue;
					}
					int n = 1;
					try {
						if (level.getRandom().nextFloat() < 0.3f) {
							n = 2;
						}
					} catch (Exception ignored) {
					}
					for (int i = 0; i < n; i++) {
						try {
							FlameMongEntity mong = new FlameMongEntity(ModFlame.FLAME_MONG, level);
							mong.setPos(x + 0.5 + i, y, z + 0.5 - i);
							mong.findHomeLava();
							level.addFreshEntityWithPassengers(mong);
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}
}


