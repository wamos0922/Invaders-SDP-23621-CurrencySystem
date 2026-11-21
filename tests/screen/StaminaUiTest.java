package screen;

import java.awt.Color;
import engine.DrawManager;

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
        check(new Color(65, 235, 85).equals(DrawManager.staminaColor(5, 5))
                        && new Color(65, 235, 85).equals(
                                DrawManager.staminaColor(3, 5)),
                "three to five stamina should use the green battery colour");
        check(new Color(255, 205, 55).equals(DrawManager.staminaColor(2, 5))
                        && new Color(255, 205, 55).equals(
                                DrawManager.staminaColor(1, 5)),
                "one or two stamina should use the yellow battery colour");
        check(new Color(245, 65, 65).equals(DrawManager.staminaColor(0, 5)),
                "zero stamina should use the red battery colour");
        System.out.println("StaminaUiTest: " + checks + " checks passed");
    }

    private StaminaUiTest() { }
}
