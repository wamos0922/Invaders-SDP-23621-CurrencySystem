package skilltree;

import java.util.List;

import entity.Bullet;
import entity.Entity;

/**
 * The five capstones, node 7 of each Specialty branch (GOG-91): Adaptive
 * Core, Blink Dash, Bullet Storm, Triple Barrel and Shockwave. One instance
 * per level; time is passed in so the rules can be tested.
 *
 * @author GoG - Progression (GOG-70)
 */
public final class SkillCapstones {

	/** Adaptive Core: piercing shots per charge. */
	static final int ADAPTIVE_SHOTS = 5;
	/** Adaptive Core: time to charge again after the last piercing shot. */
	static final long ADAPTIVE_COOLDOWN_MILLIS = 15000;
	/** Bullet Storm: one storm in each period of this length. */
	static final long STORM_EVERY_MILLIS = 20000;
	/** Bullet Storm: how long a storm lasts (end of each period). */
	static final long STORM_MILLIS = 3000;
	/** Bullet Storm: fire rate factor during a storm. */
	static final double STORM_FACTOR = 2.0;
	/** Triple Barrel: every this many volleys add a middle bullet. */
	static final int TRIPLE_EVERY = 3;
	/** Blink Dash: how far a dash goes, in pixels. */
	public static final int DASH_DISTANCE = 80;
	/** Blink Dash: two presses within this time make a double-tap. */
	static final long DASH_TAP_MILLIS = 250;
	/** Blink Dash: time before the next dash. */
	static final long DASH_COOLDOWN_MILLIS = 4000;
	/** Blink Dash: invincible time after a dash. */
	static final long DASH_INVINCIBLE_MILLIS = 250;
	/** Shockwave: neighbours in the same row this close are hit too. */
	static final int SHOCKWAVE_RANGE = 40;

	/** Effects of the unlocked nodes. */
	private final SkillEffects effects;
	/** When play started (Bullet Storm counts from here). */
	private long playStartsAt;
	/** Adaptive Core: piercing shots left in this charge. */
	private int adaptiveLeft = ADAPTIVE_SHOTS;
	/** Adaptive Core: charged again at this time. */
	private long adaptiveReadyAt;
	/** Triple Barrel: volleys fired. */
	private int volleys;
	/** Blink Dash: direction and time of the last tap. */
	private int lastTapDirection;
	private long lastTapAt = Long.MIN_VALUE / 2;
	/** Blink Dash: next dash allowed at this time. */
	private long dashReadyAt;
	/** Blink Dash: invincible until this time. */
	private long dashInvincibleUntil;

	/**
	 * @param effects
	 *            Effects of the ship's unlocked nodes.
	 * @param playStartsAt
	 *            Time the countdown ends.
	 */
	public SkillCapstones(final SkillEffects effects,
			final long playStartsAt) {
		this.effects = effects;
		this.playStartsAt = playStartsAt;
	}

	/**
	 * Adaptive Core: call right after the ship shoots.
	 *
	 * @param shot
	 *            Bullets of the shot.
	 * @param now
	 *            Current time.
	 */
	public void onShot(final List<Bullet> shot, final long now) {
		if (!this.effects.has(SkillTreeCatalog.ADAPTIVE_CORE))
			return;
		if (this.adaptiveLeft == 0 && now >= this.adaptiveReadyAt)
			this.adaptiveLeft = ADAPTIVE_SHOTS;
		if (this.adaptiveLeft == 0)
			return;
		for (Bullet bullet : shot)
			bullet.setPierce(Math.max(bullet.getPierce(), 1));
		this.adaptiveLeft--;
		if (this.adaptiveLeft == 0)
			this.adaptiveReadyAt = now + ADAPTIVE_COOLDOWN_MILLIS;
	}

