package engine;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

/**
 * Story mode backgrounds, one per part of the journey (GOG-14).
 *
 * <ul>
 * <li>Levels 1-10: space near Earth, escaping the AI (Act 1).</li>
 * <li>Levels 11-20: the first alien planet, a purple storm world (Act 2).</li>
 * <li>Levels 21-30: the second alien planet, a maroon dusk world that looks
 * livable (Act 3).</li>
 * <li>Levels 31-39: deep space (Acts 4 and 5).</li>
 * <li>Level 40: the final battle for Earth, a burning city (Act 5).</li>
 * </ul>
 *
 * The images are 448x520 pixel art in {@code res/backgrounds/}. They are
 * kept dark so enemies, bullets and the player's ship stay easy to see.
 * Endless mode and the tutorial do not use them.
 */
public final class StoryBackgrounds {

	/** Folder of the background images, inside res. */
	private static final String FOLDER = "backgrounds/";

	/** Logger, for images that fail to load. */
	private static final Logger LOGGER = Core.getLogger();

	/** Images already loaded, by file name (null if loading failed). */
	private static final Map<String, BufferedImage> CACHE = new HashMap<>();

	private StoryBackgrounds() {
	}

	/**
	 * Background file for a story level.
	 *
	 * @param level
	 *            Story level, 1 to 40.
	 * @return File name in res/backgrounds, or null outside 1-40.
	 */
	static String fileFor(final int level) {
		if (level < 1 || level > 40)
			return null;
		if (level <= 10)
			return "space_near_earth.png";
		if (level <= 20)
			return "storm_planet.png";
		if (level <= 30)
			return "planet2_dusk.png";
		if (level <= 39)
			return "deep_space.png";
		return "final_battle.png";
	}

	/**
	 * Background image for a story level, loaded once and then reused.
	 *
	 * @param level
	 *            Story level, 1 to 40.
	 * @return The image, or null if the level has none or it can't be loaded
	 *         (the level is then drawn on black, as before).
	 */
	public static synchronized BufferedImage forLevel(final int level) {
		String file = fileFor(level);
		if (file == null)
			return null;
		if (!CACHE.containsKey(file))
			CACHE.put(file, load(file));
		return CACHE.get(file);
	}

	/**
	 * Loads an image from the classpath, or from res/ when running from the
	 * project folder.
	 */
	private static BufferedImage load(final String file) {
		try {
			InputStream in = StoryBackgrounds.class.getClassLoader()
					.getResourceAsStream(FOLDER + file);
			if (in == null) {
				File onDisk = new File("res", FOLDER + file);
				if (!onDisk.isFile())
					throw new IOException("Background not found: " + file);
				return ImageIO.read(onDisk);
			}
			try (InputStream stream = in) {
				return ImageIO.read(stream);
			}
		} catch (IOException | RuntimeException e) {
			LOGGER.warning("Could not load story background " + file + ": "
					+ e);
			return null;
		}
	}
}
