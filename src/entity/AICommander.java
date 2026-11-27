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
import java.awt.image.BufferedImage;
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
 * Level 40 boss, AI Commander (GoG - GOG-77). The final boss: a rogue AI
 * mothership that wrecks the city.
 *
 * Before she appears, a two-line warning shows for 3 s: "WARNING: BOSS
 * INCOMING" and "DON'T DRAG THIS FIGHT".
 *
 * Weak point: only her glowing core can be hurt. A player bullet must reach
 * her within {@link #WEAK_POINT_HALF_WIDTH} of her centre line (the core,
 * and the glowing channel below it); bullets that hit the rest of the hull
 * only spark. Bullets fly through the gaps in her art.
 *
 * Phase 1: she drifts left and right, her wing turrets fire aimed 3-way
 * spreads, and she alternates two big attacks:
 * - Laser: locks the player's x, glides so her spike is over it, charges
 *   for 2.1 s (WARNING banner, orb, aim line), then a beam to the ground.
 * - Halo shockwave: her halo ring drops down the screen with one gap; be in
 *   the gap when it passes.
 *
 * Phase 2 (half health): she changes form (torn wings, shattered halo, open
 * eye, open jaw) and is hidden more than she is seen (1.5 s shown, 3.5 s
 * hidden). While shown, the eye fires an aimed stream. While hidden she
 * alternates a triple laser (no banner, no aim line, only a faint 0.6 s
 * flicker) and a faster shockwave made of halo shards. The escort barrage
 * comes when phase 2 starts, then 15 s, 10 s, 5 s and then 3 s after the
 * previous one ends: six fighters line up under her with no gaps (shielded
 * while they charge), fire an unavoidable missile barrage (the player loses
 * a life, or only the shield), then drop their shields and fly away. Shoot
 * a fighter down to open a gap to her core. No laser or shockwave starts if
 * it would still be running when the next barrage is due.
 *
 * Art follows the user's reference (red-lit mothership). Every coordinate
 * is in the game's 448 x 520 screen; every tunable number is a constant
 * here. Dev switch: {@code java -Dcommander.phase2=true} starts her in
 * phase 2.
 */
public class AICommander extends Entity implements Boss {

	// ---- Size and placement ----

	/** Art size in art pixels; drawn at {@link #PIXEL} screen px each. */
	public static final int ART_WIDTH = 72;
	/** Art height in art pixels. */
	public static final int ART_HEIGHT = 40;
	/** Screen pixels per art pixel, like every other sprite. */
	public static final int PIXEL = 2;
	/** Width on screen. */
	public static final int WIDTH = ART_WIDTH * PIXEL;
	/** Height on screen. */
	public static final int HEIGHT = ART_HEIGHT * PIXEL;
	/** Top edge where GameScreen places her (under the boss health bar). */
	public static final int SPAWN_Y = 84;

	// ---- Fight numbers ----

	/** Hits needed to destroy her. */
	public static final int MAX_HP = 30;
	/** Phase 2 starts at or below this health (half of MAX_HP). */
	public static final int PHASE_2_HP = MAX_HP / 2;
	/** Shown before she appears, first line. */
	public static final String WARNING_TEXT = "WARNING: BOSS INCOMING";
	/** Second line, under the first: phase 2 punishes a long fight. */
	public static final String WARNING_TEXT_2 = "DON'T DRAG THIS FIGHT";
	/** How long the intro warning shows (both lines together). */
	public static final int INTRO_MILLIS = 3000;
	/** Points for each hit on the core. */
	public static final int POINTS_PER_HIT = 300;
	/** Extra points for destroying her. */
	public static final int POINTS_DESTROYED = 5000;
	/**
	 * Weak point: a bullet hurts her only if its centre is this close to her
	 * centre line: the whole glow around the core and the glowing column
	 * below it (40 px wide). What glows can be hit.
	 */
	public static final int WEAK_POINT_HALF_WIDTH = 20;
	/** System property that starts her in phase 2, for testing. */
	public static final String PHASE_2_SWITCH = "commander.phase2";

	// ---- Laser timeline, ms into one phase 1 cycle ----
	// (phase 2 is the same timeline with the charge cut to LASER_TELL_PHASE_2)

	/** Charging ends and the beam starts growing. */
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
	/** Ground line, measured up from the bottom of the screen. */
	public static final int GROUND_FROM_BOTTOM = 6;
	/** Rest between big attacks in phase 1. */
	public static final int IDLE_MILLIS = 900;
	/**
	 * Phase 2 laser: fired while hidden, with no banner and no aim line,
	 * only a faint flicker for this long before the beams. 300 is the
	 * harder option; 0 fires with no tell at all.
	 */
	public static final int LASER_TELL_PHASE_2 = 600;
	/** Phase 2 side beams sit this far left and right of the locked beam. */
	public static final int SIDE_BEAM_OFFSET = 76;

	// ---- Wing turrets (phase 1) ----

	/** Time between turret volleys (the wings take turns). */
	public static final int TURRET_MILLIS = 1100;
	/** First volley comes this long after she appears. */
	public static final int TURRET_FIRST_MILLIS = 800;
	/** Turret bullets fall at this speed, px per frame. */
	public static final int TURRET_SPEED_Y = 4;
	/** Fastest sideways speed of an aimed turret bullet. */
	public static final int TURRET_MAX_SPEED_X = 4;

	// ---- Eye stream (phase 2) ----

	/** Bullets in one stream, fired once per shown stretch. */
	public static final int STREAM_BULLETS = 4;
	/** Time between stream bullets (each one re-aims). */
	public static final int STREAM_GAP_MILLIS = 110;
	/** The stream starts this long into a shown stretch. */
	public static final int STREAM_DELAY_MILLIS = 250;
	/** Stream bullets fall at this speed. */
	public static final int STREAM_SPEED_Y = 6;
	/** Fastest sideways speed of a stream bullet. */
	public static final int STREAM_MAX_SPEED_X = 5;

	// ---- Halo shockwave ----

	/** Phase 1: the halo glows this long before the wave drops. */
	public static final int HALO_TELL = 600;
	/** Phase 1: time for the wave to reach the ground. */
	public static final int HALO_FALL = 1400;
	/** Phase 2 (halo shards): shorter glow. */
	public static final int HALO_TELL_PHASE_2 = 300;
	/** Phase 2: faster fall. */
	public static final int HALO_FALL_PHASE_2 = 1000;
	/** Fade after the wave has passed (harmless). */
	public static final int HALO_FADE = 300;
	/** Phase 1 gap width; the player ship is 26 px wide. */
	public static final int HALO_GAP = 56;
	/** Phase 2 gap width. */
	public static final int HALO_GAP_PHASE_2 = 48;
	/** The gap opens at least this far from the player's x, so you must move. */
	public static final int HALO_GAP_MIN_DISTANCE = 90;
	/** Thickness of the wave's damage band. */
	public static final int HALO_THICKNESS = 8;

	// ---- Phase 2 ----

	/** Phase 2 visibility loop: she is shown for this long... */
	public static final int SHOWN_PHASE_2_MILLIS = 1500;
	/** ...then hidden for this long, then shown again. */
	public static final int HIDDEN_PHASE_2_MILLIS = 3500;
	/** A hidden attack starts at least this long after she vanishes. */
	public static final int HIDE_BEFORE_ATTACK_MILLIS = 500;
	/** How long a hit makes her flash while hidden. */
	public static final int HIT_FLASH_MILLIS = 120;
	/** She flickers between both forms this long when phase 2 starts. */
	public static final int PHASE_FLASH_MILLIS = 1000;
	/**
	 * Wait after each barrage ends before the next one starts (the first
	 * one comes when phase 2 starts). The last value repeats.
	 */
	private static final int[] BARRAGE_GAPS_MILLIS = {15000, 10000, 5000, 3000};

	// ---- Escort barrage timeline, ms into the attack ----

