package entity;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.RadialGradientPaint;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Path2D;
import java.util.Set;

import engine.DrawManager;
import engine.DrawManager.SpriteType;
import engine.GameSettings;

/**
 * Level 40 boss, AI Commander (GoG - GOG-77, art GOG-82).
 *
 * Phase 1: she drifts left and right. Every laser cycle she locks onto the
 * player's x, glides so her jaw is over it, charges for 2.1 s (WARNING
 * banner, orb at the jaw, aim line), then fires the Ultimate Weapon, a beam
 * straight down to the ground. Move out of the column before it fires.
 *
 * Phase 2 (half health): the Claw Slam, two claws that swing in from the top
 * corners and cover the whole bottom of the screen. It can't be dodged: the
 * player loses a life, or only the shield. It comes once when phase 2
 * starts, then every 30 s. Between slams: laser, then 2 s invisible (she
 * doesn't shoot while hidden, but bullets still hit her, with a small
 * flash), then again.
 *
 * Attack designs come from claude/level40-boss-attacks-handoff.md; numbers
 * from the Game Design Draft (Level 40) where the handoff leaves them open.
 * Every coordinate is in the game's 448 x 520 screen.
 */
public class AICommander extends Entity implements Boss {

	// ---- Fight numbers ----

	/** Hits needed to destroy her. */
	public static final int MAX_HP = 30;
	/** Phase 2 starts at or below this health (half of MAX_HP). */
	public static final int PHASE_2_HP = MAX_HP / 2;
	/** Shown before she appears. */
	public static final String WARNING_TEXT = "WARNING: BOSS INCOMING";
	/** How long the intro warning shows. */
	public static final int INTRO_MILLIS = 2000;
	/** Points for each hit that lands. */
	public static final int POINTS_PER_HIT = 300;
	/** Extra points for destroying her. */
	public static final int POINTS_DESTROYED = 5000;

	// ---- Laser (Ultimate Weapon) timeline, ms into one cycle ----

	/** Charging ends and the beam starts growing from the jaw. */
	public static final int LASER_CHARGE_END = 2100;
	/** The beam reaches the ground. */
	public static final int LASER_GROW_END = 2200;
	/** The beam starts fading (it stops hurting here). */
	public static final int LASER_FIRE_END = 3000;
	/** The beam is gone. */
	public static final int LASER_FADE_END = 3160;
	/** Pause done: the cycle is over. */
	public static final int LASER_CYCLE_END = 3400;
	/** Beam width; also its damage hitbox width. */
	public static final int BEAM_WIDTH = 24;
	/**
	 * Ground line the beam reaches, measured from the bottom of the screen:
	 * just under the player's row (y 490 in the 496-high design canvas;
	 * the real canvas height depends on the window border).
	 */
	public static final int GROUND_FROM_BOTTOM = 6;
	/** Rest between laser cycles in phase 1. */
	public static final int IDLE_MILLIS = 1000;

	// ---- Phase 2 ----

	/** How long she stays invisible after each phase 2 laser. */
	public static final int INVISIBLE_MILLIS = 2000;
	/** Rest after invisibility (and after a claw) in phase 2. */
	public static final int IDLE_PHASE_2_MILLIS = 600;
	/** How long a bullet hit makes her flash while invisible. */
	public static final int HIT_FLASH_MILLIS = 120;
	/** Time between Claw Slams (the first one comes when phase 2 starts). */
	public static final int CLAW_EVERY_MILLIS = 30000;
	/** How long she flashes between colours when phase 2 starts. */
	public static final int PHASE_FLASH_MILLIS = 1000;

	// ---- Claw Slam timeline, ms into the attack ----

	/** Claws start swinging in from off screen. */
	public static final int CLAW_SWING_START = 320;
	/** Impact: the claws meet and the player is hit. */
	public static final int CLAW_IMPACT = 1600;
	/** Bounce back after the overshoot. */
	public static final int CLAW_BOUNCE = 1720;
	/** Settled at rest. */
	public static final int CLAW_SETTLE = 1840;
	/** Claws start swinging back out. */
	public static final int CLAW_RETREAT_START = 2960;
	/** Claws are off screen again. */
	public static final int CLAW_RETREAT_END = 3680;
	/** The attack is over. */
	public static final int CLAW_END = 4000;

	// ---- Palette (Act 4 design system) ----

	/** Phase 1 colour, also the laser and banner colour (user's pick). */
	public static final Color GALAXY = new Color(0xb98aff);
	/** Phase 2 colour. */
	public static final Color STARLIGHT = new Color(0xf7f2ff);
	/** Claw spikes and arm segments. */
	public static final Color VOID = new Color(0x7f74ff);
	/** Shield bubble and "SHIELD BROKEN". */
	public static final Color ION = new Color(0x5ee6ef);
	/** Holes and outlines. */
	public static final Color SPACE_BLACK = new Color(0x05060a);
	/** Laser and WARNING banner colour (space-galaxy instead of red). */
	public static final Color ALERT = GALAXY;

