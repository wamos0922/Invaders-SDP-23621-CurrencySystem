package engine;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import entity.Ship;
import screen.AchievementsScreen;
import screen.CashOutScreen;
import screen.GameScreen;
import screen.HighScoreScreen;
import screen.ModeSelectScreen;
import screen.ScoreScreen;
import screen.Screen;
import screen.SettingsScreen;
import screen.ShopScreen;
import screen.ShipSelectScreen;
import screen.TitleScreen;
import screen.TutorialScreen;

/**
 * Implements core game logic.
 * Story Mode contains 40 levels; Endless Mode has unlimited waves.
 * Players choose a mode through the in-game mode selection screen.
 */
public final class Core {

	private static final int WIDTH = 448;
	private static final int HEIGHT = 520;
	public static final double SCALE = readScale();
	private static final int FPS = 60;
	private static final int MAX_LIVES = 3;
	private static final int EXTRA_LIFE_FRECUENCY = 3;
	private static final int NUM_LEVELS = StoryMode.TOTAL_LEVELS;

	private static Frame frame;
	private static Screen currentScreen;
	private static final Logger LOGGER = Logger.getLogger(Core.class.getSimpleName());
	private static Handler fileHandler;
	private static ConsoleHandler consoleHandler;
	private static AchievementManager achievementManager;

	private static double readScale() {
		try {
			double value = Double.parseDouble(System.getProperty("game.scale", "1.5"));
			return Math.max(1.0, Math.min(3.0, value));
		} catch (NumberFormatException e) {
			return 1.5;
		}
	}

