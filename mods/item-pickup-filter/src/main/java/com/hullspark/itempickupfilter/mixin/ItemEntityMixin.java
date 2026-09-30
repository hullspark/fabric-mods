package com.hullspark.itempickupfilter.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.hullspark.itempickupfilter.ItemPickupFilterConfig;

import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;

/**
 * {@code ItemEntity#onPlayerCollision} already no-ops on the client (its
 * very first instruction returns if {@code getEntityWorld().isClient()}),
 * so this mixin only has a real effect where the method actually runs the
 * pickup: the logical server (a dedicated server with this mod installed,
 * or the integrated server in singleplayer). Installing the mod only on
 * the client is therefore harmless rather than broken: our own isClient()
 * guard below mirrors vanilla's and simply never cancels anything there.
 */
@Mixin(ItemEntity.class)
public abstract class ItemEntityMixin {

	@Inject(method = "onPlayerCollision", at = @At("HEAD"), cancellable = true)
	private void hullspark$cancelBlacklistedPickup(PlayerEntity player, CallbackInfo ci) {
		ItemEntity self = (ItemEntity) (Object) this;
		if (self.getEntityWorld().isClient()) {
			return;
		}
		ItemStack stack = self.getStack();
		if (ItemPickupFilterConfig.isBlacklisted(stack.getItem())) {
			ci.cancel();
		}
	}
}
