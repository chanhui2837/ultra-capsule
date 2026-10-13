package com.partnercap.flame;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 열관: 화로 비슷한 블록. 연료 없이 용광로 2배속(50틱)으로 제련.
 * 모래/고기/광물 전부 가능 (화로 제련품).
 * 우클릭: 손에 든 제련품 넣기 / 빈손 우클릭: 결과물 꺼내기 / 웅크리기 우클릭: 진행중 재료 회수.
 */
public class YeolgwanBlock extends Block implements EntityBlock {

	public YeolgwanBlock(Properties properties) {
		super(properties);
	}

	@Override
	public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
		try {
			return new YeolgwanBlockEntity(pos, state);
		} catch (Exception e) {
			return null;
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
		try {
			if (level.isClientSide()) {
				return null;
			}
			if (type == ModFlame.YEOLGWAN_ENTITY) {
				return (l, p, s, be) -> {
					try {
						if (be instanceof YeolgwanBlockEntity y) {
							YeolgwanBlockEntity.tick((ServerLevel) l, p, s, y);
						}
					} catch (Exception ignored) {
					}
				};
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	@Override
	protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		try {
			if (level.isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			if (!(level instanceof ServerLevel sl)) {
				return InteractionResult.PASS;
			}
			BlockEntity be = sl.getBlockEntity(pos);
			if (!(be instanceof YeolgwanBlockEntity y)) {
				return InteractionResult.PASS;
			}
			// 웅크리기 우클릭: 재료 회수
			try {
				if (player.isCrouching()) {
					ItemStack back = y.takeInput();
					if (!back.isEmpty() && player instanceof ServerPlayer sp) {
						com.partnercap.util.TameUtil.giveToPlayer(sp, back);
						player.sendSystemMessage(Component.literal("열관 재료 회수!"));
					} else {
						player.sendSystemMessage(Component.literal("꺼낼 재료가 없다!"));
					}
					return InteractionResult.SUCCESS;
				}
			} catch (Exception ignored) {
			}
			// 빈손: 결과물 꺼내기
			try {
				if (stack.isEmpty()) {
					ItemStack out = y.takeOutput();
					if (!out.isEmpty() && player instanceof ServerPlayer sp) {
						com.partnercap.util.TameUtil.giveToPlayer(sp, out);
						player.sendSystemMessage(Component.literal("열관에서 꺼냈다!"));
					} else {
						int prog = y.progressPercent();
						player.sendSystemMessage(Component.literal(prog > 0 ? ("굽는 중... " + prog + "%") : "열관이 비어 있다! 구울 것을 들고 우클릭!"));
					}
					return InteractionResult.SUCCESS;
				}
			} catch (Exception ignored) {
			}
			// 제련품 넣기
			try {
				ItemStack one = stack.copy();
				one.setCount(1);
				if (!YeolgwanBlockEntity.isSmeltable(one)) {
					player.sendSystemMessage(Component.literal("열관에서 구울 수 없는 물건이다! (모래/고기/광물)"));
					return InteractionResult.FAIL;
				}
				if (y.insertInput(one)) {
					try {
						stack.shrink(1);
					} catch (Exception ignored) {
					}
					player.sendSystemMessage(Component.literal("열관에 넣었다! (연료 불필요, 용광로 2배속)"));
				} else {
					player.sendSystemMessage(Component.literal("열관이 가득 찼다! 빈손 우클릭으로 꺼내라!"));
				}
				return InteractionResult.SUCCESS;
			} catch (Exception ignored) {
			}
			return InteractionResult.PASS;
		} catch (Exception e) {
			return InteractionResult.PASS;
		}
	}

	@Override
	protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean moved) {
		try {
			BlockEntity be = level.getBlockEntity(pos);
			if (be instanceof YeolgwanBlockEntity y) {
				try {
					for (ItemStack s : y.dropAll()) {
						if (!s.isEmpty()) {
							ItemEntity drop = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, s);
							level.addFreshEntity(drop);
						}
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		try {
			super.affectNeighborsAfterRemoval(state, level, pos, moved);
		} catch (Exception ignored) {
		}
	}

	/** 제련 결과 하드코딩표 (화로 제련품: 모래/고기/광물/기타). */
	public static ItemStack smeltResult(Item item) {
		try {
			return SMELT.getOrDefault(item, ItemStack.EMPTY);
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}

	private static final Map<Item, ItemStack> SMELT = new HashMap<>();

	static {
		try {
			// 모래/흙계
			SMELT.put(Items.SAND, new ItemStack(Items.GLASS));
			SMELT.put(Items.RED_SAND, new ItemStack(Items.GLASS));
			SMELT.put(Items.COBBLESTONE, new ItemStack(Items.STONE));
			SMELT.put(Items.COBBLED_DEEPSLATE, new ItemStack(Items.DEEPSLATE));
			SMELT.put(Items.SANDSTONE, new ItemStack(Items.SMOOTH_SANDSTONE));
			SMELT.put(Items.RED_SANDSTONE, new ItemStack(Items.SMOOTH_RED_SANDSTONE));
			SMELT.put(Items.STONE_BRICKS, new ItemStack(Items.CRACKED_STONE_BRICKS));
			SMELT.put(Items.CLAY_BALL, new ItemStack(Items.BRICK));
			SMELT.put(Items.CLAY, new ItemStack(Items.TERRACOTTA));
			SMELT.put(Items.NETHERRACK, new ItemStack(Items.NETHER_BRICK));
			SMELT.put(Items.CACTUS, new ItemStack(Items.DYE.pick(net.minecraft.world.item.DyeColor.GREEN)));
			SMELT.put(Items.SEA_PICKLE, new ItemStack(Items.DYE.pick(net.minecraft.world.item.DyeColor.LIME)));
			// 고기/음식
			SMELT.put(Items.PORKCHOP, new ItemStack(Items.COOKED_PORKCHOP));
			SMELT.put(Items.BEEF, new ItemStack(Items.COOKED_BEEF));
			SMELT.put(Items.CHICKEN, new ItemStack(Items.COOKED_CHICKEN));
			SMELT.put(Items.MUTTON, new ItemStack(Items.COOKED_MUTTON));
			SMELT.put(Items.RABBIT, new ItemStack(Items.COOKED_RABBIT));
			SMELT.put(Items.COD, new ItemStack(Items.COOKED_COD));
			SMELT.put(Items.SALMON, new ItemStack(Items.COOKED_SALMON));
			SMELT.put(Items.KELP, new ItemStack(Items.DRIED_KELP));
			SMELT.put(Items.POTATO, new ItemStack(Items.BAKED_POTATO));
			SMELT.put(Items.CHORUS_FRUIT, new ItemStack(Items.POPPED_CHORUS_FRUIT));
			// 광물 (원석/원광/광석)
			SMELT.put(Items.RAW_IRON, new ItemStack(Items.IRON_INGOT));
			SMELT.put(Items.RAW_GOLD, new ItemStack(Items.GOLD_INGOT));
			SMELT.put(Items.RAW_COPPER, new ItemStack(Items.COPPER_INGOT));
			SMELT.put(Items.IRON_ORE, new ItemStack(Items.IRON_INGOT));
			SMELT.put(Items.GOLD_ORE, new ItemStack(Items.GOLD_INGOT));
			SMELT.put(Items.COPPER_ORE, new ItemStack(Items.COPPER_INGOT));
			SMELT.put(Items.DEEPSLATE_IRON_ORE, new ItemStack(Items.IRON_INGOT));
			SMELT.put(Items.DEEPSLATE_GOLD_ORE, new ItemStack(Items.GOLD_INGOT));
			SMELT.put(Items.DEEPSLATE_COPPER_ORE, new ItemStack(Items.COPPER_INGOT));
			SMELT.put(Items.NETHER_GOLD_ORE, new ItemStack(Items.GOLD_INGOT));
			SMELT.put(Items.ANCIENT_DEBRIS, new ItemStack(Items.NETHERITE_SCRAP));
		} catch (Exception ignored) {
		}
	}
}
