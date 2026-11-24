package entity;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import engine.DrawManager;
import engine.DrawManager.SpriteType;
import engine.GameSettings;
import engine.soundEngine.Sound;
import engine.soundEngine.SoundManager;

/**
 * Level 10 boss, AI Warship (GoG - GOG-74). Ends Act 1 (Earth).
 *
 * Phase 1 (full health to half): after the level's start countdown it flies
 * in from the top of the screen, then moves left and right twice as fast as
 * normal enemies and fires two bullets at the player.
 *
 * Phase 2 (half health to 0): it turns earth-magma and calls four Repair
 * Drones. The drones don't shoot; each heals it 1 HP every 3 seconds, up to
 * full health. While any drone is alive the boss can't be hurt, so the
 * player must destroy the drones first, then finish the boss. Drones come
 * only once.
 *
 * Numbers come from the Game Design Draft (Level 10). Art (GOG-78) comes
 * from the "Space Invader GoG" design system.
 */
public class AIWarship extends Entity implements Boss {

	/** Hits needed to destroy it. */
	public static final int MAX_HP = 15;
	/** Phase 2 starts at or below this health (half of MAX_HP). */
	public static final int PHASE_2_HP = MAX_HP / 2;
	/** Repair Drones called at the start of phase 2. */
	public static final int DRONE_COUNT = 4;
	/** Hits needed to destroy one Repair Drone. */
	public static final int DRONE_HP = 2;
	/** Each living drone heals the boss this often. */
	public static final int HEAL_MILLIS = 3000;
	/** Health one drone restores per heal. */
	public static final int HEAL_AMOUNT = 1;
	/** Shown while it flies in. */
	public static final String WARNING_TEXT = "WARNING: BOSS INCOMING";
	/** Shown when the drones arrive. */
	public static final String DRONE_TEXT = "DESTROY THE REPAIR DRONES";
	/** How long the fly-in (and the warning) lasts. */
	public static final int INTRO_MILLIS = 2000;
	/** How long the drone message shows. */
	public static final int DRONE_TEXT_MILLIS = 2500;
	/** Time between double shots. */
	public static final int SHOT_MILLIS = 1200;
	/** How long it flashes when phase 2 starts. */
	public static final int FLASH_MILLIS = 1000;
	/** How long it glows green after a heal. */
	public static final int HEAL_FLASH_MILLIS = 200;
	/** How long the explosion shows after it is destroyed. */
	public static final int EXPLOSION_MILLIS = 1000;
	/** Points for each hit that lands. */
	public static final int POINTS_PER_HIT = 300;
	/** Extra points for destroying it. */
	public static final int POINTS_DESTROYED = 5000;

	/**
	 * Fastest sideways speed of a normal enemy formation, in pixels per
	 * frame: it steps 8 px at most every 10 frames (EnemyShipFormation).
	 */
	public static final double NORMAL_ENEMY_SPEED = 8 / 10.0;
	/** Game Design Draft: twice as fast as normal enemies. */
	public static final double SPEED_X = 2 * NORMAL_ENEMY_SPEED;

	/** Phase 1 colour (earth-ocean). */
	public static final Color PHASE_1_COLOR = EnemyShip.EARTH_OCEAN;
	/** Phase 2 colour (earth-magma). */
	public static final Color PHASE_2_COLOR = EnemyShip.EARTH_MAGMA;
	/** Glow shown when a drone heals it (earth-land). */
	public static final Color HEAL_COLOR = EnemyShip.EARTH_LAND;

