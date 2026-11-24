package engine;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import screen.Screen;

/**
 * Short, simple animations on top of the story backgrounds (GOG-14), so they
 * don't look static. They are kept brief and mostly near the edges and the
 * horizon, so they never compete with enemies, bullets or the ship.
 *
 * The points to animate are found by colour in the background image, so the
 * effects always line up with the art.
 */
public final class StoryBackgroundEffects {

	/** Width and height the background images are drawn at. */
	private static final int ART_W = 448, ART_H = 520;

	private final String file;
	private final List<int[]> stars = new ArrayList<>();
	private final List<int[]> bolts = new ArrayList<>();
	private final List<int[]> beacons = new ArrayList<>();
	private final List<int[]> fires = new ArrayList<>();
	private final List<int[]> eye = new ArrayList<>();
	private final float[][] embers;
	private final long start = System.currentTimeMillis();
	private final Random random = new Random(14);

	/** Next lightning, shooting star or flak time, and its details. */
	private long nextEvent;
	private long eventStart = -1;
	private int eventX, eventY;

	/**
	 * Prepares the effects for a story level.
	 *
	 * @param level
	 *            Story level, 1 to 40.
	 * @param image
	 *            That level's background, from {@link StoryBackgrounds}.
	 */
	public StoryBackgroundEffects(final int level, final BufferedImage image) {
		this.file = StoryBackgrounds.fileFor(level);
		this.embers = "final_battle.png".equals(file) ? new float[34][3] : new float[0][3];
		for (float[] e : embers)
			resetEmber(e, true);
		this.nextEvent = start + 2500;
		if (image == null || file == null)
			return;
		for (int y = 44; y < image.getHeight(); y++) {
			for (int x = 0; x < image.getWidth(); x++) {
				int rgb = image.getRGB(x, y) & 0xFFFFFF;
				int r = rgb >> 16, g = (rgb >> 8) & 0xFF, b = rgb & 0xFF;
				if (isStar(rgb) && random.nextInt(6) == 0)
					stars.add(new int[] { x, y, random.nextInt(4000) });
				else if ((rgb == 0x5e4ea0 || rgb == 0x44377e) && x % 2 == 0 && y % 2 == 0)
					bolts.add(new int[] { x, y });
				else if (rgb == 0x5a2228 && x % 4 == 0 && y % 4 == 0)
					beacons.add(new int[] { x, y, random.nextInt(2000) });
				else if (r >= 200 && g >= 100 && b <= 120 && x % 4 == 0 && y % 4 == 0)
					fires.add(new int[] { x, y, random.nextInt(1000) });      // bright orange flame blocks
				else if (rgb == 0xc8382c && x % 4 == 0 && y % 4 == 0)
					eye.add(new int[] { x, y });
			}
		}
		while (stars.size() > 26)
			stars.remove(random.nextInt(stars.size()));
	}

	/**
	 * How many points each effect found, for tests.
	 *
	 * @return Stars, lightning, beacons, fires and AI-eye points, in that order.
	 */
	int[] counts() {
		return new int[] { stars.size(), bolts.size(), beacons.size(),
				fires.size(), eye.size() };
	}

	/** Star colours used in the five backgrounds. */
	private static boolean isStar(final int rgb) {
		switch (rgb) {
		case 0x6f7fa8: case 0x56658f: case 0x4c5a82:      // space near Earth
		case 0x463870: case 0x3a2d5c:                     // storm planet
		case 0x4a2a34:                                    // planet 2
		case 0x7a9ea4: case 0x62868a: case 0x5a7e86:      // deep space
			return true;
		default:
			return false;
		}
	}

