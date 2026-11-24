package screen;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.event.KeyEvent;
import java.util.List;

import entity.Ship.ShipType;
import shop.ShopCatalog;
import skilltree.NodeState;
import skilltree.ShardType;
import skilltree.SkillBranch;
import skilltree.SkillNode;
import skilltree.SkillTreePlaceholder;

/**
 * Skill tree page of one ship (GOG-51), opened from Customize.
 * Three columns: Armor and Salvage (shared by every ship) and the ship's
 * own Specialty branch. The shard bar is on top and the selected node is
 * explained at the bottom. Q / E switch ship.
 *
 * Uses {@link SkillTreePlaceholder} until the real logic (GOG-88) exists:
 * unlocking only shows a message for now.
 *
 * @author GoG - Progression (GOG-70)
 */
public class SkillTreeScreen extends Screen {

	/** Return code that opens this screen (see Core). */
	public static final int CODE = 9;
	/** Return code of Customize. */
	private static final int CUSTOMIZE_CODE = 7;

	/** Space between the screen edge and the content. */
	private static final int MARGIN = 16;
	/** Width of the Armor and Salvage columns. */
	private static final int SIDE_WIDTH = 108;
	/** Space between columns. */
	private static final int GAP = 8;
	/** Height of one node box. */
	private static final int NODE_HEIGHT = 26;
	/** Distance between two Specialty nodes. */
	private static final int NODE_STEP = 30;
	/** Top of the ship row. */
	private static final int SHIP_TOP = 116;
	/** Top of the node columns. */
	private static final int TREE_TOP = 166;
	/** Top of the detail panel. */
	private static final int DETAIL_TOP = 380;
	/** Colour of unselected borders. */
	private static final Color BORDER = new Color(70, 70, 70);
	/** Colour of locked node borders. */
	private static final Color LOCKED_BORDER = new Color(55, 55, 55);
	/** Fill of owned nodes. */
	private static final Color OWNED_FILL = new Color(0, 255, 0, 30);
	/** Fill of the selected node. */
	private static final Color SELECTED_FILL = new Color(0, 255, 0, 50);
	/** Dimmed text. */
	private static final Color DIM = new Color(138, 138, 138);
	/** Error text. */
	private static final Color RED = new Color(255, 70, 70);

	/** Ship shown when the page opens; set by Customize. */
	private static ShipType startShip = ShipType.STANDARD;

	/** Keys watched for presses. */
	private final KeyPresses keys;
	/** All ships, in Customize order. */
	private final ShipType[] ships = ShipType.values();
	/** Ship whose page is shown. */
	private int shipIndex;
	/** Selected column. */
	private SkillBranch column = SkillBranch.SPECIALTY;
	/** Selected node in the column. */
	private int nodeIndex;
	/** Message under the detail panel, or null. */
	private String message;
	/** Colour of the message. */
	private Color messageColor = Color.GREEN;

	/**
	 * Remembers which ship's page to open, e.g. the one browsed in
	 * Customize.
	 *
	 * @param ship
	 *            Ship to show first.
	 */
	public static void openOn(final ShipType ship) {
		if (ship != null)
			startShip = ship;
	}

