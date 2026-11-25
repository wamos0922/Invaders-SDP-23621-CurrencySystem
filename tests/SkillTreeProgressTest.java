import java.io.File;
import java.nio.file.Files;

import entity.Ship.ShipType;
import skilltree.NodeState;
import skilltree.ShardType;
import skilltree.ShardWallet;
import skilltree.SkillBranch;
import skilltree.SkillNode;
import skilltree.SkillTreeCatalog;
import skilltree.SkillTreeProgress;
import skilltree.SkillTreeProgress.Unlock;

/**
 * Standalone checks for unlocking skill nodes (GOG-88): order, payment,
 * shared branches and saving. Uses temporary files only.
 */
public final class SkillTreeProgressTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) throws Exception {
        File dir = Files.createTempDirectory("skill-tree-test").toFile();
        ShardWallet wallet = ShardWallet.forFile(new File(dir, "shards"));
        SkillTreeProgress progress = SkillTreeProgress.forFile(new File(dir, "skill-tree"), wallet);
        java.util.List<SkillNode> armor = SkillTreeCatalog.nodes(ShipType.STANDARD, SkillBranch.ARMOR);
        java.util.List<SkillNode> fastMove = SkillTreeCatalog.nodes(ShipType.FAST_MOVE, SkillBranch.SPECIALTY);

        // Arrange: nothing owned, first node available.
        check(progress.state(ShipType.STANDARD, armor.get(0)) == NodeState.AVAILABLE, "first node should be available");
        check(progress.state(ShipType.STANDARD, armor.get(1)) == NodeState.LOCKED, "second node should be locked");

        // Not enough shards: nothing changes.
        check(progress.unlock(ShipType.STANDARD, armor.get(0)) == Unlock.NOT_ENOUGH_SHARDS, "should need shards");

        // Out of order: locked.
        wallet.add(ShardType.COMMON, 100);
        wallet.add(ShardType.RARE, 20);
        check(progress.unlock(ShipType.STANDARD, armor.get(1)) == Unlock.LOCKED, "node 2 before node 1");

        // Act: unlock node 1, pays exactly its cost.
        check(progress.unlock(ShipType.STANDARD, armor.get(0)) == Unlock.OK, "unlock failed");
        check(wallet.get(ShardType.COMMON) == 90, "should pay 10 common, have " + wallet.get(ShardType.COMMON));
        check(progress.state(ShipType.STANDARD, armor.get(0)) == NodeState.OWNED, "node 1 should be owned");
        check(progress.unlock(ShipType.STANDARD, armor.get(0)) == Unlock.ALREADY_OWNED, "can't buy twice");

        // Shared branch: owned on every ship. Specialty: only on its ship.
        check(progress.has(ShipType.TWO_WAY, SkillTreeCatalog.QUICK_REPAIR), "Armor should count for every ship");
        check(progress.unlock(ShipType.FAST_MOVE, fastMove.get(0)) == Unlock.OK, "specialty unlock failed");
        check(progress.owned(ShipType.FAST_MOVE, SkillBranch.SPECIALTY) == 1, "Fast Move specialty should be 1");
        check(progress.owned(ShipType.STANDARD, SkillBranch.SPECIALTY) == 0, "Standard specialty should be 0");
        check(progress.ownedCount(ShipType.FAST_MOVE) == 2, "Fast Move page should show 2 unlocked");

        // Saved: new objects from the same files see the same progress.
        SkillTreeProgress reloaded = SkillTreeProgress.forFile(new File(dir, "skill-tree"),
                ShardWallet.forFile(new File(dir, "shards")));
        check(reloaded.owned(ShipType.STANDARD, SkillBranch.ARMOR) == 1, "armor not saved");
        check(reloaded.owned(ShipType.FAST_MOVE, SkillBranch.SPECIALTY) == 1, "specialty not saved");

        System.out.println("SkillTreeProgressTest passed (" + checks + " checks).");
    }
}
