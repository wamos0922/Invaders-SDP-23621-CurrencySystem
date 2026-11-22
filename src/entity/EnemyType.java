package entity;

import java.awt.Color;

import engine.DrawManager.SpriteType;

/**
 * Story mode enemy types (GoG - GOG-84).
 *
 * NORMAL keeps the original A/B/C ships, chosen by row. Every other type has
 * its own two animation frames, colour (from the act's design system) and
 * point value; Polar, Jelly and Beetle also have an ability. Which type goes
 * in which row is set per level with
 * {@link engine.GameSettings#withRowTypes(EnemyType...)}. Acts 3 and 4
 * (GOG-85, GOG-86) add their types here.
 */
public enum EnemyType {

	/** Original enemy: no ability, sprite chosen by row. */
	NORMAL(null, null, Color.WHITE, 0, 0),

	/** Act 2 enemy with no ability (jupiter-red-spot). */
	STORM_KING(SpriteType.StormKing1, SpriteType.StormKing2,
			new Color(0xec6a45), 20, 0),

	/** Act 2 enemy with no ability (jupiter-ochre). */
	CLOUD_RAY(SpriteType.CloudRay1, SpriteType.CloudRay2,
			new Color(0xd9a24f), 10, 0),

	/**
	 * Act 2, back row. Freeze bullet: a hit locks the player ship for
	 * {@link #FREEZE_MILLIS} without taking a life.
	 */
	POLAR(SpriteType.Polar1, SpriteType.Polar2, new Color(0x9fb8d6), 30, 0),

	/** Act 2, middle rows. Double shot: two bullets in a cone. */
	JELLY(SpriteType.Jelly1, SpriteType.Jelly2, new Color(0xf3e4c4), 30, 0),

	/**
	 * Act 2, front rows. Shield: absorbs {@link #BEETLE_SHIELD_HITS} hit(s)
	 * before the beetle can take damage.
	 */
	BEETLE(SpriteType.Beetle1, SpriteType.Beetle2, new Color(0xad6a40), 30, 1);

	/** How long a Polar freeze bullet locks the player ship. */
	public static final int FREEZE_MILLIS = 3000;
	/** Hits Beetle's shield absorbs. */
	public static final int BEETLE_SHIELD_HITS = 1;
	/** Colour of a Beetle while its shield is up. */
	public static final Color SHIELD_COLOR = new Color(0x5ee6ef);
	/** Colour of a freeze bullet, also the frozen ship's tint. */
	public static final Color FREEZE_BULLET_COLOR = new Color(0x4ab9e8);
	/**
	 * Sideways speed of each double-shot bullet, in pixels per frame. The
	 * two bullets leave from the same point, one drifting left and one
	 * right, so they spread in a cone. With the enemy bullet speed of 4,
	 * 1 is about 14 degrees each side; 2 is about 27.
	 */
	public static final int DOUBLE_SHOT_SPREAD = 1;

	/** First animation frame, null for NORMAL. */
	private final SpriteType frameA;
	/** Second animation frame, null for NORMAL. */
	private final SpriteType frameB;
	/** Sprite colour. */
	private final Color color;
	/** Points for destroying it, 0 for NORMAL (decided by its sprite). */
	private final int pointValue;
	/** Hits its shield absorbs, 0 if it has none. */
	private final int shieldHits;

	EnemyType(final SpriteType frameA, final SpriteType frameB,
			final Color color, final int pointValue, final int shieldHits) {
		this.frameA = frameA;
		this.frameB = frameB;
		this.color = color;
		this.pointValue = pointValue;
		this.shieldHits = shieldHits;
	}

	/** @return True if this type has its own sprites (anything but NORMAL). */
	public boolean hasOwnSprites() {
		return this != NORMAL;
	}

	/** @return True if this type has a special ability. */
	public boolean hasAbility() {
		return this == POLAR || this == JELLY || this == BEETLE;
	}

	/** @return First animation frame, null for NORMAL. */
	public SpriteType getFrameA() {
		return this.frameA;
	}

	/** @return Second animation frame, null for NORMAL. */
	public SpriteType getFrameB() {
		return this.frameB;
	}

	/** @return Sprite colour. */
	public Color getColor() {
		return this.color;
	}

	/** @return Points for destroying it, 0 for NORMAL. */
	public int getPointValue() {
		return this.pointValue;
	}

	/** @return Hits its shield absorbs, 0 if it has none. */
	public int getShieldHits() {
		return this.shieldHits;
	}
}
