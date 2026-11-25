package screen;

import java.awt.Color;

import engine.DrawManager;
import skilltree.ShardType;
import skilltree.ShardWallet;

/**

/**
 * Draws shard pictures and the shard bar (icon + count for each rarity)
 * for the skill tree page and later screens (GOG-51).
 *
 * @author GoG - Progression (GOG-70)
 */
final class ShardBar {
	/** Top of the bar. */
	static final int TOP = 80;
	/** Height of the bar. */
	static final int HEIGHT = 30;
	/** Size of a shard icon in the bar. */
	private static final int ICON = 28;
	/** Border colour. */
	private static final Color BORDER = new Color(70, 70, 70);

	private ShardBar() {
	}

	/**
	 * Draws the bar: a box with every shard's picture and count.
	 *
	 * @param draw
	 *            Draw manager.
	 * @param left
	 *            Left edge.
	 * @param right
	 *            Right edge.
	 */
	static void draw(final DrawManager draw, final int left, final int right) {
		draw.drawBox(left, TOP, right - left, HEIGHT, BORDER);
		ShardType[] types = ShardType.values();
		int slot = (right - left) / types.length;
		for (int i = 0; i < types.length; i++) {
			int x = left + i * slot + 8;
			icon(draw, types[i], x, TOP + (HEIGHT - ICON) / 2, ICON);
			draw.drawSmallString(Integer.toString(count(types[i])),
					x + ICON + 4, TOP + 20, types[i].getColor());
		}
	}

	/**
	 * Draws a shard picture, or a coloured box if the picture is missing.
	 *
	 * @param draw
	 *            Draw manager.
	 * @param type
	 *            Shard type.
	 * @param x
	 *            Left edge.
	 * @param y
	 *            Top edge.
	 * @param size
	 *            Width and height.
	 */
	static void icon(final DrawManager draw, final ShardType type,
			final int x, final int y, final int size) {
		if (type.getImage() != null)
			draw.drawImage(type.getImage(), x, y, size, size);
		else
			draw.fillBox(x, y, size, size, type.getColor());
		}

	/**
	 * Draws a shard's animation at the current time (GOG-52). Common has
	 * no animation and is drawn still.
	 *
	 * @param draw
	 *            Draw manager.
	 * @param type
	 *            Shard type.
	 * @param x
	 *            Left edge.
	 * @param y
	 *            Top edge.
	 * @param size
	 *            Width and height.
	 */
	static void animatedIcon(final DrawManager draw, final ShardType type,
			final int x, final int y, final int size) {
		if (type.getFrame(System.currentTimeMillis()) != null)
			draw.drawImage(type.getFrame(System.currentTimeMillis()), x, y,
					size, size);
		else
			draw.fillBox(x, y, size, size, type.getColor());
	}

	/**
		 * @param type
	 *            Shard type.
	 * @return How many the player has (GOG-10 wallet).
	 */
	static int count(final ShardType type) {
		return ShardWallet.getInstance().get(type);
	}
}
