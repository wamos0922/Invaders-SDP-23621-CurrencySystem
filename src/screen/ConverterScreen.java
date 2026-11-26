package screen;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;

import skilltree.ShardConverter;
import skilltree.ShardType;
import skilltree.ShardWallet;

/**

/**
 * Shard converter page (GOG-52), opened with the CONVERT button on the
 * skill tree page. The player picks a shard to GIVE and a shard to GET,
 * each shown by its picture: the chosen one is drawn bigger, animated
 * (from the res/shards sheets) and framed in its rarity colour; the others
 * are still and dimmed. The preview in the middle shows the two chosen
 * shards animated, and how many are given and received.
 *
 * Upgrading (small to big) costs 5 for 1 per rarity step; breaking down
 * (big to small) gives 3 per step ({@link ShardConverter}, GOG-90).
 *
 * @author GoG - Progression (GOG-70)
 */
public class ConverterScreen extends Screen {

	/** Return code that opens this screen (see Core). */
	public static final int CODE = 11;

	/** Space between the screen edge and the content. */
	private static final int MARGIN = 16;
	/** Space between tiles. */
	private static final int GAP = 8;
	/** Height of a shard tile. */
	private static final int TILE_HEIGHT = 66;
	/** Top of the GIVE row. */
	private static final int GIVE_TOP = 132;
	/** Top of the preview. */
	private static final int PREVIEW_TOP = 206;
	/** Top of the GET row. */
	private static final int GET_TOP = 300;
	/** Top of the amount row. */
	private static final int AMOUNT_TOP = 376;
	/** Height of the amount row buttons. */
	private static final int BUTTON_HEIGHT = 28;
	/** Colour of unselected borders. */
	private static final Color BORDER = new Color(70, 70, 70);
	/** Dimmed text. */
	private static final Color DIM = new Color(138, 138, 138);
	/** Error text. */
	private static final Color RED = new Color(255, 70, 70);

	/** Which row the cursor is in. */
	private enum Focus {
		/** Shard to give. */
		GIVE,
		/** Shard to get. */
		GET,
		/** -, +, MAX and CONVERT buttons. */
		AMOUNT
	}

	/** Buttons of the amount row, left to right. */
	private static final String[] BUTTONS = { "-", "+", "MAX", "CONVERT" };
	/** Width of each button. */
	private static final int[] BUTTON_WIDTHS = { 34, 34, 56, 110 };
	/** Width of the amount text between "-" and "+". */
	private static final int COUNT_WIDTH = 70;

	/** Keys watched for presses. */
	private final KeyPresses keys;
	/** Row the cursor is in. */
	private Focus focus = Focus.GIVE;
	/** Shard to give. */
	private ShardType give = ShardType.COMMON;
	/** Shard to get. */
	private ShardType get = ShardType.RARE;
	/** How many conversions (each gives the preview amounts once). */
	private int amount = 1;
	/** Selected button in the amount row. */
	private int button = BUTTONS.length - 1;
	/** Message under the buttons, or null. */
	private String message;