	/** Fighters in the escort wall; together exactly as wide as she is. */
	public static final int ESCORTS = 6;
	/** Width of one escort fighter on screen. */
	public static final int ESCORT_WIDTH = 24;
	/** Escort row, below her top edge: just under her whole hull. */
	public static final int ESCORT_OFFSET_Y = HEIGHT + 4;
	/** Hits an escort takes once its shield is down. */
	public static final int ESCORT_HP = 2;
	/** Health while shielded: nothing gets through in time. */
	private static final int ESCORT_SHIELD_HP = 1000;
	/** Missiles each escort fires. */
	public static final int MISSILES_PER_ESCORT = 2;
	/** The wall is lined up under her. */
	public static final int BARRAGE_LINED_UP = 1000;
	/** Missiles launch; shields drop. Until here the fighters can't be hurt. */
	public static final int BARRAGE_FIRE = 2200;
	/** Missiles hit the ground: unavoidable hit. */
	public static final int BARRAGE_IMPACT = 3000;
	/** Surviving fighters start flying away. */
	public static final int BARRAGE_LEAVE = 3800;
	/** The attack is over. */
	public static final int BARRAGE_END = 4600;

	// ---- Palette (red-lit mothership) ----

	/** Core, lights, laser and banner. */
	public static final Color CORE_RED = new Color(0xff2838);
	/** Darker red details. */
	public static final Color DARK_RED = new Color(0x960e1e);
	/** Hot white centre of the core and beam. */
	public static final Color HOT_WHITE = new Color(0xffd6d6);
	/** Missile fire. */
	public static final Color FIRE = new Color(0xff9a3c);
	/** Shields (escorts) and "SHIELD BROKEN". */
	public static final Color ION = new Color(0x5ee6ef);
	/** Banner fill. */
	public static final Color SPACE_BLACK = new Color(0x05060a);
	/** Laser and WARNING banner colour. */
	public static final Color ALERT = CORE_RED;

	// ---- Geometry inside her art (screen px from her top-left) ----

	/** Centre line: core, spike and laser. */
	private static final int CENTER_X = WIDTH / 2;
	/** Core centre. */
	private static final int CORE_Y = 37;
	/** Spike tip / jaw: where the main beam starts. */
	private static final int JAW_Y = 76;
	/** Wing tips: where the phase 2 side beams start. */
	private static final int WING_BEAM_Y = 40;
	/** Halo centre. */
	private static final int HALO_Y = 10;
	/** Wing turret muzzles, left one (the right mirrors it). */
	private static final int TURRET_X = 16;
	/** Wing turret muzzle height. */
	private static final int TURRET_Y = 46;

	// ---- Movement ----

	/** Sideways drift, px per frame. */
	private static final int DRIFT_SPEED = 1;
	/** Glide while lining up a laser (or getting back on screen). */
	private static final int GLIDE_SPEED = 6;
	/** Gap kept from the screen edges while drifting. */
	private static final int EDGE_MARGIN = 4;
	/**
	 * A gap this long between updates means the game froze her (item); the
	 * gap doesn't count, so no attack runs out or fires unseen.
	 */
	private static final int FROZEN_GAP_MILLIS = 100;
	/** Length of her death explosion. */
	private static final int DEATH_MILLIS = 1600;

	/** What she is doing. Visibility is separate (see {@link #isHidden(long)}). */
	enum Action {
		/** Resting / drifting. */
		IDLE,
		/** Laser: charge (or a quiet tell in phase 2), fire, fade. */
		LASER,
		/** Halo shockwave (halo shards in phase 2). */
		HALO,
		/** Phase 2: escort wall and unavoidable missile barrage. */
		BARRAGE
	}

	/** One escort fighter: its ship, its slot in the wall, where it came from. */
	private static final class Escort {
		/** The ship GameScreen hits; replaced when the shield drops. */
		private EnemyShip ship;
		/** Slot in the wall, 0 = left. */
		private final int slot;
		/** Off-screen x it flies in from and back out to. */
		private final int sideX;

		Escort(final EnemyShip ship, final int slot, final int sideX) {
			this.ship = ship;
			this.slot = slot;
			this.sideX = sideX;
		}
	}

	private int hp = MAX_HP;
	private boolean started;
	private boolean destroyed;
	private boolean phase2;
	/** Read once: start in phase 2 (testing switch). */
	private final boolean startInPhase2 = Boolean.getBoolean(PHASE_2_SWITCH);
	private boolean startSwitchDone;

	/** Time spent paused or frozen, taken off her clock. */
	private long pausedTotal;
	/** Last real time update() ran, to spot freezes. */
	private long lastRealUpdate;

	private long introEndsAt;
	private Action action = Action.IDLE;
	private long actionStart;
	private long actionLength;
	/** Big attacks done in phase 1 (laser and halo take turns). */
	private int phase1Attacks;

	// Laser.
	private int lockX = 224;
	private int chargeMillis = LASER_CHARGE_END;
	private boolean quietLaser;
	private boolean tripleLaser;
	private boolean laserHitDone;

	// Halo shockwave.
	private boolean haloQuick;
	private int haloGapX;
	private int haloCenterX;
	private int haloCenterY;
	private boolean haloHitDone;

	// Phase 2.
	private long phase2StartedAt = -1;
	private long hitFlashUntil;
	/** Hidden stretch that already had its attack. */
	private long attackStretch = -1;
	/** Shown stretch that already had its stream. */
	private long streamStretch = -1;
	private int streamShotsLeft;
	private long nextStreamShotAt;

	// Turrets.
	private long nextTurretAt = Long.MAX_VALUE;
	private boolean leftTurretNext = true;

	// Escort barrage.
	private final List<Escort> escorts = new ArrayList<Escort>();
	private int barragesStarted;
	private long nextBarrageAt = Long.MAX_VALUE;
	private boolean barrageHitDone;
	private boolean escortShieldsUp;
	private final int[] missileFromX = new int[ESCORTS * MISSILES_PER_ESCORT];
	private int missileFromY;

	// Effects.
	private long impactAt = -1;
	private long shieldBrokenAt = -1;
	private int shieldX;
	private int shieldY;
	private long coreFlashUntil;
	private long shatterAt = -1;
	private int shatterX;
	private int shatterY;
	private long destroyedAt = -1;
	/** Hull sparks: {x, y, time}. */
	private final List<long[]> sparks = new ArrayList<long[]>();

	/** Player centre, updated every frame by GameScreen. */
	private int targetX = 224;
	private int targetY = 474;
	/** Hits waiting for GameScreen. */
	private int pendingHits;
	private int sideways = DRIFT_SPEED;
	private int screenWidth = 448;
	private int screenHeight = 496;
	private final Random random = new Random();

	/** Art drawn once into images (form 1, form 2, escort). */
	private static BufferedImage form1Image;
	private static BufferedImage form2Image;
	private static BufferedImage escortImage;

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
		super(positionX, positionY, WIDTH, HEIGHT, CORE_RED);
		this.spriteType = SpriteType.Explosion; // only seen once destroyed
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
		this.nextTurretAt = this.introEndsAt + TURRET_FIRST_MILLIS;
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

	// =====================================================================
	// Timeline
	// =====================================================================

	private void startIdle(final long at, final long length) {
		this.action = Action.IDLE;
		this.actionStart = at;
		this.actionLength = length;
	}

	private void startLaser(final long at) {
		this.action = Action.LASER;
		this.actionStart = at;
		this.laserHitDone = false;
		this.quietLaser = this.phase2;
		this.tripleLaser = this.phase2;
		this.chargeMillis = this.phase2 ? LASER_TELL_PHASE_2 : LASER_CHARGE_END;
		this.lockX = clampBeamX(this.targetX);
	}

	private void startHalo(final long at) {
		this.action = Action.HALO;
		this.actionStart = at;
		this.haloHitDone = false;
		this.haloQuick = this.phase2;
		this.haloCenterX = this.positionX + CENTER_X;
		this.haloCenterY = this.positionY + HALO_Y;
		this.haloGapX = pickGapX();
	}

	private void startBarrage(final long at) {
		this.action = Action.BARRAGE;
		this.actionStart = at;
		this.barrageHitDone = false;
		this.barragesStarted++;
		this.nextBarrageAt = Long.MAX_VALUE; // set when this one ends
		this.escortShieldsUp = true;
		// Leftovers that still fly are dropped; dying ones finish fading.
		Iterator<Escort> it = this.escorts.iterator();
		while (it.hasNext())
			if (!it.next().ship.isDestroyed())
				it.remove();
		int y = this.positionY + ESCORT_OFFSET_Y;
		for (int slot = 0; slot < ESCORTS; slot++) {
			boolean left = slot < ESCORTS / 2;
			int sideX = left ? -ESCORT_WIDTH - (ESCORTS / 2 - 1 - slot) * 28
					: this.screenWidth + (slot - ESCORTS / 2) * 28;
			this.escorts.add(new Escort(new EnemyShip(sideX, y,
					EnemyType.STAR_EATER, ESCORT_SHIELD_HP), slot, sideX));
		}
	}

