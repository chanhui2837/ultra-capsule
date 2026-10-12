package com.partnercap.client;

import com.partnercap.item.GambleCapsuleItem;
import com.partnercap.network.ModPackets;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * 도박 캡슐 창: 아이템 넣기/빼기 + 10초 도박.
 */
public class GambleCapScreen extends Screen {

	private final boolean mainHand;
	private ItemStack capView = ItemStack.EMPTY;
	private ItemStack betView = ItemStack.EMPTY;
	private long untilView = 0L;

	public GambleCapScreen(boolean mainHand) {
		super(Component.literal("도박 캡슐"));
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
			if (main.getItem() instanceof GambleCapsuleItem) {
				return main;
			}
			if (off.getItem() instanceof GambleCapsuleItem) {
				return off;
			}
		} else {
			if (off.getItem() instanceof GambleCapsuleItem) {
				return off;
			}
			if (main.getItem() instanceof GambleCapsuleItem) {
				return main;
			}
		}
		for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
			ItemStack s = player.getInventory().getItem(i);
			if (s.getItem() instanceof GambleCapsuleItem) {
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
		label(cx - 150, 10, 300, "도박 캡슐 (10초 후 배율 or 소멸 · 인챈트 연동)");
		capView = ItemStack.EMPTY;
		betView = ItemStack.EMPTY;
		untilView = 0L;
		try {
			ItemStack live = findLive();
			if (live != null) {
				capView = live;
				var player = Minecraft.getInstance().player;
				if (player != null) {
					betView = GambleCapsuleItem.readBet(live, player.registryAccess());
					untilView = GambleCapsuleItem.getUntil(live);
				}
			}
		} catch (Exception ignored) {
		}
		boolean hasBet = betView != null && !betView.isEmpty();
		boolean active = hasBet && untilView > 0;
		if (!hasBet) {
			label(cx - 150, 30, 320, "비어있음: 다른 손에 아이템 들고 [넣기]");
		} else {
			String nm = betView.getHoverName().getString();
			if (active) {
				long left = Math.max(0, (untilView - System.currentTimeMillis() + 999) / 1000);
				label(cx - 100, 30, 320, "베팅: " + nm + " x" + betView.getCount() + " · 남은 " + left + "초...");
			} else {
				label(cx - 100, 30, 320, "베팅: " + nm + " x" + betView.getCount() + " · [도박하기] 가능");
			}
		}

		int by = this.height - 34;
		this.addRenderableWidget(Button.builder(Component.literal("넣기"), b -> {
			ClientPlayNetworking.send(new ModPackets.GambleInsertPayload(mainHand));
		}).bounds(cx - 160, by, 60, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("빼기"), b -> {
			ClientPlayNetworking.send(new ModPackets.GambleWithdrawPayload());
		}).bounds(cx - 95, by, 60, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("도박하기"), b -> {
			ClientPlayNetworking.send(new ModPackets.GambleStartPayload());
		}).bounds(cx - 30, by, 70, 20).build());

		this.addRenderableWidget(Button.builder(Component.literal("닫기"), b -> this.onClose()).bounds(cx + 45, by, 55, 20).build());
	}

	@Override
	public void tick() {
		super.tick();
		if (Minecraft.getInstance().player != null && Minecraft.getInstance().player.tickCount % 10 == 0) {
			rebuildWidgets();
		}
	}

	@Override
	public void extractBackground(GuiGraphicsExtractor g, int mouseX, int mouseY, float partialTick) {
		try {
			if (betView != null && !betView.isEmpty()) {
				int cx = this.width / 2;
				g.item(betView, cx - 8, 46);
			}
		} catch (Exception ignored) {
		}
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}
}
