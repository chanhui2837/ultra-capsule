package com.partnercap.util;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.partnercap.data.PartnerData;

import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.TagValueOutput;
import net.minecraft.world.phys.AABB;

/**
 * 길들임 판정 / 회복 음식 / 안장·갑옷 / 순간이동 유틸 (MC 26.3).
 */
public class TameUtil {

	public static boolean isOwnedBy(Entity entity, Player player) {
		UUID uuid = player.getUUID();
		if (entity instanceof TamableAnimal tame) {
			try {
				return tame.isTame() && tame.isOwnedBy(player);
			} catch (Exception e) {
				return false;
			}
		}
		if (entity instanceof OwnableEntity own) {
			try {
				Entity owner = own.getOwner();
				if (owner != null) {
					return owner.getUUID().equals(uuid);
				}
			} catch (Exception ignored) {
			}
			try {
				var ref = own.getOwnerReference();
				if (ref != null) {
					UUID u = ref.getUUID();
					return u != null && u.equals(uuid);
				}
			} catch (Exception ignored) {
			}
		}
		return false;
	}

	/** 반경 내 해당 플레이어가 길들인 모든 LivingEntity 수집 */
	public static List<LivingEntity> findOwnedTames(Player player, double radius) {
		List<LivingEntity> out = new ArrayList<>();
		if (!(player.level() instanceof ServerLevel level)) {
			return out;
		}
		AABB box = player.getBoundingBox().inflate(radius);
		List<Entity> entities = level.getEntities(player, box, e -> e instanceof LivingEntity living && living.isAlive() && isOwnedBy(living, player));
		for (Entity e : entities) {
			out.add((LivingEntity) e);
		}
		return out;
	}

