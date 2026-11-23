package engine;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Which items are equipped for each game mode (GOG-50): 3 slots for
 * Endless and 1 for Story. Items are the Item System's equipped items
 * (Cargo Pod, Magnet Core, Life Capsule, Time Crystal), named by their
 * item id, e.g. "magnet". The same item can be in both modes, but only
 * once per mode. Saved to the "item-loadout" file next to the other
 * saves.
 *
 * @author GoG - Progression (GOG-70)
 */
public final class ItemLoadout {

	/** Game mode with its number of item slots. */
	public enum Mode {
		/** Endless runs: 3 slots. */
		ENDLESS("Endless", 3),
		/** Story runs: 1 slot. */
		STORY("Story", 1);

		/** Name shown to the player. */
		private final String displayName;
		/** Number of slots. */
		private final int slots;

		Mode(final String displayName, final int slots) {
			this.displayName = displayName;
			this.slots = slots;
		}

		/** @return Name shown to the player. */
		public String getDisplayName() {
			return this.displayName;
		}

		/** @return Number of item slots in this mode. */
		public int getSlots() {
			return this.slots;
		}
	}

	/** Save file name. */
	private static final String FILE_NAME = "item-loadout";
	/** Shared instance. */
	private static ItemLoadout instance;

	/** Item id in each slot of each mode; null = empty. */
	private final Map<Mode, String[]> slots =
			new EnumMap<Mode, String[]>(Mode.class);
	/** Where the loadout is saved. */
	private final File file;
	/** True if the save file did not exist when loading. */
	private final boolean isNew;
	/** Game logger. */
	private final Logger logger = Logger.getLogger(ItemLoadout.class
			.getName());

	/**
	 * @param file
	 *            Save file to read and write.
	 */
	private ItemLoadout(final File file) {
		this.file = file;
		for (Mode mode : Mode.values())
			this.slots.put(mode, new String[mode.getSlots()]);
		this.isNew = !file.exists();
		load();
	}

	/**
	 * @return The shared loadout, saved next to the game. The first time
	 *         ever, it starts with the upgrade that was switched on before
	 *         items existed, so players keep what they had.
	 */
	public static synchronized ItemLoadout getInstance() {
		if (instance == null) {
			instance = new ItemLoadout(defaultFile());
			if (instance.isNew)
				instance.importOldUpgrades(PurchaseManager.getInstance()
						.getActiveUpgrades().keySet());
		}
		return instance;
	}

	/**
	 * A loadout saved to a given file, for tests.
	 *
	 * @param file
	 *            Save file.
	 * @return A new loadout reading and writing that file.
	 */
	public static ItemLoadout forFile(final File file) {
		return new ItemLoadout(file);
	}

	/**
	 * @param mode
	 *            Game mode.
	 * @param slot
	 *            Slot number, from 0.
	 * @return Item id in the slot, or null if it is empty.
	 */
	public String get(final Mode mode, final int slot) {
		String[] modeSlots = this.slots.get(mode);
		return slot < 0 || slot >= modeSlots.length ? null : modeSlots[slot];
	}

	/**
	 * @param mode
	 *            Game mode.
	 * @return Equipped item ids of the mode, in slot order, without empty
	 *         slots.
	 */
	public List<String> getEquipped(final Mode mode) {
		List<String> equipped = new ArrayList<String>();
		for (String itemId : this.slots.get(mode))
			if (itemId != null)
				equipped.add(itemId);
		return equipped;
	}

	/**
	 * @param mode
	 *            Game mode.
	 * @param itemId
	 *            Item id.
	 * @return True if the item is in one of the mode's slots.
	 */
	public boolean contains(final Mode mode, final String itemId) {
		return getEquipped(mode).contains(itemId);
	}