	private void enterPhase2(final long now) {
		this.phase2 = true;
		this.phase2StartedAt = now;
		this.attackStretch = -1;
		this.streamStretch = -1;
		this.streamShotsLeft = 0;
		this.shatterAt = now;
		this.shatterX = this.positionX + CENTER_X;
		this.shatterY = this.positionY + HALO_Y;
		startBarrage(now); // the first barrage comes right away
	}

	/**
	 * @param barragesDone
	 *            Barrages that have happened (1 = the first).
	 * @return Wait after that barrage ends before the next one: 15 s, 10 s,
	 *         5 s, then 3 s every time.
	 */
	public static int barrageGapAfter(final int barragesDone) {
		int i = Math.max(1, Math.min(barragesDone, BARRAGE_GAPS_MILLIS.length));
		return BARRAGE_GAPS_MILLIS[i - 1];
	}

	/** @return Length of a whole phase 2 laser, tell included. */
	public static int laserLengthPhase2() {
		return LASER_TELL_PHASE_2 + (LASER_CYCLE_END - LASER_CHARGE_END);
	}

	/** @return Length of a whole phase 2 halo shockwave. */
	public static int haloLengthPhase2() {
		return HALO_TELL_PHASE_2 + HALO_FALL_PHASE_2 + HALO_FADE;
	}

	private int haloTell() {
		return this.haloQuick ? HALO_TELL_PHASE_2 : HALO_TELL;
	}

	private int haloFall() {
		return this.haloQuick ? HALO_FALL_PHASE_2 : HALO_FALL;
	}

	private int haloLength() {
		return haloTell() + haloFall() + HALO_FADE;
	}

	/** @return Gap width of the current shockwave. */
	public final int haloGapWidth() {
		return this.haloQuick ? HALO_GAP_PHASE_2 : HALO_GAP;
	}

	// Laser timeline of the current laser.

	private int growEnd() {
		return this.chargeMillis + (LASER_GROW_END - LASER_CHARGE_END);
	}

	private int fireEnd() {
		return this.chargeMillis + (LASER_FIRE_END - LASER_CHARGE_END);
	}

	private int fadeEnd() {
		return this.chargeMillis + (LASER_FADE_END - LASER_CHARGE_END);
	}

	private int cycleEnd() {
		return this.chargeMillis + (LASER_CYCLE_END - LASER_CHARGE_END);
	}

	/** @return Length of one phase 2 shown + hidden loop. */
	private static int phase2Loop() {
		return SHOWN_PHASE_2_MILLIS + HIDDEN_PHASE_2_MILLIS;
	}

	/**
	 * @param now
	 *            Her clock.
	 * @return True while her phase 2 loop hides her (a hit flash can still
	 *         show her, see {@link #isVisible()}).
	 */
	final boolean isHidden(final long now) {
		if (!this.phase2 || this.destroyed)
			return false;
		long since = now - this.phase2StartedAt;
		if (since < PHASE_FLASH_MILLIS)
			return false; // the change of form is always seen
		return since % phase2Loop() >= SHOWN_PHASE_2_MILLIS;
	}

	/**
	 * A hidden attack starts once per hidden stretch, a little after she
	 * vanishes, only if it ends before she shows again and before the next
	 * barrage. Even stretches: triple laser; odd: halo shards.
	 */
	private boolean canStartHiddenAttack(final long now) {
		long since = now - this.phase2StartedAt;
		long stretch = since / phase2Loop();
		long inLoop = since % phase2Loop();
		int length = stretch % 2 == 0 ? laserLengthPhase2() : haloLengthPhase2();
		return stretch != this.attackStretch
				&& inLoop >= SHOWN_PHASE_2_MILLIS + HIDE_BEFORE_ATTACK_MILLIS
				&& inLoop + length <= phase2Loop()
				&& now + length <= this.nextBarrageAt;
	}

	/** Runs her timeline: attacks, escorts, effects. */
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

		if (this.startInPhase2 && !this.startSwitchDone) {
			this.startSwitchDone = true;
			if (!this.phase2) {
				this.hp = PHASE_2_HP;
				enterPhase2(now);
			}
		}

