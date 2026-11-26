import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import engine.DailyChallenge;
import engine.WeeklyChallenge;

/** Standalone checks for the weekly challenge (GOG-44). */
public final class WeeklyChallengeTest {
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
        LocalDate firstSunday = LocalDate.of(2026, 11, 1); // a Sunday
        check(firstSunday.getDayOfWeek() == DayOfWeek.SUNDAY, "test date is not a Sunday");

        Set<String> weeks = new HashSet<String>();
        int[] actCount = new int[5];
        for (int week = 0; week < 104; week++) {
            LocalDate sunday = firstSunday.plusWeeks(week);
            String thisWeek = key(WeeklyChallenge.stagesFor(sunday));

            // Every day Sunday..Saturday gives the same challenge.
            for (int day = 0; day < 7; day++) {
                LocalDate date = sunday.plusDays(day);
                check(WeeklyChallenge.weekStart(date).equals(sunday), "week start wrong for " + date);
                check(key(WeeklyChallenge.stagesFor(date)).equals(thisWeek), "changes within the week on " + date);
            }
            weeks.add(thisWeek);

            // Shape: act levels 1,2,3,4 then two bosses from different acts.
            List<DailyChallenge.Stage> stages = WeeklyChallenge.stagesFor(sunday);
            check(stages.size() == 6, "weekly should have 6 stages, has " + stages.size());
            for (int i = 0; i < 4; i++) {
                DailyChallenge.Stage s = stages.get(i);
                check(!s.isBoss(), "stage " + (i + 1) + " is a boss");
                check(s.getLevel() == (s.getAct() - 1) * 10 + (i + 1), "stage " + (i + 1) + " is not level " + (i + 1) + " of its act");
                check(s.describe().equals("Act " + s.getAct() + " level " + (i + 1)), "describe wrong: " + s.describe());
                actCount[s.getAct()]++;
            }
            DailyChallenge.Stage bossA = stages.get(4), bossB = stages.get(5);
            check(bossA.isBoss() && bossB.isBoss(), "last two stages are not bosses");
            check(bossA.getLevel() == bossA.getAct() * 10 && bossB.getLevel() == bossB.getAct() * 10, "boss not on a boss level");
            check(bossA.getAct() != bossB.getAct(), "both bosses from the same act");
        }
        check(weeks.size() > 50, "weekly barely changes between weeks: " + weeks.size());
        for (int act = 1; act <= 4; act++)
            check(actCount[act] > 50, "act " + act + " too rare: " + actCount[act]);

        // Weekly and daily are not the same pick.
        check(!key(WeeklyChallenge.stagesFor(firstSunday)).startsWith(key(DailyChallenge.stagesFor(firstSunday))),
                "weekly starts exactly like the daily");

        // Reset countdown: next Sunday at midnight.
        LocalDate saturday = firstSunday.plusDays(6);
        check(WeeklyChallenge.timeUntilReset(saturday.atTime(23, 0)).equals(Duration.ofHours(1)), "1h before reset");
        check(WeeklyChallenge.timeUntilReset(firstSunday.atStartOfDay()).equals(Duration.ofDays(7)), "7 days at reset");
        check(WeeklyChallenge.timeUntilReset(LocalDateTime.of(2026, 11, 4, 12, 0)).equals(Duration.ofHours(84)), "Wednesday noon");
        check(DailyChallenge.formatCountdown(Duration.ofHours(84)).equals("3d 12:00:00"), "days format: " + DailyChallenge.formatCountdown(Duration.ofHours(84)));
        check(DailyChallenge.formatCountdown(Duration.ofHours(24)).equals("24:00:00"), "daily midnight format");
        check(DailyChallenge.formatCountdown(Duration.ofSeconds(3725)).equals("01:02:05"), "short format");

        // Reward hook.
        WeeklyChallenge.setRewardHandler(null);
        check(WeeklyChallenge.payReward(saturday).contains("coming soon"), "no-handler text");
        final LocalDate[] seen = {null};
        WeeklyChallenge.setRewardHandler(w -> { seen[0] = w; return "+25 Common shards"; });
        check(WeeklyChallenge.payReward(saturday).equals("+25 Common shards"), "handler not used");
        check(firstSunday.equals(seen[0]), "handler should get the week's Sunday");
        WeeklyChallenge.setRewardHandler(w -> null);
        check(WeeklyChallenge.payReward(saturday).contains("already claimed"), "claimed text");
        WeeklyChallenge.setRewardHandler(null);

        System.out.println("WeeklyChallengeTest: " + checks + " checks passed");
    }
}
