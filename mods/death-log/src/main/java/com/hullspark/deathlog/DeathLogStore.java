package com.hullspark.deathlog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;

/**
 * Per-world persistence for each player's death history, stored as one JSON
 * file under the world's own save folder (so singleplayer worlds and
 * dedicated servers each get their own history, and copying/deleting a save
 * takes its death log with it).
 *
 * <p>Deliberately plain JSON via Gson rather than a Minecraft
 * {@code PersistentState}/NBT: this class has no Minecraft type in its
 * signature (only {@link java.nio.file.Path} and {@link DeathRecord}), so it
 * can be unit-tested directly against a temp directory. See
 * {@link DeathLogMod} for where it is wired to the actual world save path and
 * the death event.
 *
 * <p>Read/write failures never throw. An unreadable or wrongly-shaped file
 * is moved aside to {@code <name>.corrupt-<epochMillis>} rather than left in
 * place, so it survives instead of being silently overwritten by the very
 * next recorded death (which would otherwise start from an empty in-memory
 * map and truncate the original on save); loading then continues with an
 * empty log in memory, same as this repo's other mods handle their own
 * config/state files. A write goes to a sibling {@code .tmp} file that is
 * then renamed over the real one (atomically where the filesystem supports
 * it), so a crash mid-write can never leave a half-written, unreadable file
 * behind. A failed write is logged and otherwise ignored rather than
 * crashing the death event it was triggered from.
 */
public final class DeathLogStore {
	/** Oldest entries are dropped once a player passes this many recorded deaths. */
	public static final int MAX_RECORDS_PER_PLAYER = 20;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final java.lang.reflect.Type DATA_TYPE =
			new TypeToken<Map<String, List<DeathRecord>>>() {}.getType();

	private final Path path;
	private final Map<UUID, List<DeathRecord>> deaths;
	private final Listener listener;

	/** Receives non-fatal problems (bad JSON, failed write) for the mod to log. */
	public interface Listener {
		void onWarning(String message, Throwable cause);

		Listener NONE = (message, cause) -> {};
	}

	private DeathLogStore(Path path, Map<UUID, List<DeathRecord>> deaths, Listener listener) {
		this.path = path;
		this.deaths = deaths;
		this.listener = listener;
	}

	public static DeathLogStore load(Path path) {
		return load(path, Listener.NONE);
	}

	public static DeathLogStore load(Path path, Listener listener) {
		Map<UUID, List<DeathRecord>> loaded = new LinkedHashMap<>();
		if (Files.isRegularFile(path)) {
			try {
				String json = Files.readString(path, StandardCharsets.UTF_8);
				JsonElement root = JsonParser.parseString(json);
				if (!root.isJsonObject()) {
					// Not necessarily invalid JSON (e.g. a bare `[]` parses fine
					// but isn't the {playerId: [...]} shape this store writes);
					// treated the same as unreadable JSON below.
					throw new JsonSyntaxException("Expected a JSON object at the top level, found: " + root);
				}
				Map<String, List<DeathRecord>> raw = GSON.fromJson(root, DATA_TYPE);
				if (raw != null) {
					for (Map.Entry<String, List<DeathRecord>> entry : raw.entrySet()) {
						UUID playerId = parseUuidOrNull(entry.getKey());
						List<DeathRecord> records = entry.getValue();
						if (playerId == null || records == null) {
							continue;
						}
						List<DeathRecord> sanitized = new ArrayList<>();
						for (DeathRecord record : records) {
							// Gson turns a JSON `null` array element (e.g. from a
							// trailing comma) into a null list entry rather than
							// throwing, and a hand-edited file can omit a field;
							// either way isValid() catches it here instead of a
							// NullPointerException later when formatting.
							if (record != null && record.isValid()) {
								sanitized.add(record);
							}
						}
						loaded.put(playerId, List.copyOf(capTo(sanitized, MAX_RECORDS_PER_PLAYER)));
					}
				}
			} catch (IOException | JsonSyntaxException e) {
				// Move the unreadable file aside instead of silently starting
				// empty: the very next recorded death would otherwise
				// overwrite it with a fresh file, permanently losing whatever
				// was still recoverable in it (found in review before this
				// mod was ever published).
				Path quarantined = quarantine(path, listener);
				listener.onWarning(
						quarantined != null
								? "Failed to read " + path + "; moved it to " + quarantined + " and starting with an empty death log."
								: "Failed to read " + path + "; starting with an empty death log (could not move the bad file aside).",
						e);
			}
		}
		return new DeathLogStore(path, loaded, listener);
	}

	private static Path quarantine(Path path, Listener listener) {
		Path target = path.resolveSibling(path.getFileName() + ".corrupt-" + System.currentTimeMillis());
		try {
			Files.move(path, target);
			return target;
		} catch (IOException e) {
			listener.onWarning("Failed to move unreadable " + path + " aside to " + target, e);
			return null;
		}
	}

	/** Appends one death for the given player, persists, and returns their updated (newest-last) history. */
	public synchronized List<DeathRecord> recordDeath(UUID playerId, DeathRecord record) {
		List<DeathRecord> updated = new ArrayList<>(deaths.getOrDefault(playerId, List.of()));
		updated.add(record);
		updated = capTo(updated, MAX_RECORDS_PER_PLAYER);
		deaths.put(playerId, List.copyOf(updated));
		save();
		return deaths.get(playerId);
	}

	/** Newest-last history for the given player, or an empty list if they have none recorded. */
	public synchronized List<DeathRecord> getRecords(UUID playerId) {
		return deaths.getOrDefault(playerId, List.of());
	}

	private static List<DeathRecord> capTo(List<DeathRecord> records, int max) {
		if (records.size() <= max) {
			return records;
		}
		return new ArrayList<>(records.subList(records.size() - max, records.size()));
	}

	private static UUID parseUuidOrNull(String text) {
		try {
			return UUID.fromString(text);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}

	private void save() {
		Map<String, List<DeathRecord>> serializable = new LinkedHashMap<>();
		for (Map.Entry<UUID, List<DeathRecord>> entry : deaths.entrySet()) {
			serializable.put(entry.getKey().toString(), entry.getValue());
		}
		Path absolutePath = path.toAbsolutePath();
		Path parent = absolutePath.getParent();
		Path tmp = parent != null
				? parent.resolve(absolutePath.getFileName() + ".tmp")
				: Path.of(absolutePath + ".tmp");
		try {
			if (parent != null) {
				Files.createDirectories(parent);
			}
			// Write to a sibling temp file and rename over the real one,
			// rather than truncating it in place: a crash/kill/power loss
			// mid-write would otherwise leave a half-written (and therefore
			// unreadable, per load()'s quarantine-on-bad-JSON path) file
			// where a good one used to be (found in review before this mod
			// was ever published).
			Files.writeString(tmp, GSON.toJson(serializable, DATA_TYPE), StandardCharsets.UTF_8);
			try {
				Files.move(tmp, absolutePath, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tmp, absolutePath, StandardCopyOption.REPLACE_EXISTING);
			}
		} catch (IOException e) {
			listener.onWarning("Failed to write " + path + "; this death was kept in memory only.", e);
		}
	}
}
