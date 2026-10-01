package com.hullspark.deathpointmarker;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeathPointGeometryTest {

	private static final double DELTA = 1e-9;

	@Test
	void distanceIsZeroAtSamePoint() {
		assertEquals(0.0, DeathPointGeometry.distance(0, 0, 0, 0, 0, 0), DELTA);
	}

	@Test
	void distanceUsesAllThreeAxes() {
		assertEquals(Math.sqrt(1 + 4 + 4), DeathPointGeometry.distance(0, 0, 0, 1, 2, 2), DELTA);
	}

	@Test
	void withinRadiusAtExactBoundaryIsArrived() {
		// 3.0 blocks away with a 3.0 radius: the renderer treats "<=" as arrived.
		assertTrue(DeathPointGeometry.isWithinRadius(0, 0, 0, 3, 0, 0, 3.0));
	}

	@Test
	void justOutsideRadiusIsNotArrived() {
		assertFalse(DeathPointGeometry.isWithinRadius(0, 0, 0, 4, 0, 0, 3.0));
	}
}