	// ---- Movement ----

	/** Sideways drift between attacks, px per frame. */
	private static final int DRIFT_SPEED = 1;
	/** Glide speed while lining up over the player, px per frame. */
	private static final int GLIDE_SPEED = 6;
	/** Gap kept from the screen edges. */
	private static final int EDGE_MARGIN = 4;
	/** Jaw position inside the sprite (24 x 16 sprite at 2x). */
	private static final int JAW_OFFSET_X = 24;
	/** Jaw position inside the sprite. */
	private static final int JAW_OFFSET_Y = 28;
	/** Sprite frame swap interval. */
	private static final int ANIMATION_MILLIS = 500;
	/**
	 * A gap this long between updates means the game froze her (item); the
	 * gap doesn't count, so no attack runs out or fires unseen.
	 */
	private static final int FROZEN_GAP_MILLIS = 100;

	/** What she is doing. */
	enum Action {
		/** Resting / drifting. */
		IDLE,
		/** Laser cycle: charge, fire, fade. */
		LASER,
		/** Phase 2: hidden, not shooting. */
		INVISIBLE,
		/** Phase 2: Claw Slam. */
		CLAW
	}

	private int hp = MAX_HP;
	private boolean started;
	private boolean destroyed;
	private boolean phase2;

	/** Time spent paused or frozen, taken off her clock. */
	private long pausedTotal;
	/** Last real time update() ran, to spot freezes. */
	private long lastRealUpdate;

	private long introEndsAt;
	private Action action = Action.IDLE;
	private long actionStart;
	private long actionLength;
	private long nextClawAt = Long.MAX_VALUE;
	private long phase2StartedAt = -1;
	private long hitFlashUntil;
	private long nextFrameAt;

	/** Player centre, updated every frame by GameScreen. */
	private int targetX = 224;
	private int targetY = 474;
	/** Column the current laser locked onto (beam centre x). */
	private int lockX = 224;
	/** True once the current laser hit the player. */
	private boolean laserHitDone;
	/** True once the current claw hit the player. */
	private boolean clawHitDone;
	/** Hits waiting for GameScreen. */
	private int pendingHits;
	/** When the last claw hit (effects), -1 for never. */
	private long clawImpactAt = -1;
	/** When the shield popped on a claw, -1 for never. */
	private long shieldBrokenAt = -1;
	/** Player centre when the shield popped. */
	private int shieldX;
	private int shieldY;
	private int sideways = DRIFT_SPEED;
	private int screenWidth = 448;
	private int screenHeight = 496;

	/**
	 * Creates the AI Commander. She stays hidden until
	 * {@link #startFight()} (end of the level's start countdown).
	 *
	 * @param positionX
	 *            Left edge.
	 * @param positionY
	 *            Top edge.
	 */
	public AICommander(final int positionX, final int positionY) {
		super(positionX, positionY, 24 * 2, 16 * 2, GALAXY);
		this.spriteType = SpriteType.Commander1;
	}

	/** @return Her clock: real time minus pauses and freezes. */
	private long now() {
		return System.currentTimeMillis() - this.pausedTotal;
	}

	@Override
	public final void startFight() {
		if (this.started)
			return;
		this.started = true;
		this.lastRealUpdate = System.currentTimeMillis();
		long now = now();
		this.introEndsAt = now + INTRO_MILLIS;
		this.nextFrameAt = now + ANIMATION_MILLIS;
		startIdle(this.introEndsAt, IDLE_MILLIS);
	}

	/** @return True once play has begun (the countdown ended). */
	public final boolean isStarted() {
		return this.started;
	}

	/** @return True before she appears (countdown and warning). */
	public final boolean isIntro() {
		return !this.started || now() < this.introEndsAt;
	}

	/** @return True in phase 2 (half health or less). */
	public final boolean isPhase2() {
		return this.phase2;
	}

	/** @return What she is doing right now. */
	final Action getAction() {
		return this.action;
	}

	/** @return Milliseconds into the current action. */
	private long actionTime() {
		return now() - this.actionStart;
	}

	private void startIdle(final long at, final long length) {
		this.action = Action.IDLE;
		this.actionStart = at;
		this.actionLength = length;
	}

	private void startLaser(final long at) {
		this.action = Action.LASER;
		this.actionStart = at;
		this.laserHitDone = false;
		int minX = EDGE_MARGIN + JAW_OFFSET_X;
		int maxX = this.screenWidth - EDGE_MARGIN - (this.width - JAW_OFFSET_X);
		this.lockX = Math.max(minX, Math.min(maxX, this.targetX));
	}

	private void startInvisible(final long at) {
		this.action = Action.INVISIBLE;
		this.actionStart = at;
		this.actionLength = INVISIBLE_MILLIS;
	}

	private void startClaw(final long at) {
		this.action = Action.CLAW;
		this.actionStart = at;
		this.clawHitDone = false;
		this.nextClawAt = at + CLAW_EVERY_MILLIS;
	}