		long t = actionTime();
		switch (this.action) {
		case IDLE:
			if (this.phase2) {
				if (now >= this.nextBarrageAt) {
					startBarrage(now);
				} else if (canStartHiddenAttack(now)) {
					this.attackStretch = (now - this.phase2StartedAt) / phase2Loop();
					if (this.attackStretch % 2 == 0)
						startLaser(now);
					else
						startHalo(now);
				}
			} else if (t >= this.actionLength) {
				if (this.phase1Attacks++ % 2 == 0)
					startLaser(now);
				else
					startHalo(now);
			}
			break;
		case LASER:
			if (t >= cycleEnd())
				startIdle(now, this.phase2 ? 0 : IDLE_MILLIS);
			break;
		case HALO:
			if (t >= haloLength())
				startIdle(now, this.phase2 ? 0 : IDLE_MILLIS);
			break;
		case BARRAGE:
			if (this.escortShieldsUp && t >= BARRAGE_FIRE)
				launchMissiles();
			if (t >= BARRAGE_IMPACT && !this.barrageHitDone) {
				this.barrageHitDone = true;
				this.pendingHits++;
				this.impactAt = this.actionStart + BARRAGE_IMPACT;
			}
			if (t >= BARRAGE_END) {
				this.nextBarrageAt = now + barrageGapAfter(this.barragesStarted);
				startIdle(now, 0);
			}
			break;
		default:
			break;
		}
		updateEscorts(now);
	}

	/** Shields drop and every fighter fires its missiles. */
	private void launchMissiles() {
		this.escortShieldsUp = false;
		for (Escort e : this.escorts) {
			if (e.ship.isDestroyed())
				continue;
			e.ship = new EnemyShip(e.ship.getPositionX(), e.ship.getPositionY(),
					EnemyType.STAR_EATER, ESCORT_HP);
			for (int m = 0; m < MISSILES_PER_ESCORT; m++)
				this.missileFromX[e.slot * MISSILES_PER_ESCORT + m] =
						e.ship.getPositionX() + 6 + m * 12;
		}
		this.missileFromY = this.positionY + ESCORT_OFFSET_Y + 14;
	}

	/** Moves the escort wall: fly in, hold under her, fly back out. */
	private void updateEscorts(final long now) {
		long t = this.action == Action.BARRAGE ? now - this.actionStart
				: BARRAGE_END;
		Iterator<Escort> it = this.escorts.iterator();
		while (it.hasNext()) {
			Escort e = it.next();
			if (e.ship.isDestroyed()) {
				if (e.ship.isFadeFinished())
					it.remove();
				continue;
			}
			if (t >= BARRAGE_END) {
				it.remove(); // flew off
				continue;
			}
			int slotX = this.positionX + e.slot * ESCORT_WIDTH;
			int x;
			if (t < BARRAGE_LINED_UP)
				x = lerp(e.sideX, slotX, easeOut(t / (double) BARRAGE_LINED_UP));
			else if (t < BARRAGE_LEAVE)
				x = slotX;
			else
				x = lerp(slotX, e.sideX, easeIn((t - BARRAGE_LEAVE)
						/ (double) (BARRAGE_END - BARRAGE_LEAVE)));
			e.ship.setPositionX(x);
			e.ship.setPositionY(this.positionY + ESCORT_OFFSET_Y);
			e.ship.update();
		}
	}

	/** @return True while the escort fighters are shielded. */
	public final boolean areEscortsShielded() {
		return this.action == Action.BARRAGE && this.escortShieldsUp;
	}

	/**
	 * Drifts left and right; glides over the locked column during a laser
	 * (she may leave the screen partly, so the edges aren't safe); holds
	 * still during a barrage so the wall lines up under her.
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
		if (this.action == Action.LASER) {
			int goal = this.lockX - CENTER_X;
			this.positionX += Math.max(-GLIDE_SPEED, Math.min(GLIDE_SPEED,
					goal - this.positionX));
			return;
		}
		// Back on screen first (after a laser near the edge).
		if (this.positionX < minX) {
			this.positionX = Math.min(minX, this.positionX + GLIDE_SPEED);
			this.sideways = DRIFT_SPEED;
			return;
		}
		if (this.positionX > maxX) {
			this.positionX = Math.max(maxX, this.positionX - GLIDE_SPEED);
			this.sideways = -DRIFT_SPEED;
			return;
		}
		if (this.action == Action.BARRAGE)
			return;
		this.positionX += this.sideways;
		if (this.positionX <= minX) {
			this.positionX = minX;
			this.sideways = DRIFT_SPEED;
		} else if (this.positionX >= maxX) {
			this.positionX = maxX;
			this.sideways = -DRIFT_SPEED;
		}
	}

	/**
	 * Phase 1: wing turrets take turns firing aimed 3-way spreads. Phase 2:
	 * the eye fires an aimed stream once per shown stretch. Nothing while
	 * hidden, frozen, before she appears or once destroyed.
	 */
	@Override
	public final boolean shoot(final Set<Bullet> bullets,
			final GameSettings settings) {
		if (!this.started || this.destroyed || isIntro())
			return false;
		if (System.currentTimeMillis() - this.lastRealUpdate > FROZEN_GAP_MILLIS)
			return false; // frozen by an item
		long now = now();
		boolean fired = false;
		if (!this.phase2) {
			if (now >= this.nextTurretAt) {
				int x = this.positionX + (this.leftTurretNext ? TURRET_X
						: this.width - TURRET_X);
				int y = this.positionY + TURRET_Y;
				int vx = aimSpeedX(x, y, TURRET_SPEED_Y, TURRET_MAX_SPEED_X);
				for (int spread = -1; spread <= 1; spread++)
					bullets.add(bullet(x, y, TURRET_SPEED_Y, vx + spread));
				this.leftTurretNext = !this.leftTurretNext;
				this.nextTurretAt = now + TURRET_MILLIS;
				fired = true;
			}
			return fired;
		}
		if (!isHidden(now)) {
			long since = now - this.phase2StartedAt;
			long stretch = since / phase2Loop();
			long inLoop = since % phase2Loop();
			if (stretch != this.streamStretch && inLoop >= STREAM_DELAY_MILLIS
					&& inLoop < SHOWN_PHASE_2_MILLIS) {
				this.streamStretch = stretch;
				this.streamShotsLeft = STREAM_BULLETS;
				this.nextStreamShotAt = now;
			}
		} else {
			this.streamShotsLeft = 0;
		}
		if (this.streamShotsLeft > 0 && now >= this.nextStreamShotAt) {
			int x = this.positionX + CENTER_X;
			int y = this.positionY + CORE_Y + 8;
			bullets.add(bullet(x, y, STREAM_SPEED_Y,
					aimSpeedX(x, y, STREAM_SPEED_Y, STREAM_MAX_SPEED_X)));
			this.streamShotsLeft--;
			this.nextStreamShotAt = now + STREAM_GAP_MILLIS;
			fired = true;
		}
		return fired;
	}

	/** @return A red enemy bullet. */
	private static Bullet bullet(final int x, final int y, final int speedY,
			final int speedX) {
		Bullet b = BulletPool.getBullet(x, y, speedY);
		b.setSpeedX(speedX);
		b.setColor(CORE_RED);
		return b;
	}

	/** @return Sideways speed that sends a bullet from (x, y) at the player. */
	private int aimSpeedX(final int x, final int y, final int speedY,
			final int max) {
		int dy = Math.max(40, this.targetY - y);
		long vx = Math.round((this.targetX - x) * (double) speedY / dy);
		return (int) Math.max(-max, Math.min(max, vx));
	}

	@Override
	public final void setTarget(final int x, final int y) {
		this.targetX = x;
		this.targetY = y;
	}

	/**
	 * Laser and shockwave hits are checked here against the ship (once per
	 * attack); barrage hits were queued at impact.
	 */
	@Override
	public final int takePlayerHits(final Entity ship) {
		if (this.started && !this.destroyed && ship != null && !isIntro()) {
			long t = actionTime();
			Rectangle body = new Rectangle(ship.getPositionX(),
					ship.getPositionY(), ship.getWidth(), ship.getHeight());
			if (this.action == Action.LASER && !this.laserHitDone) {
				for (Rectangle beam : beamRects(t)) {
					if (beam.intersects(body)) {
						this.laserHitDone = true;
						this.pendingHits++;
						break;
					}
				}
			}
			if (this.action == Action.HALO && !this.haloHitDone
					&& waveHits(body, t)) {
				this.haloHitDone = true;
				this.pendingHits++;
			}
		}
		int hits = this.pendingHits;
		this.pendingHits = 0;
		return hits;
	}

	/** @return Beam x positions of the current laser (1, or up to 3 in phase 2). */
	final List<Integer> beamXs() {
		List<Integer> xs = new ArrayList<Integer>();
		xs.add(this.lockX);
		if (this.tripleLaser) {
			for (int side : new int[] {-SIDE_BEAM_OFFSET, SIDE_BEAM_OFFSET}) {
				int x = this.lockX + side;
				if (x >= BEAM_WIDTH / 2 && x <= this.screenWidth - BEAM_WIDTH / 2)
					xs.add(x);
			}
		}
		return xs;
	}

	/** @return Where the beam at x starts: the spike, or a wing tip. */
	private int beamTop(final int x) {
		return this.positionY + (x == this.lockX ? JAW_Y : WING_BEAM_Y);
	}

	/**
	 * @param t
	 *            Milliseconds into the laser.
	 * @return The main beam's damage area at that moment, or null when it
	 *         can't hurt (charging, fading, over).
	 */
	final Rectangle beamBounds(final long t) {
		List<Rectangle> beams = beamRects(t);
		return beams.isEmpty() ? null : beams.get(0);
	}

	/** @return Every beam's damage area at time t (empty when harmless). */
	final List<Rectangle> beamRects(final long t) {
		if (this.action != Action.LASER || t < this.chargeMillis || t >= fireEnd())
			return Collections.emptyList();
		double grow = Math.min(1.0, (t - this.chargeMillis)
				/ (double) (growEnd() - this.chargeMillis));
		List<Rectangle> rects = new ArrayList<Rectangle>();
		for (int x : beamXs()) {
			int top = beamTop(x);
			int bottom = top + (int) Math.round((groundY() - top) * grow);
			rects.add(new Rectangle(x - BEAM_WIDTH / 2, top, BEAM_WIDTH,
					Math.max(1, bottom - top)));
		}
		return rects;
	}

	/** @return Ground line: where beams end and missiles land. */
	public final int groundY() {
		return this.screenHeight - GROUND_FROM_BOTTOM;
	}

	/** @return The column the current laser locked onto. */
	public final int getLockX() {
		return this.lockX;
	}

	/** @return Centre x of the current shockwave's gap. */
	public final int getHaloGapX() {
		return this.haloGapX;
	}

	private int clampBeamX(final int x) {
		return Math.max(BEAM_WIDTH / 2, Math.min(this.screenWidth - BEAM_WIDTH / 2, x));
	}

	/** @return A gap centre at least HALO_GAP_MIN_DISTANCE from the player. */
	private int pickGapX() {
		int half = (this.phase2 ? HALO_GAP_PHASE_2 : HALO_GAP) / 2;
		int min = half + 4;
		int max = this.screenWidth - half - 4;
		for (int i = 0; i < 30; i++) {
			int x = min + this.random.nextInt(Math.max(1, max - min + 1));
			if (Math.abs(x - this.targetX) >= HALO_GAP_MIN_DISTANCE)
				return x;
		}
		return this.targetX < this.screenWidth / 2 ? max : min; // far side
	}

	/**
	 * @param x
	 *            Screen x.
	 * @param t
	 *            Milliseconds into the shockwave.
	 * @return Height of the wave front at x, or NaN where there is none.
	 */
	final double waveY(final int x, final long t) {
		double u = (t - haloTell()) / (double) haloFall();
		if (u <= 0)
			return Double.NaN;
		u = Math.min(1.0, u);
		double rx = 40 + 760 * u;
		double ry = (groundY() + 12 - this.haloCenterY) * u;
		double dx = (x - this.haloCenterX) / rx;
		if (Math.abs(dx) >= 1)
			return Double.NaN;
		return this.haloCenterY + ry * Math.sqrt(1 - dx * dx);
	}

	/** @return True if a ship x range sits fully inside the gap. */
	private boolean inGap(final int left, final int width) {
		int half = haloGapWidth() / 2;
		return left >= this.haloGapX - half && left + width <= this.haloGapX + half;
	}

	private boolean waveHits(final Rectangle ship, final long t) {
		if (t >= haloTell() + haloFall())
			return false; // fading: harmless
		double y = waveY((int) ship.getCenterX(), t);
		if (Double.isNaN(y))
			return false;
		return Math.abs(y - ship.getCenterY()) <= HALO_THICKNESS / 2.0
				+ ship.height / 2.0 && !inGap(ship.x, ship.width);
	}

	@Override
	public final void onPlayerHitBlocked() {
		this.shieldBrokenAt = now();
		this.shieldX = this.targetX;
		this.shieldY = this.targetY;
	}

	/**
	 * Bullets fly through the gaps in her art: only a bullet touching one of
	 * her pixels hits her (hull or weak point).
	 */
	@Override
	public final boolean isHitBy(final Bullet bullet) {
		if (!this.started || this.destroyed || isIntro() || bullet == null)
			return false;
		String[] art = this.phase2 ? FORM_2 : FORM_1;
		int x0 = Math.max(0, Math.floorDiv(bullet.getPositionX() - this.positionX, PIXEL));
		int x1 = Math.min(ART_WIDTH - 1, Math.floorDiv(bullet.getPositionX()
				+ bullet.getWidth() - 1 - this.positionX, PIXEL));
		int y0 = Math.max(0, Math.floorDiv(bullet.getPositionY() - this.positionY, PIXEL));
		int y1 = Math.min(ART_HEIGHT - 1, Math.floorDiv(bullet.getPositionY()
				+ bullet.getHeight() - 1 - this.positionY, PIXEL));
		for (int y = y0; y <= y1; y++)
			for (int x = x0; x <= x1; x++)
				if (art[y].charAt(x) != '.')
					return true;
		return false;
	}

	/** @return True if a bullet is on her weak point (null counts: tests). */
	public final boolean isOnWeakPoint(final Bullet bullet) {
		if (bullet == null)
			return true;
		int center = bullet.getPositionX() + bullet.getWidth() / 2;
		return Math.abs(center - (this.positionX + CENTER_X)) <= WEAK_POINT_HALF_WIDTH;
	}

	/**
	 * A player bullet reached her. On the weak point she loses 1 HP; on the
	 * hull it only sparks.
	 *
	 * @return Points earned.
	 */
	@Override
	public final int manageCollisionsBoss(final Bullet bullet) {
		if (!this.started || this.destroyed || isIntro())
			return 0;
		long now = now();
		if (!isOnWeakPoint(bullet)) {
			this.sparks.add(new long[] {bullet.getPositionX() + bullet.getWidth() / 2,
					bullet.getPositionY(), now});
			return 0;
		}
		this.hp--;
		this.coreFlashUntil = now + 80;
		if (isHidden(now))
			this.hitFlashUntil = now + HIT_FLASH_MILLIS;
		int points = POINTS_PER_HIT;
		if (this.hp <= 0) {
			destroy();
			points += POINTS_DESTROYED;
		} else if (!this.phase2 && this.hp <= PHASE_2_HP) {
			enterPhase2(now);
		}
		return points;
	}

	@Override
	public final void destroy() {
		if (this.destroyed)
			return;
		this.destroyed = true;
		this.destroyedAt = now();
		this.spriteType = SpriteType.Explosion;
		this.action = Action.IDLE;
		for (Escort e : this.escorts)
			e.ship.destroy(); // her escorts die with her (no points)
	}

	/** Hidden during the intro and while phase 2 hides her (except a hit flash). */
	@Override
	public final boolean isVisible() {
		if (isIntro())
			return false;
		if (this.destroyed)
			return true;
		long now = now();
		return !isHidden(now) || now < this.hitFlashUntil;
	}

	/** Both warning lines, split by a line break; GameScreen draws one per line. */
	@Override
	public final String getWarningText() {
		return this.started && !this.destroyed && isIntro()
				? WARNING_TEXT + "\n" + WARNING_TEXT_2 : null;
	}

	@Override
	public final void resumeAfterPause(final long pausedMilliseconds) {
		this.pausedTotal += pausedMilliseconds;
		this.lastRealUpdate += pausedMilliseconds;
	}

	/** @return The escort fighters GameScreen can hit. */
	@Override
	public final List<EnemyShip> getMinions() {
		List<EnemyShip> ships = new ArrayList<EnemyShip>(this.escorts.size());
		for (Escort e : this.escorts)
			ships.add(e.ship);
		return ships;
	}

	/** Screen shake after a barrage impact. */
	@Override
	public final Point getShakeOffset() {
		if (this.impactAt < 0)
			return null;
		long dt = now() - this.impactAt;
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

	private static Color paletteColor(final char c) {
		switch (c) {
		case 'k':
			return new Color(18, 10, 16);
		case 'd':
			return new Color(50, 40, 52);
		case 'g':
			return new Color(84, 76, 90);
		case 'l':
			return new Color(140, 132, 148);
		case 'r':
			return CORE_RED;
		case 'o':
			return DARK_RED;
		case 'w':
			return HOT_WHITE;
		default:
			return null;
		}
	}

	private static BufferedImage toImage(final String[] art) {
		BufferedImage img = new BufferedImage(art[0].length(), art.length,
				BufferedImage.TYPE_INT_ARGB);
		for (int y = 0; y < art.length; y++)
			for (int x = 0; x < art[y].length(); x++) {
				Color c = paletteColor(art[y].charAt(x));
				if (c != null)
					img.setRGB(x, y, c.getRGB());
			}
		return img;
	}

	private static synchronized BufferedImage art(final int form) {
		if (form1Image == null) {
			form1Image = toImage(FORM_1);
			form2Image = toImage(FORM_2);
			escortImage = toImage(ESCORT_ART);
		}
		return form == 1 ? form1Image : form == 2 ? form2Image : escortImage;
	}

	private Graphics2D graphics(final DrawManager drawManager) {
		Graphics2D g = (Graphics2D) drawManager.getWorldGraphics().create();
		g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
				RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
		return g;
	}

	/** Her own multi-colour art (GameScreen calls this only while visible). */
	@Override
	public final void drawBody(final DrawManager drawManager) {
		Graphics2D g = graphics(drawManager);
		try {
			long now = now();
			if (this.destroyed) {
				drawDeath(g, now);
				return;
			}
			int form = this.phase2 ? 2 : 1;
			if (this.phase2 && now - this.phase2StartedAt < PHASE_FLASH_MILLIS)
				form = ((now - this.phase2StartedAt) / 100) % 2 == 0 ? 1 : 2;
			g.drawImage(art(form), this.positionX, this.positionY, WIDTH,
					HEIGHT, null);
			drawWeakPoint(g, now, form);
		} finally {
			g.dispose();
		}
	}

	/** Pulsing glow on the core and the channel below it: shoot here. */
	private void drawWeakPoint(final Graphics2D g, final long now,
			final int form) {
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		float p = (float) (0.5 + 0.5 * Math.sin(now / 130.0));
		int cx = this.positionX + CENTER_X;
		int cy = this.positionY + CORE_Y;
		alpha(g, 0.16f + 0.18f * p);
		g.setColor(CORE_RED);
		g.fill(new Ellipse2D.Double(cx - 20, cy - 20, 40, 40));
		alpha(g, 0.45f + 0.45f * p);
		g.setStroke(new BasicStroke(2f));
		double r = 15 + 2 * p;
		g.draw(new Ellipse2D.Double(cx - r, cy - r, r * 2, r * 2));
		// Column below the core, as wide as the hit zone: soft glow, then
		// the spike's red line (phase 1) or the open jaw (phase 2).
		int half = WEAK_POINT_HALF_WIDTH;
		alpha(g, 0.10f + 0.12f * p);
		g.fillRect(cx - half, cy + 18, half * 2, JAW_Y - CORE_Y - 18);
		alpha(g, 0.35f + 0.4f * p);
		if (form == 1)
			g.fillRect(cx - 2, cy + 12, 4, JAW_Y - CORE_Y - 12);
		else
			g.fillRect(cx - 6, cy + 16, 12, JAW_Y - CORE_Y - 20);
		if (now < this.coreFlashUntil) {
			alpha(g, 0.9f);
			g.setColor(Color.WHITE);
			g.fill(new Ellipse2D.Double(cx - 10, cy - 10, 20, 20));
		}
		alpha(g, 1f);
	}

	/** Death: she breaks up in a chain of explosions, then a big flash. */
	private void drawDeath(final Graphics2D g, final long now) {
		long dt = now - this.destroyedAt;
		if (dt >= DEATH_MILLIS)
			return;
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		if (dt < 900 && (dt / 60) % 2 == 0) {
			alpha(g, 1f - dt / 900f);
			g.drawImage(art(this.phase2 ? 2 : 1), this.positionX,
					this.positionY, WIDTH, HEIGHT, null);
		}
		int[][] bursts = {{72, 37}, {30, 30}, {114, 32}, {50, 60}, {96, 58},
				{12, 34}, {132, 36}, {72, 70}, {60, 20}, {86, 22}};
		for (int i = 0; i < bursts.length; i++) {
			long bt = dt - i * 100;
			if (bt < 0 || bt >= 420)
				continue;
			float u = bt / 420f;
			double r = 4 + 20 * u;
			int x = this.positionX + bursts[i][0];
			int y = this.positionY + bursts[i][1];
			alpha(g, 1f - u);
			g.setColor(u < 0.3f ? Color.WHITE : u < 0.6f ? FIRE : CORE_RED);
			g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
		}
		if (dt >= 1000) {
			float u = (dt - 1000) / 600f;
			double r = 20 + 90 * u;
			int x = this.positionX + CENTER_X;
			int y = this.positionY + CORE_Y;
			alpha(g, 0.9f * (1f - u));
			g.setPaint(new RadialGradientPaint(x, y, (float) r,
					new float[] {0f, 0.4f, 1f},
					new Color[] {Color.WHITE, FIRE, withAlpha(CORE_RED, 0f)}));
			g.fill(new Ellipse2D.Double(x - r, y - r, r * 2, r * 2));
		}
		alpha(g, 1f);
	}

	/** Escorts, attacks and sparks, drawn with the playfield (shaken). */
	@Override
	public final void drawWarnings(final DrawManager drawManager) {
		if (!this.started)
			return;
		Graphics2D g = graphics(drawManager);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		try {
			long now = now();
			drawEscorts(g, drawManager, now);
			if (this.destroyed || isIntro())
				return;
			long t = actionTime();
			if (this.action == Action.LASER)
				drawLaser(g, drawManager, t);
			if (this.action == Action.HALO)
				drawHalo(g, t);
			if (this.action == Action.BARRAGE)
				drawMissiles(g, t);
			drawShatter(g, now);
			drawSparks(g, now);
		} finally {
			g.dispose();
		}
	}

	/** Screen flash and "SHIELD BROKEN", drawn on top without shake. */
	@Override
	public final void drawOverlay(final DrawManager drawManager) {
		if (!this.started)
			return;
		Graphics2D g = graphics(drawManager);
		g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
				RenderingHints.VALUE_ANTIALIAS_ON);
		try {
			long now = now();
			if (this.impactAt >= 0) {
				long dt = now - this.impactAt;
				if (dt >= 0 && dt < 280) {
					alpha(g, 0.85f * (1f - dt / 280f));
					g.setColor(new Color(255, 200, 190));
					g.fillRect(-20, -20, this.screenWidth + 40, this.screenHeight + 40);
				}
			}
			if (this.shieldBrokenAt >= 0)
				drawShieldBroken(g, drawManager, now - this.shieldBrokenAt);
		} finally {
			g.dispose();
		}
	}

	private void drawShieldBroken(final Graphics2D g, final DrawManager dm,
			final long dt) {
		if (dt >= 0 && dt < 320) {
			// The bubble pops: grows to 1.3, then 1.7, fading out.
			float u = dt / 320f;
			double scale = u < 0.4 ? 1 + 0.3 * (u / 0.4)
					: 1.3 + 0.4 * ((u - 0.4) / 0.6);
			double w = 40 * scale, h = 36 * scale;
			alpha(g, 1f - u);
			g.setColor(withAlpha(ION, 0.22f));
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
			g.setFont(dm.getPixelFont(18f, true));
			String text = "SHIELD BROKEN";
			int tw = g.getFontMetrics().stringWidth(text);
			g.drawString(text, (this.screenWidth - tw) / 2,
					268 - Math.round(16 * u));
		}
	}

	private static void alpha(final Graphics2D g, final float a) {
		g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER,
				Math.max(0f, Math.min(1f, a))));
	}

	private static Color withAlpha(final Color c, final float a) {
		return new Color(c.getRed(), c.getGreen(), c.getBlue(),
				Math.max(0, Math.min(255, Math.round(a * 255))));
	}

	private static int lerp(final int a, final int b, final double u) {
		return (int) Math.round(a + (b - a) * Math.max(0, Math.min(1, u)));
	}

	private static double easeOut(final double u) {
		double v = Math.max(0, Math.min(1, u));
		return 1 - (1 - v) * (1 - v);
	}

	private static double easeIn(final double u) {
		double v = Math.max(0, Math.min(1, u));
		return v * v;
	}

	/** Linear interpolation between evenly spaced keyframes. */
	private static double keyframes(final double[] values, final double u) {
		double p = Math.max(0, Math.min(1, u)) * (values.length - 1);
		int i = Math.min(values.length - 2, (int) p);
		return values[i] + (values[i + 1] - values[i]) * (p - i);
	}

	// ---- Escorts and missiles ----

	private void drawEscorts(final Graphics2D g, final DrawManager dm,
			final long now) {
		long t = this.action == Action.BARRAGE ? now - this.actionStart : 0;
		for (Escort e : this.escorts) {
			EnemyShip s = e.ship;
			if (s.isDestroyed()) {
				s.setColor(FIRE); // explosion in her colours
				dm.drawEntity(s, s.getPositionX(), s.getPositionY());
				continue;
			}
			int x = s.getPositionX();
			int y = s.getPositionY();
			alpha(g, 1f);
			g.drawImage(art(3), x, y, ESCORT_WIDTH, 16, null);
			// Core charges up while lined up.
			if (this.action == Action.BARRAGE && t >= BARRAGE_LINED_UP
					&& t < BARRAGE_FIRE) {
				float c = (t - BARRAGE_LINED_UP) / (float) (BARRAGE_FIRE - BARRAGE_LINED_UP);
				alpha(g, 0.3f + 0.6f * c);
				g.setPaint(new RadialGradientPaint(x + 12, y + 10, 4 + 6 * c,
						new float[] {0f, 0.5f, 1f}, new Color[] {Color.WHITE,
								CORE_RED, withAlpha(CORE_RED, 0f)}));
				g.fill(new Ellipse2D.Double(x + 2 - 6 * c, y - 6 * c, 20 + 12 * c,
						20 + 12 * c));
			}
			if (areEscortsShielded()) {
				// Hex shield: can't be hurt while charging.
				float flick = (now / 90) % 2 == 0 ? 1f : 0.75f;
				Path2D hex = new Path2D.Double();
				double cx = x + 12, cy = y + 8, rx = 15, ry = 12;
				for (int i = 0; i < 6; i++) {
					double a = Math.toRadians(60 * i);
					double px = cx + rx * Math.cos(a), py = cy + ry * Math.sin(a);
					if (i == 0)
						hex.moveTo(px, py);
					else
						hex.lineTo(px, py);
				}
				hex.closePath();
				alpha(g, 0.22f * flick);
				g.setColor(ION);
				g.fill(hex);
				alpha(g, 0.9f * flick);
				g.setStroke(new BasicStroke(1.5f));
				g.draw(hex);
			}
		}
		alpha(g, 1f);
	}

	/** @return Where missile i lands. */
	private int missileTargetX(final int i) {
		int n = ESCORTS * MISSILES_PER_ESCORT;
		return 18 + i * (this.screenWidth - 36) / (n - 1);
	}

	private void drawMissiles(final Graphics2D g, final long t) {
		int n = ESCORTS * MISSILES_PER_ESCORT;
		if (t >= BARRAGE_FIRE && t < BARRAGE_IMPACT) {
			for (int i = 0; i < n; i++) {
				for (int k = 5; k >= 0; k--) {
					double u = (t - BARRAGE_FIRE - k * 28)
							/ (double) (BARRAGE_IMPACT - BARRAGE_FIRE);
					if (u < 0)
						continue;
					int x = lerp(this.missileFromX[i], missileTargetX(i), easeOut(u));
					int y = (int) Math.round(this.missileFromY
							+ (groundY() - this.missileFromY) * u * u);
					if (k == 0) {
						alpha(g, 1f);
						g.setColor(CORE_RED);
						g.fillRect(x - 2, y - 7, 4, 9);
						g.setColor(HOT_WHITE);
						g.fillRect(x - 1, y, 2, 3);
					} else {
						alpha(g, 0.7f - k * 0.11f);
						g.setColor(k < 3 ? FIRE : DARK_RED);
						g.fillRect(x - 2, y - 8, 4, 4);
					}
				}
			}
		}
		if (t >= BARRAGE_IMPACT && t < BARRAGE_IMPACT + 600) {
			float u = (t - BARRAGE_IMPACT) / 600f;
			for (int i = 0; i < n; i++) {
				int x = missileTargetX(i);
				double r = 6 + 20 * u;
				alpha(g, 1f - u);
				g.setPaint(new RadialGradientPaint(x, groundY(), (float) r,
						new float[] {0f, 0.35f, 1f}, new Color[] {Color.WHITE,
								FIRE, withAlpha(CORE_RED, 0f)}));
				g.fill(new Ellipse2D.Double(x - r, groundY() - r, r * 2, r * 2));
			}
		}
		alpha(g, 1f);
	}

	// ---- Halo shockwave ----

	private void drawHalo(final Graphics2D g, final long t) {
		int tell = haloTell();
		int fall = haloFall();
		// Tell: the halo (or its shards) glows.
		if (t < tell) {
			float p = (t / 80) % 2 == 0 ? 1f : 0.5f;
			alpha(g, 0.85f * p);
			g.setColor(CORE_RED);
			g.setStroke(new BasicStroke(3f));
			int hx = this.positionX + CENTER_X;
			int hy = this.positionY + HALO_Y;
			g.draw(new Ellipse2D.Double(hx - 22, hy - 6, 44, 12));
		}
		// Ground markers at the gap (phase 1: from the start; phase 2: once it falls).
		if (!this.haloQuick || t >= tell) {
			float blink = (t / 120) % 2 == 0 ? 1f : 0.4f;
			alpha(g, 0.9f * blink);
			g.setColor(Color.WHITE);
			int half = haloGapWidth() / 2;
			for (int edge : new int[] {this.haloGapX - half, this.haloGapX + half}) {
				g.fillRect(edge - 1, groundY() - 14, 2, 14);
				g.fillRect(edge - 3, groundY() - 2, 6, 2);
			}
		}
		if (t < tell)
			return;
		float a = t < tell + fall ? 1f
				: Math.max(0f, 1f - (t - tell - fall) / (float) HALO_FADE);
		int half = haloGapWidth() / 2;
		Integer lastX = null;
		double lastY = 0;
		for (int x = 0; x <= this.screenWidth; x += 3) {
			double y = waveY(x, Math.min(t, tell + fall));
			boolean inGap = x > this.haloGapX - half && x < this.haloGapX + half;
			boolean shardGap = this.haloQuick && (x / 9) % 3 == 2;
			if (Double.isNaN(y) || inGap || shardGap) {
				lastX = null;
				continue;
			}
			if (this.haloQuick)
				y += ((x / 9) % 2 == 0 ? 1 : -1); // jagged shards
			if (lastX != null) {
				alpha(g, 0.3f * a);
				g.setColor(CORE_RED);
				g.setStroke(new BasicStroke(9f));
				g.drawLine(lastX, (int) lastY, x, (int) y);
				alpha(g, a);
				g.setStroke(new BasicStroke(3f));
				g.drawLine(lastX, (int) lastY, x, (int) y);
				g.setColor(HOT_WHITE);
				g.setStroke(new BasicStroke(1f));
				g.drawLine(lastX, (int) lastY, x, (int) y);
			}
			lastX = x;
			lastY = y;
		}
		alpha(g, 1f);
	}

	/** Phase 2 starts: her halo breaks and the pieces fly off. */
	private void drawShatter(final Graphics2D g, final long now) {
		if (this.shatterAt < 0)
			return;
		long dt = now - this.shatterAt;
		if (dt < 0 || dt >= 900)
			return;
		float u = dt / 900f;
		g.setColor(CORE_RED);
		g.setStroke(new BasicStroke(2f));
		for (int i = 0; i < 10; i++) {
			double a = Math.toRadians(i * 36 + 10);
			double d = 22 + 110 * u;
			double x = this.shatterX + Math.cos(a) * d;
			double y = this.shatterY + Math.sin(a) * d * 0.5 + 160 * u * u;
			alpha(g, 1f - u);
			g.drawLine((int) x, (int) y, (int) (x + Math.cos(a + 1.2) * 6),
					(int) (y + Math.sin(a + 1.2) * 3));
		}
		alpha(g, 1f);
	}

	private void drawSparks(final Graphics2D g, final long now) {
		Iterator<long[]> it = this.sparks.iterator();
		while (it.hasNext()) {
			long[] s = it.next();
			long dt = now - s[2];
			if (dt >= 140 || dt < 0) {
				it.remove();
				continue;
			}
			float u = dt / 140f;
			alpha(g, 1f - u);
			g.setColor(u < 0.4f ? Color.WHITE : FIRE);
			g.setStroke(new BasicStroke(1.5f));
			int len = 3 + (int) (5 * u);
			for (int[] d : new int[][] {{-1, 1}, {1, 1}, {-1, -1}, {1, -1}, {0, 1}})
				g.drawLine((int) s[0], (int) s[1], (int) s[0] + d[0] * len,
						(int) s[1] + d[1] * len);
		}
		alpha(g, 1f);
	}

	// ---- Laser ----

	private void drawLaser(final Graphics2D g, final DrawManager dm,
			final long t) {
		int charge = this.chargeMillis;
		List<Integer> xs = beamXs();

		if (t < charge && !this.quietLaser) {
			// Aim line: dashed, flickering every ~0.27 s.
			int top = beamTop(this.lockX);
			alpha(g, (t / 270) % 2 == 0 ? 0.7f : 0.15f);
			g.setColor(ALERT);
			for (int y = top + 12; y < groundY(); y += 10)
				g.fillRect(this.lockX - 1, y, 2, Math.min(6, groundY() - y));
		}

		for (int x : xs) {
			int top = beamTop(x);
			// Charge orb: pulses while growing, becomes the muzzle flare. In
			// phase 2 the charge is only a small, faint flicker.
			double orb;
			float orbAlpha = 1f;
			if (t < charge && this.quietLaser) {
				orb = 0.35;
				orbAlpha = (t / 100) % 2 == 0 ? 0.55f : 0.2f;
			} else if (t < charge)
				orb = keyframes(new double[] {0.2, 0.5, 0.35, 0.75, 0.55, 1.0,
						0.8, 1.25}, t / (double) charge);
			else if (t < growEnd())
				orb = 1.6;
			else if (t < fireEnd())
				orb = 1.45;
			else {
				orb = 1.45;
				orbAlpha = Math.max(0f, 1f - (t - fireEnd())
						/ (float) (fadeEnd() - fireEnd()));
			}
			if (x != this.lockX)
				orb *= 0.7; // side beams are a little smaller
			if (t < fadeEnd())
				drawOrb(g, x, top, orb, orbAlpha);
			if (t >= charge && t < fadeEnd())
				drawBeam(g, x, top, t);
		}

		if (t < charge && !this.quietLaser) {
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

	private void drawBeam(final Graphics2D g, final int x, final int top,
			final long t) {
		double grow = Math.min(1.0, (t - this.chargeMillis)
				/ (double) (growEnd() - this.chargeMillis));
		int bottom = top + (int) Math.round((groundY() - top) * grow);
		float a;
		if (t < fireEnd())
			a = t < growEnd() ? 1f : ((t / 60) % 2 == 0 ? 1f : 0.8f);
		else
			a = Math.max(0f, 1f - (t - fireEnd())
					/ (float) (fadeEnd() - fireEnd()));
		int h = Math.max(1, bottom - top);
		int half = BEAM_WIDTH / 2;

		// Wide soft glow.
		alpha(g, 0.18f * a);
		g.setColor(ALERT);
		g.fillRect(x - 28, top, 56, h);
		// Outer layer: transparent edges, solid middle.
		alpha(g, a);
		g.setPaint(new LinearGradientPaint(x - half, 0, x + half, 0,
				new float[] {0f, 0.2f, 0.4f, 0.6f, 0.8f, 1f},
				new Color[] {withAlpha(ALERT, 0f), withAlpha(ALERT, 0.45f),
						ALERT, ALERT, withAlpha(ALERT, 0.45f),
						withAlpha(ALERT, 0f)}));
		g.fillRect(x - half, top, BEAM_WIDTH, h);
		// Core: white strip with a glow.
		alpha(g, 0.35f * a);
		g.setColor(Color.WHITE);
		g.fillRect(x - 5, top, 10, h);
		alpha(g, a);
		g.fillRect(x - 3, top, 6, h);
		// Short streaks just outside the beam.
		g.setColor(ALERT);
		for (int i = 0; i < 6; i++) {
			int sy = top + (int) ((i * 61 + t / 3) % Math.max(1, h));
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

	// =====================================================================
	// Art (palette: k outline, d/g/l hull shades, r red light, o dark red,
	// w hot white, . see-through)
	// =====================================================================

	/** Phase 1 art, 72 x 40 (drawn at 2x). */
	private static final String[] FORM_1 = {
		"...................................rr...................................",
		"...................................rr...................................",
		"................................rrrrrrrr................................",
		"...........................rrrrrr.dlldrrrrrrr...........................",
		".........................rr.......dlld......rrr.........................",
		".......................rrr........dlld.......rrrr.......................",
		"......................or.rr.......dlld......rrr.ro......................",
		"..................r...dl...rrrrrr.dlldrrrrrrr...ld...r..................",
		".................or...dl........rrdlldrr........ld...ro.................",
		"...........r.....dl...dl..........kllk..........ld...ld.....r...........",
		"..........or.....dl...dl.........kllllk.........ld...ld.....ro..........",
		"......r...dl.....dl...dl........klloollk........ld...ld.....ld...r......",
		".....or...dl..kkkllkkkllkkkk...kllgoogllk...kkkkllkkkllkkk..ld...ro.....",
		".....dl...dlkkgllllllllllllk..kllgdoodgllk..kllllllllllllgkkld...ld.....",
		".....dl..klggggggggggggggggk.kllgdrrrrdgllk.kgggggggggggggggglk..ld.....",
		"......kkkgggggggrrgrrgrrgrrgkllgdrrrrrrdgllkgrrgrrgrrgrrgggggggkkk......",
		"...kkrrrrrrrrgggggggggggggggllgdrrrrrrrrdgllgggggggggggggggrrrrrrrrkk...",
		".kkggggggggggggggggggggggggllgddrrwwwwrrddgllggggggggggggggggggggggggkk.",
		"kgrrrrrrrrrrrggdddddddddddllgooorrwwwwrroooglldddddddddddggrrrrrrrrrrrgk",
		"kggggggggggggggggogggogggogllgddrrwwwwrrddgllgogggogggoggggggggggggggggk",
		".kkrrrrrrrrrrgggggggggggggggllgdrrrrrrrrdgllgggggggggggggggrrrrrrrrrrkk.",
		"...kkkgggggggggggggggggggggggllgdrrrrrrdgllgggggggggggggggggggggggkkk...",
		"......kkkggggggkkgggggggggggggllgdrrrrdgllgggggggggggggkkggggggkkk......",
		".........kgkggk..kkddddddddddddklgddddglkddddddddddddkk..kggkgk.........",
		"......r...r.kkk....kkdddddddddk.klgddglk.kdddddddddkk....kkk.r...r......",
		"......r...r..........kkkkdkkkdk..klgglk..kdkkkdkkkk..........r...r......",
		"......o...o......r...r...r...kgkkggrrggkkgk...r...r...r......o...o......",
		".................r...r...r....kggggrrggggk....r...r...r.................",
		".................r...r...r....kggggooggggk....r...r...r.................",
		".................o...r...o....kggggrrggggk....o...r...o.................",
		".....................o.........kgggrrgggk.........o.....................",
		"................................kggrrggk................................",
		"................................kggooggk................................",
		"................................kggrrggk................................",
		".................................kgrrgk.................................",
		"..................................krrk..................................",
		"..................................kook..................................",
		"..................................krrk..................................",
		"...................................kk...................................",
		"...................................kk..................................."
	};

	/** Phase 2 art: torn wings, shattered halo, open eye, open jaw. */
	private static final String[] FORM_2 = {
		"...................................rr...................................",
		"...................................rr...................................",
		"................................rrrrrrrr................................",
		"...........................r..rrr.dlld.rrrrrr...........................",
		".........................rr.......dlld......rrr.........................",
		".......................rrr........dlld.......rrrr.......................",
		"......................or.rr.......dlld......rrr.ro......................",
		"......................dl...rrrr...dlldrrrrrrr...ld......................",
		"................o.....dl.........rdlldr.........ld.....o................",
		"...........r......r...dl..........kllk..........ld...r......r...........",
		"..........or..........dl.....r...kloolk...r.....ld..........ro..........",
		"....o.....dl..........dl....r...klloollk...r....ld..........ld.....o....",
		"......r...dl..kkkkkkkkllkkkkr..kllgoogllk..rkkkkllkkkkkkkk..ld...r......",
		"..........dlrkglrllllllllllk..kllrrkkrrllk..kllllllllllrlgkrld..........",
		".........klggggggrgggggggggk.kllrrokkorrllk.kgggggggggrgggggglk.........",
		".......rkgggggggrrrrrgrrgrrgkllrrwwkkrorrllkgrrgrrgrrrrrgggggggkr.......",
		".....kkggrrrrrggggrgggggggggllgrorrkkrrorgllgggggggggrggggrrrrrggkk.....",
		"......rgggggrggggggrgggggggllgrrorrkkrrorrgllgggggggrggggggrgggggr......",
		".......kgooorggdddddddddddloooroorrkkrrooroooldddddddddddggrooogk.......",
		".....rkggggggggggogggogggogllgrrorrkkrrorrgllgogggogggoggggggggggkr.....",
		"......kggrrrrggggggggggrggggllgrorrkkrrorgllggggrggggggggggrrrrggk......",
		".......rggggggggggggggggrggggllrrorkkrorrllggggrggggggggggggggggr.......",
		"......kkkggggggkkgggggggggggggllrrokkorrllgggggggggggggkkggggggkkk......",
		".........rgkggk..kkddddddddddddrlrrkkrrlrddddddddddddkk..kggkgr.........",
		"..........r.kkk....kkdddddddddr.klgddglk.rdddddddddkk....kkk.r..........",
		"..........r..........kkdkdkdddk..klgglk..kdddkdkdkk..........r..........",
		"..........r......r.r.r.r.r.kgggkkgorrogkkgggk.r.r.r.r.r......r..........",
		"..........o......r.r.r.r.r..kgggggorrogggggk..r.r.r.r.r......o..........",
		".................r.r.r.r.r..kgggggorrogggggk..r.r.r.r.r.................",
		".................r.o.r.o.r..kgggggorrogggggk..r.o.r.o.r.................",
		".................r...r...r..kggggoorrooggggk..r...r...r.................",
		".................o...r...o...kgggoorroogggk...o...r...o.................",
		".....................o.......kgggoorroogggk.......o.....................",
		"............................kgggooorrooogggk............................",
		".............................kggooorroooggk.............................",
		"..............................kgooorrooogk..............................",
		"..............................kkooorroookk..............................",
		".............................kk..........kk.............................",
		"..............................k..........k..............................",
		"..............................k..........k.............................."
	};

	/** Escort fighter, 12 x 8 (drawn at 2x). */
	private static final String[] ESCORT_ART = {
		"k..........k",
		"gk...kk...kg",
		"rgk.kllk.kgr",
		".dgkgllgkgd.",
		"..dggrrggd..",
		"...dgrrgd...",
		"....dggd....",
		".....kk....."
	};
}
