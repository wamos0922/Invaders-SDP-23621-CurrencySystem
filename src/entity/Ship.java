package entity;


import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import engine.Cooldown;
import engine.Core;
import engine.DrawManager.SpriteType;

/**
 * Implements a ship, to be controlled by the player.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class Ship extends Entity {

	/** Speed of the bullets shot by the ship. */
	private static final int BULLET_SPEED = -6;
	/** Distance in px from the ship's center to each barrel of a two-way ship. */
	private static final int BARREL_OFFSET = 8;

	/**
	 * Types of player ships. Each type has its own speed, shooting interval
	 * and sprite.
	 */
	/**
	 * Types of player ships. Each type has its own speed, shooting interval
	 * and sprite.
	 */
	public enum ShipType {
		/** Standard ship. */
		STANDARD(2, 750, SpriteType.Ship),
		/** Ship with a high movement speed. */
		FAST_MOVE(4, 750, SpriteType.ShipFastMove),
		/** Ship that shoots a bullet from each of its two barrels. */
		TWO_WAY(1, 1000, SpriteType.ShipTwoWay),
		/** Ship with a high rate of fire. */
		FAST_ATTACK(2, 375, SpriteType.ShipFastAttack),
		/** Ship with a large projectile. */
		BIG_BULLET(2, 1000, SpriteType.ShipBigBullet);

		/** Movement of the ship for each unit of time. */
		private final int speed;
		/** Time between shots, in milliseconds. */
		private final int shootingInterval;
		/** Sprite of the ship while it is not destroyed. */
		private final SpriteType idleSprite;

		/**
		 * Constructor, establishes the properties of the ship type.
		 *
		 * @param speed
		 *            Movement of the ship for each unit of time.
		 * @param shootingInterval
		 *            Time between shots, in milliseconds.
		 * @param idleSprite
		 *            Sprite of the ship while it is not destroyed.
		 */
		ShipType(final int speed, final int shootingInterval,
				 final SpriteType idleSprite) {
			this.speed = speed;
			this.shootingInterval = shootingInterval;
			this.idleSprite = idleSprite;
		}

		/**
		 * Read-only stats, so the Shop and Customize screens can show them
		 * without keeping their own copy (GoG - Currency System).
		 *
		 * @return Movement of the ship for each unit of time.
		 */
		public int getSpeed() {
			return this.speed;
		}

		/** @return Time between shots, in milliseconds. */
		public int getShootingInterval() {
			return this.shootingInterval;
		}

		/** @return Bullets fired per shot. */
		public int getBulletsPerShot() {
			return this == TWO_WAY ? 2 : 1;
		}

		/** @return Sprite of the ship while it is not destroyed. */
		public SpriteType getIdleSprite() {
			return this.idleSprite;
		}
	}

	/** Type of this ship. */
	private final ShipType type;
	/** Sprite drawn while the ship is not destroyed: the type's own, or a shape skin's. */
	private SpriteType idleSprite;

	/**
	 * AUTHORED BY: VFX TEAM (effection)
	 *
	 * Blink settings for low health.
	 */
	private static final int BLINK_INTERVAL = 200;
	private static final Color BASE_COLOR = Color.GREEN;
	private static final Color BLINK_COLOR = new Color(255, 60, 60);

	/** Colour of the ship when not blinking; changed by skins (GoG). */
	private Color skinColor = BASE_COLOR;

	/**
	 * When the ship last fired (0 = not yet). The next shot is allowed once
	 * the current interval has passed since then, so a fire-rate change
	 * (Pickup Rush, Bullet Storm, items) moves the next shot instead of
	 * resetting the timer (GOG-89 review).
	 */
	private long lastShotAt;
	/** Time spent inactive between hits. */
	private Cooldown destructionCooldown;
	/** Extra shots per second from items, added to the base rate (Team CS). */
	private double itemFireRateBonus;
	/** Extra bullet speed from items, added to the base speed (Team CS). */
	private double itemBulletSpeedBonus;
	/** Extra movement speed from items, added to the base speed (Team CS). */
	private int itemMovementSpeedBonus;
	/** Skill tree factors (GOG-89): speed, fire rate, bullet speed, size. */
	private double skillSpeed = 1.0;
	private double skillFireRate = 1.0;
	private double skillBulletSpeed = 1.0;
	private double skillBulletSize = 1.0;
	/** Part of a pixel not moved yet, so +8% speed adds up over frames. */
	private double moveCarry;
	/** Current time between shots, in milliseconds. */
	private int shootingIntervalMillis;
	/** Bullets of the last shot, for skill nodes (GOG-89). */
	private final List<Bullet> lastShot = new ArrayList<Bullet>();

	/**
	 * AUTHORED BY: VFX TEAM (effection)
	 *
	 * Blink state for low health.
	 */
	private Cooldown blinkCooldown;
	private boolean blinking;
	private boolean blinkOn;

	/** Tint of the ship while frozen by a Polar bullet (GoG, GOG-84). */
	private static final Color FROZEN_COLOR = EnemyType.FREEZE_BULLET_COLOR;
	/** Time left frozen: no moving, no shooting (GoG, GOG-84). */
	private Cooldown freezeCooldown;
	/** True while the frozen tint is applied, to restore the colour once. */
	private boolean frozenTint;

	/**
	 * Constructor, establishes the ship's properties.
	 *
	 * @param positionX
	 *            Initial position of the ship in the X axis.
	 * @param positionY
	 *            Initial position of the ship in the Y axis.
	 */
	public Ship(final int positionX, final int positionY) {
		this(positionX, positionY, ShipType.STANDARD);
	}

	/**
	 * Constructor, establishes the ship's properties.
	 *
	 * @param positionX
	 *            Initial position of the ship in the X axis.
	 * @param positionY
	 *            Initial position of the ship in the Y axis.
	 * @param type
	 *            Type of the ship, which defines its speed and sprite.
	 */
	public Ship(final int positionX, final int positionY, final ShipType type) {
		super(positionX, positionY, 13 * 2, 8 * 2, Color.GREEN);

		this.type = type;
		this.idleSprite = type.idleSprite;
		this.spriteType = this.idleSprite;
		this.shootingIntervalMillis = type.shootingInterval;
		this.destructionCooldown = Core.getCooldown(1000);
		this.freezeCooldown = Core.getCooldown(EnemyType.FREEZE_MILLIS);

		/**
		 * AUTHORED BY: VFX TEAM (effection)
		 */
		this.blinkCooldown = Core.getCooldown(BLINK_INTERVAL);
	}

	/**
	 * Moves the ship speed uni ts right, or until the right screen border is
	 * reached.
		 */
	public final void moveRight() {
		this.positionX += nextStep();
	}

	/**
	 * Moves the ship speed units left, or until the left screen border is
	 * reached.
	 	 */
	public final void moveLeft() {
		this.positionX -= nextStep();
	}

	/**
	 * @return Whole pixels to move this frame. Skill bonuses can make the
	 *         speed a fraction (e.g. 4.32), so the leftover is kept for the
	 *         next frame (GOG-89).
	 */
	private int nextStep() {
		double exact = exactSpeed() + this.moveCarry;
		int whole = (int) exact;
		this.moveCarry = exact - whole;
		return whole;
	}

	/** @return Speed in pixels per frame, with item and skill bonuses. */
	private double exactSpeed() {
		return (this.type.speed + this.itemMovementSpeedBonus)
				* this.skillSpeed;
	}

	/**
	 * Shoots a bullet upwards. A two-way ship shoots one bullet from each of
	 * its two barrels instead.
	 *
	 * @param bullets
	 *            List of bullets on screen, to add the new bullet.
	 * @return Checks if the bullet was shot correctly.
	 */
	public final boolean shoot(final Set<Bullet> bullets) {
		if (isFrozen())
			return false;
		long now = System.currentTimeMillis();
		if (now - this.lastShotAt >= this.shootingIntervalMillis) {
			this.lastShotAt = now;			Set<Bullet> before = new HashSet<Bullet>(bullets);
			final int centerX = positionX + this.width / 2;
			final int bulletSpeed = (int) Math.round(BULLET_SPEED
					* this.skillBulletSpeed)
					- (int) Math.round(this.itemBulletSpeedBonus);
			if (this.type == ShipType.TWO_WAY) {
				bullets.add(BulletPool.getBullet(centerX - BARREL_OFFSET,
						positionY, bulletSpeed));
				bullets.add(BulletPool.getBullet(centerX + BARREL_OFFSET,
						positionY, bulletSpeed));
			} else if (this.type == ShipType.BIG_BULLET) {
				// Spawns a larger projectile (e.g., 9x15 scaled by 2)
								// Spawns a larger projectile (e.g., 9x15 scaled by 2)
				bullets.add(BulletPool.getCustomBullet(centerX, positionY,
						bulletSpeed, (int) Math.round(9 * 2 * this.skillBulletSize),
						(int) Math.round(15 * 2 * this.skillBulletSize)));
			} else {
				bullets.add(BulletPool.getBullet(centerX, positionY,
						bulletSpeed));
			}
			// Remember this shot's bullets for skill nodes (GOG-89).
			this.lastShot.clear();
			for (Bullet bullet : bullets)
				if (!before.contains(bullet))
					this.lastShot.add(bullet);
			return true;
		}
		return false;
	}

	/**
	 * Sets the bonuses items add on top of the base fire rate and bullet
	 * speed (Team CS - Item System). The base values stay unchanged; with
	 * both bonuses at 0 the ship shoots exactly as before.
	 *
	 * @param fireRateBonus
	 *            Extra shots per second.
	 * @param bulletSpeedBonus
	 *            Extra bullet speed in pixels per frame.
	 */
	public final void setItemBonuses(final double fireRateBonus,
									 final double bulletSpeedBonus) {
		setItemBonuses(fireRateBonus, bulletSpeedBonus, 0);
	}

	/**
	 * Same as {@link #setItemBonuses(double, double)}, plus extra movement
	 * speed (e.g. the boost item), added on top of the base speed and rounded
	 * to whole pixels per frame.
	 *
	 * @param fireRateBonus
	 *            Extra shots per second.
	 * @param bulletSpeedBonus
	 *            Extra bullet speed in pixels per frame.
	 * @param movementSpeedBonus
	 *            Extra movement speed in pixels per frame.
	 */
	public final void setItemBonuses(final double fireRateBonus,
									 final double bulletSpeedBonus, final double movementSpeedBonus) {
		this.itemMovementSpeedBonus = (int) Math.round(Math.max(0,
				movementSpeedBonus));
		this.itemBulletSpeedBonus = Math.max(0, bulletSpeedBonus);
		double bonus = Math.max(0, fireRateBonus);
				if (bonus != this.itemFireRateBonus) {
			this.itemFireRateBonus = bonus;
			updateShootingInterval();
		}
	}

	/**
	 * Sets the skill tree bonuses of this ship (GOG-89), as factors: 1.0 is
	 * no change, 1.16 is +16%. Item bonuses still add on top.
	 *
	 * @param speed
	 *            Movement speed factor.
	 * @param fireRate
	 *            Shots per second factor.
	 * @param bulletSpeed
	 *            Bullet speed factor.
	 * @param bulletSize
	 *            Bullet size factor (Big Bullet's shell only).
	 */
	public final void setSkillBonuses(final double speed,
			final double fireRate, final double bulletSpeed,
			final double bulletSize) {
		this.skillSpeed = Math.max(0.1, speed);
		this.skillBulletSpeed = Math.max(0.1, bulletSpeed);
		this.skillBulletSize = Math.max(0.1, bulletSize);
		if (fireRate != this.skillFireRate) {
			this.skillFireRate = Math.max(0.1, fireRate);
			updateShootingInterval();
		}
	}

	/**
	 * (base shots per second x skill factor) + (item bonus) -> time
	 * between shots.
	 */
	private void updateShootingInterval() {
		double perSecond = 1000.0 / this.type.shootingInterval
				* this.skillFireRate + this.itemFireRateBonus;
		this.shootingIntervalMillis = (int) Math.round(1000.0 / perSecond);
	}

	/** @return The bullets of the last shot, left to right (GOG-89). */
	public final List<Bullet> getLastShot() {
		this.lastShot.sort((a, b) -> a.getPositionX() - b.getPositionX());
		return Collections.unmodifiableList(this.lastShot);
	}

	/**
	 * @return Time between shots right now, in milliseconds, with item and
	 *         skill bonuses (GOG-89).
	 */
	public final int getShootingIntervalMillis() {
		return this.shootingIntervalMillis;
	}

	/**
	 * Paints the ship in a skin colour (GoG - Currency System). The

	/**
	 * Paints the ship in a skin colour (GoG - Currency System). The
	 * low-health blink still flashes red and returns to this colour.
	 *
	 * @param color
	 *            Skin colour; null goes back to the standard green.
	 */
	public final void setSkinColor(final Color color) {
		this.skinColor = color == null ? BASE_COLOR : color;
		if (!this.blinkOn && !isFrozen())
			setColor(this.skinColor);
	}

	/**
	 * Draws the ship with another shape (A1 - ship skins). Only the sprite
	 * changes: hitbox, speed and shooting stay those of the ship type.
	 *
	 * @param sprite
	 *            Sprite to use while the ship is not destroyed; null goes
	 *            back to the ship type's own sprite.
	 */
	public final void setShapeSprite(final SpriteType sprite) {
		this.idleSprite = sprite == null ? this.type.idleSprite : sprite;
		if (!isDestroyed())
			this.spriteType = this.idleSprite;
	}

	/** Equipped shape even while the ship is temporarily destroyed. */
	public final SpriteType getIdleSprite() {
		return this.idleSprite;
	}

	/**
	 * Updates status of the ship.
	 */
	public final void update() {
		if (!this.destructionCooldown.checkFinished())
			this.spriteType = SpriteType.ShipDestroyed;
		else
			this.spriteType = this.idleSprite;

		/**
		 * AUTHORED BY: VFX TEAM (effection)
		 *
		 * Toggle color each BLINK_INTERVAL ms.
		 */
		if (this.blinking && this.blinkCooldown.checkFinished()) {
			this.blinkOn = !this.blinkOn;
			setColor(this.blinkOn ? BLINK_COLOR : this.skinColor);
			this.blinkCooldown.reset();
		}

		// Frozen by a Polar bullet: tinted until the freeze ends (GOG-84).
		boolean frozen = isFrozen();
		if (frozen)
			setColor(FROZEN_COLOR);
		else if (this.frozenTint)
			setColor(this.blinkOn ? BLINK_COLOR : this.skinColor);
		this.frozenTint = frozen;
	}

	/**
	 * Freezes the ship for {@link EnemyType#FREEZE_MILLIS}: it can't move
	 * or shoot (GoG, GOG-84). A freeze that lands while the ship is already
	 * frozen is ignored, so the player can't be locked forever.
	 *
	 * @return True if the ship was frozen by this call.
	 */
	public final boolean freeze() {
		if (isFrozen() || isDestroyed())
			return false;
		this.freezeCooldown.reset();
		return true;
	}

	/** @return True while the ship is frozen and can't move or shoot. */
	public final boolean isFrozen() {
		return !this.freezeCooldown.checkFinished();
	}

	/**
	 * Pushes the end of the freeze back by the time the game was paused,
	 * so pausing doesn't use up the freeze.
	 *
	 * @param pausedMilliseconds
	 *            How long the game was paused.
	 */
	public final void resumeAfterPause(final long pausedMilliseconds) {
		this.freezeCooldown.delayBy(pausedMilliseconds);
	}

	/**
	 * AUTHORED BY: VFX TEAM (effection)
	 *
	 * Turns low-health blinking on or off.
	 *
	 * @param blinking
	 *            True to start blinking.
	 */
	public final void setBlinking(final boolean blinking) {
		if (this.blinking == blinking)
			return;
		this.blinking = blinking;
		this.blinkOn = false;
		setColor(this.skinColor);
		this.blinkCooldown.reset();
	}

	/**
	 * Switches the ship to its destroyed state.
	 */
	public final void destroy() {
		this.destructionCooldown.reset();
	}

	/**
	 * Checks if the ship is destroyed.
	 *
	 * @return True if the ship is currently destroyed.
	 */
	public final boolean isDestroyed() {
		return !this.destructionCooldown.checkFinished();
	}

	/**
	 	 * Getter for the ship's speed.
	 *
	 * @return Speed of the ship, including item and skill bonuses, rounded
	 *         to whole pixels per frame.
	 */
	public final int getSpeed() {
		return (int) Math.round(exactSpeed());
	}

	/**
	 * Base movement speed without item bonuses (Team CS - Item System).
	 *
	 * @return Base speed in pixels per frame.
	 */
	public final int getBaseSpeed() {
		return this.type.speed;
	}

	/**
	 * Base fire rate without item bonuses (Team CS - Item System).
	 *
	 * @return Base shots per second.
	 */
	public final double getBaseFireRate() {
		return 1000.0 / this.type.shootingInterval;
	}

	/**
	 * Getter for the ship's type.
	 *
	 * @return Type of the ship.
	 */
	public final ShipType getType() {
		return this.type;
	}
}
