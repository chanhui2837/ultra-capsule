package com.ultracapsuleadventure.medusa;

import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * 메두사의 머리. 드롭 직후엔 미충전(사용 불가).
 * 타이탄 캡슐 수리 → 충전됨(반짝임) → 조준 사용 1회 → 다시 미충전.
 * 잔여 횟수는 내구도 게이지로 표시 (5회분, 다 써도 미세하게 남아 사라지지 않음).
 */
public class MedusaHeadItem extends Item {

	public static final String ROOT_KEY = "medusa_head";
	public static final int USE_RANGE = 32;
	private static final int USE_DURATION = 72000;

	public MedusaHeadItem(Properties properties) {
		super(properties);
	}

	private static CompoundTag getRoot(ItemStack stack) {
		net.minecraft.world.item.component.CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA,
			net.minecraft.world.item.component.CustomData.EMPTY);
		CompoundTag root = custom.copyTag();
		if (!root.contains(ROOT_KEY)) {
			return new CompoundTag();
		}
		return root.getCompound(ROOT_KEY).orElse(new CompoundTag());
	}

	private static void setRoot(ItemStack stack, CompoundTag cap) {
		net.minecraft.world.item.component.CustomData custom = stack.getOrDefault(DataComponents.CUSTOM_DATA,
			net.minecraft.world.item.component.CustomData.EMPTY);
		CompoundTag root = custom.copyTag();
		root.put(ROOT_KEY, cap);
		stack.set(DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(root));
	}

	public static final int MAX_CHARGES = 5;
	/** 내구도 최대치 = 5회분 + 사라짐 방지 1 (등록 시 durability와 동일해야 함) */
	public static final int HEAD_MAX_DAMAGE = 6;

	public static int getCharges(ItemStack stack) {
		try {
			// 구 CUSTOM_DATA 방식 1회 마이그레이션 → 내구도
			CompoundTag root = getRoot(stack);
			if (root.contains("charges") || root.contains("charged")) {
				int old = 0;
				try {
					if (root.contains("charges")) {
						old = Math.max(0, root.getInt("charges").orElse(0));
					} else if (root.getBoolean("charged").orElse(false)) {
						old = MAX_CHARGES;
					}
				} catch (Exception ignored) {
				}
				old = Math.max(0, Math.min(MAX_CHARGES, old));
				try {
					stack.setDamageValue(MAX_CHARGES - old);
				} catch (Exception ignored) {
				}
				try {
					CompoundTag cap = getRoot(stack);
					cap.remove("charges");
					cap.remove("charged");
					setRoot(stack, cap);
				} catch (Exception ignored) {
				}
				return old;
			}
		} catch (Exception ignored) {
		}
		try {
			int dmg = Math.max(0, stack.getDamageValue());
			return Math.max(0, Math.min(MAX_CHARGES, (HEAD_MAX_DAMAGE - 1) - dmg));
		} catch (Exception e) {
			return 0;
		}
	}

	public static boolean isCharged(ItemStack stack) {
		return getCharges(stack) > 0;
	}

	public static void setCharged(ItemStack stack, boolean v) {
		try {
			try {
				stack.setDamageValue(v ? 0 : (HEAD_MAX_DAMAGE - 1));
			} catch (Exception ignored) {
			}
			try {
				CompoundTag cap = getRoot(stack);
				cap.remove("charges");
				cap.remove("charged");
				setRoot(stack, cap);
			} catch (Exception ignored) {
			}
		} catch (Exception ignored) {
		}
	}

	/** 1회 소모 후 남은 횟수 반환 (내구도 최대치에서 멈춰 사라지지 않음) */
	public static int consumeCharge(ItemStack stack) {
		try {
			int cur = 0;
			try {
				cur = Math.max(0, stack.getDamageValue());
			} catch (Exception ignored) {
			}
			try {
				stack.setDamageValue(Math.min(HEAD_MAX_DAMAGE - 1, cur + 1));
			} catch (Exception ignored) {
			}
			try {
				CompoundTag cap = getRoot(stack);
				cap.remove("charges");
				cap.remove("charged");
				setRoot(stack, cap);
			} catch (Exception ignored) {
			}
			return getCharges(stack);
		} catch (Exception ignored) {
			return 0;
		}
	}

	@Override
	public boolean isFoil(ItemStack stack) {
		try {
			return isCharged(stack);
		} catch (Exception e) {
			return false;
		}
	}

	/** 활처럼: 우클릭 꾹(조준) → 놓으면 발동. 조준 시작만 하고 발동은 releaseUsing에서. */
	@Override
	public InteractionResult use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean creative = false;
		try {
			creative = player.isCreative();
		} catch (Exception ignored) {
		}
		if (!creative && !isCharged(stack)) {
			if (!level.isClientSide()) {
				try {
					player.sendSystemMessage(Component.literal("메두사의 머리에 힘이 없다... 타이탄 캡슐에서 수리하라!"));
				} catch (Exception ignored) {
				}
			}
			return InteractionResult.FAIL;
		}
		try {
			player.startUsingItem(hand);
		} catch (Exception e) {
			return InteractionResult.FAIL;
		}
		return InteractionResult.CONSUME;
	}

	@Override
	public int getUseDuration(ItemStack stack, LivingEntity entity) {
		return USE_DURATION;
	}

	@Override
	public ItemUseAnimation getUseAnimation(ItemStack stack) {
		return ItemUseAnimation.BOW;
	}

	@Override
	public void onUseTick(Level level, LivingEntity living, ItemStack stack, int remaining) {
		int used = USE_DURATION - remaining;
		if (level.isClientSide() || !(living instanceof ServerPlayer sp)) {
			return;
		}
		// 조준 시작 신호음 (놓는 순간 발동, 최소 조준 시간 없음)
		try {
			if (used == 1) {
				level.playSound(null, sp.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
						SoundSource.PLAYERS, 0.6f, 0.6f);
			}
		} catch (Exception ignored) {
		}
	}

	/** 우클릭을 놓는 순간 발동 (조준 시간과 무관, 놓는 순간 시선 방향 대상에게 판정). */
	@Override
	public boolean releaseUsing(ItemStack stack, Level level, LivingEntity living, int timeLeft) {
		if (level.isClientSide() || !(living instanceof Player player)) {
			return true;
		}
		fire(level, player, stack);
		return true;
	}

	/** 실제 석화 발사: 놓는 순간의 시선 방향으로 판정 */
	private static void fire(Level level, Player player, ItemStack stack) {
		boolean creative = false;
		try {
			creative = player.isCreative();
		} catch (Exception ignored) {
		}
		if (!creative && !isCharged(stack)) {
			return;
		}
		if (!(level instanceof ServerLevel sl)) {
			return;
		}
		LivingEntity hit = null;
		double bestT = USE_RANGE;
		try {
			Vec3 eye = player.getEyePosition();
			Vec3 dir = player.getLookAngle();
			Vec3 end = eye.add(dir.scale(USE_RANGE));
			AABB sweep = new AABB(eye, end).inflate(2.0);
			for (Entity e : sl.getEntities(player, sweep,
					x -> x instanceof LivingEntity living && living.isAlive())) {
				try {
					if (e == player) {
						continue;
					}
					if (e.getVehicle() == player || e == player.getVehicle()) {
						continue;
					}
					if (e instanceof StatueEntity) {
						continue;
					}
					if (Petrify.isPetrified(e)) {
						continue;
					}
					LivingEntity le = (LivingEntity) e;
					// 안대 착용자는 머리 발사에도 면역
					try {
						if (BlindfoldItem.protects(le)) {
							continue;
						}
					} catch (Exception ignored) {
					}
					Vec3 toEnt = new Vec3(e.getX() - eye.x, (e.getY() + e.getBbHeight() * 0.5) - eye.y,
							e.getZ() - eye.z);
					double t = toEnt.dot(dir);
					if (t < 0 || t > bestT) {
						continue;
					}
					Vec3 closest = eye.add(dir.scale(t));
					Vec3 center = new Vec3(e.getX(), e.getY() + e.getBbHeight() * 0.5, e.getZ());
					if (closest.distanceTo(center) < e.getBbWidth() + 0.9) {
						hit = le;
						bestT = t;
					}
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		if (hit == null) {
			try {
				player.sendSystemMessage(Component.literal("조준 실패! 석화 가능한 대상을 똑바로 봐라!"));
			} catch (Exception ignored) {
			}
			return;
		}
		boolean ok = false;
		try {
			if (hit instanceof EnderDragon dragon) {
				ok = Petrify.applyMedusaDragonDamage(sl, dragon, null, false, true);
			} else if (hit instanceof ServerPlayer sp) {
				ok = Petrify.petrifyPlayer(sl, sp, null) != null;
			} else {
				java.util.UUID me = null;
				try {
					me = player.getUUID();
				} catch (Exception ignored) {
				}
				ok = Petrify.petrify(sl, hit, me) != null;
			}
		} catch (Exception ignored) {
		}
		if (!ok) {
			return;
		}
		if (!creative) {
			int left = consumeCharge(stack);
			try {
				if (player instanceof ServerPlayer ssp) {
					com.ultracapsuleadventure.item.UltraCapsuleAdventureItem.syncCapsule(ssp);
				}
			} catch (Exception ignored) {
			}
			try {
				if (left > 0) {
					player.sendSystemMessage(Component.literal("석화! (내구도 게이지 확인)"));
				} else {
					player.sendSystemMessage(Component.literal("석화! 머리의 힘이 다했다. 재수리가 필요하다."));
				}
			} catch (Exception ignored) {
			}
		} else {
			try {
				player.sendSystemMessage(Component.literal("석화! (크리에이티브 무한 사용)"));
			} catch (Exception ignored) {
			}
		}
	}

	@Override
	public void appendHoverText(ItemStack stack, Item.TooltipContext context, TooltipDisplay display,
			java.util.function.Consumer<Component> lines, TooltipFlag flag) {
		try {
			if (getCharges(stack) > 0) {
				lines.accept(Component.literal("충전됨 - 우클릭 꾹 조준 → 놓으면 원거리 석화"));
			} else {
				lines.accept(Component.literal("힘이 없음 - 타이탄 캡슐에서 수리 필요"));
			}
			lines.accept(Component.literal("크리에이티브: 수리 없이 무한 사용"));
		} catch (Exception ignored) {
		}
	}
}



