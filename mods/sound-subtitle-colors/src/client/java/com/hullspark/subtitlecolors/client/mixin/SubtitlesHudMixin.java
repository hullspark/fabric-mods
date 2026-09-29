package com.hullspark.subtitlecolors.client.mixin;

import com.hullspark.subtitlecolors.client.SubtitleColorPalette;
import net.minecraft.client.gui.hud.SubtitlesHud;
import net.minecraft.client.sound.SoundInstance;
import net.minecraft.client.sound.WeightedSoundSet;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(SubtitlesHud.class)
public class SubtitlesHudMixin {
	@ModifyVariable(method = "onSoundPlayed", at = @At("STORE"), ordinal = 0)
	private Text hullspark$colorizeSubtitle(Text text, SoundInstance sound, WeightedSoundSet soundSet, float range) {
		Integer color = SubtitleColorPalette.colorFor(sound.getCategory());
		if (color == null) {
			return text;
		}
		return text.copy().styled(style -> style.withColor(TextColor.fromRgb(color)));
	}
}
