package skilltree;

/**
 * How a node looks on the skill tree page.
 *
 * @author GoG - Progression (GOG-70)
 */
public enum NodeState {
	/** The node above it is not owned yet. */
	LOCKED,
	/** Next in its branch: can be bought with enough shards. */
	AVAILABLE,
	/** Bought. */
	OWNED
}
