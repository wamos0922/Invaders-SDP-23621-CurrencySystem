package engine;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.URLDecoder;
import java.nio.charset.Charset;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;

import engine.DrawManager.SpriteType;

/**
 * Manages files used in the application.
 *
 * @author <a href="mailto:RobertoIA1987@gmail.com">Roberto Izquierdo Amo</a>
 *
 */
public final class FileManager {

	/** Singleton instance of the class. */
	private static FileManager instance;
	/** Application logger. */
	private static Logger logger;
	/** Max number of high scores. */
	private static final int MAX_SCORES = 7;
	/** Name of the file containing persistent player progress. */
	private static final String PLAYER_PROFILE_FILE = "player-profile";
	/** Name of the file the player's coin balance is persisted to. */
	private static final String COINS_FILE = "coins";
	/** Name of the file the player's diamond balance is persisted to. */
	private static final String DIAMONDS_FILE = "diamonds";
	/** Name of the file containing the player's stamina and regen timestamp. */
	private static final String STAMINA_FILE = "stamina.properties";
	/** Journal that makes stamina refills atomic across both save files. */
	private static final String RESOURCE_TRANSACTION_FILE =
			"stamina-diamond-transaction.properties";
	/** Default shared font, relative to res/ or src/. */
	public static final String DEFAULT_FONT = "silkscreen/Silkscreen-Regular.ttf";
	/** Folder checked for fonts that are not on the classpath. */
	private static final String SOURCE_FONT_DIR = "src";
	/** Original game font in res/, used if the default font is missing. */
	private static final String FALLBACK_FONT = "font.ttf";

	/**
	 * private constructor.
	 */
	private FileManager() {
		logger = Core.getLogger();
	}

	/**
	 * Returns shared instance of FileManager.
	 *
	 * @return Shared instance of FileManager.
	 */
	static void resetInstanceForTests() {
		instance = null;
	}

	protected static FileManager getInstance() {
		if (instance == null)
			instance = new FileManager();
		return instance;
	}

	/**
	 * Loads sprites from disk.
	 *
	 * The graphics file holds the sprites as 0/1 bits, in the same order
	 * they are added to the sprite map in DrawManager. Lines starting with
	 * "//" are comments and are skipped, so each sprite can be labelled and
	 * grouped by team. Comments may contain any text, including digits.
	 *
	 * @param spriteMap
	 *            Mapping of sprite type and empty boolean matrix that will
	 *            contain the image.
	 * @throws IOException
	 *             In case of loading problems, or when the graphics file has
	 *             fewer bits than the registered sprites need.
	 */
	public void loadSprite(final Map<SpriteType, boolean[][]> spriteMap)
			throws IOException {
		InputStream inputStream = null;
		BufferedReader reader = null;
		StringBuilder bits = new StringBuilder();

		try {
			inputStream = DrawManager.class.getClassLoader()
					.getResourceAsStream("graphics");
			if (inputStream == null)
				throw new IOException("Graphics file not found.");
			reader = new BufferedReader(new InputStreamReader(inputStream,
					Charset.forName("UTF-8")));

			// Keep only the bits, skipping comment lines.
			String line;
			while ((line = reader.readLine()) != null) {
				if (line.trim().startsWith("//"))
					continue;
				for (int k = 0; k < line.length(); k++) {
					char c = line.charAt(k);
					if (c == '0' || c == '1')
						bits.append(c);
				}
			}
		} finally {
			if (reader != null)
				reader.close();
			else if (inputStream != null)
				inputStream.close();
		}

		// Sprite loading.
		int position = 0;
		for (Map.Entry<SpriteType, boolean[][]> sprite : spriteMap
				.entrySet()) {
			boolean[][] image = sprite.getValue();
			int needed = image.length * image[0].length;
			if (position + needed > bits.length())
				throw new IOException("Graphics file ran out of data at sprite "
						+ sprite.getKey() + ": needs " + needed + " bits, "
						+ (bits.length() - position) + " left.");
			for (int i = 0; i < image.length; i++)
				for (int j = 0; j < image[i].length; j++)
					image[i][j] = bits.charAt(position++) == '1';
			logger.fine("Sprite " + sprite.getKey() + " loaded.");
		}

		if (position < bits.length())
			logger.warning("Graphics file has " + (bits.length() - position)
					+ " unused bits. A sprite may be missing from DrawManager.");
	}

