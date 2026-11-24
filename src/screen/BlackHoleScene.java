package screen;

import entity.Entity;

/**
 * End of Act 1 (GoG - GOG-74): after the AI Warship explodes, a black hole
 * opens where it was and pulls the player's ship in, then the screen fades
 * to black. The story panels after Level 10 carry on from there.
 *
 * Timeline, from the moment the boss is destroyed:
 * <ol>
 * <li>{@link #EXPLOSION_MILLIS}: the boss explosion plays.</li>
 * <li>{@link #OPEN_MILLIS}: the black hole grows to full size.</li>
 * <li>{@link #PULL_MILLIS}: the ship is pulled in, shrinking to nothing.</li>
 * <li>{@link #FADE_MILLIS}: the screen fades to black.</li>
 * </ol>
 *
 * The scene only does the maths; GameScreen draws it and keeps the level
 * running until {@link #isFinished()}.
 */
public final class BlackHoleScene {

	/** Wait for the boss explosion before the hole opens. */
	public static final int EXPLOSION_MILLIS = 1000;
	/** Time for the hole to grow to full size. */
	public static final int OPEN_MILLIS = 800;
	/** Time to pull the ship in. */
	public static final int PULL_MILLIS = 1800;
	/** Time to fade to black at the end. */
	public static final int FADE_MILLIS = 700;
	/** Full radius of the hole's core, in pixels. */
	public static final int MAX_RADIUS = 28;
	/** Spin speed of the swirl, in radians per second. */
	private static final double SPIN_PER_SECOND = 3.0;

	/** Where the hole opens (the boss's centre). */
	private final int centerX;
	/** Where the hole opens (the boss's centre). */
	private final int centerY;
	/** When the boss was destroyed, moved forward by pauses. */
	private long startedAt;
	/** Ship centre when the pull started; set on the first pull frame. */
	private double pullFromX = Double.NaN;
	/** Ship centre when the pull started. */
	private double pullFromY;

	/**
	 * Starts the scene.
	 *
	 * @param centerX
	 *            Where the hole opens.
	 * @param centerY
	 *            Where the hole opens.
	 * @param startedAt
	 *            When the boss was destroyed, in epoch milliseconds.
	 */
	public BlackHoleScene(final int centerX, final int centerY,
			final long startedAt) {
		this.centerX = centerX;
		this.centerY = centerY;
		this.startedAt = startedAt;
	}

	/**
	 * Keeps the scene still while the game is paused.
	 *
	 * @param pausedMilliseconds
	 *            How long the game was paused.
	 */
	public void resumeAfterPause(final long pausedMilliseconds) {
		this.startedAt += pausedMilliseconds;
	}

	/** @return Where the hole opens. */
	public int getCenterX() {
		return this.centerX;
	}

	/** @return Where the hole opens. */
	public int getCenterY() {
		return this.centerY;
	}

	/** @return Milliseconds since the boss was destroyed. */
	private long elapsed(final long now) {
		return Math.max(0, now - this.startedAt);
	}

	/** @return When the pull starts, relative to the start. */
	private static long pullStart() {
		return EXPLOSION_MILLIS + OPEN_MILLIS;
	}

	/** @return When the fade starts, relative to the start. */
	private static long fadeStart() {
		return pullStart() + PULL_MILLIS;
	}

	/**
	 * @param now
	 *            Current time.
	 * @return Radius of the hole's core, 0 before it opens.
	 */
	public int getRadius(final long now) {
		long t = elapsed(now) - EXPLOSION_MILLIS;
		if (t <= 0)
			return 0;
		if (t >= OPEN_MILLIS)
			return MAX_RADIUS;
		return (int) Math.round(MAX_RADIUS * t / (double) OPEN_MILLIS);
	}

	/**
	 * @param now
	 *            Current time.
	 * @return Swirl angle, in radians.
	 */
	public double getSpin(final long now) {
		return elapsed(now) / 1000.0 * SPIN_PER_SECOND;
	}

	/**
	 * @param now
	 *            Current time.
	 * @return Pull progress, from 0 (not started) to 1 (ship swallowed).
	 */
	public double getPullProgress(final long now) {
		double t = (elapsed(now) - pullStart()) / (double) PULL_MILLIS;
		return Math.max(0, Math.min(1, t));
	}

	/**
	 * @param now
	 *            Current time.
	 * @return True while the ship is being pulled or is already gone.
	 */
	public boolean isPulling(final long now) {
		return elapsed(now) >= pullStart();
	}

	/**
	 * @param now
	 *            Current time.
	 * @return Ship size, from 1 (normal) to 0 (swallowed).
	 */
	public double getShipScale(final long now) {
		return 1 - getPullProgress(now);
	}

	/**
	 * @param now
	 *            Current time.
	 * @return Black fade over the screen, from 0 (clear) to 255 (black).
	 */
	public int getFadeAlpha(final long now) {
		double t = (elapsed(now) - fadeStart()) / (double) FADE_MILLIS;
		return (int) Math.round(255 * Math.max(0, Math.min(1, t)));
	}

	/**
	 * @param now
	 *            Current time.
	 * @return True once the screen has faded to black.
	 */
	public boolean isFinished(final long now) {
		return elapsed(now) >= fadeStart() + FADE_MILLIS;
	}

	/**
	 * Pulls the ship towards the hole. Slow at first, then faster, as if
	 * it can't escape.
	 *
	 * @param ship
	 *            Player ship.
	 * @param now
	 *            Current time.
	 */
	public void pull(final Entity ship, final long now) {
		if (!isPulling(now))
			return;
		if (Double.isNaN(this.pullFromX)) {
			this.pullFromX = ship.getPositionX() + ship.getWidth() / 2.0;
			this.pullFromY = ship.getPositionY() + ship.getHeight() / 2.0;
		}
		double p = getPullProgress(now);
		double eased = p * p;
		double x = this.pullFromX + (this.centerX - this.pullFromX) * eased;
		double y = this.pullFromY + (this.centerY - this.pullFromY) * eased;
		ship.setPositionX((int) Math.round(x - ship.getWidth() / 2.0));
		ship.setPositionY((int) Math.round(y - ship.getHeight() / 2.0));
	}
}
