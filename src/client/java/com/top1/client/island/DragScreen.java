package com.top1.client.island;

import com.top1.client.island.font.Font;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.render.IslandRender;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

public class DragScreen extends Screen {
	private static final int WHITE = 0xFFFFFFFF;
	private static final float BUTTON_WIDTH = 74.0F;
	private static final float BUTTON_HEIGHT = 18.0F;

	private final MusicIsland island;
	private final float startX;
	private final float startY;
	private final boolean hadPos;
	private boolean grabbing;
	private float offX;
	private float offY;

	public DragScreen(MusicIsland island) {
		super(Component.literal("Move the island"));
		this.island = island;
		this.startX = IslandSettings.posX;
		this.startY = IslandSettings.posY;
		this.hadPos = IslandSettings.isMoved();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		graphics.fill(0, 0, this.width, this.height, 0x99101010);

		Font title = Fonts.medium(8.0F);
		Font hint = Fonts.regular(6.5F);
		IslandRender.drawCenteredText(title, "Drag the island where you want it",
			this.width / 2.0F, this.height / 2.0F - 14.0F, 0xCCFFFFFF);
		IslandRender.drawCenteredText(hint, "hold left mouse on it to move   -   ESC cancels",
			this.width / 2.0F, this.height / 2.0F - 2.0F, 0x77FFFFFF);

		float buttonX = this.width / 2.0F - BUTTON_WIDTH / 2.0F;
		float buttonY = this.height - 40.0F;
		boolean hovered = hovered(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY);
		IslandRender.drawSquircle(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, 4.0F,
			8.0F, 8.0F, 8.0F, 8.0F, hovered ? 0xF2FFFFFF : 0xD9E8E8E8);
		Font label = Fonts.medium(7.5F);
		IslandRender.drawCenteredText(label, "SAVE POSITION",
			buttonX + BUTTON_WIDTH / 2.0F, buttonY + 6.0F, 0xFF101010);
	}

	@Override
	public boolean mouseClicked(MouseButtonEvent event, boolean doubleClick) {
		if(event.button() != 0) return true;
		double mouseX = event.x();
		double mouseY = event.y();

		float buttonX = this.width / 2.0F - BUTTON_WIDTH / 2.0F;
		float buttonY = this.height - 40.0F;
		if(hovered(buttonX, buttonY, BUTTON_WIDTH, BUTTON_HEIGHT, mouseX, mouseY)){
			IslandSettings.save();
			onSaved();
			return true;
		}

		if(island.containsPoint(mouseX, mouseY)){
			grabbing = true;
			offX = island.centerX() - (float) mouseX;
			offY = island.topY() - (float) mouseY;
		}
		return true;
	}

	@Override
	public boolean mouseDragged(MouseButtonEvent event, double dragX, double dragY) {
		if(!grabbing) return true;
		moveTo(event.x() + offX, event.y() + offY);
		return true;
	}

	@Override
	public boolean mouseReleased(MouseButtonEvent event) {
		grabbing = false;
		return true;
	}

	private void moveTo(double centerX, double topY) {
		float half = island.width() / 2.0F;
		float height = island.height();
		IslandSettings.posX = (float) Math.max(half + 2.0, Math.min(this.width - half - 2.0, centerX));
		IslandSettings.posY = (float) Math.max(2.0, Math.min(this.height - height - 2.0, topY));
	}

	@Override
	public void onClose() {
		IslandSettings.posX = hadPos ? startX : -1.0F;
		IslandSettings.posY = hadPos ? startY : -1.0F;
		Minecraft.getInstance().setScreen(null);
	}

	private void onSaved() {
		Minecraft.getInstance().setScreen(null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private static boolean hovered(float x, float y, float w, float h, double mx, double my) {
		return mx >= x && mx <= x + w && my >= y && my <= y + h;
	}
}
