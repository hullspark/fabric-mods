package com.hullspark.coordinatehud.client.mixin;

import java.util.Map;
import java.util.UUID;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.hud.ClientBossBar;

/**
 * Exposes the boss bar count so the coordinate HUD line can move out of
 * the way instead of drawing over the boss bar names (both render top
 * center).
 */
@Mixin(BossBarHud.class)
public interface BossBarHudAccessor {

	// Prefixed with this mod's id (not just "hullspark$"), since another
	// mod adding the same interface method name/signature to this vanilla
	// class would otherwise collide.
	@Accessor("bossBars")
	Map<UUID, ClientBossBar> coordinatehud$getBossBars();
}
