package com.hullspark.chestsearch;

import java.util.Locale;

/**
 * Case-insensitive substring matching for the chest search box, with no
 * Minecraft dependency so it can be unit-tested directly.
 *
 * <p>Two independent checks feed the same search box: the item's display
 * name (whatever the current game language shows, e.g. "鉄インゴット" in
 * Japanese) and its registry id path (e.g. {@code iron_ingot}, always
 * English/ASCII regardless of game language). Matching the id path too
 * means players on a non-English game language can still search with a
 * plain English word. Since id paths use {@code _} where a display name
 * would have a space, a query typed with spaces (e.g. {@code "iron
 * ingot"}) is also matched against the id path with those spaces turned
 * into {@code _} (id matching only — display names keep their spaces
 * as-is).
 */
public final class ItemSearchMatcher {

	private ItemSearchMatcher() {
	}

	/** {@code queryLower} must already be lower-cased ({@link Locale#ROOT}). */
	public static boolean nameMatches(String displayName, String queryLower) {
		return displayName.toLowerCase(Locale.ROOT).contains(queryLower);
	}

	/** {@code queryLower} must already be lower-cased ({@link Locale#ROOT}). */
	public static boolean idPathMatches(String idPath, String queryLower) {
		String idQuery = queryLower.replace(' ', '_');
		return idPath.toLowerCase(Locale.ROOT).contains(idQuery);
	}
}
