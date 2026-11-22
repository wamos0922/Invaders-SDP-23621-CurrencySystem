package entity;

import java.awt.Color;

import engine.Cooldown;
import engine.Core;
import engine.DrawManager.SpriteType;
import engine.soundEngine.Sound;
import engine.soundEngine.SoundManager;

/**
 * Implements a enemy ship, to be destroyed by the player.
 * 
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class EnemyShip extends Entity {
	
	/** Point value of a type A enemy. */
	private static final int A_TYPE_POINTS = 10;
	/** Point value of a type B enemy. */
	private static final int B_TYPE_POINTS = 20;
	/** Point value of a type C enemy. */
	private static final int C_TYPE_POINTS = 30;
	/** Point value of a bonus enemy. */
	private static final int BONUS_TYPE_POINTS = 100;

	/** Act 1 (Earth Escape) point values, row 5 (bottom) to row 1 (top). */
	private static final int SENTRY_DRONE_POINTS = 10;
	private static final int CRAWLER_POINTS = 20;
	private static final int WARDEN_BOT_POINTS = 30;
	private static final int HUNTER_JET_POINTS = 40;
	private static final int OVERSEER_POINTS = 50;

	// Act 1 colours, taken from Earth (design system tokens).
	/** earth-cloud: Overseer. */
	public static final Color EARTH_CLOUD = new Color(0xEEF3F7);
	/** earth-sky: Hunter Jet. */
	public static final Color EARTH_SKY = new Color(0x7FD4FF);
	/** earth-land: Warden Bot. */
	public static final Color EARTH_LAND = new Color(0x5CC86A);
	/** earth-desert: Crawler. */
	public static final Color EARTH_DESERT = new Color(0xE0AA5E);
	/** earth-ocean: Sentry Drone. */
	public static final Color EARTH_OCEAN = new Color(0x4A8CFF);

	/**
	 * Colour the ship returns to once it is no longer a two-hit elite or
	 * shielded (Act 1 Earth colours, Act 2 types from GOG-84).
	 */
	private Color baseColor = Color.WHITE;

	/** Cooldown between sprite changes. */
	private Cooldown animationCooldown;
	/** Checks if the ship has been hit by a bullet. */
	private boolean isDestroyed;
	/** Remaining health; ordinary and bonus ships start with one hit point. */
	private int health = 1;
	/** Values of the ship, in points, when destroyed. */
	private int pointValue;
	/** How long the explosion flickers after a hit, in milliseconds. */
	private static final int FLICKER_TIME = 150;
	/** Time between each flicker on/off switch, in milliseconds. */
	private static final int FLICKER_INTERVAL = 50;
	/** How long the explosion takes to fade away, in milliseconds. */
	private static final int FADE_TIME = 300;
	/** Moment the ship was destroyed. */
	private long destroyedAt;
	/** Special type and ability (GoG, GOG-84); NORMAL for original ships. */
	private EnemyType type = EnemyType.NORMAL;
	/** Hits the shield can still absorb (Beetle), 0 when it has none. */
	private int shield;

	/**
	 * Constructor, establishes the ship's properties.
	 * 
	 * @param positionX
	 *            Initial position of the ship in the X axis.
	 * @param positionY
	 *            Initial position of the ship in the Y axis.
	 * @param spriteType
	 *            Sprite type, image corresponding to the ship.
	 */
	public EnemyShip(final int positionX, final int positionY,
			final SpriteType spriteType) {
		super(positionX, positionY, 12 * 2, 8 * 2, Color.WHITE);

		this.spriteType = spriteType;
		this.animationCooldown = Core.getCooldown(500);
		this.isDestroyed = false;

		switch (this.spriteType) {
		case EnemyShipA1:
		case EnemyShipA2:
			this.pointValue = A_TYPE_POINTS;
			break;
		case EnemyShipB1:
		case EnemyShipB2:
			this.pointValue = B_TYPE_POINTS;
			break;
		case EnemyShipC1:
		case EnemyShipC2:
			this.pointValue = C_TYPE_POINTS;
			break;
		case Overseer1:
		case Overseer2:
			this.pointValue = OVERSEER_POINTS;
			this.baseColor = EARTH_CLOUD;
			break;
		case HunterJet1:
		case HunterJet2:
			this.pointValue = HUNTER_JET_POINTS;
			this.baseColor = EARTH_SKY;
			break;
		case WardenBot1:
		case WardenBot2:
			this.pointValue = WARDEN_BOT_POINTS;
			this.baseColor = EARTH_LAND;
			break;
		case Crawler1:
		case Crawler2:
			this.pointValue = CRAWLER_POINTS;
			this.baseColor = EARTH_DESERT;
			break;
		case SentryDrone1:
		case SentryDrone2:
			this.pointValue = SENTRY_DRONE_POINTS;
			this.baseColor = EARTH_OCEAN;
			break;
		default:
			this.pointValue = 0;
			break;
		}
		setColor(this.baseColor);
	}

	/** Creates an ordinary enemy with configurable health and the same sprite. */
	public EnemyShip(final int positionX, final int positionY,
			final SpriteType spriteType, final int health) {
		this(positionX, positionY, spriteType);
		if (health < 1)
			throw new IllegalArgumentException("Enemy health must be positive");
		this.health = health;
		refreshColor();
	}

	/**
	 * Creates a story mode enemy of a given type (GoG, GOG-84): its own
	 * sprites, colour, points and, for Beetle, a shield.
	 *
	 * @param positionX
	 *            Initial position of the ship in the X axis.
	 * @param positionY
	 *            Initial position of the ship in the Y axis.
	 * @param type
	 *            Enemy type; must not be NORMAL.
	 * @param health
	 *            Hit points once the shield is broken.
	 */
	public EnemyShip(final int positionX, final int positionY,
			final EnemyType type, final int health) {
		this(positionX, positionY, requireSpecial(type).getFrameA(), health);
		this.type = type;
		this.pointValue = type.getPointValue();
		this.shield = type.getShieldHits();
		this.baseColor = type.getColor();
		refreshColor();
	}

	/** Rejects NORMAL, which has no sprites of its own. */
	private static EnemyType requireSpecial(final EnemyType type) {
		if (type == null || !type.hasOwnSprites())
			throw new IllegalArgumentException(
					"Use the SpriteType constructor for normal enemies");
		return type;
	}

	/**
	 * Shield first, then extra health (yellow), then the base colour, so the
	 * player can see how many hits are left.
	 */
	private void refreshColor() {
		if (this.shield > 0)
			setColor(EnemyType.SHIELD_COLOR);
		else if (this.health > 1)
			setColor(Color.YELLOW);
		else
			setColor(this.baseColor);
	}

	/** @return Special type of this enemy, NORMAL for original ships. */
	public final EnemyType getType() {
		return this.type;
	}

	/** @return Hits the shield can still absorb, 0 when it is down. */
	public final int getShield() {
		return this.shield;
	}

	/**
	 * Applies damage and reports a lethal hit. The formation/caller still owns
	 * destruction and rewards, so a surviving hit cannot remove a shooter.
	 */
	public final boolean takeDamage(final int damage) {
		if (damage <= 0 || this.isDestroyed || this.health == 0)
			return false;
		// Beetle's shield absorbs the whole hit (GOG-84).
		if (this.shield > 0) {
			this.shield--;
			refreshColor();
			return false;
		}
		this.health = Math.max(0, this.health - damage);
		refreshColor();
		return this.health == 0;
	}

	/** @return Remaining hit points. */
	public final int getHealth() {
		return this.health;
	}

	/**
	 * Constructor, establishes the ship's properties for a special ship, with
	 * known starting properties.
	 */
	public EnemyShip() {
		super(-32, 60, 16 * 2, 7 * 2, Color.RED);

		this.spriteType = SpriteType.EnemyShipSpecial;
		this.isDestroyed = false;
		this.pointValue = BONUS_TYPE_POINTS;
	}

	/**
	 * Getter for the score bonus if this ship is destroyed.
	 * 
	 * @return Value of the ship.
	 */
	public final int getPointValue() {
		return this.pointValue;
	}

	/**
	 * Moves the ship the specified distance.
	 * 
	 * @param distanceX
	 *            Distance to move in the X axis.
	 * @param distanceY
	 *            Distance to move in the Y axis.
	 */
	public final void move(final int distanceX, final int distanceY) {
		this.positionX += distanceX;
		this.positionY += distanceY;
	}

	/**
	 * Updates attributes, mainly used for animation purposes.
	 */
	public final void update() {
		if (this.animationCooldown.checkFinished()) {
			this.animationCooldown.reset();

			switch (this.spriteType) {
			case EnemyShipA1:
				this.spriteType = SpriteType.EnemyShipA2;
				break;
			case EnemyShipA2:
				this.spriteType = SpriteType.EnemyShipA1;
				break;
			case EnemyShipB1:
				this.spriteType = SpriteType.EnemyShipB2;
				break;
			case EnemyShipB2:
				this.spriteType = SpriteType.EnemyShipB1;
				break;
			case EnemyShipC1:
				this.spriteType = SpriteType.EnemyShipC2;
				break;
			case EnemyShipC2:
				this.spriteType = SpriteType.EnemyShipC1;
				break;
			case Overseer1:
				this.spriteType = SpriteType.Overseer2;
				break;
			case Overseer2:
				this.spriteType = SpriteType.Overseer1;
				break;
			case HunterJet1:
				this.spriteType = SpriteType.HunterJet2;
				break;
			case HunterJet2:
				this.spriteType = SpriteType.HunterJet1;
				break;
			case WardenBot1:
				this.spriteType = SpriteType.WardenBot2;
				break;
			case WardenBot2:
				this.spriteType = SpriteType.WardenBot1;
				break;
			case Crawler1:
				this.spriteType = SpriteType.Crawler2;
				break;
			case Crawler2:
				this.spriteType = SpriteType.Crawler1;
				break;
			case SentryDrone1:
				this.spriteType = SpriteType.SentryDrone2;
				break;
			case SentryDrone2:
				this.spriteType = SpriteType.SentryDrone1;
				break;
			default:
				// Story types swap between their own two frames.
				if (this.type.hasOwnSprites() && !this.isDestroyed)
					this.spriteType = this.spriteType == this.type.getFrameA()
							? this.type.getFrameB() : this.type.getFrameA();
				break;
			}
		}
	}

	/**
	 * Destroys the ship, causing an explosion.
	 */
	public final void destroy() {
		if (!this.isDestroyed) {
			this.health = 0;
			this.isDestroyed = true;
			this.spriteType = SpriteType.Explosion;
			this.destroyedAt = System.currentTimeMillis();
			SoundManager.getInstance().playSfx(Sound.ENEMY_EXPLOSION);
		}
	}
	
	/** Opacity: 1 = visible, 0 = invisible. Flickers first, then fades. */
	public final float getAlpha() {
		if (!this.isDestroyed)
			return 1f;
		long elapsed = System.currentTimeMillis() - this.destroyedAt;
		if (elapsed < FLICKER_TIME)
			return (elapsed / FLICKER_INTERVAL) % 2 == 0 ? 1f : 0.2f;
		long fading = elapsed - FLICKER_TIME;
		if (fading >= FADE_TIME)
			return 0f;
		return 1f - fading / (float) FADE_TIME;
	}
	
	/** True when the flicker + fade animation is over. */
	public final boolean isFadeFinished() {
		return this.isDestroyed && System.currentTimeMillis()
			- this.destroyedAt >= FLICKER_TIME + FADE_TIME;
	}

	/**
	 * Checks if the ship has been destroyed.
	 *
	 * @return True if the ship has been destroyed.
	 */
	public final boolean isDestroyed() {
		return this.isDestroyed;
	}
}