	public static void main(final String[] args) {
		try {
			LOGGER.setUseParentHandlers(false);
			fileHandler = new FileHandler("log");
			fileHandler.setFormatter(new MinimalFormatter());
			consoleHandler = new ConsoleHandler();
			consoleHandler.setFormatter(new MinimalFormatter());
			LOGGER.addHandler(fileHandler);
			LOGGER.addHandler(consoleHandler);
			LOGGER.setLevel(Level.ALL);
		} catch (Exception e) {
			e.printStackTrace();
		}

		frame = new Frame(WIDTH, HEIGHT, SCALE);
		DrawManager.getInstance().setFrame(frame);
		int width = frame.getWidth();
		int height = frame.getHeight();

		int startLevel = Integer.getInteger("game.startLevel", 1);
		if (startLevel < 1 || startLevel > NUM_LEVELS) {
			startLevel = 1;
		}

		// Even when testing another level, start at mode selection.
		int returnCode = startLevel == 1 ? 1 : 2;
		GameState gameState;

		do {
			gameState = new GameState(startLevel, 0, MAX_LIVES, 0, 0, 0);

			switch (returnCode) {
				case 1:
					currentScreen = new TitleScreen(width, height, FPS);
					LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
							+ " title screen at " + FPS + " fps.");
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing title screen.");
					break;

				case 2:
					// In-game submenu; no Swing popup and no default mode.
					currentScreen = new ModeSelectScreen(width, height, FPS);
					LOGGER.info("Starting in-game mode selection screen.");
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing mode selection screen.");
					break;

				case ModeSelectScreen.STORY_SELECTED:
				case ModeSelectScreen.ENDLESS_SELECTED:
					// The player explicitly selected one of the two modes.
					boolean endless =
							returnCode == ModeSelectScreen.ENDLESS_SELECTED;
					EndlessMode.setEnabled(endless);
					LOGGER.info("Selected " + (endless ? "Endless" : "Story")
							+ " Mode.");

					gameState = new GameState(startLevel, 0, MAX_LIVES, 0, 0, 0);

					// Preserve the original tutorial before Level 1.
					if (startLevel == 1) {
						currentScreen = new TutorialScreen(width, height, FPS);
						returnCode = frame.setScreen(currentScreen);
						if (returnCode != 2) {
							break;
						}
					}

					// Both modes share the existing GameScreen and enemies.
					do {
						boolean bonusLife = gameState.getLevel()
								% EXTRA_LIFE_FRECUENCY == 0
								&& gameState.getLivesRemaining() < MAX_LIVES;

						GameSettings levelSettings = endless
								? EndlessMode.getSettings(gameState.getLevel())
								: StoryMode.getSettings(gameState.getLevel());

						if (!endless) {
							int level = gameState.getLevel();
							LOGGER.info("Story chapter "
									+ StoryMode.getChapter(level)
									+ ", level " + level);
							if (StoryMode.isBossLevel(level)) {
								// Metadata only; actual boss spawning is
								// implemented separately in GameScreen.
								LOGGER.info("Reserved boss level: " + level);
							}
						}

						currentScreen = new GameScreen(gameState, levelSettings,
								bonusLife, width, height, FPS);
						LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
								+ " game screen at " + FPS + " fps.");
						frame.setScreen(currentScreen);
						LOGGER.info("Closing game screen.");

						gameState = ((GameScreen) currentScreen).getGameState();
						int clearedLevel = gameState.getLevel();
						boolean clearedAlive = gameState.getLivesRemaining() > 0;

						gameState = new GameState(gameState.getLevel() + 1,
								gameState.getScore(),
								gameState.getLivesRemaining(),
								gameState.getBulletsShot(),
								gameState.getShipsDestroyed(),
								gameState.getPendingDiamonds(),
								gameState.getLevelsCompletedRun(),
								gameState.getCoinsCollected());

						if (endless) {
							// Preserve cards, shards, and other level hooks.
							if (clearedAlive) {
								gameState = EndlessMode.fireLevelCleared(
										gameState, clearedLevel);
							}
						} else {
							// Preserve Story Mode currency cash-out.
							gameState = offerCashOut(gameState, width, height);
						}
					} while (gameState.getLivesRemaining() > 0
							&& (endless || gameState.getLevel() <= NUM_LEVELS));

					gameState = settlePendingDiamonds(gameState);

					LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
							+ " score screen at " + FPS + " fps, with a score of "
							+ gameState.getScore() + ", "
							+ gameState.getLivesRemaining() + " lives remaining, "
							+ gameState.getBulletsShot() + " bullets shot, "
							+ gameState.getShipsDestroyed() + " ships destroyed, and "
							+ gameState.getCoinsCollected() + " coins collected.");
					currentScreen = new ScoreScreen(width, height, FPS, gameState);
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing score screen.");
					break;

				case 3:
					currentScreen = new HighScoreScreen(width, height, FPS);
					LOGGER.info("Starting high score screen.");
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing high score screen.");
					break;

				case 4:
					currentScreen = new SettingsScreen(width, height, FPS);
					LOGGER.info("Starting settings screen.");
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing settings screen.");
					break;

				case 5:
					currentScreen = new ShopScreen(width, height, FPS);
					LOGGER.info("Starting shop screen.");
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing shop screen.");
					break;

				case 6:
					currentScreen = new AchievementsScreen(width, height, FPS);
					LOGGER.info("Starting achievements screen.");
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing achievements screen.");
					break;

				case 7:
					currentScreen = new ShipSelectScreen(width, height, FPS);
					LOGGER.info("Starting ship selection screen.");
					returnCode = frame.setScreen(currentScreen);
					LOGGER.info("Closing ship selection screen.");
					break;

