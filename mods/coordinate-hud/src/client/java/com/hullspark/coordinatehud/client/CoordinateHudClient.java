package com.hullspark.coordinatehud.client;

import net.fabricmc.api.ClientModInitializer;

public final class CoordinateHudClient implements ClientModInitializer {

	@Override
	public void onInitializeClient() {
		CoordinateHudRenderer.register();
	}
}
