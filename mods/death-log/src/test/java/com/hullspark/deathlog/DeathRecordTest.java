package com.hullspark.deathlog;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DeathRecordTest {

	@Test
	void fullyPopulatedRecordIsValid() {
		DeathRecord record = new DeathRecord(1L, "minecraft:overworld", 1, 2, 3, "Steve died");
		assertTrue(record.isValid());
	}

	@Test
	void nullDimensionIdIsInvalid() {
		DeathRecord record = new DeathRecord(1L, null, 1, 2, 3, "Steve died");
		assertFalse(record.isValid());
	}

	@Test
	void blankMessageIsInvalid() {
		DeathRecord record = new DeathRecord(1L, "minecraft:overworld", 1, 2, 3, "   ");
		assertFalse(record.isValid());
	}
}