	/** Runs her timeline: attacks, animation and colour. */
	@Override
	public final void update() {
		if (!this.started || this.destroyed)
			return;
		long real = System.currentTimeMillis();
		long gap = real - this.lastRealUpdate;
		if (gap > FROZEN_GAP_MILLIS)
			this.pausedTotal += gap; // frozen by an item: stop her clock too
		this.lastRealUpdate = real;
		if (isIntro())
			return;
		long now = now();

		if (now >= this.nextFrameAt) {
			this.spriteType = this.spriteType == SpriteType.Commander1
					? SpriteType.Commander2 : SpriteType.Commander1;
			this.nextFrameAt = now + ANIMATION_MILLIS;
		}

		long t = actionTime();
		switch (this.action) {
		case IDLE:
			if (this.phase2 && now >= this.nextClawAt)
				startClaw(now);
			else if (t >= this.actionLength)
				startLaser(now);
			break;
		case LASER:
			if (t >= LASER_CYCLE_END) {
				if (this.phase2)
					startInvisible(now);
				else
					startIdle(now, IDLE_MILLIS);
			}
			break;
		case INVISIBLE:
			if (t >= this.actionLength)
				startIdle(now, IDLE_PHASE_2_MILLIS);
			break;
		case CLAW:
			if (t >= CLAW_IMPACT && !this.clawHitDone) {
				this.clawHitDone = true;
				this.pendingHits++;
				this.clawImpactAt = this.actionStart + CLAW_IMPACT;
			}
			if (t >= CLAW_END)
				startIdle(now, IDLE_PHASE_2_MILLIS);
			break;
		default:
			break;
		}

		// Phase 2 starts with a short flash between both colours.
		if (this.phase2 && now - this.phase2StartedAt < PHASE_FLASH_MILLIS)
			setColor(((now - this.phase2StartedAt) / 100) % 2 == 0 ? GALAXY
					: STARLIGHT);
		else
			setColor(this.phase2 ? STARLIGHT : GALAXY);
	}

	/**
	 * Drifts left and right; glides over the locked column during a laser;
	 * holds still during a claw.
	 */
	@Override
	public final void moveBoss(final int screenWidth, final int screenHeight,
			final GameSettings settings) {
		this.screenWidth = screenWidth;
		this.screenHeight = screenHeight;
		if (!this.started || this.destroyed || isIntro())
			return;
		int minX = EDGE_MARGIN;
		int maxX = screenWidth - this.width - EDGE_MARGIN;
		switch (this.action) {
		case LASER:
			int goal = this.lockX - JAW_OFFSET_X;
			int step = Math.max(-GLIDE_SPEED, Math.min(GLIDE_SPEED,
					goal - this.positionX));
			this.positionX += step;
			break;
		case CLAW:
			break;
		default:
			this.positionX += this.sideways;
			if (this.positionX <= minX) {
				this.positionX = minX;
				this.sideways = DRIFT_SPEED;
			} else if (this.positionX >= maxX) {
				this.positionX = maxX;
				this.sideways = -DRIFT_SPEED;
			}
			break;
		}
	}

	/** She has no normal bullets; her attacks are the laser and the claw. */
	@Override
	public final boolean shoot(final Set<Bullet> bullets,
			final GameSettings settings) {
		return false;
	}

	@Override
	public final void setTarget(final int x, final int y) {
		this.targetX = x;
		this.targetY = y;
	}

	/**
	 * Laser hits are checked here against the ship (once per beam); claw
	 * hits were queued at impact.
	 */
	@Override
	public final int takePlayerHits(final Entity ship) {
		if (this.started && !this.destroyed && this.action == Action.LASER
				&& !this.laserHitDone && ship != null) {
			Rectangle beam = beamBounds(actionTime());
			if (beam != null && beam.intersects(new Rectangle(
					ship.getPositionX(), ship.getPositionY(),
					ship.getWidth(), ship.getHeight()))) {
				this.laserHitDone = true;
				this.pendingHits++;
			}
		}
		int hits = this.pendingHits;
		this.pendingHits = 0;
		return hits;
	}

	/**
	 * @param t
	 *            Milliseconds into the laser cycle.
	 * @return The beam's damage area at that moment, or null when it can't
	 *         hurt (charging, fading, over).
	 */
	final Rectangle beamBounds(final long t) {
		if (t < LASER_CHARGE_END || t >= LASER_FIRE_END)
			return null;
		int jawY = this.positionY + JAW_OFFSET_Y;
		double grow = Math.min(1.0, (t - LASER_CHARGE_END)
				/ (double) (LASER_GROW_END - LASER_CHARGE_END));
		int bottom = jawY + (int) Math.round((groundY() - jawY) * grow);
		return new Rectangle(this.lockX - BEAM_WIDTH / 2, jawY, BEAM_WIDTH,
				Math.max(1, bottom - jawY));
	}

