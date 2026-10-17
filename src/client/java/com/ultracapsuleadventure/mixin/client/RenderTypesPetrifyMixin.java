package com.ultracapsuleadventure.mixin.client;

import com.ultracapsuleadventure.client.StoneSwap;

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

	private static Identifier UltraCapsuleAdventure$stone(Identifier texture) {
		return StoneSwap.ACTIVE ? StoneSwap.STONE : texture;
	}

	@ModifyVariable(method = "entitySolid", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$solid(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "entitySolidGlint", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$solidGlint(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "entitySolidZOffsetForward", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$solidZ(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "entityCutout", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$cutout(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "entityCutout(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$cutoutB(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "entityCutoutCull", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$cutoutCull(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "entityTranslucent", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$translucent(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "entityTranslucent(Lnet/minecraft/resources/Identifier;Z)Lnet/minecraft/client/renderer/rendertype/RenderType;", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$translucentB(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "entityTranslucentCull", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$translucentCull(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "entityTranslucentEmissive", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$translucentEmissive(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "armorCutoutNoCull", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$armor(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "armorCutoutNoCullGlint", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$armorGlint(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "eyes", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$eyes(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}

	@ModifyVariable(method = "outline", at = @At("HEAD"), argsOnly = true)
	private static Identifier UltraCapsuleAdventure$outline(Identifier texture) {
		return UltraCapsuleAdventure$stone(texture);
	}
}