	/**
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second.
	 */
	public ConverterScreen(final int width, final int height, final int fps) {
		super(width, height, fps);
		this.returnCode = SkillTreeScreen.CODE;
		this.keys = new KeyPresses(this.inputManager, KeyEvent.VK_LEFT,
				KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
				KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_S,
				KeyEvent.VK_SPACE, KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE);
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

	// --- Conversion (rules in ShardConverter, GOG-90) ---

	/** @return Rarity steps from GIVE to GET; positive = upgrade. */
	private int steps() {
		return this.get.ordinal() - this.give.ordinal();
	}

	/** @return Shards given for one conversion. */
	private int givePerConversion() {
		return ShardConverter.givePerConversion(this.give, this.get);
	}

	/** @return Shards received for one conversion. */
	private int getPerConversion() {
		return ShardConverter.getPerConversion(this.give, this.get);
	}

	/** @return Most conversions the player can afford, at least 1. */
	private int maxAmount() {
		return Math.max(1, ShardConverter.maxConversions(
				ShardWallet.getInstance(), this.give, this.get));
	}

	/** @return True if the player has enough shards to give. */
	private boolean canAfford() {
		return ShardBar.count(this.give) >= givePerConversion() * this.amount;
	}

	// --- Input ---

	private void handleKeys() {
		if (this.keys.pressed(KeyEvent.VK_ESCAPE)) {
			this.returnCode = SkillTreeScreen.CODE;
			this.isRunning = false;
		} else if (this.keys.pressed(KeyEvent.VK_SPACE)
				|| this.keys.pressed(KeyEvent.VK_ENTER)) {
			if (this.focus == Focus.AMOUNT)
				press(this.button);
			else
				this.focus = Focus.values()[this.focus.ordinal() + 1];
		} else if (this.keys.pressed(KeyEvent.VK_UP)
				|| this.keys.pressed(KeyEvent.VK_W)) {
			this.focus = Focus.values()[Math.max(0,
					this.focus.ordinal() - 1)];
		} else if (this.keys.pressed(KeyEvent.VK_DOWN)
				|| this.keys.pressed(KeyEvent.VK_S)) {
			this.focus = Focus.values()[Math.min(Focus.values().length - 1,
					this.focus.ordinal() + 1)];
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
		for (ShardType type : ShardType.values()) {
			if (tileArea(GIVE_TOP, type).contains(x, y)) {
				this.focus = Focus.GIVE;
				pickGive(type);
				return;
			}
			if (tileArea(GET_TOP, type).contains(x, y)) {
				this.focus = Focus.GET;
				pickGet(type);
				return;
			}
		}
		for (int i = 0; i < BUTTONS.length; i++)
			if (buttonArea(i).contains(x, y)) {
				this.focus = Focus.AMOUNT;
				this.button = i;
				press(i);
				return;
			}
	}

	private void moveSideways(final int step) {
		ShardType[] types = ShardType.values();
		if (this.focus == Focus.GIVE) {
			pickGive(types[Math.floorMod(this.give.ordinal() + step,
					types.length)]);
		} else if (this.focus == Focus.GET) {
			// Skip the shard being given: it can't be both.
			int next = Math.floorMod(this.get.ordinal() + step, types.length);
			if (next == this.give.ordinal())
				next = Math.floorMod(next + step, types.length);
			pickGet(types[next]);
		} else {
			this.button = Math.floorMod(this.button + step, BUTTONS.length);
		}
	}

	private void pickGive(final ShardType type) {
		this.give = type;
		if (this.get == type) {
			// Keep GET different: the next rarity up, or down from Mythic.
			ShardType[] types = ShardType.values();
			this.get = types[type.ordinal() < types.length - 1
					? type.ordinal() + 1 : type.ordinal() - 1];
		}
		this.amount = 1;
		this.message = null;
	}

	private void pickGet(final ShardType type) {
		if (type == this.give)
			return;
		this.get = type;
		this.amount = 1;
		this.message = null;
	}

	/** Runs an amount-row button. */
	private void press(final int index) {
		switch (BUTTONS[index]) {
			case "-":
				this.amount = Math.max(1, this.amount - 1);
				this.message = null;
				break;
			case "+":
				this.amount++;
				this.message = null;
				break;
			case "MAX":
				this.amount = maxAmount();
				this.message = null;
								break;
			default:
				convert();
				break;
		}
	}

	/** Converts the chosen amount and says what happened. */
	private void convert() {
		int given = givePerConversion() * this.amount;
		int received = getPerConversion() * this.amount;
		if (!canAfford()) {
			this.message = "Not enough " + this.give.getDisplayName()
					+ " shards (need " + given + ")";
		} else if (ShardConverter.convert(ShardWallet.getInstance(),
				this.give, this.get, this.amount)) {
			this.message = "Converted " + given + " "
					+ this.give.getDisplayName() + " into " + received + " "
					+ this.get.getDisplayName();
			this.amount = 1;
		} else {
			this.message = "Couldn't save - try again";
		}
	}

	// --- Layout ---

	// --- Layout ---

	private Rectangle tileArea(final int top, final ShardType type) {
		int count = ShardType.values().length;
		int tileWidth = (this.width - MARGIN * 2 - GAP * (count - 1)) / count;
		return new Rectangle(MARGIN + type.ordinal() * (tileWidth + GAP),
				top, tileWidth, TILE_HEIGHT);
	}

	private Rectangle buttonArea(final int index) {
		int total = COUNT_WIDTH + GAP * BUTTON_WIDTHS.length;
		for (int w : BUTTON_WIDTHS)
			total += w;
		int x = this.width / 2 - total / 2;
		for (int i = 0; i < index; i++) {
			x += BUTTON_WIDTHS[i] + GAP;
			if (i == 0)
				x += COUNT_WIDTH + GAP; // the amount sits after "-"
		}
		return new Rectangle(x, AMOUNT_TOP, BUTTON_WIDTHS[index],
				BUTTON_HEIGHT);
	}

	// --- Drawing ---

	private void draw() {
		drawManager.initDrawing(this);
		drawManager.drawScreenTitle(this, "CONVERTER");
		ShardBar.draw(drawManager, MARGIN, this.width - MARGIN);
		drawRow("GIVE", GIVE_TOP, this.give, this.focus == Focus.GIVE, null);
		drawPreview();
		drawRow("GET", GET_TOP, this.get, this.focus == Focus.GET,
				this.give);
		drawAmountRow();
		drawMessage();
		drawManager.drawKeyHints(this,
				"up/down row | left/right choose | space select | esc back");
		drawManager.completeDrawing(this);
	}

	/**
	 * A row of the five shard pictures. The chosen one is bigger, framed
	 * in its colour and tagged; the others are dimmed.
	 *
	 * @param label
	 *            GIVE or GET.
	 * @param top
	 *            Top of the tiles.
	 * @param chosen
	 *            Shard chosen in this row.
	 * @param active
	 *            True if the cursor is in this row.
	 * @param blocked
	 *            Shard that can't be picked here (the one given), or null.
	 */
	private void drawRow(final String label, final int top,
			final ShardType chosen, final boolean active,
			final ShardType blocked) {
		// Label with the chosen shard's picture and name next to it.
		int labelY = top - 8;
		drawManager.drawSmallString(label, MARGIN, labelY,
				active ? Color.GREEN : DIM);
		int x = MARGIN + drawManager.getSmallStringWidth(label) + 8;
		ShardBar.animatedIcon(drawManager, chosen, x, labelY - 13, 16);
		drawManager.drawSmallString(chosen.getDisplayName().toUpperCase(),
				x + 20, labelY, chosen.getColor());

		for (ShardType type : ShardType.values()) {
			Rectangle area = tileArea(top, type);
			boolean selected = type == chosen;
			if (selected) {
				drawManager.fillBox(area.x, area.y, area.width, area.height,
						new Color(255, 255, 255, 18));
				drawManager.drawBox(area.x, area.y, area.width, area.height,
						type.getColor());
				drawManager.drawBox(area.x + 1, area.y + 1, area.width - 2,
						area.height - 2, active ? type.getColor() : BORDER);
			} else {
				drawManager.drawBox(area.x, area.y, area.width, area.height,
						BORDER);
			}
			int size = selected ? 36 : 28;
			int iconX = area.x + area.width / 2 - size / 2;
			int iconY = area.y + 4 + (36 - size) / 2;
			if (selected)
				ShardBar.animatedIcon(drawManager, type, iconX, iconY, size);
			else
				ShardBar.icon(drawManager, type, iconX, iconY, size);
			if (!selected)
				// Dim the picture: darker if it can't be chosen at all.
				drawManager.fillBox(area.x + 1, area.y + 1, area.width - 2,
						area.height - 2, new Color(0, 0, 0,
								type == blocked ? 200 : 130));
			String name = type.getDisplayName().toUpperCase();
			drawManager.drawSmallString(name, area.x + area.width / 2
					- drawManager.getSmallStringWidth(name) / 2,
					area.y + 50, selected ? type.getColor() : DIM);
			String count = Integer.toString(ShardBar.count(type));
			drawManager.drawSmallString(count, area.x + area.width / 2
					- drawManager.getSmallStringWidth(count) / 2,
					area.y + 62, selected ? Color.WHITE : DIM);
		}
	}

	/** Big GIVE picture, arrow with UPGRADE / BREAK DOWN, big GET picture. */
	private void drawPreview() {
		int boxWidth = this.width - MARGIN * 2;
		drawManager.drawBox(MARGIN, PREVIEW_TOP, boxWidth, 76, BORDER);
		int big = 48;
		int giveX = MARGIN + boxWidth / 4 - big / 2;
		int getX = MARGIN + boxWidth * 3 / 4 - big / 2;
		ShardBar.animatedIcon(drawManager, this.give, giveX, PREVIEW_TOP + 6,
				big);
		ShardBar.animatedIcon(drawManager, this.get, getX, PREVIEW_TOP + 6,
				big);

		String given = "-" + givePerConversion() * this.amount;
		String received = "+" + getPerConversion() * this.amount;
		drawManager.drawRegularString(given, giveX + big / 2
				- drawManager.getRegularStringWidth(given) / 2,
				PREVIEW_TOP + 70, canAfford() ? Color.WHITE : RED);
		drawManager.drawRegularString(received, getX + big / 2
				- drawManager.getRegularStringWidth(received) / 2,
				PREVIEW_TOP + 70, Color.GREEN);

		boolean upgrade = steps() > 0;
		Color dirColor = upgrade ? new Color(255, 179, 71)
				: new Color(90, 170, 255);
		String direction = upgrade ? "UPGRADE" : "BREAK DOWN";
		String rate = upgrade ? givePerConversion() + " = 1"
				: "1 = " + getPerConversion();
		int center = this.width / 2;
		drawManager.drawSmallString(direction, center
				- drawManager.getSmallStringWidth(direction) / 2,
				PREVIEW_TOP + 24, dirColor);
		drawManager.drawRegularString("->", center
				- drawManager.getRegularStringWidth("->") / 2,
				PREVIEW_TOP + 44, dirColor);
		drawManager.drawSmallString(rate, center
				- drawManager.getSmallStringWidth(rate) / 2,
				PREVIEW_TOP + 60, DIM);
	}

	/** -, amount, +, MAX and CONVERT. */
	private void drawAmountRow() {
		for (int i = 0; i < BUTTONS.length; i++) {
			Rectangle area = buttonArea(i);
			boolean selected = this.focus == Focus.AMOUNT && this.button == i;
			boolean convert = i == BUTTONS.length - 1;
			boolean lit = convert && canAfford();
			if (lit)
				drawManager.fillBox(area.x, area.y, area.width, area.height,
						Color.GREEN);
			else if (selected)
				drawManager.fillBox(area.x, area.y, area.width, area.height,
						new Color(0, 255, 0, 40));
			drawManager.drawBox(area.x, area.y, area.width, area.height,
					selected || lit ? Color.GREEN : BORDER);
			Color text = lit ? Color.BLACK : convert && !canAfford() ? DIM
					: selected ? Color.GREEN : Color.WHITE;
			drawManager.drawRegularString(BUTTONS[i], area.x + area.width / 2
					- drawManager.getRegularStringWidth(BUTTONS[i]) / 2,
					area.y + 19, text);
			if (i == 0) {
				// The amount sits between "-" and "+".
				String count = "X " + this.amount;
				int countLeft = area.x + area.width + GAP;
				drawManager.drawRegularString(count, countLeft + COUNT_WIDTH / 2
						- drawManager.getRegularStringWidth(count) / 2,
						area.y + 19, Color.WHITE);
			}
		}
	}

	private void drawMessage() {
		String text;
		Color color;
				if (this.message != null) {
			text = this.message;
			color = this.message.startsWith("Converted") ? Color.GREEN : RED;
		} else {
			text = "Up: 5 = 1 - Down: 1 = 3 - can't be undone";
			color = DIM;
		}
		text = text.toUpperCase();
		drawManager.drawSmallString(text, this.width / 2
				- drawManager.getSmallStringWidth(text) / 2,
				AMOUNT_TOP + BUTTON_HEIGHT + 20, color);
	}
}
