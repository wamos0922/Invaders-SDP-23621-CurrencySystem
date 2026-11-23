package skilltree;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;

/**
 * The five shard rarities used to pay for skill tree nodes (GOG-51).
 * Each one has a colour for text, a still picture from res/shards and,
 * except Common, an animation sheet there (frames of 40 x 40 side by
 * side, GOG-39): Rare 12 frames, Epic 16, Legendary 20, Mythic 24.
 *
 * @author GoG - Progression (GOG-70)
 */
public enum ShardType {
	/** Most common shard. */
	COMMON("Common", new Color(167, 176, 194), "shard_common"),
	/** Second tier. */
	RARE("Rare", new Color(90, 170, 255), "shard_rare"),
	/** Third tier. */
	EPIC("Epic", new Color(217, 140, 255), "shard_epic"),
	/** Fourth tier. */
	LEGENDARY("Legendary", new Color(255, 179, 71), "shard_legendary"),
	/** Rarest shard. */
	MYTHIC("Mythic", new Color(255, 122, 60), "shard_mythic");

	/** Time each animation frame is shown, in milliseconds. */
	private static final int FRAME_MILLIS = 80;

	/** Name shown to the player. */
	private final String displayName;
	/** Colour of the shard's text. */
	private final Color color;
	/** File name inside res/shards, without ".png". */
	private final String fileName;
	/** Still picture, loaded on first use. Null if it could not be read. */
	private BufferedImage image;
	/** Animation frames, loaded on first use. Empty if there is no sheet. */
	private BufferedImage[] frames;
	/** True once loading was tried, so a missing file is read only once. */
	private boolean loadTried;

	ShardType(final String displayName, final Color color,
			final String fileName) {
		this.displayName = displayName;
		this.color = color;
		this.fileName = fileName;
	}

	/** @return Name shown to the player, e.g. "Rare". */
	public String getDisplayName() {
		return this.displayName;
	}

	/** @return Colour used for this shard's text. */
	public Color getColor() {
		return this.color;
	}

	/**
	 * @return The shard's still picture from res/shards, or null if it is
	 *         missing (callers then draw a coloured box instead).
	 */
	public BufferedImage getImage() {
		load();
		return this.image;
	}

	/**
	 * @param timeMillis
	 *            Current time, e.g. System.currentTimeMillis().
	 * @return The animation frame to show at that time, or the still
	 *         picture if the shard has no animation (Common).
	 */
	public BufferedImage getFrame(final long timeMillis) {
		load();
		if (this.frames.length == 0)
			return this.image;
		return this.frames[(int) (timeMillis / FRAME_MILLIS
				% this.frames.length)];
	}

	/** Reads the still picture and the animation sheet, once. */
	private void load() {
		if (this.loadTried)
			return;
		this.loadTried = true;
		this.image = read(this.fileName + ".png");
		BufferedImage sheet = read(this.fileName + "_sheet.png");
		if (sheet == null || sheet.getHeight() <= 0) {
			this.frames = new BufferedImage[0];
			return;
		}
		// Square frames side by side: frame size = sheet height.
		int size = sheet.getHeight();
		int count = sheet.getWidth() / size;
		this.frames = new BufferedImage[count];
		for (int i = 0; i < count; i++)
			this.frames[i] = sheet.getSubimage(i * size, 0, size, size);
	}

	/**
	 * @param name
	 *            File name inside res/shards.
	 * @return The picture, or null if it is missing or unreadable.
	 */
	private static BufferedImage read(final String name) {
		try (InputStream in = ShardType.class.getClassLoader()
				.getResourceAsStream("shards/" + name)) {
			return in == null ? null : ImageIO.read(in);
		} catch (IOException e) {
			return null;
		}
	}
}
