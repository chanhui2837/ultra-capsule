package com.partnercap.flame;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 초월의 막대: 겨눈 대상(16m, 시선) 이동속도 저하. 쿨 10초.
 * 화염몽이 항상 오른손에 들고 있음. 플레이어도 주워서 사용 가능.
 */
public class TranscendWandItem extends Item {

	public static final double RANGE = 16.0;
	public static final int SLOW_TICKS = 60;
	public static final int COOLDOWN_TICKS = 200;

	public TranscendWandItem(Properties properties) {
		super(properties);
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		try {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (!(level instanceof ServerLevel sl)) {
				return InteractionResult.FAIL;
			}
			if (player.getCooldowns().isOnCooldown(player.getItemInHand(hand))) {
				try {
					player.sendSystemMessage(Component.literal("지팡이가 아직 식는 중이다!"));
				} catch (Exception ignored) {
				}
				return InteractionResult.FAIL;
			}
			LivingEntity hit = null;
			double bestT = RANGE;
			try {
				Vec3 eye = player.getEyePosition();
				Vec3 dir = player.getLookAngle();
				Vec3 end = eye.add(dir.scale(RANGE));
				AABB sweep = new AABB(eye, end).inflate(2.0);
				for (Entity e : sl.getEntities(player, sweep,
						x -> x instanceof LivingEntity living && living.isAlive() && x != player)) {
					try {
						LivingEntity le = (LivingEntity) e;
						Vec3 toEnt = new Vec3(e.getX() - eye.x, (e.getY() + e.getBbHeight() * 0.5) - eye.y,
								e.getZ() - eye.z);
						double t = toEnt.dot(dir);
						if (t < 0 || t > bestT) {
							continue;
						}
						Vec3 closest = eye.add(dir.scale(t));
						Vec3 center = new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
						if (closest.distanceTo(center) < e.getBbWidth() + 0.9) {
							hit = le;
							bestT = t;
						}
					} catch (Exception ignored) {
					}
				}
			} catch (Exception ignored) {
			}
			if (hit == null) {
				try {
					player.sendSystemMessage(Component.literal("겨눌 대상이 없다! 똑바로 봐라!"));
				} catch (Exception ignored) {
				}
				return InteractionResult.FAIL;
			}
			try {
				hit.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, SLOW_TICKS, 1, false, true, true));
			} catch (Exception e) {
				return InteractionResult.FAIL;
			}
			try {
				player.getCooldowns().addCooldown(player.getItemInHand(hand), COOLDOWN_TICKS);
			} catch (Exception ignored) {
			}
			try {
				sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ENCHANT,
						hit.getX(), hit.getY() + 1, hit.getZ(), 16, 0.4, 0.8, 0.4, 0.1);
			} catch (Exception ignored) {
			}
			try {
				sl.playSound(null, player.blockPosition(), SoundEvents.EVOKER_CAST_SPELL, SoundSource.PLAYERS, 0.8f, 1.4f);
			} catch (Exception ignored) {
			}
			return InteractionResult.SUCCESS;
		} catch (Exception e) {
			return InteractionResult.FAIL;
		}
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		try {
			lines.accept(Component.literal("겨눈 대상 이동속도 저하 (16m, 쿨 10초)"));
			lines.accept(Component.literal("화염몽이 오른손에 항상 소지"));
		} catch (Exception ignored) {
		}
	}
}
