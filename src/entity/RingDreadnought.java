package entity;

import java.awt.Color;
import java.util.Set;

import engine.DrawManager;
import engine.DrawManager.SpriteType;
import engine.GameSettings;
import engine.soundEngine.Sound;
import engine.soundEngine.SoundManager;

/**
 * Level 30 boss, Ring Dreadnought (GoG - GOG-76). Ends Act 3 (Saturn).
 *
 * After the level's start countdown it flies in from the top while a
 * warning blinks. It moves left and right and bobs up and down a little.
 *
 * Phase 1 (full health to half): both wings fire a fan of 3 bullets that
 * spread outwards, and the centre cannon fires a spreading burst: a tight
 * group that opens up as it falls. Every volley leaves gaps at the player's
 * row of at least 1.5 player ship widths.
 *
 * Phase 2 (half health to 0): it turns saturn-butterscotch, the fans come
 * more often and it adds a shotgun. It flashes as a warning, swoops down
 * close to the player, fires 5 pellets that only fly a short distance, then
 * flies back up. Fans and bursts wait while it swoops, so their gaps stay
 * wide.
 *
 * Numbers come from the Game Design Draft (Level 30). Art (GOG-81) comes
 * from the "Space Invader GoG - Act 3 Saturn" design system.
 */
public class RingDreadnought extends Entity implements Boss {

	/** Hits needed to destroy it. */
	public static final int MAX_HP = 20;
	/** Phase 2 starts at or below this health (half of MAX_HP). */
	public static final int PHASE_2_HP = MAX_HP / 2;
	/** Shown while it flies in. */
	public static final String WARNING_TEXT = "WARNING: BOSS INCOMING";
	/** How long the fly-in (and the warning) lasts. */
	public static final int INTRO_MILLIS = 2000;

	/** Bullets in each wing's fan. */
	public static final int FAN_BULLETS = 3;
	/** Time between wing fans, phase 1. */
	public static final int FAN_MILLIS = 2000;
	/** Time between wing fans, phase 2. */
	public static final int FAN_PHASE_2_MILLIS = 1200;
	/** Fan bullets fall at this speed, in pixels per frame. */
	public static final int FAN_SPEED_Y = 4;
	/** Sideways speeds of the left wing's fan; the right wing mirrors it. */
	private static final int[] FAN_SPEEDS_X = {-2, -1, 0};

	/** Time between spreading bursts. */
	public static final int BURST_MILLIS = 3000;
	/** Spreading burst bullets fall at this speed. */
	public static final int BURST_SPEED_Y = 3;
	/** Sideways speeds of the burst: starts tight, opens as it falls. */
	private static final int[] BURST_SPEEDS_X = {-2, -1, 1, 2};

	/** Pellets in one shotgun blast (phase 2). */
	public static final int SHOTGUN_PELLETS = 5;
	/** Time between shotguns, phase 2. */
	public static final int SHOTGUN_MILLIS = 4000;
	/** How long it flashes before it swoops. */
	public static final int SHOTGUN_WARNING_MILLIS = 600;
	/** Time to swoop down. */
	public static final int SWOOP_DOWN_MILLIS = 700;
	/** Time it stays down after firing. */
	public static final int SWOOP_HOLD_MILLIS = 300;
	/** Time to fly back up. */
	public static final int SWOOP_UP_MILLIS = 800;
	/** At the bottom of the swoop its lower edge is this far above the player. */
	public static final int SWOOP_GAP = 150;
	/** Pellets vanish after flying this far down. */
	public static final int SHOTGUN_RANGE = 200;
	/** Pellet speed down. */
	private static final int SHOTGUN_SPEED_Y = 5;
	/** Sideways speeds of the pellets: close together. */
	private static final int[] SHOTGUN_SPEEDS_X = {-2, -1, 0, 1, 2};