	/**
	 * Draws this frame's effects. Call it right after drawing the background.
	 *
	 * @param drawManager
	 *            Draw manager.
	 * @param screen
	 *            Screen being drawn.
	 */
	public void draw(final DrawManager drawManager, final Screen screen) {
		if (file == null)
			return;
		long now = System.currentTimeMillis();
		float sx = screen.getWidth() / (float) ART_W;
		float sy = screen.getHeight() / (float) ART_H;

		// Twinkling stars: a brighter dot that fades in and out, with a small
		// cross-shaped sparkle at its brightest.
		for (int[] s : stars) {
			double phase = ((now - start + s[2]) % 3200) / 3200.0;
			int a = (int) (230 * Math.max(0, Math.sin(phase * Math.PI * 2)));
			if (a > 6) {
				box(drawManager, sx, sy, s[0] - 2, s[1] - 2, 6, 6, new Color(180, 200, 255, a / 4));
				box(drawManager, sx, sy, s[0] - 1, s[1] - 1, 4, 4, new Color(235, 242, 255, a));
			}
			if (a > 100) {
				Color arm = new Color(210, 225, 255, a - 70);
				box(drawManager, sx, sy, s[0] - 6, s[1], 5, 2, arm);
				box(drawManager, sx, sy, s[0] + 3, s[1], 5, 2, arm);
				box(drawManager, sx, sy, s[0], s[1] - 6, 2, 5, arm);
				box(drawManager, sx, sy, s[0], s[1] + 3, 2, 5, arm);
			}
		}

		switch (file) {
		case "storm_planet.png":
			lightning(drawManager, screen, sx, sy, now);
			break;
		case "planet2_dusk.png":
			for (int[] b : beacons) {                     // slow blink, 1 s on, 1.2 s off
				if ((now - start + b[2]) % 2200 < 1000)
				{
					box(drawManager, sx, sy, b[0] - 3, b[1] - 3, 10, 10, new Color(255, 90, 70, 60));
					box(drawManager, sx, sy, b[0] - 1, b[1] - 1, 6, 6, new Color(255, 130, 110, 220));
				}
			}
			break;
		case "space_near_earth.png":
			shootingStar(drawManager, sx, sy, now, 4000, 3000, 210, 230, 255);
			break;
		case "deep_space.png":
			shootingStar(drawManager, sx, sy, now, 3500, 2500, 140, 230, 235);
			break;
		case "final_battle.png":
			finalBattle(drawManager, sx, sy, now);
			break;
		default:
			break;
		}
	}

	/** Every 5-9 s the bolts brighten twice, with a very faint purple sky flash. */
	private void lightning(final DrawManager dm, final Screen screen,
			final float sx, final float sy, final long now) {
		if (eventStart < 0 && now >= nextEvent)
			eventStart = now;
		if (eventStart < 0)
			return;
		long t = now - eventStart;
		boolean on = t < 110 || (t > 180 && t < 280) || (t > 360 && t < 420);
		if (on) {
			dm.fillBox(0, 0, screen.getWidth(), screen.getHeight(), new Color(170, 150, 255, 28));
			for (int[] b : bolts)
				box(dm, sx, sy, b[0] - 2, b[1] - 2, 6, 6, new Color(170, 150, 255, 40));
			for (int[] b : bolts)
				box(dm, sx, sy, b[0], b[1], 3, 3, new Color(235, 225, 255, 230));
		}
		if (t > 420) {
			eventStart = -1;
			nextEvent = now + 5000 + random.nextInt(4000);
		}
	}

	/**
	 * Every few seconds a faint shooting star crosses diagonally, upper area
	 * only (teal in deep space, white-blue near Earth).
	 */
	private void shootingStar(final DrawManager dm, final float sx, final float sy, final long now,
			final int minGap, final int extraGap, final int r, final int g, final int b) {
		if (eventStart < 0 && now >= nextEvent) {
			eventStart = now;
			eventX = 120 + random.nextInt(300);
			eventY = 60 + random.nextInt(140);
		}
		if (eventStart < 0)
			return;
		long t = now - eventStart;
		float k = t / 900f;
		if (k > 1) {
			eventStart = -1;
			nextEvent = now + minGap + random.nextInt(extraGap);
			return;
		}
		int hx = (int) (eventX - k * 180), hy = (int) (eventY + k * 115);
		float fade = 1 - k * 0.5f;
		box(dm, sx, sy, hx - 3, hy - 3, 9, 9, new Color(r, g, b, (int) (70 * fade)));
		box(dm, sx, sy, hx - 1, hy - 1, 5, 5, new Color(255, 255, 255, (int) (240 * fade)));
		for (int i = 1; i < 24; i++) {
			int a = (int) (210 * (1 - i / 24f) * fade);
			int size = i < 8 ? 4 : 3;
			box(dm, sx, sy, hx + i * 3, hy - i * 2, size, size, new Color(r, g, b, a));
		}
	}

