package screen;

/** Deterministic checks for the player-facing stamina countdown formatting. */
public final class StaminaUiTest {

    private static int checks;

    private static void check(final boolean condition, final String message) {
        checks++;
        if (!condition)
            throw new AssertionError(message);
    }

    public static void main(final String[] args) {
        check("10:00".equals(TitleScreen.formatStaminaCountdown(600_000L)),
                "a full regeneration interval should display 10:00");
        check("09:59".equals(TitleScreen.formatStaminaCountdown(599_000L)),
                "remaining milliseconds should round up to the next second");
        check("00:01".equals(TitleScreen.formatStaminaCountdown(1L)),
                "a partial second should display one second");
        check("00:00".equals(TitleScreen.formatStaminaCountdown(0L)),
                "zero remaining time should display 00:00");
        check("00:00".equals(TitleScreen.formatStaminaCountdown(-1L)),
                "negative durations should be clamped to zero");
        System.out.println("StaminaUiTest: " + checks + " checks passed");
    }

    private StaminaUiTest() { }
}
