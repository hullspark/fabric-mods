package com.hullspark.deathpointmarker.client;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.fabricmc.loader.api.FabricLoader;

import com.hullspark.deathpointmarker.ArrivedDeathPoint;

/**
 * Persists which death point the player has already recovered (see
 * {@link ArrivedDeathPoint}) to {@code config/death-point-marker.json}, so
 * it survives restarting the game (only the in-memory state would
 * otherwise be lost on relaunch, and the HUD line would reappear for a
 * death point already dealt with).
 *
 * <p>Loaded lazily once (from the render thread, on first use) and kept in
 * memory after that; {@link #markArrived} only touches disk when the
 * arrived point actually changes, not on every render call, since render
 * runs every frame. A read/write failure (missing/corrupt file,
 * unwritable config dir) never crashes the game: this mod just falls back
 * to tracking the state in memory only for the rest of the session.
 */
final class DeathPointStateStore {
	private static final Gson GSON = new GsonBuilder().create();
	private static final String FILE_NAME = "death-point-marker.json";

	private static boolean loaded;
	private static Optional<ArrivedDeathPoint> arrived = Optional.empty();

	private DeathPointStateStore() {
	}

	static synchronized Optional<ArrivedDeathPoint> getArrived() {
		if (!loaded) {
			arrived = load();
			loaded = true;
		}
		return arrived;
	}

	static synchronized void markArrived(String worldId, String dimension, int x, int y, int z) {
		if (!loaded) {
			arrived = load();
			loaded = true;
		}
		ArrivedDeathPoint next = new ArrivedDeathPoint(worldId, dimension, x, y, z);
		if (arrived.isPresent() && arrived.get().equals(next)) {
			// Already the persisted point; nothing changed, don't write.
			return;
		}
		arrived = Optional.of(next);
		save(next);
	}

	private static Path configPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(FILE_NAME);
	}

	private static Optional<ArrivedDeathPoint> load() {
		try {
			Path path = configPath();
			if (!Files.exists(path)) {
				return Optional.empty();
			}
			String json = Files.readString(path, StandardCharsets.UTF_8);
			Data data = GSON.fromJson(json, Data.class);
			if (data == null || data.worldId == null || data.dimension == null) {
				return Optional.empty();
			}
			return Optional.of(new ArrivedDeathPoint(data.worldId, data.dimension, data.x, data.y, data.z));
		} catch (RuntimeException | IOException e) {
			DeathPointMarkerClient.LOGGER.warn(
					"[death-point-marker] could not read {}, starting with no recovered death point ({})",
					FILE_NAME, e.toString());
			return Optional.empty();
		}
	}

	private static void save(ArrivedDeathPoint point) {
		try {
			Path path = configPath();
			Files.createDirectories(path.getParent());
			Data data = new Data();
			data.worldId = point.worldId();
			data.dimension = point.dimension();
			data.x = point.x();
			data.y = point.y();
			data.z = point.z();
			Files.writeString(path, GSON.toJson(data), StandardCharsets.UTF_8);
		} catch (RuntimeException | IOException e) {
			DeathPointMarkerClient.LOGGER.warn(
					"[death-point-marker] could not write {}, recovered death point won't survive a restart ({})",
					FILE_NAME, e.toString());
		}
	}

	private static final class Data {
		String worldId;
		String dimension;
		int x;
		int y;
		int z;
	}
}
