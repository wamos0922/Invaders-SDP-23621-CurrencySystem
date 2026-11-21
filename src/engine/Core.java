
package engine;

import java.io.IOException;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.LayoutManager;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import entity.Ship;
import screen.*;

/**
 * Main game loop.
 * Story Mode supports independent save files.
 * High-score recording is reserved for Endless Mode.
 */
public final class Core {

	private static final int WIDTH = 448;
	private static final int HEIGHT = 520;
	private static final int FPS = 60;

	public static final double SCALE = readScale();

	private static final int MAX_LIVES = 3;
	private static final int EXTRA_LIFE_FRECUENCY = 3;
	private static final int NUM_LEVELS =
			StoryMode.TOTAL_LEVELS;

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
			return Math.max(
					1.0,
					Math.min(
							3.0,
							Double.parseDouble(
									System.getProperty(
											"game.scale", "1.5"
									)
							)
					)
			);
		} catch (NumberFormatException ex) {
			return 1.5;
		}
	}

	public static void main(String[] args) {

		try {
			LOGGER.setUseParentHandlers(false);

			fileHandler = new FileHandler("log");
			fileHandler.setFormatter(
					new MinimalFormatter()
			);

			consoleHandler = new ConsoleHandler();
			consoleHandler.setFormatter(
					new MinimalFormatter()
			);

			LOGGER.addHandler(fileHandler);
			LOGGER.addHandler(consoleHandler);
			LOGGER.setLevel(Level.ALL);

		} catch (Exception ex) {
			ex.printStackTrace();
		}

		frame = new Frame(WIDTH, HEIGHT, SCALE);

		DrawManager.getInstance().setFrame(frame);

		int width = frame.getWidth();
		int height = frame.getHeight();

		int startLevel =
				Integer.getInteger("game.startLevel", 1);

		if (startLevel < 1 || startLevel > NUM_LEVELS) {
			startLevel = 1;
		}

		int returnCode =
				startLevel == 1
						? 1
						: (EndlessMode.isEnabled() ? 8 : 2);

		do {
			GameState gameState = new GameState(
					startLevel,
					0,
					MAX_LIVES,
					0,
					0,
					0
			);

			switch (returnCode) {

				case 1:
					currentScreen = new TitleScreen(
							width, height, FPS
					);

					LOGGER.info(
							"Starting title screen."
					);

					returnCode =
							frame.setScreen(currentScreen);
					break;

				case 2:
				case 8:

					boolean endless = returnCode == 8;

					EndlessMode.setEnabled(endless);

					StorySaveManager.Slot activeSave = null;

					LOGGER.info(
							endless
									? "Starting Endless Mode"
									: "Starting Story Mode"
					);

					if (!endless && startLevel == 1) {

						StoryMenuPanel.Choice choice =
								chooseStoryMode();

						if (choice == null) {
							returnCode = 1;
							break;
						}

						if (choice.newGame) {

							boolean started =
									StoryMode.startStory(
											frame,
											width,
											height,
											FPS
									);

							if (!started) {
								returnCode = 1;
								break;
							}

							try {
								activeSave =
										StorySaveManager.create(
												StoryMode.getPlayerName()
										);

							} catch (IOException ex) {
								showSaveError(
										"Could not create a save file",
										ex
								);

								returnCode = 1;
								break;
							}

						} else {

							activeSave = choice.slot;

							if (activeSave.completed) {
								JOptionPane.showMessageDialog(
										frame,
										"This story is already completed."
								);

								returnCode = 1;
								break;
							}

							StoryMode.setPlayerName(
									activeSave.name
							);

							gameState =
									activeSave.toGameState();
						}

					} else if (endless && startLevel == 1) {

						currentScreen = new TutorialScreen(
								width, height, FPS
						);

						returnCode =
								frame.setScreen(currentScreen);

						if (returnCode != 2) {
							break;
						}
					}

					// Charge only after Story save selection or the Endless tutorial
					// has completed, so backing out before gameplay costs nothing.
					// This remains the backend guard for direct/non-menu entry paths.
					if (!StaminaManager.getInstance().tryConsumeForRun()) {
						LOGGER.info("Run blocked: not enough stamina.");
						returnCode = 1;
						break;
					}

					do {
						int currentLevel =
								gameState.getLevel();

						boolean bonusLife =
								currentLevel
										% EXTRA_LIFE_FRECUENCY == 0
										&& gameState.getLivesRemaining()
										< MAX_LIVES;

						GameSettings levelSettings;

						if (endless) {
							levelSettings =
									EndlessMode.getSettings(
											currentLevel
									);

						} else {
							levelSettings =
									StoryMode.getSettings(
											currentLevel
									);

							LOGGER.info(
									"Story Mode - Chapter "
											+ StoryMode.getChapter(
											currentLevel
									)
											+ ", Level "
											+ currentLevel
							);
						}

						currentScreen = new GameScreen(
								gameState,
								levelSettings,
								bonusLife,
								width,
								height,
								FPS
						);

						frame.setScreen(currentScreen);

						GameState result =
								((GameScreen) currentScreen)
										.getGameState();

						int clearedLevel =
								result.getLevel();

						boolean clearedAlive =
								result.getLivesRemaining() > 0;

						gameState = new GameState(
								result.getLevel() + 1,
								result.getScore(),
								result.getLivesRemaining(),
								result.getBulletsShot(),
								result.getShipsDestroyed(),
								result.getPendingDiamonds(),
								result.getLevelsCompletedRun(),
								result.getCoinsCollected()
						);

						if (endless) {

							if (clearedAlive) {
								gameState =
										EndlessMode.fireLevelCleared(
												gameState,
												clearedLevel
										);
							}

						} else {

							if (clearedAlive) {
								// GOG-38: first-clear bonus for boss
								// levels, shown before the story panels.
								showLevelReward(clearedLevel,
										width, height);
								StoryMode.onLevelCompleted(
										frame,
										clearedLevel
								);
							}

							gameState = offerCashOut(
									gameState,
									width,
									height
							);

							// Story checkpoints are only
							// updated after clearing a level.
							if (clearedAlive
									&& activeSave != null) {

								try {
									activeSave =
											StorySaveManager.checkpoint(
													activeSave,
													gameState,
													clearedLevel >= NUM_LEVELS
											);

								} catch (IOException ex) {
									showSaveError(
											"Auto-save failed",
											ex
									);
								}
							}
						}

					} while (
							gameState.getLivesRemaining() > 0
									&& (endless
									|| gameState.getLevel()
									<= NUM_LEVELS)
					);

					gameState =
							settlePendingDiamonds(gameState);

					LOGGER.info(
							"Starting score screen with score "
									+ gameState.getScore()
					);

					// =====================================
					// HIGH SCORES: ENDLESS MODE ONLY
					// =====================================
					// true  = Endless Mode: leaderboard on
					// false = Story Mode: leaderboard off
					currentScreen = new ScoreScreen(
							width,
							height,
							FPS,
							gameState,
							endless
					);

					returnCode =
							frame.setScreen(currentScreen);

					if (endless && returnCode == 2) {
						returnCode = 8;
					}

					break;

				case 3:
					currentScreen = new HighScoreScreen(
							width, height, FPS
					);

					returnCode =
							frame.setScreen(currentScreen);
					break;

				case 4:
					currentScreen = new SettingsScreen(
							width, height, FPS
					);

					returnCode =
							frame.setScreen(currentScreen);
					break;

				case 5:
					currentScreen = new ShopScreen(
							width, height, FPS
					);

					returnCode =
							frame.setScreen(currentScreen);
					break;

				case 6:
					currentScreen = new AchievementsScreen(
							width, height, FPS
					);

					returnCode =
							frame.setScreen(currentScreen);
					break;

				case 7:
					currentScreen = new ShipSelectScreen(
							width, height, FPS
					);

					returnCode =
							frame.setScreen(currentScreen);
					break;

				default:
					LOGGER.warning(
							"Unknown return code "
									+ returnCode
					);

					returnCode = 1;
			}

		} while (returnCode != 0);

		if (fileHandler != null) {
			fileHandler.flush();
			fileHandler.close();
		}

		System.exit(0);
	}

	/**
	 * Displays the Story Mode save-selection submenu
	 * inside the existing game window.
	 */
	private static StoryMenuPanel.Choice chooseStoryMode() {

		if (SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException(
					"Must run from game thread"
			);
		}

		CountDownLatch done =
				new CountDownLatch(1);

		AtomicReference<StoryMenuPanel.Choice> selected =
				new AtomicReference<>();

		SwingUtilities.invokeLater(() -> {

			Container content =
					frame.getContentPane();

			LayoutManager oldLayout =
					content.getLayout();

			StoryMenuPanel panel =
					new StoryMenuPanel(choice -> {

						selected.set(choice);

						content.removeAll();
						content.setLayout(oldLayout);

						content.revalidate();
						content.repaint();

						frame.requestFocusInWindow();
						done.countDown();
					});

			content.removeAll();
			content.setLayout(new BorderLayout());

			content.add(
					panel,
					BorderLayout.CENTER
			);

			content.revalidate();
			content.repaint();

			panel.requestFocusInWindow();
		});

		try {
			done.await();

		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}

		return selected.get();
	}

	private static void showSaveError(
			String label,
			IOException ex) {

		LOGGER.warning(
				label + ": " + ex.getMessage()
		);

		try {
			SwingUtilities.invokeAndWait(
					() -> JOptionPane.showMessageDialog(
							frame,
							label + ": " + ex.getMessage(),
							"Story Save Error",
							JOptionPane.ERROR_MESSAGE
					)
			);

		} catch (Exception ignored) {
			// Error already logged.
		}
	}

	private static GameState offerCashOut(
			GameState gameState,
			int width,
			int height) {

		boolean canContinue =
				gameState.getLivesRemaining() > 0
						&& gameState.getLevel() <= NUM_LEVELS;

		if (!canContinue
				|| gameState.getPendingDiamonds() <= 0) {
			return gameState;
		}

		CashOutScreen cashOutScreen =
				new CashOutScreen(
						width,
						height,
						FPS,
						gameState
				);

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
				gameState.getCoinsCollected()
		);
	}

	/**
	 * Pays a story level's first-clear bonus, if it has one, and shows it
	 * (GOG-38). Does nothing for other levels or levels already claimed.
	 *
	 * @param clearedLevel
	 *            Story level that was just cleared alive.
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 */
	private static void showLevelReward(final int clearedLevel,
			final int width, final int height) {
		LevelRewards.Reward reward = LevelRewards.claim(clearedLevel);
		if (reward == null)
			return;
		currentScreen = new LevelRewardScreen(width, height, FPS,
				clearedLevel, reward);
		frame.setScreen(currentScreen);
	}

	private static GameState settlePendingDiamonds(
			GameState gameState) {

		int pending =
				gameState.getPendingDiamonds();

		if (pending <= 0) {
			return gameState;
		}

		if (gameState.getLivesRemaining() > 0) {

			DiamondManager.getInstance()
					.addDiamonds(pending);

			LOGGER.info(
					"Run completed, banked "
							+ pending
							+ " diamonds."
			);

		} else {
			LOGGER.info(
					"Run lost, "
							+ pending
							+ " pending diamonds forfeited."
			);
		}

		return new GameState(
				gameState.getLevel(),
				gameState.getScore(),
				gameState.getLivesRemaining(),
				gameState.getBulletsShot(),
				gameState.getShipsDestroyed(),
				0,
				gameState.getLevelsCompletedRun(),
				gameState.getCoinsCollected()
		);
	}

	private Core() {}

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

			achievementManager =
					new AchievementManager();

			java.util.Set<String> roster =
					new java.util.HashSet<>();

			for (Ship.ShipType type
					: Ship.ShipType.values()) {

				roster.add(getShipId(type));
			}

			achievementManager.configureLevel10Ships(
					roster
			);
		}

		return achievementManager;
	}

	public static String getSelectedShipId() {
		return getShipId(getSelectedShipType());
	}

	public static String getShipId(
			Ship.ShipType type) {

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
						"Unknown ship type: " + type
				);
		}
	}

	public static Ship.ShipType getSelectedShipType() {
		return PurchaseManager.getInstance()
				.getSelectedShip();
	}

	public static void setSelectedShipType(
			Ship.ShipType shipType) {

		if (shipType != null) {
			PurchaseManager.getInstance()
					.selectShip(shipType);
		}
	}

	public static int getNumLevels() {
		return NUM_LEVELS;
	}

	public static Cooldown getCooldown(
			int milliseconds) {

		return new Cooldown(milliseconds);
	}

	public static Cooldown getVariableCooldown(
			int milliseconds,
			int variance) {

		return new Cooldown(
				milliseconds,
				variance
		);
	}

	public static void setCurrentShipType(
			Ship.ShipType shipType) {

		currentShipType = shipType;
	}

	public static Ship.ShipType getCurrentShipType() {
		return currentShipType;
	}
}
