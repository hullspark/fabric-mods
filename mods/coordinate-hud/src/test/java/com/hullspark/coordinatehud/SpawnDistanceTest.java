package com.hullspark.coordinatehud;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SpawnDistanceTest {

	private static final double DELTA = 1e-9;

	@Test
	void sameColumnIsZeroEvenWithLargeHeightDifference() {
		// Standing directly above/below spawn: horizontal distance is 0
		// regardless of how far away Y is (this is the bug being fixed:
		// the old 3D distance reported 51m here for a Y difference alone).
		assertEquals(0.0, SpawnDistance.horizontal(0, 0, 0, 0), DELTA);
	}

	@Test
	void horizontalDistanceIgnoresY() {
		assertEquals(5.0, SpawnDistance.horizontal(3, 4, 0, 0), DELTA);
	}

	@Test
	void handlesNegativeCoordinates() {
		assertEquals(5.0, SpawnDistance.horizontal(-3, -4, 0, 0), DELTA);
	}

	@Test
	void diagonalDistance() {
		assertEquals(Math.sqrt(2.0), SpawnDistance.horizontal(1, 1, 0, 0), DELTA);
	}
}
