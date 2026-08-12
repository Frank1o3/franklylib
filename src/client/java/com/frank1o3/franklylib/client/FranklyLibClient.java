package com.frank1o3.franklylib.client;

import com.frank1o3.franklylib.client.gui.animation.FranklyUiAnimations;
import com.frank1o3.franklylib.client.gui.style.FranklyUiStyles;
import net.fabricmc.api.ClientModInitializer;

public class FranklyLibClient implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		FranklyUiAnimations.registerReloadListener();
		FranklyUiStyles.registerReloadListener();
	}
}
