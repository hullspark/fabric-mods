package com.hullspark.deathlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DeathLogStoreTest {

	@TempDir
	Path tempDir;

	@Test
	void loadingAMissingFileStartsEmpty(@TempDir Path dir) {
		DeathLogStore store = DeathLogStore.load(dir.resolve("does-not-exist.json"));
		assertEquals(List.of(), store.getRecords(UUID.randomUUID()));
	}

	@Test
	void recordedDeathsSurviveAReload() {
		Path path = tempDir.resolve("deathlog.json");
		UUID playerId = UUID.randomUUID();
		DeathRecord record = new DeathRecord(1_000L, "minecraft:overworld", 1, 64, -1, "Steve was slain by Zombie");

		DeathLogStore store = DeathLogStore.load(path);
		store.recordDeath(playerId, record);

		DeathLogStore reloaded = DeathLogStore.load(path);
		assertEquals(List.of(record), reloaded.getRecords(playerId));
	}

	@Test
	void oldestEntryIsDroppedOncePastTheCap() {
		Path path = tempDir.resolve("deathlog.json");
		UUID playerId = UUID.randomUUID();
		DeathLogStore store = DeathLogStore.load(path);

		for (int i = 0; i < DeathLogStore.MAX_RECORDS_PER_PLAYER + 5; i++) {
			store.recordDeath(playerId, new DeathRecord(i, "minecraft:overworld", i, 64, 0, "death " + i));
		}

		List<DeathRecord> records = store.getRecords(playerId);
		assertEquals(DeathLogStore.MAX_RECORDS_PER_PLAYER, records.size());
		// Oldest 5 (timestamps 0..4) should have been evicted; the list should
		// start at the 6th death and end at the most recent one.
		assertEquals(5L, records.get(0).timestampMillis());
		assertEquals(DeathLogStore.MAX_RECORDS_PER_PLAYER + 4L, records.get(records.size() - 1).timestampMillis());
	}

	@Test
	void deathsForDifferentPlayersAreKeptSeparate() {
		Path path = tempDir.resolve("deathlog.json");
		UUID alice = UUID.randomUUID();
		UUID bob = UUID.randomUUID();
		DeathLogStore store = DeathLogStore.load(path);

		store.recordDeath(alice, new DeathRecord(1L, "minecraft:overworld", 0, 0, 0, "Alice died"));

		assertEquals(1, store.getRecords(alice).size());
		assertEquals(List.of(), store.getRecords(bob));
	}

	@Test
	void aTrailingCommaNullEntryIsSkippedInsteadOfCrashing() throws IOException {
		Path path = tempDir.resolve("deathlog.json");
		UUID playerId = UUID.randomUUID();
		// Gson parses a trailing comma in a JSON array as a `null` element
		// rather than an error; item-pickup-filter hit exactly this once
		// (see docs/verify/item-pickup-filter-2026-09-27.md).
		String json = """
				{
				  "%s": [
				    {"timestampMillis": 1, "dimensionId": "minecraft:overworld", "x": 0, "y": 0, "z": 0, "message": "ok"},
				    null,
				  ]
				}
				""".formatted(playerId);
		Files.writeString(path, json, StandardCharsets.UTF_8);

		DeathLogStore store = DeathLogStore.load(path);

		assertEquals(1, store.getRecords(playerId).size());
	}

	@Test
	void anEntryMissingRequiredFieldsIsSkipped() throws IOException {
		Path path = tempDir.resolve("deathlog.json");
		UUID playerId = UUID.randomUUID();
		String json = """
				{
				  "%s": [
				    {"timestampMillis": 1, "x": 0, "y": 0, "z": 0}
				  ]
				}
				""".formatted(playerId);
		Files.writeString(path, json, StandardCharsets.UTF_8);

		DeathLogStore store = DeathLogStore.load(path);

		assertEquals(List.of(), store.getRecords(playerId));
	}

	@Test
	void unreadableJsonIsReportedAsAWarningAndStartsEmptyInsteadOfThrowing() throws IOException {
		Path path = tempDir.resolve("deathlog.json");
		Files.writeString(path, "{ not valid json", StandardCharsets.UTF_8);
		AtomicInteger warnings = new AtomicInteger();

		DeathLogStore store = DeathLogStore.load(path, (message, cause) -> warnings.incrementAndGet());

		assertEquals(1, warnings.get());
		assertEquals(List.of(), store.getRecords(UUID.randomUUID()));
	}

	@Test
	void anUnreadableFileIsMovedAsideInsteadOfBeingOverwrittenByTheNextDeath() throws IOException {
		// Found in review: without this, the very first death recorded after
		// a load failure would silently overwrite whatever was still in the
		// bad file with a brand-new (empty-until-now) one, permanently
		// losing it instead of just failing to parse it once.
		Path path = tempDir.resolve("deathlog.json");
		String original = "{ not valid json";
		Files.writeString(path, original, StandardCharsets.UTF_8);

		DeathLogStore store = DeathLogStore.load(path);
		assertFalse(Files.exists(path), "the unreadable file should have been moved aside, not left in place");

		Path quarantined = findSingleQuarantineFile(tempDir);
		assertEquals(original, Files.readString(quarantined, StandardCharsets.UTF_8));

		store.recordDeath(UUID.randomUUID(), new DeathRecord(1L, "minecraft:overworld", 0, 0, 0, "died"));

		// The quarantined copy must still be exactly what it was; only a
		// fresh deathlog.json should have been written next to it.
		assertEquals(original, Files.readString(quarantined, StandardCharsets.UTF_8));
		assertTrue(Files.exists(path));
	}

	@Test
	void aTopLevelJsonArrayIsTreatedAsCorruptRatherThanAsAnEmptyLog() throws IOException {
		// `[]`/`[1,2,3]` parse as valid JSON but not as the {playerId: [...]}
		// object this store writes; Gson's Map adapter silently accepts an
		// array as an empty map instead of throwing, so this needs an
		// explicit check rather than relying on JsonSyntaxException.
		Path path = tempDir.resolve("deathlog.json");
		Files.writeString(path, "[]", StandardCharsets.UTF_8);
		AtomicInteger warnings = new AtomicInteger();

		DeathLogStore store = DeathLogStore.load(path, (message, cause) -> warnings.incrementAndGet());

		assertEquals(1, warnings.get());
		assertEquals(List.of(), store.getRecords(UUID.randomUUID()));
		assertFalse(Files.exists(path), "the array-shaped file should have been quarantined");
	}

	@Test
	void savingCreatesParentDirectoriesIfNeeded() {
		Path path = tempDir.resolve("nested/dir/deathlog.json");
		DeathLogStore store = DeathLogStore.load(path);

		store.recordDeath(UUID.randomUUID(), new DeathRecord(1L, "minecraft:overworld", 0, 0, 0, "died"));

		assertTrue(Files.exists(path));
	}

	@Test
	void noTemporaryFileIsLeftBehindAfterASuccessfulSave() throws IOException {
		Path path = tempDir.resolve("deathlog.json");
		DeathLogStore store = DeathLogStore.load(path);

		store.recordDeath(UUID.randomUUID(), new DeathRecord(1L, "minecraft:overworld", 0, 0, 0, "died"));

		assertFalse(Files.exists(tempDir.resolve("deathlog.json.tmp")));
	}

	private static Path findSingleQuarantineFile(Path dir) throws IOException {
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.corrupt-*")) {
			List<Path> matches = new java.util.ArrayList<>();
			stream.forEach(matches::add);
			assertEquals(1, matches.size(), "expected exactly one quarantined file in " + dir);
			return matches.get(0);
		}
	}
}
