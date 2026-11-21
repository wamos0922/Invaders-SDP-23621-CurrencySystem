import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Files;

import engine.CurrencyManager;
import engine.DiamondManager;
import engine.LevelRewards;

/**
 * Standalone checks for level completion rewards (GOG-38). Uses a temporary
 * save folder, so the player's real save is never touched.
 */
public final class LevelRewardsTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("level-rewards-test").toFile();
        System.setProperty("game.saveDirectory", dir.getAbsolutePath());

        // Which levels pay what.
        int paying = 0;
        for (int level = -1; level <= 42; level++) {
            LevelRewards.Reward r = LevelRewards.rewardFor(level);
            if (r != null) paying++;
            if (level == 5 || level == 15 || level == 25 || level == 35)
                check(r != null && r.getCoins() == 10 && r.getDiamonds() == 0,
                        "mid-boss reward wrong at " + level);
            else if (level == 10 || level == 20 || level == 30)
                check(r != null && r.getCoins() == 20 && r.getDiamonds() == 1,
                        "planet reward wrong at " + level);
            else if (level == 40)
                check(r != null && r.getCoins() == 50 && r.getDiamonds() == 3,
                        "final reward wrong");
            else
                check(r == null, "level " + level + " should pay nothing");
        }
        check(paying == 8, "expected 8 paying levels, got " + paying);

        // Claiming pays once, banks straight to the balances.
        int coins0 = CurrencyManager.getInstance().getCoins();
        int diamonds0 = DiamondManager.getInstance().getDiamonds();
        check(LevelRewards.claim(3) == null, "non-boss level paid");
        LevelRewards.Reward paid = LevelRewards.claim(10);
        check(paid != null, "level 10 did not pay");
        check(CurrencyManager.getInstance().getCoins() == coins0 + 20,
                "coins not banked");
        check(DiamondManager.getInstance().getDiamonds() == diamonds0 + 1,
                "diamond not banked");
        check(LevelRewards.claim(10) == null, "level 10 paid twice");
        check(CurrencyManager.getInstance().getCoins() == coins0 + 20,
                "second claim changed coins");

        // The claim survives a restart (re-read from the save file).
        Method reload = LevelRewards.class.getDeclaredMethod("reloadForTests");
        reload.setAccessible(true);
        reload.invoke(null);
        check(LevelRewards.isClaimed(10), "claim not saved to disk");
        check(!LevelRewards.isClaimed(5), "unclaimed level marked claimed");
        check(LevelRewards.claim(10) == null, "paid again after restart");
        check(new File(dir, "level-rewards").isFile(), "save file missing");

        System.out.println("LevelRewardsTest: " + checks + " checks passed");
    }
}
