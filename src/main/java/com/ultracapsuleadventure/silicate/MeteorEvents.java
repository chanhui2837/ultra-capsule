package com.ultracapsuleadventure.silicate;

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.ultracapsuleadventure.UltraCapsuleAdventureMod;
import com.ultracapsuleadventure.entity.GloomDevourerEntity;
import com.ultracapsuleadventure.entity.ModEntityTypes;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

/**
 * 운석 이벤트.
 *  - 마인크래프트 하루가 지날 때마다 5% 확률로, 오버월드의 무작위 플레이어 근처(48~88칸)에 운석이 떨어진다.
 *  - 떨어지는 동안 하늘에서 불꽃 궤적이 내려오고, 충돌하면 규산 블록으로 된 큰 운석이 땅에 박힌다.
 *  - 규산 블록을 캘 때 10% 확률로 먹보 도깨비가 블록을 뚫고 나온다. 한 운석에서는 단 1마리만 나온다.
 */
public final class MeteorEvents {

	/** 하루마다 운석이 떨어질 확률 */
	public static final float DAILY_CHANCE = 0.05f;
	/** 규산 블록을 캘 때 먹보 도깨비가 나올 확률 */
	public static final float DEVOURER_CHANCE = 0.10f;
	private static final int FALL_TICKS = 120;

	private static final class Pending {
		final ServerLevel level;
		final BlockPos ground;
		final double sx;
		final double sy;
		final double sz;
		int tick;

		Pending(ServerLevel level, BlockPos ground, double sx, double sy, double sz) {
			this.level = level;
			this.ground = ground;
			this.sx = sx;
			this.sy = sy;
			this.sz = sz;
		}
	}

	private static final List<Pending> PENDING = new ArrayList<>();
	private static MinecraftServer lastServer;
	private static long lastDay = -1;
	private static Method clockMethod;
	private static boolean clockSearched;
	private static boolean clockWarned;

	private MeteorEvents() {
	}

	public static void register() {
		try {
			ServerTickEvents.END_SERVER_TICK.register(MeteorEvents::tick);
		} catch (Exception ignored) {
		}
		try {
			PlayerBlockBreakEvents.AFTER.register((world, player, pos, state, blockEntity) -> {
				try {
					onBreak(world, player, pos, state);
				} catch (Exception e) {
					UltraCapsuleAdventureMod.LOGGER.warn("[UltraCapsuleAdventure] 규산 블록 채굴 처리 오류: {}", e.toString());
				}
			});
		} catch (Exception ignored) {
		}
	}

	// ------------------------------------------------------------------ daily roll

	/** 마크 시간 기준 날짜 번호. 시간 조회 메서드 이름이 버전마다 달라 리플렉션으로 찾는다. */
	private static long dayIndex(ServerLevel level) {
		if (!clockSearched) {
			clockSearched = true;
			for (String n : new String[] {"getDayTime", "getOverworldClockTime", "getDefaultClockTime", "getGameTime"}) {
				try {
					Method m = level.getClass().getMethod(n);
					if (m.getReturnType() == long.class) {
						clockMethod = m;
						break;
					}
				} catch (Exception ignored) {
				}
			}
		}
		if (clockMethod == null) {
			if (!clockWarned) {
				clockWarned = true;
				UltraCapsuleAdventureMod.LOGGER.warn("[UltraCapsuleAdventure] 월드 시간 조회 실패 - 운석 일일 이벤트 비활성");
			}
			return -1;
		}
		try {
			long t = (Long) clockMethod.invoke(level);
			return Math.floorDiv(t, 24000L);
		} catch (Exception e) {
			return -1;
		}
	}

	private static void tick(MinecraftServer server) {
		if (server != lastServer) {
			lastServer = server;
			lastDay = -1;
			PENDING.clear();
		}
		ServerLevel level = server.overworld();
		if (level == null) {
			return;
		}
		long day = dayIndex(level);
		if (day >= 0) {
			if (lastDay < 0) {
				lastDay = day;                     // 서버/월드 시작 직후엔 굴리지 않음
			} else if (day > lastDay) {
				lastDay = day;
				rollDaily(level);
			}
		}
		tickPending();
	}

