package com.hullspark.deathpointmarker;

/**
 * Pure 3D distance/arrival math with no Minecraft dependency, so it can be
 * unit-tested directly.
 */
public final class DeathPointGeometry {

	private DeathPointGeometry() {
	}

	public static double distance(int x1, int y1, int z1, int x2, int y2, int z2) {
		double dx = x1 - x2;
		double dy = y1 - y2;
		double dz = z1 - z2;
		return Math.sqrt(dx * dx + dy * dy + dz * dz);
	}

	public static boolean isWithinRadius(int x1, int y1, int z1, int x2, int y2, int z2, double radius) {
		return distance(x1, y1, z1, x2, y2, z2) <= radius;
	}
}
