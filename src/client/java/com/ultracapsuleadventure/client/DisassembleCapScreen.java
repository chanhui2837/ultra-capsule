package com.ultracapsuleadventure.client;

import java.util.List;

import com.ultracapsuleadventure.item.DisassembleCapsuleItem;
import com.ultracapsuleadventure.network.ModPackets;
import com.ultracapsuleadventure.util.GambleLogic;
import com.ultracapsuleadventure.util.ItemNbt;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/**
 * 분해 캡슐 창: 아이템 넣기/빼기 + 제작법 역분해.
 */
public class DisassembleCapScreen extends Screen {

	private final boolean mainHand;
	private ItemStack storedView = ItemStack.EMPTY;
	private String expectView = "";

	public DisassembleCapScreen(boolean mainHand) {
		super(Component.literal("분해 캡슐"));
		this.mainHand = mainHand;
	}

	private ItemStack findLive() {
		var player = Minecraft.getInstance().player;
		if (player == null) {
			return null;
		}
		ItemStack main = player.getMainHandItem();
		ItemStack off = player.getOffhandItem();
		if (mainHand) {
			if (main.getItem() instanceof DisassembleCapsuleItem) {
				return main;
			}
			if (off.getItem() instanceof DisassembleCapsuleItem) {
				return off;
			}
		} else {
			if (off.getItem() instanceof DisassembleCapsuleItem) {
				return off;
			}
			if (main.getItem() instanceof DisassembleCapsuleItem) {
				return main;
			}
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() instanceof DisassembleCapsuleItem) {
				return s;
			}
		}
		return null;
	}

	private void label(int x, int y, int w, String text) {
		this.addRenderableWidget(new StringWidget(x, y, w, 12, Component.literal(text), this.font));
	}

	@Override
	protected void init() {
		int cx = this.width / 2;
		label(cx - 150, 10, 300, "분해 캡슐 (제작대의 반대)");
		storedView = ItemStack.EMPTY;
		expectView = "";
		try {
			ItemStack live = findLive();
			var player = Minecraft.getInstance().player;
			if (live != null && player != null) {
				storedView = DisassembleCapsuleItem.readStored(live, player.registryAccess());
			}
			if (storedView != null && !storedView.isEmpty()) {
				expectView = com.ultracapsuleadventure.item.DisassembleCapsuleItem.getExpect(live);
				if (expectView == null || expectView.isEmpty()) {
					expectView = "분해 준비 중...";
				}
			} else {
				expectView = "비어있음: 다른 손 아이템 + [넣기]";
			}
		} catch (Exception ignored) {
		}
		label(cx - 100, 30, 340, expectView);

		int by = this.height - 34;
		this.addRenderableWidget(Button.builder(Component.literal("넣기"), b -> {
			ClientPlayNetworking.send(new ModPackets.DisInsertPayload(mainHand));
		}).bounds(cx - 160, by, 60, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("빼기"), b -> {
			ClientPlayNetworking.send(new ModPackets.DisWithdrawPayload());
		}).bounds(cx - 95, by, 60, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("분해하기"), b -> {
			ClientPlayNetworking.send(new ModPackets.DisassemblePayload());
		}).bounds(cx - 30, by, 70, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose()).bounds(cx + 45, by, 55, 20).build());
	}

	@Override
	public void tick() {
		super.tick();
		if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.tickCount % 20 == 0) {
			rebuildWidgets();
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		try {
			if (storedView != null && !storedView.isEmpty()) {
				int cx = this.width / 2;
				g.item(storedView, cx - 8, 46);
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}


