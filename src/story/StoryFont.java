
package story;

import engine.Core;

import java.awt.Font;
import java.awt.FontFormatException;
import java.io.IOException;

/**
 * Shared Story Mode font.
 *
 * Uses the exact same font loader as the
 * main menu in DrawManager.java.
 */
public final class StoryFont {

    private static Font baseFont;

    private StoryFont() {
        // Utility class.
    }

    private static Font getBaseFont() {
        if (baseFont == null) {
            try {
                baseFont = Core.getFileManager()
                        .loadFont(14f);
            } catch (IOException
                     | FontFormatException
                     | NullPointerException e) {

                System.err.println(
                        "Could not load game font: "
                                + e.getMessage());

                baseFont = new Font(
                        Font.MONOSPACED,
                        Font.PLAIN,
                        14);
            }
        }

        return baseFont;
    }

    public static Font regular(float size) {
        return getBaseFont().deriveFont(
                Font.PLAIN, size);
    }

    public static Font bold(float size) {
        return getBaseFont().deriveFont(
                Font.BOLD, size);
    }
}