	/** Total speed of its bullets. */
	private static final int BULLET_SPEED = 5;
	/** Its bullets always move down at least this fast. */
	private static final int MIN_BULLET_SPEED_Y = 2;
	/** Cannons sit this far in from its left and right edges. */
	private static final int CANNON_INSET = 8;
	/** Gap kept between it and the screen edge. */
	private static final int EDGE_MARGIN = 4;
	/** Sprite frame swap interval. */
	private static final int ANIMATION_MILLIS = 500;
	/** Colour swap interval while flashing. */
	private static final int FLASH_INTERVAL = 100;
	/** Horizontal distance between drone spots. */
	private static final int DRONE_SPACING_X = 40;
	/** Gap between its bottom edge and the drone row. */
	private static final int DRONE_GAP_Y = 18;
	/** Drones follow their spot slowly, in pixels per frame. */
	private static final double DRONE_SPEED_X = 1.0;
	/** Drones fly in from the top at this speed, in pixels per frame. */
	private static final double DRONE_SPEED_Y = 2.0;
	/** How far a drone sways to each side of its spot. */
	private static final int DRONE_SWAY = 10;
	/** Time for one full sway, left and back. */
	private static final int DRONE_SWAY_MILLIS = 3000;
	/** The explosion sprite is drawn this many screen pixels per pixel. */
	private static final int EXPLOSION_PIXEL = 4;

	/** Remaining health. */
	private int hp = MAX_HP;
	/** True once the level's start countdown has ended (startFight). */
	private boolean started;
	/** True once it has been destroyed. */
	private boolean destroyed;
	/** True in phase 2 (half health or less, drones called). */
	private boolean phase2;
	/** Exact sideways position; positionX is this rounded. */
	private double exactX;
	/** Current sideways speed, signed. */
	private double speedX = SPEED_X;
	/** Where it stops flying in. */
	private final int targetY;
	/** Where the fly-in starts, just above the screen. */
	private final int startY;
	/** Player centre for aimed shots; negative until known. */
	private int aimX = -1;
	/** Player centre for aimed shots. */
	private int aimY = -1;

	/** When the fly-in ends. */
	private long introEndsAt;
	/** When it can shoot again. */
	private long nextShotAt;
	/** When the sprite frame next swaps. */
	private long nextFrameAt;
	/** When the phase 2 flash ends. */
	private long flashEndsAt;
	/** When the green heal glow ends. */
	private long healFlashEndsAt;
	/** When the drone message stops showing. */
	private long droneTextEndsAt;
	/** When it was destroyed. */
	private long destroyedAt;

	/** Repair Drones, alive or still fading out. */
	private final List<Drone> drones = new ArrayList<Drone>();

	/** One Repair Drone: its ship, its spot and its heal timer. */
	private static final class Drone {
		/** The drone itself. */
		private final EnemyShip ship;
		/** Spot in the row under the boss, 0 to DRONE_COUNT - 1. */
		private final int slot;
		/** When it heals the boss next. */
		private long nextHealAt;
		/** Sway clock start. */
		private long swayStart;
		/** Exact position; the ship's position is this rounded. */
		private double x;
		/** Exact position; the ship's position is this rounded. */
		private double y;

		Drone(final EnemyShip ship, final int slot, final long now) {
			this.ship = ship;
			this.slot = slot;
			this.nextHealAt = now + HEAL_MILLIS;
			this.swayStart = now;
		}
	}

	/**
	 * Creates the AI Warship. It stays hidden above the screen until
	 * {@link #startFight()} (end of the level's start countdown), then flies
	 * down to its spot for {@link #INTRO_MILLIS}.
	 *
	 * @param positionX
	 *            Left edge.
	 * @param positionY
	 *            Top edge once it has flown in.
	 */
	public AIWarship(final int positionX, final int positionY) {
		super(positionX, -16 * 2, 24 * 2, 16 * 2, PHASE_1_COLOR);
		this.spriteType = SpriteType.AIWarship1;
		this.exactX = positionX;
		this.targetY = positionY;
		this.startY = -this.height;
	}

	/**
	 * Starts its clock when play begins: the fly-in starts now, and every
	 * timer counts from here. Calling it again does nothing.
	 */
	@Override
	public final void startFight() {
		if (this.started)
			return;
		this.started = true;
		long now = System.currentTimeMillis();
		this.introEndsAt = now + INTRO_MILLIS;
		this.nextShotAt = this.introEndsAt + SHOT_MILLIS;
		this.nextFrameAt = now + ANIMATION_MILLIS;
	}