	/** Sideways speed, in pixels per frame. */
	public static final double SPEED_X = 1.5;
	/** How far it bobs up and down from its row. */
	public static final int BOB_RANGE = 12;
	/** Time for one bob, down and back up. */
	private static final int BOB_MILLIS = 4000;

	/** How long it flashes when phase 2 starts. */
	public static final int FLASH_MILLIS = 1000;
	/** How long the explosion shows after it is destroyed. */
	public static final int EXPLOSION_MILLIS = 1000;
	/** Points for each hit that lands. */
	public static final int POINTS_PER_HIT = 300;
	/** Extra points for destroying it. */
	public static final int POINTS_DESTROYED = 5000;

	/** Phase 1 colour (saturn-gold). */
	public static final Color PHASE_1_COLOR = EnemyShip.SATURN_GOLD;
	/** Phase 2 colour (saturn-butterscotch). */
	public static final Color PHASE_2_COLOR = EnemyShip.SATURN_BUTTERSCOTCH;
	/** Shotgun warning flash. */
	public static final Color WARNING_COLOR = Color.WHITE;

	/** Gap kept between it and the screen edge. */
	private static final int EDGE_MARGIN = 4;
	/** Sprite frame swap interval. */
	private static final int ANIMATION_MILLIS = 500;
	/** Colour swap interval while flashing. */
	private static final int FLASH_INTERVAL = 100;
	/** The explosion sprite is drawn this many screen pixels per pixel. */
	private static final int EXPLOSION_PIXEL = 4;
	/** Delay between the first fan and the first burst, so they alternate. */
	private static final int BURST_OFFSET_MILLIS = 1000;
	/** Player row used before the game tells it where the player is. */
	private static final int DEFAULT_PLAYER_Y = 480;

	/** Where it is in the shotgun swoop. */
	private enum Swoop { NONE, WARNING, DOWN, HOLD, UP }

	/** Remaining health. */
	private int hp = MAX_HP;
	/** True once the level's start countdown has ended (startFight). */
	private boolean started;
	/** True once it has been destroyed. */
	private boolean destroyed;
	/** True in phase 2 (half health or less). */
	private boolean phase2;
	/** Exact sideways position; positionX is this rounded. */
	private double exactX;
	/** Current sideways speed, signed. */
	private double speedX = SPEED_X;
	/** Its row once it has flown in (top edge, before bobbing). */
	private final int rowY;
	/** Where the fly-in starts, just above the screen. */
	private final int startY;
	/** Player centre, for the swoop depth. */
	private int playerY = DEFAULT_PLAYER_Y;
	/** Current swoop step. */
	private Swoop swoop = Swoop.NONE;
	/** Top edge where the current swoop step started. */
	private int swoopFromY;
	/** Top edge at the bottom of the swoop. */
	private int swoopLowY;
	/** True once this swoop's shotgun has fired. */
	private boolean shotgunFired;

	/** When the fly-in ends. */
	private long introEndsAt;
	/** When the bob clock started. */
	private long bobStart;
	/** When the wings fire next. */
	private long nextFanAt;
	/** When the centre cannon bursts next. */
	private long nextBurstAt;
	/** When the next shotgun swoop starts (phase 2). */
	private long nextShotgunAt;
	/** When the current swoop step ends. */
	private long swoopStepEndsAt;
	/** When the sprite frame next swaps. */
	private long nextFrameAt;
	/** When the phase 2 flash ends. */
	private long flashEndsAt;
	/** When it was destroyed. */
	private long destroyedAt;

	/**
	 * Creates the Ring Dreadnought. It stays hidden above the screen until
	 * {@link #startFight()}, then flies down to its row for
	 * {@link #INTRO_MILLIS}.
	 *
	 * @param positionX
	 *            Left edge.
	 * @param positionY
	 *            Top edge once it has flown in.
	 */
	public RingDreadnought(final int positionX, final int positionY) {
		super(positionX, -16 * 2, 24 * 2, 16 * 2, PHASE_1_COLOR);
		this.spriteType = SpriteType.RingDreadnought1;
		this.exactX = positionX;
		this.rowY = positionY;
		this.startY = -this.height;
	}

