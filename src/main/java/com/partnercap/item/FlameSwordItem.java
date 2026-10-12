package com.partnercap.item;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 불길의 검. 맞은 적에게 불을 붙이고, 이 검으로 처치하면 영혼이 소울 캡슐에 저장됨.
 */
public class FlameSwordItem extends Item {

	public FlameSwordItem(Properties properties) {
		super(properties);
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		try {
			target.igniteForSeconds(4.0F);
		} catch (Exception ignored) {
		}
	}
}