	/**
	 * Loads the shared game font (Silkscreen by default).
	 * TESTING: run with -Dgame.font=<path> to try another font
	 * without changing code.
	 *
	 * @param size
	 *            Point size of the font.
	 * @return New font.
	 * @throws IOException
	 *             If neither the chosen font nor the fallback can be read.
	 * @throws FontFormatException
	 *             In case of incorrect font format.
	 */
	public Font loadFont(final float size) throws IOException,
			FontFormatException {
		String path = System.getProperty("game.font", DEFAULT_FONT);
		try {
			return loadFont(path, size);
		} catch (IOException e) {
			logger.warning(e.getMessage() + " Using " + FALLBACK_FONT
					+ " instead.");
			return loadFont(FALLBACK_FONT, size);
		}
	}

	/**
	 * Loads a font from the classpath, or from src/ if it is not there.
	 *
	 * @param resourcePath
	 *            Path relative to res/ or src/, for example
	 *            "silkscreen/Silkscreen-Regular.ttf".
	 * @param size
	 *            Font size.
	 * @return Loaded font.
	 * @throws IOException
	 *             If the font cannot be found or read.
	 * @throws FontFormatException
	 *             In case of incorrect font format.
	 */
	public Font loadFont(final String resourcePath, final float size)
			throws IOException, FontFormatException {
		InputStream inputStream = FileManager.class.getClassLoader()
				.getResourceAsStream(resourcePath);
		if (inputStream == null) {
			File file = new File(SOURCE_FONT_DIR, resourcePath);
			if (!file.isFile())
				throw new IOException("Font not found on classpath or at "
						+ file.getPath() + ".");
			inputStream = new FileInputStream(file);
		}
		try (InputStream in = inputStream) {
			return Font.createFont(Font.TRUETYPE_FONT, in).deriveFont(size);
		}
	}

	/**
	 * Returns the application default scores if there is no user high scores
	 * file.
	 *
	 * @return Default high scores.
	 * @throws IOException
	 *             In case of loading problems.
	 */
	private List<Score> loadDefaultHighScores() throws IOException {
		List<Score> highScores = new ArrayList<Score>();
		InputStream inputStream = null;
		BufferedReader reader = null;

		try {
			inputStream = FileManager.class.getClassLoader()
					.getResourceAsStream("scores");
			reader = new BufferedReader(new InputStreamReader(inputStream));

			Score highScore = null;
			String name = reader.readLine();
			String score = reader.readLine();

			while ((name != null) && (score != null)) {
				highScore = new Score(name, Integer.parseInt(score));
				highScores.add(highScore);
				name = reader.readLine();
				score = reader.readLine();
			}
		} finally {
			if (inputStream != null)
				inputStream.close();
		}

		return highScores;
	}

	/**
	 * Loads high scores from file, and returns a sorted list of pairs score -
	 * value.
	 *
	 * @return Sorted list of scores - players.
	 * @throws IOException
	 *             In case of loading problems.
	 */
	public List<Score> loadHighScores() throws IOException {

		List<Score> highScores = new ArrayList<Score>();
		InputStream inputStream = null;
		BufferedReader bufferedReader = null;

		try {
			String jarPath = FileManager.class.getProtectionDomain()
					.getCodeSource().getLocation().getPath();
			jarPath = URLDecoder.decode(jarPath, "UTF-8");

			String scoresPath = new File(jarPath).getParent();
			scoresPath += File.separator;
			scoresPath += "scores";

			File scoresFile = new File(scoresPath);
			inputStream = new FileInputStream(scoresFile);
			bufferedReader = new BufferedReader(new InputStreamReader(
					inputStream, Charset.forName("UTF-8")));

			logger.info("Loading user high scores.");

			Score highScore = null;
			String name = bufferedReader.readLine();
			String score = bufferedReader.readLine();

			while ((name != null) && (score != null)) {
				highScore = new Score(name, Integer.parseInt(score));
				highScores.add(highScore);
				name = bufferedReader.readLine();
				score = bufferedReader.readLine();
			}

		} catch (FileNotFoundException e) {
			// loads default if there's no user scores.
			logger.info("Loading default high scores.");
			highScores = loadDefaultHighScores();
		} finally {
			if (bufferedReader != null)
				bufferedReader.close();
		}

		Collections.sort(highScores);
		return highScores;
	}

