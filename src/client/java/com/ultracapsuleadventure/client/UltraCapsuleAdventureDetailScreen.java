package com.ultracapsuleadventure.client;

import java.util.List;

import com.ultracapsuleadventure.data.PartnerData;
import com.ultracapsuleadventure.item.UltraCapsuleAdventureItem;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

/**
 * 전체보기: 선택 펫 1마리의 외형만 크게 표시. 자동 360도 회전 + ◀▶ 수동 회전.
 */
public class UltraCapsuleAdventureDetailScreen extends Screen {

	private final ItemStack capsule;
	private final int index;
	private Entity preview;
	private float yaw = 0f;
	private boolean autoSpin = true;

	public UltraCapsuleAdventureDetailScreen(ItemStack capsule, int index) {
		super(Component.literal("파트너 전체보기"));
		this.capsule = capsule.copy();
		this.index = index;
	}

	private CompoundTag petTag() {
		ItemStack live = null;
		var player = Minecraft.getInstance().player;
		if (player != null) {
			if (player.getMainHandItem().getItem() instanceof UltraCapsuleAdventureItem) {
				live = player.getMainHandItem();
			} else if (player.getOffhandItem().getItem() instanceof UltraCapsuleAdventureItem) {
				live = player.getOffhandItem();
			} else {
				for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
					ItemStack s = player.getInventory().getItem(i);
					if (s.getItem() instanceof UltraCapsuleAdventureItem) {
						live = s;
						break;
					}
				}
			}
		}
		if (live == null) {
			live = capsule;
		}
		List<CompoundTag> pets = PartnerData.getPets(live);
		if (index < 0 || index >= pets.size()) {
			return null;
		}
		return pets.get(index);
	}

	private void label(int x, int y, int w, String text) {
		this.addRenderableWidget(new StringWidget(x, y, w, 12, Component.literal(text), this.font));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		CompoundTag pet = petTag();
		if (pet == null) {
			label(cx - 150, 20, 300, "펫이 없습니다 (소환됨). 닫기를 눌러주세요.");
		} else {
			String typeId = PartnerData.getTypeId(pet);
			String shortType = typeId.contains(":") ? typeId.substring(typeId.indexOf(':') + 1) : typeId;
			String name = pet.getString(PartnerData.PC_NAME).orElse("");
			String gear = "";
			if (PartnerData.isSaddled(pet)) {
				gear += " [안장]";
			}
			if (!PartnerData.getArmorId(pet).isEmpty()) {
				gear += " [갑옷]";
			}
			label(cx - 150, 10, 340, (index + 1) + ". " + shortType + (name.isEmpty() ? "" : " \"" + name + "\"") + gear);
			label(cx - 150, 24, 340,
					String.format("체력 %.1f / %.1f", PartnerData.getHealth(pet), PartnerData.getMaxHealth(pet)));
			Level level = Minecraft.getInstance().level;
			preview = EntityPreview.load(level, pet);
			if (preview == null) {
				label(cx - 150, 40, 300, "(외형없음)");
			}
		}

		int by = this.height - 30;
		this.addRenderableWidget(Button.builder(Component.literal("◀"), b -> {
			yaw -= 0.5f;
		}).bounds(cx - 140, by, 36, 20).build());

		this.addRenderableWidget(Button.builder(
				Component.literal(autoSpin ? "자동회전: ON" : "자동회전: OFF"), b -> {
					autoSpin = !autoSpin;
					rebuildWidgets();
				}).bounds(cx - 100, by, 110, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("▶"), b -> {
			yaw += 0.5f;
		}).bounds(cx + 15, by, 36, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("닫기"), b -> {
			Minecraft.getInstance().gui.setScreen(new UltraCapsuleAdventureScreen(capsule));
		}).bounds(cx + 60, by, 70, 20).build());
	}

	@Override
	public void tick() {
		super.tick();
		if (autoSpin) {
			yaw += 0.04f;
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		if (preview == null) {
			return;
		}
		try {
			int cx = this.width / 2;
			int size = Math.max(120, Math.min(this.width - 40, this.height - 150));
			int rx = cx - size / 2;
			int ry = 44;
			float maxDim = Math.max(0.6f, Math.max(preview.getBbWidth(), preview.getBbHeight()));
			float scalePx = (size * 0.72f) / maxDim;
			EntityPreview.submit(g, preview, scalePx, rx, ry, size, size, yaw);
		} catch (Exception ignored) {
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}



