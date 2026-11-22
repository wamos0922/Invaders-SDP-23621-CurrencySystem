package entity;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Set;

import engine.DrawManager;
import engine.DrawManager.SpriteType;
import engine.GameSettings;

/**
 * Level 20 boss, Tempest Queen (GoG - GOG-75).
 *
 * She hides behind a gas shield that opens and closes. While it is closed
 * she can't be hurt (player bullets just disappear) and she shoots straight
 * down. While it is open she rests and the player can hit her. At half
 * health she gets angry (phase 2): she flashes and turns cream, the shield
 * stays open for less time and she shoots twice as often.
 *
 * Phase 2 mobs: in phase 2 every player bullet that reaches her calls one
 * random Act 2 mob (Polar, Jelly, Beetle, Storm King or Cloud Ray), whether
 * it lands (shield open) or is blocked (shield closed). Mobs hover around her and
 * move with her, shoot all the time with their own abilities, and stay
 * until they are killed, so they stack with every hit (no limit). When she
 * is destroyed, every mob dies with her.
 *
 * Numbers come from the Game Design Draft (Level 20). Art comes from the
 * "Space Invader GoG - Act 2 Jupiter" design system.
 */
public class TempestQueen extends Entity implements Boss {

	/** Hits needed to destroy her. */
	public static final int MAX_HP = 15;
	/** Phase 2 starts at or below this health (half of MAX_HP). */
	public static final int PHASE_2_HP = MAX_HP / 2;
	/** Shown before she appears. */
	public static final String WARNING_TEXT = "WARNING: BOSS INCOMING";
	/** How long the warning shows before she appears. */
	public static final int INTRO_MILLIS = 2000;
	/** How long the shield stays closed. */
	public static final int SHIELD_CLOSED_MILLIS = 4000;
	/** How long the shield stays open in phase 1. */
	public static final int SHIELD_OPEN_MILLIS = 2000;
	/** How long the shield stays open in phase 2. */
	public static final int SHIELD_OPEN_PHASE_2_MILLIS = 1500;
	/** Time between shots while the shield is closed, phase 1. */
	public static final int SHOT_MILLIS = 1000;
	/** Time between shots while the shield is closed, phase 2. */
	public static final int SHOT_PHASE_2_MILLIS = 500;
	/** How long she flashes when phase 2 starts. */
	public static final int FLASH_MILLIS = 1000;
	/** Points for each hit that lands. */
	public static final int POINTS_PER_HIT = 300;
	/** Extra points for destroying her. */
	public static final int POINTS_DESTROYED = 5000;
	/** Shortest time between two shots of one mob. */
	public static final int MOB_SHOT_MIN_MILLIS = 1500;
	/** Longest time between two shots of one mob. */
	public static final int MOB_SHOT_MAX_MILLIS = 2500;
	/** Act 2 mobs she can call in phase 2, picked at random. */
	public static final EnemyType[] MOB_TYPES = {EnemyType.POLAR,
			EnemyType.JELLY, EnemyType.BEETLE, EnemyType.STORM_KING,
			EnemyType.CLOUD_RAY};
	/** Mobs per row around her. */
	private static final int MOBS_PER_ROW = 6;
	/** Horizontal distance between mob spots. */
	private static final int MOB_SPACING_X = 36;
	/** Vertical distance between mob rows. */
	private static final int MOB_SPACING_Y = 28;
	/** Gap between her bottom edge and the first mob row. */
	private static final int MOB_GAP_Y = 16;

	/** Phase 1 colour (jupiter-red-spot). */
	public static final Color PHASE_1_COLOR = new Color(0xec6a45);
	/** Phase 2 colour (jupiter-cream). */
	public static final Color PHASE_2_COLOR = new Color(0xf3e4c4);
	/** Gas Shield colour (jupiter-polar). */
	public static final Color SHIELD_COLOR = new Color(0x9fb8d6);