	/**
	 * Starts its clock when play begins: the fly-in starts now and every
	 * timer counts from here. Calling it again does nothing.
	 */
	@Override
	public final void startFight() {
		if (this.started)
			return;
		this.started = true;
		long now = System.currentTimeMillis();
		this.introEndsAt = now + INTRO_MILLIS;
		this.bobStart = this.introEndsAt;
		this.nextFanAt = this.introEndsAt + FAN_MILLIS / 2;
		this.nextBurstAt = this.nextFanAt + BURST_OFFSET_MILLIS;
		this.nextFrameAt = now + ANIMATION_MILLIS;
	}

	/** @return True once play has begun (the countdown ended). */
	public final boolean isStarted() {
		return this.started;
	}

	/** @return True before it is in place: countdown and fly-in. */
	public final boolean isIntro() {
		return !this.started || System.currentTimeMillis() < this.introEndsAt;
	}

	/** @return True in phase 2 (half health or less). */
	public final boolean isPhase2() {
		return this.phase2;
	}

	/** @return True from the shotgun warning until it is back up. */
	public final boolean isSwooping() {
		return this.swoop != Swoop.NONE;
	}

	/** @return True while it flashes before a shotgun swoop. */
	public final boolean isShotgunWarning() {
		return this.swoop == Swoop.WARNING;
	}

	/** Swaps sprite frames, runs the swoop steps and sets the colour. */
	@Override
	public final void update() {
		if (this.destroyed || isIntro())
			return;
		long now = System.currentTimeMillis();

		if (now >= this.nextFrameAt) {
			this.spriteType = this.spriteType == SpriteType.RingDreadnought1
					? SpriteType.RingDreadnought2 : SpriteType.RingDreadnought1;
			this.nextFrameAt = now + ANIMATION_MILLIS;
		}

		if (this.phase2 && this.swoop == Swoop.NONE
				&& now >= this.nextShotgunAt) {
			this.swoop = Swoop.WARNING;
			this.swoopStepEndsAt = now + SHOTGUN_WARNING_MILLIS;
		}

		if (this.swoop == Swoop.WARNING)
			setColor((now / FLASH_INTERVAL) % 2 == 0 ? WARNING_COLOR
					: PHASE_2_COLOR);
		else if (this.phase2 && now < this.flashEndsAt)
			setColor((now / FLASH_INTERVAL) % 2 == 0 ? PHASE_1_COLOR
					: PHASE_2_COLOR);
		else
			setColor(this.phase2 ? PHASE_2_COLOR : PHASE_1_COLOR);
	}

	/**
	 * Flies in, then moves left and right with a small bob. During a
	 * shotgun swoop it stops sideways and dives towards the player, then
	 * returns to its row.
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
			double eased = 1.0 - (1.0 - progress) * (1.0 - progress);
			this.positionY = (int) Math.round(this.startY
					+ (this.rowY - this.startY) * eased);
			return;
		}

		if (this.swoop == Swoop.NONE || this.swoop == Swoop.WARNING) {
			moveSideways(screenWidth);
			this.positionY = bobY(now);
		}
		if (this.swoop != Swoop.NONE)
			advanceSwoop(now);
	}

	/** Moves left and right, bouncing off the screen edges. */
	private void moveSideways(final int screenWidth) {
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
	}

	/** @return Top edge with the bob: a slow wave around its row. */
	private int bobY(final long now) {
		double wave = Math.sin(2 * Math.PI * (now - this.bobStart)
				/ BOB_MILLIS);
		return this.rowY + (int) Math.round(BOB_RANGE * (wave + 1) / 2);
	}

