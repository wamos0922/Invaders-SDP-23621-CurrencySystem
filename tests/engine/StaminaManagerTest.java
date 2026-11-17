package engine;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

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

        check(StaminaManager.calculateSimulatedTimestamp(10_000L, 3_000L)
                == 7_000L, "tester time simulation should subtract elapsed time");
        check(StaminaManager.calculateSimulatedTimestamp(1_000L, 2_000L)
                == 0L, "tester time simulation should not create negative timestamps");
        boolean rejectedNegativeElapsed = false;
        try {
            StaminaManager.calculateSimulatedTimestamp(1_000L, -1L);
        } catch (IllegalArgumentException expected) {
            rejectedNegativeElapsed = true;
        }
        check(rejectedNegativeElapsed,
                "tester time simulation should reject negative elapsed time");

        runPersistenceAndPurchaseChecks();
        System.out.println("StaminaManagerTest: " + checks + " checks passed");
    }

    private static void runPersistenceAndPurchaseChecks() {
        String oldSaveDirectory = System.getProperty("game.saveDirectory");
        String oldTesterMode = System.getProperty(
                StaminaManager.TESTER_MODE_PROPERTY);
        Path saveDirectory = null;
        try {
            saveDirectory = Files.createTempDirectory("gog-stamina-test-");
            System.setProperty("game.saveDirectory", saveDirectory.toString());
            System.setProperty(StaminaManager.TESTER_MODE_PROPERTY, "true");
            resetManagers();

            StaminaManager stamina = StaminaManager.getInstance();
            DiamondManager diamonds = DiamondManager.getInstance();
            stamina.setStaminaForTester(0);
            System.setProperty(StaminaManager.TESTER_MODE_PROPERTY, "false");
            check(!stamina.tryConsumeForRun(),
                    "a run must be rejected when stamina is empty");
            check(stamina.getStamina() == 0,
                    "a rejected run must not change stamina");
            System.setProperty(StaminaManager.TESTER_MODE_PROPERTY, "true");
            check(stamina.tryConsumeForRun(),
                    "tester mode should allow a run without consuming stamina");
            check(stamina.getStamina() == 0,
                    "tester bypass must leave stamina unchanged");

            stamina.setStaminaForTester(4);
            check(!stamina.tryRefillWithDiamond(),
                    "refill must fail without a diamond");
            check(stamina.getStamina() == 4,
                    "an unaffordable refill must not change stamina");

            diamonds.setDiamondsForTester(1);
            check(stamina.tryRefillWithDiamond(),
                    "refill from 4/5 should succeed with one diamond");
            check(stamina.getStamina() == 5,
                    "successful refill should restore maximum stamina");
            check(diamonds.getDiamonds() == 0,
                    "successful refill should spend exactly one diamond");
            check(!stamina.tryRefillWithDiamond(),
                    "refill must be rejected when stamina is full");

            stamina.setStaminaForTester(0);
            diamonds.setDiamondsForTester(1);
            check(stamina.tryRefillWithDiamond(),
                    "refill from 0/5 should succeed with one diamond");
            check(stamina.getStamina() == 5 && diamonds.getDiamonds() == 0,
                    "empty-bar refill should restore 5 stamina and spend one diamond");

            resetManagers();
            check(StaminaManager.getInstance().getStamina() == 5,
                    "refilled stamina should survive reloading managers");
            check(DiamondManager.getInstance().getDiamonds() == 0,
                    "spent diamond balance should survive reloading managers");

            // If either legacy save cannot be updated, the atomic journal
            // remains authoritative and is recovered by a later save/load.
            stamina = StaminaManager.getInstance();
            diamonds = DiamondManager.getInstance();
            stamina.setStaminaForTester(0);
            diamonds.setDiamondsForTester(1);
            Files.deleteIfExists(saveDirectory.resolve("stamina.properties"));
            Files.createDirectory(saveDirectory.resolve("stamina.properties"));
            check(stamina.tryRefillWithDiamond(),
                    "a refill should commit once its transaction journal is saved");
            check(stamina.getStamina() == 5 && diamonds.getDiamonds() == 0,
                    "journaled refill should expose both updated balances");
            resetManagers();
            check(StaminaManager.getInstance().getStamina() == 5
                    && DiamondManager.getInstance().getDiamonds() == 0,
                    "journal should preserve both balances after manager reload");
            Files.delete(saveDirectory.resolve("stamina.properties"));
            check(FileManager.getInstance().saveStamina(5,
                    System.currentTimeMillis()),
                    "a later save should recover the pending refill journal");
            resetManagers();
            stamina = StaminaManager.getInstance();
            diamonds = DiamondManager.getInstance();
            check(stamina.getStamina() == 5 && diamonds.getDiamonds() == 0,
                    "recovered journal should materialize both resource files");

            // If the journal itself cannot be written, neither balance changes.
            stamina.setStaminaForTester(0);
            diamonds.setDiamondsForTester(1);
            Files.createDirectory(saveDirectory.resolve(
                    "stamina-diamond-transaction.properties"));
            check(!stamina.tryRefillWithDiamond(),
                    "refill must fail when its transaction journal cannot be saved");
            check(stamina.getStamina() == 0 && diamonds.getDiamonds() == 1,
                    "failed journal write must leave both balances unchanged");
            Files.delete(saveDirectory.resolve(
                    "stamina-diamond-transaction.properties"));

            stamina.setStaminaForTester(1);
            System.setProperty(StaminaManager.TESTER_MODE_PROPERTY, "false");
            check(stamina.tryConsumeForRun(),
                    "a run should consume one stamina when persistence succeeds");
            check(stamina.getStamina() == 0,
                    "a successful run should deduct exactly one stamina");
            System.setProperty(StaminaManager.TESTER_MODE_PROPERTY, "true");
            stamina.setStaminaForTester(3);
            System.setProperty(StaminaManager.TESTER_MODE_PROPERTY, "false");
            Files.deleteIfExists(saveDirectory.resolve("stamina.properties"));
            Files.createDirectory(saveDirectory.resolve("stamina.properties"));
            check(!stamina.tryConsumeForRun(),
                    "a run must not start when stamina consumption cannot be saved");
            check(stamina.getStamina() == 3,
                    "failed stamina save must not consume stamina in memory");
            Files.delete(saveDirectory.resolve("stamina.properties"));
            System.setProperty(StaminaManager.TESTER_MODE_PROPERTY, "true");

            diamonds.setDiamondsForTester(2);
            Files.deleteIfExists(saveDirectory.resolve("diamonds"));
            Files.createDirectory(saveDirectory.resolve("diamonds"));
            check(!diamonds.trySpend(1),
                    "diamond spending must fail when the balance cannot be saved");
            check(diamonds.getDiamonds() == 2,
                    "failed diamond save must not reduce the in-memory balance");
            Files.delete(saveDirectory.resolve("diamonds"));
        } catch (IOException e) {
            throw new AssertionError("Could not prepare persistence tests", e);
        } finally {
            resetManagers();
            restoreProperty("game.saveDirectory", oldSaveDirectory);
            restoreProperty(StaminaManager.TESTER_MODE_PROPERTY, oldTesterMode);
            if (saveDirectory != null) {
                try (java.util.stream.Stream<Path> paths = Files.walk(saveDirectory)) {
                    paths.sorted(Comparator.reverseOrder()).forEach(path -> {
                        try {
                            Files.deleteIfExists(path);
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
                } catch (IOException e) {
                    throw new AssertionError("Could not clean persistence tests", e);
                }
            }
        }
    }

    private static void resetManagers() {
        StaminaManager.resetInstanceForTests();
        DiamondManager.resetInstanceForTests();
        FileManager.resetInstanceForTests();
    }

    private static void restoreProperty(final String name, final String value) {
        if (value == null)
            System.clearProperty(name);
        else
            System.setProperty(name, value);
    }
}
