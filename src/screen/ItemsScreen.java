package screen;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import engine.ItemLoadout;
import engine.ItemLoadout.Mode;
import engine.PurchaseManager;
import shop.ShopCatalog;
import shop.ShopCategory;
import shop.ShopItem;

/**
 * Items page (GOG-50), opened from Customize. The player equips items per
 * game mode: 3 slots for Endless and 1 for Story. Items are the old shop
 * upgrades (Cargo Pod, Magnet Core, Life Capsule, Time Crystal) and work
 * on every ship.
 *
 * Choose a slot, press space, then choose an item and press space to put
 * it in that slot (again on the same item takes it out).
 *
 * @author GoG - Progression (GOG-70)
 */
public class ItemsScreen extends Screen {

	/** Return code that opens this screen (see Core). */
	public static final int CODE = 10;
	/** Return code of Customize. */
	private static final int CUSTOMIZE_CODE = 7;

	/** Space between the screen edge and the content. */
	private static final int MARGIN = 16;
	/** Space between boxes. */
	private static final int GAP = 8;
	/** Height of a slot box. */
	private static final int SLOT_HEIGHT = 44;
	/** Height of an item box. */
	private static final int ITEM_HEIGHT = 58;
	/** Top of the Endless panel. */
	private static final int ENDLESS_TOP = 92;
	/** Top of the Story panel. */
	private static final int STORY_TOP = 180;
	/** Top of the owned items list. */
	private static final int ITEMS_TOP = 284;
	/** Colour of unselected borders. */
	private static final Color BORDER = new Color(70, 70, 70);
	/** Dimmed text. */
	private static final Color DIM = new Color(138, 138, 138);
	/** Fill of the selected box. */
	private static final Color SELECTED_FILL = new Color(0, 255, 0, 40);

	/** Which part of the page the cursor is in. */
	private enum Focus {
		/** The Endless slots. */
		ENDLESS,
		/** The Story slot. */
		STORY,
		/** The item list. */
		ITEMS
	}

	/** Keys watched for presses. */
	private final KeyPresses keys;
	/** Every item, in shop order. */
	private final List<ShopItem> items =
			ShopCatalog.getByCategory(ShopCategory.UPGRADE);
	/** Saved loadout. */
	private final ItemLoadout loadout = ItemLoadout.getInstance();
	/** Where the cursor is. */
	private Focus focus = Focus.ENDLESS;
	/** Mode of the slot that items go into. */
	private Mode targetMode = Mode.ENDLESS;
	/** Slot that items go into. */
	private int targetSlot;
	/** Item under the cursor in the list. */
	private int itemIndex;
	/** Message under the list, or null. */
	private String message;

	/**
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second.
	 */
	public ItemsScreen(final int width, final int height, final int fps) {
		super(width, height, fps);
		this.returnCode = CUSTOMIZE_CODE;
		this.keys = new KeyPresses(this.inputManager, KeyEvent.VK_LEFT,
				KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
				KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_S,
				KeyEvent.VK_X, KeyEvent.VK_SPACE, KeyEvent.VK_ENTER,
				KeyEvent.VK_ESCAPE);
	}

	/**
	 * Starts the action.
	 *
	 * @return Next screen code.
	 */
	public final int run() {
		super.run();
		return this.returnCode;
	}

	/** Updates the elements on screen and checks for events. */
	protected final void update() {
		super.update();
		this.keys.startFrame();
		if (this.inputDelay.checkFinished()) {
			handleKeys();
			handleMouse();
		}
		this.keys.endFrame();
		draw();
	}

	// --- Input ---

	private void handleKeys() {
		boolean confirm = this.keys.pressed(KeyEvent.VK_SPACE)
				|| this.keys.pressed(KeyEvent.VK_ENTER);
		if (this.keys.pressed(KeyEvent.VK_ESCAPE)) {
			if (this.focus == Focus.ITEMS) {
				this.focus = this.targetMode == Mode.ENDLESS ? Focus.ENDLESS
						: Focus.STORY;
			} else {
				this.returnCode = CUSTOMIZE_CODE;
				this.isRunning = false;
			}
		} else if (confirm) {
			if (this.focus == Focus.ITEMS)
				equipSelected();
			else
				this.focus = Focus.ITEMS;
		} else if (this.keys.pressed(KeyEvent.VK_X)) {
			clearTarget();
		} else if (this.keys.pressed(KeyEvent.VK_UP)
				|| this.keys.pressed(KeyEvent.VK_W)) {
			moveFocus(-1);
		} else if (this.keys.pressed(KeyEvent.VK_DOWN)
				|| this.keys.pressed(KeyEvent.VK_S)) {
			moveFocus(1);
		} else if (this.keys.pressed(KeyEvent.VK_LEFT)
				|| this.keys.pressed(KeyEvent.VK_A)) {
			moveSideways(-1);
		} else if (this.keys.pressed(KeyEvent.VK_RIGHT)
				|| this.keys.pressed(KeyEvent.VK_D)) {
			moveSideways(1);
		}
	}

