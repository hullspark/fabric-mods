package com.hullspark.deathlog;

/**
 * One recorded death: when it happened (wall-clock, not in-game time, so it
 * stays meaningful across worlds with different day counts), where (dimension
 * id + block position), and the vanilla death message text (e.g. "Steve was
 * slain by Zombie") as it would have appeared in chat.
 *
 * <p>Deliberately not a {@code record}: Gson can populate a record's fields
 * via reflection without ever calling its canonical constructor, so a
 * compact-constructor {@code Objects.requireNonNull} would not actually catch
 * a malformed JSON entry (missing/renamed field) at load time the way it
 * would for a normal constructor call. See {@link DeathLogStore} for how
 * loaded entries are validated instead.
 */
public final class DeathRecord {
	private final long timestampMillis;
	private final String dimensionId;
	private final int x;
	private final int y;
	private final int z;
	private final String message;

	public DeathRecord(long timestampMillis, String dimensionId, int x, int y, int z, String message) {
		this.timestampMillis = timestampMillis;
		this.dimensionId = dimensionId;
		this.x = x;
		this.y = y;
		this.z = z;
		this.message = message;
	}

	public long timestampMillis() {
		return timestampMillis;
	}

	public String dimensionId() {
		return dimensionId;
	}

	public int x() {
		return x;
	}

	public int y() {
		return y;
	}

	public int z() {
		return z;
	}

	public String message() {
		return message;
	}

	/**
	 * True only for an entry that has everything needed to be displayed and
	 * trusted: both string fields present and non-blank. Used to drop
	 * corrupt entries (e.g. Gson turning a trailing-comma JSON array element
	 * into a {@code null} list entry, or a hand-edited file missing a field)
	 * instead of crashing or showing a blank line.
	 */
	public boolean isValid() {
		return dimensionId != null && !dimensionId.isBlank() && message != null && !message.isBlank();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof DeathRecord other)) {
			return false;
		}
		return timestampMillis == other.timestampMillis
				&& x == other.x
				&& y == other.y
				&& z == other.z
				&& java.util.Objects.equals(dimensionId, other.dimensionId)
				&& java.util.Objects.equals(message, other.message);
	}

	@Override
	public int hashCode() {
		return java.util.Objects.hash(timestampMillis, dimensionId, x, y, z, message);
	}

	@Override
	public String toString() {
		return "DeathRecord{" + timestampMillis + ", " + dimensionId + ", (" + x + "," + y + "," + z + "), '"
				+ message + "'}";
	}
}