	private static void rollDaily(ServerLevel level) {
		RandomSource rng = level.getRandom();
		if (rng.nextFloat() >= DAILY_CHANCE) {
			return;
		}
		List<ServerPlayer> candidates = new ArrayList<>();
		for (ServerPlayer p : level.players()) {
			if (p.isAlive() && !p.isSpectator()) {
				candidates.add(p);
			}
		}
		if (candidates.isEmpty()) {
			return;
		}
		schedule(level, candidates.get(rng.nextInt(candidates.size())));
	}

	/** 테스트용/즉시 호출: 해당 플레이어 근처에 운석을 떨어뜨린다. */
	public static void forceMeteor(ServerLevel level, ServerPlayer player) {
		schedule(level, player);
	}

	// ------------------------------------------------------------------ fall & impact

	private static void schedule(ServerLevel level, ServerPlayer player) {
		RandomSource rng = level.getRandom();
		BlockPos ground = null;
		for (int tries = 0; tries < 12 && ground == null; tries++) {
			double ang = rng.nextDouble() * Math.PI * 2.0;
			double dist = 48.0 + rng.nextDouble() * 40.0;
			int x = (int) Math.floor(player.getX() + Math.cos(ang) * dist);
			int z = (int) Math.floor(player.getZ() + Math.sin(ang) * dist);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
			BlockPos p = new BlockPos(x, y, z);
			BlockState st = level.getBlockState(p);
			if (st.getFluidState().isEmpty() && !st.isAir()) {
				ground = p;
			}
		}
		if (ground == null) {
			return;
		}
		double a2 = rng.nextDouble() * Math.PI * 2.0;
		PENDING.add(new Pending(level, ground, ground.getX() + Math.cos(a2) * 70.0, ground.getY() + 170.0,
				ground.getZ() + Math.sin(a2) * 70.0));
		try {
			player.sendSystemMessage(Component.literal("하늘에서 불길한 빛이 내려오고 있다..."));
		} catch (Exception ignored) {
		}
	}

	private static void tickPending() {
		Iterator<Pending> it = PENDING.iterator();
		while (it.hasNext()) {
			Pending p = it.next();
			p.tick++;
			float f = p.tick / (float) FALL_TICKS;
			double x = p.sx + (p.ground.getX() + 0.5 - p.sx) * f;
			double y = p.sy + (p.ground.getY() + 1.0 - p.sy) * f;
			double z = p.sz + (p.ground.getZ() + 0.5 - p.sz) * f;
			try {
				p.level.sendParticles(ParticleTypes.FLAME, x, y, z, 14, 0.9, 0.9, 0.9, 0.03);
				p.level.sendParticles(ParticleTypes.LARGE_SMOKE, x, y, z, 8, 1.2, 1.2, 1.2, 0.02);
			} catch (Exception ignored) {
			}
			if (p.tick >= FALL_TICKS) {
				it.remove();
				try {
					impact(p.level, p.ground);
				} catch (Exception e) {
					UltraCapsuleAdventureMod.LOGGER.warn("[UltraCapsuleAdventure] 운석 생성 오류: {}", e.toString());
				}
			}
		}
	}

	private static void impact(ServerLevel level, BlockPos ground) {
		buildMeteor(level, ground);
		try {
			level.playSound(null, ground, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 6.0f, 0.5f);
			level.playSound(null, ground, SoundEvents.ANVIL_USE, SoundSource.BLOCKS, 5.0f, 0.4f);
			level.sendParticles(ParticleTypes.EXPLOSION_EMITTER, ground.getX() + 0.5, ground.getY() + 4.0,
					ground.getZ() + 0.5, 3, 3.0, 2.0, 3.0, 0.0);
			level.sendParticles(ParticleTypes.LARGE_SMOKE, ground.getX() + 0.5, ground.getY() + 4.0,
					ground.getZ() + 0.5, 60, 4.0, 3.0, 4.0, 0.05);
		} catch (Exception ignored) {
		}
		for (ServerPlayer sp : level.players()) {
			double dx = ground.getX() - sp.getX();
			double dz = ground.getZ() - sp.getZ();
			double dist = Math.sqrt(dx * dx + dz * dz);
			if (dist > 200.0) {
				continue;
			}
			String dir = Math.abs(dx) > Math.abs(dz) ? (dx > 0 ? "동쪽" : "서쪽") : (dz > 0 ? "남쪽" : "북쪽");
			try {
				sp.sendSystemMessage(Component.literal("쿠우웅! " + dir + " 약 " + (int) (Math.round(dist / 10.0) * 10)
						+ "블록 떨어진 곳에 운석이 떨어졌다!"));
			} catch (Exception ignored) {
			}
		}
	}

