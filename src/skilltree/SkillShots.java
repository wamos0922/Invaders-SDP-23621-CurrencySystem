package skilltree;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import entity.Bullet;
import entity.Entity;

/**
 * The Specialty nodes that change shots and hits (GOG-89): Piercing Shell,
 * Hot Rounds, Slipstream, Wide Barrels, Crossfire, Pickup Rush, Near Miss
 * and Heavy Impact. One instance per level; the game calls it when the
 * ship shoots, picks up an item, kills an enemy or hits a boss.
 *
 * @author GoG - Progression (GOG-70)
 */
public final class SkillShots {

	/** Hot Rounds: every this many shots pierces. */
	static final int HOT_ROUNDS_EVERY = 10;
	/** Slipstream: bullet speed factor while moving. */
	static final double SLIPSTREAM_FACTOR = 1.2;
	/** Wide Barrels: sideways speed of each Two-Way bullet. */
	static final int WIDE_BARRELS_SPREAD = 1;
	/** Pickup Rush: how long the fire rate boost lasts. */
	static final long PICKUP_RUSH_MILLIS = 3000;
	/** Pickup Rush: fire rate factor while it lasts. */
	static final double PICKUP_RUSH_FACTOR = 1.2;
	/** Crossfire: extra score for both kills of one volley. */
	static final double CROSSFIRE_BONUS = 0.5;
	/** Heavy Impact: every this many boss hits counts twice. */
	static final int HEAVY_IMPACT_EVERY = 5;
	/** Near Miss: score for a close dodge. */
	public static final int NEAR_MISS_SCORE = 10;
	/** Near Miss: how close (pixels beside the ship) counts. */
	static final int NEAR_MISS_MARGIN = 14;

	/** Effects of the unlocked nodes. */
	private final SkillEffects effects;
	/** Shots fired this level. */
	private int shots;
	/** Number of the last shot, to tell volleys apart. */
	private long volley;
	/** Pickup Rush is on until this time. */
	private long rushUntil;
	/** Boss hits this level. */
	private int bossHits;
	/** Crossfire: score of the first kill of each volley. */
	private final Map<Long, Integer> firstKills = new HashMap<Long, Integer>();

	/**
	 * @param effects
	 *            Effects of the ship's unlocked nodes.
	 */
	public SkillShots(final SkillEffects effects) {
		this.effects = effects;
	}

	/**
	 * Call right after the ship shoots.
	 *
	 * @param shot
	 *            Bullets of the shot, left to right.
	 * @param moving
	 *            True if the ship was moving.
	 */
	public void onShot(final List<Bullet> shot, final boolean moving) {
		this.shots++;
		this.volley++;
		boolean pierce = this.effects.has(SkillTreeCatalog.PIERCING_SHELL)
				|| this.effects.has(SkillTreeCatalog.HOT_ROUNDS)
						&& this.shots % HOT_ROUNDS_EVERY == 0;
		for (int i = 0; i < shot.size(); i++) {
			Bullet bullet = shot.get(i);
			bullet.setVolley(this.volley);
			if (pierce)
				bullet.setPierce(1);
			if (moving && this.effects.has(SkillTreeCatalog.SLIPSTREAM))
				bullet.setSpeed((int) Math.round(bullet.getSpeed()
						* SLIPSTREAM_FACTOR));
			if (shot.size() == 2
					&& this.effects.has(SkillTreeCatalog.WIDE_BARRELS))
				bullet.setSpeedX(i == 0 ? -WIDE_BARRELS_SPREAD
						: WIDE_BARRELS_SPREAD);
		}
	}

	/**
	 * Call when a bullet kills an enemy and would be used up.
	 *
	 * @param bullet
	 *            The bullet.
	 * @return True if the bullet goes on through the enemy (one pierce
	 *         used up), false if it is used up.
	 */
	public boolean passThrough(final Bullet bullet) {
		if (bullet.getPierce() <= 0)
			return false;
		bullet.setPierce(bullet.getPierce() - 1);
		return true;
	}

	/**
	 * Crossfire: call for every kill. When both bullets of a Two-Way volley
	 * kill, both kills get +50% score (added on the second kill).
	 *
	 * @param bullet
	 *            Bullet that killed.
	 * @param points
	 *            Score the kill gave.
	 * @return Extra score to add now.
	 */
	public int crossfireBonus(final Bullet bullet, final int points) {
		if (!this.effects.has(SkillTreeCatalog.CROSSFIRE)
				|| bullet.getVolley() == 0)
			return 0;
		Integer first = this.firstKills.remove(bullet.getVolley());
		if (first == null) {
			if (this.firstKills.size() > 200)
				this.firstKills.clear(); // old volleys that never paired
			this.firstKills.put(bullet.getVolley(), points);
			return 0;
		}
		return (int) Math.round((first + points) * CROSSFIRE_BONUS);
	}

	/**
	 * Pickup Rush: call when an item is picked up.
	 *
	 * @param now
	 *            Current time.
	 */
	public void onItemCollected(final long now) {
		if (this.effects.has(SkillTreeCatalog.PICKUP_RUSH))
			this.rushUntil = now + PICKUP_RUSH_MILLIS;
	}

	/**
	 * @param now
	 *            Current time.
	 * @return Extra fire rate factor right now: 1.2 during Pickup Rush.
	 */
	public double fireRateFactor(final long now) {
		return now < this.rushUntil ? PICKUP_RUSH_FACTOR : 1.0;
	}

	/**
	 * The game was paused: Pickup Rush lasts that much longer, so pausing
	 * doesn't use it up.
	 *
	 * @param pausedMillis
	 *            How long the game was paused.
	 */
	public void resumeAfterPause(final long pausedMillis) {
		if (pausedMillis > 0 && this.rushUntil > 0)
			this.rushUntil += pausedMillis;
	}

	/**
	 * Heavy Impact: call for every boss hit.
	 *
	 * @return True if this hit should count twice.
	 */
	public boolean heavyImpact() {
		if (!this.effects.has(SkillTreeCatalog.HEAVY_IMPACT))
			return false;
		this.bossHits++;
		return this.bossHits % HEAVY_IMPACT_EVERY == 0;
	}

	/**
	 * Near Miss: call for every enemy bullet that did not hit the ship.
	 *
	 * @param bullet
	 *            Enemy bullet, moving down.
	 * @param ship
	 *            Player ship.
	 * @return True once, in the frame the bullet passes the ship's top
	 *         edge close beside it.
	 */
	public boolean nearMiss(final Bullet bullet, final Entity ship) {
		if (!this.effects.has(SkillTreeCatalog.NEAR_MISS)
				|| bullet.getSpeed() <= 0)
			return false;
		int bottom = bullet.getPositionY() + bullet.getHeight();
		int top = ship.getPositionY();
		boolean crossedNow = bottom >= top && bottom - bullet.getSpeed() < top;
		int center = bullet.getPositionX() + bullet.getWidth() / 2;
		boolean close = center >= ship.getPositionX() - NEAR_MISS_MARGIN
				&& center <= ship.getPositionX() + ship.getWidth()
						+ NEAR_MISS_MARGIN;
		return crossedNow && close;
	}
}
