package com.hullspark.chestsearch.client.mixin;

import java.util.Locale;

import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreen;
import net.minecraft.client.input.CharInput;
import net.minecraft.client.input.KeyInput;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.screen.slot.Slot;
import net.minecraft.text.Text;

import com.hullspark.chestsearch.ItemSearchMatcher;

/**
 * Adds a Ctrl+F item search to every vanilla container screen (chests,
 * barrels, shulker boxes, furnaces, the crafting table, ...), since they
 * all go through this one base class. Matching stacks get a green
 * highlight, non-matching ones get dimmed, so the player can spot what
 * they're looking for without reading every tooltip by hand.
 *
 * The Creative inventory is deliberately left untouched (see the
 * {@code keyPressed} guard below): it already has its own vanilla search
 * tab, and its own {@code keyPressed}/{@code charTyped} use hotkeys (e.g.
 * "t") that would otherwise collide with typing into our search box.
 *
 * The mixin class extends {@link Screen} — HandledScreen's real
 * superclass — purely so it can declare a genuine {@code charTyped}
 * override: in this Minecraft version {@code charTyped} is only a
 * default method on the {@code Element}/{@code ParentElement}
 * interfaces and neither {@link HandledScreen} nor {@link Screen}
 * overrides it, so there is no concrete method body anywhere in the
 * target's own class file for {@code @Inject} to attach to. Declaring a
 * real override here is what Mixin actually merges into the target
 * class. The constructor below is never invoked — Mixin discards it —
 * it only exists to satisfy the compiler.
 *
 * Purely a client-side rendering/filtering aid built from data already
 * visible in the open container; it doesn't move, take or affect any
 * item, so it isn't automation and isn't something a server could
 * reasonably flag as a cheat.
 */
@Mixin(HandledScreen.class)
public abstract class HandledScreenMixin extends Screen {

	private static final int MAX_QUERY_LENGTH = 32;
	private static final int MATCH_COLOR = 0x8032CD32;
	private static final int DIM_COLOR = 0xB0000000;
	private static final int BOX_BACKGROUND = 0xC0000000;
	private static final int BOX_TEXT_COLOR = 0xFFFFFFFF;

	protected HandledScreenMixin(Text title) {
		super(title);
	}

	@Unique
	private boolean hullspark$searchActive;

	@Unique
	private final StringBuilder hullspark$query = new StringBuilder();

	@Unique
	private String hullspark$queryLower = "";

	@Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
	private void hullspark$onKeyPressed(KeyInput input, CallbackInfoReturnable<Boolean> cir) {
		if ((Object) this instanceof CreativeInventoryScreen) {
			// Creative already has its own search tab and its own hotkeys
			// (e.g. "t" switches tabs); stay out of its way entirely.
			return;
		}

		int key = input.key();
		boolean ctrlHeld = (input.modifiers() & GLFW.GLFW_MOD_CONTROL) != 0;

		if (key == GLFW.GLFW_KEY_F && ctrlHeld) {
			hullspark$searchActive = !hullspark$searchActive;
			if (!hullspark$searchActive) {
				hullspark$setQuery("");
			}
			cir.setReturnValue(true);
			return;
		}

		if (!hullspark$searchActive) {
			return;
		}

		if (key == GLFW.GLFW_KEY_ESCAPE) {
			hullspark$searchActive = false;
			hullspark$setQuery("");
			cir.setReturnValue(true);
			return;
		}

		if (key == GLFW.GLFW_KEY_BACKSPACE) {
			if (hullspark$query.length() > 0) {
				hullspark$setQuery(hullspark$query.substring(0, hullspark$query.length() - 1));
			}
			cir.setReturnValue(true);
			return;
		}

		// Consume every other key while the search box is active (actual
		// text entry happens in charTyped below) so number keys don't also
		// trigger vanilla's hotbar-swap/quick-move while typing a search.
		cir.setReturnValue(true);
	}