	/** Sideways speed in pixels per frame. */
	private static final int SPEED_X = 2;
	/** Speed of her bullets, straight down. */
	private static final int BULLET_SPEED = 5;
	/** Gap kept between her and the screen edge, for the shield. */
	private static final int EDGE_MARGIN = 4;
	/** Shield sprite is drawn 2 sprite pixels (4 screen pixels) up-left. */
	private static final int SHIELD_OFFSET = 4;
	/** Colour swap interval while flashing. */
	private static final int FLASH_INTERVAL = 100;
	/** Sprite frame swap interval. */
	private static final int ANIMATION_MILLIS = 500;

	/** Remaining health. */
	private int hp = MAX_HP;
	/** True once she has been destroyed. */
	private boolean destroyed;
	/** True in phase 2 (half health or less). */
	private boolean phase2;
	/** True while the shield is open and she can be hurt. */
	private boolean shieldOpen;
	/** Current sideways speed. */
	private int speedX = SPEED_X;

	/** When the warning ends and she appears. */
	private long introEndsAt;
	/** When the shield next opens or closes. */
	private long shieldChangesAt;
	/** When she can shoot again. */
	private long nextShotAt;
	/** When the phase 2 flash ends. */
	private long flashEndsAt;
	/** When the sprite frame next swaps. */
	private long nextFrameAt;

	/** Gas Shield overlay, drawn around her while the shield is closed. */
	private final Entity shield;
	/** Mobs she called in phase 2, alive or still fading out. */
	private final List<Mob> mobs = new ArrayList<Mob>();
	/** Picks mob types and shot timing. */
	private final Random random = new Random();
	/** Screen width, for keeping mobs on screen. */
	private int screenWidth = 448;

	/** One mob: its ship, its spot around her and its shot timer. */
	private static final class Mob {
		/** The mob itself. */
		private final EnemyShip ship;
		/** Spot index around her: row = slot / 6, column = slot % 6. */
		private final int slot;
		/** When it can shoot next. */
		private long nextShotAt;

		Mob(final EnemyShip ship, final int slot, final long nextShotAt) {
			this.ship = ship;
			this.slot = slot;
			this.nextShotAt = nextShotAt;
		}
	}

	/**
	 * Creates the Tempest Queen. She stays hidden behind the warning for
	 * {@link #INTRO_MILLIS}, then appears with her shield closed.
	 *
	 * @param positionX
	 *            Left edge.
	 * @param positionY
	 *            Top edge.
	 */
	public TempestQueen(final int positionX, final int positionY) {
		super(positionX, positionY, 24 * 2, 16 * 2, PHASE_1_COLOR);
		this.spriteType = SpriteType.TempestQueen1;
		this.shield = new Entity(positionX - SHIELD_OFFSET,
				positionY - SHIELD_OFFSET, 28 * 2, 20 * 2, SHIELD_COLOR);
		this.shield.spriteType = SpriteType.GasShield;
		long now = System.currentTimeMillis();
		this.introEndsAt = now + INTRO_MILLIS;
		this.shieldChangesAt = this.introEndsAt + SHIELD_CLOSED_MILLIS;
		this.nextShotAt = this.introEndsAt + SHOT_MILLIS;
		this.nextFrameAt = now + ANIMATION_MILLIS;
	}

	/** @return True while the warning shows and she hasn't appeared yet. */
	public final boolean isIntro() {
		return System.currentTimeMillis() < this.introEndsAt;
	}

	/** @return True while the shield is open and she can be hurt. */
	public final boolean isShieldOpen() {
		return this.shieldOpen;
	}

	/** @return True in phase 2 (half health or less). */
	public final boolean isPhase2() {
		return this.phase2;
	}

