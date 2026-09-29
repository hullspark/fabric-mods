package com.hullspark.deathpointmarker.client.mixin;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;

/**
 * Exposes the boss bar count so this HUD line can drop below the boss
 * bar area (and coordinate-hud's line, which also drops below it) when
 * it would otherwise reach into the top-center strip they use.
 */
@Mixin(BossBarHud.class)
public interface BossBarHudAccessor {

	// Prefixed with this mod's id (not just "hullspark$"), since another
	// mod adding the same interface method name/signature to this vanilla
	// class would otherwise collide.
	@Accessor("bossBars")
	Map<UUID, ClientBossBar> deathpointmarker$getBossBars();
}
