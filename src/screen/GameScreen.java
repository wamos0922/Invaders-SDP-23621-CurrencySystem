package screen;


import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Set;

import engine.soundEngine.Sound;
import engine.soundEngine.SoundManager;
import story.StoryMusic;
import engine.CoinDropManager;
import engine.Cooldown;
import engine.Core;
import engine.DrawManager;
import engine.CurrencyManager;
import engine.DailyChallenge;
import engine.EndlessMode;
import engine.GameSettings;
import engine.GameState;
import engine.Achievement;
import engine.DamageDimEffect;
import engine.LightningChain;
import engine.GameEvents;
import engine.GlitchEffect;
import engine.PurchaseManager;
import engine.MeteorManager;
import engine.ScreenShake;
import engine.PointsEffect;
import engine.PlayerSounds;
import engine.soundEngine.ItemSounds;
import entity.AIWarship;
import entity.Bullet;
import entity.BulletPool;
import entity.Coin;
import entity.CoinPool;
import entity.EnemyShip;
import entity.Boss;
import entity.EnemyType;
import entity.FinalBoss;
import entity.TempestQueen;
import entity.AICommander;
import entity.EnemyShipFormation;
import entity.RingDreadnought;
import entity.Entity;
import entity.Particle;
import entity.Ship;
import item.ItemSystem;
import skilltree.SkillArmor;
import skilltree.SkillEffects;
import skilltree.SkillSalvage;
import skilltree.SkillShots;
import skilltree.SkillStat;
import item.ItemAPI.EventType;
import item.ItemAPI.ItemEvent;

