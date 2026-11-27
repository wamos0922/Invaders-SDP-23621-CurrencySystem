package entity;

import java.awt.Color;

import engine.DrawManager.SpriteType;

/**
 * Implements a bullet that moves vertically up or down, optionally drifting
 * sideways (used by aimed boss attacks).
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 * 
 */
public class Bullet extends Entity {
	/** Dimensions of an ordinary player or enemy bullet. */
	public static final int DEFAULT_WIDTH = 6;
	public static final int DEFAULT_HEIGHT = 10;

	/**
	 * Speed of the bullet, positive or negative depending on direction -
	 * positive is down.
	 */
	private int speed;
	/** Horizontal speed of the bullet, positive is right. Usually zero. */
	private int speedX;
	/** True for a Polar freeze bullet (GoG, GOG-84). */
	private boolean freezing;
	/** True for a Stinger vulnerable bullet (GoG, GOG-86). */
	private boolean vulnerable;
	/**
	 * The bullet disappears once its top passes this height: short-range
	 * shots such as the Ring Dreadnought's shotgun (GoG, GOG-76). No limit
	 * by default.
	 */
	private int maxY = Integer.MAX_VALUE;
	/** Enemies this bullet can still go through (skill nodes, GOG-89). */
	private int pierce;
	/** Shot (volley) the bullet belongs to; 0 = none (GOG-89). */
	private long volley;

	/**
	 * Constructor, establishes the bullet's properties.
	 * 
	 * @param positionX
	 *            Initial position of the bullet in the X axis.
	 * @param positionY
	 *            Initial position of the bullet in the Y axis.
	 * @param speed
	 *            Speed of the bullet, positive or negative depending on
	 *            direction - positive is down.
	 */
    public Bullet(final int positionX, final int positionY, final int speed) {
        this(positionX, positionY, speed, DEFAULT_WIDTH, DEFAULT_HEIGHT);
    }

    /**
     * Constructor, establishes the bullet's properties with custom size.
     *
     * @param positionX
     *            Initial position of the bullet in the X axis.
     * @param positionY
     *            Initial position of the bullet in the Y axis.
     * @param speed
     *            Speed of the bullet.
     * @param width
     *            Width of the bullet.
     * @param height
     *            Height of the bullet.
     */
    public Bullet(final int positionX, final int positionY, final int speed, final int width, final int height) {
        super(positionX, positionY, width, height, Color.WHITE);

        this.speed = speed;
        setSprite();
    }

	/**
	 * Sets correct sprite for the bullet, based on speed.
	 */
	public final void setSprite() {
		if (speed < 0)
			this.spriteType = SpriteType.Bullet;
		else
			this.spriteType = SpriteType.EnemyBullet;
	}

	/**
	 * Updates the bullet's position.
	 */
	public final void update() {
		this.positionX += this.speedX;
		this.positionY += this.speed;
	}

	/**
	 * Setter of the speed of the bullet.
	 * 
	 * @param speed
	 *            New speed of the bullet.
	 */
	public final void setSpeed(final int speed) {
		this.speed = speed;
	}

	/**
	 * Getter for the speed of the bullet.
	 * 
	 * @return Speed of the bullet.
	 */
	public final int getSpeed() {
		return this.speed;
	}

	/**
	 * Setter of the horizontal speed of the bullet.
	 *
	 * @param speedX
	 *            New horizontal speed of the bullet, positive is right.
	 */
	public final void setSpeedX(final int speedX) {
		this.speedX = speedX;
	}

	/**
	 * Marks the bullet as a Polar freeze bullet, or back to a normal one
	 * (GoG, GOG-84). A freeze bullet is drawn in light blue (#4AB9E8).
	 *
	 * @param freezing
	 *            True for a freeze bullet.
	 */
	public final void setFreezing(final boolean freezing) {
		this.freezing = freezing;
		setColor(freezing ? EnemyType.FREEZE_BULLET_COLOR : Color.WHITE);
	}

	/**
	 * @return True if hitting the player freezes the ship instead of
	 *         taking a life.
	 */
	public final boolean isFreezing() {
		return this.freezing;
	}

	/** @return Enemies this bullet can still go through (GOG-89). */
	public final int getPierce() {
		return this.pierce;
	}

	/** @param pierce Enemies this bullet can go through (GOG-89). */
	public final void setPierce(final int pierce) {
		this.pierce = Math.max(0, pierce);
	}

	/** @return Shot (volley) the bullet belongs to; 0 = none (GOG-89). */
	public final long getVolley() {
		return this.volley;
	}

	/** @param volley Shot (volley) the bullet belongs to (GOG-89). */
	public final void setVolley(final long volley) {
		this.volley = volley;
	}

	/**
	 * Marks the bullet as a Stinger vulnerable bullet, or back to a normal
	 * one (GoG, GOG-86). A vulnerable bullet is drawn in violet (#B98AFF).
	 *
	 * @param vulnerable
	 *            True for a vulnerable bullet.
	 */
	public final void setVulnerable(final boolean vulnerable) {
		this.vulnerable = vulnerable;
		setColor(vulnerable ? EnemyType.VULNERABLE_BULLET_COLOR : Color.WHITE);
	}

	/**
	 * @return True if hitting the player leaves them at 1 life for a while
	 *         instead of taking a life. The player can shoot it down.
	 */
	public final boolean isVulnerable() {
		return this.vulnerable;
	}

	/**
	 * Sets how far down the bullet flies before it disappears (GoG,
	 * GOG-76). {@link Integer#MAX_VALUE} means no limit.
	 *
	 * @param maxY
	 *            Height past which the bullet is removed.
	 */
	public final void setMaxY(final int maxY) {
		this.maxY = maxY;
	}

	/** @return Height past which the bullet is removed (GoG, GOG-76). */
	public final int getMaxY() {
		return this.maxY;
	}

	/**
	 * Getter for the horizontal speed of the bullet.
	 *
	 * @return Horizontal speed of the bullet.
	 */
	public final int getSpeedX() {
		return this.speedX;
	}
}