	private static int noise(int x, int y, int z, int seed) {
		int n = (x * 73856093) ^ (y * 19349663) ^ (z * 83492791) ^ seed;
		n = (n ^ (n >>> 13)) * 1274126177;
		return (n ^ (n >>> 16)) & 0xFF;
	}

	/** 규산 블록으로 된 큰 운석(반지름 5~7)을 땅에 박는다. 주변은 분화구처럼 파이고 그을린다. */
	private static void buildMeteor(ServerLevel level, BlockPos ground) {
		RandomSource rng = level.getRandom();
		BlockState silicate = ModSilicate.SILICATE_BLOCK.defaultBlockState();
		int seed = rng.nextInt();
		double r = 5.0 + rng.nextInt(3);
		double rx = r;
		double ry = r * 0.85;
		double rz = r * (0.9 + 0.2 * rng.nextDouble());
		int cx = ground.getX();
		int cy = ground.getY() + 1;
		int cz = ground.getZ();
		int ext = (int) Math.ceil(r + 5);

		// 1) 분화구: 중심에서 멀어질수록 얕게 파고, 위쪽 식물/나무는 날려버림
		double craterR = r + 4.5;
		for (int dx = -ext; dx <= ext; dx++) {
			for (int dz = -ext; dz <= ext; dz++) {
				double d = Math.sqrt(dx * dx + dz * dz);
				if (d > craterR) {
					continue;
				}
				int x = cx + dx;
				int z = cz + dz;
				int top = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z) - 1;
				for (int y = top + 12; y > top; y--) {
					BlockPos bp = new BlockPos(x, y, z);
					BlockState st = level.getBlockState(bp);
					if (!st.isAir() && (st.canBeReplaced() || st.is(BlockTags.LEAVES) || st.is(BlockTags.LOGS))) {
						level.setBlock(bp, Blocks.AIR.defaultBlockState(), 2);
					}
				}
				int depth = (int) Math.floor(2.2 * (1.0 - d / craterR));
				for (int y = top; y > top - depth; y--) {
					BlockPos bp = new BlockPos(x, y, z);
					if (!level.getBlockState(bp).is(Blocks.BEDROCK)) {
						level.setBlock(bp, Blocks.AIR.defaultBlockState(), 2);
					}
				}
				int floorY = top - depth;
				BlockPos fp = new BlockPos(x, floorY, z);
				BlockState floor = level.getBlockState(fp);
				if (!floor.isAir() && floor.getFluidState().isEmpty() && !floor.is(Blocks.BEDROCK)) {
					int n = noise(x, floorY, z, seed);
					if (n < 90) {
						level.setBlock(fp, Blocks.COARSE_DIRT.defaultBlockState(), 2);
					} else if (n < 150) {
						level.setBlock(fp, Blocks.GRAVEL.defaultBlockState(), 2);
					} else if (n < 180) {
						level.setBlock(fp, Blocks.BLACKSTONE.defaultBlockState(), 2);
					}
				}
			}
		}

		// 2) 운석 본체: 노이즈로 울퉁불퉁한 타원체
		for (int dx = -ext; dx <= ext; dx++) {
			for (int dy = -ext; dy <= ext; dy++) {
				for (int dz = -ext; dz <= ext; dz++) {
					double v = (dx * dx) / (rx * rx) + (dy * dy) / (ry * ry) + (dz * dz) / (rz * rz);
					double nz = (noise(cx + dx, cy + dy, cz + dz, seed) / 255.0 - 0.5) * 0.45;
					if (v + nz < 1.0) {
						BlockPos bp = new BlockPos(cx + dx, cy + dy, cz + dz);
						if (!level.getBlockState(bp).is(Blocks.BEDROCK)) {
							level.setBlock(bp, silicate, 2);
						}
					}
				}
			}
		}

