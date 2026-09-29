package com.hullspark.chestsearch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class ItemSearchMatcherTest {

	@Test
	void nameMatchIsCaseInsensitiveSubstring() {
		assertTrue(ItemSearchMatcher.nameMatches("Golden Apple", "apple"));
		assertFalse(ItemSearchMatcher.nameMatches("Golden Apple", "sword"));
	}

	@Test
	void idPathMatchesPlainSubstring() {
		assertTrue(ItemSearchMatcher.idPathMatches("iron_ingot", "iron"));
		assertTrue(ItemSearchMatcher.idPathMatches("iron_ingot", "ingot"));
	}

	@Test
	void idPathMatchesQueryWithSpaceAsUnderscore() {
		// "iron ingot" (space) must still find the "iron_ingot" (underscore) id,
		// e.g. when the display name is in a non-Latin language and the id
		// path is the only thing a Latin-alphabet query can match against.
		assertTrue(ItemSearchMatcher.idPathMatches("iron_ingot", "iron ingot"));
	}

	@Test
	void idPathDoesNotMatchUnrelatedWord() {
		assertFalse(ItemSearchMatcher.idPathMatches("iron_ingot", "bread"));
	}

	@Test
	void nameMatchDoesNotConvertSpacesToUnderscores() {
		// Display names never contain underscores, so this conversion is
		// intentionally id-path-only; a space in the query should still
		// match a display name that actually has a space.
		assertTrue(ItemSearchMatcher.nameMatches("Iron Ingot", "iron ingot"));
	}
}