	@Override
	public boolean charTyped(CharInput input) {
		if (!hullspark$searchActive || (Object) this instanceof CreativeInventoryScreen) {
			return super.charTyped(input);
		}
		if (input.isValidChar() && hullspark$query.length() < MAX_QUERY_LENGTH) {
			hullspark$setQuery(hullspark$query + input.asString());
		}
		return true;
	}

	@Inject(method = "drawSlot", at = @At("HEAD"))
	private void hullspark$highlightMatchingSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (!hullspark$searchActive || hullspark$queryLower.isEmpty() || !slot.hasStack()) {
			return;
		}
		if (hullspark$matches(slot.getStack())) {
			// HEAD runs before the vanilla item icon is drawn, so this fill
			// ends up as a highlighted background behind the icon rather
			// than covering it.
			context.fill(slot.x - 1, slot.y - 1, slot.x + 17, slot.y + 17, MATCH_COLOR);
		}
	}

	@Inject(method = "drawSlot", at = @At("RETURN"))
	private void hullspark$dimNonMatchingSlot(DrawContext context, Slot slot, int mouseX, int mouseY, CallbackInfo ci) {
		if (!hullspark$searchActive || hullspark$queryLower.isEmpty() || !slot.hasStack()) {
			return;
		}
		if (!hullspark$matches(slot.getStack())) {
			// RETURN runs after the item icon is drawn, so this fill lands
			// on top of it and actually dims it, instead of just darkening
			// an already-opaque icon's background where it wouldn't show.
			context.fill(slot.x - 1, slot.y - 1, slot.x + 17, slot.y + 17, DIM_COLOR);
		}
	}

	@Inject(method = "renderMain", at = @At("TAIL"))
	private void hullspark$renderSearchBox(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
		// renderMain (not render) is used because some screens built on top
		// of HandledScreen (e.g. the crafting table, furnaces, the survival
		// inventory) override render() to call renderMain() directly
		// without chaining to HandledScreen's own render(), so a render()
		// injection would silently never fire there.
		if (!hullspark$searchActive) {
			return;
		}

		MinecraftClient client = MinecraftClient.getInstance();
		String text = "Search: " + hullspark$query + "_";
		int boxWidth = Math.max(70, client.textRenderer.getWidth(text) + 8);
		// `width` is declared on Screen, not HandledScreen itself, so
		// Mixin's @Shadow can't resolve it there (confirmed the hard way:
		// it crashed the moment any inventory opened). Read it directly
		// since this class now genuinely extends Screen.
		int boxX = this.width - boxWidth - 4;
		int boxY = 4;

		context.fill(boxX, boxY, boxX + boxWidth, boxY + 14, BOX_BACKGROUND);
		context.drawTextWithShadow(client.textRenderer, text, boxX + 4, boxY + 3, BOX_TEXT_COLOR);
	}

	@Unique
	private void hullspark$setQuery(String query) {
		hullspark$query.setLength(0);
		hullspark$query.append(query);
		hullspark$queryLower = query.toLowerCase(Locale.ROOT);
	}

	@Unique
	private boolean hullspark$matches(ItemStack stack) {
		if (ItemSearchMatcher.nameMatches(stack.getName().getString(), hullspark$queryLower)) {
			return true;
		}
		// Also check the item's default name, so a search still finds a
		// stack the player renamed on an anvil (which replaces the
		// display name returned by getName()).
		if (ItemSearchMatcher.nameMatches(stack.getItem().getName().getString(), hullspark$queryLower)) {
			return true;
		}
		// Also check the registry id's path (e.g. "iron_ingot"), so a
		// player whose game language isn't English can still search with
		// a plain English word even though the display name is localized.
		String idPath = Registries.ITEM.getId(stack.getItem()).getPath();
		return ItemSearchMatcher.idPathMatches(idPath, hullspark$queryLower);
	}
}