	/**
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second.
	 */
	public SkillTreeScreen(final int width, final int height, final int fps) {
		super(width, height, fps);
		this.returnCode = CUSTOMIZE_CODE;
		this.shipIndex = startShip.ordinal();
		this.keys = new KeyPresses(this.inputManager, KeyEvent.VK_LEFT,
				KeyEvent.VK_RIGHT, KeyEvent.VK_UP, KeyEvent.VK_DOWN,
				KeyEvent.VK_A, KeyEvent.VK_D, KeyEvent.VK_W, KeyEvent.VK_S,
				KeyEvent.VK_Q, KeyEvent.VK_E, KeyEvent.VK_SPACE,
				KeyEvent.VK_ENTER, KeyEvent.VK_ESCAPE);
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
		if (this.keys.pressed(KeyEvent.VK_ESCAPE)) {
			this.returnCode = CUSTOMIZE_CODE;
			this.isRunning = false;
		} else if (this.keys.pressed(KeyEvent.VK_SPACE)
				|| this.keys.pressed(KeyEvent.VK_ENTER)) {
			tryUnlock();
		} else if (this.keys.pressed(KeyEvent.VK_Q)) {
			changeShip(-1);
		} else if (this.keys.pressed(KeyEvent.VK_E)) {
			changeShip(1);
		} else if (this.keys.pressed(KeyEvent.VK_UP)
				|| this.keys.pressed(KeyEvent.VK_W)) {
			selectNode(this.column, this.nodeIndex - 1);
		} else if (this.keys.pressed(KeyEvent.VK_DOWN)
				|| this.keys.pressed(KeyEvent.VK_S)) {
			selectNode(this.column, this.nodeIndex + 1);
		} else if (this.keys.pressed(KeyEvent.VK_LEFT)
				|| this.keys.pressed(KeyEvent.VK_A)) {
			changeColumn(-1);
		} else if (this.keys.pressed(KeyEvent.VK_RIGHT)
				|| this.keys.pressed(KeyEvent.VK_D)) {
			changeColumn(1);
		}
	}

	private void handleMouse() {
		if (!this.keys.clicked())
			return;
		int x = this.keys.mouseX();
		int y = this.keys.mouseY();
		if (prevShipArea().contains(x, y)) {
			changeShip(-1);
			return;
		}
		if (nextShipArea().contains(x, y)) {
			changeShip(1);
			return;
		}
		for (SkillBranch branch : SkillBranch.values())
			for (int i = 0; i < branch.size(); i++)
				if (nodeArea(branch, i).contains(x, y)) {
					if (branch == this.column && i == this.nodeIndex)
						tryUnlock();
					else
						selectNode(branch, i);
					return;
				}
	}

	private void changeShip(final int step) {
		this.shipIndex = Math.floorMod(this.shipIndex + step,
				this.ships.length);
		openOn(ship());
		this.message = null;
	}

	private void changeColumn(final int step) {
		SkillBranch[] columns = SkillBranch.values();
		SkillBranch next = columns[Math.floorMod(this.column.ordinal() + step,
				columns.length)];
		// Keep roughly the same height when moving between columns.
		int index = (int) Math.round(this.nodeIndex * (next.size() - 1.0)
				/ Math.max(1, this.column.size() - 1));
		selectNode(next, index);
	}

	private void selectNode(final SkillBranch branch, final int index) {
		this.column = branch;
		this.nodeIndex = Math.max(0, Math.min(branch.size() - 1, index));
		this.message = null;
	}

	/** Placeholder until GOG-88: explains what would happen. */
	private void tryUnlock() {
		switch (state(selected())) {
			case OWNED:
				this.message = "Already unlocked";
				this.messageColor = Color.GREEN;
				break;
			case LOCKED:
				this.message = "Unlock the node above first";
				this.messageColor = DIM;
				break;
			default:
				this.message = "Unlocking comes with GOG-88";
				this.messageColor = DIM;
				break;
		}
	}

	// --- Data (placeholder until GOG-88) ---

	private ShipType ship() {
		return this.ships[this.shipIndex];
	}

	private List<SkillNode> nodes(final SkillBranch branch) {
		return SkillTreePlaceholder.nodes(ship(), branch);
	}

	private SkillNode selected() {
		return nodes(this.column).get(this.nodeIndex);
	}

	private NodeState state(final SkillNode node) {
		return SkillTreePlaceholder.state(ship(), node);
	}

	private int unlockedCount() {
		int count = 0;
		for (SkillBranch branch : SkillBranch.values())
			for (SkillNode node : nodes(branch))
				if (state(node) == NodeState.OWNED)
					count++;
		return count;
	}