	/** 엔티티를 NBT로 직렬화 + PC_* 캐시 부여 (26.3 ValueOutput 방식) */
	public static CompoundTag savePet(LivingEntity entity) {
		CompoundTag tag = new CompoundTag();
		try {
			HolderLookup.Provider registries = entity.registryAccess();
			TagValueOutput output = TagValueOutput.createWithContext(ProblemReporter.DISCARDING, registries);
			entity.saveAsPassenger(output);
			tag = output.buildResult();
		} catch (Exception e) {
			tag = new CompoundTag();
		}
		String typeId;
		try {
			typeId = net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString();
		} catch (Exception e) {
			typeId = "unknown";
		}
		tag.putString("id", typeId);
		tag.putString(PartnerData.PC_TYPE, typeId);
		try {
			tag.putFloat(PartnerData.PC_HEALTH, entity.getHealth());
			tag.putFloat(PartnerData.PC_MAX, entity.getMaxHealth());
		} catch (Exception ignored) {
		}
		try {
			if (entity.hasCustomName() && entity.getCustomName() != null) {
				tag.putString(PartnerData.PC_NAME, entity.getCustomName().getString());
			} else if (!tag.contains(PartnerData.PC_NAME)) {
				tag.putString(PartnerData.PC_NAME, "");
			}
		} catch (Exception ignored) {
		}
		if (!tag.contains(PartnerData.PC_ARMOR)) {
			tag.putString(PartnerData.PC_ARMOR, "");
		}
		// 이미 착용 중인 안장/갑옷을 캡슐 플래그에도 반영 (GUI 장착/해제 표시용)
		try {
			net.minecraft.world.item.ItemStack body = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.BODY);
			if (body != null && !body.isEmpty()) {
				try {
					tag.putString(PartnerData.PC_ARMOR,
							net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(body.getItem()).toString());
				} catch (Exception ignored) {
				}
			}
		} catch (Exception ignored) {
		}
		try {
			net.minecraft.world.item.ItemStack saddle = entity.getItemBySlot(net.minecraft.world.entity.EquipmentSlot.SADDLE);
			if (saddle != null && !saddle.isEmpty()) {
				tag.putBoolean(PartnerData.PC_SADDLED, true);
				tag.putBoolean("Saddle", true);
			}
		} catch (Exception ignored) {
		}
		return tag;
	}

	// ---------- 회복 ----------

	public record HealResult(boolean ok, String message, int consumed, float healedTo) {
	}

	/** 엔티티 타입별 회복량. 0이면 해당 아이템으로 회복 불가 */
	public static float foodHealAmount(String typeId, Item item) {
		String t = typeId.toLowerCase();
		boolean isWolf = t.contains("wolf");
		boolean isCat = t.contains("cat") || t.contains("ocelot");
		boolean isParrot = t.contains("parrot");
		boolean isHorse = t.contains("horse") || t.contains("donkey") || t.contains("mule");
		boolean isLlama = t.contains("llama");
		boolean isCamel = t.contains("camel");

		if (isWolf) {
			if (item == Items.PORKCHOP || item == Items.BEEF || item == Items.CHICKEN
					|| item == Items.MUTTON || item == Items.RABBIT) {
				return 3.0f;
			}
			if (item == Items.COOKED_PORKCHOP || item == Items.COOKED_BEEF
					|| item == Items.COOKED_CHICKEN || item == Items.COOKED_MUTTON
					|| item == Items.COOKED_RABBIT) {
				return 8.0f;
			}
			if (item == Items.ROTTEN_FLESH) {
				return 4.0f;
			}
			return 0;
		}
		if (isCat) {
			if (item == Items.COD || item == Items.SALMON || item == Items.TROPICAL_FISH) {
				return 2.0f;
			}
			if (item == Items.COOKED_COD || item == Items.COOKED_SALMON) {
				return 4.0f;
			}
			return 0;
		}
		if (isParrot) {
			if (item == Items.WHEAT_SEEDS || item == Items.MELON_SEEDS
					|| item == Items.PUMPKIN_SEEDS || item == Items.BEETROOT_SEEDS
					|| item == Items.TORCHFLOWER_SEEDS) {
				return 2.0f;
			}
			return 0;
		}
		if (isHorse || isLlama || isCamel) {
			if (item == Items.HAY_BLOCK) {
				return 10.0f;
			}
			if (item == Items.GOLDEN_APPLE || item == Items.GOLDEN_CARROT) {
				return 8.0f;
			}
			if (item == Items.SUGAR) {
				return 2.0f;
			}
			if (item == Items.WHEAT || item == Items.APPLE || item == Items.CARROT) {
				return 4.0f;
			}
			return 0;
		}
		if (item == Items.COOKED_BEEF || item == Items.COOKED_PORKCHOP
				|| item == Items.COOKED_CHICKEN || item == Items.GOLDEN_APPLE) {
			return 4.0f;
		}
		if (item == Items.BEEF || item == Items.PORKCHOP || item == Items.APPLE
				|| item == Items.BREAD || item == Items.CARROT || item == Items.WHEAT) {
			return 2.0f;
		}
		return 0;
	}

	public static ItemStack findFoodInInventory(Player player, String typeId) {
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.isEmpty()) {
				continue;
			}
			if (foodHealAmount(typeId, s.getItem()) > 0) {
				return s;
			}
		}
		return ItemStack.EMPTY;
	}

	public static void consumeFoods(Player player, String typeId, int amount) {
		int left = amount;
		for (int i = 0; i < player.getInventory().getContainerSize() && left > 0; i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.isEmpty() || foodHealAmount(typeId, s.getItem()) <= 0) {
				continue;
			}
			int take = Math.min(left, s.getCount());
			s.shrink(take);
			left -= take;
		}
	}

	/** 회복 1회: 음식 1개 소모, 그 음식 회복량만큼 체력+ */
	public static HealResult healOnce(CompoundTag pet, Player player) {
		String typeId = PartnerData.getTypeId(pet);
		float hp = PartnerData.getHealth(pet);
		float max = PartnerData.getMaxHealth(pet);
		if (hp >= max - 0.001f) {
			return new HealResult(false, "이미 풀체력입니다.", 0, hp);
		}
		ItemStack food = findFoodInInventory(player, typeId);
		if (food.isEmpty()) {
			return new HealResult(false, "먹이가 없습니다! (" + typeId + "용 음식 필요)", 0, hp);
		}
		float heal = foodHealAmount(typeId, food.getItem());
		food.shrink(1);
		float nhp = Math.min(max, hp + heal);
		pet.putFloat("Health", nhp);
		pet.putFloat(PartnerData.PC_HEALTH, nhp);
		return new HealResult(true, "회복 +" + heal + " (음식 1개 소모)", 1, nhp);
	}

	/** 완전회복: 부족분만큼 음식 소모 */
	public static HealResult healFull(CompoundTag pet, Player player) {
		String typeId = PartnerData.getTypeId(pet);
		float hp = PartnerData.getHealth(pet);
		float max = PartnerData.getMaxHealth(pet);
		if (hp >= max - 0.001f) {
			return new HealResult(false, "이미 풀체력입니다.", 0, hp);
		}
		float missing = max - hp;
		List<ItemStack> foods = new ArrayList<>();
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (!s.isEmpty() && foodHealAmount(typeId, s.getItem()) > 0) {
				foods.add(s);
			}
		}
		if (foods.isEmpty()) {
			return new HealResult(false, "먹이가 없습니다!", 0, hp);
		}
		foods.sort((a, b) -> Float.compare(foodHealAmount(typeId, b.getItem()), foodHealAmount(typeId, a.getItem())));
		float healed = 0;
		int consumed = 0;
		for (ItemStack s : foods) {
			while (s.getCount() > 0 && healed < missing - 0.001f) {
				float h = foodHealAmount(typeId, s.getItem());
				healed += h;
				s.shrink(1);
				consumed++;
			}
			if (healed >= missing - 0.001f) {
				break;
			}
		}
		if (healed < missing - 0.001f) {
			float nhp = Math.min(max, hp + healed);
			pet.putFloat("Health", nhp);
			pet.putFloat(PartnerData.PC_HEALTH, nhp);
			return new HealResult(true, "음식 부족! +" + healed + "만 회복 (음식 " + consumed + "개 소모)", consumed, nhp);
		}
		pet.putFloat("Health", max);
		pet.putFloat(PartnerData.PC_HEALTH, max);
		return new HealResult(true, "완전회복! (음식 " + consumed + "개 소모)", consumed, max);
	}

	// ---------- 안장 / 갑옷 ----------

	public static boolean supportsSaddle(String typeId) {
		String t = typeId.toLowerCase();
		return t.contains("horse") || t.contains("donkey") || t.contains("mule")
				|| t.contains("pig") || t.contains("strider") || t.contains("camel")
				|| t.contains("skeleton_horse") || t.contains("zombie_horse");
	}

	public static boolean supportsArmor(String typeId) {
		String t = typeId.toLowerCase();
		return t.contains("wolf") || t.contains("horse") || t.contains("donkey") || t.contains("mule");
	}

	public record GearResult(CompoundTag tag, String message) {
	}

	private static LivingEntity loadTemp(CompoundTag pet, Player player) {
		try {
			Level level = player.level();
			if (level == null) {
				return null;
			}
			EntitySpawnRequest request = new EntitySpawnRequest(EntitySpawnReason.LOAD, true);
			Entity tmp = EntityType.loadEntityRecursive(pet.copy(), level, request, EntityProcessor.NOP);
			if (tmp instanceof LivingEntity living && living.isAlive()) {
				return living;
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private static CompoundTag resaveKeepingName(LivingEntity living, CompoundTag original) {
		String keepName = "";
		try {
			keepName = original.getString(PartnerData.PC_NAME).orElse("");
		} catch (Exception ignored) {
		}
		CompoundTag fresh = savePet(living);
		if (!keepName.isEmpty()) {
			try {
				fresh.putString(PartnerData.PC_NAME, keepName);
			} catch (Exception ignored) {
			}
		}
		return fresh;
	}

	private static ItemStack takeOne(Player player, Item item) {
		try {
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack s = player.getInventory().getItem(i);
				if (!s.isEmpty() && s.getItem() == item) {
					ItemStack one = s.copy();
					one.setCount(1);
					s.shrink(1);
					return one;
				}
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	private static ItemStack takeMatchingArmor(Player player, String typeId) {
		boolean wolf = typeId.toLowerCase().contains("wolf");
		Item[] order = wolf
				? new Item[] { Items.WOLF_ARMOR }
				: new Item[] { Items.DIAMOND_HORSE_ARMOR, Items.NETHERITE_HORSE_ARMOR, Items.GOLDEN_HORSE_ARMOR,
						Items.IRON_HORSE_ARMOR, Items.COPPER_HORSE_ARMOR, Items.LEATHER_HORSE_ARMOR };
		for (Item want : order) {
			try {
				ItemStack one = takeOne(player, want);
				if (one != null) {
					return one;
				}
			} catch (Exception ignored) {
			}
		}
		return null;
	}

	public static void giveToPlayer(ServerPlayer player, ItemStack stack) {
		if (stack == null || stack.isEmpty()) {
			return;
		}
		ItemStack copy = stack.copy();
		try {
			player.getInventory().add(copy);
		} catch (Exception ignored) {
		}
		if (!copy.isEmpty()) {
			try {
				player.drop(copy, false, net.minecraft.util.Prediction.SERVER_ONLY);
			} catch (Exception ignored) {
			}
		}
	}

	/**
	 * 임시 엔티티에 직접 입혀서 저장 (바닐라 NBT와 캡슐 표시가 항상 일치).
	 * 소환 후 모습 = 미리보기 모습 = 해제 후 모습이 완전히 동기화됨.
	 */
	public static GearResult tryEquipFromInventory(CompoundTag pet, Player player) {
		String typeId = PartnerData.getTypeId(pet);
		if (!supportsSaddle(typeId) && !supportsArmor(typeId)) {
			return new GearResult(pet, "이 몹은 안장/갑옷 미지원");
		}
		LivingEntity living = loadTemp(pet, player);
		if (living == null) {
			return new GearResult(pet, "장비 처리 실패 (엔티티 로드 불가)");
		}
		boolean changed = false;
		StringBuilder sb = new StringBuilder();
		if (supportsSaddle(typeId)) {
			ItemStack cur = null;
			try {
				cur = living.getItemBySlot(EquipmentSlot.SADDLE);
			} catch (Exception ignored) {
			}
			if (cur == null || cur.isEmpty()) {
				ItemStack saddle = takeOne(player, Items.SADDLE);
				if (saddle != null) {
					try {
						living.setItemSlot(EquipmentSlot.SADDLE, saddle);
						changed = true;
						sb.append("안장 장착! ");
					} catch (Exception ignored) {
					}
				} else {
					sb.append("안장 없음(인벤토리). ");
				}
			} else {
				sb.append("이미 안장 있음. ");
			}
		}
		if (supportsArmor(typeId)) {
			ItemStack cur = null;
			try {
				cur = living.getItemBySlot(EquipmentSlot.BODY);
			} catch (Exception ignored) {
			}
			if (cur == null || cur.isEmpty()) {
				ItemStack armor = takeMatchingArmor(player, typeId);
				if (armor != null) {
					try {
						living.setItemSlot(EquipmentSlot.BODY, armor);
						changed = true;
						sb.append("갑옷 장착! ");
					} catch (Exception ignored) {
					}
				} else if (!supportsSaddle(typeId)) {
					sb.append("갑옷 없음(인벤토리). ");
				}
			} else {
				sb.append("이미 갑옷 있음. ");
			}
		}
		if (!changed) {
			String s = sb.toString().trim();
			return new GearResult(pet, s.isEmpty() ? "이미 장착됨" : s);
		}
		return new GearResult(resaveKeepingName(living, pet), sb.toString().trim());
	}

	/** 임시 엔티티에서 벗겨서 인벤토리로 회수 후 저장 (안팎 완전 동기화) */
	public static GearResult unequipToInventory(CompoundTag pet, Player player) {
		LivingEntity living = loadTemp(pet, player);
		if (living == null) {
			return new GearResult(pet, "장비 처리 실패 (엔티티 로드 불가)");
		}
		StringBuilder got = new StringBuilder();
		try {
			ItemStack saddle = living.getItemBySlot(EquipmentSlot.SADDLE);
			if (saddle != null && !saddle.isEmpty()) {
				if (player instanceof ServerPlayer sp) {
					giveToPlayer(sp, saddle.copy());
				}
				living.setItemSlot(EquipmentSlot.SADDLE, ItemStack.EMPTY);
				got.append("안장 ");
			}
		} catch (Exception ignored) {
		}
		try {
			ItemStack armor = living.getItemBySlot(EquipmentSlot.BODY);
			if (armor != null && !armor.isEmpty()) {
				if (player instanceof ServerPlayer sp) {
					giveToPlayer(sp, armor.copy());
				}
				living.setItemSlot(EquipmentSlot.BODY, ItemStack.EMPTY);
				got.append("갑옷 ");
			}
		} catch (Exception ignored) {
		}
		CompoundTag fresh = resaveKeepingName(living, pet);
		String what = got.length() == 0 ? "해제할 장비 없음" : got.toString().trim() + " 회수!";
		return new GearResult(fresh, what);
	}

	/** 소환 직후 이름+안장+갑옷 적용 (26.x는 SADDLE/BODY 슬롯에 직접 장착) */
	public static void applyEquipment(Entity summoned, CompoundTag pet) {
		try {
			String name = pet.getString(PartnerData.PC_NAME).orElse("");
			if (!name.isEmpty() && summoned instanceof LivingEntity living) {
				living.setCustomName(net.minecraft.network.chat.Component.literal(name));
				living.setCustomNameVisible(true);
			}
		} catch (Exception ignored) {
		}
		if (!(summoned instanceof LivingEntity living)) {
			return;
		}
		String typeId = PartnerData.getTypeId(pet);
		try {
			if (pet.getBoolean(PartnerData.PC_SADDLED).orElse(false) && supportsSaddle(typeId)) {
				living.setItemSlot(net.minecraft.world.entity.EquipmentSlot.SADDLE, new ItemStack(Items.SADDLE));
			}
		} catch (Exception ignored) {
		}
		try {
			String armorId = pet.getString(PartnerData.PC_ARMOR).orElse("");
			if (!armorId.isEmpty() && supportsArmor(typeId)) {
				Item armor = lookupArmorItem(armorId);
				if (armor != null) {
					living.setItemSlot(net.minecraft.world.entity.EquipmentSlot.BODY, new ItemStack(armor));
				}
			}
		} catch (Exception ignored) {
		}
	}

	public static Item lookupArmorItem(String armorId) {
		try {
			int i = armorId.indexOf(':');
			if (i < 0) {
				return null;
			}
			net.minecraft.resources.Identifier id = net.minecraft.resources.Identifier.fromNamespaceAndPath(
					armorId.substring(0, i), armorId.substring(i + 1));
			Object got = net.minecraft.core.registries.BuiltInRegistries.ITEM.getOptional(id).orElse(null);
			if (got instanceof Item item && item != Items.AIR) {
				return item;
			}
		} catch (Exception ignored) {
		}
		return null;
	}

	// ---------- 불러내기(순간이동) ----------

	/** 캡슐 안/밖 상관없이, 현재 차원에 로드된 내 길들임 몹 전부 나에게 순간이동 */
	public static int recallAllToMe(ServerPlayer player) {
		if (!(player.level() instanceof ServerLevel level)) {
			return 0;
		}
		BlockPos p = player.blockPosition();
		AABB box = player.getBoundingBox().inflate(512);
		List<Entity> entities = level.getEntities(player, box, e -> e instanceof LivingEntity living && living.isAlive() && isOwnedBy(living, player));
		int n = 0;
		for (Entity e : entities) {
			try {
				e.teleportTo(p.getX() + 0.5 + (n % 5) - 2, p.getY() + 1, p.getZ() + 0.5 + (n / 5) - 1);
				if (e instanceof TamableAnimal tame) {
					try {
						if (tame.isOrderedToSit()) {
							tame.setOrderedToSit(false);
						}
					} catch (Exception ignored) {
					}
				}
				n++;
			} catch (Exception ignored) {
			}
		}
		return n;
	}
}