	/** @return Ground line: where the beam ends and the claws meet. */
	public final int groundY() {
		return this.screenHeight - GROUND_FROM_BOTTOM;
	}

	/** @return The column the current laser locked onto. */
	public final int getLockX() {
		return this.lockX;
	}

	@Override
	public final void onPlayerHitBlocked() {
		if (this.action == Action.CLAW) {
			this.shieldBrokenAt = now();
			this.shieldX = this.targetX;
			this.shieldY = this.targetY;
		}
	}

	@Override
	public final int manageCollisionsBoss(final Bullet bullet) {
		if (!this.started || this.destroyed || isIntro())
			return 0;
		long now = now();
		this.hp--;
		if (this.action == Action.INVISIBLE)
			this.hitFlashUntil = now + HIT_FLASH_MILLIS;
		int points = POINTS_PER_HIT;
		if (this.hp <= 0) {
			destroy();
			points += POINTS_DESTROYED;
		} else if (!this.phase2 && this.hp <= PHASE_2_HP) {
			// Phase 2: the first Claw Slam comes right away.
			this.phase2 = true;
			this.phase2StartedAt = now;
			startClaw(now);
		}
		return points;
	}

	@Override
	public final void destroy() {
		this.destroyed = true;
		this.spriteType = SpriteType.Explosion;
		this.action = Action.IDLE;
	}

	/** Hidden during the intro and while invisible (except a hit flash). */
	@Override
	public final boolean isVisible() {
		if (isIntro())
			return false;
		if (this.destroyed || this.action != Action.INVISIBLE)
			return true;
		return now() < this.hitFlashUntil;
	}

	@Override
	public final String getWarningText() {
		return this.started && !this.destroyed && isIntro() ? WARNING_TEXT
				: null;
	}

	@Override
	public final void resumeAfterPause(final long pausedMilliseconds) {
		this.pausedTotal += pausedMilliseconds;
		this.lastRealUpdate += pausedMilliseconds;
	}

