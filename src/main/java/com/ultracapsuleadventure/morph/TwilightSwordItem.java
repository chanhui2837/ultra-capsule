package com.ultracapsuleadventure.morph;

import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * 황혼의 검: 파란 점토 2개 + 막대. 맞은 적에게 어둠 효과를 남기고,
 * 이 검으로 처치하면 은신 캡슐에 그 엔티티가 등록됨 (MorphEvents가 처리).
 */
public class TwilightSwordItem extends Item {

	public TwilightSwordItem(Properties properties) {
		super(properties);
	}

	@Override
	public void hurtEnemy(ItemStack stack, LivingEntity target, LivingEntity attacker) {
		super.hurtEnemy(stack, target, attacker);
		try {
			target.addEffect(new MobEffectInstance(MobEffects.DARKNESS, 60, 0, false, false, true));
		} catch (Exception ignored) {
		}
		try {
			target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, false, true, true));
		} catch (Exception ignored) {
		}
	}
}