	/**
	 * Saves user high scores to disk.
	 *
	 * @param highScores
	 *            High scores to save.
	 * @throws IOException
	 *             In case of loading problems.
	 */
	public void saveHighScores(final List<Score> highScores)
			throws IOException {
		OutputStream outputStream = null;
		BufferedWriter bufferedWriter = null;

		try {
			String jarPath = FileManager.class.getProtectionDomain()
					.getCodeSource().getLocation().getPath();
			jarPath = URLDecoder.decode(jarPath, "UTF-8");

			String scoresPath = new File(jarPath).getParent();
			scoresPath += File.separator;
			scoresPath += "scores";

			File scoresFile = new File(scoresPath);

			if (!scoresFile.exists())
				scoresFile.createNewFile();

			outputStream = new FileOutputStream(scoresFile);
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(
					outputStream, Charset.forName("UTF-8")));

			logger.info("Saving user high scores.");

			// Saves 7 or less scores.
			int savedCount = 0;
			for (Score score : highScores) {
				if (savedCount >= MAX_SCORES)
					break;
				bufferedWriter.write(score.getName());
				bufferedWriter.newLine();
				bufferedWriter.write(Integer.toString(score.getScore()));
				bufferedWriter.newLine();
				savedCount++;
			}

		} finally {
			if (bufferedWriter != null)
				bufferedWriter.close();
		}
	}

	/**
	 * Loads persistent player progress from disk.
	 *
	 * @return Loaded profile, or a new one when no profile has been saved.
	 * @throws IOException In case of loading problems.
	 */
	public PlayerProfile loadPlayerProfile() throws IOException {
		File profileFile = getPlayerProfileFile();
		if (!profileFile.exists())
			return new PlayerProfile();

		InputStream inputStream = null;
		try {
			inputStream = new FileInputStream(profileFile);
			Properties properties = new Properties();
			properties.load(inputStream);

			int totalEnemiesKilled = Integer.parseInt(properties.getProperty(
					"totalEnemiesKilled", "0"));
			int flawlessLevelsCleared = Integer.parseInt(properties.getProperty(
					"flawlessLevelsCleared", "0"));
			int totalItemsUsed = Integer.parseInt(
					properties.getProperty("totalItemsUsed", "0"));
			Set<String> unlockedAchievements = new HashSet<String>();
			String unlocked = properties.getProperty("unlockedAchievements", "");
			if (!unlocked.isEmpty())
				for (String achievementId : unlocked.split(","))
					unlockedAchievements.add(achievementId);

			logger.info("Loading player profile.");
			Set<String> completedShips = new HashSet<String>();
			String completed = properties.getProperty("level10CompletedShips", "");
			if (!completed.isEmpty())
				for (String shipId : completed.split(","))
					completedShips.add(shipId);
			return new PlayerProfile(totalEnemiesKilled, unlockedAchievements,
					completedShips, flawlessLevelsCleared, totalItemsUsed);
		} finally {
			if (inputStream != null)
				inputStream.close();
		}
	}

	/**
	 * Saves persistent player progress to disk.
	 *
	 * @param playerProfile Player progress to save.
	 * @throws IOException In case of saving problems.
	 */
	public void savePlayerProfile(final PlayerProfile playerProfile)
			throws IOException {
		File profileFile = getPlayerProfileFile();
		if (!profileFile.exists())
			profileFile.createNewFile();

		OutputStream outputStream = null;
		try {
			outputStream = new FileOutputStream(profileFile);
			Properties properties = new Properties();
			properties.setProperty("totalEnemiesKilled", Integer.toString(
					playerProfile.getTotalEnemiesKilled()));
			properties.setProperty("flawlessLevelsCleared", Integer.toString(
					playerProfile.getFlawlessLevelsCleared()));
			properties.setProperty("unlockedAchievements", joinAchievementIds(
					playerProfile.getUnlockedAchievements()));
			properties.setProperty("level10CompletedShips", joinAchievementIds(
					playerProfile.getLevel10CompletedShips()));
			properties.setProperty("totalItemsUsed",
					Integer.toString(playerProfile.getTotalItemsUsed()));
			properties.store(outputStream, "Space Invaders player profile");
			logger.info("Saving player profile.");
		} finally {
			if (outputStream != null)
				outputStream.close();
		}
	}

	/**
	 * Returns the file used to persist player progress.
	 *
	 * @return Player profile file.
	 * @throws IOException In case the application directory cannot be decoded.
	 */
	private File getPlayerProfileFile() throws IOException {
		String jarPath = FileManager.class.getProtectionDomain()
				.getCodeSource().getLocation().getPath();
		jarPath = URLDecoder.decode(jarPath, "UTF-8");
		return new File(new File(jarPath).getParent(), PLAYER_PROFILE_FILE);
	}

	/**
	 * Serializes achievement identifiers for the profile file.
	 *
	 * @param achievementIds Achievement identifiers.
	 * @return Comma-separated achievement identifiers.
	 */
	private String joinAchievementIds(final Set<String> achievementIds) {
		StringBuilder joinedIds = new StringBuilder();
		for (String achievementId : achievementIds) {
			if (joinedIds.length() > 0)
				joinedIds.append(',');
			joinedIds.append(achievementId);
		}
		return joinedIds.toString();
	}

	/**
	 * Loads the player's persisted coin balance (GoG - Currency System).
	 * Kept in its own file so it never interferes with the player profile.
	 *
	 * @return Saved coin balance, or 0 if there is no save file yet or it
	 *         cannot be read.
	 */
	public int loadCoins() {
		return loadBalance(COINS_FILE, "coin");
	}

	/**
	 * Saves the player's coin balance to disk (GoG - Currency System).
	 *
	 * @param coins
	 *            Current coin balance to persist.
	 */
	public void saveCoins(final int coins) {
		saveBalance(COINS_FILE, coins, "coin");
	}

	/**
	 * Loads the player's persisted diamond balance (GoG - Currency System).
	 *
	 * @return Saved diamond balance, or 0 if there is no save file yet or
	 *         it cannot be read.
	 */
	public int loadDiamonds() {
		return loadBalance(DIAMONDS_FILE, "diamond");
	}

	/**
	 * Saves the player's diamond balance to disk (GoG - Currency System).
	 *
	 * @param diamonds
	 *            Current diamond balance to persist.
	 */
	public boolean saveDiamonds(final int diamonds) {
		if (!recoverPendingResourceTransaction())
			return false;
		return saveBalance(DIAMONDS_FILE, diamonds, "diamond");
	}

	/**
	 * Loads the player's saved stamina value.
	 *
	 * @param defaultValue Value to use when no valid stamina save exists.
	 * @return Saved stamina, or the supplied default.
	 */
	public int loadStamina(final int defaultValue) {
		Properties properties = loadStaminaProperties();
		try {
			return Math.max(0, Integer.parseInt(
					properties.getProperty("currentStamina", "")));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Loads the timestamp used to calculate real-time stamina regeneration.
	 *
	 * @param defaultValue Timestamp to use when no valid save exists.
	 * @return Saved timestamp in milliseconds since the Unix epoch.
	 */
	public long loadStaminaTimestamp(final long defaultValue) {
		Properties properties = loadStaminaProperties();
		try {
			return Long.parseLong(properties.getProperty("lastUpdateTime", ""));
		} catch (NumberFormatException e) {
			return defaultValue;
		}
	}

	/**
	 * Saves stamina and its regeneration timestamp together.
	 *
	 * @param stamina Current stamina value.
	 * @param timestamp Last regeneration reference time in milliseconds.
	 */
	public boolean saveStamina(final int stamina, final long timestamp) {
		if (!recoverPendingResourceTransaction())
			return false;
		Properties properties = new Properties();
		properties.setProperty("currentStamina", Integer.toString(
				Math.max(0, stamina)));
		properties.setProperty("lastUpdateTime", Long.toString(timestamp));
		return saveProperties(STAMINA_FILE, properties, "stamina");
	}

	/**
	 * Commits a stamina refill and its diamond charge as one recoverable
	 * transaction. The journal is written atomically before either balance is
	 * changed, so a crash between the two save-file updates cannot lose the
	 * refill or the diamond charge.
	 *
	 * @return true once the transaction journal is safely persisted.
	 */
	public boolean saveStaminaAndDiamonds(final int stamina,
			final long timestamp, final int diamonds) {
		if (!recoverPendingResourceTransaction())
			return false;
		Properties transaction = new Properties();
		transaction.setProperty("currentStamina", Integer.toString(
				Math.max(0, stamina)));
		transaction.setProperty("lastUpdateTime", Long.toString(timestamp));
		transaction.setProperty("diamonds", Integer.toString(
				Math.max(0, diamonds)));
		if (!saveProperties(RESOURCE_TRANSACTION_FILE, transaction,
				"stamina/diamond transaction"))
			return false;
		// The journal is authoritative from this point. Applying the individual
		// files is best-effort and will be retried on the next load or save.
		if (!applyResourceTransaction(transaction))
			logger.warning("Stamina/diamond transaction is journaled and will be recovered later.");
		return true;
	}

	/** Writes a properties save through a temporary file and atomic replace. */
	private boolean saveProperties(final String fileName,
			final Properties properties, final String label) {
		OutputStream outputStream = null;
		try {
			File saveFile = getSaveFile(fileName);
			File tempFile = getSaveFile(fileName + ".tmp");
			outputStream = new FileOutputStream(tempFile);
			properties.store(outputStream, "Space Invaders " + label);
			outputStream.close();
			outputStream = null;
			try {
				Files.move(tempFile.toPath(), saveFile.toPath(),
						StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tempFile.toPath(), saveFile.toPath(),
						StandardCopyOption.REPLACE_EXISTING);
			}
			return true;
		} catch (IOException e) {
			logger.warning("Couldn't save " + label + ": " + e.getMessage());
			return false;
		} finally {
			if (outputStream != null) {
				try {
					outputStream.close();
				} catch (IOException e) {
					// Preserve the original save result.
				}
			}
		}
	}

	/**
	 * Reads the stamina save file. Missing or corrupt files produce empty
	 * properties so callers can apply safe defaults.
	 *
	 * @return Loaded stamina properties, or an empty set.
	 */
	private Properties loadStaminaProperties() {
		Properties transaction = loadPendingResourceTransaction();
		if (isCompleteResourceTransaction(transaction))
			return transaction;
		Properties properties = new Properties();
		InputStream inputStream = null;
		try {
			File saveFile = getSaveFile(STAMINA_FILE);
			if (!saveFile.isFile())
				return properties;
			inputStream = new FileInputStream(saveFile);
			properties.load(inputStream);
		} catch (IOException e) {
			logger.warning("Couldn't load stamina save: " + e.getMessage());
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// Nothing else to do if closing fails.
				}
			}
		}
		return properties;
	}

	/** Reads the pending refill journal, or null when no journal exists. */
	private Properties loadPendingResourceTransaction() {
		InputStream inputStream = null;
		try {
			File journal = getSaveFile(RESOURCE_TRANSACTION_FILE);
			if (!journal.exists())
				return null;
			if (!journal.isFile())
				return new Properties();
			Properties properties = new Properties();
			inputStream = new FileInputStream(journal);
			properties.load(inputStream);
			return properties;
		} catch (IOException e) {
			logger.warning("Couldn't read stamina/diamond transaction: "
					+ e.getMessage());
			return new Properties();
		} finally {
			if (inputStream != null) {
				try {
					inputStream.close();
				} catch (IOException e) {
					// Nothing else to do.
				}
			}
		}
	}

	private boolean isCompleteResourceTransaction(final Properties transaction) {
		return transaction != null && transaction.containsKey("currentStamina")
				&& transaction.containsKey("lastUpdateTime")
				&& transaction.containsKey("diamonds");
	}

	/** Retries a journaled refill before allowing an independent resource save. */
	private boolean recoverPendingResourceTransaction() {
		Properties transaction = loadPendingResourceTransaction();
		return transaction == null || applyResourceTransaction(transaction);
	}

	/** Materializes a journal into both legacy save files, then removes it. */
	private boolean applyResourceTransaction(final Properties transaction) {
		try {
			int stamina = Integer.parseInt(
					transaction.getProperty("currentStamina", ""));
			long timestamp = Long.parseLong(
					transaction.getProperty("lastUpdateTime", ""));
			int diamonds = Integer.parseInt(
					transaction.getProperty("diamonds", ""));
			Properties staminaProperties = new Properties();
			staminaProperties.setProperty("currentStamina", Integer.toString(
					Math.max(0, stamina)));
			staminaProperties.setProperty("lastUpdateTime", Long.toString(timestamp));
			if (!saveProperties(STAMINA_FILE, staminaProperties, "stamina"))
				return false;
			if (!saveBalance(DIAMONDS_FILE, Math.max(0, diamonds), "diamond"))
				return false;
			Files.deleteIfExists(getSaveFile(RESOURCE_TRANSACTION_FILE).toPath());
			return true;
		} catch (NumberFormatException | IOException e) {
			logger.warning("Couldn't apply stamina/diamond transaction: "
					+ e.getMessage());
			return false;
		}
	}

	/**
	 * Shared implementation of loadCoins() and loadDiamonds(). Never throws:
	 * a missing or corrupt file just means starting from zero, so the game
	 * keeps running.
	 *
	 * @param fileName
	 *            Name of the save file, next to the running jar.
	 * @param label
	 *            Short currency label, used only for log messages.
	 * @return Saved balance, never negative.
	 */
	private int loadBalance(final String fileName, final String label) {
		if (DIAMONDS_FILE.equals(fileName)) {
			Properties transaction = loadPendingResourceTransaction();
			if (isCompleteResourceTransaction(transaction)) {
				try {
					return Math.max(0, Integer.parseInt(
							transaction.getProperty("diamonds")));
				} catch (NumberFormatException e) {
					logger.warning("Stamina/diamond transaction journal is corrupt; using the last saved balance.");
				}
			}
		}
		BufferedReader bufferedReader = null;
		try {
			File balanceFile = getSaveFile(fileName);
			if (!balanceFile.exists()) {
				logger.info("No " + label + " balance saved yet, starting from 0.");
				return 0;
			}
			bufferedReader = new BufferedReader(new InputStreamReader(
					new FileInputStream(balanceFile), Charset.forName("UTF-8")));
			String line = bufferedReader.readLine();
			int balance = line == null ? 0 : Integer.parseInt(line.trim());
			logger.info("Loading user " + label + " balance.");
			return Math.max(0, balance);
		} catch (NumberFormatException e) {
			logger.warning("The " + label + " balance file is corrupt, starting from 0.");
			return 0;
		} catch (IOException e) {
			logger.warning("Couldn't load " + label + " balance, starting from 0.");
			return 0;
		} finally {
			try {
				if (bufferedReader != null)
					bufferedReader.close();
			} catch (IOException e) {
				// Nothing else to do if closing fails.
			}
		}
	}

	/**
	 * Shared implementation of saveCoins() and saveDiamonds(). Writes to a
	 * temporary file first and then replaces the real one, so a crash in the
	 * middle of a save can't leave an empty or half-written balance file.
	 * Never throws: a failed save is logged and the game keeps running.
	 *
	 * @param fileName
	 *            Name of the save file, next to the running jar.
	 * @param balance
	 *            Balance to persist.
	 * @param label
	 *            Short currency label, used only for log messages.
	 */
	private boolean saveBalance(final String fileName, final int balance,
							 final String label) {
		BufferedWriter bufferedWriter = null;
		try {
			File balanceFile = getSaveFile(fileName);
			File tempFile = getSaveFile(fileName + ".tmp");
			bufferedWriter = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(tempFile), Charset.forName("UTF-8")));
			bufferedWriter.write(Integer.toString(balance));
			bufferedWriter.newLine();
			bufferedWriter.close();
			bufferedWriter = null;

			try {
				Files.move(tempFile.toPath(), balanceFile.toPath(),
						StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(tempFile.toPath(), balanceFile.toPath(),
						StandardCopyOption.REPLACE_EXISTING);
			}
			return true;
		} catch (IOException e) {
			logger.warning("Couldn't save " + label + " balance: "
					+ e.getMessage());
			return false;
		} finally {
			try {
				if (bufferedWriter != null)
					bufferedWriter.close();
			} catch (IOException e) {
				// Nothing else to do if closing fails.
			}
		}
	}

	/**
	 * Returns a save file located next to the running jar, the same place
	 * the high scores and player profile are kept.
	 *
	 * @param fileName
	 *            Name of the file.
	 * @return The file.
	 * @throws IOException
	 *             In case the application directory cannot be decoded.
	 */
	private File getSaveFile(final String fileName) throws IOException {
		// Allows isolated persistence tests without touching a player's real save.
		String saveDirectory = System.getProperty("game.saveDirectory");
		if (saveDirectory != null && !saveDirectory.trim().isEmpty())
			return new File(saveDirectory, fileName);
		String jarPath = FileManager.class.getProtectionDomain()
				.getCodeSource().getLocation().getPath();
		jarPath = URLDecoder.decode(jarPath, "UTF-8");
		return new File(new File(jarPath).getParent(), fileName);
	}
}