	/** Runs the swoop: warning, down, hold (shotgun fires), back up. */
	private void advanceSwoop(final long now) {
		switch (this.swoop) {
		case WARNING:
			if (now >= this.swoopStepEndsAt) {
				this.swoop = Swoop.DOWN;
				this.swoopFromY = this.positionY;
				this.swoopLowY = Math.max(this.positionY,
						this.playerY - SWOOP_GAP - this.height);
				this.swoopStepEndsAt = now + SWOOP_DOWN_MILLIS;
				this.shotgunFired = false;
			}
			break;
		case DOWN:
			this.positionY = ease(this.swoopFromY, this.swoopLowY, now,
					SWOOP_DOWN_MILLIS);
			if (now >= this.swoopStepEndsAt) {
				this.positionY = this.swoopLowY;
				this.swoop = Swoop.HOLD;
				this.swoopStepEndsAt = now + SWOOP_HOLD_MILLIS;
			}
			break;
		case HOLD:
			if (now >= this.swoopStepEndsAt) {
				this.swoop = Swoop.UP;
				this.swoopFromY = this.positionY;
				this.swoopStepEndsAt = now + SWOOP_UP_MILLIS;
			}
			break;
		case UP:
			int backY = bobY(now);
			this.positionY = ease(this.swoopFromY, backY, now, SWOOP_UP_MILLIS);
			if (now >= this.swoopStepEndsAt) {
				this.positionY = backY;
				this.swoop = Swoop.NONE;
				this.nextShotgunAt = now + SHOTGUN_MILLIS;
				// Volleys wait a moment after it is back up.
				this.nextFanAt = Math.max(this.nextFanAt,
						now + FAN_PHASE_2_MILLIS / 2);
				this.nextBurstAt = Math.max(this.nextBurstAt,
						now + BURST_OFFSET_MILLIS);
			}
			break;
		default:
			break;
		}
	}

	/** @return Height between from and to for the step ending at stepEnds. */
	private int ease(final int from, final int to, final long now,
			final int duration) {
		double progress = 1.0 - (this.swoopStepEndsAt - now)
				/ (double) duration;
		progress = Math.max(0, Math.min(1, progress));
		double eased = progress * progress * (3 - 2 * progress);
		return (int) Math.round(from + (to - from) * eased);
	}

	/** Remembers the player's height for how deep to swoop. */
	@Override
	public final void setTarget(final int x, final int y) {
		this.playerY = y;
	}

	/**
	 * Wing fans and the spreading burst on their timers, and the shotgun
	 * at the bottom of a swoop. Fans and bursts wait while it swoops.
	 *
	 * @return True if anything was fired.
	 */
	@Override
	public final boolean shoot(final Set<Bullet> bullets,
			final GameSettings settings) {
		if (this.destroyed || isIntro())
			return false;
		long now = System.currentTimeMillis();
		if (this.swoop == Swoop.HOLD && !this.shotgunFired) {
			this.shotgunFired = true;
			fireShotgun(bullets);
			return true;
		}
		if (isSwooping())
			return false;
		boolean fired = false;
		if (now >= this.nextFanAt) {
			this.nextFanAt = now + (this.phase2 ? FAN_PHASE_2_MILLIS
					: FAN_MILLIS);
			fireFans(bullets);
			fired = true;
		}
		if (now >= this.nextBurstAt) {
			this.nextBurstAt = now + BURST_MILLIS;
			fireBurst(bullets);
			fired = true;
		}
		return fired;
	}

	/**
	 * Both wings: 3 bullets each, spreading outwards. The left wing tip is
	 * its left edge and the right wing tip its right edge.
	 */
	private void fireFans(final Set<Bullet> bullets) {
		int y = this.positionY + this.height / 2;
		for (int i = 0; i < FAN_BULLETS; i++) {
			bullets.add(bullet(this.positionX, y, FAN_SPEED_Y,
					FAN_SPEEDS_X[i], Integer.MAX_VALUE));
			bullets.add(bullet(this.positionX + this.width, y, FAN_SPEED_Y,
					-FAN_SPEEDS_X[i], Integer.MAX_VALUE));
		}
	}

