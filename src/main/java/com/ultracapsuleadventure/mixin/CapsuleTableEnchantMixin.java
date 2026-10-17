package com.ultracapsuleadventure.mixin;

import java.util.List;
import java.util.stream.Stream;

import com.ultracapsuleadventure.util.EnchantLevels;

import net.minecraft.core.Holder;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * 인챈트 테이블 후보 제한: 캡슐(소울·도박·타이탄)에는 지정 인챈트만 뜬다.
 * 바닐라 태그 체계(melee_weapon 등)로는 약탈만/행운+약탈만 같은 조합을
 * 표현할 수 없어서 결과 목록에서 직접 거른다.
 * (소울=약탈, 도박=행운·약탈, 타이탄=효율·빠른 장전)
 */
@Mixin(EnchantmentHelper.class)
public abstract class CapsuleTableEnchantMixin {

	@Inject(method = "getAvailableEnchantmentResults", at = @At("RETURN"), cancellable = true)
	private static void UltraCapsuleAdventure$onlyDesignated(int level, ItemStack stack,
			Stream<Holder<Enchantment>> enchantments,
			CallbackInfoReturnable<List<EnchantmentInstance>> cir) {
		try {
			List<EnchantmentInstance> out = cir.getReturnValue();
			if (out == null || out.isEmpty()) {
				return;
			}
			if (!EnchantLevels.isCapsule(stack)) {
				return;
			}
			out.removeIf(inst -> {
				try {
					return inst == null || !EnchantLevels.isCapsuleEnchantAllowed(stack, inst.enchantment());
				} catch (Exception ignored) {
					return false;
				}
			});
		} catch (Exception ignored) {
		}
	}
}


