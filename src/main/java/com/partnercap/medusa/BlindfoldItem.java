package com.partnercap.medusa;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;

/**
 * 안대. 투구 슬롯에 장착하면 바닐라 실명 효과가 유지되고,
 * 메두사 시선 석화(본체 응시·머리 발사 모두)에 면역이 된다.
 * 조합: 실 + 가죽 + 실 (1줄) → 1개.
 */
public class BlindfoldItem extends Item {

	public BlindfoldItem(Properties properties) {
		super(properties);
	}

	/** 안대 착용 중이면 시선 석화 면역 */
	public static boolean protects(LivingEntity e) {
		try {
			if (e == null || !e.isAlive()) {
				return false;
			}
			ItemStack head = null;
			try {
				head = e.getItemBySlot(EquipmentSlot.HEAD);
			} catch (Exception ignored) {
				return false;
			}
			return head != null && !head.isEmpty() && head.getItem() instanceof BlindfoldItem;
		} catch (Exception ex) {
			return false;
		}
	}

	/** 착용 중 실명 유지 (20틱마다 30틱 부여 → 벗으면 최대 1.5초 잔상) */
	public static void register() {
		try {
			ServerTickEvents.END_SERVER_TICK.register(server -> {
				try {
					if (server.getTickCount() % 20 != 0) {
						return;
					}
					for (ServerPlayer p : server.getPlayerList().getPlayers()) {
						try {
							if (p == null || !p.isAlive()) {
								continue;
							}
							if (!protects(p)) {
								continue;
							}
							try {
								p.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 30, 0, false, false, true));
							} catch (Exception ignored) {
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

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		lines.accept(Component.literal("투구 슬롯 장착: 앞이 안 보이지만 메두사 시선 면역"));
		lines.accept(Component.literal("장착 중 실명 효과 유지"));
	}
}
