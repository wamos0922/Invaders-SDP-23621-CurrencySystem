import java.io.File;
import java.nio.file.Files;

import skilltree.ShardType;
import skilltree.ShardWallet;

/**
 * Standalone checks for the shard wallet (GOG-10). Uses a temporary file,
 * so the player's real save is never touched.
 */
public final class ShardWalletTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        File file = new File(Files.createTempDirectory("shard-wallet-test").toFile(), "shards");

        // Arrange: a new wallet is empty.
        ShardWallet wallet = ShardWallet.forFile(file);
        for (ShardType type : ShardType.values())
            check(wallet.get(type) == 0, type + " should start at 0");

        // Act + assert: adding.
        check(wallet.add(ShardType.COMMON, 30), "add failed");
        check(wallet.add(ShardType.RARE, 5), "add failed");
        check(wallet.get(ShardType.COMMON) == 30 && wallet.get(ShardType.RARE) == 5, "wrong amounts after add");

        // Spending is all or nothing.
        int[] tooMuch = { 10, 6, 0, 0, 0 };
        check(!wallet.canAfford(tooMuch), "should not afford 6 rare");
        check(!wallet.spend(tooMuch), "spend should fail");
        check(wallet.get(ShardType.COMMON) == 30, "failed spend must not take common");
        int[] cost = { 20, 5, 0, 0, 0 };
        check(wallet.spend(cost), "spend failed");
        check(wallet.get(ShardType.COMMON) == 10 && wallet.get(ShardType.RARE) == 0, "wrong amounts after spend");

        // A change can't take a rarity below 0.
        check(!wallet.apply(new int[] { -11, 0, 0, 0, 0 }), "should not go below 0");
        check(wallet.apply(new int[] { -10, 2, 0, 0, 0 }), "apply failed");

        // Saved: a new wallet from the same file sees the same amounts.
        ShardWallet reloaded = ShardWallet.forFile(file);
        check(reloaded.get(ShardType.COMMON) == 0 && reloaded.get(ShardType.RARE) == 2, "not saved");

        // A broken line reads as 0 instead of crashing.
        Files.write(file.toPath(), "common=abc\nrare=3\n".getBytes("UTF-8"));
        ShardWallet broken = ShardWallet.forFile(file);
        check(broken.get(ShardType.COMMON) == 0 && broken.get(ShardType.RARE) == 3, "broken file not handled");

        System.out.println("ShardWalletTest passed (" + checks + " checks).");
    }
}
