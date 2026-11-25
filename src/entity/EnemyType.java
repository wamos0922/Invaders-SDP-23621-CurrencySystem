package entity;

import java.awt.Color;

import engine.DrawManager.SpriteType;

/**
 * Story mode enemy types (GoG - GOG-84).
 *
 * NORMAL keeps the original A/B/C ships, chosen by row. Every other type has
 * its own two animation frames, colour (from the act's design system) and
 * point value; Polar, Jelly, Beetle (Act 2) and Comet, Mite, Stinger
 * (Act 4) also have an ability. Which type goes
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
	BEETLE(SpriteType.Beetle1, SpriteType.Beetle2, new Color(0xad6a40), 30, 1),

	/**
	 * Level 10 boss helper (GOG-74): the AI Warship calls these at half
	 * health. They don't shoot; they heal the boss until destroyed. Stays
	 * earth-land green at any health, as in the design system.
	 */
	REPAIR_DRONE(SpriteType.RepairDrone1, SpriteType.RepairDrone2,
			EnemyShip.EARTH_LAND, 100, 0),

	/**
	 * Act 3 (Saturn), row 1: flagship of the allied fleet. No ability, it
	 * only shoots (saturn-ring-ice, GOG-28).
	 */
	RING_COMMANDER(SpriteType.RingCommander1, SpriteType.RingCommander2,
			new Color(0xece6d2), 50, 0),

	/** Act 3 (Saturn), row 2: soldier pod. No ability (saturn-moon). */
	MOONLET_POD(SpriteType.MoonletPod1, SpriteType.MoonletPod2,
			new Color(0x9a7564), 40, 0),

	/** Act 3 (Saturn), row 3: elite strike ship. No ability (saturn-gold). */
	HALO_LANCER(SpriteType.HaloLancer1, SpriteType.HaloLancer2,
			new Color(0xe8c867), 30, 0),

	/** Act 3 (Saturn), row 4: twin-pod fighter. No ability (saturn-butterscotch). */
	ORBIT_FIGHTER(SpriteType.OrbitFighter1, SpriteType.OrbitFighter2,
			new Color(0xd0913f), 20, 0),

	/** Act 3 (Saturn), row 5: hexagon patrol drone, weakest. No ability (saturn-hexagon). */
	HEX_SENTINEL(SpriteType.HexSentinel1, SpriteType.HexSentinel2,
			new Color(0x6fb3ae), 10, 0),

	/** Act 4 enemy with no ability (space-starlight). */
	STAR_EATER(SpriteType.StarEater1, SpriteType.StarEater2,
			new Color(0xf7f2ff), 20, 0),

	/** Act 4 enemy with no ability (space-nebula). */
	NEBULA_WRAITH(SpriteType.Wraith1, SpriteType.Wraith2,
			new Color(0xff8fe6), 10, 0),

	/**
	 * Act 4, front rows. Kamikaze: a normal enemy until the player's bullet
	 * hits it. That hit doesn't kill it: it leaves the formation, locks onto
	 * where the player is and dives there in a straight line, then explodes
	 * at that spot. A second hit while it dives kills it (GOG-86).
	 */
	COMET(SpriteType.Comet1, SpriteType.Comet2, new Color(0x5ee6ef), 30, 0),

	/**
	 * Act 4, middle rows. Boom: when it dies it sprays shrapnel downward and
	 * hits the enemies next to it (GOG-86).
	 */
	MITE(SpriteType.Mite1, SpriteType.Mite2, new Color(0x7f74ff), 30, 0),

	/**
	 * Act 4, back rows. Vulnerable bullet: a hit leaves the player with only
	 * 1 life for {@link #VULNERABLE_MILLIS}, then the lives come back. The
	 * player can shoot its bullets down (GOG-86).
	 */
	STINGER(SpriteType.Stinger1, SpriteType.Stinger2, new Color(0xb98aff),
			30, 0);

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
	/** Comet dive speed, in pixels per frame. */
	public static final int COMET_DIVE_SPEED = 4;
	/**
	 * Comet blast reach: the player is hit if the ship's centre is this
	 * close to where the Comet explodes (about 2 ship widths of blast).
	 */
	public static final int COMET_BLAST_RADIUS = 40;
	/**
	 * Mite blast reach: enemies whose centre is this close take 1 hit
	 * (the ones directly left, right, above and below).
	 */
	public static final int MITE_BLAST_RADIUS = 45;
	/**
	 * Type placed between Mites in a Mite row, so two Mites are never next
	 * to each other and one boom can't chain (GOG-86).
	 */
	public static final EnemyType MITE_ROW_FILLER = STAR_EATER;
	/** Sideways speeds of the 3 shrapnel bullets a Mite sprays. */
	public static final int[] MITE_SHRAPNEL_SPEED_X = {-1, 0, 1};
	/** How long a Stinger hit leaves the player at 1 life. */
	public static final int VULNERABLE_MILLIS = 5000;
	/** Colour of a Stinger's vulnerable bullet (space-galaxy). */
	public static final Color VULNERABLE_BULLET_COLOR = new Color(0xb98aff);

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

	/**
	 * @return True if a ship of this type turns yellow while it has more
	 *         than one hit left. Repair Drones keep their design colour.
	 */
	public boolean showsExtraHealth() {
		return this != REPAIR_DRONE;
	}

	/** @return True if this type has a special ability. */
	public boolean hasAbility() {
		return this == POLAR || this == JELLY || this == BEETLE
				|| this == COMET || this == MITE || this == STINGER;
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
