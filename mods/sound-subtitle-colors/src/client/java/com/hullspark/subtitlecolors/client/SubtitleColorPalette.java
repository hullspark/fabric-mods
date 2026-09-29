package com.hullspark.subtitlecolors.client;

import java.util.Map;
import net.minecraft.sound.SoundCategory;

/**
 * Maps vanilla {@link SoundCategory} to a color-blind-safe categorical
 * palette based on Okabe & Ito, 2008 (https://jfly.uni-koeln.de/color/),
 * with the WEATHER blue brightened from the original Okabe–Ito value so
 * it reads clearly against this mod's dark, semi-transparent subtitle
 * background. Categories that are not source/threat information for the
 * player (MASTER, MUSIC, RECORDS, UI) are left out on purpose and keep
 * the vanilla subtitle color.
 *
 * <p>WEATHER's original Okabe–Ito blue ({@code #0072B2}) has a contrast
 * ratio of only ~4.0:1 against black, below the WCAG AA text threshold
 * (4.5:1), and it was rated the hardest of the 7 colors to read against
 * the subtitle background during the 2026-09-29 real-machine playtest
 * (`docs/verify/playtest-2026-09-29/README.md`). {@code #0072FC} changes
 * only the blue channel (raised from {@code 0xB2} to {@code 0xFC}, R and
 * G unchanged) and clears 4.5:1 contrast against black while staying
 * clearly distinguishable from every other color in this palette —
 * including PLAYERS' sky blue — under normal vision, protanopia and
 * deuteranopia (CIELAB ΔE ≥ 20 in all three; see
 * `docs/verify/sound-subtitle-colors-2026-09-29-weather-blue.md` for the
 * full calculation and the candidates considered).
 */
public final class SubtitleColorPalette {
	private static final Map<SoundCategory, Integer> COLORS_BY_CATEGORY = Map.of(
			SoundCategory.HOSTILE, 0xD55E00, // vermillion: danger
			SoundCategory.NEUTRAL, 0xF0E442, // yellow: passive/neutral mobs
			SoundCategory.PLAYERS, 0x56B4E9, // sky blue: other players
			SoundCategory.BLOCKS, 0xE69F00, // orange: block interactions
			SoundCategory.WEATHER, 0x0072FC, // blue (brightened from Okabe-Ito #0072B2): weather
			SoundCategory.AMBIENT, 0x009E73, // bluish green: ambience
			SoundCategory.VOICE, 0xCC79A7 // reddish purple: voice/speech
	);

	private SubtitleColorPalette() {
	}

	/** Returns the RGB color for {@code category}, or {@code null} to keep the vanilla color. */
	public static Integer colorFor(SoundCategory category) {
		return COLORS_BY_CATEGORY.get(category);
	}
}
