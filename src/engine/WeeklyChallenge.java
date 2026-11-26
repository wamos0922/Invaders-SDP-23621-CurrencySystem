package engine;

import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/**
 * Weekly challenge (GOG-44): a longer run that changes every Sunday at
 * local midnight.
 *
 * <ol>
 * <li>Level 1 of a random story act.</li>
 * <li>Level 2 of a random story act.</li>
 * <li>Level 3 of a random story act.</li>
 * <li>Level 4 of a random story act.</li>
 * <li>A boss from a random act.</li>
 * <li>A boss from a different act.</li>
 * </ol>
 *
 * The picks are seeded by the week (its Sunday), so everyone gets the same
 * challenge that week. It reuses the daily challenge's stages, seeding and
 * running flag ({@link DailyChallenge}); only the stage list, the reset day
 * and the reward hook differ. Shard rewards and the once-per-week rule
 * belong to GOG-46 (Ikhwan), through {@link #setRewardHandler}.
 */
public final class WeeklyChallenge {

	/** Act levels played before the bosses: levels 1 to this. */
	static final int ACT_LEVEL_STAGES = 4;
	/** Boss stages at the end, each from a different act. */
	static final int BOSS_STAGES = 2;
	/**
	 * Lives given back before the first boss, up to the normal maximum:
	 * the bosses are at full strength until GOG-47 nerfs them.
	 */
	public static final int EXTRA_LIVES_BEFORE_BOSSES = 1;
	/** Mixed into the week so weekly and daily picks differ. */
	private static final long SEED_SALT = 0x776B6C79L;

	/** Pays the reward for a cleared weekly challenge (GOG-46). */
	private static RewardHandler rewardHandler;

	/**
	 * Pays the reward when a weekly challenge is cleared. Ikhwan's GOG-46
	 * code implements this: shards into the wallet, once per week.
	 */
	public interface RewardHandler {
		/**
		 * @param weekStart
		 *            Sunday that starts the week that was cleared.
		 * @return Text for the results screen describing what was paid,
		 *         or null if nothing was paid (already claimed this week).
		 */
		String onWeeklyCleared(LocalDate weekStart);
	}

	/** Not used. Static class. */
	private WeeklyChallenge() {
	}

	/**
	 * @param date
	 *            Any day.
	 * @return The Sunday that starts that day's challenge week.
	 */
	public static LocalDate weekStart(final LocalDate date) {
		return date.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
	}

	/**
	 * The stages for the week containing a day. Every day of the same week
	 * gives the same stages, on every computer.
	 *
	 * @param date
	 *            Any day of the week.
	 * @return Stages in play order: act levels 1 to 4, then the bosses.
	 */
	public static List<DailyChallenge.Stage> stagesFor(final LocalDate date) {
		Random random = new Random(DailyChallenge.mix(
				weekStart(date).toEpochDay() + SEED_SALT));
		List<DailyChallenge.Stage> stages = new ArrayList<DailyChallenge.Stage>();

		for (int levelInAct = 1; levelInAct <= ACT_LEVEL_STAGES; levelInAct++) {
			int act = 1 + random.nextInt(DailyChallenge.ACTS);
			stages.add(new DailyChallenge.Stage(
					DailyChallenge.levelOf(act, levelInAct), act, false));
		}

		List<Integer> bossActs = new ArrayList<Integer>();
		for (int act = 1; act <= DailyChallenge.ACTS; act++)
			bossActs.add(act);
		Collections.shuffle(bossActs, random);
		for (int i = 0; i < Math.min(BOSS_STAGES, DailyChallenge.ACTS); i++) {
			int act = bossActs.get(i);
			stages.add(new DailyChallenge.Stage(DailyChallenge.levelOf(act,
					StoryMode.LEVELS_PER_CHAPTER), act, true));
		}
		return stages;
	}

	/**
	 * Lives to start a stage with: one life back before the first boss,
	 * never above the maximum. Other stages keep the lives left.
	 *
	 * @param stages
	 *            The run's stages, in play order.
	 * @param index
	 *            Index of the stage about to start.
	 * @param lives
	 *            Lives left after the previous stage.
	 * @param maxLives
	 *            Most lives the player can have.
	 * @return Lives for the stage.
	 */
	public static int livesForStage(final List<DailyChallenge.Stage> stages,
			final int index, final int lives, final int maxLives) {
		for (int i = 0; i < index; i++)
			if (stages.get(i).isBoss())
				return lives; // Only the first boss gives a life back.
		if (!stages.get(index).isBoss())
			return lives;
		return Math.min(maxLives, lives + EXTRA_LIVES_BEFORE_BOSSES);
	}

	/** @return This week's stages, by the computer's local date. */
	public static List<DailyChallenge.Stage> stagesForThisWeek() {
		return stagesFor(LocalDate.now());
	}

	/**
	 * @param now
	 *            The current local time.
	 * @return Time left until the challenge changes, next Sunday at
	 *         midnight.
	 */
	public static Duration timeUntilReset(final LocalDateTime now) {
		LocalDateTime nextSunday = weekStart(now.toLocalDate()).plusWeeks(1)
				.atStartOfDay();
		return Duration.between(now, nextSunday);
	}

	/**
	 * Sets the code that pays the reward for a cleared week (GOG-46).
	 *
	 * @param handler
	 *            Reward code, or null for none.
	 */
	public static void setRewardHandler(final RewardHandler handler) {
		rewardHandler = handler;
	}

	/**
	 * Pays the reward for clearing the weekly challenge, if reward code is
	 * set up.
	 *
	 * @param date
	 *            A day of the week that was cleared.
	 * @return Text for the results screen.
	 */
	public static String payReward(final LocalDate date) {
		if (rewardHandler == null)
			return "Shard rewards: coming soon";
		String paid = rewardHandler.onWeeklyCleared(weekStart(date));
		return paid == null ? "Reward already claimed this week" : paid;
	}
}
