package com.top1.client.mixin;

import com.top1.client.SongIslandClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.BossHealthOverlay;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BossHealthOverlay.class)
public class BossHealthOverlayMixin {
	@Inject(method = "render", at = @At("HEAD"), cancellable = true)
	private void songIsland$hideWhenPlaying(GuiGraphics graphics, CallbackInfo ci) {
		if(SongIslandClient.island() != null && SongIslandClient.island().shouldHideBossbar()) ci.cancel();
	}
}