/**
 * Implements the game screen, where the action happens.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public class GameScreen extends Screen {

	/** Milliseconds until the screen accepts user input. */
	private static final int INPUT_DELAY = 6000;
	/** Bonus score for each life remaining at the end of the level. */
	private static final int LIFE_SCORE = 100;
	/** Minimum time between bonus ship's appearances. */
	private static final int BONUS_SHIP_INTERVAL = 20000;
	/** Maximum variance in the time between bonus ship's appearances. */
	private static final int BONUS_SHIP_VARIANCE = 10000;
	/** Time until bonus ship explosion disappears. */
	private static final int BONUS_SHIP_EXPLOSION = 500;
	/** Time from finishing the level to screen change. */
	private static final int SCREEN_CHANGE_INTERVAL = 1500;
	// Game over animation timings. AUTHORED BY: VFX TEAM (Effection)
	/** Pause after the player ship explodes, before enemies disappear. */
	private static final int GAME_OVER_PAUSE = 500;
	/** Time between the first enemy rows starting to shrink on game over. */
	private static final int GAME_OVER_ROW_INTERVAL_START = 400;
	/** Fastest time between enemy rows starting to shrink on game over. */
	private static final int GAME_OVER_ROW_INTERVAL_MIN = 250;
	/** Reduction of the row interval after each enemy row starts. */
	private static final int GAME_OVER_ROW_INTERVAL_STEP = 50;
	/** Time an enemy takes to shrink and fade out on game over. */
	private static final int GAME_OVER_SHRINK_DURATION = 450;
	/** Time after the last enemy disappears before the banner appears. */
	private static final int GAME_OVER_LAST_ROW_HOLD = 400;
	/** Text of the game over banner. */
	private static final String GAME_OVER_TEXT = "GAME OVER";
	/** Time between each letter of the game over banner appearing. */
	private static final int GAME_OVER_TYPE_INTERVAL = 100;
	/** Time the game over banner stays on or off while blinking. */
	private static final int GAME_OVER_BLINK_INTERVAL = 200;
	/** Number of times the game over banner blinks. */
	private static final int GAME_OVER_BLINK_COUNT = 3;
	/** Time the game over banner stays steady before fading out. */
	private static final int GAME_OVER_TEXT_HOLD = 600;
	/** Time the fade to black takes before the game over menu. */
	private static final int GAME_OVER_FADE_DURATION = 800;
	/** How long an achievement unlock popup remains visible. */
	private static final int ACHIEVEMENT_POPUP_INTERVAL = 3000;
	/** Time used for the popup to slide in. */
	private static final int ACHIEVEMENT_POPUP_SLIDE_IN = 250;
	/** Time used for the popup to slide out. */
	private static final int ACHIEVEMENT_POPUP_SLIDE_OUT = 350;
	/** Height of the interface separation line. */
	private static final int SEPARATION_LINE_HEIGHT = 40;
	/** Distance between the lines of a boss warning (GOG-77). */
	private static final int WARNING_LINE_HEIGHT = 22;
	/** Speed of a Mite's shrapnel bullets (GOG-86). */
	private static final int MITE_SHRAPNEL_SPEED = 4;
	/**
	 * Highest the ship may fly in endless free roam (GOG-7): below the HUD,
	 * the item slots and the bonus ship's lane.
	 */
	private static final int FREE_ROAM_TOP = SEPARATION_LINE_HEIGHT + 36;
	/** Distance from the bottom of the screen to the ship's starting row. */
	private static final int SHIP_ROW_FROM_BOTTOM = 30;
	/** Lives at or below this value start the glitch. */
	private static final int LOW_HEALTH_LIVES = 1;
	/** Low-life effect duration (ms). */
	private static final int LOW_HEALTH_EFFECT_DURATION = 5000;
	/** Coins awarded when a regular enemy's drop chance succeeds. */
	private static final int COIN_VALUE = 1;
	/** Coins guaranteed when the special bonus ship is destroyed. */
	private static final int BONUS_COIN_VALUE = 5;
	/** Delay before the extra score boost popup follows the normal one (ms, Team CS). */
	private static final int SCORE_BOOST_POPUP_DELAY = 250;
	/** The extra score boost popup starts this much lower, so the two can be told apart (px, Team CS). */
	private static final int SCORE_BOOST_POPUP_OFFSET_Y = 14;
	/** Minimum time between two pause-key toggles, to debounce the key. */
	private static final int PAUSE_KEY_DEBOUNCE = 200;
	/** Minimum time between two pause-menu cursor moves. */
	private static final int PAUSE_SELECTION_DEBOUNCE = 200;

	/**
	 * Options listed in the pause menu, in the order they are drawn.
	 */
	private enum PauseOption {
		/** Closes the pause menu and continues the level. */
		RESUME("Resume"),
		/** Restarts the current level from the beginning. */
		RESTART("Restart"),
		/** Ends the run and returns to the score / main menu flow. */
		QUIT("Quit to menu");

		/** Text drawn for this option. */
		private final String label;

		PauseOption(final String label) {
			this.label = label;
		}

		/**
		 * @return Text drawn for this option.
		 */
		private String getLabel() {
			return this.label;
		}

		/**
		 * @return Option below this one, wrapping around to the top.
		 */
		private PauseOption next() {
			PauseOption[] values = values();
			return values[(this.ordinal() + 1) % values.length];
		}

		/**
		 * @return Option above this one, wrapping around to the bottom.
		 */
		private PauseOption previous() {
			PauseOption[] values = values();
			return values[(this.ordinal() - 1 + values.length) % values.length];
		}
	}

	/** Current game difficulty settings. */
	private GameSettings gameSettings;
	/** Current difficulty level number. */
	private int level;
	/** Formation of enemy ships. */
	private EnemyShipFormation enemyShipFormation;
	/** Player's ship. */
	private Ship ship;
	/** Warp In effect, null if not bought. AUTHORED BY: VFX TEAM (Effection) */
	private engine.WarpIn warpIn;
	/** Bonus enemy ship that appears sometimes. */
	private EnemyShip enemyShipSpecial;
	/** Minimum time between bonus ship appearances. */
	private Cooldown enemyShipSpecialCooldown;
	/** Time until bonus ship explosion disappears. */
	private Cooldown enemyShipSpecialExplosionCooldown;
	/** Time from finishing the level to screen change. */
	private Cooldown screenFinishedCooldown;

	// Game over animation state. AUTHORED BY: VFX TEAM (Effection)
	/** Time until the next enemy row explodes on game over. */
	private Cooldown gameOverRowCooldown;
	/** Current time between enemy rows starting to shrink on game over. */
	private int gameOverRowInterval;
	/** Enemies shrinking on game over, with the moment each one started. */
	private Map<EnemyShip, Long> shrinkingEnemies;
	/** Moment the last enemy disappeared, 0 while enemies remain. */
	private long gameOverLastEnemyGone;
	/** Moment the game over banner starts appearing. */
	private long gameOverBannerStart;
	/** Checks if the game over sequence is playing. */
	private boolean gameOverActive;
	/** Checks if the game over banner is shown. */
	private boolean showGameOverText;

	/** Time until the achievement unlock popup closes. */
	private Cooldown achievementPopupCooldown;
	/** Achievement currently shown in the unlock popup. */
	private Achievement unlockedAchievement;
	/** Achievements waiting to be shown in the unlock popup. */
	private Queue<Achievement> achievementPopupQueue;
	/** Time when the current achievement popup started. */
	private long achievementPopupStartedAt;
	/** Set of all bullets fired by on screen ships. */
	private Set<Bullet> bullets;
	/** Set of coins currently dropped and falling on screen. */
	private Set<Coin> coins;
	/** Story background for this level, or null for endless mode (GOG-14). */
	private java.awt.image.BufferedImage storyBackground;
	/** Small animations on top of the story background (GOG-14). */
	private engine.StoryBackgroundEffects backgroundEffects;

	/** Coins picked up since this level (or its last restart) began. They
	 * are already banked in CurrencyManager, so a restart has to take them
	 * back, or restarting would be a free coin farm. */
	private int coinsCollectedThisLevel;
	/** Decides, with a low configurable chance, whether a kill drops a
	 * coin, so currency income doesn't scale 1:1 with kills. */
	private CoinDropManager coinDropManager;
	/** Current score. */
	private int score;
	/** Player lives left. */
	private int lives;
	/** Total bullets shot by the player. */
	private int bulletsShot;
	/** Total ships destroyed by the player. */
	private int shipsDestroyed;
	/** Total coins collected by the player. */
	private int coinsCollected;
	/** Moment the game starts. */
	private long gameStartTime;
	/** Checks if the level is finished. */
	private boolean levelFinished;
	/** True once the boss's clock was started at the end of the countdown (GOG-75). */
	private boolean bossFightStarted;
	/**
	 * End of Act 1 (GOG-74): the black hole that swallows the player after
	 * the AI Warship is destroyed; null on every other level.
	 */
	private BlackHoleScene blackHole;
	/** True while a Stinger hit keeps the player at 1 life (GOG-86). */
	private boolean vulnerableActive;
	/** Lives the player had when the Stinger hit, given back later (GOG-86). */
	private int livesBeforeVulnerable;
	/** When the 1-life period ends (GOG-86). */
	private long vulnerableUntil;
	/** Sparkles from destroyed ships. AUTHORED BY: VFX TEAM (Effection) */
	private List<Particle> particles = new ArrayList<Particle>();
	/** Colors the sparkles can have. AUTHORED BY: VFX TEAM (Effection) */
	private static final Color[] SPARKLE_COLORS = { Color.WHITE,
			Color.YELLOW, Color.ORANGE, Color.CYAN };
	/** Checks if a bonus life is received. */
	private boolean bonusLife;
	/** Timer for the low-life effect. */
	private Cooldown lowHealthTimer;
	/** Lives in the last frame. */
	private int prevLives;
	/** True while the low-life effect is on. */
	private boolean lowHealthActive;

	/** Whether the player has taken actual damage during this run. */
	private boolean tookDamage;
	/** Dims the screen when the player is hit. */
	private DamageDimEffect damageDim;
	/** Lightning Chain kill effect. AUTHORED BY: VFX TEAM (Effection) */
	private LightningChain lightningChain;
	/** Test switch: java -Dvfx.lightning=true turns it on without buying. */
	private static final boolean LIGHTNING_ON = Boolean.getBoolean("vfx.lightning");
	/** Test switch: java -Dgame.invincible=true never loses a life (GOG-77 testing). */
	private static final boolean INVINCIBLE = Boolean.getBoolean("game.invincible");
	/** True when Lightning Chain is bought and switched on (or the test switch is on). */
	private boolean lightningOn;
	/** Glitch effect for low health. */
	private GlitchEffect glitch;
	/** Screen shake when an enemy is destroyed. */
	private ScreenShake screenShake;
	/** True if Comet Trail is bought and switched on. AUTHORED BY: VFX TEAM (Effection) */
	private boolean cometTrailOn;
	/** Bonus meteor and its diamonds. AUTHORED BY: VFX TEAM (Effection) */
	private MeteorManager meteors;
	/** Points shown when enemies are destroyed. AUTHORED BY: VFX TEAM (Effection) */
	private PointsEffect pointsEffect;
	/** Ghost Trail ship effect, null if not bought or switched off. AUTHORED BY: VFX TEAM (Effection) */
	private engine.GhostTrail ghostTrail;
	/** Diamonds earned this run but not yet cashed out; lost on death,
	 * banked into DiamondManager only when the player cashes out. */
	private int pendingDiamonds;
	/** Levels cleared alive during the current run. */
	private int levelsCompletedRun;
	/** Score boost popups waiting to be shown (Team CS): {due time, x, y, points, bonus ship 1/0}. */
	private List<long[]> scoreBoostPopups;
	/** Item system of this run (Team CS). Kept across levels by ItemSystem. */
	private ItemSystem items;
	/** Effects of the ship's unlocked skill tree nodes (GOG-89). */
	private SkillEffects skills;
	/** Armor skill nodes in this level: shields and blocked hits (GOG-89). */
	private SkillArmor armor;
	/** Specialty skill nodes that change shots and hits (GOG-89). */
	private SkillShots skillShots;
	/** Item slot keys (1-9) held last frame, so holding a key uses it once. */
	private boolean[] itemSlotKeysHeld = new boolean[9];
	/** Skin chosen in Customize; animated skins change colour each frame (GoG). */
	private shop.Skin skin;
	/** Whether gameplay is currently paused. */
	private boolean paused;
	/** Wall-clock time when the current pause began. */
	private long pauseStartedAt;
	/** Debounces the pause key so holding it doesn't toggle every frame. */
	private Cooldown pauseKeyCooldown;
	/** Debounces cursor moves in the pause menu. */
	private Cooldown pauseSelectionCooldown;
	/** Option the cursor is on in the pause menu. */
	private PauseOption pauseSelection;
	/** Score the level started with, restored by "Restart". */
	private final int initialScore;
	/** Lives the level started with (bonus life included), restored by
	 * "Restart". */
	private final int initialLives;
	/** Bullets-shot count the level started with, restored by "Restart". */
	private final int initialBulletsShot;
	/** Ships-destroyed count the level started with, restored by
	 * "Restart". */
	private final int initialShipsDestroyed;
	/** Pending diamonds the level started with, restored by "Restart". */
	private final int initialPendingDiamonds;

	/** Boss encounter; normal stages use enemyShipFormation instead. */
	private Boss midBoss;

	/**
	 * Constructor, establishes the properties of the screen.
	 *
	 * @param gameState
	 *            Current game state.
	 * @param gameSettings
	 *            Current game settings.
	 * @param bonusLife
	 *            Checks if a bonus life is awarded this level.
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @param fps
	 *            Frames per second, frame rate at which the game is run.
	 */
	public GameScreen(final GameState gameState,
	                  final GameSettings gameSettings, final boolean bonusLife,
	                  final int width, final int height, final int fps) {
		super(width, height, fps);

		this.gameSettings = gameSettings;
		this.bonusLife = bonusLife;
		this.level = gameState.getLevel();
		// GOG-14: each part of the story has its own background.
		this.storyBackground = EndlessMode.isEnabled() ? null
				: engine.StoryBackgrounds.forLevel(this.level);
		this.backgroundEffects = this.storyBackground == null ? null
				: new engine.StoryBackgroundEffects(this.level, this.storyBackground);
		this.score = gameState.getScore();
		this.lives = gameState.getLivesRemaining();
		if (this.bonusLife)
			this.lives++;
		this.bulletsShot = gameState.getBulletsShot();
		this.shipsDestroyed = gameState.getShipsDestroyed();
		this.pendingDiamonds = gameState.getPendingDiamonds();
		this.levelsCompletedRun = gameState.getLevelsCompletedRun();
		this.coinsCollected = gameState.getCoinsCollected();
		this.tookDamage = false;



		// Snapshot of how the level started, so "Restart" has something to
		// come back to.
		this.initialScore = this.score;
		this.initialLives = this.lives;
		this.initialBulletsShot = this.bulletsShot;
		this.initialShipsDestroyed = this.shipsDestroyed;
		this.initialPendingDiamonds = this.pendingDiamonds;
	}

	/**
	 * Initializes basic screen properties, and adds necessary elements.
	 */

	public final void initialize() {
		super.initialize();
		// GOG-35: Dedicated gameplay music for Story Mode level ranges.
		// Endless and Daily Challenges retain their normal gameplay BGM.
		if (EndlessMode.isEnabled() || DailyChallenge.isRunning()) {
			StoryMusic.stop();
			SoundManager.getInstance().playBgm(Sound.BGM_GAME);
		} else {
			SoundManager.getInstance().stopBgm();
			// Levels 1-9: FirstPhase, 11-19: SecondPhase.
			// Level 40 still continues Panel 27's finalBoss.wav.
			StoryMusic.playForLevel(this.level);
		}

		// Initialize the player in both boss and normal stages.
		this.ship = new Ship(this.width / 2, this.height - SHIP_ROW_FROM_BOTTOM,
				Core.getSelectedShipType());

		// Story mode boss levels (endless waves are always formations for
		// now, GOG-7). Game design draft: only every 10th level (10, 20,
		// 30, 40) has a boss; there is no mid-boss, so levels 5, 15, 25
		// and 35 are normal formations. GOG-24: the existing final boss
		// is the placeholder for every boss level.
		if (!EndlessMode.isEnabled()
				&& this.level >= 1
				&& this.level <= 40
				&& this.level % 10 == 0) {
			// GOG-74: level 10 is the AI Warship; GOG-75: level 20 is the
			// Tempest Queen; GOG-76: level 30 is the Ring Dreadnought;
			// GOG-77: level 40 is the AI Commander.
			if (this.level == 10)
				this.midBoss = new AIWarship(this.width / 2 - 24, 100);
			else if (this.level == 20)
				this.midBoss = new TempestQueen(this.width / 2 - 24, 100);
			else if (this.level == 30)
				this.midBoss = new RingDreadnought(this.width / 2 - 24, 100);
			else if (this.level == 40)
				this.midBoss = new AICommander(
						this.width / 2 - AICommander.WIDTH / 2, AICommander.SPAWN_Y);
			else
				this.midBoss = new FinalBoss(this.width / 2 - 24, 60,
						DrawManager.SpriteType.FinalBoss1);
			this.bossFightStarted = false;
		} else {
			// Act 1 (story levels 1-10) uses the Earth Escape enemies.
			boolean earthAct = !EndlessMode.isEnabled()
					&& this.level >= 1 && this.level <= 10;
			this.enemyShipFormation = new EnemyShipFormation(
					this.gameSettings, earthAct);
			this.enemyShipFormation.attach(this);
		}
		// Skin chosen in Customize (GoG - Currency System).
		this.skin = PurchaseManager.getInstance().getSelectedSkin();
		this.ship.setSkinColor(this.skin.getColor());
		// Ship look chosen in Customize (A1): only the sprite changes.
		shop.ShapeSkin shape = PurchaseManager.getInstance()
				.getSelectedShape(this.ship.getType());
		this.ship.setShapeSprite(shape == null ? null : shape.getSprite());
		// Skill tree nodes unlocked for this ship (GOG-89).
		this.skills = SkillEffects.forShip(this.ship.getType());
		this.skillShots = new SkillShots(this.skills);
		this.ship.setSkillBonuses(this.skills.multiplier(SkillStat.SPEED),
				this.skills.multiplier(SkillStat.FIRE_RATE),
				this.skills.multiplier(SkillStat.BULLET_SPEED),
				this.skills.multiplier(SkillStat.BULLET_SIZE));
		// Appears each 10-30 seconds.
		this.enemyShipSpecialCooldown = Core.getVariableCooldown(
				BONUS_SHIP_INTERVAL, BONUS_SHIP_VARIANCE);
		this.enemyShipSpecialCooldown.reset();
		this.enemyShipSpecialExplosionCooldown = Core
				.getCooldown(BONUS_SHIP_EXPLOSION);
		this.screenFinishedCooldown = Core.getCooldown(SCREEN_CHANGE_INTERVAL);
		this.lowHealthTimer = Core.getCooldown(LOW_HEALTH_EFFECT_DURATION);
		this.prevLives = this.lives;
		this.lowHealthActive = false;
		this.bullets = new HashSet<Bullet>();
		this.cometTrailOn = PurchaseManager.getInstance().isEffectOn("comet_trail"); // AUTHORED BY: VFX TEAM (Effection)
		this.damageDim = new DamageDimEffect(900, 0.75f,
				new java.awt.Color(150, 0, 0));  //new update dim effect
		GameEvents.subscribe(GameEvents.Type.PLAYER_HIT, this.damageDim);
		this.lightningChain = new LightningChain(); // AUTHORED BY: VFX TEAM (Effection)
		this.lightningOn = LIGHTNING_ON || PurchaseManager.getInstance().isEffectOn("lightning_chain");   // Lightning Chain, on unless switched off in Customize.
		GameEvents.subscribe(GameEvents.Type.PLAYER_HIT, this.lightningChain);
		this.glitch = new GlitchEffect();
		this.screenShake = new ScreenShake();
		this.meteors = new MeteorManager(this.level, this.width, this.height,SEPARATION_LINE_HEIGHT);
		this.pointsEffect = new PointsEffect(SEPARATION_LINE_HEIGHT);
		this.scoreBoostPopups = new ArrayList<long[]>(); // Item System (Team CS)
		// Warp In effect, null if not bought or switched off. AUTHORED BY: VFX TEAM (Effection) */
		if (PurchaseManager.getInstance().isEffectOn("warp_in")) {
			this.warpIn = new engine.WarpIn();
			this.warpIn.startLevel(INPUT_DELAY);
		}
		if (PurchaseManager.getInstance().isEffectOn("ghost_trail"))        // Ghost Trail, on unless switched off in Customize. AUTHORED BY: VFX TEAM (Effection)
			this.ghostTrail = new engine.GhostTrail();
		this.coins = new HashSet<Coin>();
		this.coinsCollectedThisLevel = 0;
		this.achievementPopupQueue = new LinkedList<Achievement>();
		this.coinDropManager = new CoinDropManager();
		// Scavenger skill node: more coins (GOG-89).
		this.coinDropManager.setDropChance(Math.min(1.0,
				CoinDropManager.DEFAULT_DROP_CHANCE
						+ SkillSalvage.coinChanceBonus(this.skills)));
		this.pauseKeyCooldown = Core.getCooldown(PAUSE_KEY_DEBOUNCE);
		this.pauseSelectionCooldown = Core.getCooldown(PAUSE_SELECTION_DEBOUNCE);
		this.pauseSelection = PauseOption.RESUME;
		this.pauseStartedAt = 0;

		// Item System (Team CS): level 1 starts a new run, later levels keep it.
		// Salvage and Cooled Barrel skill nodes change its drops (GOG-89).
		ItemSystem.setRunBonuses(SkillSalvage.itemChanceBonus(this.skills),
				SkillSalvage.groundTimeBonus(this.skills),
				SkillSalvage.rapidFireExtraStacks(this.skills));
		this.items = ItemSystem.forLevel(this.level);
		this.items.beginLevel(this.level, this.width, this.ship,
				new ItemSystem.GameHooks() {
					// Real lives, also while a Stinger holds them at 1, so the
					// life item respects the cap (GOG-86).
					public int getLives() { return realLives(); }
					public void addLife() { lives++; }
					public void addScore(final int points) { score += points; }
				});

		// Special input delay / countdown.
		this.gameStartTime = System.currentTimeMillis();
		this.inputDelay = Core.getCooldown(INPUT_DELAY);
		this.inputDelay.reset();
		// Armor skill nodes: Launch Shield starts when the countdown ends (GOG-89).
		this.armor = new SkillArmor(this.skills, this.midBoss != null);
		this.armor.startLevel(this.gameStartTime + INPUT_DELAY);
	}


	/**
	 * Starts the 5 s low-life effect when lives become 1. Stops on timeout
	 * or life gain.
	 * AUTHORED BY: VFX TEAM (Effection)
	 */
	private void updateLowHealthEffect() {
		boolean lowHealth = this.lives > 0 && this.lives <= LOW_HEALTH_LIVES;
		if (lowHealth && this.lives != this.prevLives)
			this.lowHealthTimer.reset();
		this.prevLives = this.lives;
		this.lowHealthActive = lowHealth && !this.lowHealthTimer.checkFinished();
	}

	/**
	 * Starts the action.
	 *
	 * @return Next screen code.
	 */
	public final int run() {
		boolean clearedNormally = false;
		try {
			super.run();
			// An interrupted or otherwise abnormal exit must not retain music.
			clearedNormally = !this.isRunning && this.levelFinished
					&& !this.gameOverActive && this.lives > 0;
		} finally {
			GameEvents.unsubscribe(GameEvents.Type.PLAYER_HIT, this.damageDim);
			GameEvents.unsubscribe(GameEvents.Type.PLAYER_HIT, this.lightningChain);
			// Only successful Story Mode clears of Levels 36-39 hand off music.
			if (!StoryMusic.shouldContinueAfterLevel(this.level,
					!EndlessMode.isEnabled() && !DailyChallenge.isRunning(),
					clearedNormally)) {
				StoryMusic.stop();
			}
		}

		this.items.endLevel(); // Item System (Team CS)


		this.score += LIFE_SCORE * (this.lives - 1);
		this.logger.info("Screen cleared with a score of " + this.score);

		return this.returnCode;
	}

	/**
	 * Updates the elements on screen and checks for events.
	 */
	protected final void update() {
		super.update();
		// Item System (Team CS): item bonuses on top of the base fire rate,
		// bullet speed and movement speed (stacks and the boost item).
		this.ship.setItemBonuses(
				this.items.fireRateBonus(this.ship.getBaseFireRate()),
				this.items.bulletSpeedBonus(),
				this.items.movementSpeedBonus(this.ship.getBaseSpeed()));
		// Skill nodes (GOG-89); Pickup Rush raises the fire rate for a while.
		this.ship.setSkillBonuses(this.skills.multiplier(SkillStat.SPEED),
				this.skills.multiplier(SkillStat.FIRE_RATE)
						* this.skillShots.fireRateFactor(System.currentTimeMillis()),
				this.skills.multiplier(SkillStat.BULLET_SPEED),
				this.skills.multiplier(SkillStat.BULLET_SIZE));
		// Animated skins (GoG); plain skins return the same colour.
		this.ship.setSkinColor(this.skin.colorAt(System.currentTimeMillis()));

		if (inputManager.isKeyDown(KeyEvent.VK_P)
				&& this.pauseKeyCooldown.checkFinished()) {
			if (this.paused)
				resumeGameplay();
			else {
				this.paused = true;
				this.pauseStartedAt = System.currentTimeMillis();
			}
			this.pauseKeyCooldown.reset();
			// Ghost Trail starts fresh after a pause. AUTHORED BY: VFX TEAM (Effection)
			if (this.ghostTrail != null)
				this.ghostTrail.clear();
			this.pauseSelection = PauseOption.RESUME;
			this.logger.info(this.paused ? "Game paused" : "Game resumed");
		}

		if (this.paused) {
			updatePauseMenu();
			draw();
			return;
		}

		if (this.inputDelay.checkFinished() && !this.levelFinished) {

			// The boss's clock starts when the countdown ends, so its
			// warning and timers don't run out during it (GOG-75).
			if (this.midBoss != null && !this.bossFightStarted) {
				this.midBoss.startFight();
				this.bossFightStarted = true;
			}

			// A Polar freeze bullet locks moving and shooting (GOG-84);
			// items can still be used.
			if (!this.ship.isDestroyed() && !this.ship.isFrozen()) {
				boolean moveRight = inputManager.isKeyDown(KeyEvent.VK_RIGHT)
						|| inputManager.isKeyDown(KeyEvent.VK_D);
				boolean moveLeft = inputManager.isKeyDown(KeyEvent.VK_LEFT)
						|| inputManager.isKeyDown(KeyEvent.VK_A);

				boolean isRightBorder = this.ship.getPositionX()
						+ this.ship.getWidth() + this.ship.getSpeed() > this.width - 1;
				boolean isLeftBorder = this.ship.getPositionX()
						- this.ship.getSpeed() < 1;

				if (moveRight && !isRightBorder) {
					this.ship.moveRight();
				}
				if (moveLeft && !isLeftBorder) {
					this.ship.moveLeft();
				}
				// Endless free roam (GOG-7): the ship can also fly up and
				// down, anywhere between the HUD and its starting row.
				if (EndlessMode.isEnabled())
					moveFreeRoam();
				if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
					// Count bullets, not trigger pulls: Two Way fires two at
										// once, and accuracy is kills per bullet.
					int bulletsBefore = this.bullets.size();
					if (this.ship.shoot(this.bullets)) {
						this.bulletsShot += this.bullets.size() - bulletsBefore;
						// Skill nodes change the new bullets (GOG-89).
						this.skillShots.onShot(this.ship.getLastShot(),
								moveLeft || moveRight);
					}
				}
			}
			if (!this.ship.isDestroyed())
				useItemSlots(); // Item System (Team CS)

			if (this.enemyShipSpecial != null) {
				if (!this.enemyShipSpecial.isDestroyed())
					this.enemyShipSpecial.move(this.items.enemiesFrozen() ? 0 : 2, 0);
				else if (this.enemyShipSpecialExplosionCooldown.checkFinished())
					this.enemyShipSpecial = null;

			}
			if (this.enemyShipSpecial == null
					&& this.enemyShipSpecialCooldown.checkFinished()) {
				this.enemyShipSpecial = new EnemyShip();
				this.enemyShipSpecialCooldown.reset();
				this.logger.info("A special ship appears");
			}
			if (this.enemyShipSpecial != null
					&& this.enemyShipSpecial.getPositionX() > this.width) {
				this.enemyShipSpecial = null;
				this.logger.info("The special ship has escaped");
			}

			this.ship.update();
			if (this.midBoss != null) {
				if (!this.items.enemiesFrozen()) {
					this.midBoss.update();
					this.midBoss.moveBoss(this.width, this.height, this.gameSettings);
				}
				this.midBoss.setTarget(
						this.ship.getPositionX() + this.ship.getWidth() / 2,
						this.ship.getPositionY() + this.ship.getHeight() / 2);
				this.midBoss.shoot(this.bullets, this.gameSettings);
				manageBossAttackHits(); // AI Commander laser and claw (GOG-77)
			} else {
				if (!this.items.enemiesFrozen()) {
					this.enemyShipFormation.update();
					this.enemyShipFormation.updateDives(); // Comet kamikaze (GOG-86)
				}
				// Halo Lancer snipers aim at the player (GOG-85).
				this.enemyShipFormation.setTarget(
						this.ship.getPositionX() + this.ship.getWidth() / 2,
						this.ship.getPositionY() + this.ship.getHeight() / 2);
				this.enemyShipFormation.shoot(this.bullets);
				// No bonus meteor in challenges: challenges pay no
				// diamonds, so its diamonds would be lost (GOG-104).
				if (!DailyChallenge.isRunning())
					this.meteors.trySpawn(this.enemyShipFormation, this.ship); // VFX meteor needs a normal formation.
			}
			updateLowHealthEffect();
			this.ship.setBlinking(this.lowHealthActive);
		}

		// Item System (Team CS): drops fall/expire, pickups, effect timers.
		this.items.update(this.ship, this.inputDelay.checkFinished()
				&& !this.levelFinished && this.lives > 0
				&& !this.ship.isDestroyed());
		manageCollisions();
		handleCometBlasts(); // GOG-86
		handleFighterDrops(); // GOG-85
		handleMiteBooms(); // GOG-86
		updateVulnerable(); // GOG-86
		manageFreeRoamCrashes();
		showScoreBoostPopups(); // Item System (Team CS)
		cleanBullets();
		updateParticles(); // AUTHORED BY: VFX TEAM (Effection)
		this.lightningChain.update();
		updateCoins();
		this.pendingDiamonds += this.meteors.update(this.ship, this.lives > 0); // VFX TEAM (Effection): updates the meteor and its diamonds, returns the diamonds to be added to pending
		handleItemEvents();
		updateAchievementPopup();
		// End of Act 1: the black hole pulls the ship in (GOG-74).
		if (this.blackHole != null)
			this.blackHole.pull(this.ship, System.currentTimeMillis());
		draw();

		// Game over sequence, only when the player runs out of lives. AUTHORED BY: VFX TEAM (Effection)
		if (this.lives == 0 && !this.levelFinished)
			startGameOverSequence();
		if (this.gameOverActive) {
			updateGameOverSequence();
			return;
		}

		boolean isCleared = this.midBoss != null
				? this.midBoss.isDestroyed() : this.enemyShipFormation.isEmpty();
		if ((isCleared || this.lives == 0)
				&& !this.levelFinished) {
			// Lives held at 1 by a Stinger come back when the level is won.
			if (isCleared)
				endVulnerable();
			this.levelFinished = true;
			this.screenFinishedCooldown.reset();

			// Level cleared alive: level N is worth N diamonds, kept pending
			// until cashed out (see engine.DiamondManager), and coins still
			// falling are collected so the last kills' drops aren't lost.
			if (isCleared && this.lives > 0) {
				this.levelsCompletedRun++; // Sharpshooter tiers (Mantis)
				SoundManager.getInstance().stopBgm();
				// Keep Panel 26 music alive across victorious Story Levels 36-39.
				if (!StoryMusic.shouldContinueAfterLevel(this.level,
						!EndlessMode.isEnabled() && !DailyChallenge.isRunning(), true)) {
					StoryMusic.stop();
				}
				SoundManager.getInstance().playSfx(Sound.VICTORY);
				this.pendingDiamonds += this.level;
				collectRemainingCoins();
				this.pendingDiamonds += this.meteors.collectRemaining(); // VFX TEAM (Effection): collects remaining diamonds from the meteor and adds them to pending
				// Act 1 ends: the AI Warship's wreck opens a black hole
				// that swallows the player (GOG-74).
				if (this.midBoss instanceof AIWarship) {
					AIWarship warship = (AIWarship) this.midBoss;
					this.blackHole = new BlackHoleScene(warship.getCenterX(),
							warship.getCenterY(), System.currentTimeMillis());
					// Bullets stop moving once the level is over; clear
					// them so none hang in the air during the scene.
					BulletPool.recycle(this.bullets);
					this.bullets.clear();
				}
				showUnlockedAchievements(Core.getAchievementManager()
						.recordFlawlessLevelCompleted(this.tookDamage));
				// Story progress achievements do not count in challenges,
				// which jump straight to later story levels (GOG-102).
				if (!DailyChallenge.isRunning())
					showUnlockedAchievement(Core.getAchievementManager()
							.recordLevelCompleted(this.level));
				showUnlockedAchievement(Core.getAchievementManager().recordRunCoins(this.coinsCollected));

				// Level 10 / game won only count in story mode (GOG-7).
				if (!EndlessMode.isEnabled() && !DailyChallenge.isRunning())
					showUnlockedAchievement(Core.getAchievementManager()
							.recordLevelCompleted(
									this.level,
									Core.getShipId(this.ship.getType()),
									true));

				// Last level cleared alive: the game is beaten.
				if (!EndlessMode.isEnabled() && !DailyChallenge.isRunning()
						&& this.level == Core.getNumLevels())
					showUnlockedAchievement(Core.getAchievementManager()
							.recordGameWon(Core.getSelectedShipId()));
			}
		}

		if (this.levelFinished && this.screenFinishedCooldown.checkFinished()
				&& (this.blackHole == null
				|| this.blackHole.isFinished(System.currentTimeMillis()))
				&& this.unlockedAchievement == null
				&& this.achievementPopupQueue.isEmpty())
			this.isRunning = false;

	}

	/**
	 * Starts the game over sequence: the player ship explodes, then after a
	 * pause the remaining enemies shrink away row by row.
	 * AUTHORED BY: VFX TEAM (Effection)
	 * Any further inquiries please contact us.
	 */
	private void startGameOverSequence() {
		SoundManager.getInstance().stopBgm();
		// The final boss music stops as soon as the player runs out of lives.
		StoryMusic.stop();
		SoundManager.getInstance().playSfx(Sound.GAME_OVER);
		this.levelFinished = true;
		this.gameOverActive = true;

		// Shows the player ship explosion; it stays since the ship is no
		// longer updated.
		this.ship.update();

		BulletPool.recycle(this.bullets);
		this.bullets.clear();
		this.meteors.clear(); // VFX TEAM (Effection):

		// Clears explosions left from enemies shot just before.
		if (this.enemyShipFormation != null)
			this.enemyShipFormation.removeDestroyed();
		this.shrinkingEnemies = new LinkedHashMap<EnemyShip, Long>();
		if (this.midBoss != null)
			this.midBoss.destroy();

		this.gameOverRowInterval = GAME_OVER_ROW_INTERVAL_START;
		this.gameOverRowCooldown = Core.getCooldown(GAME_OVER_PAUSE);
		this.gameOverRowCooldown.reset();
		this.logger.info("Game over, starting disappear sequence.");
	}

	/**
	 * Makes the next enemy row start shrinking, speeding up after each row,
	 * shows the game over banner once no enemies are left, then ends the
	 * screen.
	 * AUTHORED BY: VFX TEAM (Effection)
	 * Any further inquiries please contact us.
	 */
	private void updateGameOverSequence() {
		long now = System.currentTimeMillis();
		if (this.showGameOverText) {
			if (now - this.gameOverBannerStart
					>= getGameOverFadeStart() + GAME_OVER_FADE_DURATION)
				this.isRunning = false;
			return;
		}

		// Clears a bonus ship explosion (shot just before game over) after
		// its usual time, as gameplay does, since that update no longer runs.
		if (this.enemyShipSpecial != null
				&& this.enemyShipSpecial.isDestroyed()
				&& this.enemyShipSpecialExplosionCooldown.checkFinished())
			this.enemyShipSpecial = null;

		// Removes enemies that finished shrinking.
		Iterator<Long> starts = this.shrinkingEnemies.values().iterator();
		while (starts.hasNext())
			if (now - starts.next() >= GAME_OVER_SHRINK_DURATION)
				starts.remove();

		if (!this.gameOverRowCooldown.checkFinished())
			return;

		List<EnemyShip> row = this.enemyShipFormation == null
				? java.util.Collections.<EnemyShip>emptyList()
				: this.enemyShipFormation.removeBottomRow();
		if (!row.isEmpty()) {
			for (EnemyShip enemyShip : row)
				this.shrinkingEnemies.put(enemyShip, now);
			this.gameOverRowCooldown = Core.getCooldown(
					this.gameOverRowInterval);
			this.gameOverRowCooldown.reset();
			this.gameOverRowInterval = Math.max(GAME_OVER_ROW_INTERVAL_MIN,
					this.gameOverRowInterval - GAME_OVER_ROW_INTERVAL_STEP);
		} else if (this.enemyShipSpecial != null
				&& !this.enemyShipSpecial.isDestroyed()) {
			// The bonus ship sits above the formation, so it goes last.
			this.shrinkingEnemies.put(this.enemyShipSpecial, now);
			this.enemyShipSpecial = null;
		} else if (this.shrinkingEnemies.isEmpty()) {
			// Short beat on the empty screen before the banner.
			if (this.gameOverLastEnemyGone == 0)
				this.gameOverLastEnemyGone = now;
			if (now - this.gameOverLastEnemyGone >= GAME_OVER_LAST_ROW_HOLD) {
				this.enemyShipSpecial = null;
				this.showGameOverText = true;
				this.gameOverBannerStart = now;
			}
		}
	}

	/**
	 * Draws the enemies shrinking and fading away on game over.
	 * AUTHORED BY: VFX TEAM (Effection)
	 * Any further inquiries please contact us.
	 */
	private void drawShrinkingEnemies() {
		long now = System.currentTimeMillis();
		for (Map.Entry<EnemyShip, Long> entry
				: this.shrinkingEnemies.entrySet()) {
			double progress = (double) (now - entry.getValue())
					/ GAME_OVER_SHRINK_DURATION;
			drawManager.drawEntityShrunk(entry.getKey(), 1 - progress);
		}
	}

	/**
	 * Gets the time, from the banner start, when the fade to black begins.
	 * AUTHORED BY: VFX TEAM (Effection)
	 * Any further inquiries please contact us.
	 *
	 * @return Milliseconds after the banner starts.
	 */
	private int getGameOverFadeStart() {
		return GAME_OVER_TEXT.length() * GAME_OVER_TYPE_INTERVAL
				+ GAME_OVER_BLINK_COUNT * GAME_OVER_BLINK_INTERVAL * 2
				+ GAME_OVER_TEXT_HOLD;
	}

	/**
	 * Draws the game over banner: typed out letter by letter, then blinking,
	 * then the screen fades to black.
	 * AUTHORED BY: VFX TEAM (Effection)
	 * Any further inquiries please contact us.
	 */
	private void drawGameOverSequence() {
		long elapsed = System.currentTimeMillis() - this.gameOverBannerStart;
		int typeTime = GAME_OVER_TEXT.length() * GAME_OVER_TYPE_INTERVAL;
		int blinkTime = GAME_OVER_BLINK_COUNT * GAME_OVER_BLINK_INTERVAL * 2;

		if (elapsed < typeTime) {
			drawManager.drawGameOverBanner(this, GAME_OVER_TEXT,
					(int) (elapsed / GAME_OVER_TYPE_INTERVAL) + 1);
		} else {
			long blinkElapsed = elapsed - typeTime;
			boolean visible = blinkElapsed >= blinkTime
					|| (blinkElapsed / GAME_OVER_BLINK_INTERVAL) % 2 == 1;
			if (visible)
				drawManager.drawGameOverBanner(this, GAME_OVER_TEXT,
						GAME_OVER_TEXT.length());
		}

		long fadeElapsed = elapsed - getGameOverFadeStart();
		if (fadeElapsed > 0)
			drawManager.drawFadeOverlay(this, (int) (fadeElapsed * 255
					/ GAME_OVER_FADE_DURATION));
	}

	/**
	 * Handles cursor movement and selection while the pause menu is open.
	 */
	private void updatePauseMenu() {
		if (!this.pauseSelectionCooldown.checkFinished())
			return;

		if (inputManager.isKeyDown(KeyEvent.VK_UP)
				|| inputManager.isKeyDown(KeyEvent.VK_W)) {
			this.pauseSelection = this.pauseSelection.previous();
			this.pauseSelectionCooldown.reset();
		} else if (inputManager.isKeyDown(KeyEvent.VK_DOWN)
				|| inputManager.isKeyDown(KeyEvent.VK_S)) {
			this.pauseSelection = this.pauseSelection.next();
			this.pauseSelectionCooldown.reset();
		} else if (inputManager.isKeyDown(KeyEvent.VK_SPACE)) {
			this.pauseSelectionCooldown.reset();
			confirmPauseSelection();
		}
	}

	/**
	 * Carries out whichever pause menu option the cursor was on.
	 */
	private void confirmPauseSelection() {
		switch (this.pauseSelection) {
			case RESUME:
				resumeGameplay();
				break;
			case RESTART:
				restartLevel();
				break;
			case QUIT:
				// Stop the custom final boss soundtrack immediately on quit.
				StoryMusic.stop();
				// No dedicated "abandon" path exists in the screen flow,
				// but ending the run the same way running out of lives
				// does takes us straight to the existing score screen,
				// which already offers "back to menu" / "play again".
				this.lives = 0;
				this.paused = false;
				break;
			default:
				break;
		}
	}

	/** Resumes the level without consuming the boss's warning time. */
	private void resumeGameplay() {
		if (this.paused && this.midBoss != null) {
			this.midBoss.resumeAfterPause(
					System.currentTimeMillis() - this.pauseStartedAt);
		}
		// Pausing doesn't use up the 1-life period (GOG-86).
		if (this.paused && this.vulnerableActive)
			this.vulnerableUntil += System.currentTimeMillis() - this.pauseStartedAt;
		// Pausing doesn't use up a freeze (GOG-84).
		if (this.paused)
			this.ship.resumeAfterPause(
					System.currentTimeMillis() - this.pauseStartedAt);
		// Nor the black hole scene (GOG-74).
		if (this.paused && this.blackHole != null)
			this.blackHole.resumeAfterPause(
					System.currentTimeMillis() - this.pauseStartedAt);
		// Nor the Armor skill shields (GOG-89).
		if (this.paused)
			this.armor.resumeAfterPause(
					System.currentTimeMillis() - this.pauseStartedAt);
		// Nor Pickup Rush (GOG-89).
		if (this.paused)
			this.skillShots.resumeAfterPause(
					System.currentTimeMillis() - this.pauseStartedAt);
		this.paused = false;
		this.pauseStartedAt = 0;
	}

	/**
	 * Restarts the current level: score, lives, bullets shot, ships
	 * destroyed and pending diamonds go back to what they were when the
	 * level began, and every on-screen element is rebuilt from scratch.
	 */
	private void restartLevel() {
		this.score = this.initialScore;
		this.lives = this.initialLives;
		this.vulnerableActive = false; // GOG-86
		this.bulletsShot = this.initialBulletsShot;
		this.shipsDestroyed = this.initialShipsDestroyed;
		this.pendingDiamonds = this.initialPendingDiamonds;
		refundCoinsCollectedThisLevel();
		this.tookDamage = false; // a restarted level can still be flawless
		this.levelFinished = false;
		this.blackHole = null;
		initialize();
		this.paused = false;
	}

	/**
	 * Draws the elements associated with the screen.
	 */
	private void draw() {
		drawManager.initDrawing(this);
		if (this.storyBackground != null) {
			drawManager.drawBackground(this, this.storyBackground);
			this.backgroundEffects.draw(drawManager, this);
		}

		if (!this.paused) {
			// Screen shake: move game world only. AUTHORED BY: VFX TEAM (Effection)
			this.screenShake.update();
			drawManager.setWorldOffset(shakeX(), shakeY());

			// Ship, enemies, bullets and falling coins are hidden while
			// paused, so the pause menu isn't cluttered by the frozen
			// battlefield behind it - only the top HUD stays visible.
			// Warp In draws the ship itself while it beams in. AUTHORED BY: VFX TEAM (Effection)
			boolean warping = this.warpIn != null
					&& this.warpIn.update(this.ship, this.lives > 0);
			// Ghost Trail behind the ship, gone once the level ends (game over or level clear).
			// AUTHORED BY: VFX TEAM (Effection)
			if (this.ghostTrail != null && (this.levelFinished || warping))
				this.ghostTrail.clear();
			else if (this.ghostTrail != null)
				this.ghostTrail.draw(drawManager, this.ship,
						this.skin.colorAt(System.currentTimeMillis()),
						engine.GhostTrail.isBoosting(this.items));
			// End of Act 1 (GOG-74): the black hole sits behind the ship,
			// which shrinks away while it is pulled in.
			long sceneNow = System.currentTimeMillis();
			if (this.blackHole != null)
				drawManager.drawBlackHole(this.blackHole.getCenterX(),
						this.blackHole.getCenterY(),
						this.blackHole.getRadius(sceneNow),
						this.blackHole.getSpin(sceneNow));
			if (this.blackHole != null && this.blackHole.isPulling(sceneNow))
				drawManager.drawEntityShrunk(this.ship,
						this.blackHole.getShipScale(sceneNow));
			else if (!warping)
				drawManager.drawEntity(this.ship, this.ship.getPositionX(),
						this.ship.getPositionY());
			drawManager.drawWarpIn(this.warpIn, this.ship,
					this.skin.colorAt(System.currentTimeMillis()),
					SEPARATION_LINE_HEIGHT);
			if (this.warpIn != null && this.warpIn.takeImpact())
				this.screenShake.trigger();
			drawManager.drawItemShield(this.ship, this.items); // Item System (Team CS)
			if (this.armor.shieldActive(System.currentTimeMillis())) // Armor skill shield (GOG-89)
				drawManager.drawBox(this.ship.getPositionX() - 4,
						this.ship.getPositionY() - 4, this.ship.getWidth() + 8,
						this.ship.getHeight() + 8, new java.awt.Color(120, 220, 255));
			if (this.enemyShipSpecial != null)
				drawManager.drawEntity(this.enemyShipSpecial,
						this.enemyShipSpecial.getPositionX(),
						this.enemyShipSpecial.getPositionY());

			if (this.midBoss != null) {
				if (this.midBoss.isVisible())
					this.midBoss.drawBody(drawManager); // own art, GOG-77
				this.midBoss.drawWarnings(drawManager);
			} else {
				this.enemyShipFormation.draw();
			}

			// Hit sparkles move with the game world.
			drawManager.drawParticles(this, this.particles,
					SEPARATION_LINE_HEIGHT);
			drawManager.drawLightningChain(this.lightningChain);

			for (Bullet bullet : this.bullets) {
				// Comet trail, player bullets only. AUTHORED BY: VFX TEAM (Effection)
				if (this.cometTrailOn && bullet.getSpeed() < 0)
					drawManager.drawBulletTrail(bullet);
				drawManager.drawEntity(bullet, bullet.getPositionX(),
						bullet.getPositionY());
			}
			// Stop shake offset before full-screen dim.
			drawManager.setWorldOffset(0, 0);
			// Damage dim (under HUD, so score/lives stay bright). AUTHORED BY: VFX TEAM (Effection)
			drawManager.drawDamageDim(this, this.damageDim);

			// Coins are part of the game world, so they shake too.
			drawManager.setWorldOffset(shakeX(), shakeY());
			for (Coin coin : this.coins)
				drawManager.drawCoin(coin, coin.getPositionX(),
						coin.getPositionY());
			this.meteors.draw(drawManager); // VFX TEAM (Effection): draws the meteor and its diamonds
			drawManager.drawItemDrops(this.items); // Item System (Team CS)
			// HUD must stay still: remove shake offset.
			drawManager.setWorldOffset(0, 0);
			// Boss full-screen effects, e.g. the claw flash (GOG-77).
			if (this.midBoss != null)
				this.midBoss.drawOverlay(drawManager);
			// Black hole scene ends by fading to black (GOG-74).
			if (this.blackHole != null)
				drawManager.drawFadeOverlay(this,
						this.blackHole.getFadeAlpha(sceneNow));
		}

		// Points popups (above the dim, under the HUD). AUTHORED BY: VFX TEAM (Effection)
		drawManager.drawPointsEffect(this, this.pointsEffect);

		// Interface.
		drawManager.drawScore(this, this.score);
		drawManager.drawLives(this, this.lives);
		// Stinger hit: show how long the player stays at 1 life (GOG-86).
		if (this.vulnerableActive) {
			long left = Math.max(0, this.vulnerableUntil
					- System.currentTimeMillis());
			drawManager.drawCenteredRegularString(this, "VULNERABLE "
							+ ((left + 999) / 1000), SEPARATION_LINE_HEIGHT + 20,
					EnemyType.VULNERABLE_BULLET_COLOR);
		}
		drawManager.drawLevel(this, this.level);
		// Explicit Y (36, not the 25 default): the level indicator now
		// sits centred at y=14, and the coin icon is tall enough that at
		// y=25 it climbs into the level text above it. Dropping it to its
		// own row below clears both.
		drawManager.drawCoinBalance(this, CurrencyManager.getInstance()
				.getCoins(), 36);
		drawManager.drawDiamonds(this, engine.DiamondManager.getInstance()
				.getDiamonds());
		drawManager.drawHorizontalLine(this, SEPARATION_LINE_HEIGHT - 1);
		drawManager.drawItemHud(this, this.items); // Item System (Team CS)
		// Boss health bar: fixed overlay a row below the item HUD slots
		// (y 43-59, Team CS), so the two never collide.
		drawManager.drawBossHealthBar(this, this.midBoss, 64);
		// Blinking boss warning before the boss appears (GOG-75). A boss can
		// give more than one line, split by "\n", centred as a block (GOG-77).
		if (this.midBoss != null && this.midBoss.getWarningText() != null
				&& (System.currentTimeMillis() / 250) % 2 == 0) {
			String[] lines = this.midBoss.getWarningText().split("\n");
			int top = this.height / 2
					- (lines.length - 1) * WARNING_LINE_HEIGHT / 2;
			for (int i = 0; i < lines.length; i++)
				drawManager.drawCenteredRegularString(this, lines[i],
						top + i * WARNING_LINE_HEIGHT, Color.RED);
		}
		// Low-health glitch (covers game + HUD). AUTHORED BY: VFX TEAM (Effection)
		this.glitch.setEnabled(this.lowHealthActive && !this.levelFinished);
		drawManager.drawGlitch(this, this.glitch);

		if (this.paused) {
			String[] options = { PauseOption.RESUME.getLabel(),
					PauseOption.RESTART.getLabel(),
					PauseOption.QUIT.getLabel() };
			drawManager.drawPauseMenu(this, options,
					this.pauseSelection.ordinal());
			drawManager.drawKeyHints(this,
					"w+s / arrows to move, space to select, P to resume");
		}

		// Countdown to game start (skipped while paused, so the pause menu
		// stays visible instead of being painted over).
		if (!this.paused && !this.inputDelay.checkFinished()) {
			int countdown = (int) ((INPUT_DELAY
					- (System.currentTimeMillis()
					- this.gameStartTime)) / 1000);
			drawManager.drawCountDown(this, this.level, countdown,
					this.bonusLife);
			drawManager.drawHorizontalLine(this, this.height / 2 - this.height
					/ 12);
			drawManager.drawHorizontalLine(this, this.height / 2 + this.height
					/ 12);
			drawManager.drawItemHint(this, this.items); // Item System (Team CS)
			drawManager.drawKeyHints(this, EndlessMode.isEnabled()
					? "arrows / WASD fly anywhere, P to pause"
					: "P to pause");
		}


		// Game over animation. AUTHORED BY: VFX TEAM (Effection)
		if (this.shrinkingEnemies != null)
			drawShrinkingEnemies();
		if (this.showGameOverText)
			drawGameOverSequence();

		// Draw the notification after every gameplay and HUD element.
		if (this.unlockedAchievement != null)
			drawManager.drawAchievementUnlocked(this, this.unlockedAchievement,
					System.currentTimeMillis() - this.achievementPopupStartedAt,
					ACHIEVEMENT_POPUP_INTERVAL, ACHIEVEMENT_POPUP_SLIDE_IN,
					ACHIEVEMENT_POPUP_SLIDE_OUT);

		drawManager.completeDrawing(this);
	}

	/**
	 * Scatters sparkles from the center of a destroyed ship.
	 * Must be called BEFORE the ship is destroyed, while its position and
	 * size are still valid.
	 * AUTHORED BY: VFX TEAM (Effection)
	 *
	 * @param ship
	 *            Ship that was destroyed.
	 */
	private void spawnSparkles(final EnemyShip ship) {
		float centerX = ship.getPositionX() + ship.getWidth() / 2f;
		float centerY = ship.getPositionY() + ship.getHeight() / 2f;
		for (int i = 0; i < 14; i++) {
			Color color = SPARKLE_COLORS[(int) (Math.random()
					* SPARKLE_COLORS.length)];
			this.particles.add(new Particle(centerX, centerY, color));
		}
	}

	/**
	 * Moves the sparkles and removes the ones that faded away or left the
	 * play area. Called every frame, outside the levelFinished gate, so
	 * sparkles keep fading after a level ends instead of piling up.
	 * Removal happens after the update loop, never during iteration.
	 * AUTHORED BY: VFX TEAM (Effection)
	 */
	private void updateParticles() {
		for (Particle particle : this.particles)
			particle.update();
		this.particles.removeIf(particle -> particle.isDead()
				|| !particle.isInside(0, SEPARATION_LINE_HEIGHT,
				this.width, this.height));
	}

	/**
	 * Cleans bullets that go off screen.
	 */
	private void cleanBullets() {
		Set<Bullet> recyclable = new HashSet<Bullet>();
		for (Bullet bullet : this.bullets) {
			bullet.update();
			if (bullet.getPositionY() < SEPARATION_LINE_HEIGHT
					|| bullet.getPositionY() > this.height
					|| bullet.getPositionY() > bullet.getMaxY() // short range (GOG-76)
					|| bullet.getPositionX() + bullet.getWidth() < 0
					|| bullet.getPositionX() > this.width)
				recyclable.add(bullet);
		}
		this.bullets.removeAll(recyclable);
		BulletPool.recycle(recyclable);
	}

	/**
	 * Moves the ship up or down in endless free roam (GOG-7). It stays
	 * between FREE_ROAM_TOP and its starting row at the bottom.
	 */
	private void moveFreeRoam() {
		boolean moveUp = inputManager.isKeyDown(KeyEvent.VK_UP)
				|| inputManager.isKeyDown(KeyEvent.VK_W);
		boolean moveDown = inputManager.isKeyDown(KeyEvent.VK_DOWN)
				|| inputManager.isKeyDown(KeyEvent.VK_S);
		int speed = this.ship.getSpeed();
		int y = this.ship.getPositionY();
		if (moveUp && !moveDown)
			this.ship.setPositionY(Math.max(FREE_ROAM_TOP, y - speed));
		if (moveDown && !moveUp)
			this.ship.setPositionY(Math.min(
					this.height - SHIP_ROW_FROM_BOTTOM, y + speed));
	}

	/**
	 * Free roam (GOG-7): flying into an enemy costs a life, like being shot
	 * (a shield blocks it), and sends the ship back to its starting row.
	 * The enemy is wrecked too, but gives no score, coins or drops, so
	 * ramming is not a way to farm rewards.
	 */
	private void manageFreeRoamCrashes() {
		if (!EndlessMode.isEnabled() || this.levelFinished
				|| this.ship.isDestroyed() || this.lives <= 0)
			return;
		EnemyShip rammed = null;
		for (EnemyShip enemyShip : this.enemyShipFormation) {
			if (!enemyShip.isDestroyed()
					&& checkCollision(enemyShip, this.ship)) {
				rammed = enemyShip;
				break;
			}
		}
		if (rammed == null)
			return;

		spawnSparkles(rammed);
		this.enemyShipFormation.destroy(rammed);
		this.screenShake.trigger();
		// Back to the starting row, so the ship does not crash again
		// straight away into the enemies next to the one it hit.
		this.ship.setPositionY(this.height - SHIP_ROW_FROM_BOTTOM);
		if (INVINCIBLE)
			return;
		if (this.armor.tryBlockHit(System.currentTimeMillis())) { // Armor skill nodes (GOG-89)
			this.logger.info("Crashed into an enemy, blocked by Armor.");
			return;
		}
		if (this.items.tryBlockHit()) { // Shield item (Team CS)
			this.logger.info("Crashed into an enemy, blocked by shield.");
			return;
		}
		this.ship.destroy();
		this.lives--;
		this.armor.onLifeLost(this.lives, System.currentTimeMillis()); // GOG-89
		PlayerSounds.playHit(this.lives);
		this.tookDamage = true;
		GameEvents.emit(GameEvents.Type.PLAYER_HIT, this.lives);
		this.logger.info("Crashed into an enemy, " + this.lives
				+ " lives remaining.");
	}

	/** @return Screen shake x, plus the boss's own shake (GOG-77). */
	private int shakeX() {
		java.awt.Point boss = this.midBoss != null
				? this.midBoss.getShakeOffset() : null;
		return this.screenShake.getOffsetX() + (boss != null ? boss.x : 0);
	}

	/** @return Screen shake y, plus the boss's own shake (GOG-77). */
	private int shakeY() {
		java.awt.Point boss = this.midBoss != null
				? this.midBoss.getShakeOffset() : null;
		return this.screenShake.getOffsetY() + (boss != null ? boss.y : 0);
	}

	/**
	 * Boss attacks that aren't bullets (GOG-77: AI Commander laser and Claw
	 * Slam). Each hit takes a life, or only the shield.
	 */
	private void manageBossAttackHits() {
		int hits = this.midBoss.takePlayerHits(this.ship);
		for (int i = 0; i < hits; i++) {
			if (this.levelFinished || this.ship.isDestroyed()
					|| this.lives <= 0)
				return;
			if (INVINCIBLE)
				continue;
			if (this.armor.tryBlockHit(System.currentTimeMillis()) // Armor skill nodes (GOG-89)
					|| this.items.tryBlockHit()) { // Shield item (Team CS)
				this.midBoss.onPlayerHitBlocked();
				this.logger.info("Boss attack blocked by a shield.");
				continue;
			}
			this.ship.destroy();
			this.lives--;
			this.armor.onLifeLost(this.lives, System.currentTimeMillis()); // GOG-89
			PlayerSounds.playHit(this.lives);
			this.tookDamage = true;
			GameEvents.emit(GameEvents.Type.PLAYER_HIT, this.lives);
			this.logger.info("Hit by a boss attack, " + this.lives
					+ " lives remaining.");
		}
	}

	/**
	 * Manages collisions between bullets and ships.
	 */
	private void manageCollisions() {
		Set<Bullet> recyclable = new HashSet<Bullet>();
		// Bullets Hex Sentinel shields send back (GOG-85); added after the
		// loop so the bullet set isn't changed while it is being read.
		Set<Bullet> reflected = new HashSet<Bullet>();
		for (Bullet bullet : this.bullets) {
			if (bullet.getSpeed() > 0) {
				if (!this.levelFinished && !this.ship.isDestroyed()
						&& !checkCollision(bullet, this.ship)
						&& this.skillShots.nearMiss(bullet, this.ship)) {
					this.score += SkillShots.NEAR_MISS_SCORE; // Near Miss (GOG-89)
					this.pointsEffect.spawn(bullet.getPositionX(),
							this.ship.getPositionY(), SkillShots.NEAR_MISS_SCORE);
				}
				if (checkCollision(bullet, this.ship) && !this.levelFinished
						&& !recyclable.contains(bullet)) {
					recyclable.add(bullet);
					if (bullet.isVulnerable()) {
						// Stinger bullet: 1 life left for 5 s (GOG-86).
						startVulnerable();
					} else if (bullet.isFreezing()) {
						// Polar freeze bullet: locks the ship, no life lost (GOG-84).
						if (this.ship.freeze())
							this.logger.info("Player ship frozen by a Polar bullet.");
					} else if (!this.ship.isDestroyed() && !INVINCIBLE
							&& !this.armor.tryBlockHit(System.currentTimeMillis()) // Armor skill nodes (GOG-89)
							&& !this.items.tryBlockHit()) { // Shield item (Team CS)
						this.ship.destroy();
						this.lives--;
						this.armor.onLifeLost(this.lives, System.currentTimeMillis()); // GOG-89
						PlayerSounds.playHit(this.lives); // <-- Ajout du son
						this.tookDamage = true; // Survivor tiers (Mantis)
						GameEvents.emit(GameEvents.Type.PLAYER_HIT, this.lives); // AUTHORED BY: VFX TEAM (Effection)
						this.logger.info("Hit on player ship, " + this.lives
								+ " lives remaining.");
					}
				}
			} else {
				// Stinger bullets can be shot down (GOG-86).
				Bullet shotDown = findVulnerableBulletHit(bullet, recyclable);
				if (shotDown != null) {
					recyclable.add(bullet);
					recyclable.add(shotDown);
					continue;
				}
				// Boss stages are the ones that actually spawned a boss, so
				// endless waves 5 and 10 hit their formation (GOG-7).
				if (this.midBoss != null) {
					// Boss minions (GOG-75 Tempest Queen mobs) are hit first.
					if (hitBossMinion(bullet)) {
						recyclable.add(bullet);
					} else if (!this.midBoss.isDestroyed()
							&& checkCollision(bullet, (Entity) this.midBoss)
							&& this.midBoss.isHitBy(bullet)) { // gaps in the art, GOG-77
						this.score += this.midBoss.manageCollisionsBoss(bullet);
						// Heavy Impact: every 5th boss hit counts twice (GOG-89).
						if (!this.midBoss.isDestroyed()
								&& this.skillShots.heavyImpact())
							this.score += this.midBoss.manageCollisionsBoss(bullet);
						if (this.midBoss.isDestroyed()) {
							this.shipsDestroyed++;
							this.screenShake.trigger();
							showUnlockedAchievement(Core.getAchievementManager()
									.recordBossDefeated());
							showUnlockedAchievement(Core.getAchievementManager()
									.recordEnemyDefeated());
						}
						recyclable.add(bullet);
					}
				} else {
					for (EnemyShip enemyShip : this.enemyShipFormation) {
						if (!enemyShip.isDestroyed()
								&& checkCollision(bullet, enemyShip)) {
							// Comet: the first hit sends it diving at the
							// player instead of killing it (GOG-86).
							if (this.enemyShipFormation.startDive(enemyShip,
									this.ship)) {
								recyclable.add(bullet);
								SoundManager.getInstance().playSfx(Sound.ENEMY_HIT);
								continue;
							}
							// Surviving hits consume the bullet without awarding kill rewards.
							boolean shielded = enemyShip.getShield() > 0;
							if (!enemyShip.takeDamage(1)) {
								recyclable.add(bullet);
								SoundManager.getInstance().playSfx(Sound.ENEMY_HIT);
								// Hex Sentinel: its shield sends the bullet
								// back at the player (GOG-85).
								if (shielded && enemyShip.getType()
										== EnemyType.HEX_SENTINEL)
									this.enemyShipFormation.reflectShot(bullet,
											reflected);
								continue;
							}
							int killPoints = this.items.scoreFor(enemyShip.getPointValue());
							this.score += killPoints;
							// Crossfire: both bullets of a volley kill (GOG-89).
							this.score += this.skillShots.crossfireBonus(bullet, killPoints);
							this.shipsDestroyed++;
							spawnSparkles(enemyShip);
							this.enemyShipFormation.destroy(enemyShip);
							if (this.lightningOn) // Lightning Chain. AUTHORED BY: VFX TEAM (Effection)
								this.lightningChain.onKill(
										enemyShip.getPositionX() + enemyShip.getWidth() / 2,
										enemyShip.getPositionY() + enemyShip.getHeight() / 2,
										aliveEnemyCenters());
							this.screenShake.trigger();
							this.pointsEffect.spawn(
									enemyShip.getPositionX() + enemyShip.getWidth() / 2,
									enemyShip.getPositionY() + enemyShip.getHeight() / 2,
									enemyShip.getPointValue());
							queueScoreBoostPopup(enemyShip, false); // Item System (Team CS)
							maybeDropCoin(enemyShip);
							this.items.onEnemyDefeated(enemyShip, false);
							showUnlockedAchievement(Core.getAchievementManager()
									.recordEnemyDefeated());
							// Piercing skill nodes let the bullet go on (GOG-89).
							if (!this.skillShots.passThrough(bullet))
								recyclable.add(bullet);
						}
					}
				}
				if (this.enemyShipSpecial != null
						&& !this.enemyShipSpecial.isDestroyed()
						&& checkCollision(bullet, this.enemyShipSpecial)) {
					this.score += this.items.scoreFor(this.enemyShipSpecial.getPointValue()); // score boost (Team CS)
					this.shipsDestroyed++;
					spawnSparkles(this.enemyShipSpecial); // AUTHORED BY: VFX TEAM (Effection)
					this.enemyShipSpecial.destroy();
					this.screenShake.trigger();
					// Bonus points popup. AUTHORED BY: VFX TEAM (Effection)
					this.pointsEffect.spawnBonus(
							this.enemyShipSpecial.getPositionX() + this.enemyShipSpecial.getWidth() / 2,
							this.enemyShipSpecial.getPositionY() + this.enemyShipSpecial.getHeight() / 2,
							this.enemyShipSpecial.getPointValue());
					queueScoreBoostPopup(this.enemyShipSpecial, true); // Item System (Team CS)
					dropCoin(this.enemyShipSpecial, BONUS_COIN_VALUE);
					this.items.onEnemyDefeated(this.enemyShipSpecial, true);
					showUnlockedAchievement(Core.getAchievementManager()
							.recordEnemyDefeated());
					this.enemyShipSpecialExplosionCooldown.reset();
					recyclable.add(bullet);
				}
				// Bonus meteor: only player bullets hit it. VFX TEAM (Effection)
				if (!recyclable.contains(bullet) && this.meteors.tryHit(bullet))
					recyclable.add(bullet);
			}
		}
		this.bullets.removeAll(recyclable);
		BulletPool.recycle(recyclable);
		this.bullets.addAll(reflected);
	}

	/**
	 * Checks a player bullet against the boss's minions (GOG-75). A kill
	 * gives the same rewards as a normal enemy; a hit that a shield absorbs
	 * only plays the hit sound.
	 *
	 * @param bullet
	 *            Player bullet.
	 * @return True if the bullet hit a minion (and should be removed).
	 */
	private boolean hitBossMinion(final Bullet bullet) {
		for (EnemyShip minion : this.midBoss.getMinions()) {
			if (minion.isDestroyed() || !checkCollision(bullet, minion))
				continue;
			if (!minion.takeDamage(1)) {
				SoundManager.getInstance().playSfx(Sound.ENEMY_HIT);
				return true;
			}
			minion.destroy();
			rewardKill(minion);
			return true;
		}
		return false;
	}

	/**
	 * Gives the rewards for an enemy the player killed outside the normal
	 * bullet hit: score, sparkles, points popup, coin and item chances,
	 * achievements (boss minions GOG-75, Mite chain kills GOG-86).
	 *
	 * @param enemy
	 *            Enemy that was just destroyed.
	 */
	private void rewardKill(final EnemyShip enemy) {
		this.score += this.items.scoreFor(enemy.getPointValue());
		this.shipsDestroyed++;
		spawnSparkles(enemy);
		this.screenShake.trigger();
		this.pointsEffect.spawn(
				enemy.getPositionX() + enemy.getWidth() / 2,
				enemy.getPositionY() + enemy.getHeight() / 2,
				enemy.getPointValue());
		queueScoreBoostPopup(enemy, false); // Item System (Team CS)
		maybeDropCoin(enemy);
		this.items.onEnemyDefeated(enemy, false);
		showUnlockedAchievement(Core.getAchievementManager()
				.recordEnemyDefeated());
	}

	/**
	 * Finds a Stinger bullet that a player bullet touches (GOG-86).
	 *
	 * @param playerBullet
	 *            Player bullet.
	 * @param recyclable
	 *            Bullets already being removed this frame.
	 * @return The Stinger bullet hit, or null.
	 */
	private Bullet findVulnerableBulletHit(final Bullet playerBullet,
	                                       final Set<Bullet> recyclable) {
		for (Bullet other : this.bullets)
			if (other.isVulnerable() && other.getSpeed() > 0
					&& !recyclable.contains(other)
					&& checkCollision(playerBullet, other))
				return other;
		return null;
	}

	/**
	 * A Comet reached the spot it locked onto and exploded (GOG-86). The
	 * player loses a life if the ship's centre is still within
	 * {@link EnemyType#COMET_BLAST_RADIUS}; a shield blocks it.
	 */
	private void handleCometBlasts() {
		if (this.enemyShipFormation == null)
			return;
		for (java.awt.Point blast : this.enemyShipFormation.takeDetonations()) {
			this.screenShake.trigger();
			if (this.levelFinished || this.ship.isDestroyed()
					|| this.lives <= 0)
				continue;
			double dx = this.ship.getPositionX() + this.ship.getWidth() / 2.0
					- blast.x;
			double dy = this.ship.getPositionY() + this.ship.getHeight() / 2.0
					- blast.y;
			if (Math.sqrt(dx * dx + dy * dy) > EnemyType.COMET_BLAST_RADIUS
					|| INVINCIBLE)
				continue;
			if (this.armor.tryBlockHit(System.currentTimeMillis())) { // Armor skill nodes (GOG-89)
				this.logger.info("Comet blast blocked by Armor.");
				continue;
			}
			if (this.items.tryBlockHit()) { // Shield item (Team CS)
				this.logger.info("Comet blast blocked by shield.");
				continue;
			}
			this.ship.destroy();
			this.lives--;
			this.armor.onLifeLost(this.lives, System.currentTimeMillis()); // GOG-89
			PlayerSounds.playHit(this.lives);
			this.tookDamage = true;
			GameEvents.emit(GameEvents.Type.PLAYER_HIT, this.lives);
			this.logger.info("Caught in a Comet blast, " + this.lives
					+ " lives remaining.");
		}
	}

	/**
	 * A falling Orbit Fighter crashed into the player (GOG-85): the player
	 * loses a life for each one; a shield blocks it.
	 */
	private void handleFighterDrops() {
		if (this.enemyShipFormation == null || this.levelFinished
				|| this.ship.isDestroyed() || this.lives <= 0)
			return;
		int crashes = this.enemyShipFormation.takeDropHits(this.ship);
		for (int i = 0; i < crashes && this.lives > 0
				&& !this.ship.isDestroyed(); i++) {
			this.screenShake.trigger();
			if (INVINCIBLE)
				continue;
			if (this.armor.tryBlockHit(System.currentTimeMillis())) { // Armor skill nodes (GOG-89)
				this.logger.info("Orbit Fighter crash blocked by Armor.");
				continue;
			}
			if (this.items.tryBlockHit()) { // Shield item (Team CS)
				this.logger.info("Orbit Fighter crash blocked by shield.");
				continue;
			}
			this.ship.destroy();
			this.lives--;
			this.armor.onLifeLost(this.lives, System.currentTimeMillis()); // GOG-89
			PlayerSounds.playHit(this.lives);
			this.tookDamage = true;
			GameEvents.emit(GameEvents.Type.PLAYER_HIT, this.lives);
			this.logger.info("Hit by a falling Orbit Fighter, " + this.lives
					+ " lives remaining.");
		}
	}

	/**
	 * Mites that died explode (GOG-86): 3 shrapnel bullets spray downward
	 * and every enemy within {@link EnemyType#MITE_BLAST_RADIUS} takes 1 hit.
	 * Enemies killed that way give normal rewards, and a Mite killed by a
	 * blast explodes too (chain reaction).
	 */
	private void handleMiteBooms() {
		if (this.enemyShipFormation == null)
			return;
		java.util.List<java.awt.Point> booms =
				this.enemyShipFormation.takeMiteBooms();
		while (!booms.isEmpty()) {
			for (java.awt.Point boom : booms) {
				for (int speedX : EnemyType.MITE_SHRAPNEL_SPEED_X) {
					Bullet shrapnel = BulletPool.getBullet(boom.x, boom.y,
							MITE_SHRAPNEL_SPEED);
					shrapnel.setSpeedX(speedX);
					this.bullets.add(shrapnel);
				}
				java.util.List<EnemyShip> hit = new java.util.ArrayList<EnemyShip>();
				for (EnemyShip enemy : this.enemyShipFormation) {
					if (enemy.isDestroyed())
						continue;
					double dx = enemy.getPositionX() + enemy.getWidth() / 2.0
							- boom.x;
					double dy = enemy.getPositionY() + enemy.getHeight() / 2.0
							- boom.y;
					if (Math.sqrt(dx * dx + dy * dy) <= EnemyType.MITE_BLAST_RADIUS)
						hit.add(enemy);
				}
				for (EnemyShip enemy : hit)
					if (!enemy.isDestroyed() && enemy.takeDamage(1)) {
						rewardKill(enemy);
						this.enemyShipFormation.destroy(enemy);
					}
			}
			booms = this.enemyShipFormation.takeMiteBooms();
		}
	}

	/**
	 * A Stinger bullet hit the player (GOG-86): the lives drop to 1 for
	 * {@link EnemyType#VULNERABLE_MILLIS}, then come back. A second Stinger
	 * hit during that time does nothing. It isn't damage, so the shield
	 * item doesn't block it.
	 */
	private void startVulnerable() {
		if (this.vulnerableActive || this.ship.isDestroyed()
				|| this.lives <= 0 || INVINCIBLE)
			return;
		this.vulnerableActive = true;
		this.livesBeforeVulnerable = this.lives;
		this.lives = 1;
		this.vulnerableUntil = System.currentTimeMillis()
				+ EnemyType.VULNERABLE_MILLIS;
		PlayerSounds.playHit(this.lives);
		this.logger.info("Hit by a Stinger bullet: 1 life left for 5 s.");
	}

	/**
	 * @return The player's real lives: while a Stinger holds them at 1, the
	 *         lives held back plus any gained meanwhile; otherwise the
	 *         current lives (GOG-86).
	 */
	private int realLives() {
		if (this.vulnerableActive && this.lives > 0)
			return this.livesBeforeVulnerable + this.lives - 1;
		return this.lives;
	}

	/** Gives the lives back once the 1-life period is over (GOG-86). */
	private void updateVulnerable() {
		if (this.vulnerableActive && !this.paused
				&& System.currentTimeMillis() >= this.vulnerableUntil)
			endVulnerable();
	}

	/**
	 * Ends the 1-life period: the lives held back come back, plus any life
	 * gained meanwhile. Losing the last life during it is a game over, so
	 * nothing comes back then.
	 */
	private void endVulnerable() {
		if (!this.vulnerableActive)
			return;
		this.vulnerableActive = false;
		if (this.lives > 0) {
			this.lives = this.livesBeforeVulnerable + this.lives - 1;
			this.logger.info("Lives back to " + this.lives + ".");
		}
		// vulnerableActive is false now, so realLives() == lives again.
	}

	/**
	 * @return Centers of all enemies that are still alive.
	 *         AUTHORED BY: VFX TEAM (Effection)
	 */
	private java.util.List<java.awt.Point> aliveEnemyCenters() {
		java.util.List<java.awt.Point> centers = new java.util.ArrayList<java.awt.Point>();
		for (EnemyShip e : this.enemyShipFormation)
			if (!e.isDestroyed())
				centers.add(new java.awt.Point(e.getPositionX() + e.getWidth() / 2,
						e.getPositionY() + e.getHeight() / 2));
		return centers;
	}

	/**
	 * Rolls the coin-drop chance for a just-destroyed regular enemy and, if
	 * it succeeds, drops a coin at its position. A coin does not drop on
	 * every kill on purpose: see {@link CoinDropManager} for why.
	 *
	 * @param destroyedEnemy
	 *            Enemy ship that was just destroyed.
	 */
	private void maybeDropCoin(final EnemyShip destroyedEnemy) {
		if (this.coinDropManager.rollForDrop())
			dropCoin(destroyedEnemy, COIN_VALUE);
	}

	/**
	 * Drops a coin worth the given value from the center of a destroyed
	 * enemy. Used directly for the special ship, which always pays out.
	 *
	 * @param destroyedEnemy
	 *            Enemy ship that was just destroyed.
	 * @param value
	 *            Coins awarded when the coin is collected.
	 */
	private void dropCoin(final EnemyShip destroyedEnemy, final int value) {
		this.coins.add(CoinPool.getCoin(
				destroyedEnemy.getPositionX() + destroyedEnemy.getWidth() / 2,
				destroyedEnemy.getPositionY() + destroyedEnemy.getHeight() / 2,
				value));
	}

	/**
	 * Moves falling coins, hands coins touched by the ship to the
	 * CurrencyManager, updates coins collected in the current run, and recycles coins that leave the screen.
	 */
	private void updateCoins() {
		Set<Coin> recyclable = new HashSet<Coin>();
		for (Coin coin : this.coins) {
			coin.update();
			if (this.lives > 0 && !this.ship.isDestroyed()
					&& checkCollision(coin, this.ship)) {
				CurrencyManager.getInstance().addCoins(coin.getValue());
				this.coinsCollectedThisLevel += coin.getValue();
				this.coinsCollected += coin.getValue();
				recyclable.add(coin);
				this.logger.info("Coin collected, balance: "
						+ CurrencyManager.getInstance().getCoins());
			} else if (coin.getPositionY() > this.height) {
				recyclable.add(coin);
			}
		}
		this.coins.removeAll(recyclable);
		CoinPool.recycle(recyclable);
	}

	/**
	 * Undoes this attempt's coin income when the level is restarted, the
	 * same way score and pending diamonds go back to their starting values,
	 * and returns coins still falling to the pool instead of dropping them.
	 * Never takes more than the current balance, so it can't go negative.
	 */
	private void refundCoinsCollectedThisLevel() {
		CurrencyManager wallet = CurrencyManager.getInstance();
		int refund = Math.min(this.coinsCollectedThisLevel, wallet.getCoins());
		if (refund > 0) {
			wallet.trySpend(refund);
			this.logger.info("Level restarted, took back " + refund
					+ " coins, balance: " + wallet.getCoins());
		}
		// The restarted level's coins no longer count toward the run total.
		this.coinsCollected = Math.max(0,
				this.coinsCollected - this.coinsCollectedThisLevel);
		this.coinsCollectedThisLevel = 0;
		CoinPool.recycle(this.coins);
		this.coins.clear();
	}

	/**
	 * Collects every coin still on screen at once, used when the level is
	 * cleared so drops from the last enemies aren't lost.
	 */
	private void collectRemainingCoins() {
		if (this.coins.isEmpty())
			return;
		int total = 0;
		for (Coin coin : this.coins)
			total += coin.getValue();
		CurrencyManager.getInstance().addCoins(total);
		this.coinsCollectedThisLevel += total;
		this.coinsCollected += total;
		this.logger.info("Level cleared, collected " + total
				+ " remaining coins, balance: "
				+ CurrencyManager.getInstance().getCoins());
		CoinPool.recycle(this.coins);
		this.coins.clear();
	}

	/**
	 * Uses an item slot when its number key is pressed (Team CS). Key 1 is
	 * the first slot; holding a key uses the slot only once.
	 */
	private void useItemSlots() {
		int slots = Math.min(this.itemSlotKeysHeld.length,
				this.items.api().getView().slots.size());
		for (int slot = 0; slot < slots; slot++) {
			boolean down = inputManager.isKeyDown(KeyEvent.VK_1 + slot);
			if (down && !this.itemSlotKeysHeld[slot])
				this.items.useSlot(slot);
			this.itemSlotKeysHeld[slot] = down;
		}
	}

	/**
	 * While a score boost is on, the normal points popup shows the base
	 * points and a second popup with the extra points follows a moment
	 * later, a little lower (Team CS). The score itself already includes
	 * the extra points.
	 *
	 * @param enemy
	 *            Enemy that was just destroyed.
	 * @param bonusShip
	 *            Whether it is the bonus ship (uses its own popup style).
	 */
	private void queueScoreBoostPopup(final EnemyShip enemy,
	                                  final boolean bonusShip) {
		int extra = this.items.scoreBonus(enemy.getPointValue());
		if (extra <= 0)
			return;
		this.scoreBoostPopups.add(new long[] {
				System.currentTimeMillis() + SCORE_BOOST_POPUP_DELAY,
				enemy.getPositionX() + enemy.getWidth() / 2,
				enemy.getPositionY() + enemy.getHeight() / 2
						+ SCORE_BOOST_POPUP_OFFSET_Y, extra,
				bonusShip ? 1 : 0 });
	}

	/** Shows the queued score boost popups whose time has come (Team CS). */
	private void showScoreBoostPopups() {
		long now = System.currentTimeMillis();
		Iterator<long[]> iterator = this.scoreBoostPopups.iterator();
		while (iterator.hasNext()) {
			long[] popup = iterator.next();
			if (popup[0] > now)
				continue;
			if (popup[4] == 1)
				this.pointsEffect.spawnBonus((int) popup[1], (int) popup[2],
						(int) popup[3]);
			else
				this.pointsEffect.spawn((int) popup[1], (int) popup[2],
						(int) popup[3]);
			iterator.remove();
		}
	}

	/**
	 * Takes this frame's item events (Team CS) so they don't pile up. The
	 * item system logs them itself; sound and effects can hook in here.
	 */
	private void handleItemEvents() {
		for (ItemEvent event : this.items.drainEvents()) {
			if (event.type == EventType.ITEM_COLLECTED
					|| event.type == EventType.ITEM_USED)
				ItemSounds.play(event.itemId);
			if (event.type == EventType.ITEM_COLLECTED) // Pickup Rush (GOG-89)
				this.skillShots.onItemCollected(System.currentTimeMillis());
		}
	}

	/**
	 * Displays a popup when an enemy defeat unlocks an achievement.
	 *
	 * @param achievement Newly unlocked achievement, if any.
	 */
	private void showUnlockedAchievement(final Achievement achievement) {
		if (achievement != null)
			this.achievementPopupQueue.add(achievement);
	}

	/** Queues every achievement unlocked by one completed level. */
	private void showUnlockedAchievements(
			final List<Achievement> unlockedAchievements) {
		this.achievementPopupQueue.addAll(unlockedAchievements);
	}

	/** Advances the unlock-popup queue without interrupting gameplay. */
	private void updateAchievementPopup() {
		if (this.unlockedAchievement == null) {
			startNextAchievementPopup();
			return;
		}

		if (System.currentTimeMillis() - this.achievementPopupStartedAt
				>= ACHIEVEMENT_POPUP_INTERVAL) {
			this.unlockedAchievement = null;
			startNextAchievementPopup();
		}
	}

	/** Starts the next queued unlock notification, if there is one. */
	private void startNextAchievementPopup() {
		if (!this.achievementPopupQueue.isEmpty()) {
			this.unlockedAchievement = this.achievementPopupQueue.remove();
			this.achievementPopupStartedAt = System.currentTimeMillis();
		}
	}

	/**
	 * Checks if two entities are colliding.
	 *
	 * @param a
	 *            First entity, the bullet.
	 * @param b
	 *            Second entity, the ship.
	 * @return Result of the collision test.
	 */
	private boolean checkCollision(final Entity a, final Entity b) {
		// Calculate center point of the entities in both axis.
		int centerAX = a.getPositionX() + a.getWidth() / 2;
		int centerAY = a.getPositionY() + a.getHeight() / 2;
		int centerBX = b.getPositionX() + b.getWidth() / 2;
		int centerBY = b.getPositionY() + b.getHeight() / 2;
		// Calculate maximum distance without collision.
		int maxDistanceX = a.getWidth() / 2 + b.getWidth() / 2;
		int maxDistanceY = a.getHeight() / 2 + b.getHeight() / 2;
		// Calculates distance.
		int distanceX = Math.abs(centerAX - centerBX);
		int distanceY = Math.abs(centerAY - centerBY);

		return distanceX < maxDistanceX && distanceY < maxDistanceY;
	}

	/**
	 * Returns a GameState object representing the status of the game.
	 *
	 * @return Current game state.
	 */
	public final GameState getGameState() {
		return new GameState(this.level, this.score, realLives(),
				this.bulletsShot, this.shipsDestroyed, this.pendingDiamonds,
				this.levelsCompletedRun, this.coinsCollected);
	}
}
