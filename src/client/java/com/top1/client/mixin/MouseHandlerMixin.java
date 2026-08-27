package com.top1.client.mixin;

import com.top1.client.SongIslandClient;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.input.MouseButtonInfo;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MouseHandler.class)
public class MouseHandlerMixin {
	@Inject(method = "onButton(JLnet/minecraft/client/input/MouseButtonInfo;I)V", at = @At("HEAD"), cancellable = true)
	private void songIsland$onButton(long window, MouseButtonInfo info, int action, CallbackInfo ci) {
		if(action == 1 && SongIslandClient.island() != null){
			MouseHandler self = (MouseHandler) (Object) this;
			var mc = net.minecraft.client.Minecraft.getInstance();
			double x = self.getScaledXPos(mc.getWindow());
			double y = self.getScaledYPos(mc.getWindow());
			if(SongIslandClient.island().handleClick(x, y, info.button())) ci.cancel();
		}
	}

	@Inject(method = "onScroll", at = @At("HEAD"), cancellable = true)
	private void songIsland$onScroll(long window, double horizontal, double vertical, CallbackInfo ci) {
		if(SongIslandClient.island() != null){
			MouseHandler self = (MouseHandler) (Object) this;
			var mc = net.minecraft.client.Minecraft.getInstance();
			double x = self.getScaledXPos(mc.getWindow());
			double y = self.getScaledYPos(mc.getWindow());
			if(SongIslandClient.island().handleScroll(x, y, vertical)) ci.cancel();
		}
	}
}
