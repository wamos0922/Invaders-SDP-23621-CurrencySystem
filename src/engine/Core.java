
package engine;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.LayoutManager;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import engine.soundEngine.SoundManager;
import entity.Ship;
import item.ItemSystem;
import screen.*;
import story.NameEntryPanel;

/**
 * Main game loop. Selects a persistent player profile before the title menu.
 * Story Mode and Endless Mode then use that selected profile.
 *
 * IMPORTANT: The profile-folder routing applies to FileManager and
 * LevelRewards. Shop-owned items/equipment/achievements still require
 * separate persistence updates.
 */
public final class Core {
	private static final int WIDTH = 448;
	private static final int HEIGHT = 520;
	private static final int FPS = 60;
	private static final int MAX_LIVES = 3;
	private static final int EXTRA_LIFE_FRECUENCY = 3;
	private static final int NUM_LEVELS = StoryMode.TOTAL_LEVELS;

	public static final double SCALE = readScale();

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
			return Math.max(1.0, Math.min(3.0,
					Double.parseDouble(
							System.getProperty("game.scale", "1.5"))));
		} catch (NumberFormatException ex) {
			return 1.5;
		}
	}

	public static void main(String[] args) {
		try {
			LOGGER.setUseParentHandlers(false);

			fileHandler = new FileHandler("log");
			fileHandler.setFormatter(new MinimalFormatter());

			consoleHandler = new ConsoleHandler();
			consoleHandler.setFormatter(new MinimalFormatter());

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

		// Profile selection is the first menu after launch.
		StorySaveManager.Slot selectedProfile = chooseOrCreateProfile();

		if (selectedProfile == null) {
			shutdownAndClose();
			return;
		}

		try {
			ProfileSession.activate(selectedProfile);
		} catch (IOException ex) {
			showSaveError("Could not activate player profile", ex);
			shutdownAndClose();
			return;
		}

		StoryMode.setPlayerName(selectedProfile.name);

		int startLevel = Integer.getInteger("game.startLevel", 1);

		if (startLevel < 1 || startLevel > NUM_LEVELS) {
			startLevel = 1;
		}

		int returnCode = startLevel == 1
				? 1
				: (EndlessMode.isEnabled() ? 8 : 2);

		do {
			GameState gameState = new GameState(
					startLevel, 0, MAX_LIVES, 0, 0, 0);

			switch (returnCode) {
				case 1:
					currentScreen =
							new TitleScreen(width, height, FPS);

					LOGGER.info(
							"Starting title screen for "
									+ selectedProfile.name);

					returnCode = frame.setScreen(currentScreen);
					break;

				case 2:
				case 8:

					// A replay can bypass the title menu's normal selection path.
					// Check stamina before tutorials or other run-start side effects.
					if (!StaminaManager.getInstance().canStartRun()) {
						currentScreen = new TitleScreen(width, height, FPS, true);
						returnCode = frame.setScreen(currentScreen);
						break;
					}
					boolean endless = returnCode == 8;
					EndlessMode.setEnabled(endless);
					// Equip this mode's items for the run (GOG-50).
					ItemSystem.setRunEquipment(ItemLoadout.getInstance()
							.getEquipped(endless ? ItemLoadout.Mode.ENDLESS
									: ItemLoadout.Mode.STORY));

					StorySaveManager.Slot activeSave =
							ProfileSession.getActive();
					boolean staminaBlocked = false;

					LOGGER.info(endless
							? "Starting Endless Mode"
							: "Starting Story Mode");

					if (!endless && startLevel == 1) {
						if (activeSave == null) {
							returnCode = 1;
							break;
						}

						StoryMode.setPlayerName(activeSave.name);

						// Repair old incomplete Level 41 checkpoints.
						if (activeSave.level == NUM_LEVELS + 1
								&& !activeSave.completed) {
							try {
								activeSave =
										repairOldLevel41Save(activeSave);

								ProfileSession.updateCheckpoint(
										activeSave);
							} catch (IOException ex) {
								showSaveError(
										"Could not repair the Level 41 checkpoint",
										ex);

								returnCode = 1;
								break;
							}
						}

						if (activeSave.completed) {
							JOptionPane.showMessageDialog(
									frame,
									"This story is complete. "
											+ "You can still play Endless Mode.");

							returnCode = 1;
							break;
						}

						if (activeSave.level < 1
								|| activeSave.level > NUM_LEVELS) {
							JOptionPane.showMessageDialog(
									frame,
									"Invalid Story Mode checkpoint: Level "
											+ activeSave.level);

							returnCode = 1;
							break;
						}

						// Show the prologue only at Level 1.
						if (activeSave.level == 1) {
							boolean started = StoryMode.startStory(
									frame, width, height, FPS);

							if (!started) {
								returnCode = 1;
								break;
							}
						}

						gameState = activeSave.toGameState();

						// Bank diamonds preserved in older checkpoints.
						if (gameState.getPendingDiamonds() > 0) {
							GameState settled = bankStoryDiamonds(gameState);

							if (settled.getPendingDiamonds() == 0) {
								try {
									activeSave = StorySaveManager.checkpoint(
											activeSave, settled, false);
									ProfileSession.updateCheckpoint(activeSave);
									gameState = settled;
								} catch (IOException ex) {
									showSaveError(
											"Could not save migrated Story diamonds", ex);
									returnCode = 1;
									break;
								}
							} else {
								showSaveError(
											"Could not bank old Story diamonds",
											new IOException("Please check whether your save folder is writable."));
								returnCode = 1;
								break;
							}
						}

					} else if (endless && startLevel == 1) {
						currentScreen =
								new TutorialScreen(width, height, FPS);

						returnCode = frame.setScreen(currentScreen);

						if (returnCode != 2) {
							break;
						}
					}

					// Endless charges once for the whole run. Story charges at the
					// start of every level attempt, including a retry after game over.
					if (endless && !StaminaManager.getInstance().tryConsumeForRun()) {
						LOGGER.info("Endless run blocked: stamina could not be consumed.");
						currentScreen = new TitleScreen(width, height, FPS, true);
						returnCode = frame.setScreen(currentScreen);
						break;
					}

					boolean exitStoryAfterClear = false;

					do {
						if (!endless) {
							if (!StaminaManager.getInstance().tryConsumeForRun()) {
								LOGGER.info("Story level blocked: not enough stamina.");
								// Do not bank pending diamonds when stamina blocks the next attempt.
								currentScreen = new TitleScreen(width, height, FPS, true);
								returnCode = frame.setScreen(currentScreen);
								staminaBlocked = true;
								break;
							}
						}
						int currentLevel = gameState.getLevel();

						boolean bonusLife =
								currentLevel
										% EXTRA_LIFE_FRECUENCY == 0
										&& gameState.getLivesRemaining()
										< MAX_LIVES;

						GameSettings settings;

						if (endless) {
							settings =
									EndlessMode.getSettings(currentLevel);
						} else {
							settings =
									StoryMode.getSettings(currentLevel);

							LOGGER.info(
									"Story Mode - Chapter "
											+ StoryMode.getChapter(
											currentLevel)
											+ ", Level "
											+ currentLevel);
						}

						currentScreen = new GameScreen(
								gameState,
								settings,
								bonusLife,
								width,
								height,
								FPS);

						frame.setScreen(currentScreen);

						GameState result =
								((GameScreen) currentScreen)
										.getGameState();

						int clearedLevel = result.getLevel();

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
								result.getCoinsCollected());

						if (endless) {
							// Preserve the existing Endless Mode logic.
							if (clearedAlive) {
								gameState =
										EndlessMode.fireLevelCleared(
												gameState,
												clearedLevel);
							}
						} else {
							if (clearedAlive) {
								// Permanently bank Story Mode diamonds.
								int diamondsThisLevel =
										gameState.getPendingDiamonds();

								gameState =
										bankStoryDiamonds(gameState);

								if (gameState.getPendingDiamonds() != 0) {
									showSaveError(
											"Story reward could not be saved",
											new IOException(
													"Could not bank the cleared "
															+ "level's diamonds. "
															+ "Please check your save directory."));

									exitStoryAfterClear = true;
									break;
								}

								// Preserve boss rewards and cutscenes.
								showLevelReward(
										clearedLevel,
										width,
										height);

								StoryMode.onLevelCompleted(
										frame,
										clearedLevel);

								// Save real progression before showing
								// the Continue / Exit screen.
								if (activeSave == null) {
									showSaveError(
											"Missing Story profile",
											new IOException(
													"No active Story checkpoint "
															+ "was selected."));

									exitStoryAfterClear = true;
									break;
								}

								try {
									activeSave =
											StorySaveManager.checkpoint(
													activeSave,
													gameState,
													clearedLevel >= NUM_LEVELS);

									ProfileSession.updateCheckpoint(
											activeSave);
								} catch (IOException ex) {
									showSaveError(
											"Auto-save failed", ex);

									exitStoryAfterClear = true;
									break;
								}

								// Story Mode completion screen.
								currentScreen =
										new StoryLevelCompleteScreen(
												width,
												height,
												FPS,
												clearedLevel,
												diamondsThisLevel);

								frame.setScreen(currentScreen);

								if (((StoryLevelCompleteScreen)
										currentScreen).didExit()) {
									exitStoryAfterClear = true;
									break;
								}
							}

							// Dying or quitting within a level
							// leaves the previous checkpoint intact.
						}

					} while (
							gameState.getLivesRemaining() > 0
									&& (endless
									|| gameState.getLevel() <= NUM_LEVELS));

					if (staminaBlocked) {
						break;
					}

					if (!endless && exitStoryAfterClear) {
						returnCode = 1;
						break;
					}

					// Endless Mode retains its existing diamond settlement.
					gameState = endless
							? settlePendingDiamonds(gameState)
							: withPendingDiamonds(gameState, 0);

					LOGGER.info(
							"Starting score screen with score "
									+ gameState.getScore());

					currentScreen = new ScoreScreen(
							width,
							height,
							FPS,
							gameState,
							endless);

					returnCode = frame.setScreen(currentScreen);

					if (endless && returnCode == 2) {
						returnCode = 8;
					}
					break;

				case 3:
					currentScreen =
							new HighScoreScreen(width, height, FPS);

					returnCode = frame.setScreen(currentScreen);
					break;

				case 4:
					currentScreen =
							new SettingsScreen(width, height, FPS);

					returnCode = frame.setScreen(currentScreen);
					break;

				case 5:
					currentScreen =
							new ShopScreen(width, height, FPS);

					returnCode = frame.setScreen(currentScreen);
					break;

				case 6:
					currentScreen =
							new AchievementsScreen(width, height, FPS);

					returnCode = frame.setScreen(currentScreen);
					break;

				case 7:
					currentScreen =
							new ShipSelectScreen(width, height, FPS);

					returnCode = frame.setScreen(currentScreen);
					break;

				case ChallengeMenuScreen.CODE:
					// Challenges menu and the daily challenge (GOG-102).
					returnCode = runChallenges(width, height);
					break;

				case ItemsScreen.CODE:
					// Items page, opened from Customize (GOG-50).
					currentScreen =
							new ItemsScreen(width, height, FPS);

					returnCode = frame.setScreen(currentScreen);
					break;

				case ConverterScreen.CODE:
					// Shard converter, opened from the skill tree (GOG-52).
					currentScreen =
							new ConverterScreen(width, height, FPS);

					returnCode = frame.setScreen(currentScreen);
					break;

				case SkillTreeScreen.CODE:
					// Skill tree page, opened from Customize (GOG-51).
					currentScreen =
							new SkillTreeScreen(width, height, FPS);

					returnCode = frame.setScreen(currentScreen);
					break;

				default:
					LOGGER.warning(
							"Unknown return code " + returnCode);

					returnCode = 1;
					break;
			}

		} while (returnCode != 0);

		shutdownAndClose();
		System.exit(0);
	}

	private static void shutdownAndClose() {
		if (fileHandler != null) {
			fileHandler.flush();
			fileHandler.close();
			fileHandler = null;
		}

		if (consoleHandler != null) {
			consoleHandler.flush();
			consoleHandler.close();
			consoleHandler = null;
		}

		ProfileSession.resetForShutdown();

		if (frame != null) {
			frame.dispose();
		}
	}

	// =========================================
	// PROFILE SELECTOR / NAME ENTRY
	// =========================================

	private static StorySaveManager.Slot chooseOrCreateProfile() {
		while (true) {
			ProfileMenuPanel.Choice choice =
					chooseProfileMenu();

			if (choice == null) {
				return null;
			}

			if (!choice.newGame) {
				return choice.slot;
			}

			String name = requestProfileName();

			if (name == null) {
				continue;
			}

			try {
				return StorySaveManager.create(name);
			} catch (IOException ex) {
				showSaveError(
						"Could not create profile", ex);
			}
		}
	}

	private static ProfileMenuPanel.Choice chooseProfileMenu() {
		if (SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException(
					"Must run from the game thread");
		}

		CountDownLatch done = new CountDownLatch(1);

		AtomicReference<ProfileMenuPanel.Choice> chosen =
				new AtomicReference<>();

		SwingUtilities.invokeLater(() -> {
			Container content = frame.getContentPane();
			LayoutManager previous = content.getLayout();

			ProfileMenuPanel panel = new ProfileMenuPanel(choice -> {
				chosen.set(choice);

				content.removeAll();
				content.setLayout(previous);
				content.revalidate();
				content.repaint();

				frame.requestFocusInWindow();
				done.countDown();
			});

			content.removeAll();
			content.setLayout(new BorderLayout());
			content.add(panel, BorderLayout.CENTER);
			content.revalidate();
			content.repaint();
			panel.requestFocusInWindow();

			// CI FIX:
			// The profile selection screen now appears before
			// the main title screen. Log successful startup.
			LOGGER.info("Starting profile selection screen.");
		});

		await(done, "Profile selection interrupted");

		return chosen.get();
	}

	private static String requestProfileName() {
		if (SwingUtilities.isEventDispatchThread()) {
			throw new IllegalStateException(
					"Must run from the game thread");
		}

		CountDownLatch done = new CountDownLatch(1);

		AtomicReference<String> chosen =
				new AtomicReference<>();

		SwingUtilities.invokeLater(() -> {
			Container content = frame.getContentPane();
			LayoutManager previous = content.getLayout();

			NameEntryPanel panel = new NameEntryPanel(name -> {
				chosen.set(name);

				content.removeAll();
				content.setLayout(previous);
				content.revalidate();
				content.repaint();

				frame.requestFocusInWindow();
				done.countDown();
			});

			content.removeAll();
			content.setLayout(new BorderLayout());
			content.add(panel, BorderLayout.CENTER);
			content.revalidate();
			content.repaint();
			panel.requestFocusInWindow();
		});

		await(done, "Profile name entry interrupted");

		return chosen.get();
	}

	private static void await(
			CountDownLatch done,
			String message) {

		try {
			done.await();
		} catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(message, ex);
		}
	}

	private static void showSaveError(
			String label,
			IOException ex) {

		LOGGER.warning(
				label + ": " + ex.getMessage());

		try {
			SwingUtilities.invokeAndWait(() ->
					JOptionPane.showMessageDialog(
							frame,
							label + ": " + ex.getMessage(),
							"Profile Save Error",
							JOptionPane.ERROR_MESSAGE));
		} catch (Exception ignored) {
			// Error already logged.
		}
	}

	/**
	 * Repairs old incomplete Level 41 checkpoints.
	 */
	private static StorySaveManager.Slot repairOldLevel41Save(
			StorySaveManager.Slot slot) throws IOException {

		if (slot.cleared < 1
				|| slot.cleared >= NUM_LEVELS) {
			throw new IOException(
					"Cannot infer the correct Story level from this save "
							+ "(cleared=" + slot.cleared + "). "
							+ "Make a backup and edit saves/story/"
							+ slot.id + ".properties manually.");
		}

		Path original = Paths.get(
				"saves",
				"story",
				slot.id + ".properties");

		Path backup = Paths.get(
				"saves",
				"story",
				slot.id + ".before-level41-fix.bak");

		if (!Files.isRegularFile(original)) {
			throw new IOException(
					"Missing save file: " + original);
		}

		if (!Files.exists(backup)) {
			Files.copy(original, backup);
		}

		int resumeLevel = slot.cleared + 1;

		GameState repaired = new GameState(
				resumeLevel,
				slot.score,
				Math.max(1, slot.lives),
				slot.bullets,
				slot.kills,
				slot.diamonds,
				slot.cleared,
				slot.coins);

		StorySaveManager.Slot result =
				StorySaveManager.checkpoint(
						slot,
						repaired,
						false);

		LOGGER.warning(
				"Repaired invalid Level 41 save for "
						+ slot.name
						+ ": resume at Level "
						+ resumeLevel);

		JOptionPane.showMessageDialog(
				frame,
				"An older cash-out bug saved Level 41.\n"
						+ "Your save was repaired to Level "
						+ resumeLevel
						+ ".\n"
						+ "A backup of the original save was created.",
				"Story Save Repaired",
				JOptionPane.INFORMATION_MESSAGE);

		return result;
	}

	/**
	 * Copies GameState while changing pending diamonds.
	 */
	private static GameState withPendingDiamonds(
			GameState state,
			int pending) {

		return new GameState(
				state.getLevel(),
				state.getScore(),
				state.getLivesRemaining(),
				state.getBulletsShot(),
				state.getShipsDestroyed(),
				pending,
				state.getLevelsCompletedRun(),
				state.getCoinsCollected());
	}

	/**
	 * Permanently banks diamonds from cleared Story levels.
	 */
	private static GameState bankStoryDiamonds(
			GameState state) {

		int pending = state.getPendingDiamonds();

		if (pending <= 0) {
			return state;
		}

		DiamondManager manager =
				DiamondManager.getInstance();

		int before = manager.getDiamonds();

		int expected = (int) Math.min(
				Integer.MAX_VALUE,
				(long) before + pending);

		manager.addDiamonds(pending);

		if (manager.getDiamonds() != expected) {
			LOGGER.warning(
					"Could not bank "
							+ pending
							+ " Story diamonds; preserving pending amount.");

			return state;
		}

		LOGGER.info(
				"Story level complete: banked "
						+ pending
						+ " diamonds.");

		return withPendingDiamonds(state, 0);
	}

	private static void showLevelReward(
			int clearedLevel,
			int width,
			int height) {

		LevelRewards.Reward reward =
				LevelRewards.claim(clearedLevel);

		if (reward == null) {
			return;
		}

		currentScreen = new LevelRewardScreen(
				width,
				height,
				FPS,
				clearedLevel,
				reward);

		frame.setScreen(currentScreen);
	}

	/**
	 * Shows the challenges menu and, if the player picks it, plays today's
	 * daily challenge and its results screen (GOG-102). Uses no stamina
	 * and never touches story saves or story rewards.
	 *
	 * @param width
	 *            Screen width.
	 * @param height
	 *            Screen height.
	 * @return Next screen code: the challenges menu again after a run, or
	 *         the title screen.
	 */
	private static int runChallenges(final int width, final int height) {
		currentScreen = new ChallengeMenuScreen(width, height, FPS);
		if (frame.setScreen(currentScreen) != ChallengeMenuScreen.PLAY_DAILY)
			return 1;

		SoundManager.getInstance().stopBgm();
		EndlessMode.setEnabled(false);
		ItemSystem.setRunEquipment(ItemLoadout.getInstance()
				.getEquipped(ItemLoadout.Mode.STORY));

		LocalDate day = LocalDate.now();
		List<DailyChallenge.Stage> stages = DailyChallenge.stagesFor(day);
		LOGGER.info("Starting daily challenge for " + day);

		GameState state = new GameState(stages.get(0).getLevel(), 0,
				MAX_LIVES, 0, 0, 0);
		int stagesCleared = 0;
		DailyChallenge.setRunning(true);
		try {
			for (DailyChallenge.Stage stage : stages) {
				// Challenges pay shards, not story diamonds: pending
				// diamonds are dropped between stages.
				state = new GameState(stage.getLevel(), state.getScore(),
						state.getLivesRemaining(), state.getBulletsShot(),
						state.getShipsDestroyed(), 0,
						state.getLevelsCompletedRun(),
						state.getCoinsCollected());
				currentScreen = new GameScreen(state,
						StoryMode.getSettings(stage.getLevel()), false,
						width, height, FPS);
				frame.setScreen(currentScreen);
				state = ((GameScreen) currentScreen).getGameState();
				if (state.getLivesRemaining() <= 0)
					break;
				stagesCleared++;
			}
		} finally {
			DailyChallenge.setRunning(false);
		}

		boolean cleared = stagesCleared == stages.size();
		String reward = cleared ? DailyChallenge.payReward(day) : null;
		LOGGER.info("Daily challenge " + (cleared ? "cleared" : "failed")
				+ ": " + stagesCleared + "/" + stages.size() + " stages.");

		currentScreen = new ChallengeResultScreen(width, height, FPS,
				cleared, stagesCleared, stages.size(), state.getScore(),
				reward);
		frame.setScreen(currentScreen);
		return ChallengeMenuScreen.CODE;
	}

	/**
	 * Endless Mode's existing diamond settlement.
	 */
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
							+ " diamonds.");
		} else {
			LOGGER.info(
					"Run lost, "
							+ pending
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
					new java.util.HashSet<>();

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
						"Unknown ship type: " + type);
		}
	}

	public static Ship.ShipType getSelectedShipType() {
		return PurchaseManager.getInstance()
				.getSelectedShip();
	}

	public static void setSelectedShipType(
			Ship.ShipType type) {

		if (type != null) {
			PurchaseManager.getInstance()
					.selectShip(type);
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

		return new Cooldown(milliseconds, variance);
	}

	public static void setCurrentShipType(
			Ship.ShipType type) {

		currentShipType = type;
	}

	public static Ship.ShipType getCurrentShipType() {
		return currentShipType;
	}
}