	/** Swaps sprite frames, runs the shield timer and the phase 2 flash. */
	@Override
	public final void update() {
		if (this.destroyed || isIntro())
			return;
		long now = System.currentTimeMillis();

		if (now >= this.nextFrameAt) {
			this.spriteType = this.spriteType == SpriteType.TempestQueen1
					? SpriteType.TempestQueen2 : SpriteType.TempestQueen1;
			this.nextFrameAt = now + ANIMATION_MILLIS;
		}

		if (now >= this.shieldChangesAt) {
			this.shieldOpen = !this.shieldOpen;
			if (this.shieldOpen) {
				this.shieldChangesAt = now + (this.phase2
						? SHIELD_OPEN_PHASE_2_MILLIS : SHIELD_OPEN_MILLIS);
			} else {
				this.shieldChangesAt = now + SHIELD_CLOSED_MILLIS;
				// First shot comes one interval after the shield closes.
				this.nextShotAt = now + shotInterval();
			}
		}

		// Mobs animate; dead ones are removed once their fade-out ends.
		Iterator<Mob> it = this.mobs.iterator();
		while (it.hasNext()) {
			Mob mob = it.next();
			if (mob.ship.isFadeFinished())
				it.remove();
			else
				mob.ship.update();
		}

		// Phase 2 flash: swap between both colours, then stay cream.
		if (this.phase2 && now < this.flashEndsAt)
			setColor((now / FLASH_INTERVAL) % 2 == 0 ? PHASE_1_COLOR
					: PHASE_2_COLOR);
		else
			setColor(this.phase2 ? PHASE_2_COLOR : PHASE_1_COLOR);
	}

	/** Moves left and right at the top, bouncing off the screen edges. */
	@Override
	public final void moveBoss(final int screenWidth, final int screenHeight,
			final GameSettings settings) {
		if (this.destroyed || isIntro())
			return;
		this.screenWidth = screenWidth;
		this.positionX += this.speedX;
		int maxX = screenWidth - this.width - EDGE_MARGIN;
		if (this.positionX <= EDGE_MARGIN) {
			this.positionX = EDGE_MARGIN;
			this.speedX = Math.abs(this.speedX);
		} else if (this.positionX >= maxX) {
			this.positionX = maxX;
			this.speedX = -Math.abs(this.speedX);
		}
		// Mobs move with her.
		for (Mob mob : this.mobs)
			if (!mob.ship.isDestroyed())
				placeMob(mob);
	}

	/** Puts a mob on its spot around her, kept inside the screen. */
	private void placeMob(final Mob mob) {
		int row = mob.slot / MOBS_PER_ROW;
		int column = mob.slot % MOBS_PER_ROW;
		int centerX = this.positionX + this.width / 2;
		int x = centerX + (2 * column - (MOBS_PER_ROW - 1)) * MOB_SPACING_X / 2
				- mob.ship.getWidth() / 2;
		x = Math.max(0, Math.min(x, this.screenWidth - mob.ship.getWidth()));
		int y = this.positionY + this.height + MOB_GAP_Y + row * MOB_SPACING_Y;
		mob.ship.setPositionX(x);
		mob.ship.setPositionY(y);
	}

	/** Calls one random Act 2 mob into the first free spot around her. */
	private void spawnMob() {
		Set<Integer> taken = new java.util.HashSet<Integer>();
		for (Mob mob : this.mobs)
			if (!mob.ship.isDestroyed())
				taken.add(mob.slot);
		int slot = 0;
		while (taken.contains(slot))
			slot++;
		EnemyType type = MOB_TYPES[this.random.nextInt(MOB_TYPES.length)];
		Mob mob = new Mob(new EnemyShip(0, 0, type, 1), slot,
				System.currentTimeMillis() + nextMobShotDelay());
		placeMob(mob);
		this.mobs.add(mob);
	}

	/** @return Random wait before a mob's next shot. */
	private int nextMobShotDelay() {
		return MOB_SHOT_MIN_MILLIS + this.random.nextInt(
				MOB_SHOT_MAX_MILLIS - MOB_SHOT_MIN_MILLIS + 1);
	}

	/**
	 * @return Her mobs, alive or still fading out (GameScreen checks player
	 *         bullets against them).
	 */
	@Override
	public final List<EnemyShip> getMinions() {
		List<EnemyShip> ships = new ArrayList<EnemyShip>(this.mobs.size());
		for (Mob mob : this.mobs)
			ships.add(mob.ship);
		return Collections.unmodifiableList(ships);
	}

