package com.ultracapsuleadventure.mech;

import com.ultracapsuleadventure.item.UltraCapsuleAdventureItem;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * 메카 캡슐. 조합 불가, 도구제작자 거래로만 입수.
 * 들고 꾹 우클릭 3초 충전 → 인벤토리에서 소모 + 로봇 변신 (펼침 모션).
 */
public class MechCapsuleItem extends Item {

	public static final int CHARGE_TICKS = 60;
	private static final int USE_DURATION = 72000;
	/** 캡슐 내구도 = 로봇 체력 (MechRobotEntity.CAPSULE_DURABILITY와 동일) */
	public static final int MAX_DURABILITY = 200;

	public MechCapsuleItem(Properties properties) {
		super(properties);
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		return true;
	}

	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		try {
			player.startUsingItem(hand);
		} catch (Exception e) {
			return InteractionResult.FAIL;
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_DURATION;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
		int used = USE_DURATION - remaining;
		if (!(living instanceof Player player)) {
			return;
		}
		if (level.isClientSide()) {
			return;
		}
		if (!(living instanceof ServerPlayer sp)) {
			return;
		}
		if (used % 5 == 0 && level instanceof ServerLevel sl) {
			try {
				for (int i = 0; i < 3; i++) {
					double ox = (sl.getRandom().nextDouble() - 0.5) * 1.2;
					double oy = sl.getRandom().nextDouble() * 2.0;
					double oz = (sl.getRandom().nextDouble() - 0.5) * 1.2;
					sl.sendParticles(net.minecraft.core.particles.ParticleTypes.ELECTRIC_SPARK,
							player.getX() + ox, player.getY() + oy, player.getZ() + oz,
							1, 0, 0.15, 0, 0.0);
				}
			} catch (Exception ignored) {
			}
		}
		if (used % 10 == 0 && used < CHARGE_TICKS) {
			int k = (int) (12.0 * used / CHARGE_TICKS);
			StringBuilder bar = new StringBuilder("로봇 변신 충전 ");
			for (int i = 0; i < 12; i++) {
				bar.append(i < k ? "■" : "□");
			}
			try {
				sp.sendSystemMessage(Component.literal(bar.toString()));
			} catch (Exception ignored) {
			}
			try {
				float pitch = 0.5f + 1.0f * (used / (float) CHARGE_TICKS);
				level.playSound(null, sp.blockPosition(),
						net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
						SoundSource.PLAYERS, 0.8f, pitch);
			} catch (Exception ignored) {
			}
		}
		if (used >= CHARGE_TICKS) {
			try {
				sp.stopUsingItem();
			} catch (Exception ignored) {
			}
			ItemStack thrown = ItemStack.EMPTY;
			try {
				if (!stack.isEmpty()) {
					thrown = stack.copyWithCount(1);
					stack.shrink(1);
				}
			} catch (Exception ignored) {
			}
			if (thrown.isEmpty()) {
				return;
			}
			// 실제 캡슐을 던짐 → 착지 지점에서 변신 (끊김 없는 인과)
			try {
				Vec3 look = sp.getLookAngle();
				net.minecraft.world.entity.item.ItemEntity toss = new net.minecraft.world.entity.item.ItemEntity(
						(ServerLevel) level, sp.getX(), sp.getY() + 1.4, sp.getZ(), thrown);
				toss.setDeltaMovement(look.x * 0.7, 0.45, look.z * 0.7);
				// 감시 중 주워지면 변신이 취소되므로 픽업 불가 (착지 감지 전까지)
				try {
					toss.setThrower(sp);
				} catch (Exception ignored) {
				}
				try {
					toss.setNeverPickUp();
				} catch (Exception ignored) {
				}
				((ServerLevel) level).addFreshEntity(toss);
				MechRobotEntity.watchThrown((ServerLevel) level, toss, sp.getUUID());
			} catch (Exception e) {
				try {
					com.ultracapsuleadventure.util.TameUtil.giveToPlayer(sp, thrown);
				} catch (Exception ignored) {
				}
				return;
			}
			UltraCapsuleAdventureItem.syncCapsule(sp);
			try {
				sp.sendSystemMessage(Component.literal("로봇 변신 시작!"));
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
		int used = USE_DURATION - timeLeft;
		if (!level.isClientSide() && living instanceof ServerPlayer sp && used < CHARGE_TICKS) {
			try {
				sp.sendSystemMessage(Component.literal("충전 취소됨"));
			} catch (Exception ignored) {
			}
		}
		return true;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		lines.accept(Component.literal("조합 불가 - 도구제작자 거래로 입수"));
		lines.accept(Component.literal("들고 우클릭 꾹(3초): 로봇으로 변신"));
		lines.accept(Component.literal("탑승 후 좌클릭: 레이저빔 / 스페이스: 점프"));
		lines.accept(Component.literal("탑승 중 좌클릭 꾹(3초): 캡슐로 복귀 (변신 충전과 동일)"));
		lines.accept(Component.literal("쉬프트 하차 후 웅크리기 우클릭 꾹: 캡슐로 복귀"));
		lines.accept(Component.literal("로봇 체력=HUD 차량체력+오버레이 표시 ↔ 캡슐 내구도 연동"));
	}
}



