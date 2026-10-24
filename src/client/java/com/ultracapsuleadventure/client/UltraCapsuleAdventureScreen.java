package com.ultracapsuleadventure.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.ultracapsuleadventure.data.PartnerData;
import com.ultracapsuleadventure.network.ModPackets;
import com.ultracapsuleadventure.soul.SoulData;
import com.ultracapsuleadventure.voidcap.VoidData;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.entity.EntityRenderDispatcher;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityProcessor;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntitySpawnRequest;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * C키 관리창 (MC 26.3).
 * - 겹침 없는 상단 고정 레이아웃 + 휠 스크롤
 * - 각 펫의 3D 외형 미리보기 (바닐라 인벤토리 인형과 동일 방식)
 */
public class UltraCapsuleAdventureScreen extends Screen {

	private final ItemStack capsuleSnapshot;
	private List<CompoundTag> pets = new ArrayList<>();
	private final List<EditBox> nameBoxes = new ArrayList<>();
	private final Map<Integer, Entity> previews = new HashMap<>();
	private final List<HoldHealButton> healButtons = new ArrayList<>();
	private int topIndex = 0;
	private String lastSig = "";

	public enum Mode {
		PARTNER,
		SOUL,
		VOID,
		TITAN,
		GAMBLE,
		DIS,
		MECH,
		STEALTH
	}

	public Mode mode = Mode.PARTNER;
	private boolean stealthSynced = false;

	private static final String[] MODE_NAMES = { "울트라 캡슐 어드벤처", "영혼 캡슐", "공허 캡슐", "타이탄 캡슐", "도박 캡슐", "분해 캡슐", "메카 캡슐", "은신 캡슐" };

	private static final int START_Y = 38;
	private static final int ROW_H = 56;

	public UltraCapsuleAdventureScreen(ItemStack capsule) {
		super(Component.literal("울트라 캡슐 어드벤처"));
		this.capsuleSnapshot = capsule.copy();
		refreshPets();
	}

	private void refreshPets() {
		if (mode == Mode.TITAN || mode == Mode.GAMBLE || mode == Mode.DIS || mode == Mode.MECH) {
			pets = new ArrayList<>();
			if (topIndex > maxTop()) {
				topIndex = maxTop();
			}
			return;
		}
		if (mode == Mode.STEALTH) {
			// 등록 목록을 합성 태그 행으로 올려 파트너/소울창과 같은 스크롤·미리보기 기구를 공유
			List<CompoundTag> rows = new ArrayList<>();
			try {
				ItemStack h = findLiveStealthCapsule();
				if (h == null) {
					h = capsuleSnapshot;
				}
				for (String id : com.ultracapsuleadventure.morph.MorphData.getAll(h)) {
					CompoundTag t = new CompoundTag();
					t.putString("id", id);
					rows.add(t);
				}
			} catch (Exception ignored) {
			}
			pets = rows;
			if (topIndex > maxTop()) {
				topIndex = maxTop();
			}
			return;
		}
		if (mode == Mode.VOID) {
			ItemStack live = findLiveVoidCapsule();
			if (live != null) {
				pets = VoidData.getVoids(live);
			} else {
				pets = VoidData.getVoids(capsuleSnapshot);
			}
		} else if (mode == Mode.SOUL) {
			ItemStack live = findLiveSoulCapsule();
			if (live != null) {
				pets = SoulData.getSouls(live);
			} else {
				pets = SoulData.getSouls(capsuleSnapshot);
			}
		} else {
			ItemStack live = findLiveCapsule();
			if (live != null) {
				pets = PartnerData.getPets(live);
			} else {
				pets = PartnerData.getPets(capsuleSnapshot);
			}
		}
		if (topIndex > maxTop()) {
			topIndex = maxTop();
		}
	}

