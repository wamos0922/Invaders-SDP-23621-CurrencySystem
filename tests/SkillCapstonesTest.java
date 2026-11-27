import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;

import entity.Bullet;
import entity.Ship;
import entity.Ship.ShipType;
import skilltree.ShardType;
import skilltree.ShardWallet;
import skilltree.SkillBranch;
import skilltree.SkillCapstones;
import skilltree.SkillEffects;
import skilltree.SkillNode;
import skilltree.SkillTreeCatalog;
import skilltree.SkillTreeProgress;

/**
 * Standalone checks for the five capstones (GOG-91). Time is passed in,
 * so no waiting.
 */
public final class SkillCapstonesTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    /** Effects with the whole Specialty branch of a ship. */
    private static SkillEffects full(ShipType ship) throws Exception {
        File dir = Files.createTempDirectory("capstones-test").toFile();
        ShardWallet wallet = ShardWallet.forFile(new File(dir, "shards"));
        for (ShardType type : ShardType.values()) wallet.add(type, 10000);
        SkillTreeProgress progress = SkillTreeProgress.forFile(new File(dir, "skill-tree"), wallet);
        for (SkillNode node : SkillTreeCatalog.nodes(ship, SkillBranch.SPECIALTY)) progress.unlock(ship, node);
        return SkillEffects.of(ship, progress);
    }

    public static void main(String[] args) throws Exception {
        // Adaptive Core: 5 piercing shots, then 15 s to charge again.
        SkillCapstones core = new SkillCapstones(full(ShipType.STANDARD), 0);
        int piercing = 0;
        for (int i = 0; i < 8; i++) {
            Bullet b = new Bullet(0, 0, -6);
            core.onShot(Arrays.asList(b), 1000 + i * 100);
            if (b.getPierce() > 0) piercing++;
        }
        check(piercing == 5, "first charge should give 5 piercing shots, got " + piercing);
        Bullet later = new Bullet(0, 0, -6);
        core.onShot(Arrays.asList(later), 1400 + 15000 + 1);
        check(later.getPierce() == 1, "charged again after 15 s");

        // Bullet Storm: x2 fire rate in the last 3 s of every 20 s of play.
        SkillCapstones storm = new SkillCapstones(full(ShipType.FAST_ATTACK), 6000);
        check(storm.fireRateFactor(6000 + 10000) == 1.0, "no storm at 10 s");
        check(storm.fireRateFactor(6000 + 18000) == 2.0, "storm at 18 s");
        check(storm.fireRateFactor(6000 + 21000) == 1.0, "storm over at 21 s");
        check(storm.fireRateFactor(1000) == 1.0, "no storm during the countdown");
        // A 5 s pause shifts the storm rhythm: the 18 s storm now comes at 23 s.
        SkillCapstones pausedStorm = new SkillCapstones(full(ShipType.FAST_ATTACK), 6000);
        pausedStorm.resumeAfterPause(5000);
        check(pausedStorm.fireRateFactor(6000 + 18000) == 1.0, "no storm at 18 s after a 5 s pause");
        check(pausedStorm.fireRateFactor(6000 + 23000) == 2.0, "storm at 23 s after a 5 s pause");

        // Triple Barrel: every 3rd volley.
        SkillCapstones triple = new SkillCapstones(full(ShipType.TWO_WAY), 0);
        int extra = 0;
        for (int i = 0; i < 9; i++) if (triple.addsMiddleBullet()) extra++;
        check(extra == 3, "9 volleys should add 3 middle bullets, got " + extra);

        // Blink Dash: double-tap dashes 80 px, invincible briefly, 4 s cooldown.
        SkillCapstones dash = new SkillCapstones(full(ShipType.FAST_MOVE), 0);
        check(dash.onDirectionPressed(1, 1000) == 0, "single tap: no dash");
        check(dash.onDirectionPressed(1, 1200) == 80, "double tap: dash right");
        check(dash.dashInvincible(1300) && !dash.dashInvincible(1500), "invincible for 250 ms");
        // A pause during the dash keeps the invincibility for the rest of it.
        SkillCapstones dashPause = new SkillCapstones(full(ShipType.FAST_MOVE), 0);
        dashPause.onDirectionPressed(1, 1000);
        dashPause.onDirectionPressed(1, 1200); // dash: invincible until 1450
        dashPause.resumeAfterPause(3000);      // paused at 1300 for 3 s
        check(dashPause.dashInvincible(4400) && !dashPause.dashInvincible(4500), "dash invincibility survives a pause");
        dash.onDirectionPressed(-1, 2000);
        check(dash.onDirectionPressed(-1, 2100) == 0, "cooldown: no dash 1 s later");
        dash.onDirectionPressed(-1, 5300);
        check(dash.onDirectionPressed(-1, 5400) == -80, "dash left after the cooldown");
        check(dash.onDirectionPressed(1, 9000) == 0 && dash.onDirectionPressed(-1, 9100) == 0,
                "left then right is not a double-tap");

        // Shockwave: only Big Bullet's capstone; same-row neighbours within 40 px.
        check(new SkillCapstones(full(ShipType.BIG_BULLET), 0).hasShockwave(), "Big Bullet has Shockwave");
        check(!new SkillCapstones(SkillEffects.none(), 0).hasShockwave(), "no node, no Shockwave");
        Ship near = new Ship(100, 100), far = new Ship(200, 100); // 26 x 16, centre (113, 108)
        check(SkillCapstones.isNextTo(113 - 30, 108, near), "30 px left in the same row is next to");
        check(!SkillCapstones.isNextTo(113 - 30, 108, far), "far away is not");
        check(!SkillCapstones.isNextTo(113, 108 - 30, near), "the row above is not");

        System.out.println("SkillCapstonesTest passed (" + checks + " checks).");
    }
}