	/**
	 * @param type
	 *            Ship.
	 * @return The role name of its Specialty branch.
	 */
	private static String role(final ShipType type) {
		switch (type) {
			case FAST_MOVE:
				return "Interceptor";
			case FAST_ATTACK:
				return "Gunner";
			case TWO_WAY:
				return "Twin Gun";
			case BIG_BULLET:
				return "Cannon";
			default:
				return "All-Rounder";
		}
	}

	// --- Layout ---

	private int columnLeft(final SkillBranch branch) {
		switch (branch) {
			case ARMOR:
				return MARGIN;
			case SPECIALTY:
				return MARGIN + SIDE_WIDTH + GAP;
			default:
				return this.width - MARGIN - SIDE_WIDTH;
		}
	}

	private int columnWidth(final SkillBranch branch) {
		return branch == SkillBranch.SPECIALTY
				? this.width - MARGIN * 2 - SIDE_WIDTH * 2 - GAP * 2
				: SIDE_WIDTH;
	}

	private Rectangle nodeArea(final SkillBranch branch, final int index) {
		int treeHeight = NODE_STEP * (SkillBranch.SPECIALTY.size() - 1)
				+ NODE_HEIGHT;
		int step = branch.size() <= 1 ? 0
				: (treeHeight - NODE_HEIGHT) / (branch.size() - 1);
		return new Rectangle(columnLeft(branch), TREE_TOP + index * step,
				columnWidth(branch), NODE_HEIGHT);
	}

	private Rectangle prevShipArea() {
		return new Rectangle(MARGIN, SHIP_TOP, 30, 30);
	}

	private Rectangle nextShipArea() {
		return new Rectangle(this.width - MARGIN - 30, SHIP_TOP, 30, 30);
	}

	// --- Drawing ---

	private void draw() {
		drawManager.initDrawing(this);
		drawManager.drawScreenTitle(this, "SKILL TREE");
		drawShardBar();
		drawShipRow();
		drawColumns();
		drawDetail();
		drawManager.drawKeyHints(this,
				"arrows move | space unlock | q/e ship | esc back");
		drawManager.completeDrawing(this);
	}

	/** Shard bar across the top. */
	private void drawShardBar() {
		ShardBar.draw(drawManager, MARGIN, this.width - MARGIN);
	}

	/** Arrows, ship picture, name, role and progress. */
	private void drawShipRow() {
		drawManager.drawRegularString("<", prevShipArea().x + 8,
				SHIP_TOP + 20, Color.GREEN);
		drawManager.drawRegularString(">", nextShipArea().x + 10,
				SHIP_TOP + 20, Color.GREEN);
		ShipType type = ship();
		String name = ShopCatalog.shipName(type).toUpperCase();
		String info = role(type) + " - " + unlockedCount() + " / 15 unlocked";
		int[] size = drawManager.getSpriteSize(type.getIdleSprite());
		int spriteWidth = size[0] * 2;
		int textWidth = Math.max(drawManager.getRegularStringWidth(name),
				drawManager.getSmallStringWidth(info));
		int x = this.width / 2 - (spriteWidth + 10 + textWidth) / 2;
		drawManager.drawSpriteScaled(type.getIdleSprite(), x, SHIP_TOP + 7,
				2, Color.GREEN);
		drawManager.drawRegularString(name, x + spriteWidth + 10,
				SHIP_TOP + 14, Color.WHITE);
		drawManager.drawSmallString(info, x + spriteWidth + 10,
				SHIP_TOP + 28, DIM);
	}

	/** Column headers and every node. */
	private void drawColumns() {
		for (SkillBranch branch : SkillBranch.values()) {
			String header = branch == SkillBranch.SPECIALTY
					? role(ship()).toUpperCase()
					: branch.getDisplayName().toUpperCase() + " (ALL)";
			int left = columnLeft(branch);
			drawManager.drawSmallString(header, left + columnWidth(branch) / 2
					- drawManager.getSmallStringWidth(header) / 2,
					TREE_TOP - 6, DIM);
			// Line joining the nodes of the branch.
			Rectangle first = nodeArea(branch, 0);
			Rectangle last = nodeArea(branch, branch.size() - 1);
			drawManager.fillBox(first.x + first.width / 2 - 1, first.y,
					2, last.y + last.height - first.y, new Color(46, 46, 46));
			List<SkillNode> nodes = nodes(branch);
			for (int i = 0; i < nodes.size(); i++)
				drawNode(nodes.get(i), nodeArea(branch, i),
						branch == this.column && i == this.nodeIndex);
		}
	}

