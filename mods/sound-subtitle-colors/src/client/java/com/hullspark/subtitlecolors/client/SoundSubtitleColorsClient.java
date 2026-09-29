package com.hullspark.subtitlecolors.client;

import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SoundSubtitleColorsClient implements ClientModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("sound-subtitle-colors");

	@Override
	public void onInitializeClient() {
		LOGGER.info("Sound Subtitle Colors loaded");
	}
}
