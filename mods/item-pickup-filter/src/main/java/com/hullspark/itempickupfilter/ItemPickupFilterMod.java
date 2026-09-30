package com.hullspark.itempickupfilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.api.ModInitializer;

public final class ItemPickupFilterMod implements ModInitializer {
	public static final String MOD_ID = "item-pickup-filter";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ItemPickupFilterConfig.load();
	}
}
