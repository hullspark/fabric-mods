package com.hullspark.deathlog;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.entity.LivingEntity;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

/**
 * Records every player death (server-authoritative: fires wherever world
 * logic actually runs, the integrated server in singleplayer or a dedicated
 * server running this mod) and exposes the history with {@code /deathlog}.
 *
 * <p>Purely a display feature built from the vanilla death message and the
 * player's own position; it performs no automation and does not bypass
 * anything a server could reasonably consider a cheat.
 */
public final class DeathLogMod implements ModInitializer {
	public static final Logger LOGGER = LoggerFactory.getLogger("death-log");

	/**
	 * How many of a player's most recent deaths /deathlog prints. Equal to
	 * {@link DeathLogStore#MAX_RECORDS_PER_PLAYER} so every death that gets
	 * persisted is also reachable through the command (found in review:
	 * an earlier, smaller display limit meant the oldest half of what was
	 * stored could never actually be seen).
	 */
	private static final int DISPLAY_LIMIT = DeathLogStore.MAX_RECORDS_PER_PLAYER;

	private static volatile DeathLogStore store;

	private final DeathLogStore.Listener storeListener =
			(message, cause) -> LOGGER.warn(message, cause);

	/**
	 * The vanilla death message (the same text the death screen and chat
	 * show, e.g. "fell from a high place" rather than the generic
	 * "died"/"hit the ground too hard") captured per player right before
	 * death actually happens. {@code ServerPlayerEntity#onDeath} resets
	 * the entity's damage tracker before {@code ServerLivingEntityEvents
	 * .AFTER_DEATH} fires, so by then {@code getDamageTracker()
	 * .getDeathMessage()} has already gone back to a generic fallback
	 * (found via real-device playtest: every death recorded as "<name>
	 * died" regardless of cause). {@code ALLOW_DEATH} fires earlier,
	 * while the tracker still holds the real message, so this grabs it
	 * there and {@link #onAfterDeath} consumes it.
	 *
	 * <p>Stamped with the server tick it was captured on: {@code
	 * ALLOW_DEATH} also fires for a death a totem of undying (or another
	 * mod's {@code ALLOW_DEATH} handler) ends up preventing, in which case
	 * no {@code AFTER_DEATH} follows and the entry would otherwise sit
	 * here until the player's *next* real death, wrongly reused as that
	 * death's message. A real death's {@code AFTER_DEATH} always lands on
	 * the same tick as its {@code ALLOW_DEATH}, so a tick mismatch means
	 * the entry is stale and {@link #onAfterDeath} falls back instead of
	 * using it.
	 */
	private final Map<UUID, PendingDeathMessage> pendingDeathMessages = new ConcurrentHashMap<>();

	private record PendingDeathMessage(String message, int tick) {
	}

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTING.register(server -> {
			store = DeathLogStore.load(server.getSavePath(WorldSavePath.ROOT).resolve("deathlog.json"), storeListener);
		});
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> {
			store = null;
			pendingDeathMessages.clear();
		});

		ServerLivingEntityEvents.ALLOW_DEATH.register(this::onAllowDeath);
		ServerLivingEntityEvents.AFTER_DEATH.register(this::onAfterDeath);

		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
				dispatcher.register(CommandManager.literal("deathlog").executes(DeathLogMod::runDeathLogCommand)));
	}

	private boolean onAllowDeath(LivingEntity entity, net.minecraft.entity.damage.DamageSource damageSource, float damageAmount) {
		if (entity instanceof ServerPlayerEntity player) {
			String message = player.getDamageTracker().getDeathMessage().getString();
			int tick = player.getEntityWorld().getServer().getTicks();
			pendingDeathMessages.put(player.getUuid(), new PendingDeathMessage(message, tick));
		}
		return true;
	}

	private void onAfterDeath(LivingEntity entity, net.minecraft.entity.damage.DamageSource damageSource) {
		if (!(entity instanceof ServerPlayerEntity player)) {
			return;
		}
		DeathLogStore currentStore = store;
		if (currentStore == null) {
			LOGGER.warn("Death log store was not ready; a death for {} was not recorded.", player.getName().getString());
			return;
		}
		World world = player.getEntityWorld();
		BlockPos pos = player.getBlockPos();
		// ALLOW_DEATH should always have run (and populated this) on this
		// same tick first; damageSource.getDeathMessage(player) is only a
		// fallback for the unlikely case it didn't (e.g. a mod-added death
		// path that skips that event) or the captured entry is stale (see
		// pendingDeathMessages' javadoc), and is a plainer message that
		// won't include combat/fall variants (e.g. "hit the ground too
		// hard" instead of "fell from a high place").
		PendingDeathMessage pending = pendingDeathMessages.remove(player.getUuid());
		String message = pending != null && pending.tick() == player.getEntityWorld().getServer().getTicks()
				? pending.message()
				: damageSource.getDeathMessage(player).getString();
		DeathRecord record = new DeathRecord(
				System.currentTimeMillis(),
				world.getRegistryKey().getValue().toString(),
				pos.getX(), pos.getY(), pos.getZ(),
				message);
		currentStore.recordDeath(player.getUuid(), record);
	}

	private static int runDeathLogCommand(CommandContext<ServerCommandSource> context) {
		ServerPlayerEntity player = context.getSource().getPlayer();
		if (player == null) {
			context.getSource().sendFeedback(() -> Text.literal("/deathlog can only be used by a player."), false);
			return 0;
		}
		DeathLogStore currentStore = store;
		if (currentStore == null) {
			context.getSource().sendFeedback(() -> Text.literal("Death log is not ready yet."), false);
			return 0;
		}
		List<String> lines = DeathLogFormatter.format(currentStore.getRecords(player.getUuid()), DISPLAY_LIMIT);
		for (String line : lines) {
			context.getSource().sendFeedback(() -> Text.literal(line), false);
		}
		return lines.size();
	}
}