	/**
	 * Triple Barrel: call once per volley.
	 *
	 * @return True if this volley gets a third bullet up the middle.
	 */
	public boolean addsMiddleBullet() {
		if (!this.effects.has(SkillTreeCatalog.TRIPLE_BARREL))
			return false;
		this.volleys++;
		return this.volleys % TRIPLE_EVERY == 0;
	}

	/**
	 * Bullet Storm.
	 *
	 * @param now
	 *            Current time.
	 * @return 2.0 during the last 3 s of every 20 s of play, otherwise 1.0.
	 */
	public double fireRateFactor(final long now) {
		if (!this.effects.has(SkillTreeCatalog.BULLET_STORM)
				|| now < this.playStartsAt)
			return 1.0;
		long inPeriod = (now - this.playStartsAt) % STORM_EVERY_MILLIS;
		return inPeriod >= STORM_EVERY_MILLIS - STORM_MILLIS ? STORM_FACTOR
				: 1.0;
	}

	/**
	 * Blink Dash: call when left or right is pressed (not held).
	 *
	 * @param direction
	 *            -1 for left, 1 for right.
	 * @param now
	 *            Current time.
	 * @return How far to move the ship now: 0, or +-80 px on a double-tap.
	 */
	public int onDirectionPressed(final int direction, final long now) {
		if (!this.effects.has(SkillTreeCatalog.BLINK_DASH))
			return 0;
		boolean doubleTap = direction == this.lastTapDirection
				&& now - this.lastTapAt <= DASH_TAP_MILLIS;
		this.lastTapDirection = direction;
		this.lastTapAt = now;
		if (!doubleTap || now < this.dashReadyAt)
			return 0;
		this.dashReadyAt = now + DASH_COOLDOWN_MILLIS;
		this.dashInvincibleUntil = now + DASH_INVINCIBLE_MILLIS;
		this.lastTapAt = Long.MIN_VALUE / 2; // a third tap is a new double-tap
		return direction * DASH_DISTANCE;
	}

	/**
	 * @param now
	 *            Current time.
	 * @return True just after a Blink Dash: hits are ignored.
	 */
	public boolean dashInvincible(final long now) {
		return now < this.dashInvincibleUntil;
	}

	/**
	 * The game was paused: the Bullet Storm rhythm, the Adaptive Core
	 * recharge and the Blink Dash cooldown and invincibility all wait for
	 * that long, so pausing doesn't use them up.
	 *
	 * @param pausedMillis
	 *            How long the game was paused.
	 */
	public void resumeAfterPause(final long pausedMillis) {
		if (pausedMillis <= 0)
			return;
		this.playStartsAt += pausedMillis;
		if (this.adaptiveReadyAt > 0)
			this.adaptiveReadyAt += pausedMillis;
		if (this.dashReadyAt > 0)
			this.dashReadyAt += pausedMillis;
		if (this.dashInvincibleUntil > 0)
			this.dashInvincibleUntil += pausedMillis;
	}

	/** @return True if hits should also destroy nearby enemies. */
	public boolean hasShockwave() {
		return this.effects.has(SkillTreeCatalog.SHOCKWAVE);
	}

	/**
	 * Shockwave.
	 *
	 * @param centerX
	 *            Centre of the enemy that was hit.
	 * @param centerY
	 *            Centre of the enemy that was hit.
	 * @param other
	 *            Another enemy.
	 * @return True if the other enemy is right next to the one hit: in the
	 *         same row, left or right, at most 40 px away. Diagonal and
	 *         vertical neighbours are not hit, so it stays a capstone and
	 *         not a screen-clearer.
	 */
	public static boolean isNextTo(final int centerX, final int centerY,
			final Entity other) {
		int dx = other.getPositionX() + other.getWidth() / 2 - centerX;
		int dy = other.getPositionY() + other.getHeight() / 2 - centerY;
		return Math.abs(dy) < other.getHeight() / 2
				&& Math.abs(dx) <= SHOCKWAVE_RANGE;
	}
}
