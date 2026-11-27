import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import engine.DailyChallenge;

/** Standalone checks for the daily challenge (GOG-102). */
public final class DailyChallengeTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static String key(List<DailyChallenge.Stage> stages) {
        StringBuilder sb = new StringBuilder();
        for (DailyChallenge.Stage s : stages) sb.append(s.getLevel()).append(',');
        return sb.toString();
    }

    public static void main(String[] args) {
        LocalDate start = LocalDate.of(2026, 11, 1);
        Set<String> different = new HashSet<String>();
        Set<Integer> waveActs = new HashSet<Integer>();
        Set<Integer> bossActs = new HashSet<Integer>();
        int[] waveActCount = new int[5];
        int sameAsYesterday = 0;
        int previousAct = -1;

        for (int day = 0; day < 365; day++) {
            LocalDate date = start.plusDays(day);
            List<DailyChallenge.Stage> stages = DailyChallenge.stagesFor(date);

            // Same date always gives the same challenge.
            check(key(stages).equals(key(DailyChallenge.stagesFor(date))),
                    "not repeatable on " + date);
            different.add(key(stages));

            // Shape: one level-4 wave, then the boss stage(s).
            check(stages.size() >= 2, "too few stages on " + date);
            DailyChallenge.Stage wave = stages.get(0);
            check(!wave.isBoss(), "first stage is a boss on " + date);
            check(wave.getLevel() % 10 == 4, "wave is not a level 4 on " + date);
            check(wave.getLevel() == (wave.getAct() - 1) * 10 + 4, "wave level/act mismatch");
            waveActs.add(wave.getAct());
            waveActCount[wave.getAct()]++;
            if (wave.getAct() == previousAct) sameAsYesterday++;
            previousAct = wave.getAct();

            Set<Integer> actsThisDay = new HashSet<Integer>();
            for (int i = 1; i < stages.size(); i++) {
                DailyChallenge.Stage boss = stages.get(i);
                check(boss.isBoss(), "later stage is not a boss on " + date);
                check(boss.getLevel() == boss.getAct() * 10, "boss is not a boss level");
                check(boss.getLevel() >= 10 && boss.getLevel() <= 40, "boss level out of range");
                check(actsThisDay.add(boss.getAct()), "two bosses from one act on " + date);
                bossActs.add(boss.getAct());
            }
        }
        check(different.size() > 4, "challenge barely changes between days");
        // Over a year each act should lead roughly a quarter of the days,
        // and the same act should not keep repeating day after day.
        for (int act = 1; act <= 4; act++)
            check(waveActCount[act] > 60, "act " + act + " too rare: " + waveActCount[act]);
        check(sameAsYesterday < 140, "same act repeats too often: " + sameAsYesterday);
        check(waveActs.size() == 4, "not every act appears as a wave: " + waveActs);
        check(bossActs.size() == 4, "not every act appears as a boss: " + bossActs);

        // Reset countdown.
        check(DailyChallenge.timeUntilReset(LocalDateTime.of(2026, 11, 25, 22, 0))
                .equals(Duration.ofHours(2)), "countdown wrong at 22:00");
        check(DailyChallenge.timeUntilReset(LocalDateTime.of(2026, 11, 25, 0, 0))
                .equals(Duration.ofHours(24)), "countdown wrong at midnight");
        check(DailyChallenge.formatCountdown(Duration.ofSeconds(3725)).equals("01:02:05"),
                "format wrong");

        // Reward hook: none set, then set, then already claimed.
        DailyChallenge.setRewardHandler(null);
        check(DailyChallenge.payReward(start).contains("coming soon"), "no-handler text");
        DailyChallenge.setRewardHandler(d -> "+10 Common shards");
        check(DailyChallenge.payReward(start).equals("+10 Common shards"), "handler not used");
        DailyChallenge.setRewardHandler(d -> null);
        check(DailyChallenge.payReward(start).contains("already claimed"), "claimed text");
        DailyChallenge.setRewardHandler(null);

        System.out.println("DailyChallengeTest: " + checks + " checks passed");
    }
}
