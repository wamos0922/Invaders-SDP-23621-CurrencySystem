import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import entity.Bullet;
import entity.Ship;
import entity.Ship.ShipType;
import skilltree.ShardType;
import skilltree.ShardWallet;
import skilltree.SkillBranch;
import skilltree.SkillEffects;
import skilltree.SkillNode;
import skilltree.SkillShots;
import skilltree.SkillTreeCatalog;
import skilltree.SkillTreeProgress;

/**
 * Standalone checks for the Specialty nodes that change shots and hits
 * (GOG-89): pierce, Hot Rounds, Slipstream, Wide Barrels, Crossfire,
 * Pickup Rush, Heavy Impact and Near Miss.
 */
public final class SkillShotsTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    /** Effects with the first n Specialty nodes of a ship. */
    private static SkillEffects specialty(ShipType ship, int n) throws Exception {
        File dir = Files.createTempDirectory("skill-shots-test").toFile();
        ShardWallet wallet = ShardWallet.forFile(new File(dir, "shards"));
        for (ShardType type : ShardType.values()) wallet.add(type, 10000);
        SkillTreeProgress progress = SkillTreeProgress.forFile(new File(dir, "skill-tree"), wallet);
        java.util.List<SkillNode> nodes = SkillTreeCatalog.nodes(ship, SkillBranch.SPECIALTY);
        for (int i = 0; i < n; i++) progress.unlock(ship, nodes.get(i));
        return SkillEffects.of(ship, progress);
    }

    public static void main(String[] args) throws Exception {
        // Piercing Shell (Big Bullet 3): every bullet goes through one enemy.
        SkillShots big = new SkillShots(specialty(ShipType.BIG_BULLET, 3));
        Bullet shell = new Bullet(0, 0, -6);
        big.onShot(Arrays.asList(shell), false);
        check(big.passThrough(shell), "first kill: goes on");
        check(!big.passThrough(shell), "second kill: used up");

        // Hot Rounds (Fast Attack 5): only every 10th shot pierces.
        SkillShots gunner = new SkillShots(specialty(ShipType.FAST_ATTACK, 5));
        int piercing = 0;
        for (int i = 1; i <= 20; i++) {
            Bullet b = new Bullet(0, 0, -6);
            gunner.onShot(Arrays.asList(b), false);
            if (b.getPierce() > 0) piercing++;
        }
        check(piercing == 2, "20 shots should give 2 piercing, got " + piercing);

        // Slipstream (Fast Move 3): +20% bullet speed only while moving.
        SkillShots mover = new SkillShots(specialty(ShipType.FAST_MOVE, 3));
        Bullet still = new Bullet(0, 0, -10), moving = new Bullet(0, 0, -10);
        mover.onShot(Arrays.asList(still), false);
        mover.onShot(Arrays.asList(moving), true);
        check(still.getSpeed() == -10 && moving.getSpeed() == -12, "Slipstream speeds " + still.getSpeed() + " " + moving.getSpeed());

        // Wide Barrels (Two-Way 2): the two bullets drift apart.
        SkillShots twin = new SkillShots(specialty(ShipType.TWO_WAY, 5));
        Bullet left = new Bullet(0, 0, -6), right = new Bullet(16, 0, -6);
        twin.onShot(Arrays.asList(left, right), false);
        check(left.getSpeedX() < 0 && right.getSpeedX() > 0, "Wide Barrels spread");

        // Crossfire (Two-Way 5): second kill of a volley adds 50% of both.
        check(twin.crossfireBonus(left, 20) == 0, "first kill: no bonus yet");
        check(twin.crossfireBonus(right, 20) == 20, "second kill: 50% of 40");
        Bullet lone = new Bullet(0, 0, -6);
        twin.onShot(Arrays.asList(lone), false);
        check(twin.crossfireBonus(lone, 30) == 0, "a single kill gets nothing");

        // Pickup Rush (Standard 5): +20% fire rate for 3 s after a pickup.
        SkillShots standard = new SkillShots(specialty(ShipType.STANDARD, 5));
        check(standard.fireRateFactor(0) == 1.0, "no rush before a pickup");
        standard.onItemCollected(1000);
        check(standard.fireRateFactor(3500) == 1.2 && standard.fireRateFactor(4100) == 1.0, "rush lasts 3 s");
        // Pausing doesn't use up the rush: a 5 s pause moves the end to 9 s.
        standard.onItemCollected(1000);
        standard.resumeAfterPause(5000);
        check(standard.fireRateFactor(8900) == 1.2 && standard.fireRateFactor(9100) == 1.0, "rush survives a pause");

        // Heavy Impact (Big Bullet 5): every 5th boss hit counts twice.
        SkillShots heavy = new SkillShots(specialty(ShipType.BIG_BULLET, 5));
        int doubled = 0;
        for (int i = 0; i < 10; i++) if (heavy.heavyImpact()) doubled++;
        check(doubled == 2, "10 boss hits should give 2 double hits, got " + doubled);

        // Near Miss (Fast Move 6): a bullet passing just beside the ship's top edge.
        SkillShots dodger = new SkillShots(specialty(ShipType.FAST_MOVE, 6));
        Ship ship = new Ship(100, 400, ShipType.FAST_MOVE); // 26 px wide
        Bullet beside = new Bullet(132, 400 - 10, 4); // bottom at 400, 6 px right of the ship
        check(dodger.nearMiss(beside, ship), "close pass should count");
        Bullet far = new Bullet(200, 400 - 10, 4);
        check(!dodger.nearMiss(far, ship), "far pass should not count");
        check(!new SkillShots(SkillEffects.none()).nearMiss(beside, ship), "no node, no Near Miss");

                // A fire-rate change (e.g. Pickup Rush) doesn't let the ship fire again
        // straight away: the next shot still waits for the interval.
        Ship gun = new Ship(100, 400, ShipType.STANDARD);
        java.util.Set<Bullet> fired = new java.util.HashSet<Bullet>();
        check(gun.shoot(fired), "first shot fires");
        gun.setSkillBonuses(1.0, 1.2, 1.0, 1.0); // Pickup Rush starts
        check(!gun.shoot(fired), "no instant extra shot when the fire rate changes");
        gun.setSkillBonuses(1.0, 1.0, 1.0, 1.0); // Pickup Rush ends
        check(!gun.shoot(fired), "no instant extra shot when it changes back");

        System.out.println("SkillShotsTest passed (" + checks + " checks).");
    }
}
