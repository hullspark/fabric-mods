package com.hullspark.coordinatehud;

/**
 * Horizontal (X/Z-plane) distance between two block coordinates, ignoring
 * any difference in height (Y). Pulled out as a plain function with no
 * Minecraft dependency so it can be unit-tested directly: a straight-line
 * (3D) distance would report a large number for a player standing right
 * above spawn on a tall structure, which is misleading for a "how far do
 * I need to walk" readout.
 */
public final class SpawnDistance {

	private SpawnDistance() {
	}

	public static double horizontal(int x1, int z1, int x2, int z2) {
		double dx = x1 - x2;
		double dz = z1 - z2;
		return Math.sqrt(dx * dx + dz * dz);
	}
}
