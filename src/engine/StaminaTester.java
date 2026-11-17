package engine;

/**
 * Command-line controls for local stamina testing. This utility is disabled
 * unless the application is launched with -Dgame.testerMode=true.
 *
 * Examples (Windows PowerShell):
 *   java -Dgame.testerMode=true -cp "bin;res" engine.StaminaTester stamina 0
 *   java -Dgame.testerMode=true -cp "bin;res" engine.StaminaTester diamonds 20
 *   java -Dgame.testerMode=true -cp "bin;res" engine.StaminaTester add-diamonds 5
 *   java -Dgame.testerMode=true -cp "bin;res" engine.StaminaTester advance 30
 */
public final class StaminaTester {

    private StaminaTester() {
        // Utility class.
    }

    /**
     * Applies one tester command to the saved player data.
     *
     * @param args Command and value: stamina 0-5, diamonds amount, or advance
     *             elapsed minutes.
     */
    public static void main(final String[] args) {
        if (!Boolean.getBoolean(StaminaManager.TESTER_MODE_PROPERTY)) {
            System.err.println("Tester mode is disabled. Relaunch with -D"
                    + StaminaManager.TESTER_MODE_PROPERTY + "=true.");
            System.exit(1);
        }
        if (args.length != 2) {
            printUsage();
            System.exit(2);
        }

        try {
            int value = Integer.parseInt(args[1]);
            StaminaManager stamina = StaminaManager.getInstance();
            if ("stamina".equalsIgnoreCase(args[0])) {
                stamina.setStaminaForTester(value);
                System.out.println("Stamina set to " + stamina.getStamina()
                        + "/" + stamina.getMaxStamina());
            } else if ("diamonds".equalsIgnoreCase(args[0])) {
                DiamondManager.getInstance().setDiamondsForTester(value);
                System.out.println("Diamonds set to "
                        + DiamondManager.getInstance().getDiamonds());
            } else if ("add-diamonds".equalsIgnoreCase(args[0])) {
                DiamondManager.getInstance().addDiamondsForTester(value);
                System.out.println("Diamonds increased to "
                        + DiamondManager.getInstance().getDiamonds());
            } else if ("advance".equalsIgnoreCase(args[0])) {
                if (value < 0)
                    throw new IllegalArgumentException(
                            "minutes must not be negative");
                stamina.simulateElapsedTimeForTester(value * 60_000L);
                System.out.println("Simulated " + value
                        + " minutes; stamina is " + stamina.getStamina()
                        + "/" + stamina.getMaxStamina());
            } else {
                printUsage();
                System.exit(2);
            }
        } catch (NumberFormatException e) {
            System.err.println("Value must be a whole number.");
            printUsage();
            System.exit(2);
        }
    }

    private static void printUsage() {
        System.err.println("Usage: StaminaTester <stamina|diamonds|add-diamonds|advance> <value>");
        System.err.println("  stamina: 0-5; diamonds: set balance; add-diamonds: add amount;");
        System.err.println("  advance: elapsed minutes to simulate.");
    }
}
