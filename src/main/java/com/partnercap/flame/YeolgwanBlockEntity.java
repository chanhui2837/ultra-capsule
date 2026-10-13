package com.partnercap.flame;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 열관 내부: 입력 8개 + 출력 8개. 조리 50틱 (용광로 100틱의 2배속), 연료 불필요.
 */
public class YeolgwanBlockEntity extends BlockEntity {

	public static final int COOK_TOTAL = 50;
	public static final int SLOT_MAX = 8;

	private final List<ItemStack> inputs = new ArrayList<>();
	private final List<ItemStack> outputs = new ArrayList<>();
	private int cookTime = 0;

	public YeolgwanBlockEntity(BlockPos pos, BlockState state) {
		super(ModFlame.YEOLGWAN_ENTITY, pos, state);
	}

	public static boolean isSmeltable(ItemStack stack) {
		try {
			if (stack.isEmpty()) {
				return false;
			}
			ItemStack r = YeolgwanBlock.smeltResult(stack.getItem());
			return !r.isEmpty();
		} catch (Exception e) {
			return false;
		}
	}

	public boolean insertInput(ItemStack one) {
		try {
			if (!isSmeltable(one)) {
				return false;
			}
			// 같은 종류면 합치기
			for (ItemStack s : inputs) {
				try {
					if (ItemStack.isSameItemSameComponents(s, one) && s.getCount() < 64) {
						s.grow(1);
						setChanged();
						return true;
					}
				} catch (Exception ignored) {
				}
			}
			if (inputs.size() >= SLOT_MAX) {
				return false;
			}
			inputs.add(one.copy());
			setChanged();
			return true;
		} catch (Exception e) {
			return false;
		}
	}

	public ItemStack takeOutput() {
		try {
			if (outputs.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack s = outputs.remove(0);
			setChanged();
			return s;
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}

	public ItemStack takeInput() {
		try {
			if (inputs.isEmpty()) {
				return ItemStack.EMPTY;
			}
			ItemStack s = inputs.remove(inputs.size() - 1);
			cookTime = 0;
			setChanged();
			return s;
		} catch (Exception e) {
			return ItemStack.EMPTY;
		}
	}

	public List<ItemStack> dropAll() {
		List<ItemStack> all = new ArrayList<>();
		try {
			all.addAll(inputs);
			all.addAll(outputs);
			inputs.clear();
			outputs.clear();
		} catch (Exception ignored) {
		}
		return all;
	}

	public int progressPercent() {
		try {
			if (inputs.isEmpty()) {
				return 0;
			}
			return (int) (100.0 * cookTime / COOK_TOTAL);
		} catch (Exception e) {
			return 0;
		}
	}

	public static void tick(ServerLevel level, BlockPos pos, BlockState state, YeolgwanBlockEntity be) {
		try {
			if (be.inputs.isEmpty()) {
				if (be.cookTime != 0) {
					be.cookTime = 0;
					be.setChanged();
				}
				return;
			}
			ItemStack first = be.inputs.get(0);
			ItemStack result = YeolgwanBlock.smeltResult(first.getItem());
			if (result.isEmpty()) {
				be.inputs.remove(0);
				be.cookTime = 0;
				be.setChanged();
				return;
			}
			be.cookTime++;
			if (be.cookTime >= COOK_TOTAL) {
				be.cookTime = 0;
				try {
					first.shrink(1);
					if (first.isEmpty()) {
						be.inputs.remove(0);
					}
				} catch (Exception ignored) {
				}
				ItemStack out = result.copy();
				boolean merged = false;
				for (ItemStack s : be.outputs) {
					try {
						if (ItemStack.isSameItemSameComponents(s, out) && s.getCount() + out.getCount() <= 64) {
							s.grow(out.getCount());
							merged = true;
							break;
						}
					} catch (Exception ignored) {
					}
				}
				if (!merged) {
					if (be.outputs.size() < SLOT_MAX) {
						be.outputs.add(out);
					} else {
						// 출력 가득: 바깥에 튕겨냄
						try {
							net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
									level, pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, out);
							level.addFreshEntity(drop);
						} catch (Exception ignored) {
						}
					}
				}
				try {
					level.sendParticles(net.minecraft.core.particles.ParticleTypes.FLAME,
							pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 6, 0.2, 0.2, 0.2, 0.02);
				} catch (Exception ignored) {
				}
				be.setChanged();
			} else if (be.cookTime % 10 == 0) {
				try {
					level.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
							pos.getX() + 0.5, pos.getY() + 1.1, pos.getZ() + 0.5, 2, 0.15, 0.15, 0.15, 0.01);
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void saveAdditional(ValueOutput out) {
		try {
			super.saveAdditional(out);
		} catch (Exception ignored) {
		}
		try {
			out.putInt("Cook", cookTime);
		} catch (Exception ignored) {
		}
		try {
			var lin = out.list("Inputs", ItemStack.CODEC);
			for (ItemStack s : inputs) {
				try {
					lin.add(s.copy());
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		try {
			var lout = out.list("Outputs", ItemStack.CODEC);
			for (ItemStack s : outputs) {
				try {
					lout.add(s.copy());
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void loadAdditional(ValueInput in) {
		try {
			super.loadAdditional(in);
		} catch (Exception ignored) {
		}
		try {
			cookTime = in.getIntOr("Cook", 0);
		} catch (Exception ignored) {
		}
		try {
			inputs.clear();
			for (ItemStack s : in.listOrEmpty("Inputs", ItemStack.CODEC)) {
				try {
					if (!s.isEmpty() && inputs.size() < SLOT_MAX) {
						inputs.add(s);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		try {
			outputs.clear();
			for (ItemStack s : in.listOrEmpty("Outputs", ItemStack.CODEC)) {
				try {
					if (!s.isEmpty() && outputs.size() < SLOT_MAX) {
						outputs.add(s);
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
	}
}
