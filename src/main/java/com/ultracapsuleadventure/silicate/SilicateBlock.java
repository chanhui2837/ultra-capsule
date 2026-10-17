package com.ultracapsuleadventure.silicate;

import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;

/**
 * 규산 블록 (운석의 몸체).
 * SPENT=true 이면 이 운석에서는 이미 먹보 도깨비가 나왔다는 표시 (운석당 1마리 제한).
 * 외형·드롭은 SPENT와 무관하게 동일하다.
 */
public class SilicateBlock extends Block {

	public static final BooleanProperty SPENT = BooleanProperty.create("spent");

	public SilicateBlock(Properties properties) {
		super(properties);
		this.registerDefaultState(this.stateDefinition.any().setValue(SPENT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(SPENT);
	}
}


