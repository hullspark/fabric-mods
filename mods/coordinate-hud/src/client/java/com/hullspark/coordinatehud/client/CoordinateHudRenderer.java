package com.hullspark.coordinatehud.client;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraft.world.WorldProperties.SpawnPoint;

import com.hullspark.coordinatehud.SpawnDistance;
import com.hullspark.coordinatehud.client.mixin.BossBarHudAccessor;

/**
 * Draws a single-line HUD bar: facing direction + bearing in degrees,
 * block coordinates, and horizontal (X/Z-plane) distance to the world
 * spawn point (used as a stand-in "destination" until a player-settable
 * waypoint is added in a later unit). Height (Y) is deliberately excluded
 * from the distance: it's meant to answer "how far do I need to walk",
 * and a tall drop/climb directly above spawn would otherwise read as a
 * large distance despite being right on top of it.
 *
 * Purely a display feature: it reads no data other than the local
 * player's own position/orientation and the world's spawn point, both
 * already visible to the player via the F3 debug screen. It performs no
 * automation and does not bypass anything a server could reasonably
 * consider a cheat.
 */
public final class CoordinateHudRenderer implements HudElement {

	private static final int MARGIN_TOP = 4;
	private static final int BACKGROUND_ARGB = 0x90000000;
	private static final int TEXT_COLOR = 0xFFFFFFFF;

	// Matches vanilla BossBarHud.render(): the first bar's name is drawn at
	// y=3 (12 - the 9px font height) and each following bar adds 19px
	// (9px name + 10px bar+gap). Confirmed against the remapped 1.21.11
	// jar with javap since these constants aren't otherwise exposed.
	private static final int BOSS_BAR_FIRST_Y = 12;
	private static final int BOSS_BAR_ROW_HEIGHT = 19;

	public static void register() {
		HudElementRegistry.addLast(Identifier.of("coordinate-hud", "bar"), new CoordinateHudRenderer());
	}

	@Override
	public void render(DrawContext context, RenderTickCounter tickCounter) {
		MinecraftClient client = MinecraftClient.getInstance();
		PlayerEntity player = client.player;
		World world = client.world;
		if (player == null || world == null || client.options.hudHidden) {
			return;
		}
		// Hide behind any open screen (chest, inventory, death, menus, ...)
		// so the line doesn't draw over GUI content; ChatScreen is exempted
		// so coordinates stay visible while typing.
		if (client.currentScreen != null && !(client.currentScreen instanceof ChatScreen)) {
			return;
		}

		BlockPos pos = player.getBlockPos();

		float yaw = player.getYaw() % 360.0f;
		if (yaw < 0) {
			yaw += 360.0f;
		}
		String direction = CompassDirection.label(yaw);
		// Minecraft yaw has 0=south, 90=west, clockwise; convert to a
		// navigational bearing (0=north, 90=east, clockwise) for display.
		int bearing = Math.round((yaw + 180.0f) % 360.0f) % 360;

		SpawnPoint spawnPoint = world.getSpawnPoint();
		String spawnText;
		if (spawnPoint.getDimension().equals(world.getRegistryKey())) {
			BlockPos spawnPos = spawnPoint.getPos();
			double distanceToSpawn = SpawnDistance.horizontal(pos.getX(), pos.getZ(), spawnPos.getX(), spawnPos.getZ());
			spawnText = String.format("Spawn:%.0fm", distanceToSpawn);
		} else {
			// World spawn is in a different dimension (e.g. we're in the
			// Nether/End); a same-dimension block distance would be meaningless.
			spawnText = "Spawn:--";
		}

		String line = String.format(
				"%s %d°  X:%d Y:%d Z:%d  %s",
				direction, bearing, pos.getX(), pos.getY(), pos.getZ(), spawnText);

		int screenWidth = context.getScaledWindowWidth();
		int textWidth = client.textRenderer.getWidth(line);
		int x = (screenWidth - textWidth) / 2;
		int y = MARGIN_TOP;

		int bossBarCount = countVisibleBossBars(client, context);
		if (bossBarCount > 0) {
			// Vanilla draws boss bar names top-center starting at y=3, which
			// overlaps this line's default y=4. Drop below every active bar
			// instead, regardless of how many there are.
			y = BOSS_BAR_FIRST_Y + BOSS_BAR_ROW_HEIGHT * bossBarCount;
		}

		context.fill(x - 4, y - 2, x + textWidth + 4, y + client.textRenderer.fontHeight + 2, BACKGROUND_ARGB);
		context.drawTextWithShadow(client.textRenderer, line, x, y, TEXT_COLOR);
	}

	/**
	 * Counts boss bars the way vanilla actually renders them: vanilla stops
	 * drawing once a row's bottom would reach {@code scaledWindowHeight / 3}
	 * (see {@code BossBarHud.render()}), so a server with many bars doesn't
	 * cover the whole screen. Using the raw map size instead would push this
	 * line down for bars that never actually get drawn.
	 */
	static int countVisibleBossBars(MinecraftClient client, DrawContext context) {
		BossBarHud bossBarHud = client.inGameHud.getBossBarHud();
		int total = ((BossBarHudAccessor) bossBarHud).coordinatehud$getBossBars().size();
		int cutoff = context.getScaledWindowHeight() / 3;
		int y = BOSS_BAR_FIRST_Y;
		int visible = 0;
		for (int i = 0; i < total; i++) {
			visible++;
			y += BOSS_BAR_ROW_HEIGHT;
			if (y >= cutoff) {
				break;
			}
		}
		return visible;
	}
}