		// 3) 튕겨 나간 파편 몇 개
		for (int i = 0; i < 7; i++) {
			double ang = rng.nextDouble() * Math.PI * 2.0;
			double dist = r + 2.0 + rng.nextDouble() * 4.0;
			int x = cx + (int) Math.round(Math.cos(ang) * dist);
			int z = cz + (int) Math.round(Math.sin(ang) * dist);
			int y = level.getHeight(Heightmap.Types.MOTION_BLOCKING_NO_LEAVES, x, z);
			BlockPos bp = new BlockPos(x, y, z);
			if (level.getBlockState(bp).isAir()) {
				level.setBlock(bp, silicate, 2);
			}
		}
	}

	// ------------------------------------------------------------------ mining -> devourer

	private static void onBreak(Level world, net.minecraft.world.entity.player.Player player, BlockPos pos, BlockState state) {
		if (!(world instanceof ServerLevel level)) {
			return;
		}
		if (!(state.getBlock() instanceof SilicateBlock) || state.getValue(SilicateBlock.SPENT)) {
			return;
		}
		if (level.getRandom().nextFloat() >= DEVOURER_CHANCE) {
			return;
		}
		// 이 운석은 이제 소진: 연결된 모든 규산 블록을 SPENT로 표시 (한 운석당 1마리)
		markMeteorSpent(level, pos);
		spawnEmerging(level, player, pos, state);
	}

	/** 깨진 위치 주변에서 26방향으로 이어진 규산 블록 전체를 SPENT로 바꾼다 (최대 20000개). */
	private static void markMeteorSpent(ServerLevel level, BlockPos start) {
		Set<Long> visited = new HashSet<>();
		ArrayDeque<BlockPos> queue = new ArrayDeque<>();
		visited.add(start.asLong());
		queue.add(start);
		int count = 0;
		while (!queue.isEmpty() && count < 20000) {
			BlockPos p = queue.poll();
			for (int dx = -1; dx <= 1; dx++) {
				for (int dy = -1; dy <= 1; dy++) {
					for (int dz = -1; dz <= 1; dz++) {
						if (dx == 0 && dy == 0 && dz == 0) {
							continue;
						}
						BlockPos n = p.offset(dx, dy, dz);
						if (!visited.add(n.asLong())) {
							continue;
						}
						BlockState st = level.getBlockState(n);
						if (st.getBlock() instanceof SilicateBlock) {
							if (!st.getValue(SilicateBlock.SPENT)) {
								level.setBlock(n, st.setValue(SilicateBlock.SPENT, true), 2);
							}
							queue.add(n);
							count++;
						}
					}
				}
			}
		}
	}

	private static boolean open(ServerLevel level, BlockPos p) {
		return level.getBlockState(p).getCollisionShape(level, p).isEmpty();
	}

	private static void spawnEmerging(ServerLevel level, net.minecraft.world.entity.player.Player player, BlockPos pos,
			BlockState broken) {
		GloomDevourerEntity e = new GloomDevourerEntity(ModEntityTypes.GLOOM_DEVOURER, level);
		// 서 있을 위치: 깨진 칸에서 위로 올라가며 머리 위까지 열린 첫 칸
		int fy = Integer.MIN_VALUE;
		for (int dy = 0; dy <= 10; dy++) {
			BlockPos a = pos.above(dy);
			if (open(level, a) && open(level, a.above())) {
				fy = a.getY();
				break;
			}
		}
		if (fy == Integer.MIN_VALUE) {
			e.setPos(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5);
		} else {
			e.beginEmerge(pos.getX() + 0.5, fy, pos.getZ() + 0.5, 3.0);
		}
		level.addFreshEntityWithPassengers(e);
		try {
			e.setTarget(player);
		} catch (Exception ignored) {
		}
		try {
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, broken), pos.getX() + 0.5,
					pos.getY() + 0.8, pos.getZ() + 0.5, 40, 0.6, 0.4, 0.6, 0.15);
			level.playSound(null, pos, SoundEvents.ENDERMAN_STARE, SoundSource.HOSTILE, 1.5f, 0.5f);
			level.playSound(null, pos, SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 2.0f, 0.6f);
		} catch (Exception ignored) {
		}
	}
}


