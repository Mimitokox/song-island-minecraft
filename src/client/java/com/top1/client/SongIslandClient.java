package com.top1.client;

import com.top1.client.island.MusicIsland;
import com.top1.client.island.WorldLyrics;
import com.top1.client.island.font.Fonts;
import com.top1.client.island.render.IslandRender;
import com.top1.client.music.MusicTracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;

@Environment(EnvType.CLIENT)
public class SongIslandClient implements ClientModInitializer {
	private static MusicIsland island;
	private static MusicTracker tracker;
	private static boolean fontsReady;
	private static boolean loggedFonts;
	private static boolean loggedRender;

	public static MusicIsland island() {
		return island;
	}

	@Override
	public void onInitializeClient() {
		com.top1.client.island.IslandSettings.load();
		VersionCheck.check();
		ClientPlayConnectionEvents.JOIN.register((handler, sender, client) ->
			client.execute(VersionCheck::notifyPlayer));
		tracker = new MusicTracker();
		island = new MusicIsland(tracker);
		ClientLifecycleEvents.CLIENT_STOPPING.register(client -> {
			if(tracker != null) tracker.shutdown();
			forceExit();
		});
		WorldRenderEvents.END_MAIN.register(context -> {
			if(island != null && island.isWorldTextEnabled()) WorldLyrics.capture();
		});
	}

	private interface Kernel32 extends com.sun.jna.Library {
		Kernel32 INSTANCE = com.sun.jna.Native.load("kernel32", Kernel32.class);

		com.sun.jna.Pointer GetCurrentProcess();

		boolean TerminateProcess(com.sun.jna.Pointer handle, int code);
	}

	private static void forceExit(){
		Runtime.getRuntime().addShutdownHook(new Thread(SongIslandClient::kill, "song-island-hook"));

		Thread killer = new Thread(() -> {
			try{
				Thread.sleep(2500L);
			}catch(InterruptedException e){
			}
			kill();
		}, "song-island-exit");
		killer.setDaemon(false);
		killer.start();
	}

	private static void kill(){
		try{
			Kernel32.INSTANCE.TerminateProcess(Kernel32.INSTANCE.GetCurrentProcess(), 0);
		}catch(Throwable e){
			Runtime.getRuntime().halt(0);
		}
	}

	public static void renderIsland() {
		if(island == null) return;
		if(!fontsReady){
			try{
				Fonts.init();
				fontsReady = true;
			}catch(Throwable e){
				if(!loggedFonts){
					loggedFonts = true;
					org.slf4j.LoggerFactory.getLogger("song-island").error("font init failed", e);
				}
				return;
			}
		}
		try{
			if(island.isWorldTextEnabled()) WorldLyrics.render(tracker);
			island.render();
			IslandRender.flush();
		}catch(Throwable e){
			if(!loggedRender){
				loggedRender = true;
				org.slf4j.LoggerFactory.getLogger("song-island").error("island tick failed", e);
			}
		}
	}
}
