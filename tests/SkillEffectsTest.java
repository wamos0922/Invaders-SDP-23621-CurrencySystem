import java.io.File;
import java.nio.file.Files;

import entity.Ship;
import entity.Ship.ShipType;
import skilltree.ShardType;
import skilltree.ShardWallet;
import skilltree.SkillBranch;
import skilltree.SkillEffects;
import skilltree.SkillNode;
import skilltree.SkillStat;
import skilltree.SkillTreeCatalog;
import skilltree.SkillTreeProgress;

/**
 * Standalone checks for skill node effects on the ship (GOG-89): stat
 * totals per ship, fire rate, fractional speed and shared branches.
 */
public final class SkillEffectsTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    /** Unlocks the first n nodes of a branch, with plenty of shards. */
    private static void unlock(SkillTreeProgress progress, ShipType ship, SkillBranch branch, int n) {
        java.util.List<SkillNode> nodes = SkillTreeCatalog.nodes(ship, branch);
        for (int i = 0; i < n; i++)
            check(progress.unlock(ship, nodes.get(i)) == SkillTreeProgress.Unlock.OK, "unlock " + nodes.get(i).getId());
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("skill-effects-test").toFile();
        ShardWallet wallet = ShardWallet.forFile(new File(dir, "shards"));
        for (ShardType type : ShardType.values()) wallet.add(type, 10000);
        SkillTreeProgress progress = SkillTreeProgress.forFile(new File(dir, "skill-tree"), wallet);

        // Nothing unlocked: no bonus.
        check(SkillEffects.of(ShipType.STANDARD, progress).multiplier(SkillStat.SPEED) == 1.0, "no bonus expected");
        check(SkillEffects.none().percent(SkillStat.FIRE_RATE) == 0, "none() should be empty");

        // Full Fast Attack Specialty: +18% fire rate (3 x 6), +10% speed.
        unlock(progress, ShipType.FAST_ATTACK, SkillBranch.SPECIALTY, 7);
        SkillEffects fastAttack = SkillEffects.of(ShipType.FAST_ATTACK, progress);
        check(fastAttack.percent(SkillStat.FIRE_RATE) == 18, "fire rate " + fastAttack.percent(SkillStat.FIRE_RATE));
        check(fastAttack.percent(SkillStat.SPEED) == 10, "speed " + fastAttack.percent(SkillStat.SPEED));
        check(fastAttack.has(SkillTreeCatalog.BULLET_STORM), "capstone should be owned");
        // Another ship does not get Fast Attack's nodes.
        check(SkillEffects.of(ShipType.TWO_WAY, progress).percent(SkillStat.FIRE_RATE) == 0, "Two-Way got Fast Attack nodes");

        // Shared Armor counts for every ship.
        unlock(progress, ShipType.STANDARD, SkillBranch.ARMOR, 1);
        check(SkillEffects.of(ShipType.BIG_BULLET, progress).has(SkillTreeCatalog.QUICK_REPAIR), "Armor not shared");

        // Ship: fire rate factor changes the time between shots.
        Ship ship = new Ship(0, 0, ShipType.FAST_ATTACK);
        check(ship.getShootingIntervalMillis() == 375, "base interval " + ship.getShootingIntervalMillis());
        ship.setSkillBonuses(1.0, fastAttack.multiplier(SkillStat.FIRE_RATE), 1.0, 1.0);
        check(ship.getShootingIntervalMillis() == 318, "+18% should give 318 ms, got " + ship.getShootingIntervalMillis());

        // Ship: +25% speed on Two-Way (speed 1) moves 5 px every 4 frames.
        Ship twoWay = new Ship(100, 0, ShipType.TWO_WAY);
        twoWay.setSkillBonuses(1.25, 1.0, 1.0, 1.0);
        for (int i = 0; i < 4; i++) twoWay.moveRight();
        check(twoWay.getPositionX() == 105, "expected x 105, got " + twoWay.getPositionX());
        for (int i = 0; i < 4; i++) twoWay.moveLeft();
        check(twoWay.getPositionX() == 100, "expected back at 100, got " + twoWay.getPositionX());

        System.out.println("SkillEffectsTest passed (" + checks + " checks).");
    }
}
