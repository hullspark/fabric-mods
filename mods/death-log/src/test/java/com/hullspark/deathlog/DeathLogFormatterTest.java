package com.hullspark.deathlog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;

class DeathLogFormatterTest {

	@Test
	void emptyHistoryShowsExplanation() {
		List<String> lines = DeathLogFormatter.format(List.of(), 10);
		assertEquals(List.of(DeathLogFormatter.NO_DEATHS_MESSAGE), lines);
	}

	@Test
	void mostRecentDeathIsListedFirst() {
		DeathRecord older = new DeathRecord(1_000L, "minecraft:overworld", 1, 2, 3, "Steve was slain by Zombie");
		DeathRecord newer = new DeathRecord(2_000L, "minecraft:the_nether", 4, 5, 6, "Steve was pricked to death");

		List<String> lines = DeathLogFormatter.format(List.of(older, newer), 10);

		assertEquals(2, lines.size());
		assertTrue(lines.get(0).startsWith("#1"));
		assertTrue(lines.get(0).contains("the_nether"), "newest death should be first: " + lines.get(0));
		assertTrue(lines.get(1).startsWith("#2"));
		assertTrue(lines.get(1).contains("overworld"), "oldest death should be last: " + lines.get(1));
	}

	@Test
	void limitCapsHowManyLinesAreShownEvenWithMoreStored() {
		List<DeathRecord> records = List.of(
				new DeathRecord(1L, "minecraft:overworld", 0, 0, 0, "a"),
				new DeathRecord(2L, "minecraft:overworld", 0, 0, 0, "b"),
				new DeathRecord(3L, "minecraft:overworld", 0, 0, 0, "c"));

		List<String> lines = DeathLogFormatter.format(records, 2);

		assertEquals(2, lines.size());
		assertTrue(lines.get(0).endsWith("c"));
		assertTrue(lines.get(1).endsWith("b"));
	}

	@Test
	void timestampIsFormattedInUtcRegardlessOfHostTimeZone() {
		// 2026-09-30T12:00:00Z
		long epochMillis = 1790769600000L;
		DeathRecord record = new DeathRecord(epochMillis, "minecraft:overworld", 0, 64, 0, "Steve died");

		List<String> lines = DeathLogFormatter.format(List.of(record), 1);

		assertTrue(lines.get(0).contains("2026-09-30 12:00 UTC"), lines.get(0));
	}

	@Test
	void vanillaMinecraftNamespaceIsShortenedButOthersAreNot() {
		DeathRecord vanilla = new DeathRecord(1L, "minecraft:the_end", 0, 0, 0, "died");
		DeathRecord custom = new DeathRecord(1L, "othermod:somewhere", 0, 0, 0, "died");

		assertTrue(DeathLogFormatter.format(List.of(vanilla), 1).get(0).contains("the_end"));
		assertTrue(DeathLogFormatter.format(List.of(custom), 1).get(0).contains("othermod:somewhere"));
	}
}
