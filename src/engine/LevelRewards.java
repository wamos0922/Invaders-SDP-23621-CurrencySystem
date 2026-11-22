package engine;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.TreeSet;
import java.util.logging.Logger;

/**
 * Level completion rewards (GOG-38): a one-time bonus for clearing a story
 * boss level for the first time.
 *
 * <ul>
 * <li>Mid-boss levels (5, 15, 25, 35): coins.</li>
 * <li>Planet boss levels (10, 20, 30): coins and a diamond.</li>
 * <li>The final level (40): the biggest bonus.</li>
 * </ul>
 *
 * The bonus is banked straight away through CurrencyManager and
 * DiamondManager. It is never added to the run's pending diamonds, so the
 * cash-out decision works exactly as before. Each level pays out once per
 * player (not once per story save), so replaying a level cannot farm it.
 * Claimed levels are kept in the "level-rewards" file next to the other
 * save files.
 */
public final class LevelRewards {

	/** Coins for clearing a mid-boss level. */
	static final int MID_BOSS_COINS = 10;
	/** Coins for clearing a planet's last level. */
	static final int PLANET_COINS = 20;
	/** Diamonds for clearing a planet's last level. */
	static final int PLANET_DIAMONDS = 1;
	/** Coins for clearing the final level. */
	static final int FINAL_COINS = 50;
	/** Diamonds for clearing the final level. */
	static final int FINAL_DIAMONDS = 3;
	/** Levels per planet. */
	private static final int LEVELS_PER_PLANET = 10;
	/** Save file listing the levels whose reward was already claimed. */
	private static final String FILE_NAME = "level-rewards";

	/** Application logger. */
	private static final Logger LOGGER = Logger.getLogger(Core.class
			.getSimpleName());
	/** Levels already claimed; loaded from disk on first use. */
	private static TreeSet<Integer> claimed;

	/** A bonus for one cleared level. */
	public static final class Reward {
		/** Heading shown on the reward screen. */
		private final String title;
		/** Bonus coins. */
		private final int coins;
		/** Bonus diamonds. */
		private final int diamonds;

		Reward(final String title, final int coins, final int diamonds) {
			this.title = title;
			this.coins = coins;
			this.diamonds = diamonds;
		}

		/** @return Heading shown on the reward screen. */
		public String getTitle() {
			return this.title;
		}

		/** @return Bonus coins. */
		public int getCoins() {
			return this.coins;
		}

		/** @return Bonus diamonds. */
		public int getDiamonds() {
			return this.diamonds;
		}
	}

	/** Not used. Static class. */
	private LevelRewards() {
	}

	/**
	 * The bonus a story level is worth, whether or not it was claimed.
	 *
	 * @param level
	 *            Story level, 1 to the last level.
	 * @return The bonus, or null if the level gives none.
	 */
	public static Reward rewardFor(final int level) {
		if (level < 1 || level > StoryMode.TOTAL_LEVELS)
			return null;
		if (level == StoryMode.TOTAL_LEVELS)
			return new Reward("Story complete!", FINAL_COINS, FINAL_DIAMONDS);
		if (level % LEVELS_PER_PLANET == 0)
			return new Reward("Planet cleared!", PLANET_COINS,
					PLANET_DIAMONDS);
		if (level % LEVELS_PER_PLANET == LEVELS_PER_PLANET / 2)
			return new Reward("Boss defeated!", MID_BOSS_COINS, 0);
		return null;
	}

	/**
	 * Pays a level's bonus if it has one and it was never claimed before.
	 * Call once when a story level is cleared alive.
	 *
	 * @param level
	 *            Story level that was just cleared.
	 * @return The bonus that was paid, or null if nothing was paid.
	 */
	public static synchronized Reward claim(final int level) {
		Reward reward = rewardFor(level);
		if (reward == null || claimedLevels().contains(level))
			return null;

		claimedLevels().add(level);
		if (!save())
			LOGGER.warning("Could not save level rewards; level " + level
					+ " may pay out again next time.");

		if (reward.getCoins() > 0)
			CurrencyManager.getInstance().addCoins(reward.getCoins());
		if (reward.getDiamonds() > 0)
			DiamondManager.getInstance().addDiamonds(reward.getDiamonds());
		LOGGER.info("Level " + level + " reward: +" + reward.getCoins()
				+ " coins, +" + reward.getDiamonds() + " diamonds.");
		return reward;
	}

	/**
	 * @param level
	 *            Story level.
	 * @return True if that level's bonus was already paid.
	 */
	public static synchronized boolean isClaimed(final int level) {
		return claimedLevels().contains(level);
	}

	/** Forgets the cached claims so the next call reads the file again. */
	static synchronized void reloadForTests() {
		claimed = null;
	}

	/** @return Claimed levels, loaded from disk on first use. */
	private static TreeSet<Integer> claimedLevels() {
		if (claimed == null) {
			claimed = new TreeSet<Integer>();
			try {
				File file = saveFile();
				if (file.isFile()) {
					String text = new String(Files.readAllBytes(
							file.toPath()), StandardCharsets.UTF_8).trim();
					for (String part : text.split(",")) {
						if (!part.trim().isEmpty())
							claimed.add(Integer.parseInt(part.trim()));
					}
				}
			} catch (IOException | NumberFormatException e) {
				LOGGER.warning("Could not read level rewards: "
						+ e.getMessage());
			}
		}
		return claimed;
	}

	/** @return True if the claimed levels were written to disk. */
	private static boolean save() {
		StringBuilder text = new StringBuilder();
		for (int level : Collections.unmodifiableSet(claimedLevels())) {
			if (text.length() > 0)
				text.append(',');
			text.append(level);
		}
		try {
			File file = saveFile();
			File temp = new File(file.getPath() + ".tmp");
			Files.write(temp.toPath(),
					text.toString().getBytes(StandardCharsets.UTF_8));
			Files.move(temp.toPath(), file.toPath(),
					StandardCopyOption.REPLACE_EXISTING);
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * Same folder as the other save files (see FileManager), including the
	 * "game.saveDirectory" override used by tests.
	 *
	 * @return The save file.
	 * @throws IOException
	 *             If the application folder cannot be found.
	 */
	private static File saveFile() throws IOException {
		String saveDirectory = System.getProperty("game.saveDirectory");
		if (saveDirectory != null && !saveDirectory.trim().isEmpty())
			return new File(saveDirectory, FILE_NAME);
		String jarPath = LevelRewards.class.getProtectionDomain()
				.getCodeSource().getLocation().getPath();
		jarPath = URLDecoder.decode(jarPath, "UTF-8");
		return new File(new File(jarPath).getParent(), FILE_NAME);
	}
}
