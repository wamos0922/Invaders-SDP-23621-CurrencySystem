package engine;

/** Standalone deterministic checks for GOG-11 stamina regeneration. */
public final class StaminaManagerTest {

    private static int checks;

    private static void check(final boolean condition, final String message) {
        checks++;
        if (!condition)
            throw new AssertionError(message);
    }

    public static void main(final String[] args) {
        long interval = StaminaManager.REGEN_INTERVAL_MILLIS;
        long[] result;

        result = StaminaManager.calculateRegeneration(0, 1_000L,
                1_000L + interval - 1L);
        check(result[0] == 0L, "regenerated before a full interval");
        check(result[1] == 1_000L, "partial interval was not preserved");

        result = StaminaManager.calculateRegeneration(0, 1_000L,
                1_000L + interval);
        check(result[0] == 1L, "one interval should regenerate one stamina");
        check(result[1] == 1_000L + interval,
                "timestamp should advance by one complete interval");

        result = StaminaManager.calculateRegeneration(2, 1_000L,
                1_000L + interval * 2L + interval / 2L);
        check(result[0] == 4L, "two elapsed intervals should add two stamina");
        check(result[1] == 1_000L + interval * 2L,
                "remaining half interval should be preserved");

        result = StaminaManager.calculateRegeneration(4, 1_000L,
                1_000L + interval * 3L);
        check(result[0] == StaminaManager.MAX_STAMINA,
                "regeneration should stop at maximum stamina");
        check(result[1] == 1_000L + interval * 3L,
                "full stamina should reset the regeneration reference time");

        result = StaminaManager.calculateRegeneration(5, 1_000L,
                1_000L + interval * 10L);
        check(result[0] == 5L, "full stamina must never exceed maximum");
        check(result[1] == 1_000L,
                "full stamina reads must not continually rewrite the timestamp");

        result = StaminaManager.calculateRegeneration(3, 5_000L, 4_000L);
        check(result[0] == 3L, "clock rollback must not grant stamina");
        check(result[1] == 4_000L,
                "clock rollback should reset the timestamp safely");

        result = StaminaManager.calculateRegeneration(99, 1_000L, 2_000L);
        check(result[0] == StaminaManager.MAX_STAMINA,
                "loaded stamina should be capped at maximum");

        System.out.println("StaminaManagerTest: " + checks + " checks passed");
    }
}
