package skilltree;

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
import java.util.Properties;

/**
 * Reads and writes the small key=value save files of the skill tree
 * (shards, unlocked nodes). Files sit next to the other saves, or in
 * game.saveDirectory when that is set (tests). Writing goes through a
 * temporary file, so a crash never leaves half a file.
 *
 * @author GoG - Progression (GOG-70)
 */
final class SaveFile {

	private SaveFile() {
	}

	/**
	 * @param name
	 *            File name, e.g. "shards".
	 * @return Where that save file lives.
	 */
	static File locate(final String name) {
		String saveDirectory = System.getProperty("game.saveDirectory");
		if (saveDirectory != null && !saveDirectory.trim().isEmpty())
			return new File(saveDirectory, name);
		try {
			String path = SaveFile.class.getProtectionDomain().getCodeSource()
					.getLocation().getPath();
			path = URLDecoder.decode(path, "UTF-8");
			return new File(new File(path).getParent(), name);
		} catch (IOException e) {
			return new File(name);
		}
	}

	/**
	 * @param file
	 *            File to read.
	 * @return Its values; empty if it does not exist or can't be read.
	 */
	static Properties read(final File file) {
		Properties values = new Properties();
		if (!file.exists())
			return values;
		try (Reader reader = Files.newBufferedReader(file.toPath(),
				StandardCharsets.UTF_8)) {
			values.load(reader);
		} catch (IOException | IllegalArgumentException e) {
			values.clear();
		}
		return values;
	}

	/**
	 * @param file
	 *            File to write.
	 * @param values
	 *            Values to save.
	 * @param comment
	 *            First line of the file.
	 * @return False if the file could not be written.
	 */
	static boolean write(final File file, final Properties values,
			final String comment) {
		try {
			Path path = file.toPath();
			Path temp = path.resolveSibling(file.getName() + ".tmp");
			try (Writer writer = Files.newBufferedWriter(temp,
					StandardCharsets.UTF_8)) {
				values.store(writer, comment);
			}
			try {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING,
						StandardCopyOption.ATOMIC_MOVE);
			} catch (AtomicMoveNotSupportedException e) {
				Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
			}
			return true;
		} catch (IOException e) {
			return false;
		}
	}

	/**
	 * @param values
	 *            Saved values.
	 * @param key
	 *            Key to read.
	 * @return The value as a number of 0 or more; 0 if missing or broken.
	 */
	static int count(final Properties values, final String key) {
		try {
			return Math.max(0, Integer.parseInt(values.getProperty(key, "0")
					.trim()));
		} catch (NumberFormatException e) {
			return 0;
		}
	}
}
