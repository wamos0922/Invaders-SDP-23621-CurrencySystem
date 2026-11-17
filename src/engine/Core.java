
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
import screen.ScoreScreen;
import screen.Screen;
import screen.SettingsScreen;
import screen.ShopScreen;
import screen.ShipSelectScreen;
import screen.TitleScreen;
import screen.TutorialScreen;

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

	private static final Logger LOGGER =
			Logger.getLogger(Core.class.getSimpleName());

	private static Handler fileHandler;
	private static ConsoleHandler consoleHandler;
	private static AchievementManager achievementManager;

	private static Ship.ShipType currentShipType =
			Ship.ShipType.STANDARD;

	private static double readScale() {
		try {
			double value = Double.parseDouble(
					System.getProperty("game.scale", "1.5"));
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

		// 1 = Main Menu
		// 2 = Story Mode
		// 8 = Endless Mode
		int returnCode = startLevel == 1
				? 1
				: (EndlessMode.isEnabled() ? 8 : 2);

		do {
			GameState gameState = new GameState(
					startLevel, 0, MAX_LIVES, 0, 0, 0);

			switch (returnCode) {

				case 1:
					currentScreen = new TitleScreen(
							width, height, FPS);

					LOGGER.info("Starting title screen.");

					returnCode = frame.setScreen(currentScreen);
					break;

				case 2:
				case 8:

					boolean endless = returnCode == 8;
					EndlessMode.setEnabled(endless);

					LOGGER.info(endless
							? "Starting Endless Mode"
							: "Starting Story Mode");

					if (startLevel == 1) {

						if (endless) {
							// Endless Mode retains its own tutorial.
							currentScreen = new TutorialScreen(
									width, height, FPS);

							returnCode = frame.setScreen(currentScreen);

							if (returnCode != 2) {
								break;
							}

						} else {
							// StoryMode.java controls:
							// Panels 1-5 -> Tutorial -> Panels 6-8.
							boolean started = StoryMode.startStory(
									frame, width, height, FPS);

							if (!started) {
								returnCode = 1;
								break;
							}
						}
					}

					do {
						int currentLevel = gameState.getLevel();

						boolean bonusLife =
								currentLevel
										% EXTRA_LIFE_FRECUENCY == 0
										&& gameState.getLivesRemaining()
										< MAX_LIVES;

						GameSettings levelSettings;

						if (endless) {
							levelSettings =
									EndlessMode.getSettings(currentLevel);
						} else {
							levelSettings =
									StoryMode.getSettings(currentLevel);

							LOGGER.info(
									"Story Mode - Chapter "
											+ StoryMode.getChapter(currentLevel)
											+ ", Level " + currentLevel);
						}

						currentScreen = new GameScreen(
								gameState,
								levelSettings,
								bonusLife,
								width,
								height,
								FPS);

						frame.setScreen(currentScreen);

						gameState =
								((GameScreen) currentScreen)
										.getGameState();

						int clearedLevel = gameState.getLevel();

						boolean clearedAlive =
								gameState.getLivesRemaining() > 0;

						gameState = new GameState(
								gameState.getLevel() + 1,
								gameState.getScore(),
								gameState.getLivesRemaining(),
								gameState.getBulletsShot(),
								gameState.getShipsDestroyed(),
								gameState.getPendingDiamonds(),
								gameState.getLevelsCompletedRun(),
								gameState.getCoinsCollected());

						if (endless) {
							if (clearedAlive) {
								gameState =
										EndlessMode.fireLevelCleared(
												gameState,
												clearedLevel);
							}

						} else {
							// All story panel scheduling is
							// handled inside StoryMode.java.
							if (clearedAlive) {
								StoryMode.onLevelCompleted(
										frame, clearedLevel);
							}

							// Preserve the existing currency system.
							gameState = offerCashOut(
									gameState, width, height);
						}

					} while (
							gameState.getLivesRemaining() > 0
									&& (endless
									|| gameState.getLevel() <= NUM_LEVELS)
					);

					gameState = settlePendingDiamonds(gameState);

					LOGGER.info(
							"Starting score screen with score "
									+ gameState.getScore());

					currentScreen = new ScoreScreen(
							width, height, FPS, gameState);

					returnCode = frame.setScreen(currentScreen);

					if (endless && returnCode == 2) {
						returnCode = 8;
					}
					break;

				case 3:
					currentScreen = new HighScoreScreen(
							width, height, FPS);
					returnCode = frame.setScreen(currentScreen);
					break;

				case 4:
					currentScreen = new SettingsScreen(
							width, height, FPS);
					returnCode = frame.setScreen(currentScreen);
					break;

				case 5:
					currentScreen = new ShopScreen(
							width, height, FPS);
					returnCode = frame.setScreen(currentScreen);
					break;

				case 6:
					currentScreen = new AchievementsScreen(
							width, height, FPS);
					returnCode = frame.setScreen(currentScreen);
					break;

				case 7:
					currentScreen = new ShipSelectScreen(
							width, height, FPS);
					returnCode = frame.setScreen(currentScreen);
					break;

				default:
					LOGGER.warning(
							"Unknown return code " + returnCode);
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

	private static GameState offerCashOut(
			final GameState gameState,
			final int width,
			final int height) {

		boolean canContinue =
				gameState.getLivesRemaining() > 0
						&& gameState.getLevel() <= NUM_LEVELS;

		if (!canContinue
				|| gameState.getPendingDiamonds() <= 0) {
			return gameState;
		}

		CashOutScreen cashOutScreen = new CashOutScreen(
				width, height, FPS, gameState);

		currentScreen = cashOutScreen;
		frame.setScreen(currentScreen);

		if (!cashOutScreen.didCashOut()) {
			return gameState;
		}

		return new GameState(
				NUM_LEVELS + 1,
				gameState.getScore(),
				gameState.getLivesRemaining(),
				gameState.getBulletsShot(),
				gameState.getShipsDestroyed(),
				0,
				gameState.getLevelsCompletedRun(),
				gameState.getCoinsCollected());
	}

	private static GameState settlePendingDiamonds(
			final GameState gameState) {

		int pending = gameState.getPendingDiamonds();

		if (pending <= 0) {
			return gameState;
		}

		if (gameState.getLivesRemaining() > 0) {
			DiamondManager.getInstance().addDiamonds(pending);

			LOGGER.info(
					"Run completed, banked "
							+ pending + " diamonds.");
		} else {
			LOGGER.info(
					"Run lost, " + pending
							+ " pending diamonds forfeited.");
		}

		return new GameState(
				gameState.getLevel(),
				gameState.getScore(),
				gameState.getLivesRemaining(),
				gameState.getBulletsShot(),
				gameState.getShipsDestroyed(),
				0,
				gameState.getLevelsCompletedRun(),
				gameState.getCoinsCollected());
	}

	private Core() {
	}

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

			java.util.Set<String> roster =
					new java.util.HashSet<String>();

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
				throw new IllegalArgumentException(
						"Unknown ship type: " + type);
		}
	}

	public static Ship.ShipType getSelectedShipType() {
		return PurchaseManager.getInstance()
				.getSelectedShip();
	}

	public static void setSelectedShipType(
			final Ship.ShipType shipType) {
		if (shipType != null) {
			PurchaseManager.getInstance()
					.selectShip(shipType);
		}
	}

	public static int getNumLevels() {
		return NUM_LEVELS;
	}

	public static Cooldown getCooldown(
			final int milliseconds) {
		return new Cooldown(milliseconds);
	}

	public static Cooldown getVariableCooldown(
			final int milliseconds,
			final int variance) {
		return new Cooldown(milliseconds, variance);
	}

	public static void setCurrentShipType(
			final Ship.ShipType shipType) {
		currentShipType = shipType;
	}

	public static Ship.ShipType getCurrentShipType() {
		return currentShipType;
	}
}
