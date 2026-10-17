package com.ultracapsuleadventure.fluid;

import java.util.Optional;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.FlowingFluid;

import com.ultracapsuleadventure.item.ModItems;

/**
 * 공허의 액체 (검은 물. 엔드에 웅덩이로 생성, 양동이로 퍼나름).
 */
public abstract class VoidFluid extends FlowingFluid {

	@Override
	public Fluid getFlowing() {
		return ModFluids.VOID_FLOWING;
	}

	@Override
	public Fluid getSource() {
		return ModFluids.VOID_STILL;
	}

	@Override
	public boolean isSame(Fluid fluid) {
		return fluid == ModFluids.VOID_STILL || fluid == ModFluids.VOID_FLOWING;
	}

	@Override
	public Item getBucket() {
		return ModItems.VOID_BUCKET;
	}

	@Override
	protected BlockState createLegacyBlock(FluidState state) {
		return ModBlocks.VOID_LIQUID.defaultBlockState().setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
	}

	@Override
	public Optional<SoundEvent> getPickupSound() {
		return Optional.of(SoundEvents.BUCKET_FILL);
	}

	@Override
	protected boolean canConvertToSource(ServerLevel level) {
		return true;
	}

	@Override
	protected int getSlopeFindDistance(LevelReader level) {
		return 4;
	}

	@Override
	public int getDropOff(LevelReader level) {
		return 1;
	}

	@Override
	public int getTickDelay(LevelReader level) {
		return 5;
	}

	@Override
	public boolean canBeReplacedWith(FluidState state, BlockGetter level, BlockPos pos, Fluid fluid, Direction direction) {
		return direction == Direction.DOWN && !fluid.is(ModFluids.VOID_TAG);
	}

	@Override
	protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
		net.minecraft.world.level.block.entity.BlockEntity be = null;
		try {
			be = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
		} catch (Exception ignored) {
		}
		try {
			net.minecraft.world.level.block.Block.dropResources(state, level, pos, be);
		} catch (Exception ignored) {
		}
	}

	@Override
	public float getExplosionResistance() {
		return 100.0F;
	}

	public static class Flowing extends VoidFluid {
		@Override
		protected void createFluidStateDefinition(StateDefinition.Builder<Fluid, FluidState> builder) {
			super.createFluidStateDefinition(builder);
			builder.add(LEVEL);
		}

		@Override
		public int getAmount(FluidState state) {
			return state.getValue(LEVEL);
		}

		@Override
		public boolean isSource(FluidState state) {
			return false;
		}
	}

	public static class Still extends VoidFluid {
		@Override
		public int getAmount(FluidState state) {
			return 8;
		}

		@Override
		public boolean isSource(FluidState state) {
			return true;
		}
	}
}