	private void drawNode(final SkillNode node, final Rectangle area,
			final boolean selected) {
		NodeState state = state(node);
		Color border = selected ? Color.GREEN
				: state == NodeState.OWNED ? new Color(47, 143, 47)
				: state == NodeState.AVAILABLE ? Color.WHITE : LOCKED_BORDER;
		drawManager.fillBox(area.x, area.y, area.width, area.height,
				Color.BLACK);
		if (selected || state == NodeState.OWNED)
			drawManager.fillBox(area.x, area.y, area.width, area.height,
					selected ? SELECTED_FILL : OWNED_FILL);
		drawManager.drawBox(area.x, area.y, area.width, area.height, border);
		if (selected || node.isCapstone())
			drawManager.drawBox(area.x + 1, area.y + 1, area.width - 2,
					area.height - 2, node.isCapstone() && !selected
							? new Color(255, 179, 71) : border);
		String text = node.getName().toUpperCase();
		Color textColor = state == NodeState.LOCKED ? DIM
				: state == NodeState.OWNED ? new Color(125, 255, 125)
				: Color.WHITE;
		drawManager.drawSmallString(text, area.x + area.width / 2
				- drawManager.getSmallStringWidth(text) / 2, area.y + 17,
				textColor);
	}

	/** Name, effect, cost and status of the selected node. */
	private void drawDetail() {
		SkillNode node = selected();
		NodeState state = state(node);
		int left = MARGIN;
		int width = this.width - MARGIN * 2;
		drawManager.drawBox(left, DETAIL_TOP, width, 72,
				state == NodeState.AVAILABLE ? Color.WHITE : BORDER);
		drawManager.drawRegularString(node.getName(), left + 10,
				DETAIL_TOP + 20, Color.WHITE);
		String where = this.column.isShared()
				? this.column.getDisplayName() + " - all ships"
				: ShopCatalog.shipName(ship()) + " only";
		drawManager.drawSmallString(where, left + width - 10
				- drawManager.getSmallStringWidth(where), DETAIL_TOP + 18,
				DIM);
		drawManager.drawSmallString(node.getDescription(), left + 10,
				DETAIL_TOP + 38, new Color(200, 200, 200));

		// Cost: one icon and number per shard type the node needs.
		int x = left + 10;
		int y = DETAIL_TOP + 50;
		drawManager.drawSmallString("COST", x, y + 13, DIM);
		x += drawManager.getSmallStringWidth("COST") + 8;
		for (ShardType type : ShardType.values()) {
			int cost = node.getCost(type);
			if (cost == 0)
				continue;
			ShardBar.icon(drawManager, type, x, y - 4, 22);
			boolean enough = ShardBar.count(type) >= cost;
			Color color = state == NodeState.OWNED ? DIM
					: enough ? Color.WHITE : RED;
			drawManager.drawSmallString(Integer.toString(cost), x + 23,
					y + 13, color);
			x += 23 + drawManager.getSmallStringWidth(Integer.toString(cost))
					+ 10;
		}
		String status = state == NodeState.OWNED ? "OWNED"
				: state == NodeState.LOCKED ? "LOCKED" : "SPACE TO UNLOCK";
		drawManager.drawSmallString(status, left + width - 10
				- drawManager.getSmallStringWidth(status), y + 13,
				state == NodeState.AVAILABLE ? Color.GREEN : DIM);

		if (this.message != null)
			drawManager.drawSmallString(this.message.toUpperCase(),
					this.width / 2 - drawManager.getSmallStringWidth(
							this.message.toUpperCase()) / 2,
					DETAIL_TOP + 86, this.messageColor);
	}
}