	/** Centre cannon: a tight group that opens up as it falls. */
	private void fireBurst(final Set<Bullet> bullets) {
		int x = this.positionX + this.width / 2;
		int y = this.positionY + this.height;
		for (int speed : BURST_SPEEDS_X)
			bullets.add(bullet(x, y, BURST_SPEED_Y, speed, Integer.MAX_VALUE));
	}

	/** Shotgun: 5 close pellets that vanish after a short distance. */
	private void fireShotgun(final Set<Bullet> bullets) {
		int x = this.positionX + this.width / 2;
		int y = this.positionY + this.height;
		for (int speed : SHOTGUN_SPEEDS_X)
			bullets.add(bullet(x, y, SHOTGUN_SPEED_Y, speed,
					y + SHOTGUN_RANGE));
	}

	/** @return An enemy bullet from the pool with the given motion. */
	private static Bullet bullet(final int x, final int y, final int speedY,
			final int speedX, final int maxY) {
		Bullet bullet = BulletPool.getBullet(x, y, speedY);
		bullet.setSpeedX(speedX);
		bullet.setMaxY(maxY);
		return bullet;
	}

	/**
	 * A player bullet reached it. While flying in it just disappears.
	 * Otherwise it loses 1 HP; the hit that brings it to half health starts
	 * phase 2.
	 *
	 * @return Points earned.
	 */
	@Override
	public final int manageCollisionsBoss(final Bullet bullet) {
		if (this.destroyed || isIntro())
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
			this.nextShotgunAt = now + FLASH_MILLIS + SHOTGUN_WARNING_MILLIS;
		}
		return points;
	}

	/** Destroys it with a big explosion. */
	@Override
	public final void destroy() {
		if (this.destroyed)
			return;
		this.destroyed = true;
		this.hp = 0;
		this.swoop = Swoop.NONE;
		this.destroyedAt = System.currentTimeMillis();
		this.spriteType = SpriteType.Explosion;
		SoundManager.getInstance().playSfx(Sound.ENEMY_EXPLOSION);
	}

	/** Hidden before the fight starts and once destroyed (explosion). */
	@Override
	public final boolean isVisible() {
		return this.started && !this.destroyed;
	}

	/** Once destroyed, draws a big explosion where it was. */
	@Override
	public final void drawWarnings(final DrawManager drawManager) {
		if (!this.destroyed || !this.started)
			return;
		long elapsed = System.currentTimeMillis() - this.destroyedAt;
		if (elapsed >= EXPLOSION_MILLIS)
			return;
		int[] size = drawManager.getSpriteSize(SpriteType.Explosion);
		int x = this.positionX + this.width / 2 - size[0] * EXPLOSION_PIXEL / 2;
		int y = this.positionY + this.height / 2 - size[1] * EXPLOSION_PIXEL / 2;
		Color color = (elapsed / FLASH_INTERVAL) % 2 == 0 ? PHASE_1_COLOR
				: Color.WHITE;
		drawManager.drawSpriteScaled(SpriteType.Explosion, x, y,
				EXPLOSION_PIXEL, color);
	}

	/** @return The warning while it flies in, otherwise null. */
	@Override
	public final String getWarningText() {
		return this.started && !this.destroyed && isIntro() ? WARNING_TEXT
				: null;
	}

	/** Keeps every timer still while the game is paused. */
	@Override
	public final void resumeAfterPause(final long pausedMilliseconds) {
		this.introEndsAt += pausedMilliseconds;
		this.bobStart += pausedMilliseconds;
		this.nextFanAt += pausedMilliseconds;
		this.nextBurstAt += pausedMilliseconds;
		this.nextShotgunAt += pausedMilliseconds;
		this.swoopStepEndsAt += pausedMilliseconds;
		this.nextFrameAt += pausedMilliseconds;
		this.flashEndsAt += pausedMilliseconds;
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