	/** @return How many of her mobs are alive. */
	public final int getAliveMobCount() {
		int alive = 0;
		for (Mob mob : this.mobs)
			if (!mob.ship.isDestroyed())
				alive++;
		return alive;
	}

	/**
	 * Her mobs shoot all the time, each on its own timer and with its own
	 * ability. She shoots one bullet straight down, only while the shield
	 * is closed.
	 *
	 * @return True if anything was fired.
	 */
	@Override
	public final boolean shoot(final Set<Bullet> bullets,
			final GameSettings settings) {
		if (this.destroyed || isIntro())
			return false;
		long now = System.currentTimeMillis();
		boolean fired = false;
		for (Mob mob : this.mobs) {
			if (mob.ship.isDestroyed() || now < mob.nextShotAt)
				continue;
			EnemyShipFormation.fireShot(mob.ship, bullets);
			mob.nextShotAt = now + nextMobShotDelay();
			fired = true;
		}
		if (this.shieldOpen || now < this.nextShotAt)
			return fired;
		this.nextShotAt = now + shotInterval();
		bullets.add(BulletPool.getBullet(this.positionX + this.width / 2,
				this.positionY + this.height, BULLET_SPEED));
		return true;
	}

	/** @return Time between shots in the current phase. */
	private int shotInterval() {
		return this.phase2 ? SHOT_PHASE_2_MILLIS : SHOT_MILLIS;
	}

	/**
	 * A player bullet reached her. Before she appears it just disappears.
	 * With the shield closed it disappears too, but in phase 2 it calls a
	 * mob. With the shield open she loses 1 HP.
	 *
	 * @return Points earned.
	 */
	@Override
	public final int manageCollisionsBoss(final Bullet bullet) {
		if (this.destroyed || isIntro())
			return 0;
		if (!this.shieldOpen) {
			// Shooting the closed shield in phase 2 calls a mob.
			if (this.phase2)
				spawnMob();
			return 0;
		}
		this.hp--;
		int points = POINTS_PER_HIT;
		if (this.hp <= 0) {
			destroy();
			points += POINTS_DESTROYED;
		} else if (!this.phase2 && this.hp <= PHASE_2_HP) {
			this.phase2 = true;
			this.flashEndsAt = System.currentTimeMillis() + FLASH_MILLIS;
		} else if (this.phase2) {
			// Every hit in phase 2 calls another mob.
			spawnMob();
		}
		return points;
	}

	/** Destroys her, showing the explosion. */
	@Override
	public final void destroy() {
		this.destroyed = true;
		this.shieldOpen = false;
		this.spriteType = SpriteType.Explosion;
		// Her mobs die with her (no points for them).
		for (Mob mob : this.mobs)
			mob.ship.destroy();
	}

	/** Hidden during the warning. */
	@Override
	public final boolean isVisible() {
		return !isIntro();
	}

	/** Draws her mobs, and the Gas Shield around her while it is closed. */
	@Override
	public final void drawWarnings(final DrawManager drawManager) {
		for (Mob mob : this.mobs)
			drawManager.drawEntity(mob.ship, mob.ship.getPositionX(),
					mob.ship.getPositionY());
		if (this.destroyed || isIntro() || this.shieldOpen)
			return;
		this.shield.positionX = this.positionX - SHIELD_OFFSET;
		this.shield.positionY = this.positionY - SHIELD_OFFSET;
		drawManager.drawEntity(this.shield, this.shield.positionX,
				this.shield.positionY);
	}

	/** @return The warning text while it should show, otherwise null. */
	@Override
	public final String getWarningText() {
		return !this.destroyed && isIntro() ? WARNING_TEXT : null;
	}

	/** Keeps every timer still while the game is paused. */
	@Override
	public final void resumeAfterPause(final long pausedMilliseconds) {
		this.introEndsAt += pausedMilliseconds;
		this.shieldChangesAt += pausedMilliseconds;
		this.nextShotAt += pausedMilliseconds;
		this.flashEndsAt += pausedMilliseconds;
		this.nextFrameAt += pausedMilliseconds;
		for (Mob mob : this.mobs)
			mob.nextShotAt += pausedMilliseconds;
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
