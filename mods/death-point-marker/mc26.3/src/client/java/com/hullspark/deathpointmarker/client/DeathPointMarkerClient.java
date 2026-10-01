package com.hullspark.deathpointmarker.client;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ClientModInitializer;

public final class DeathPointMarkerClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("death-point-marker");

	@Override
	public void onInitializeClient() {
		DeathPointMarkerRenderer.register();
	}
}
