package com.partnercap.medusa;

import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

/**
 * 석화 석상. Mob이지만 AI 없음 + 무적 + 고정 + 밂힘 불가.
 * 곡괭이(돌 캘 수 있는 도구) 우클릭으로만 제거 → 조약돌 2개.
 * 그 외 상호작용은 조용히 무시 (안내 메시지 없음).
 */
public class StatueEntity extends Mob {

	private static final EntityDataAccessor<String> VICTIM = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Float> YAW = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> PITCH = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> BODY = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> HEAD = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> WPOS = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Float> WSPD = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.FLOAT);
	private static final EntityDataAccessor<Integer> FTICK = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> BABY = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> CUBE = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Boolean> ISPLAYER = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<String> PNAME = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.STRING);
	private static final EntityDataAccessor<Boolean> CROUCH = SynchedEntityData.defineId(StatueEntity.class,
			EntityDataSerializers.BOOLEAN);

	private double lockX;
	private double lockY;
	private double lockZ;
	private boolean locked = false;

	public StatueEntity(EntityType<? extends StatueEntity> type, Level level) {
		super(type, level);
		try {
			setNoAi(true);
		} catch (Exception ignored) {
		}
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 20.0)
				.add(Attributes.MOVEMENT_SPEED, 0.0);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(VICTIM, "minecraft:pig");
		builder.define(YAW, 0f);
		builder.define(PITCH, 0f);
		builder.define(BODY, 0f);
		builder.define(HEAD, 0f);
		builder.define(WPOS, 0f);
		builder.define(WSPD, 0f);
		builder.define(FTICK, 0);
		builder.define(BABY, false);
		builder.define(CUBE, 0);
		builder.define(ISPLAYER, false);
		builder.define(PNAME, "");
		builder.define(CROUCH, false);
	}

	public void setVisual(String victimType, float yaw, float pitch, float body, float head,
			float wpos, float wspd, int ftick, boolean baby, int cube,
			boolean isPlayer, String pname, boolean crouch) {
		try {
			entityData.set(VICTIM, victimType == null ? "minecraft:pig" : victimType);
			entityData.set(YAW, yaw);
			entityData.set(PITCH, pitch);
			entityData.set(BODY, body);
			entityData.set(HEAD, head);
			entityData.set(WPOS, wpos);
			entityData.set(WSPD, wspd);
			entityData.set(FTICK, ftick);
			entityData.set(BABY, baby);
			entityData.set(CUBE, cube);
			entityData.set(ISPLAYER, isPlayer);
			entityData.set(PNAME, pname == null ? "" : pname);
			entityData.set(CROUCH, crouch);
		} catch (Exception ignored) {
		}
	}

	public String getVictimType() {
		try {
			return entityData.get(VICTIM);
		} catch (Exception e) {
			return "minecraft:pig";
		}
	}

	public float getSYaw() {
		try {
			return entityData.get(YAW);
		} catch (Exception e) {
			return 0f;
		}
	}

	public float getSPitch() {
		try {
			return entityData.get(PITCH);
		} catch (Exception e) {
			return 0f;
		}
	}

	public float getSBody() {
		try {
			return entityData.get(BODY);
		} catch (Exception e) {
			return 0f;
		}
	}

	public float getSHead() {
		try {
			return entityData.get(HEAD);
		} catch (Exception e) {
			return 0f;
		}
	}

	public float getSWpos() {
		try {
			return entityData.get(WPOS);
		} catch (Exception e) {
			return 0f;
		}
	}

	public float getSWspd() {
		try {
			return entityData.get(WSPD);
		} catch (Exception e) {
			return 0f;
		}
	}

	public int getFTick() {
		try {
			return entityData.get(FTICK);
		} catch (Exception e) {
			return 0;
		}
	}

	public boolean getSBaby() {
		try {
			return entityData.get(BABY);
		} catch (Exception e) {
			return false;
		}
	}

	public int getSCube() {
		try {
			return entityData.get(CUBE);
		} catch (Exception e) {
			return 0;
		}
	}

	public boolean getSIsPlayer() {
		try {
			return entityData.get(ISPLAYER);
		} catch (Exception e) {
			return false;
		}
	}

	public String getSPName() {
		try {
			return entityData.get(PNAME);
		} catch (Exception e) {
			return "";
		}
	}

	public boolean getSCrouch() {
		try {
			return entityData.get(CROUCH);
		} catch (Exception e) {
			return false;
		}
	}

	@Override
	public boolean isInvulnerable() {
		// false로 둬야 곡괭이 좌클릭이 ALLOW_DAMAGE까지 도달한다.
		// 실제 무적 판정은 MedusaEvents.ALLOW_DAMAGE/ALLOW_DEATH가 담당 (곡괭이 외 전부 차단).
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public void tick() {
		try {
			super.tick();
		} catch (Exception e) {
			return;
		}
		try {
			if (!locked) {
				lockX = getX();
				lockY = getY();
				lockZ = getZ();
				locked = true;
			} else {
				double dx = getX() - lockX;
				double dy = getY() - lockY;
				double dz = getZ() - lockZ;
				if (dx * dx + dy * dy + dz * dz > 0.0001) {
					setPos(lockX, lockY, lockZ);
				}
			}
			setDeltaMovement(0, 0, 0);
		} catch (Exception ignored) {
		}
		try {
			clearFire();
		} catch (Exception ignored) {
		}
		try {
			getNavigation().stop();
		} catch (Exception ignored) {
		}
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		try {
			if (hand != InteractionHand.MAIN_HAND) {
				return InteractionResult.PASS;
			}
			if (level().isClientSide()) {
				return InteractionResult.SUCCESS;
			}
			ItemStack held = ItemStack.EMPTY;
			try {
				held = player.getItemInHand(hand);
			} catch (Exception ignored) {
			}
			boolean pickaxe = false;
			try {
				pickaxe = !held.isEmpty()
						&& held.isCorrectToolForDrops(Blocks.COBBLESTONE.defaultBlockState());
			} catch (Exception ignored) {
			}
			if (!pickaxe) {
				return InteractionResult.SUCCESS;
			}
			// 곡괭이 채굴: 석상 제거 + 조약돌 2개, 원본은 돌아오지 않음
			if (level() instanceof ServerLevel sl) {
				try {
					for (int i = 0; i < 2; i++) {
						net.minecraft.world.entity.item.ItemEntity drop = new net.minecraft.world.entity.item.ItemEntity(
								sl, getX(), getY() + 0.8, getZ(), new ItemStack(Blocks.COBBLESTONE.asItem()));
						sl.addFreshEntity(drop);
					}
				} catch (Exception ignored) {
				}
				try {
					sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
							getX(), getY() + 1, getZ(), 10, 0.5, 0.8, 0.5, 0.05);
				} catch (Exception ignored) {
				}
				try {
					sl.playSound(null, blockPosition(),
							net.minecraft.sounds.SoundEvents.STONE_BREAK,
							net.minecraft.sounds.SoundSource.BLOCKS, 1.0f, 1.0f);
				} catch (Exception ignored) {
				}
			}
			try {
				discard();
			} catch (Exception ignored) {
			}
			return InteractionResult.SUCCESS;
		} catch (Exception e) {
			return InteractionResult.PASS;
		}
	}

	@Override
	protected void addAdditionalSaveData(ValueOutput out) {
		try {
			super.addAdditionalSaveData(out);
		} catch (Exception ignored) {
		}
		try {
			out.putString("Victim", getVictimType());
			out.putFloat("SYaw", entityData.get(YAW));
			out.putFloat("SPitch", entityData.get(PITCH));
			out.putFloat("SBody", entityData.get(BODY));
			out.putFloat("SHead", entityData.get(HEAD));
			out.putFloat("SWpos", entityData.get(WPOS));
			out.putFloat("SWspd", entityData.get(WSPD));
			out.putInt("FTick", entityData.get(FTICK));
			out.putBoolean("Baby", entityData.get(BABY));
			out.putInt("Cube", entityData.get(CUBE));
			out.putBoolean("IsPlayer", entityData.get(ISPLAYER));
			out.putString("PName", entityData.get(PNAME));
			out.putBoolean("Crouch", entityData.get(CROUCH));
			out.putDouble("LockX", lockX);
			out.putDouble("LockY", lockY);
			out.putDouble("LockZ", lockZ);
		} catch (Exception ignored) {
		}
	}

	@Override
	protected void readAdditionalSaveData(ValueInput in) {
		try {
			super.readAdditionalSaveData(in);
		} catch (Exception ignored) {
		}
		try {
			String victim = in.getString("Victim").orElse("minecraft:pig");
			float yaw = in.getFloatOr("SYaw", 0f);
			float pitch = in.getFloatOr("SPitch", 0f);
			float body = in.getFloatOr("SBody", 0f);
			float head = in.getFloatOr("SHead", 0f);
			float wpos = in.getFloatOr("SWpos", 0f);
			float wspd = in.getFloatOr("SWspd", 0f);
			int ftick = in.getInt("FTick").orElse(0);
			boolean baby = in.getBooleanOr("Baby", false);
			int cube = in.getInt("Cube").orElse(0);
			boolean isPlayer = in.getBooleanOr("IsPlayer", false);
			String pname = in.getString("PName").orElse("");
			boolean crouch = in.getBooleanOr("Crouch", false);
			setVisual(victim, yaw, pitch, body, head, wpos, wspd, ftick, baby, cube, isPlayer, pname, crouch);
			lockX = in.getDoubleOr("LockX", getX());
			lockY = in.getDoubleOr("LockY", getY());
			lockZ = in.getDoubleOr("LockZ", getZ());
			locked = true;
		} catch (Exception ignored) {
		}
	}

	public static boolean isStatue(Entity e) {
		return e instanceof StatueEntity;
	}
}
