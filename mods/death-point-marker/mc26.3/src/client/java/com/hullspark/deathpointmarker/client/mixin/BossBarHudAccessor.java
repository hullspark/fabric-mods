package com.hullspark.deathpointmarker.client.mixin;

import java.util.Map;
import java.util.UUID;
import net.minecraft.client.gui.components.BossHealthOverlay;
import net.minecraft.client.gui.components.LerpingBossEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/**
 * Exposes the boss bar count so this HUD line can drop below the boss
 * bar area (and coordinate-hud's line, which also drops below it) when
 * it would otherwise reach into the top-center strip they use.
 */
@Mixin(BossHealthOverlay.class)
public interface BossBarHudAccessor {

	// Prefixed with this mod's id (not just "hullspark$"), since another
	// mod adding the same interface method name/signature to this vanilla
	// class would otherwise collide.
	@Accessor("events")
	Map<UUID, LerpingBossEvent> deathpointmarker$getBossBars();
}
