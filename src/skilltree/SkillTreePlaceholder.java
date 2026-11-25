package skilltree;

import entity.Ship.ShipType;

/**
 * TEMPORARY progress for the skill tree page until unlocking exists
 * (GOG-88, next PR): the first node of each branch is available, the rest
 * are locked. The real nodes come from {@link SkillTreeCatalog} and the
 * real shards from {@link ShardWallet}.
 *
 * @author GoG - Progression (GOG-70)
 */
public final class SkillTreePlaceholder {

	private SkillTreePlaceholder() {
	}

	/**
	 * @param ship
	 *            Ship whose page is shown.
	 * @param node
	 *            Node.
	 * @return Placeholder state: the first node of each branch is
	 *         available, the rest are locked.
	 */
	public static NodeState state(final ShipType ship, final SkillNode node) {
				return node.getIndex() == 0 ? NodeState.AVAILABLE : NodeState.LOCKED;
	}
}