	/** Flickering fires, rising embers, small flak flashes and the AI eye. */
	private void finalBattle(final DrawManager dm, final float sx, final float sy, final long now) {
		for (int[] f : fires) {
			long tick = (now + f[2]) / 120;
			if ((tick * 31 + f[0]) % 3 == 0)
				box(dm, sx, sy, f[0], f[1], 4, 4, new Color(255, 225, 140, 150));
			else if ((tick * 17 + f[1]) % 4 == 0)
				box(dm, sx, sy, f[0], f[1], 4, 4, new Color(90, 30, 15, 110));
		}
		for (float[] e : embers) {
			e[1] -= e[2];
			e[0] += (float) Math.sin((now + e[2] * 1000) / 600.0) * 0.2f;
			if (e[1] < 300)
				resetEmber(e, false);
			int a = Math.max(0, (int) Math.min(220, (e[1] - 300) * 2));
			box(dm, sx, sy, (int) e[0] - 1, (int) e[1] - 1, 5, 5, new Color(255, 120, 40, a / 4));
			box(dm, sx, sy, (int) e[0], (int) e[1], 3, 3, new Color(255, 170, 80, a));
		}
		int pulse = (int) (100 + 90 * Math.sin((now - start) / 500.0));
		for (int[] p : eye)
			box(dm, sx, sy, p[0] - 2, p[1] - 2, 8, 8, new Color(255, 60, 40, pulse / 4));
		for (int[] p : eye)
			box(dm, sx, sy, p[0], p[1], 4, 4, new Color(255, 110, 90, pulse));
		if (eventStart < 0 && now >= nextEvent) {
			eventStart = now;
			eventX = 40 + random.nextInt(370);
			eventY = 225 + random.nextInt(45);
		}
		if (eventStart >= 0) {
			long t = now - eventStart;
			int a = (int) (230 * (1 - t / 450f));
			if (a > 0) {
				Color c = new Color(255, 190, 100, a);
				box(dm, sx, sy, eventX - 4, eventY - 4, 12, 12, new Color(255, 160, 80, a / 4));
				box(dm, sx, sy, eventX - 1, eventY - 1, 6, 6, new Color(255, 245, 210, a));
				int r = t < 150 ? 6 : 12;
				box(dm, sx, sy, eventX - r, eventY, 5, 5, c);
				box(dm, sx, sy, eventX + r, eventY, 5, 5, c);
				box(dm, sx, sy, eventX, eventY - r, 5, 5, c);
				box(dm, sx, sy, eventX, eventY + r, 5, 5, c);
				box(dm, sx, sy, eventX - r * 2 / 3, eventY - r * 2 / 3, 4, 4, c);
				box(dm, sx, sy, eventX + r * 2 / 3, eventY - r * 2 / 3, 4, 4, c);
				box(dm, sx, sy, eventX - r * 2 / 3, eventY + r * 2 / 3, 4, 4, c);
				box(dm, sx, sy, eventX + r * 2 / 3, eventY + r * 2 / 3, 4, 4, c);
			} else {
				eventStart = -1;
				nextEvent = now + 2000 + random.nextInt(2500);
			}
		}
	}

	private void resetEmber(final float[] e, final boolean anywhere) {
		e[0] = random.nextInt(ART_W);
		e[1] = anywhere ? 300 + random.nextInt(120) : 400 + random.nextInt(30);
		e[2] = 0.15f + random.nextFloat() * 0.25f;
	}

	private static void box(final DrawManager dm, final float sx, final float sy,
			final int x, final int y, final int w, final int h, final Color c) {
		dm.fillBox(Math.round(x * sx), Math.round(y * sy),
				Math.max(1, Math.round(w * sx)), Math.max(1, Math.round(h * sy)), c);
	}
}
