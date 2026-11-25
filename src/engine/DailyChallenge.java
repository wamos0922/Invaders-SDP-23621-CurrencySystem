package engine;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Daily challenge (GOG-102, design in GOG-45): a short run that changes
 * every day at local midnight.
 *
 * <ol>
 * <li>A level-4 wave from a random story act.</li>
 * <li>A boss from a random story act (BOSS_STAGES of them, each from a
 * different act).</li>
 * </ol>
 *
 * The picks are seeded by the date, so everyone gets the same challenge on
 * the same day. Waves are played as their story level numbers, so the
 * normal story enemies, bosses and backgrounds are used. Story saves,
 * stamina and story rewards are not touched.
 *
 * Shard rewards and the once-per-day rule belong to GOG-46 (Ikhwan): they
 * plug in through {@link #setRewardHandler(RewardHandler)}. Nerfed bosses
 * belong to GOG-47 (Nikmar); until then the bosses are at normal strength.
 */
public final class DailyChallenge {

	/** Number of story acts the waves can come from. */
	public static final int ACTS = StoryMode.TOTAL_LEVELS
			/ StoryMode.LEVELS_PER_CHAPTER;
	/** Which level of an act the first wave uses. */
	static final int WAVE_LEVEL_IN_ACT = 4;
	/**
	 * Boss stages at the end of the run, each from a different act. The
	 * GOG-45 design has 1; GOG-32 mentions a mid and a final boss, so this
	 * can become 2 without other changes.
	 */
	static final int BOSS_STAGES = 1;
	/** Mixed into the date so the daily and weekly picks differ. */
	private static final long SEED_SALT = 0x6F67L;

	/** True while a daily challenge run is being played. */
	private static boolean running;
	/** Pays the reward for a cleared challenge (GOG-46). */
	private static RewardHandler rewardHandler;

	/** One stage of the challenge. */
	public static final class Stage {
		/** Story level number this stage is played as. */
		private final int level;
		/** Story act the stage comes from, 1 to ACTS. */
		private final int act;
		/** True for a boss stage. */
		private final boolean boss;

		Stage(final int level, final int act, final boolean boss) {
			this.level = level;
			this.act = act;
			this.boss = boss;
		}

		/** @return Story level number this stage is played as. */
		public int getLevel() {
			return this.level;
		}

		/** @return Story act the stage comes from, 1 to ACTS. */
		public int getAct() {
			return this.act;
		}

		/** @return True for a boss stage. */
		public boolean isBoss() {
			return this.boss;
		}

		/** @return Short description for menus, e.g. "Act 2 boss". */
		public String describe() {
			return this.boss ? "Act " + this.act + " boss"
					: "Act " + this.act + " level " + WAVE_LEVEL_IN_ACT;
		}
	}

	/**
	 * Pays the reward when a daily challenge is cleared. Ikhwan's GOG-46
	 * code implements this: shards into the wallet, once per day.
	 */
	public interface RewardHandler {
		/**
		 * @param date
		 *            Day of the challenge that was cleared.
		 * @return Text for the results screen describing what was paid,
		 *         e.g. "+10 Common shards", or null if nothing was paid
		 *         (already claimed today).
		 */
		String onDailyCleared(LocalDate date);
	}

	/** Not used. Static class. */
	private DailyChallenge() {
	}

	/**
	 * The stages for one day. Same date, same stages, on every computer.
	 *
	 * @param date
	 *            The day.
	 * @return Stages in play order: the act wave, then the boss stage(s).
	 */
	public static List<Stage> stagesFor(final LocalDate date) {
		Random random = new Random(mix(date.toEpochDay() + SEED_SALT));
		List<Stage> stages = new ArrayList<Stage>();

		int waveAct = 1 + random.nextInt(ACTS);
		stages.add(new Stage(levelOf(waveAct, WAVE_LEVEL_IN_ACT), waveAct,
				false));

		List<Integer> bossActs = new ArrayList<Integer>();
		for (int act = 1; act <= ACTS; act++)
			bossActs.add(act);
		Collections.shuffle(bossActs, random);
		for (int i = 0; i < Math.min(BOSS_STAGES, ACTS); i++) {
			int act = bossActs.get(i);
			stages.add(new Stage(levelOf(act, StoryMode.LEVELS_PER_CHAPTER),
					act, true));
		}
		return stages;
	}

	/** @return Today's stages, by the computer's local date. */
	public static List<Stage> stagesForToday() {
		return stagesFor(LocalDate.now());
	}

	/**
	 * @param now
	 *            The current local time.
	 * @return Time left until the challenge changes at the next midnight.
	 */
	public static Duration timeUntilReset(final LocalDateTime now) {
		LocalDateTime midnight = now.toLocalDate().plusDays(1).atStartOfDay();
		return Duration.between(now, midnight);
	}

	/**
	 * @param duration
	 *            A duration.
	 * @return The duration as HH:MM:SS.
	 */
	public static String formatCountdown(final Duration duration) {
		long seconds = Math.max(0, duration.getSeconds());
		return String.format("%02d:%02d:%02d", seconds / 3600,
				(seconds / 60) % 60, seconds % 60);
	}

	/** @return True while a daily challenge run is being played. */
	public static boolean isRunning() {
		return running;
	}

	/**
	 * Marks a daily challenge run as started or finished. Core calls this
	 * around the run, so GameScreen can skip story-only achievements.
	 *
	 * @param value
	 *            True when the run starts, false when it ends.
	 */
	public static void setRunning(final boolean value) {
		running = value;
	}

	/**
	 * Sets the code that pays the reward for a cleared challenge (GOG-46).
	 *
	 * @param handler
	 *            Reward code, or null for none.
	 */
	public static void setRewardHandler(final RewardHandler handler) {
		rewardHandler = handler;
	}

	/**
	 * Pays the reward for clearing the challenge, if reward code is set up.
	 *
	 * @param date
	 *            Day of the challenge that was cleared.
	 * @return Text for the results screen.
	 */
	public static String payReward(final LocalDate date) {
		if (rewardHandler == null)
			return "Shard rewards: coming soon";
		String paid = rewardHandler.onDailyCleared(date);
		return paid == null ? "Reward already claimed today" : paid;
	}

	/**
	 * Scrambles a seed (SplitMix64). Without this, neighbouring days give
	 * java.util.Random almost the same first numbers, so the same act
	 * came up several days in a row.
	 *
	 * @param seed
	 *            Raw seed, e.g. the day number.
	 * @return Well-mixed seed.
	 */
	static long mix(final long seed) {
		long z = seed + 0x9E3779B97F4A7C15L;
		z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
		z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
		return z ^ (z >>> 31);
	}

	/**
	 * @param act
	 *            Story act, 1 to ACTS.
	 * @param levelInAct
	 *            Level inside the act, 1 to LEVELS_PER_CHAPTER.
	 * @return The story level number.
	 */
	private static int levelOf(final int act, final int levelInAct) {
		return (act - 1) * StoryMode.LEVELS_PER_CHAPTER + levelInAct;
	}
}
