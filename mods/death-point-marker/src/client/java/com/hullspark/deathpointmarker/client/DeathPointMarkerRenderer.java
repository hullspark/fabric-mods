package com.hullspark.deathpointmarker.client;

import java.util.Optional;

import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.BossBarHud;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.GlobalPos;
import net.minecraft.world.World;

import com.hullspark.deathpointmarker.ArrivedDeathPoint;
import com.hullspark.deathpointmarker.DeathPointGeometry;
import com.hullspark.deathpointmarker.client.mixin.BossBarHudAccessor;

/**
 * Draws a single-line HUD bar with the bearing, distance and coordinates
 * back to the player's last death point, once the server has one recorded
 * for this player (vanilla's own {@link PlayerEntity#getLastDeathPos()} —
 * the same data the vanilla Recovery Compass points at). Using this
 * instead of watching for the death screen means it also works with the
 * {@code doImmediateRespawn} gamerule (which skips the death screen
 * entirely) and survives relogging: the death position itself is
 * server-authoritative, per-player data. (This mod separately records,
 * locally, which death point has already been recovered — see
 * {@link DeathPointStateStore} below — so that part is not purely
 * server-driven.)
 *
 * Anchored top-left, but its line can get long enough (coordinates plus
 * bearing) that its right edge reaches into the top-center strip the
 * vanilla boss bar and this lane's own coordinate-hud both use.
 * coordinate-hud's own line drops further down the more boss bars are
 * active (see coordinate-hud's {@code CoordinateHudRenderer}), so when
 * that happens this line drops below wherever coordinate-hud's line
 * would land instead of overlapping it (see {@code render} below). Also
 * hidden while the player is in a different dimension than the one they
 * died in, or while any screen other than chat is open (see
 * {@code render} below).
 *
 * Once the player comes within recovery range (3 blocks) of a death
 * point, that point is recorded (via {@link DeathPointStateStore}, which
 * persists it to disk so it survives restarting the game) and the line
 * stays hidden even after walking away again, until the server reports a
 * different death point (the next death).
 *
 * Purely a display feature built from the player's own position and data
 * the vanilla server already sends the client for the Recovery Compass;
 * it performs no automation and does not bypass anything a server could
 * reasonably consider a cheat.
 */
public final class DeathPointMarkerRenderer implements HudElement {

	private static final int MARGIN = 4;
	private static final int BACKGROUND_ARGB = 0x90000000;
	private static final int TEXT_COLOR = 0xFFFFFFFF;
	private static final double ARRIVED_RADIUS = 3.0;

	// A worst-case rendering of coordinate-hud's line (max compass label,
	// max bearing digits, world-border-edge coordinates, max Spawn
	// distance), measured with the real font at render time below instead
	// of a guessed pixel width. This mod doesn't depend on coordinate-hud
	// being installed; it just needs an upper bound on how wide that line
	// could ever be so this one knows how far the top-center strip reaches.
	private static final String WORST_CASE_COORDINATE_HUD_LINE = "SW 359°  X:-29999984 Y:-64 Z:-29999984  Spawn:42426407m";
	// coordinate-hud pads its background 4px past the text on each side.
	private static final int COORDINATE_HUD_BACKGROUND_MARGIN = 4;

	// Matches vanilla BossBarHud.render() and coordinate-hud's
	// CoordinateHudRenderer: the first bar row starts at y=12, each
	// following row adds 19px, and coordinate-hud's own line lands at
	// 12 + 19*n once n bars are showing. This line drops one row further,
	// below wherever coordinate-hud's line would be.
	private static final int BOSS_BAR_FIRST_Y = 12;
	private static final int BOSS_BAR_ROW_HEIGHT = 19;