				default:
					LOGGER.warning("Unknown return code " + returnCode
							+ ", returning to main menu.");
					returnCode = 1;
					break;
			}
		} while (returnCode != 0);

		if (fileHandler != null) {
			fileHandler.flush();
			fileHandler.close();
		}
		System.exit(0);
	}

	/** Between story levels, lets the player bank or risk pending diamonds. */
	private static GameState offerCashOut(final GameState gameState,
	                                      final int width, final int height) {
		boolean canContinue = gameState.getLivesRemaining() > 0
				&& gameState.getLevel() <= NUM_LEVELS;
		if (!canContinue || gameState.getPendingDiamonds() <= 0) {
			return gameState;
		}

		CashOutScreen cashOutScreen = new CashOutScreen(width, height, FPS,
				gameState);
		currentScreen = cashOutScreen;
		LOGGER.info("Starting " + WIDTH + "x" + HEIGHT
				+ " cash-out screen at " + FPS + " fps.");
		frame.setScreen(currentScreen);
		LOGGER.info("Closing cash-out screen.");

		if (!cashOutScreen.didCashOut()) {
			return gameState;
		}

		// Diamonds were banked by the cash-out screen.
		return new GameState(NUM_LEVELS + 1, gameState.getScore(),
				gameState.getLivesRemaining(), gameState.getBulletsShot(),
				gameState.getShipsDestroyed(), 0,
				gameState.getLevelsCompletedRun(), gameState.getCoinsCollected());
	}

	/** Settle diamonds at the end of a run. */
	private static GameState settlePendingDiamonds(final GameState gameState) {
		int pending = gameState.getPendingDiamonds();
		if (pending <= 0) {
			return gameState;
		}
		if (gameState.getLivesRemaining() > 0) {
			DiamondManager.getInstance().addDiamonds(pending);
			LOGGER.info("Run completed, banked " + pending + " diamonds.");
		} else {
			LOGGER.info("Run lost, " + pending
					+ " pending diamonds forfeited.");
		}
		return new GameState(gameState.getLevel(), gameState.getScore(),
				gameState.getLivesRemaining(), gameState.getBulletsShot(),
				gameState.getShipsDestroyed(), 0,
				gameState.getLevelsCompletedRun(), gameState.getCoinsCollected());
	}

	private Core() { }

	public static Logger getLogger() {
		return LOGGER;
	}

	public static DrawManager getDrawManager() {
		return DrawManager.getInstance();
	}

	public static InputManager getInputManager() {
		return InputManager.getInstance();
	}

	public static FileManager getFileManager() {
		return FileManager.getInstance();
	}

	public static AchievementManager getAchievementManager() {
		if (achievementManager == null) {
			achievementManager = new AchievementManager();
			java.util.Set<String> roster = new java.util.HashSet<String>();
			for (Ship.ShipType type : Ship.ShipType.values()) {
				roster.add(getShipId(type));
			}
			achievementManager.configureLevel10Ships(roster);
		}
		return achievementManager;
	}

	public static String getSelectedShipId() {
		return getShipId(getSelectedShipType());
	}

	public static String getShipId(final Ship.ShipType type) {
		switch (type) {
			case STANDARD:
				return AchievementManager.STARTER_SHIP_ID;
			case FAST_MOVE:
				return "fast_move";
			case TWO_WAY:
				return "two_way";
			case FAST_ATTACK:
				return "fast_attack";
			case BIG_BULLET:
				return "big_bullet";
			default:
				throw new IllegalArgumentException("Unknown ship type: " + type);
		}
	}

	public static Ship.ShipType getSelectedShipType() {
		return PurchaseManager.getInstance().getSelectedShip();
	}

	public static void setSelectedShipType(final Ship.ShipType shipType) {
		if (shipType != null) {
			PurchaseManager.getInstance().selectShip(shipType);
		}
	}

	public static int getNumLevels() {
		return NUM_LEVELS;
	}

	public static Cooldown getCooldown(final int milliseconds) {
		return new Cooldown(milliseconds);
	}

	public static Cooldown getVariableCooldown(final int milliseconds,
	                                           final int variance) {
		return new Cooldown(milliseconds, variance);
	}

	private static entity.Ship.ShipType currentShipType =
			entity.Ship.ShipType.STANDARD;

	public static void setCurrentShipType(final entity.Ship.ShipType shipType) {
		currentShipType = shipType;
	}

	public static entity.Ship.ShipType getCurrentShipType() {
		return currentShipType;
	}
}
