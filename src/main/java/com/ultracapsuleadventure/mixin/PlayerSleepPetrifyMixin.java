package com.ultracapsuleadventure.mixin;

import com.mojang.datafixers.util.Either;
import com.ultracapsuleadventure.medusa.Petrify;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Unit;
import net.minecraft.world.attribute.BedRule;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.AbstractBedBlock;
import net.minecraft.world.level.block.state.BlockState;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.List;

@Mixin(ServerPlayer.class)
public abstract class PlayerSleepPetrifyMixin {

	@Inject(method = "startSleepInBed", at = @At("HEAD"), cancellable = true)
	private void UltraCapsuleAdventure$noPetrifiedMonsterBlockSleep(AbstractBedBlock bed, BlockState state, BedRule rule, BlockPos pos,
			CallbackInfoReturnable<Either<Player.BedSleepingProblem, Unit>> cir) {
		try {
			ServerPlayer player = (ServerPlayer) (Object) this;
			if (player.level() == null || player.level().isClientSide()) {
				return;
			}
			List<Monster> monsters = player.level().getEntitiesOfClass(Monster.class,
					player.getBoundingBox().inflate(8.0, 5.0, 8.0));
			boolean anyRealMonster = false;
			for (Monster m : monsters) {
				try {
					if (m.isAlive() && !m.isSpectator() && !Petrify.isPetrified(m)) {
						anyRealMonster = true;
						break;
					}
				} catch (Exception ignored) {
				}
			}
			if (!anyRealMonster && !monsters.isEmpty()) {
				cir.setReturnValue(Either.right(Unit.INSTANCE));
			}
		} catch (Exception ignored) {
		}
	}
}
