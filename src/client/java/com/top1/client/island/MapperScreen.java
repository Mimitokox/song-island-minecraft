package com.top1.client.island;

import com.top1.client.island.font.Font;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.render.IslandRender;
import com.top1.client.music.LocalLyrics;
import com.top1.client.music.LyricsFetcher;
import com.top1.client.music.MusicTracker;
import dev.redstones.mediaplayerinfo.IMediaSession;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class MapperScreen extends Screen {
	private static final int WHITE = 0xFFFFFFFF;
	private static final float LINE_STEP = 13.0F;

	private final MusicTracker tracker;
	private final List<String> texts;
	private final List<Float> stamps = new ArrayList<>();
	private final String artist;
	private final String title;
	private static final float REACTION = 0.18F;
	private final Anim scroll = new Anim(220L);
	private int index;
	private String message = "";
	private long messageAt;

	public MapperScreen(MusicTracker tracker, String artist, String title, List<String> texts) {
		super(Component.literal("Manual Map Music - Song Island v" + com.top1.client.VersionCheck.VERSION));
		this.tracker = tracker;
		this.artist = artist;
		this.title = title;
		this.texts = texts;
	}

	public static MapperScreen create(MusicTracker tracker) {
		IMediaSession session = tracker.getSession();
		if(session == null) return null;
		List<String> texts = LocalLyrics.plainLines(tracker.getRawLyrics());
		if(texts.isEmpty()) return null;
		return new MapperScreen(tracker, session.getMedia().getArtist(), session.getMedia().getTitle(), texts);
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
		renderTransparentBackground(graphics);

		float width = this.width;
		float centerX = width / 2.0F;
		float centerY = this.height / 2.0F;
		float position = tracker.rawPosition();

		Font header = Fonts.medium(9.0F);
		Font small = Fonts.regular(7.0F);
		Font active = Fonts.medium(9.5F);
		Font idle = Fonts.regular(8.5F);

		IslandRender.drawCenteredText(header, title + "  |  " + artist, centerX, 22.0F, WHITE);
		IslandRender.drawCenteredText(small, MusicTracker.formatTime((long) position)
			+ "   mapped " + index + " / " + texts.size(),
			centerX, 36.0F, 0x99FFFFFF);
		IslandRender.drawCenteredText(small, index < texts.size()
			? "hit SPACE when the highlighted line STARTS"
			: "all lines mapped - ENTER or ESC to save",
			centerX, 48.0F, 0x88FFE38F);

		scroll.update(Math.max(0, index - 1) * LINE_STEP);
		float offset = scroll.get();
		float listTop = centerY - 40.0F;

		for(int i = 0; i < texts.size(); i++){
			float y = listTop + i * LINE_STEP - offset;
			if(y < 50.0F || y > this.height - 60.0F) continue;
			boolean playingNow = i == index - 1;
			boolean waiting = i == index;
			int color = waiting ? 0xFFE3D18F
				: playingNow ? WHITE
				: i < index ? 0x8899FF99 : 0x55FFFFFF;
			Font font = playingNow || waiting ? active : idle;
			String text = texts.get(i);
			if(i < index && i < stamps.size()){
				text = MusicTracker.formatTime(stamps.get(i).longValue()) + "  " + text;
			}
			IslandRender.drawCenteredText(font, text, centerX, y, color);
		}

		Font hint = Fonts.regular(7.5F);
		float hintY = this.height - 44.0F;
		IslandRender.drawCenteredText(hint, "SPACE - this line starts now     BACKSPACE - undo", centerX, hintY, 0xAAFFFFFF);
		IslandRender.drawCenteredText(hint, "ENTER - save     ESC - save and close", centerX, hintY + 12.0F, 0xAAFFFFFF);
		IslandRender.drawCenteredText(hint, "DELETE - throw this mapping away and get the full lyrics back",
			centerX, hintY + 24.0F, 0x77FFFFFF);

		if(!message.isEmpty() && System.currentTimeMillis() - messageAt < 3000L){
			IslandRender.drawCenteredText(Fonts.medium(8.0F), message, centerX, hintY - 18.0F, 0xFF9BE38F);
		}
	}

	@Override
	public boolean keyPressed(KeyEvent event) {
		int key = event.key();
		if(key == GLFW.GLFW_KEY_SPACE){
			if(index < texts.size()){
				stamps.add(Math.max(0.0F, tracker.rawPosition() - REACTION));
				index++;
				if(index >= texts.size()) saveAndReport();
			}
			return true;
		}
		if(key == GLFW.GLFW_KEY_BACKSPACE){
			if(index > 0){
				index--;
				if(index < stamps.size()) stamps.remove(stamps.size() - 1);
			}
			return true;
		}
		if(key == GLFW.GLFW_KEY_DELETE){
			tracker.forgetMapping();
			stamps.clear();
			index = 0;
			message = "mapping removed - original lyrics restored";
			messageAt = System.currentTimeMillis();
			return true;
		}
		if(key == GLFW.GLFW_KEY_ENTER || key == GLFW.GLFW_KEY_KP_ENTER){
			saveAndReport();
			return true;
		}
		return super.keyPressed(event);
	}

	@Override
	public void onClose() {
		if(!stamps.isEmpty()) save();
		tracker.reloadLocal();
		Minecraft.getInstance().setScreen(null);
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private void saveAndReport() {
		message = save() ? "saved " + stamps.size() + " lines" : "nothing to save yet";
		messageAt = System.currentTimeMillis();
	}

	private boolean save() {
		if(stamps.isEmpty()) return false;
		List<LyricsFetcher.LyricLine> lines = new ArrayList<>();
		for(int i = 0; i < stamps.size() && i < texts.size(); i++){
			lines.add(new LyricsFetcher.LyricLine(stamps.get(i), texts.get(i)));
		}
		return LocalLyrics.save(artist, title, lines);
	}
}