	/** @return True once play has begun (the countdown ended). */
	public final boolean isStarted() {
		return this.started;
	}

	/**
	 * @return True before it is in place: during the level's start
	 *         countdown and while it flies in.
	 */
	public final boolean isIntro() {
		return !this.started || System.currentTimeMillis() < this.introEndsAt;
	}

	/** @return True in phase 2 (the drones have been called). */
	public final boolean isPhase2() {
		return this.phase2;
	}

	/** @return True while a Repair Drone is alive and protecting it. */
	public final boolean isInvincible() {
		return getAliveDroneCount() > 0;
	}

	/** @return How many Repair Drones are alive. */
	public final int getAliveDroneCount() {
		int alive = 0;
		for (Drone drone : this.drones)
			if (!drone.ship.isDestroyed())
				alive++;
		return alive;
	}

	/** Swaps sprite frames, runs the drones' healing and sets the colour. */
	@Override
	public final void update() {
		if (this.destroyed || !this.started)
			return;
		long now = System.currentTimeMillis();

		if (now >= this.nextFrameAt) {
			this.spriteType = this.spriteType == SpriteType.AIWarship1
					? SpriteType.AIWarship2 : SpriteType.AIWarship1;
			this.nextFrameAt = now + ANIMATION_MILLIS;
		}

		// Drones animate and heal; dead ones go once their fade-out ends.
		Iterator<Drone> it = this.drones.iterator();
		while (it.hasNext()) {
			Drone drone = it.next();
			if (drone.ship.isFadeFinished()) {
				it.remove();
				continue;
			}
			if (drone.ship.isDestroyed())
				continue;
			drone.ship.update();
			if (now >= drone.nextHealAt) {
				drone.nextHealAt = now + HEAL_MILLIS;
				if (this.hp < MAX_HP) {
					this.hp = Math.min(MAX_HP, this.hp + HEAL_AMOUNT);
					this.healFlashEndsAt = now + HEAL_FLASH_MILLIS;
				}
			}
		}

		// Phase 2 flash: swap between both colours, then stay magma.
		if (this.phase2 && now < this.flashEndsAt)
			setColor((now / FLASH_INTERVAL) % 2 == 0 ? PHASE_1_COLOR
					: PHASE_2_COLOR);
		else if (now < this.healFlashEndsAt)
			setColor(HEAL_COLOR);
		else
			setColor(this.phase2 ? PHASE_2_COLOR : PHASE_1_COLOR);
	}

	/**
	 * Flies in from the top, then moves left and right, bouncing off the
	 * screen edges. Drones follow their spots under it.
	 */
	@Override
	public final void moveBoss(final int screenWidth, final int screenHeight,
			final GameSettings settings) {
		if (this.destroyed || !this.started)
			return;
		long now = System.currentTimeMillis();
		if (now < this.introEndsAt) {
			double progress = 1.0 - (this.introEndsAt - now)
					/ (double) INTRO_MILLIS;
			// Ease out: fast at first, settling into place.
			double eased = 1.0 - (1.0 - progress) * (1.0 - progress);
			this.positionY = (int) Math.round(this.startY
					+ (this.targetY - this.startY) * eased);
			return;
		}
		this.positionY = this.targetY;

		this.exactX += this.speedX;
		int maxX = screenWidth - this.width - EDGE_MARGIN;
		if (this.exactX <= EDGE_MARGIN) {
			this.exactX = EDGE_MARGIN;
			this.speedX = Math.abs(this.speedX);
		} else if (this.exactX >= maxX) {
			this.exactX = maxX;
			this.speedX = -Math.abs(this.speedX);
		}
		this.positionX = (int) Math.round(this.exactX);

		for (Drone drone : this.drones)
			if (!drone.ship.isDestroyed())
				moveDrone(drone, screenWidth, now);
	}

