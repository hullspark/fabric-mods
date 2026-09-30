package com.hullspark.itempickupfilter;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

/**
 * Reads (and creates, on first run) {@code config/item-pickup-filter.json}:
 * a single {@code "blacklist"} array of item ids that should never be
 * auto-picked-up. Falls back to the shipped defaults if the file is
 * missing or malformed, so a hand-edit mistake never crashes the game.
 *
 * <p>The list is kept as {@link Identifier}s rather than resolved {@link Item}s:
 * resolving against {@link Registries#ITEM} at load time would depend on
 * whether another mod has already registered its items by the time our
 * {@code onInitialize} runs (Fabric does not order mod entrypoints), so a
 * modded item id could silently fail to resolve depending on mod load
 * order. Comparing by id at pickup time sidesteps that entirely.
 */
public final class ItemPickupFilterConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final List<String> DEFAULT_BLACKLIST = List.of(
			"minecraft:rotten_flesh",
			"minecraft:poisonous_potato",
			"minecraft:spider_eye"
	);

	private static volatile Set<Identifier> blacklist = Set.of();

	private ItemPickupFilterConfig() {
	}

	public static void load() {
		Data data;
		try {
			Path path = FabricLoader.getInstance().getConfigDir().resolve("item-pickup-filter.json");
			data = Files.exists(path) ? readExisting(path) : createDefault(path);
		} catch (RuntimeException e) {
			ItemPickupFilterMod.LOGGER.warn(
					"[item-pickup-filter] unexpected error loading config, falling back to defaults ({})",
					e.toString());
			data = new Data();
			data.blacklist = DEFAULT_BLACKLIST;
		}
		blacklist = resolve(data.blacklist);
	}

	private static Data createDefault(Path path) {
		Data data = new Data();
		data.blacklist = DEFAULT_BLACKLIST;
		write(path, data);
		return data;
	}

	private static Data readExisting(Path path) {
		try {
			String json = Files.readString(path, StandardCharsets.UTF_8);
			Data data = GSON.fromJson(json, Data.class);
			if (data == null || data.blacklist == null) {
				throw new JsonSyntaxException("missing 'blacklist' array");
			}
			return data;
		} catch (IOException | JsonSyntaxException e) {
			ItemPickupFilterMod.LOGGER.warn(
					"[item-pickup-filter] could not read {}, falling back to defaults ({})", path, e.toString());
			Data data = new Data();
			data.blacklist = DEFAULT_BLACKLIST;
			return data;
		}
	}

	private static void write(Path path, Data data) {
		try {
			Files.createDirectories(path.getParent());
			Files.writeString(path, GSON.toJson(data), StandardCharsets.UTF_8);
		} catch (IOException e) {
			ItemPickupFilterMod.LOGGER.warn("[item-pickup-filter] could not write {} ({})", path, e.toString());
		}
	}

	private static Set<Identifier> resolve(List<String> ids) {
		Set<Identifier> identifiers = new LinkedHashSet<>();
		for (String id : ids) {
			if (id == null) {
				ItemPickupFilterMod.LOGGER.warn("[item-pickup-filter] skipping null entry in blacklist");
				continue;
			}
			Identifier identifier = Identifier.tryParse(id);
			if (identifier == null) {
				ItemPickupFilterMod.LOGGER.warn("[item-pickup-filter] skipping invalid item id '{}'", id);
				continue;
			}
			identifiers.add(identifier);
		}
		return identifiers;
	}

	public static boolean isBlacklisted(Item item) {
		return blacklist.contains(Registries.ITEM.getId(item));
	}

	private static final class Data {
		List<String> blacklist;
	}
}
