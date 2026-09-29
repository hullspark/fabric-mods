package com.hullspark.deathpointmarker;

import java.util.Objects;

/**
 * The death point the player has already recovered (walked within 3
 * blocks of), if any. Once a death point is recorded as arrived, the HUD
 * line for it stays hidden even after walking away again, until the
 * server reports a different death point (a new death). Kept as a plain
 * value class with no Minecraft dependency so the "is this the same
 * point I already arrived at" logic can be unit-tested directly.
 *
 * <p>{@code worldId} identifies which world/server this was recorded in
 * (e.g. {@code "server:play.example.com"} for multiplayer, or
 * {@code "singleplayer:My World (1)"} for singleplayer — the save's
 * folder name under {@code saves/}, not its display name, since that
 * isn't unique). Dimension + coordinates alone are not enough: two
 * different singleplayer worlds (or two different servers) can easily
 * have a death recorded at the same block coordinates in the same
 * dimension, and without a world identifier the persisted state would
 * wrongly stay "arrived" — hiding a brand new death's marker — after
 * switching to a different world/server that happens to reuse those
 * coordinates.
 */
public final class ArrivedDeathPoint {

	private final String worldId;
	private final String dimension;
	private final int x;
	private final int y;
	private final int z;

	public ArrivedDeathPoint(String worldId, String dimension, int x, int y, int z) {
		this.worldId = Objects.requireNonNull(worldId, "worldId");
		this.dimension = Objects.requireNonNull(dimension, "dimension");
		this.x = x;
		this.y = y;
		this.z = z;
	}

	/** True if {@code worldId}/{@code dimension}/{@code x}/{@code y}/{@code z} is this exact death point. */
	public boolean matches(String worldId, String dimension, int x, int y, int z) {
		return this.worldId.equals(worldId)
				&& this.dimension.equals(dimension)
				&& this.x == x && this.y == y && this.z == z;
	}

	public String worldId() {
		return worldId;
	}

	public String dimension() {
		return dimension;
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

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (!(obj instanceof ArrivedDeathPoint other)) {
			return false;
		}
		return x == other.x && y == other.y && z == other.z
				&& worldId.equals(other.worldId) && dimension.equals(other.dimension);
	}

	@Override
	public int hashCode() {
		return Objects.hash(worldId, dimension, x, y, z);
	}
}
