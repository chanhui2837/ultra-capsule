package com.partnercap.flame;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;

/**
 * 화염룡의 피가 담긴 유리병: 마시면 3분간 F키 홀드 화염 브레스.
 * (빈손일 때만, 입에서 불이 나간다)
 */
public class DragonBloodBottleItem extends Item {

	public static final long DURATION_MS = 180000L;

	public DragonBloodBottleItem(Properties properties) {
		super(properties);
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return 32;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.DRINK;
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
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity living) {
		try {
			if (!level.isClientSide() && living instanceof ServerPlayer sp) {
				FlameBreath.grant(sp, DURATION_MS);
				try {
					sp.sendSystemMessage(Component.literal("몸속에 용의 피가 끓는다! 3분간 빈손으로 F를 꾹 눌러 불을 뿜어라!"));
				} catch (Exception ignored) {
				}
				try {
					level.playSound(null, sp.blockPosition(), SoundEvents.FIRECHARGE_USE, net.minecraft.sounds.SoundSource.PLAYERS, 1.0f, 0.8f);
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		ItemStack out = ItemStack.EMPTY;
		try {
			out = new ItemStack(Items.GLASS_BOTTLE);
		} catch (Exception ignored) {
		}
		try {
			stack.shrink(1);
		} catch (Exception ignored) {
		}
		if (stack.isEmpty()) {
			return out.isEmpty() ? stack : out;
		}
		try {
			if (living instanceof Player p && !out.isEmpty()) {
				try {
					if (!p.getInventory().add(out)) {
						try {
							net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
									level, p.getX(), p.getY() + 0.5, p.getZ(), out);
							level.addFreshEntity(drop);
						} catch (Exception ignored) {
						}
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		return stack;
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		try {
			lines.accept(Component.literal("마시면 3분간 화염 브레스!"));
			lines.accept(Component.literal("빈손 + F 꾹: 입에서 불"));
		} catch (Exception ignored) {
		}
	}
}