	private void handleMouse() {
		if (!this.keys.clicked())
			return;
		int x = this.keys.mouseX();
		int y = this.keys.mouseY();
		for (Mode mode : Mode.values())
			for (int i = 0; i < mode.getSlots(); i++)
				if (slotArea(mode, i).contains(x, y)) {
					selectSlot(mode, i);
					this.focus = Focus.ITEMS;
					return;
				}
		for (int i = 0; i < this.items.size(); i++)
			if (itemArea(i).contains(x, y)) {
				this.focus = Focus.ITEMS;
				this.itemIndex = i;
				equipSelected();
				return;
			}
	}

	private void moveFocus(final int step) {
		Focus[] all = Focus.values();
		this.focus = all[Math.max(0, Math.min(all.length - 1,
				this.focus.ordinal() + step))];
		if (this.focus == Focus.ENDLESS)
			selectSlot(Mode.ENDLESS, Math.min(this.targetSlot,
					Mode.ENDLESS.getSlots() - 1));
		else if (this.focus == Focus.STORY)
			selectSlot(Mode.STORY, 0);
		this.message = null;
	}

	private void moveSideways(final int step) {
		if (this.focus == Focus.ITEMS) {
			this.itemIndex = Math.floorMod(this.itemIndex + step,
					this.items.size());
		} else {
			selectSlot(this.targetMode, Math.floorMod(this.targetSlot + step,
					this.targetMode.getSlots()));
		}
		this.message = null;
	}

	private void selectSlot(final Mode mode, final int slot) {
		this.targetMode = mode;
		this.targetSlot = slot;
		this.message = null;
	}

	/**
	 * Puts the item under the cursor into the selected slot, or takes it
	 * out if it is already there.
	 */
	private void equipSelected() {
		ShopItem item = this.items.get(this.itemIndex);
		if (!PurchaseManager.getInstance().isOwned(item)) {
			this.message = item.getName() + " is locked - buy it in the Shop";
			return;
		}
		String where = this.targetMode.getDisplayName() + " slot "
				+ (this.targetSlot + 1);
		if (item.getRef().equals(this.loadout.get(this.targetMode,
				this.targetSlot))) {
			if (this.loadout.clear(this.targetMode, this.targetSlot))
				this.message = item.getName() + " removed from " + where;
		} else if (this.loadout.equip(this.targetMode, this.targetSlot,
				item.getRef())) {
			this.message = item.getName() + " equipped in " + where;
		} else {
			this.message = "Couldn't save - try again";
		}
	}

	private void clearTarget() {
		if (this.loadout.get(this.targetMode, this.targetSlot) != null
				&& this.loadout.clear(this.targetMode, this.targetSlot))
			this.message = this.targetMode.getDisplayName() + " slot "
					+ (this.targetSlot + 1) + " emptied";
	}

	// --- Layout ---

	private int slotWidth() {
		return (this.width - MARGIN * 2 - 20 - GAP * 2) / 3;
	}

	private Rectangle slotArea(final Mode mode, final int slot) {
		int top = (mode == Mode.ENDLESS ? ENDLESS_TOP : STORY_TOP) + 24;
		return new Rectangle(MARGIN + 10 + slot * (slotWidth() + GAP), top,
				slotWidth(), SLOT_HEIGHT);
	}

	private Rectangle itemArea(final int index) {
		int itemWidth = (this.width - MARGIN * 2 - GAP) / 2;
		return new Rectangle(MARGIN + (index % 2) * (itemWidth + GAP),
				ITEMS_TOP + 14 + (index / 2) * (ITEM_HEIGHT + GAP),
				itemWidth, ITEM_HEIGHT);
	}

	// --- Drawing ---

	private void draw() {
		drawManager.initDrawing(this);
		drawManager.drawScreenTitle(this, "ITEMS");
		drawPanel(Mode.ENDLESS, ENDLESS_TOP, this.focus == Focus.ENDLESS);
		drawPanel(Mode.STORY, STORY_TOP, this.focus == Focus.STORY);
		drawItems();
		drawStatus();
		drawManager.drawKeyHints(this, this.focus == Focus.ITEMS
				? "left/right item | space equip | esc back to slots"
				: "arrows choose slot | space pick item | x empty | esc back");
		drawManager.completeDrawing(this);
	}