	/**
	 * Puts an item in a slot. If the item was in another slot of the same
	 * mode, that slot is emptied (an item is only once per mode).
	 *
	 * @param mode
	 *            Game mode.
	 * @param slot
	 *            Slot number, from 0.
	 * @param itemId
	 *            Item id to equip.
	 * @return False if the slot does not exist or saving failed (then
	 *         nothing changes).
	 */
	public boolean equip(final Mode mode, final int slot,
			final String itemId) {
		String[] modeSlots = this.slots.get(mode);
		if (itemId == null || slot < 0 || slot >= modeSlots.length)
			return false;
		String[] before = modeSlots.clone();
		for (int i = 0; i < modeSlots.length; i++)
			if (itemId.equals(modeSlots[i]))
				modeSlots[i] = null;
		modeSlots[slot] = itemId;
		if (!save()) {
			System.arraycopy(before, 0, modeSlots, 0, before.length);
			return false;
		}
		return true;
	}

	/**
	 * Empties a slot.
	 *
	 * @param mode
	 *            Game mode.
	 * @param slot
	 *            Slot number, from 0.
	 * @return False if the slot does not exist or saving failed.
	 */
	public boolean clear(final Mode mode, final int slot) {
		String[] modeSlots = this.slots.get(mode);
		if (slot < 0 || slot >= modeSlots.length)
			return false;
		String before = modeSlots[slot];
		modeSlots[slot] = null;
		if (!save()) {
			modeSlots[slot] = before;
			return false;
		}
		return true;
	}

	/**
	 * Puts the given items (the upgrades that were on before items
	 * existed) into the first free slots of both modes.
	 *
	 * @param itemIds
	 *            Item ids to equip.
	 */
	void importOldUpgrades(final Collection<String> itemIds) {
		for (Mode mode : Mode.values()) {
			String[] modeSlots = this.slots.get(mode);
			int free = 0;
			for (String itemId : itemIds) {
				while (free < modeSlots.length && modeSlots[free] != null)
					free++;
				if (free >= modeSlots.length)
					break;
				if (!contains(mode, itemId))
					modeSlots[free] = itemId;
			}
		}
		save();
	}

	// --- Saving ---

	private void load() {
		if (!this.file.exists())
			return;
		Properties saved = new Properties();
		try (Reader reader = Files.newBufferedReader(this.file.toPath(),
				StandardCharsets.UTF_8)) {
			saved.load(reader);
		} catch (IOException | IllegalArgumentException e) {
			this.logger.warning("Couldn't load the item loadout, starting"
					+ " empty.");
			return;
		}
		for (Mode mode : Mode.values()) {
			String[] modeSlots = this.slots.get(mode);
			for (int i = 0; i < modeSlots.length; i++) {
				String value = saved.getProperty(key(mode, i), "").trim();
				if (!value.isEmpty() && !contains(mode, value))
					modeSlots[i] = value;
			}
		}
	}

	private boolean save() {
		Properties saved = new Properties();
		for (Mode mode : Mode.values()) {
			String[] modeSlots = this.slots.get(mode);
			for (int i = 0; i < modeSlots.length; i++)
				saved.setProperty(key(mode, i),
						modeSlots[i] == null ? "" : modeSlots[i]);
		}
		try {
			Path path = this.file.toPath();
			Path temp = path.resolveSibling(FILE_NAME + ".tmp");
			try (Writer writer = Files.newBufferedWriter(temp,
					StandardCharsets.UTF_8)) {
				saved.store(writer, "Items equipped per game mode (GOG-50)");
			}
			try {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
			}
			return true;
		} catch (IOException e) {
			this.logger.warning("Couldn't save the item loadout: "
					+ e.getMessage());
			return false;
		}
	}

	private static String key(final Mode mode, final int slot) {
		return mode.name().toLowerCase() + "." + slot;
	}

	/**
	 * @return The save file: in game.saveDirectory if set (tests),
	 *         otherwise next to the game like the other saves.
	 */
	private static File defaultFile() {
		String saveDirectory = System.getProperty("game.saveDirectory");
		if (saveDirectory != null && !saveDirectory.trim().isEmpty())
			return new File(saveDirectory, FILE_NAME);
		try {
			String path = ItemLoadout.class.getProtectionDomain()
					.getCodeSource().getLocation().getPath();
			path = URLDecoder.decode(path, "UTF-8");
			return new File(new File(path).getParent(), FILE_NAME);
		} catch (IOException e) {
			return new File(FILE_NAME);
		}
	}
}