	/** Screen shake after a claw impact (handoff table). */
	@Override
	public final Point getShakeOffset() {
		if (this.clawImpactAt < 0)
			return null;
		long dt = now() - this.clawImpactAt;
		if (dt < 0 || dt >= 400)
			return null;
		if (dt < 40)
			return new Point(-9, 7);
		if (dt < 80)
			return new Point(8, -6);
		if (dt < 120)
			return new Point(-7, -7);
		if (dt < 160)
			return new Point(6, 6);
		if (dt < 240)
			return new Point(-4, 3);
		if (dt < 320)
			return new Point(3, -2);
		return new Point(0, 0);
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

	// =====================================================================
	// Drawing
	// =====================================================================

	/** Laser and claws, drawn with the playfield (shaken). */
	@Override
	public final void drawWarnings(final DrawManager drawManager) {
		if (!this.started || this.destroyed || isIntro())
			return;
		Graphics2D g = (Graphics2D) drawManager.getWorldGraphics().create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		long t = actionTime();
		if (this.action == Action.LASER)
			drawLaser(g, drawManager, t);
		if (this.action == Action.CLAW)
			drawClaws(g, t);
		drawImpactGlow(g);
		g.dispose();
	}

	/** Screen flash and "SHIELD BROKEN", drawn on top without shake. */
	@Override
	public final void drawOverlay(final DrawManager drawManager) {
		if (!this.started)
			return;
		Graphics2D g = (Graphics2D) drawManager.getWorldGraphics().create();
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		long now = now();
		if (this.clawImpactAt >= 0) {
			long dt = now - this.clawImpactAt;
			if (dt >= 0 && dt < 280) {
				alpha(g, 0.95f * (1f - dt / 280f));
				g.setColor(STARLIGHT);
				g.fillRect(-20, -20, this.screenWidth + 40, this.screenHeight + 40);
			}
		}
		if (this.shieldBrokenAt >= 0) {
			long dt = now - this.shieldBrokenAt;
			if (dt >= 0 && dt < 320) {
				// The bubble pops: grows to 1.3, then 1.7, fading out.
				float u = dt / 320f;
				double scale = u < 0.4 ? 1 + 0.3 * (u / 0.4)
						: 1.3 + 0.4 * ((u - 0.4) / 0.6);
				double w = 40 * scale, h = 36 * scale;
				alpha(g, 1f - u);
				g.setColor(new Color(0x5e, 0xe6, 0xef, 56));
				g.fill(new Ellipse2D.Double(this.shieldX - w / 2,
						this.shieldY - h / 2, w, h));
				g.setColor(ION);
				g.setStroke(new BasicStroke(2f));
				g.draw(new Ellipse2D.Double(this.shieldX - w / 2,
						this.shieldY - h / 2, w, h));
			}
			if (dt >= 0 && dt < 800) {
				float u = dt / 800f;
				alpha(g, 1f - u);
				g.setColor(ION);
				g.setFont(drawManager.getPixelFont(18f, true));
				String text = "SHIELD BROKEN";
				int tw = g.getFontMetrics().stringWidth(text);
				g.drawString(text, (this.screenWidth - tw) / 2,
						268 - Math.round(16 * u));
			}
		}
		g.dispose();
	}

	private static void alpha(final Graphics2D g, final float a) {
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				Math.max(0f, Math.min(1f, a))));
	}

	private static Color withAlpha(final Color c, final float a) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(),
				Math.max(0, Math.min(255, Math.round(a * 255))));
	}

	/** Linear interpolation between evenly spaced keyframes. */
	private static double keyframes(final double[] values, final double u) {
		double p = Math.max(0, Math.min(1, u)) * (values.length - 1);
		int i = Math.min(values.length - 2, (int) p);
		return values[i] + (values[i + 1] - values[i]) * (p - i);
	}

	// ---- Laser ----

	private void drawLaser(final Graphics2D g, final DrawManager dm,
			final long t) {
		int jawX = this.lockX;
		int jawY = this.positionY + JAW_OFFSET_Y;

		if (t < LASER_CHARGE_END) {
			// Aim line: dashed, flickering every ~0.27 s.
			alpha(g, (t / 270) % 2 == 0 ? 0.7f : 0.15f);
			g.setColor(ALERT);
			for (int y = jawY + 12; y < groundY(); y += 10)
				g.fillRect(jawX - 1, y, 2, Math.min(6, groundY() - y));
		}

		// Charge orb: pulses while growing, becomes the muzzle flare.
		double orb;
		float orbAlpha = 1f;
		if (t < LASER_CHARGE_END)
			orb = keyframes(new double[] {0.2, 0.5, 0.35, 0.75, 0.55, 1.0,
					0.8, 1.25}, t / (double) LASER_CHARGE_END);
		else if (t < LASER_GROW_END)
			orb = 1.6;
		else if (t < LASER_FIRE_END)
			orb = 1.45;
		else {
			orb = 1.45;
			orbAlpha = Math.max(0f, 1f - (t - LASER_FIRE_END)
					/ (float) (LASER_FADE_END - LASER_FIRE_END));
		}
		if (t < LASER_FADE_END)
			drawOrb(g, jawX, jawY, orb, orbAlpha);

		if (t >= LASER_CHARGE_END && t < LASER_FADE_END)
			drawBeam(g, jawX, jawY, t);

		if (t < LASER_CHARGE_END) {
			float blink = (t / 340) % 2 == 0 ? 1f : 0.15f;
			drawBanner(g, dm, blink);
		}
	}

	private void drawOrb(final Graphics2D g, final int cx, final int cy,
			final double scale, final float a) {
		double r = 16 * scale;
		alpha(g, 0.35f * a);
		g.setColor(ALERT);
		g.fill(new Ellipse2D.Double(cx - r * 1.6, cy - r * 1.6, r * 3.2,
				r * 3.2));
		alpha(g, a);
		g.setPaint(new RadialGradientPaint(cx, cy, (float) Math.max(1, r),
				new float[] {0f, 0.18f, 0.42f, 0.7f, 1f},
				new Color[] {Color.WHITE, Color.WHITE, ALERT,
						withAlpha(ALERT, 0f), withAlpha(ALERT, 0f)}));
		g.fill(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
		g.setColor(Color.WHITE);
		int bar = (int) Math.round(24 * scale);
		g.fillRect(cx - bar, cy - 1, bar * 2, 2);
		g.fillRect(cx - 1, cy - bar, 2, bar * 2);
	}

	private void drawBeam(final Graphics2D g, final int x, final int jawY,
			final long t) {
		double grow = Math.min(1.0, (t - LASER_CHARGE_END)
				/ (double) (LASER_GROW_END - LASER_CHARGE_END));
		int bottom = jawY + (int) Math.round((groundY() - jawY) * grow);
		float a;
		if (t < LASER_FIRE_END)
			a = t < LASER_GROW_END ? 1f : ((t / 60) % 2 == 0 ? 1f : 0.8f);
		else
			a = Math.max(0f, 1f - (t - LASER_FIRE_END)
					/ (float) (LASER_FADE_END - LASER_FIRE_END));
		int h = Math.max(1, bottom - jawY);
		int half = BEAM_WIDTH / 2;

		// Wide soft glow.
		alpha(g, 0.18f * a);
		g.setColor(ALERT);
		g.fillRect(x - 28, jawY, 56, h);
		// Outer layer: transparent edges, solid middle.
		alpha(g, a);
		g.setPaint(new LinearGradientPaint(x - half, 0, x + half, 0,
				new float[] {0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f},
				new Color[] {withAlpha(ALERT, 0f), withAlpha(ALERT, 0.45f),
						ALERT, ALERT, withAlpha(ALERT, 0.45f),
						withAlpha(ALERT, 0f)}));
		g.fillRect(x - half, jawY, BEAM_WIDTH, h);
		// Core: white strip with a glow.
		alpha(g, 0.35f * a);
		g.setColor(Color.WHITE);
		g.fillRect(x - 5, jawY, 10, h);
		alpha(g, a);
		g.fillRect(x - 3, jawY, 6, h);
		// Short streaks just outside the beam.
		g.setColor(ALERT);
		for (int i = 0; i < 6; i++) {
			int sy = jawY + (int) ((i * 61 + t / 3) % Math.max(1, h));
			int sx = i % 2 == 0 ? x - half - 5 : x + half + 3;
			g.fillRect(sx, sy, 2, 10);
		}
		if (grow >= 1.0)
			drawImpactBurst(g, x, groundY(), t, a);
	}

	private void drawImpactBurst(final Graphics2D g, final int cx,
			final int cy, final long t, final float a) {
		double pulse = 1.05 + 0.15 * Math.sin(t / 40.0);
		double w = 64 * pulse, h = 40 * pulse;
		alpha(g, a);
		g.setPaint(new RadialGradientPaint(cx, cy, (float) (w / 2),
				new float[] {0f, 0.35f, 1f},
				new Color[] {Color.WHITE, ALERT, withAlpha(ALERT, 0f)}));
		AffineTransform saved = g.getTransform();
		g.translate(cx, cy);
		g.scale(1.0, h / w);
		g.fill(new Ellipse2D.Double(-w / 2, -w / 2, w, w));
		g.setTransform(saved);
		g.setColor(Color.WHITE);
		g.setStroke(new BasicStroke(2f));
		for (int deg : new int[] {-72, -45, -20, 0, 20, 45, 72}) {
			double rad = Math.toRadians(deg);
			double len = 30 * pulse;
			g.drawLine(cx, cy, (int) Math.round(cx + Math.sin(rad) * len),
					(int) Math.round(cy - Math.cos(rad) * len));
		}
	}

	/** WARNING banner: 360 x 76 octagon at (44, 218), centred on screen. */
	private void drawBanner(final Graphics2D g, final DrawManager dm,
			final float blink) {
		int bw = 360, bh = 76, cut = 16;
		int bx = (this.screenWidth - bw) / 2, by = 218;
		Path2D box = new Path2D.Double();
		box.moveTo(bx + cut, by);
		box.lineTo(bx + bw - cut, by);
		box.lineTo(bx + bw, by + cut);
		box.lineTo(bx + bw, by + bh - cut);
		box.lineTo(bx + bw - cut, by + bh);
		box.lineTo(bx + cut, by + bh);
		box.lineTo(bx, by + bh - cut);
		box.lineTo(bx, by + cut);
		box.closePath();

		alpha(g, blink);
		g.setColor(SPACE_BLACK);
		g.fill(box);
		g.setColor(withAlpha(ALERT, 0.16f));
		g.fill(box);
		// Side tabs.
		g.setColor(ALERT);
		g.fillRect(bx - 6, by + (bh - 24) / 2, 6, 24);
		g.fillRect(bx + bw, by + (bh - 24) / 2, 6, 24);
		g.setStroke(new BasicStroke(3f));
		g.draw(box);
		// Hatched stripes, 6 on / 4 off.
		for (int sy : new int[] {by + 9, by + bh - 9 - 4})
			for (int sx = bx + 80; sx < bx + 280; sx += 10)
				g.fillRect(sx, sy, Math.min(6, bx + 280 - sx), 4);

		// Content: <<< (!) WARNING (!) >>>
		Font font = dm.getPixelFont(30f, true);
		g.setFont(font);
		FontMetrics fm = g.getFontMetrics();
		String word = "WARNING";
		int spacing = 3;
		int textW = 0;
		for (char c : word.toCharArray())
			textW += fm.charWidth(c) + spacing;
		textW -= spacing;
		int chevW = 26, triW = 24, gap = 10;
		int total = chevW + gap + triW + gap + textW + gap + triW + gap + chevW;
		int x = bx + (bw - total) / 2;
		int midY = by + bh / 2;
		drawChevrons(g, x, midY, true, blink);
		x += chevW + gap;
		drawWarnTriangle(g, x, midY, triW, blink);
		x += triW + gap;
		int baseline = midY + (fm.getAscent() - fm.getDescent()) / 2;
		// Soft glow behind the text, then white letters.
		int cx = x;
		for (char c : word.toCharArray()) {
			String s = String.valueOf(c);
			alpha(g, 0.35f * blink);
			g.setColor(ALERT);
			for (int[] o : new int[][] {{-2, 0}, {2, 0}, {0, -2}, {0, 2}})
				g.drawString(s, cx + o[0], baseline + o[1]);
			alpha(g, blink);
			g.setColor(Color.WHITE);
			g.drawString(s, cx, baseline);
			cx += fm.charWidth(c) + spacing;
		}
		x += textW + gap;
		drawWarnTriangle(g, x, midY, triW, blink);
		x += triW + gap;
		drawChevrons(g, x, midY, false, blink);
	}

	/** Three 3 px chevrons, the outer one at 50% opacity. */
	private static void drawChevrons(final Graphics2D g, final int x,
			final int midY, final boolean left, final float blink) {
		g.setColor(ALERT);
		g.setStroke(new BasicStroke(3f, BasicStroke.CAP_SQUARE,
				BasicStroke.JOIN_MITER));
		for (int i = 0; i < 3; i++) {
			boolean outer = left ? i == 0 : i == 2;
			alpha(g, (outer ? 0.5f : 1f) * blink);
			int cx = x + i * 9;
			if (left) {
				g.drawLine(cx + 7, midY - 8, cx, midY);
				g.drawLine(cx, midY, cx + 7, midY + 8);
			} else {
				g.drawLine(cx, midY - 8, cx + 7, midY);
				g.drawLine(cx + 7, midY, cx, midY + 8);
			}
		}
	}

	/** Warning sign: triangle in the alert colour with a white "!". */
	private static void drawWarnTriangle(final Graphics2D g, final int x,
			final int midY, final int w, final float blink) {
		alpha(g, blink);
		Path2D tri = new Path2D.Double();
		tri.moveTo(x + w / 2.0, midY - w / 2.0);
		tri.lineTo(x + w, midY + w / 2.0 - 2);
		tri.lineTo(x, midY + w / 2.0 - 2);
		tri.closePath();
		g.setColor(ALERT);
		g.fill(tri);
		g.setColor(Color.WHITE);
		g.fillRect(x + w / 2 - 1, midY - 4, 3, 8);
		g.fillRect(x + w / 2 - 1, midY + 6, 3, 3);
	}

	// ---- Claw Slam ----

	/**
	 * @param t
	 *            Milliseconds into the claw attack.
	 * @return Left claw angle in degrees (-100 off screen, 0 at rest), or NaN
	 *         when the claws are off screen. The right claw mirrors it.
	 */
	static double clawAngle(final long t) {
		if (t < CLAW_SWING_START || t >= CLAW_RETREAT_END)
			return Double.NaN;
		if (t < CLAW_IMPACT) {
			double u = (t - CLAW_SWING_START)
					/ (double) (CLAW_IMPACT - CLAW_SWING_START);
			return -100 + 103 * u * u * u; // ease-in, ends 3 deg past rest
		}
		if (t < CLAW_BOUNCE)
			return 3 - 4.5 * (t - CLAW_IMPACT)
					/ (double) (CLAW_BOUNCE - CLAW_IMPACT);
		if (t < CLAW_SETTLE)
			return -1.5 + 1.5 * (t - CLAW_BOUNCE)
					/ (double) (CLAW_SETTLE - CLAW_BOUNCE);
		if (t < CLAW_RETREAT_START)
			return 0;
		double u = (t - CLAW_RETREAT_START)
				/ (double) (CLAW_RETREAT_END - CLAW_RETREAT_START);
		return -100 * u * u * u;
	}

	private void drawClaws(final Graphics2D g, final long t) {
		double angle = clawAngle(t);
		if (Double.isNaN(angle))
			return;
		Graphics2D base = (Graphics2D) g.create();
		base.translate(0, groundY() - 490); // designed for ground y 490
		// Left claw: pivot at the top-left corner.
		Graphics2D left = (Graphics2D) base.create();
		left.rotate(Math.toRadians(angle), 0, 0);
		drawClawAtRest(left);
		left.dispose();
		// Right claw: mirror of the left around the screen's middle.
		Graphics2D right = (Graphics2D) base.create();
		right.translate(this.screenWidth, 0);
		right.scale(-1, 1);
		right.rotate(Math.toRadians(angle), 0, 0);
		drawClawAtRest(right);
		right.dispose();
		base.dispose();
	}

	/** Left claw in its rest pose: arm down the left edge, blade along the bottom. */
	private static void drawClawAtRest(final Graphics2D g) {
		alpha(g, 1f);
		// Arm: 26 px segmented bar, x -22 to 4, y -40 to 456.
		int armX = -22, armW = 26, top = -40, bottom = 456;
		int y = top;
		int i = 0;
		while (y < bottom) {
			int h = Math.min(14, bottom - y);
			g.setColor(i % 2 == 0 ? VOID : GALAXY);
			g.fillRect(armX, y, armW, h);
			y += h;
			if (y < bottom) {
				g.setColor(SPACE_BLACK);
				g.fillRect(armX, y, armW, Math.min(3, bottom - y));
				y += 3;
			}
			i++;
		}
		g.setColor(SPACE_BLACK);
		g.setStroke(new BasicStroke(1f));
		g.drawRect(armX, top, armW, bottom - top);

		// Blade: 80 x 160 upright path, turned 90 deg clockwise
		// ((x, y) -> (160 - y, x)), scaled 1.9, box at (-72, 386).
		Graphics2D b = (Graphics2D) g.create();
		b.translate(-72, 386);
		b.scale(1.9, 1.9);
		b.transform(new AffineTransform(0, 1, -1, 0, 160, 0));
		Path2D body = new Path2D.Double();
		body.moveTo(34, 158);
		body.curveTo(10, 130, 0, 95, 8, 62);
		body.curveTo(16, 30, 38, 12, 64, 2);
		body.curveTo(58, 30, 60, 60, 52, 92);
		body.curveTo(46, 118, 46, 140, 48, 158);
		body.closePath();
		b.setPaint(new java.awt.GradientPaint(0, 0, GALAXY, 0, 160, VOID));
		b.fill(body);
		b.setColor(withAlpha(SPACE_BLACK, 0.6f));
		b.setStroke(new BasicStroke(1f / 1.9f));
		b.draw(body);
		// Spikes on the outer edge.
		b.setColor(VOID);
		int[][] spikes = {{12, 44, 0, 38, 9, 52}, {6, 66, -7, 62, 4, 77},
				{5, 92, -7, 94, 6, 103}, {10, 118, -3, 125, 14, 128},
				{22, 24, 13, 14, 28, 19}, {24, 146, 14, 154, 30, 152}};
		for (int[] s : spikes) {
			Path2D tri = new Path2D.Double();
			tri.moveTo(s[0], s[1]);
			tri.lineTo(s[2], s[3]);
			tri.lineTo(s[4], s[5]);
			tri.closePath();
			b.fill(tri);
		}
		// Holes.
		b.setColor(SPACE_BLACK);
		double[][] holes = {{30, 36, 5, 9, -30}, {22, 58, 5, 10, -15},
				{19, 82, 5, 10, 0}, {20, 106, 5, 10, 10}, {25, 128, 4, 9, 20},
				{44, 22, 3, 6, -40}, {38, 50, 3, 7, -20}, {34, 76, 3, 7, 0},
				{34, 102, 3, 7, 10}, {37, 126, 3, 6, 15}};
		for (double[] h : holes) {
			AffineTransform saved = b.getTransform();
			b.rotate(Math.toRadians(h[4]), h[0], h[1]);
			b.fill(new Ellipse2D.Double(h[0] - h[2], h[1] - h[3], h[2] * 2,
					h[3] * 2));
			b.setTransform(saved);
		}
		// Highlights on the inner (grabbing) edge.
		Path2D edge = new Path2D.Double();
		edge.moveTo(64, 2);
		edge.curveTo(58, 30, 60, 60, 52, 92);
		edge.curveTo(46, 118, 46, 140, 48, 158);
		b.setStroke(new BasicStroke(1.2f));
		b.setColor(withAlpha(STARLIGHT, 0.85f));
		b.draw(edge);
		Path2D faint = new Path2D.Double();
		faint.moveTo(60, 10);
		faint.curveTo(40, 22, 24, 40, 16, 64);
		b.setColor(withAlpha(STARLIGHT, 0.4f));
		b.draw(faint);
		b.dispose();
	}

	/** Glow and rings where the claws meet, after an impact. */
	private void drawImpactGlow(final Graphics2D g) {
		if (this.clawImpactAt < 0)
			return;
		long dt = now() - this.clawImpactAt;
		if (dt < 0 || dt >= 1040)
			return;
		int cx = this.screenWidth / 2, cy = groundY() - 20;
		// Glow: 120 x 60, peak 0.6, gone by 1.04 s after impact.
		float ga = 0.6f * (1f - dt / 1040f);
		alpha(g, ga);
		g.setPaint(new RadialGradientPaint(cx, cy, 60f,
				new float[] {0f, 0.3f, 0.65f, 1f},
				new Color[] {Color.WHITE, STARLIGHT, GALAXY,
						withAlpha(GALAXY, 0f)}));
		AffineTransform saved = g.getTransform();
		g.translate(cx, cy);
		g.scale(1.0, 0.5);
		g.fill(new Ellipse2D.Double(-60, -60, 120, 120));
		g.setTransform(saved);
		// Ring 1: starlight, 2 px, peak 0.5, grows to 2.2x, gone by 0.64 s.
		drawRing(g, cx, cy, dt, 640, 2.2, 0.5f, STARLIGHT, 2f);
		// Ring 2: galaxy, 1 px, peak 0.3, grows to 1.8x, gone by 0.8 s.
		drawRing(g, cx, cy, dt, 800, 1.8, 0.3f, GALAXY, 1f);
	}

	private static void drawRing(final Graphics2D g, final int cx,
			final int cy, final long dt, final long life, final double grow,
			final float peak, final Color color, final float stroke) {
		if (dt >= life)
			return;
		double u = dt / (double) life;
		double scale = 1 + (grow - 1) * u;
		double w = 120 * scale, h = 60 * scale;
		alpha(g, (float) (peak * (1 - u)));
		g.setColor(color);
		g.setStroke(new BasicStroke(stroke));
		g.draw(new Ellipse2D.Double(cx - w / 2, cy - h / 2, w, h));
	}
}
