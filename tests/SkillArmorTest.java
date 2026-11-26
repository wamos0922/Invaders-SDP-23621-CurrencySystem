import java.io.File;
import java.nio.file.Files;

import entity.Ship.ShipType;
import skilltree.ShardType;
import skilltree.ShardWallet;
import skilltree.SkillArmor;
import skilltree.SkillBranch;
import skilltree.SkillEffects;
import skilltree.SkillNode;
import skilltree.SkillSalvage;
import skilltree.SkillTreeCatalog;
import skilltree.SkillTreeProgress;

/**
 * Standalone checks for the Armor and Salvage nodes (GOG-89). Time is
 * passed in, so no waiting.
 */
public final class SkillArmorTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    private static SkillEffects withShared(int armor, int salvage) throws Exception {
        File dir = Files.createTempDirectory("skill-armor-test").toFile();
        ShardWallet wallet = ShardWallet.forFile(new File(dir, "shards"));
        for (ShardType type : ShardType.values()) wallet.add(type, 10000);
        SkillTreeProgress progress = SkillTreeProgress.forFile(new File(dir, "skill-tree"), wallet);
        java.util.List<SkillNode> a = SkillTreeCatalog.nodes(ShipType.STANDARD, SkillBranch.ARMOR);
        java.util.List<SkillNode> s = SkillTreeCatalog.nodes(ShipType.STANDARD, SkillBranch.SALVAGE);
        for (int i = 0; i < armor; i++) progress.unlock(ShipType.STANDARD, a.get(i));
        for (int i = 0; i < salvage; i++) progress.unlock(ShipType.STANDARD, s.get(i));
        return SkillEffects.of(ShipType.STANDARD, progress);
    }

    public static void main(String[] args) throws Exception {
        // No Armor nodes: nothing is blocked.
        SkillArmor none = new SkillArmor(SkillEffects.none(), false);
        none.startLevel(1000);
        check(!none.shieldActive(1500) && !none.tryBlockHit(1500), "no nodes should block nothing");

        // Quick Repair only: invincible 1.5 s after losing a life.
        SkillArmor repair = new SkillArmor(withShared(1, 0), false);
        repair.startLevel(0);
        check(!repair.shieldActive(100), "Quick Repair gives no Launch Shield");
        repair.onLifeLost(2, 10000);
        check(repair.tryBlockHit(11400), "still invincible at +1.4 s");
        check(!repair.tryBlockHit(11600), "no longer invincible at +1.6 s");

        // Launch Shield: 2 s after play starts.
        SkillArmor launch = new SkillArmor(withShared(2, 0), false);
        launch.startLevel(6000);
        check(launch.shieldActive(7000) && launch.tryBlockHit(7000), "shield at +1 s");
        check(!launch.shieldActive(8100), "shield gone at +2.1 s");

        // Last Stand: shield when down to 1 life, once per level.
        SkillArmor stand = new SkillArmor(withShared(3, 0), false);
        stand.startLevel(0);
        stand.onLifeLost(2, 20000);
        check(!stand.shieldActive(22000), "no Last Stand at 2 lives");
        stand.onLifeLost(1, 30000);
        check(stand.shieldActive(32500), "Last Stand shield at 1 life");
        check(!stand.shieldActive(33100), "Last Stand lasts 3 s");

        // Hull Plating: the first hit of the level is blocked, only once.
        SkillArmor plating = new SkillArmor(withShared(4, 0), true);
        plating.startLevel(0);
        check(plating.tryBlockHit(50000), "first hit blocked");
        check(!plating.tryBlockHit(60000), "second hit not blocked");

        // Salvage numbers.
        SkillEffects salvage = withShared(0, 4);
        check(SkillSalvage.coinChanceBonus(salvage) == 0.02, "Scavenger +2%");
        check(SkillSalvage.itemChanceBonus(salvage) == 0.03, "Lucky Find +3%");
        check(SkillSalvage.groundTimeBonus(salvage) == 3000, "Long Fuse +3 s");
        check(SkillSalvage.shardRewardFactor(salvage) == 1.10, "Shard Hunter x1.1");
        check(SkillSalvage.coinChanceBonus(SkillEffects.none()) == 0, "no Scavenger, no bonus");

        System.out.println("SkillArmorTest passed (" + checks + " checks).");
    }
}
