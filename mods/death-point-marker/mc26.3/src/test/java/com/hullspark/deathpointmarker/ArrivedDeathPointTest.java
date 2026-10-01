package com.hullspark.deathpointmarker;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ArrivedDeathPointTest {

	@Test
	void matchesSameWorldDimensionAndCoordinates() {
		ArrivedDeathPoint arrived = new ArrivedDeathPoint("singleplayer:My World", "minecraft:overworld", 10, 64, -5);
		assertTrue(arrived.matches("singleplayer:My World", "minecraft:overworld", 10, 64, -5));
	}

	@Test
	void doesNotMatchDifferentCoordinates() {
		ArrivedDeathPoint arrived = new ArrivedDeathPoint("singleplayer:My World", "minecraft:overworld", 10, 64, -5);
		assertFalse(arrived.matches("singleplayer:My World", "minecraft:overworld", 11, 64, -5));
	}

	@Test
	void doesNotMatchDifferentDimensionEvenWithSameCoordinates() {
		// Dying at the same block coordinates in two different dimensions
		// (overworld vs. nether) must not be treated as the same death point.
		ArrivedDeathPoint arrived = new ArrivedDeathPoint("singleplayer:My World", "minecraft:overworld", 10, 64, -5);
		assertFalse(arrived.matches("singleplayer:My World", "minecraft:the_nether", 10, 64, -5));
	}

	@Test
	void doesNotMatchDifferentWorldEvenWithSameDimensionAndCoordinates() {
		// A different singleplayer world (or a different server) can easily
		// have a death at the exact same block coordinates and dimension;
		// that must not be treated as the same, already-recovered point.
		ArrivedDeathPoint arrived = new ArrivedDeathPoint("singleplayer:My World", "minecraft:overworld", 10, 64, -5);
		assertFalse(arrived.matches("singleplayer:Another World", "minecraft:overworld", 10, 64, -5));
		assertFalse(arrived.matches("server:play.example.com", "minecraft:overworld", 10, 64, -5));
	}
}
