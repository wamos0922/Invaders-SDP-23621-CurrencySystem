import java.util.HashSet;
import java.util.Set;

import entity.Ship.ShipType;
import skilltree.ShardType;
import skilltree.SkillBranch;
import skilltree.SkillNode;
import skilltree.SkillTreeCatalog;

/**
 * Standalone checks for the skill tree node list (GOG-88): sizes, unique
 * ids, costs rising with depth, and the totals from the design doc.
 */
public final class SkillTreeCatalogTest {
    private static int checks;

    private static void check(boolean condition, String message) {
        checks++;
        if (!condition) throw new AssertionError(message);
    }

    public static void main(String[] args) {
        Set<String> ids = new HashSet<String>();
        for (ShipType ship : ShipType.values())
            for (SkillBranch branch : SkillBranch.values()) {
                java.util.List<SkillNode> nodes = SkillTreeCatalog.nodes(ship, branch);
                check(nodes.size() == branch.size(), ship + " " + branch + " has " + nodes.size() + " nodes");
                for (int i = 0; i < nodes.size(); i++) {
                    SkillNode node = nodes.get(i);
                    check(node.getIndex() == i && node.getBranch() == branch, node.getId() + " wrong position");
                    check(!node.getName().isEmpty() && !node.getDescription().isEmpty(), node.getId() + " has no text");
                    if (!branch.isShared() || ship == ShipType.STANDARD)
                        check(ids.add(node.getId()), "duplicate id " + node.getId());
                    if (i > 0)
                        check(node.getCost(ShardType.COMMON) > nodes.get(i - 1).getCost(ShardType.COMMON),
                                node.getId() + " should cost more than the node above");
                }
                check(nodes.get(nodes.size() - 1).isCapstone() == (branch == SkillBranch.SPECIALTY),
                        "only the last Specialty node is a capstone");
            }
        check(ids.size() == 4 + 4 + 5 * 7, "expected 43 nodes, got " + ids.size());

        // Totals from docs/design/skill-tree-and-items.md, section 4.
        int[] specialty = new int[5];
        for (SkillNode node : SkillTreeCatalog.nodes(ShipType.STANDARD, SkillBranch.SPECIALTY))
            for (ShardType type : ShardType.values()) specialty[type.ordinal()] += node.getCost(type);
        check(java.util.Arrays.equals(specialty, new int[] { 355, 105, 41, 13, 4 }),
                "Specialty total " + java.util.Arrays.toString(specialty));
        int[] shared = new int[5];
        for (SkillBranch branch : new SkillBranch[] { SkillBranch.ARMOR, SkillBranch.SALVAGE })
            for (SkillNode node : SkillTreeCatalog.nodes(ShipType.STANDARD, branch))
                for (ShardType type : ShardType.values()) shared[type.ordinal()] += node.getCost(type);
        check(java.util.Arrays.equals(shared, new int[] { 210, 60, 18, 4, 0 }),
                "shared total " + java.util.Arrays.toString(shared));

        System.out.println("SkillTreeCatalogTest passed (" + checks + " checks).");
    }
}
