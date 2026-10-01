package com.hullspark.deathlog;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

/**
 * Turns a player's stored {@link DeathRecord} list into plain-text lines for
 * chat feedback. Pure and Minecraft-independent so it can be unit-tested
 * without booting the game; {@link DeathLogMod} wraps each line in a
 * {@code Text.literal(...)} when actually sending it.
 */
public final class DeathLogFormatter {

	public static final String NO_DEATHS_MESSAGE = "No deaths recorded yet.";

	// Always UTC: the underlying timestamp is wall-clock (System.currentTimeMillis()),
	// and a fixed zone keeps the formatted log reproducible regardless of the
	// host machine's local timezone (matters for both tests and cross-server play).
	private static final DateTimeFormatter TIMESTAMP_FORMAT =
			DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneOffset.UTC);

	private DeathLogFormatter() {}

	/**
	 * Newest-first lines, each numbered starting at 1, capped at {@code limit}
	 * entries even if more are stored. Returns a single explanatory line
	 * (not empty) when there is nothing to show, so callers always have at
	 * least one line to send.
	 */
	public static List<String> format(List<DeathRecord> records, int limit) {
		if (records.isEmpty()) {
			return List.of(NO_DEATHS_MESSAGE);
		}
		int count = Math.min(limit, records.size());
		List<String> lines = new ArrayList<>(count);
		for (int i = 0; i < count; i++) {
			// records is stored oldest-first (append order); walk backwards
			// from the end so the most recent death is #1.
			DeathRecord record = records.get(records.size() - 1 - i);
			lines.add(formatLine(i + 1, record));
		}
		return lines;
	}

	private static String formatLine(int index, DeathRecord record) {
		String timestamp = TIMESTAMP_FORMAT.format(Instant.ofEpochMilli(record.timestampMillis()));
		String dimension = shortDimensionName(record.dimensionId());
		return "#%d  %s UTC  %s (%d, %d, %d)  — %s".formatted(
				index, timestamp, dimension, record.x(), record.y(), record.z(), record.message());
	}

	/** "minecraft:the_nether" -> "the_nether"; a non-vanilla namespace is kept in full so it stays identifiable. */
	private static String shortDimensionName(String dimensionId) {
		if (dimensionId.startsWith("minecraft:")) {
			return dimensionId.substring("minecraft:".length());
		}
		return dimensionId;
	}
}
