package com.partnercap.mixin.client;

import com.partnercap.client.StoneSwap;

import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * 석화된 생물을 그리는 동안에는 몸통·갑옷·발광눈 등 모든 엔티티 텍스처를 돌로 바꿔치기.
 */
@Mixin(RenderTypes.class)
public abstract class RenderTypesPetrifyMixin {

	private static Identifier partnercap$stone(Identifier texture) {
		return StoneSwap.ACTIVE ? StoneSwap.STONE : texture;
	}

	@ModifyVariable(method = "entitySolid", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$solid(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "entitySolidGlint", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$solidGlint(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "entitySolidZOffsetForward", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$solidZ(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "entityCutout", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$cutout(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "entityCutout(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$cutoutB(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "entityCutoutCull", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$cutoutCull(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "entityTranslucent", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$translucent(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "entityTranslucent(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$translucentB(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "entityTranslucentCull", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$translucentCull(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "entityTranslucentEmissive", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$translucentEmissive(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "armorCutoutNoCull", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$armor(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "armorCutoutNoCullGlint", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$armorGlint(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "eyes", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$eyes(Identifier texture) {
		return partnercap$stone(texture);
	}

	@ModifyVariable(method = "outline", at = @At("HEAD"), argsOnly = true)
	private static Identifier partnercap$outline(Identifier texture) {
		return partnercap$stone(texture);
	}
}