	public static void register() {
		HudElementRegistry.addLast(Identifier.of("death-point-marker", "bar"), new DeathPointMarkerRenderer());
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

		Optional<GlobalPos> lastDeathPos = player.getLastDeathPos();
		if (lastDeathPos.isEmpty()) {
			return;
		}
		GlobalPos death = lastDeathPos.get();
		if (!death.dimension().equals(world.getRegistryKey())) {
			return;
		}

		BlockPos pos = player.getBlockPos();
		BlockPos deathPos = death.pos();
		String dimensionId = death.dimension().getValue().toString();
		String worldId = worldId(client);

		Optional<ArrivedDeathPoint> arrived = DeathPointStateStore.getArrived();
		if (arrived.isPresent()
				&& arrived.get().matches(worldId, dimensionId, deathPos.getX(), deathPos.getY(), deathPos.getZ())) {
			// Already recovered this exact death point in this exact
			// world/server; stay hidden until the server reports a
			// different one (the next death).
			return;
		}

		double distance = DeathPointGeometry.distance(
				pos.getX(), pos.getY(), pos.getZ(), deathPos.getX(), deathPos.getY(), deathPos.getZ());
		if (distance <= ARRIVED_RADIUS) {
			DeathPointStateStore.markArrived(worldId, dimensionId, deathPos.getX(), deathPos.getY(), deathPos.getZ());
			return;
		}

		double dx = deathPos.getX() - pos.getX();
		double dz = deathPos.getZ() - pos.getZ();
		// Same convention as Entity#getRotationVector: yaw 0 = south (+Z),
		// increasing clockwise towards west (+90). Invert atan2 to solve for
		// the yaw that points from the player towards the death point.
		float yawToDeath = (float) (Math.toDegrees(Math.atan2(-dx, dz)));
		if (yawToDeath < 0) {
			yawToDeath += 360.0f;
		}
		String direction = CompassDirection.label(yawToDeath);
		int bearing = Math.round((yawToDeath + 180.0f) % 360.0f) % 360;

		String line = String.format(
				"Death: %s %d° %.0fm  X:%d Y:%d Z:%d",
				direction, bearing, distance, deathPos.getX(), deathPos.getY(), deathPos.getZ());

		int x = MARGIN;
		int y = MARGIN;

		int textWidth = client.textRenderer.getWidth(line);
		int screenWidth = context.getScaledWindowWidth();
		int centerStripHalfWidth = client.textRenderer.getWidth(WORST_CASE_COORDINATE_HUD_LINE) / 2
				+ COORDINATE_HUD_BACKGROUND_MARGIN;
		// +4 for this line's own right-side background padding, +1 to
		// absorb integer-division rounding, so the two backgrounds can
		// never touch even in the (unreachable in normal play) worst case.
		if (x + textWidth + 4 + 1 > screenWidth / 2 - centerStripHalfWidth) {
			// Same left edge, just lower: one row below wherever
			// coordinate-hud's own line would land (which itself drops
			// below any active boss bars), so it stays out of the
			// top-center strip at every GUI scale and boss bar count.
			int bossBarCount = countVisibleBossBars(client, context);
			y = BOSS_BAR_FIRST_Y + BOSS_BAR_ROW_HEIGHT * (bossBarCount + 1);
		}

		context.fill(x - 4, y - 2, x + textWidth + 4, y + client.textRenderer.fontHeight + 2, BACKGROUND_ARGB);
		context.drawTextWithShadow(client.textRenderer, line, x, y, TEXT_COLOR);
	}

	/**
	 * A best-effort identifier for "which world/server is this", so the
	 * persisted arrived-death-point state (see {@link DeathPointStateStore})
	 * doesn't get confused between two different worlds/servers that happen
	 * to share a dimension + block coordinates for a death. Multiplayer uses
	 * the server address; singleplayer uses the save's folder name under
	 * {@code saves/} (the two namespaces can never collide since they use
	 * different prefixes).
	 *
	 * <p>The save's <em>display</em> name ({@code SaveProperties.getLevelName()},
	 * what's shown in the world list) is deliberately not used here: it isn't
	 * unique — every new world defaults to "New World", and copies/backups
	 * keep their original name — so two different worlds could still collide
	 * under it. The folder name Minecraft actually creates under
	 * {@code saves/} (e.g. "New World (1)") is disambiguated by Minecraft
	 * itself and is unique among the saves that currently exist (deleting a
	 * world and creating a new one with the same display name reuses that
	 * same folder name, so this identifier is not unique across time).
	 */
	private static String worldId(MinecraftClient client) {
		var currentServer = client.getCurrentServerEntry();
		if (currentServer != null) {
			return "server:" + currentServer.address;
		}
		var integratedServer = client.getServer();
		if (integratedServer != null) {
			String saveFolderName = integratedServer.getSavePath(WorldSavePath.ROOT).normalize().getFileName().toString();
			return "singleplayer:" + saveFolderName;
		}
		return "unknown";
	}

	/**
	 * Counts boss bars the way vanilla actually renders them: vanilla stops
	 * drawing once a row's bottom would reach {@code scaledWindowHeight / 3}
	 * (see {@code BossBarHud.render()}), so a server with many bars doesn't
	 * cover the whole screen. Using the raw map size instead would drop
	 * this line further than necessary for bars that never actually get
	 * drawn.
	 */
	private static int countVisibleBossBars(MinecraftClient client, DrawContext context) {
		BossBarHud bossBarHud = client.inGameHud.getBossBarHud();
		int total = ((BossBarHudAccessor) bossBarHud).deathpointmarker$getBossBars().size();
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
