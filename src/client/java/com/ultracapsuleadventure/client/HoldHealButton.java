package com.ultracapsuleadventure.client;

import com.ultracapsuleadventure.network.ModPackets;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/**
 * 회복 버튼: 짧게 클릭=1회 회복, 0.6초 꾹 누름=풀회복 (누르는 동안 라벨이 풀회복!으로 변함).
 */
public class HoldHealButton extends Button {

	public final int petIndex;
	public long pressStart = -1;
	public boolean morphed = false;
	public static final long HOLD_MS = 600;

	public HoldHealButton(int x, int y, int w, int h, int petIndex) {
		super(x, y, w, h, Component.literal("회복"), b -> {
		}, Button.DEFAULT_NARRATION);
		this.petIndex = petIndex;
	}

	@Override
	public void extractContents(GuiGraphicsExtractor extractor, int i, int j, float f) {
		this.extractDefaultSprite(extractor);
		this.extractDefaultLabel(extractor.textRendererForWidget(this, GuiGraphicsExtractor.HoveredTextEffects.NONE));
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClicked) {
		boolean r = super.mouseClicked(event, doubleClicked);
		if (r && isActive()) {
			pressStart = System.currentTimeMillis();
			morphed = false;
		}
		return r;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		boolean r = super.mouseReleased(event);
		if (pressStart >= 0) {
			boolean full = morphed || (System.currentTimeMillis() - pressStart >= HOLD_MS);
			ClientPlayNetworking.send(new ModPackets.HealPetPayload(petIndex, full));
			try {
				setMessage(Component.literal("회복"));
			} catch (Exception ignored) {
			}
			pressStart = -1;
			morphed = false;
		}
		return r;
	}
}