	/**
	 * Moves a drone towards its spot under the boss, slowly, swaying side
	 * to side. A new drone flies down from above the screen first.
	 */
	private void moveDrone(final Drone drone, final int screenWidth,
			final long now) {
		int shipWidth = drone.ship.getWidth();
		double sway = DRONE_SWAY * Math.sin(2 * Math.PI
				* (now - drone.swayStart) / DRONE_SWAY_MILLIS
				+ drone.slot * Math.PI / 2);
		double spotX = droneSpotX(drone.slot) + sway;
		spotX = Math.max(0, Math.min(spotX, screenWidth - shipWidth));
		double spotY = this.targetY + this.height + DRONE_GAP_Y;

		drone.x += Math.max(-DRONE_SPEED_X,
				Math.min(DRONE_SPEED_X, spotX - drone.x));
		drone.y = Math.min(spotY, drone.y + DRONE_SPEED_Y);
		drone.ship.setPositionX((int) Math.round(drone.x));
		drone.ship.setPositionY((int) Math.round(drone.y));
	}

	/** @return Left edge of a drone spot, centred as a row under it. */
	private double droneSpotX(final int slot) {
		double centerX = this.exactX + this.width / 2.0;
		return centerX + (2 * slot - (DRONE_COUNT - 1)) * DRONE_SPACING_X
				/ 2.0 - 12;
	}

	/** Calls the Repair Drones, just above the screen over their spots. */
	private void callDrones(final long now) {
		for (int slot = 0; slot < DRONE_COUNT; slot++) {
			EnemyShip ship = new EnemyShip(0, 0, EnemyType.REPAIR_DRONE,
					DRONE_HP);
			Drone drone = new Drone(ship, slot, now);
			drone.x = droneSpotX(slot);
			drone.y = -ship.getHeight();
			ship.setPositionX((int) Math.round(drone.x));
			ship.setPositionY((int) Math.round(drone.y));
			this.drones.add(drone);
		}
	}

	/**
	 * @return Its drones, alive or still fading out (GameScreen checks
	 *         player bullets against them first).
	 */
	@Override
	public final List<EnemyShip> getMinions() {
		List<EnemyShip> ships = new ArrayList<EnemyShip>(this.drones.size());
		for (Drone drone : this.drones)
			ships.add(drone.ship);
		return Collections.unmodifiableList(ships);
	}

	/** Remembers the player's centre for aimed shots. */
	@Override
	public final void setTarget(final int x, final int y) {
		this.aimX = x;
		this.aimY = y;
	}

	/**
	 * Fires two bullets, one from each cannon, aimed at the player. It
	 * keeps shooting in phase 2. Drones never shoot.
	 *
	 * @return True if it fired.
	 */
	@Override
	public final boolean shoot(final Set<Bullet> bullets,
			final GameSettings settings) {
		if (this.destroyed || isIntro())
			return false;
		long now = System.currentTimeMillis();
		if (now < this.nextShotAt)
			return false;
		this.nextShotAt = now + SHOT_MILLIS;
		int y = this.positionY + this.height;
		bullets.add(aimedBullet(this.positionX + CANNON_INSET, y));
		bullets.add(aimedBullet(this.positionX + this.width - CANNON_INSET, y));
		return true;
	}

	/** @return A bullet from the given cannon towards the player. */
	private Bullet aimedBullet(final int fromX, final int fromY) {
		int speedX = 0;
		int speedY = BULLET_SPEED;
		if (this.aimX >= 0 && this.aimY > fromY) {
			double dx = this.aimX - fromX;
			double dy = this.aimY - fromY;
			double length = Math.sqrt(dx * dx + dy * dy);
			speedX = (int) Math.round(dx / length * BULLET_SPEED);
			speedY = Math.max(MIN_BULLET_SPEED_Y,
					(int) Math.round(dy / length * BULLET_SPEED));
		}
		Bullet bullet = BulletPool.getBullet(fromX, fromY, speedY);
		bullet.setSpeedX(speedX);
		return bullet;
	}