	/** One mode: its name, how many are equipped and its slots. */
	private void drawPanel(final Mode mode, final int top,
			final boolean active) {
		int panelHeight = SLOT_HEIGHT + 34;
		drawManager.drawBox(MARGIN, top, this.width - MARGIN * 2, panelHeight,
				active ? Color.GREEN : BORDER);
		drawManager.drawRegularString(mode.getDisplayName().toUpperCase(),
				MARGIN + 10, top + 18, active ? Color.GREEN : Color.WHITE);
		String count = this.loadout.getEquipped(mode).size() + " / "
				+ mode.getSlots() + " EQUIPPED";
		drawManager.drawSmallString(count, this.width - MARGIN - 10
				- drawManager.getSmallStringWidth(count), top + 16, DIM);
		for (int i = 0; i < mode.getSlots(); i++)
			drawSlot(mode, i);
	}

	private void drawSlot(final Mode mode, final int slot) {
		Rectangle area = slotArea(mode, slot);
		boolean target = mode == this.targetMode && slot == this.targetSlot;
		if (target)
			drawManager.fillBox(area.x, area.y, area.width, area.height,
					SELECTED_FILL);
		drawManager.drawBox(area.x, area.y, area.width, area.height,
				target ? Color.GREEN : BORDER);
		String label = "SLOT " + (slot + 1);
		drawManager.drawSmallString(label, area.x + area.width / 2
				- drawManager.getSmallStringWidth(label) / 2, area.y + 14, DIM);
		ShopItem item = itemById(this.loadout.get(mode, slot));
		String name = item == null ? "- EMPTY -" : item.getName().toUpperCase();
		drawManager.drawSmallString(name, area.x + area.width / 2
				- drawManager.getSmallStringWidth(name) / 2, area.y + 32,
				item == null ? DIM : Color.WHITE);
	}

	/** Every item with its description and where it is equipped. */
	private void drawItems() {
		drawManager.drawSmallString("ITEMS (WORK ON EVERY SHIP)", MARGIN,
				ITEMS_TOP + 6, this.focus == Focus.ITEMS ? Color.GREEN : DIM);
		PurchaseManager purchases = PurchaseManager.getInstance();
		for (int i = 0; i < this.items.size(); i++) {
			ShopItem item = this.items.get(i);
			Rectangle area = itemArea(i);
			boolean cursor = this.focus == Focus.ITEMS && i == this.itemIndex;
			boolean owned = purchases.isOwned(item);
			if (cursor)
				drawManager.fillBox(area.x, area.y, area.width, area.height,
						SELECTED_FILL);
			drawManager.drawBox(area.x, area.y, area.width, area.height,
					cursor ? Color.GREEN : BORDER);
			drawManager.drawSmallString(item.getName().toUpperCase(),
					area.x + 8, area.y + 14, owned ? Color.WHITE : DIM);
			int lineY = area.y + 28;
			for (String line : wrap(item.getDescription(), area.width - 16)) {
				drawManager.drawSmallString(line, area.x + 8, lineY,
						new Color(200, 200, 200));
				lineY += 11;
			}
			String tag = !owned ? "LOCKED - BUY IN SHOP"
					: (this.loadout.contains(Mode.ENDLESS, item.getRef())
							? "[ENDLESS] " : "")
					+ (this.loadout.contains(Mode.STORY, item.getRef())
							? "[STORY]" : "");
			drawManager.drawSmallString(tag, area.x + 8,
					area.y + area.height - 5,
					owned ? Color.GREEN : new Color(255, 70, 70));
		}
	}

	private void drawStatus() {
		String text;
		Color color;
		if (this.message != null) {
			text = this.message;
			color = Color.GREEN;
		} else if (this.focus == Focus.ITEMS) {
			text = "Choose an item for " + this.targetMode.getDisplayName()
					+ " slot " + (this.targetSlot + 1);
			color = DIM;
		} else {
			text = "Space to choose an item for this slot";
			color = DIM;
		}
		text = text.toUpperCase();
		drawManager.drawSmallString(text, this.width / 2
				- drawManager.getSmallStringWidth(text) / 2,
				itemArea(this.items.size() - 1).y + ITEM_HEIGHT + 16, color);
	}

	private ShopItem itemById(final String itemId) {
		for (ShopItem item : this.items)
			if (item.getRef().equals(itemId))
				return item;
		return null;
	}

	/**
	 * Splits text into lines that fit a width in the small font.
	 *
	 * @param text
	 *            Text to split.
	 * @param maxWidth
	 *            Widest a line may be.
	 * @return Lines, in order.
	 */
	private List<String> wrap(final String text, final int maxWidth) {
		List<String> lines = new ArrayList<String>();
		StringBuilder line = new StringBuilder();
		for (String word : text.split(" ")) {
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (line.length() > 0
					&& drawManager.getSmallStringWidth(candidate) > maxWidth) {
				lines.add(line.toString());
				line = new StringBuilder(word);
			} else {
				line = new StringBuilder(candidate);
			}
		}
		if (line.length() > 0)
			lines.add(line.toString());
		return lines;
	}
}
