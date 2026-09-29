package com.hullspark.deathpointmarker.client;

/**
 * Maps a yaw angle (Minecraft convention: 0 = south, 90 = west, -90/270 =
 * east, 180/-180 = north, increasing clockwise) to an 8-point compass label.
 */
public final class CompassDirection {

	private static final String[] LABELS = {
			"S", "SW", "W", "NW", "N", "NE", "E", "SE"
	};

	private CompassDirection() {
	}

	public static String label(float yawDegrees) {
		float normalized = yawDegrees % 360.0f;
		if (normalized < 0) {
			normalized += 360.0f;
		}
		int index = Math.round(normalized / 45.0f) % 8;
		return LABELS[index];
	}
}