	/**
	 * A player bullet reached it. While flying in, or while a drone is
	 * alive, the bullet just disappears. Otherwise it loses 1 HP; the hit
	 * that brings it to half health starts phase 2 and calls the drones.
	 *
	 * @return Points earned.
	 */
	@Override
	public final int manageCollisionsBoss(final Bullet bullet) {
		if (this.destroyed || isIntro() || isInvincible())
			return 0;
		this.hp--;
		int points = POINTS_PER_HIT;
		if (this.hp <= 0) {
			destroy();
			points += POINTS_DESTROYED;
		} else if (!this.phase2 && this.hp <= PHASE_2_HP) {
			long now = System.currentTimeMillis();
			this.phase2 = true;
			this.flashEndsAt = now + FLASH_MILLIS;
			this.droneTextEndsAt = now + DRONE_TEXT_MILLIS;
			callDrones(now);
		}
		return points;
	}

	/** Destroys it with a big explosion; any drone left dies with it. */
	@Override
	public final void destroy() {
		if (this.destroyed)
			return;
		this.destroyed = true;
		this.hp = 0;
		this.destroyedAt = System.currentTimeMillis();
		this.spriteType = SpriteType.Explosion;
		for (Drone drone : this.drones)
			drone.ship.destroy();
		SoundManager.getInstance().playSfx(Sound.ENEMY_EXPLOSION);
	}

	/** Hidden before the fight starts and once destroyed (explosion). */
	@Override
	public final boolean isVisible() {
		return this.started && !this.destroyed;
	}

	/** Draws its drones and, once destroyed, the explosion. */
	@Override
	public final void drawWarnings(final DrawManager drawManager) {
		for (Drone drone : this.drones)
			if (!drone.ship.isFadeFinished())
				drawManager.drawEntity(drone.ship, drone.ship.getPositionX(),
						drone.ship.getPositionY());
		if (!this.destroyed || !this.started)
			return;
		long elapsed = System.currentTimeMillis() - this.destroyedAt;
		if (elapsed >= EXPLOSION_MILLIS)
			return;
		int[] size = drawManager.getSpriteSize(SpriteType.Explosion);
		int x = getCenterX() - size[0] * EXPLOSION_PIXEL / 2;
		int y = getCenterY() - size[1] * EXPLOSION_PIXEL / 2;
		Color color = (elapsed / FLASH_INTERVAL) % 2 == 0 ? PHASE_2_COLOR
				: Color.WHITE;
		drawManager.drawSpriteScaled(SpriteType.Explosion, x, y,
				EXPLOSION_PIXEL, color);
	}

	/**
	 * @return The warning while it flies in, the drone message when they
	 *         arrive, otherwise null.
	 */
	@Override
	public final String getWarningText() {
		if (!this.started || this.destroyed)
			return null;
		long now = System.currentTimeMillis();
		if (now < this.introEndsAt)
			return WARNING_TEXT;
		if (this.phase2 && now < this.droneTextEndsAt && isInvincible())
			return DRONE_TEXT;
		return null;
	}

	/** Keeps every timer still while the game is paused. */
	@Override
	public final void resumeAfterPause(final long pausedMilliseconds) {
		this.introEndsAt += pausedMilliseconds;
		this.nextShotAt += pausedMilliseconds;
		this.nextFrameAt += pausedMilliseconds;
		this.flashEndsAt += pausedMilliseconds;
		this.healFlashEndsAt += pausedMilliseconds;
		this.droneTextEndsAt += pausedMilliseconds;
		for (Drone drone : this.drones) {
			drone.nextHealAt += pausedMilliseconds;
			drone.swayStart += pausedMilliseconds;
		}
	}

	/** @return Horizontal centre, where the black hole opens. */
	public final int getCenterX() {
		return this.positionX + this.width / 2;
	}

	/** @return Vertical centre, where the black hole opens. */
	public final int getCenterY() {
		return this.positionY + this.height / 2;
	}

	@Override
	public final int getHp() {
		return this.hp;
	}

	@Override
	public final int getMaxHp() {
		return MAX_HP;
	}

	@Override
	public final boolean isDestroyed() {
		return this.destroyed;
	}
}