	private ItemStack findLiveCapsule() {
		if (Minecraft.getInstance().player == null) {
			return null;
		}
		var player = Minecraft.getInstance().player;
		if (player.getMainHandItem().getItem() instanceof com.ultracapsuleadventure.item.UltraCapsuleAdventureItem) {
			return player.getMainHandItem();
		}
		if (player.getOffhandItem().getItem() instanceof com.ultracapsuleadventure.item.UltraCapsuleAdventureItem) {
			return player.getOffhandItem();
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() instanceof com.ultracapsuleadventure.item.UltraCapsuleAdventureItem) {
				return s;
			}
		}
		return null;
	}

	private ItemStack findLiveVoidCapsule() {
		if (Minecraft.getInstance().player == null) {
			return null;
		}
		var player = Minecraft.getInstance().player;
		if (player.getMainHandItem().getItem() instanceof com.ultracapsuleadventure.item.VoidCapsuleItem) {
			return player.getMainHandItem();
		}
		if (player.getOffhandItem().getItem() instanceof com.ultracapsuleadventure.item.VoidCapsuleItem) {
			return player.getOffhandItem();
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() instanceof com.ultracapsuleadventure.item.VoidCapsuleItem) {
				return s;
			}
		}
		return null;
	}

	private ItemStack findLiveTitanCapsule() {
		if (Minecraft.getInstance().player == null) {
			return null;
		}
		var player = Minecraft.getInstance().player;
		if (player.getMainHandItem().getItem() instanceof com.ultracapsuleadventure.titan.TitanCapsuleItem) {
			return player.getMainHandItem();
		}
		if (player.getOffhandItem().getItem() instanceof com.ultracapsuleadventure.titan.TitanCapsuleItem) {
			return player.getOffhandItem();
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() instanceof com.ultracapsuleadventure.titan.TitanCapsuleItem) {
				return s;
			}
		}
		return null;
	}

	private ItemStack findLiveGambleCapsule() {
		if (Minecraft.getInstance().player == null) {
			return null;
		}
		var player = Minecraft.getInstance().player;
		if (player.getMainHandItem().getItem() instanceof com.ultracapsuleadventure.item.GambleCapsuleItem) {
			return player.getMainHandItem();
		}
		if (player.getOffhandItem().getItem() instanceof com.ultracapsuleadventure.item.GambleCapsuleItem) {
			return player.getOffhandItem();
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() instanceof com.ultracapsuleadventure.item.GambleCapsuleItem) {
				return s;
			}
		}
		return null;
	}

	private ItemStack findLiveDisCapsule() {
		if (Minecraft.getInstance().player == null) {
			return null;
		}
		var player = Minecraft.getInstance().player;
		if (player.getMainHandItem().getItem() instanceof com.ultracapsuleadventure.item.DisassembleCapsuleItem) {
			return player.getMainHandItem();
		}
		if (player.getOffhandItem().getItem() instanceof com.ultracapsuleadventure.item.DisassembleCapsuleItem) {
			return player.getOffhandItem();
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() instanceof com.ultracapsuleadventure.item.DisassembleCapsuleItem) {
				return s;
			}
		}
		return null;
	}

	private ItemStack findLiveMechCapsule() {
		if (Minecraft.getInstance().player == null) {
			return null;
		}
		var player = Minecraft.getInstance().player;
		if (player.getMainHandItem().getItem() instanceof com.ultracapsuleadventure.mech.MechCapsuleItem) {
			return player.getMainHandItem();
		}
		if (player.getOffhandItem().getItem() instanceof com.ultracapsuleadventure.mech.MechCapsuleItem) {
			return player.getOffhandItem();
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() instanceof com.ultracapsuleadventure.mech.MechCapsuleItem) {
				return s;
			}
		}
		return null;
	}

	private ItemStack findLiveSoulCapsule() {		if (Minecraft.getInstance().player == null) {
			return null;
		}
		var player = Minecraft.getInstance().player;
		if (player.getMainHandItem().getItem() instanceof com.ultracapsuleadventure.item.SoulCapsuleItem) {
			return player.getMainHandItem();
		}
		if (player.getOffhandItem().getItem() instanceof com.ultracapsuleadventure.item.SoulCapsuleItem) {
			return player.getOffhandItem();
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() instanceof com.ultracapsuleadventure.item.SoulCapsuleItem) {
				return s;
			}
		}
		return null;
	}

	private ItemStack findLiveStealthCapsule() {
		if (Minecraft.getInstance().player == null) {
			return null;
		}
		var player = Minecraft.getInstance().player;
		if (player.getMainHandItem().getItem() instanceof com.ultracapsuleadventure.morph.StealthCapsuleItem) {
			return player.getMainHandItem();
		}
		if (player.getOffhandItem().getItem() instanceof com.ultracapsuleadventure.morph.StealthCapsuleItem) {
			return player.getOffhandItem();
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() instanceof com.ultracapsuleadventure.morph.StealthCapsuleItem) {
				return s;
			}
		}
		return null;
	}

	/** 행 시작 Y (은신은 상단 인간복귀 버튼만큼 아래) */
	private int rowTop() {
		return mode == Mode.STEALTH ? START_Y + 22 : START_Y;
	}

	private int visibleRows() {
		return Math.max(1, Math.min(8, (this.height - rowTop() - 66) / ROW_H));
	}

	private int maxTop() {
		return Math.max(0, pets.size() - visibleRows());
	}

	private String signature() {
		StringBuilder sb = new StringBuilder();
		if (mode == Mode.TITAN) {
			sb.append("T|");
			try {
				ItemStack t = findLiveTitanCapsule();
				if (t == null) {
					t = capsuleSnapshot;
				}
				long nowSec = System.currentTimeMillis() / 1000;
				sb.append(com.ultracapsuleadventure.titan.TitanData.hasStored(t)).append(',')
						.append(com.ultracapsuleadventure.titan.TitanData.getLabel(t)).append(',')
						.append(com.ultracapsuleadventure.titan.TitanData.getDoneAt(t) / 1000 - nowSec).append(',')
						.append(com.ultracapsuleadventure.titan.TitanData.getCooldownUntil(t) / 1000 - nowSec).append(',')
						.append(com.ultracapsuleadventure.titan.TitanData.hasAnvil(t));
				var p2 = Minecraft.getInstance().player;
				if (p2 != null) {
					for (int i = 0; i < p2.getInventory().getContainerSize(); i++) {
						try {
							ItemStack s = p2.getInventory().getItem(i);
							if (s == null || s.isEmpty()) {
								continue;
							}
							sb.append(';').append(i).append(':')
									.append(s.getDamageValue()).append(':').append(s.getCount());
							try {
								if (com.ultracapsuleadventure.titan.TitanData.isRepairingItem(s)) {
									sb.append(':').append(
											(com.ultracapsuleadventure.titan.TitanData.getRepairDoneAt(s)
													- System.currentTimeMillis()) / 1000);
								}
							} catch (Exception ignored) {
							}
						} catch (Exception ignored) {
						}
					}
				}
			} catch (Exception ignored) {
			}
			return sb.toString();
		}
		if (mode == Mode.GAMBLE || mode == Mode.DIS || mode == Mode.MECH || mode == Mode.STEALTH) {
			try {
				var player = Minecraft.getInstance().player;
				if (mode == Mode.GAMBLE) {
					sb.append("G|");
					ItemStack g = findLiveGambleCapsule();
					if (g == null) {
						g = capsuleSnapshot;
					}
					boolean has = com.ultracapsuleadventure.item.GambleCapsuleItem.hasBet(g);
					sb.append(has).append(',');
					if (has && player != null) {
						try {
							ItemStack bet = com.ultracapsuleadventure.item.GambleCapsuleItem.readBet(g, player.registryAccess());
							sb.append(bet.getHoverName().getString()).append(',').append(bet.getCount());
						} catch (Exception ignored) {
						}
					}
					try {
						sb.append(',').append(com.ultracapsuleadventure.item.GambleCapsuleItem.getUntil(g) / 1000
								- System.currentTimeMillis() / 1000);
					} catch (Exception ignored) {
					}
				} else if (mode == Mode.DIS) {
					sb.append("D|");
					ItemStack d = findLiveDisCapsule();
					if (d == null) {
						d = capsuleSnapshot;
					}
					boolean has = com.ultracapsuleadventure.item.DisassembleCapsuleItem.hasStored(d);
					sb.append(has).append(',');
					if (has && player != null) {
						try {
							ItemStack st = com.ultracapsuleadventure.item.DisassembleCapsuleItem.readStored(d,
									player.registryAccess());
							sb.append(st.getHoverName().getString()).append(',').append(st.getCount());
						} catch (Exception ignored) {
						}
					}
					try {
						sb.append(',').append(com.ultracapsuleadventure.item.DisassembleCapsuleItem.getExpect(d));
					} catch (Exception ignored) {
					}
				} else if (mode == Mode.STEALTH) {
					sb.append("H|");
					ItemStack h = findLiveStealthCapsule();
					if (h == null) {
						h = capsuleSnapshot;
					}
					try {
						for (String id : com.ultracapsuleadventure.morph.MorphData.getAll(h)) {
							sb.append(id).append(';');
						}
						sb.append('|').append(com.ultracapsuleadventure.morph.MorphData.getSelectedIndex(h));
					} catch (Exception ignored) {
					}
					try {
						var pl = Minecraft.getInstance().player;
						if (pl != null) {
							sb.append('|').append(com.ultracapsuleadventure.morph.MorphEvents.morphId(pl));
						}
					} catch (Exception ignored) {
					}
				} else {
					sb.append("M|");
					ItemStack m = findLiveMechCapsule();
					if (m == null) {
						m = capsuleSnapshot;
					}
					sb.append(m.getCount());
				}
			} catch (Exception ignored) {
			}
			return sb.toString();
		}
		sb.append(mode == Mode.SOUL ? "S|" : (mode == Mode.VOID ? "V|" : "P|"));
		sb.append(pets.size()).append('|');
		for (CompoundTag pet : pets) {
			if (mode == Mode.SOUL) {
				sb.append(SoulData.getTypeId(pet)).append(',').append(SoulData.getLevel(pet)).append(';');
				continue;
			}
			if (mode == Mode.VOID) {
				sb.append(PartnerData.getTypeId(pet)).append(',')
						.append(PartnerData.getHealth(pet)).append(';');
				continue;
			}
			sb.append(PartnerData.getTypeId(pet)).append(',')
					.append(PartnerData.getHealth(pet)).append(',')
					.append(PartnerData.getMaxHealth(pet)).append(',')
					.append(pet.getString(PartnerData.PC_NAME).orElse("")).append(',')
					.append(PartnerData.isSaddled(pet)).append(',')
					.append(PartnerData.getArmorId(pet)).append(';');
		}
		return sb.toString();
	}

	private void label(int x, int y, int w, String text) {
		StringWidget widget = new StringWidget(x, y, w, 12, Component.literal(text), this.font);
		this.addRenderableWidget(widget);
	}

	@Override
	protected void init() {
		nameBoxes.clear();
		healButtons.clear();
		refreshPets();
		int cx = this.width / 2;

		label(cx - 170, 8, 200, MODE_NAMES[mode.ordinal()] + " 관리창 (C)");
		this.addRenderableWidget(Button.builder(
				Component.literal(MODE_NAMES[mode.ordinal()]), b -> {
					Mode[] all = Mode.values();
					mode = all[(mode.ordinal() + 1) % all.length];
					topIndex = 0;
					rebuildWidgets();
				}).bounds(cx + 60, 6, 110, 18).build());
		if (mode == Mode.SOUL) {
			label(cx - 170, 20, 460, "불길의 검 처치로 저장 · 이름/회복 없음 · 버튼: 순환");
		} else if (mode == Mode.TITAN) {
			label(cx - 170, 20, 460, "[맡기기] 인벤토리 자동 탐색 · 완료 후 꺼내기 · 3분 쿨타임");
		} else if (mode == Mode.VOID) {
			label(cx - 170, 20, 460, "엔티티 우클릭 흡수 · 풀어주기/존재 삭제 · 버튼: 순환");
		} else if (mode == Mode.STEALTH) {
			label(cx - 170, 20, 460, "황혼의 검 처치로 무제한 등록(중복 제외) · 행 버튼으로 변신/삭제");
		} else {
			label(cx - 170, 20, 460, "캡슐: 길들인 몹 우클릭 저장 · 휠: 스크롤 · 회복 꾹(0.6초)=풀회복");
		}

		if (mode == Mode.TITAN) {
			buildTitanPanel(cx);
			lastSig = signature();
			return;
		}
		if (mode == Mode.STEALTH) {
			// 열 때 서버 상태를 한 번 요청해 클라 잔상과 맞춘다
			if (!stealthSynced) {
				stealthSynced = true;
				try {
					ClientPlayNetworking.send(new ModPackets.StealthSyncRequestPayload());
				} catch (Exception ignored) {
				}
			}
			buildStealthRows(cx);
			lastSig = signature();
			return;
		}
		if (mode == Mode.GAMBLE || mode == Mode.DIS || mode == Mode.MECH) {
			buildUtilityPanel(cx);
			lastSig = signature();
			return;
		}

		int vis = visibleRows();
		int from = Math.min(topIndex, maxTop());
		int to = Math.min(pets.size(), from + vis);

		for (int idx = from; idx < to; idx++) {
			final int petIndex = idx;
			CompoundTag pet = pets.get(idx);
			int row = idx - from;
			int y = rowTop() + row * ROW_H;

			if (mode == Mode.VOID) {
				String typeId = PartnerData.getTypeId(pet);
				String shortType = typeId.contains(":") ? typeId.substring(typeId.indexOf(':') + 1) : typeId;
				String name = pet.getString(PartnerData.PC_NAME).orElse("");
				float hp = PartnerData.getHealth(pet);
				float max = PartnerData.getMaxHealth(pet);
				Entity previewNow = getPreview(idx);
				String pvMark = previewNow == null ? " (외형없음)" : "";
				label(cx - 124, y, 300, (idx + 1) + ". " + shortType + (name.isEmpty() ? "" : " \"" + name + "\"") + pvMark);
				label(cx - 124, y + 12, 300, String.format("체력 %.1f / %.1f (동결중)", hp, max));
				this.addRenderableWidget(Button.builder(Component.literal("풀어주기"), b -> {
					ClientPlayNetworking.send(new ModPackets.VoidReleasePayload(petIndex));
				}).bounds(cx - 124, y + 26, 60, 18).build());
				this.addRenderableWidget(Button.builder(Component.literal("존재 삭제"), b -> {
					ClientPlayNetworking.send(new ModPackets.VoidErasePayload(petIndex));
				}).bounds(cx - 60, y + 26, 60, 18).build());
				continue;
			}

			if (mode == Mode.SOUL) {
				String typeId = SoulData.getTypeId(pet);
				String shortType = typeId.contains(":") ? typeId.substring(typeId.indexOf(':') + 1) : typeId;
				int lv = SoulData.getLevel(pet);
				Entity previewNow = getPreview(idx);
				String pvMark = previewNow == null ? " (외형없음)" : "";
				label(cx - 124, y, 300, (idx + 1) + ". Lv." + lv + " " + shortType + pvMark);
				label(cx - 124, y + 12, 300, "소환하면 널 위해 싸운다 (투명+파랑빛)");
				this.addRenderableWidget(Button.builder(Component.literal("소환"), b -> {
					ClientPlayNetworking.send(new ModPackets.SoulSummonOnePayload(petIndex));
				}).bounds(cx - 124, y + 26, 52, 18).build());
				this.addRenderableWidget(Button.builder(Component.literal("합성"), b -> {
					ClientPlayNetworking.send(new ModPackets.SoulMergePayload(petIndex));
				}).bounds(cx - 68, y + 26, 52, 18).build());
				this.addRenderableWidget(Button.builder(Component.literal("삭제"), b -> {
					ClientPlayNetworking.send(new ModPackets.SoulDeletePayload(petIndex));
				}).bounds(cx - 12, y + 26, 52, 18).build());
				continue;
			}

			String typeId = PartnerData.getTypeId(pet);
			String shortType = typeId.contains(":") ? typeId.substring(typeId.indexOf(':') + 1) : typeId;
			String name = pet.getString(PartnerData.PC_NAME).orElse("");
			float hp = PartnerData.getHealth(pet);
			float max = PartnerData.getMaxHealth(pet);
			float frac = max <= 0 ? 0 : Math.max(0, Math.min(1, hp / max));
			int filled = Math.round(frac * 10);
			String bar = "■".repeat(Math.max(0, filled)) + "□".repeat(Math.max(0, 10 - filled));

			Entity previewNow = getPreview(idx);
			String pvMark = previewNow == null ? " (외형없음)" : "";
			label(cx - 124, y, 300, (idx + 1) + ". " + shortType + (name.isEmpty() ? "" : " \"" + name + "\"") + pvMark);
			label(cx - 124, y + 12, 300, String.format("체력 %.1f / %.1f", hp, max));

			String equip = "";
			if (PartnerData.isSaddled(pet)) {
				equip += "[안장] ";
			}
			if (!PartnerData.getArmorId(pet).isEmpty()) {
				equip += "[갑옷] ";
			}
			if (!equip.isEmpty()) {
				label(cx + 96, y + 12, 120, equip.trim());
			}

			String curName = pet.getString(PartnerData.PC_NAME).orElse("");
			EditBox nameBox = new EditBox(this.font, cx - 124, y + 26, 104, 18, Component.literal("이름"));
			nameBox.setMaxLength(32);
			nameBox.setValue(curName);
			this.addRenderableWidget(nameBox);
			nameBoxes.add(nameBox);

			this.addRenderableWidget(Button.builder(Component.literal("이름적용"), b -> {
				String v = nameBox.getValue();
				ClientPlayNetworking.send(new ModPackets.RenamePetPayload(petIndex, v == null ? "" : v));
			}).bounds(cx - 16, y + 26, 58, 18).build());

			// 회복: 클릭=1회 회복, 0.6초 꾹 누름=풀회복 (HoldHealButton이 press/release 직접 처리)
			HoldHealButton healBtn = this.addRenderableWidget(new HoldHealButton(cx + 46, y + 26, 44, 18, petIndex));
			healButtons.add(healBtn);

			boolean equipped = PartnerData.isSaddled(pet) || !PartnerData.getArmorId(pet).isEmpty();
			final boolean doUnequip = equipped;
			this.addRenderableWidget(Button.builder(Component.literal(equipped ? "장비 해제" : "장비 장착"), b -> {
				ClientPlayNetworking.send(new ModPackets.EquipPetPayload(petIndex, doUnequip));
			}).bounds(cx + 94, y + 26, 56, 18).build());

			this.addRenderableWidget(Button.builder(Component.literal("소환"), b -> {
				ClientPlayNetworking.send(new ModPackets.SummonOnePayload(petIndex));
			}).bounds(cx + 154, y + 26, 36, 18).build());

			this.addRenderableWidget(Button.builder(Component.literal("전체보기"), b -> {
				ItemStack cap = findLiveCapsule();
				if (cap == null) {
					cap = capsuleSnapshot;
				}
				Minecraft.getInstance().gui.setScreen(new UltraCapsuleAdventureDetailScreen(cap, petIndex));
			}).bounds(cx - 170, y + 38, 48, 16).build());
		}

		int shownFrom = pets.isEmpty() ? 0 : from + 1;
		int shownTo = to;
		label(cx - 170, this.height - 48, 360,
				shownFrom + "-" + shownTo + " / 총 " + pets.size() + "마리 · 휠로 스크롤");

		int by = this.height - 30;
		if (mode == Mode.SOUL) {
			this.addRenderableWidget(Button.builder(Component.literal("모두 소환"), b -> {
				ClientPlayNetworking.send(new ModPackets.SoulSummonAllPayload());
			}).bounds(cx - 110, by, 80, 20).build());

			this.addRenderableWidget(Button.builder(Component.literal("불러내기"), b -> {
				ClientPlayNetworking.send(new ModPackets.SoulRecallPayload());
			}).bounds(cx - 25, by, 80, 20).build());

			this.addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose()).bounds(cx + 60, by, 60, 20).build());
		} else if (mode == Mode.VOID) {
			this.addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose()).bounds(cx - 30, by, 60, 20).build());
		} else {
			this.addRenderableWidget(Button.builder(Component.literal("모두 소환"), b -> {
				ClientPlayNetworking.send(new ModPackets.SummonAllPayload());
			}).bounds(cx - 70, by, 80, 20).build());

			this.addRenderableWidget(Button.builder(Component.literal("불러내기"), b -> {
				ClientPlayNetworking.send(new ModPackets.RecallPayload());
			}).bounds(cx + 15, by, 80, 20).build());

			this.addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose()).bounds(cx + 100, by, 60, 20).build());
		}

		lastSig = signature();
	}

	private int titanSel = -1;
	private int titanPage = 0;

	private void buildTitanPanel(int cx) {
		var player = Minecraft.getInstance().player;
		ItemStack cap = findLiveTitanCapsule();
		if (cap == null) {
			cap = capsuleSnapshot;
		}
		long now = System.currentTimeMillis();
		long cool = 0;
		boolean stored = false;
		boolean anvil = false;
		String labelName = "";
		long done = 0;
		try {
			cool = com.ultracapsuleadventure.titan.TitanData.getCooldownUntil(cap);
			done = com.ultracapsuleadventure.titan.TitanData.getDoneAt(cap);
			labelName = com.ultracapsuleadventure.titan.TitanData.getLabel(cap);
			stored = com.ultracapsuleadventure.titan.TitanData.hasStored(cap);
			anvil = com.ultracapsuleadventure.titan.TitanData.hasAnvil(cap);
		} catch (Exception ignored) {
		}
		// 후보: 닳은 장비/미충전 머리 + 수리 중인 것
		java.util.List<Integer> slots = new java.util.ArrayList<>();
		String jobName = "";
		long jobLeft = -1;
		int jobDmg = 0;
		try {
			if (player != null) {
				for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
					ItemStack s = player.getInventory().getItem(i);
					if (s == null || s.isEmpty()) {
						continue;
					}
					boolean repairing = false;
					try {
						repairing = com.ultracapsuleadventure.titan.TitanData.isRepairingItem(s);
					} catch (Exception ignored) {
					}
					if (repairing) {
						slots.add(0, i);
						try {
							jobName = s.getHoverName().getString();
							jobLeft = Math.max(0,
									(com.ultracapsuleadventure.titan.TitanData.getRepairDoneAt(s) - now) / 1000);
							jobDmg = s.getDamageValue();
						} catch (Exception ignored) {
						}
						continue;
					}
					boolean ok = false;
					try {
						ok = com.ultracapsuleadventure.titan.TitanData.isRepairableTarget(s);
					} catch (Exception ignored) {
					}
					if (ok) {
						slots.add(i);
					}
				}
			}
		} catch (Exception ignored) {
		}
		int y = START_Y;
		label(cx - 170, 38, 460, "재료·쿨타임·내구도 소모 없음 · 시간에 따라 복구");
		if (cool > now) {
			label(cx - 170, y, 340, "쿨타임 " + ((cool - now) / 1000) + "초 남음! (사용 불가)");
			y += 16;
		} else if (jobLeft >= 0) {
			label(cx - 170, y, 340, "수리 중: " + jobName + " (" + jobLeft + "초 남음 · 손상 " + jobDmg + ")");
			y += 16;
		} else if (stored) {
			label(cx - 170, y, 340, "수리 완료: " + labelName + " (꺼내세요!)");
			y += 16;
		} else if (anvil) {
			label(cx - 170, y, 340, "모루 수리 중... (" + Math.max(0, (done - now) / 1000) + "초 남음)");
			y += 16;
		}
		int perPage = 6;
		int pages = Math.max(1, (slots.size() + perPage - 1) / perPage);
		if (titanPage >= pages) {
			titanPage = pages - 1;
		}
		if (titanPage < 0) {
			titanPage = 0;
		}
		int from = titanPage * perPage;
		int to = Math.min(slots.size(), from + perPage);
		for (int r = from; r < to; r++) {
			int slot = slots.get(r);
			String text = "?";
			try {
				ItemStack s = player.getInventory().getItem(slot);
				String nm = s.getHoverName().getString();
				if (com.ultracapsuleadventure.titan.TitanData.isRepairingItem(s)) {
					text = nm + " · 수리 중";
				} else if (s.getItem() instanceof com.ultracapsuleadventure.medusa.MedusaHeadItem) {
					text = nm + " · 미충전";
				} else {
					text = nm + " · 손상 " + s.getDamageValue();
				}
				if (slot == titanSel) {
					text = "▶ " + text;
				}
			} catch (Exception ignored) {
			}
			final int pick = slot;
			final String msg = text;
			this.addRenderableWidget(Button.builder(Component.literal(msg), b -> {
				titanSel = pick;
				rebuildWidgets();
			}).bounds(cx - 170, y, 300, 20).build());
			y += 22;
		}
		if (pages > 1) {
			this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
				titanPage = Math.max(0, titanPage - 1);
				rebuildWidgets();
			}).bounds(cx - 170, y, 40, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
				titanPage = Math.min(pages - 1, titanPage + 1);
				rebuildWidgets();
			}).bounds(cx - 125, y, 40, 20).build());
			y += 22;
		}
		if (!(cool > now)) {
			this.addRenderableWidget(Button.builder(Component.literal("선택 장비 수리"), b -> {
				ClientPlayNetworking.send(new ModPackets.TitanRepairStartPayload(titanSel));
			}).bounds(cx - 170, y, 150, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal("수리 중단"), b -> {
				ClientPlayNetworking.send(new ModPackets.TitanRepairCancelPayload());
			}).bounds(cx - 15, y, 150, 20).build());
			y += 22;
			if (stored) {
				this.addRenderableWidget(Button.builder(Component.literal("꺼내기"), b -> {
					ClientPlayNetworking.send(new ModPackets.TitanWithdrawPayload());
				}).bounds(cx - 170, y, 100, 20).build());
			}
		}
		int by = this.height - 30;
		this.addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose()).bounds(cx - 30, by, 60, 20).build());
	}

	private int gambleSel = -1;
	private int gamblePage = 0;
	private int disSel = -1;
	private int disPage = 0;

	/** 은신 목록 행 (파트너/소울창과 같은 카드형): 미리보기+이름+변신/삭제 */
	private void buildStealthRows(int cx) {
		String curMid = "";
		try {
			var pl = Minecraft.getInstance().player;
			if (pl != null) {
				curMid = com.ultracapsuleadventure.morph.MorphEvents.morphId(pl);
			}
		} catch (Exception ignored) {
		}
		final String cur = curMid == null ? "" : curMid;
		// 상단 인간 복귀 버튼 (행 버튼과 분리)
		this.addRenderableWidget(Button.builder(
				Component.literal(cur.isEmpty() ? "★ 인간 모습 (복귀 불필요)" : "★ 인간으로 복귀 (변신 해제)"), b -> {
					ClientPlayNetworking.send(new ModPackets.StealthUnmorphPayload());
				}).bounds(cx - 170, START_Y, 340, 18).build());

		int vis = visibleRows();
		int from = Math.min(topIndex, maxTop());
		int to = Math.min(pets.size(), from + vis);
		if (pets.isEmpty()) {
			label(cx - 124, rowTop(), 300, "비어있음 — 황혼의 검으로 처치해 등록하세요.");
			label(cx - 124, rowTop() + 12, 300, "개수 무제한 · 같은 종류 중복 등록 없음");
		}
		for (int idx = from; idx < to; idx++) {
			final int rowIndex = idx;
			CompoundTag pet = pets.get(idx);
			String typeId = "";
			try {
				typeId = pet.getString("id").orElse("");
			} catch (Exception ignored) {
			}
			String shortType = typeId.contains(":") ? typeId.substring(typeId.indexOf(':') + 1) : typeId;
			int row = idx - from;
			int y = rowTop() + row * ROW_H;
			String marks = "";
			try {
				ItemStack h = findLiveStealthCapsule();
				if (h == null) {
					h = capsuleSnapshot;
				}
				if (idx == com.ultracapsuleadventure.morph.MorphData.getSelectedIndex(h)) {
					marks += " ◀선택";
				}
			} catch (Exception ignored) {
			}
			if (!typeId.isEmpty() && typeId.equals(cur)) {
				marks += " [변신중]";
			}
			Entity previewNow = getPreview(idx);
			String pvMark = previewNow == null ? " (외형없음)" : "";
			label(cx - 124, y, 300, (idx + 1) + ". " + shortType + marks + pvMark);
			label(cx - 124, y + 12, 300, "변신해도 이동속도는 그대로 · 같은 종은 선공 전까지 중립");
			this.addRenderableWidget(Button.builder(Component.literal("변신하기"), b -> {
				ClientPlayNetworking.send(new ModPackets.StealthMorphPayload(rowIndex));
			}).bounds(cx - 124, y + 26, 70, 18).build());
			this.addRenderableWidget(Button.builder(Component.literal("삭제"), b -> {
				ClientPlayNetworking.send(new ModPackets.StealthRemovePayload(rowIndex));
			}).bounds(cx - 50, y + 26, 52, 18).build());
		}

		int shownFrom = pets.isEmpty() ? 0 : from + 1;
		int shownTo = to;
		label(cx - 170, this.height - 48, 360,
				shownFrom + "-" + shownTo + " / 총 " + pets.size() + "종 · 휠로 스크롤");

		int by = this.height - 30;
		this.addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose()).bounds(cx - 30, by, 60, 20).build());
	}

	private java.util.List<Integer> listableSlots() {
		java.util.List<Integer> out = new java.util.ArrayList<>();
		try {
			var player = Minecraft.getInstance().player;
			if (player == null) {
				return out;
			}
			for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
				ItemStack s = player.getInventory().getItem(i);
				if (s == null || s.isEmpty()) {
					continue;
				}
				try {
					if (s.getItem() instanceof com.ultracapsuleadventure.item.UltraCapsuleAdventureItem
							|| s.getItem() instanceof com.ultracapsuleadventure.item.SoulCapsuleItem
							|| s.getItem() instanceof com.ultracapsuleadventure.item.GambleCapsuleItem
							|| s.getItem() instanceof com.ultracapsuleadventure.item.DisassembleCapsuleItem
							|| s.getItem() instanceof com.ultracapsuleadventure.titan.TitanCapsuleItem
							|| s.getItem() instanceof com.ultracapsuleadventure.mech.MechCapsuleItem
							|| s.getItem() instanceof com.ultracapsuleadventure.morph.StealthCapsuleItem) {
						continue;
					}
				} catch (Exception ignored) {
				}
				out.add(i);
			}
		} catch (Exception ignored) {
		}
		return out;
	}

	private void buildUtilityPanel(int cx) {
		var player = Minecraft.getInstance().player;
		int y = START_Y;
		if (mode == Mode.GAMBLE) {
			label(cx - 170, 38, 460, "10초 후 2배 or 소멸 · 50% · 목록에서 걸 아이템 선택");
			ItemStack g = findLiveGambleCapsule();
			if (g == null) {
				g = capsuleSnapshot;
			}
			boolean has = false;
			try {
				has = com.ultracapsuleadventure.item.GambleCapsuleItem.hasBet(g);
			} catch (Exception ignored) {
			}
			int y2 = START_Y;
			if (has) {
				String nm = "?";
				int count = 0;
				long until = 0;
				try {
					ItemStack bet = com.ultracapsuleadventure.item.GambleCapsuleItem.readBet(g, player.registryAccess());
					nm = bet.getHoverName().getString();
					count = bet.getCount();
					until = com.ultracapsuleadventure.item.GambleCapsuleItem.getUntil(g);
				} catch (Exception ignored) {
				}
				if (until > 0) {
					long left = Math.max(0, (until - System.currentTimeMillis() + 999) / 1000);
					label(cx - 170, y2, 340, "베팅: " + nm + " x" + count + " · 남은 " + left + "초...");
				} else {
					label(cx - 170, y2, 340, "베팅: " + nm + " x" + count + " · [도박하기] 가능");
				}
				y2 += 16;
			}
			java.util.List<Integer> slots = listableSlots();
			int perPage = 6;
			int pages = Math.max(1, (slots.size() + perPage - 1) / perPage);
			if (gamblePage >= pages) {
				gamblePage = pages - 1;
			}
			if (gamblePage < 0) {
				gamblePage = 0;
			}
			int from = gamblePage * perPage;
			int to = Math.min(slots.size(), from + perPage);
			for (int r = from; r < to; r++) {
				int slot = slots.get(r);
				String text = "?";
				try {
					ItemStack s = player.getInventory().getItem(slot);
					text = s.getHoverName().getString() + " x" + s.getCount();
					if (slot == gambleSel) {
						text = "▶ " + text;
					}
				} catch (Exception ignored) {
				}
				final int pick = slot;
				final String msg = text;
				this.addRenderableWidget(Button.builder(Component.literal(msg), b -> {
					gambleSel = pick;
					rebuildWidgets();
				}).bounds(cx - 170, y2, 300, 20).build());
				y2 += 22;
			}
			if (pages > 1) {
				this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
					gamblePage = Math.max(0, gamblePage - 1);
					rebuildWidgets();
				}).bounds(cx - 170, y2, 40, 20).build());
				this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
					gamblePage = Math.min(pages - 1, gamblePage + 1);
					rebuildWidgets();
				}).bounds(cx - 125, y2, 40, 20).build());
				y2 += 22;
			}
			this.addRenderableWidget(Button.builder(Component.literal("도박하기"), b -> {
				if (gambleSel >= 0) {
					ClientPlayNetworking.send(new ModPackets.GambleBetPayload(gambleSel));
				}
				ClientPlayNetworking.send(new ModPackets.GambleStartPayload());
			}).bounds(cx - 170, y2, 100, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal("빼기"), b -> {
				ClientPlayNetworking.send(new ModPackets.GambleWithdrawPayload());
			}).bounds(cx - 65, y2, 80, 20).build());
		} else if (mode == Mode.DIS) {
			label(cx - 170, 38, 460, "역조합 분해 · 목록에서 선택 후 장전");
			ItemStack d = findLiveDisCapsule();
			if (d == null) {
				d = capsuleSnapshot;
			}
			boolean has = false;
			try {
				has = com.ultracapsuleadventure.item.DisassembleCapsuleItem.hasStored(d);
			} catch (Exception ignored) {
			}
			int y3 = START_Y;
			if (has) {
				String nm = "?";
				String expect = "";
				try {
					ItemStack st = com.ultracapsuleadventure.item.DisassembleCapsuleItem.readStored(d, player.registryAccess());
					nm = st.getHoverName().getString();
					expect = com.ultracapsuleadventure.item.DisassembleCapsuleItem.getExpect(d);
				} catch (Exception ignored) {
				}
				label(cx - 170, y3, 340, "장전: " + nm);
				y3 += 14;
				label(cx - 170, y3, 340, expect == null ? "" : expect);
				y3 += 16;
			}
			java.util.List<Integer> slots2 = listableSlots();
			int perPage2 = 6;
			int pages2 = Math.max(1, (slots2.size() + perPage2 - 1) / perPage2);
			if (disPage >= pages2) {
				disPage = pages2 - 1;
			}
			if (disPage < 0) {
				disPage = 0;
			}
			int from2 = disPage * perPage2;
			int to2 = Math.min(slots2.size(), from2 + perPage2);
			for (int r = from2; r < to2; r++) {
				int slot = slots2.get(r);
				String text = "?";
				try {
					ItemStack s = player.getInventory().getItem(slot);
					text = s.getHoverName().getString() + " x" + s.getCount();
					if (slot == disSel) {
						text = "▶ " + text;
					}
				} catch (Exception ignored) {
				}
				final int pick = slot;
				final String msg = text;
				this.addRenderableWidget(Button.builder(Component.literal(msg), b -> {
					disSel = pick;
					rebuildWidgets();
				}).bounds(cx - 170, y3, 300, 20).build());
				y3 += 22;
			}
			if (pages2 > 1) {
				this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
					disPage = Math.max(0, disPage - 1);
					rebuildWidgets();
				}).bounds(cx - 170, y3, 40, 20).build());
				this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
					disPage = Math.min(pages2 - 1, disPage + 1);
					rebuildWidgets();
				}).bounds(cx - 125, y3, 40, 20).build());
				y3 += 22;
			}
			this.addRenderableWidget(Button.builder(Component.literal("장전하기"), b -> {
				ClientPlayNetworking.send(new ModPackets.DisStagePayload(disSel));
			}).bounds(cx - 170, y3, 80, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal("빼기"), b -> {
				ClientPlayNetworking.send(new ModPackets.DisWithdrawPayload());
			}).bounds(cx - 85, y3, 60, 20).build());
			this.addRenderableWidget(Button.builder(Component.literal("분해하기"), b -> {
				ClientPlayNetworking.send(new ModPackets.DisassemblePayload());
			}).bounds(cx - 20, y3, 70, 20).build());
		} else {
			label(cx - 170, 38, 460, "들고 우클릭 꾹(3초): 로봇 변신");
			label(cx - 170, y, 340, "탑승 후 좌클릭: 레이저 / 스페이스: 점프");
			y += 14;
			label(cx - 170, y, 340, "탑승 중 좌클릭 꾹(3초): 캡슐 복귀 (변신과 동일)");
			y += 14;
			label(cx - 170, y, 340, "쉬프트 하차 후 웅크리기 우클릭 꾹: 캡슐 복귀");
		}
		int by = this.height - 30;
		this.addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose()).bounds(cx - 30, by, 60, 20).build());
	}

	@Override
	public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
		if (pets.size() <= visibleRows()) {
			return false;
		}
		// 페이지 단위가 아니라 휠 한 칸에 한 줄씩 이어서 스크롤 (목록 전체가 한 화면에 쭉 이어짐)
		int next = topIndex + (scrollY < 0 ? 1 : -1);
		next = Math.max(0, Math.min(maxTop(), next));
		if (next != topIndex) {
			topIndex = next;
			rebuildWidgets();
			return true;
		}
		return false;
	}

	@Override
	public void tick() {
		super.tick();
		long now = System.currentTimeMillis();
		for (HoldHealButton b : healButtons) {
			try {
				if (b.pressStart >= 0 && !b.morphed && now - b.pressStart >= HoldHealButton.HOLD_MS) {
					b.morphed = true;
					b.setMessage(Component.literal("풀회복!"));
				}
			} catch (Exception ignored) {
			}
		}
		if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.tickCount % 20 == 0) {
			refreshPets();
			String sig = signature();
			if (!sig.equals(lastSig)) {
				previews.clear();
				rebuildWidgets();
			}
		}
	}

	private Entity getPreview(int index) {
		Entity cached = previews.get(index);
		if (cached != null && cached.isAlive()) {
			return cached;
		}
		if (index < 0 || index >= pets.size()) {
			return null;
		}
		Level level = Minecraft.getInstance().level;
		Entity created = EntityPreview.load(level, pets.get(index));
		if (created != null) {
			previews.put(index, created);
		}
		return created;
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		try {
			super.extractBackground(g, mouseX, mouseY, partialTick);
		} catch (Exception ignored) {
		}
		try {
			drawPanels(g);
		} catch (Exception ignored) {
		}
		// 각 행 좌측에 3D 외형 미리보기 (바닐라 대장간과 동일하게 배경 단계에서 제출)
		try {
			int cx = this.width / 2;
			int vis = visibleRows();
			int from = Math.min(topIndex, maxTop());
			int to = Math.min(pets.size(), from + vis);
			Minecraft mc = Minecraft.getInstance();
			if (mc.level == null) {
				return;
			}
			for (int idx = from; idx < to; idx++) {
				int row = idx - from;
				int y = rowTop() + row * ROW_H;
				int rx = cx - 170;
				int ry = y + 2;
				try {
					Entity preview = getPreview(idx);
					if (preview != null) {
						float maxDim = Math.max(0.6f, Math.max(preview.getBbWidth(), preview.getBbHeight()));
						float scalePx = Math.min(30f, 30f / maxDim);
						EntityPreview.submit(g, preview, scalePx, rx, ry, 44, 58, 0f);
					}
				} catch (Exception ignored) {
					// 개별 미리보기 실패는 무시 (목록은 정상 표시)
				}
			}
		} catch (Exception ignored) {
		}
	}

	/** 모드별 포인트 색 (파트너·영혼·공허·타이탄·도박·분해·메카·은신) */
	private static final int[] ACCENT = {0xFFD8A63A, 0xFF8A5CF0, 0xFF5B66E0, 0xFFE0792A, 0xFF3FB36B, 0xFFD2483F, 0xFF3FB8D0, 0xFF4FD8C8};

	/** 관리창 장식: 어두운 패널, 모드색 머리띠, 카드형 행, 체력 게이지, 하단 바 */
	private void drawPanels(GuiGraphicsExtractor g) {
		int cx = this.width / 2;
		int accent = ACCENT[mode.ordinal()];
		int pl = Math.max(2, cx - 186);
		int pr = Math.min(this.width - 2, cx + 206);
		int bottom = this.height - 2;

		// 바깥 패널 + 테두리
		GuiDraw.fill(g, pl, 2, pr, bottom, 0xCC0E0E16);
		GuiDraw.border(g, pl, 2, pr, bottom, 0xFF2C2C3C);
		// 머리띠 + 모드색 라인
		GuiDraw.fill(g, pl + 1, 3, pr - 1, 36, 0xE0181826);
		GuiDraw.fill(g, pl + 1, 36, pr - 1, 37, accent);
		// 모드 표시 점 (현재 모드만 밝게)
		int dots = Mode.values().length;
		for (int i = 0; i < dots; i++) {
			int dx = cx + 60 + i * 16;
			int c = (i == mode.ordinal()) ? ACCENT[i] : 0xFF3A3A4C;
			GuiDraw.fill(g, dx, 33, dx + 12, 35, c);
		}
		// 하단 바
		GuiDraw.fill(g, pl + 1, this.height - 54, pr - 1, bottom - 1, 0xE0181826);
		GuiDraw.fill(g, pl + 1, this.height - 54, pr - 1, this.height - 53, 0xFF2C2C3C);

		if (mode == Mode.TITAN || mode == Mode.GAMBLE || mode == Mode.DIS || mode == Mode.MECH) {
			return;
		}
		// 행 카드
		int vis = visibleRows();
		int from = Math.min(topIndex, maxTop());
		int to = Math.min(pets.size(), from + vis);
		for (int idx = from; idx < to; idx++) {
			int y = rowTop() + (idx - from) * ROW_H;
			int cl = cx - 178;
			int cr = cx + 198;
			int ct = y - 1;
			int cb = y + ROW_H - 4;
			GuiDraw.fill(g, cl, ct, cr, cb, 0xB01E1E2E);
			GuiDraw.border(g, cl, ct, cr, cb, 0xFF34344A);
			GuiDraw.fill(g, cl, ct, cl + 3, cb, accent);
			// 미리보기 받침
			GuiDraw.fill(g, cx - 172, y + 1, cx - 126, y + 61, 0xFF0C0C14);
			GuiDraw.border(g, cx - 172, y + 1, cx - 126, y + 61, 0xFF2A2A3C);
			// 체력 게이지 (영혼·은신 모드는 체력 개념 없음)
			if (mode != Mode.SOUL && mode != Mode.STEALTH && idx < pets.size()) {
				CompoundTag pet = pets.get(idx);
				float hp = PartnerData.getHealth(pet);
				float max = PartnerData.getMaxHealth(pet);
				float frac = max <= 0 ? 0f : Math.max(0f, Math.min(1f, hp / max));
				int bx1 = cx - 124;
				int bx2 = cx + 190;
				int by1 = y + 48;
				GuiDraw.fill(g, bx1, by1, bx2, by1 + 7, 0xFF0A0A12);
				int fillW = Math.round((bx2 - bx1 - 2) * frac);
				int hc = frac > 0.5f ? 0xFF3FBF5F : (frac > 0.25f ? 0xFFE0B83A : 0xFFD64545);
				if (mode == Mode.VOID) {
					hc = 0xFF6A6FE0; // 공허: 동결 상태
				}
				GuiDraw.fill(g, bx1 + 1, by1 + 1, bx1 + 1 + fillW, by1 + 6, hc);
				GuiDraw.fill(g, bx1 + 1, by1 + 1, bx1 + 1 + fillW, by1 + 2, 0x40FFFFFF);
				GuiDraw.border(g, bx1, by1, bx2, by1 + 7, 0xFF2A2A3C);
			}
		}
		// 스크롤바: 목록이 한 화면보다 길 때만 표시 (현재 위치 / 전체 길이)
		if (pets.size() > vis) {
			int sbx = pr - 6;
			int sbTop = rowTop();
			int sbBot = this.height - 56;
			int trackH = Math.max(8, sbBot - sbTop);
			GuiDraw.fill(g, sbx, sbTop, sbx + 3, sbTop + trackH, 0xFF0A0A12);
			int thumbH = Math.max(10, Math.round(trackH * (vis / (float) pets.size())));
			int maxT = Math.max(1, maxTop());
			int thumbY = sbTop + Math.round((trackH - thumbH) * (Math.min(topIndex, maxTop()) / (float) maxT));
			GuiDraw.fill(g, sbx, thumbY, sbx + 3, thumbY + thumbH, accent);
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}



