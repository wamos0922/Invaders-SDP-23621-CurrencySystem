package engine;

import java.awt.image.BufferedImage;
import java.util.HashSet;
import java.util.Set;

/**
 * Checks the story backgrounds (GOG-14): which image each level uses, that
 * every image loads at the game's screen size, and that each background
 * finds what it animates.
 *
 * Run from the project root after compiling src and tests into bin:
 *   java -cp "bin;res" engine.StoryBackgroundsTest     (Windows)
 *   java -cp bin:res engine.StoryBackgroundsTest       (macOS / Linux)
 */
public final class StoryBackgroundsTest {

	private static int checks;

	private static void check(final boolean condition, final String message) {
		checks++;
		if (!condition)
			throw new AssertionError(message);
	}

	public static void main(final String[] args) {
		checkLevelMapping();
		checkImages();
		checkEffects();
		System.out.println("StoryBackgroundsTest: " + checks
				+ " checks passed");
	}

	/** Each part of the story gets its own background, with clean edges. */
	private static void checkLevelMapping() {
		check(StoryBackgrounds.fileFor(0) == null, "no background before level 1");
		check(StoryBackgrounds.fileFor(41) == null, "no background after level 40");
		expect(1, 10, "space_near_earth.png");
		expect(11, 20, "storm_planet.png");
		expect(21, 30, "planet2_dusk.png");
		expect(31, 39, "deep_space.png");
		expect(40, 40, "final_battle.png");
	}

	private static void expect(final int from, final int to, final String file) {
		for (int level = from; level <= to; level++)
			check(file.equals(StoryBackgrounds.fileFor(level)),
					"level " + level + " should use " + file);
	}

	/** All five images load, are 448x520, are distinct, and are cached. */
	private static void checkImages() {
		Set<BufferedImage> seen = new HashSet<>();
		for (int level : new int[] { 1, 11, 21, 31, 40 }) {
			BufferedImage image = StoryBackgrounds.forLevel(level);
			check(image != null, "background for level " + level + " should load");
			check(image.getWidth() == 448 && image.getHeight() == 520,
					"background for level " + level + " should be 448x520");
			check(seen.add(image), "level " + level + " should have its own image");
			check(StoryBackgrounds.forLevel(level) == image,
					"background for level " + level + " should be loaded only once");
		}
		check(StoryBackgrounds.forLevel(5) == StoryBackgrounds.forLevel(1),
				"levels in the same part share one image");
		check(StoryBackgrounds.forLevel(0) == null, "level 0 has no background");
	}

	/** Each background finds the things it animates in its own image. */
	private static void checkEffects() {
		int[] earth = effects(1), storm = effects(11), planet2 = effects(21),
				space = effects(31), finale = effects(40);
		check(earth[0] > 0, "space near Earth should have twinkling stars");
		check(storm[1] > 0, "storm planet should find its lightning bolts");
		check(planet2[2] > 0, "planet 2 should find the spire beacons");
		check(space[0] > 0, "deep space should have twinkling stars");
		check(finale[3] > 0, "final battle should find fires to flicker");
		check(finale[4] > 0, "final battle should find the AI tower's eye");
		for (int[] c : new int[][] { earth, storm, planet2, space })
			check(c[0] <= 26, "at most 26 stars twinkle, to keep it calm");
		check(new StoryBackgroundEffects(0, null).counts()[0] == 0,
				"no effects outside story levels");
	}

	private static int[] effects(final int level) {
		return new StoryBackgroundEffects(level,
				StoryBackgrounds.forLevel(level)).counts();
	}

	private StoryBackgroundsTest() {
	}
}
