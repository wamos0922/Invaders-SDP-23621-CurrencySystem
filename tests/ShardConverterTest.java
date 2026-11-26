import java.io.File;
import java.nio.file.Files;

import skilltree.ShardConverter;
import skilltree.ShardType;
import skilltree.ShardWallet;

/**
 * Standalone checks for shard conversion (GOG-90): rates in both
 * directions, all-or-nothing, and that a round trip never gains shards.
 */
public final class ShardConverterTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        // Rates.
        check(ShardConverter.givePerConversion(ShardType.COMMON, ShardType.RARE) == 5, "5 common -> 1 rare");
        check(ShardConverter.givePerConversion(ShardType.COMMON, ShardType.EPIC) == 25, "25 common -> 1 epic");
        check(ShardConverter.givePerConversion(ShardType.COMMON, ShardType.MYTHIC) == 625, "625 common -> 1 mythic");
        check(ShardConverter.getPerConversion(ShardType.RARE, ShardType.COMMON) == 3, "1 rare -> 3 common");
        check(ShardConverter.getPerConversion(ShardType.MYTHIC, ShardType.COMMON) == 81, "1 mythic -> 81 common");

        // Round trips always lose.
        for (ShardType a : ShardType.values())
            for (ShardType b : ShardType.values()) {
                if (a == b) continue;
                double there = (double) ShardConverter.getPerConversion(a, b) / ShardConverter.givePerConversion(a, b);
                double back = (double) ShardConverter.getPerConversion(b, a) / ShardConverter.givePerConversion(b, a);
                check(there * back < 1.0, a + " -> " + b + " -> " + a + " should lose shards");
            }

        // Arrange a wallet with 12 common.
        ShardWallet wallet = ShardWallet.forFile(new File(Files.createTempDirectory("converter-test").toFile(), "shards"));
        wallet.add(ShardType.COMMON, 12);
        check(ShardConverter.maxConversions(wallet, ShardType.COMMON, ShardType.RARE) == 2, "12 common = 2 conversions");
        check(ShardConverter.maxConversions(wallet, ShardType.COMMON, ShardType.COMMON) == 0, "same rarity can't convert");

        // Too many: nothing changes.
        check(!ShardConverter.convert(wallet, ShardType.COMMON, ShardType.RARE, 3), "3 conversions should fail");
        check(wallet.get(ShardType.COMMON) == 12, "failed convert must not take shards");

        // Act: upgrade 2 times, then break 1 rare down.
        check(ShardConverter.convert(wallet, ShardType.COMMON, ShardType.RARE, 2), "upgrade failed");
        check(wallet.get(ShardType.COMMON) == 2 && wallet.get(ShardType.RARE) == 2, "after upgrade");
        check(ShardConverter.convert(wallet, ShardType.RARE, ShardType.COMMON, 1), "break down failed");
        check(wallet.get(ShardType.COMMON) == 5 && wallet.get(ShardType.RARE) == 1, "after break down");

                // Very large amounts are refused, never wrapped around (overflow).
        ShardWallet big = ShardWallet.forFile(new File(Files.createTempDirectory("converter-big").toFile(), "shards"));
        // 81 x 53,024,316 = 4,294,969,596 wraps around int to just 4,300:
        // without the check the player would lose 53M Mythic for 4,300 Common.
        big.add(ShardType.MYTHIC, 53_024_316);
        check(!ShardConverter.convert(big, ShardType.MYTHIC, ShardType.COMMON, 53_024_316), "81 x 53M common is too many");
        check(big.get(ShardType.MYTHIC) == 53_024_316 && big.get(ShardType.COMMON) == 0, "refused convert changes nothing");
        check(ShardConverter.convert(big, ShardType.MYTHIC, ShardType.COMMON, 1_000_000), "81M common still fits");
        check(big.get(ShardType.COMMON) == 81_000_000, "81M common received");
        check(big.add(ShardType.COMMON, Integer.MAX_VALUE - 81_000_000), "fill up to the limit");
        check(!big.add(ShardType.COMMON, 1), "wallet can't go past the int limit");
        check(!ShardConverter.convert(big, ShardType.MYTHIC, ShardType.COMMON, 1), "full wallet refuses more");
        check(big.get(ShardType.COMMON) == Integer.MAX_VALUE, "still exactly at the limit");
        
        System.out.println("ShardConverterTest passed (" + checks + " checks).");
    }
}
