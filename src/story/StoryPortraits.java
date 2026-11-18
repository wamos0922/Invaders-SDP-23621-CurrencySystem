
package story;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Handles character portraits for Story Mode.
 *
 * - Uses NeutralRixley.png as the size reference
 * - Preserves every portrait's aspect ratio
 * - Prevents stretching or distortion
 * - Keeps portraits aligned consistently
 * - Does not draw additional borders or backgrounds
 * - Supports emotion-based portrait switching
 */
public final class StoryPortraits {

    private static final String PORTRAIT_FOLDER =
            "res/story/portraits/";

    private static final String REFERENCE_PORTRAIT =
            PORTRAIT_FOLDER
                    + "Rixley/NeutralRixley.png";

    private static final Map<String, BufferedImage> CACHE =
            new HashMap<>();

    // Maximum available portrait space in StoryPanel.
    private static final int MAX_PORTRAIT_WIDTH = 110;
    private static final int MAX_PORTRAIT_HEIGHT = 140;

    private StoryPortraits() {
    }

    // =========================================
    // CHECK PORTRAIT AVAILABILITY
    // =========================================

    public static boolean hasPortrait(Dialogue dialogue) {

        if (dialogue == null ||
                dialogue.getType() ==
                        Dialogue.DialogueType.NARRATION) {
            return false;
        }

        return getFolder(
                dialogue.getPortraitCharacter()
        ) != null;
    }

    // =========================================
    // CHARACTER FOLDERS
    // =========================================

    private static String getFolder(String character) {

        if (character == null) {
            return null;
        }

        switch (character.trim().toLowerCase(Locale.ROOT)) {

            case "protagonist":
                return "Protagonist";

            case "rixley":
                return "Rixley";

            case "blorp":
                return "Blorp";

            case "dr. draco":
            case "draco":
                return "Draco";

            default:
                // Aliens and other characters
                // will be supported later.
                return null;
        }
    }

    // =========================================
    // EMOTION MAPPING
    // =========================================

    private static String getExpression(
            String character,
            String emotion) {

        String e = emotion == null
                ? "normal"
                : emotion.trim().toLowerCase(Locale.ROOT);

        // Normal always means Neutral.
        if (e.equals("normal") || e.equals("neutral")) {
            return "Neutral";
        }

        switch (character) {

            case "Blorp":

                switch (e) {

                    case "happy":
                        return "Happy";

                    case "panic":
                    case "panicked":
                    case "panicking":
                    case "shocked":
                        return "Panic";

                    case "sad":
                        return "Sad";

                    case "yummy":
                        return "Yummy";

                    case "confused":
                        return "Neutral";

                    default:
                        return "Neutral";
                }

            case "Rixley":

                switch (e) {

                    case "angry":
                        return "Angry";

                    case "confident":
                        return "Confident";

                    case "confused":
                        return "Confused";

                    case "shocked":
                        return "Shocked";

                    case "panic":
                    case "panicked":
                    case "panicking":
                        return "Panic";

                    default:
                        return "Neutral";
                }

            case "Protagonist":

                switch (e) {

                    case "angry":
                        return "Angry";

                    case "confident":
                    case "energetic":
                    case "energitic":
                        return "Confident";

                    case "confused":
                        return "Confused";

                    case "shocked":
                        return "Shocked";

                    case "panic":
                    case "panicked":
                    case "panicking":
                        return "Panicked";

                    default:
                        return "Neutral";
                }

            case "Draco":

                if (e.equals("happy")) {
                    return "Happy";
                }

                return "Neutral";

            default:
                return "Neutral";
        }
    }

    // =========================================
    // LOAD AND CACHE IMAGES
    // =========================================

    private static BufferedImage loadImage(String path) {

        if (CACHE.containsKey(path)) {
            return CACHE.get(path);
        }

        BufferedImage image = null;

        try {

            File file = new File(path);

            if (file.isFile()) {

                image = ImageIO.read(file);

            } else {

                System.err.println(
                        "[STORY] Missing portrait: " + path
                );
            }

        } catch (IOException e) {

            System.err.println(
                    "[STORY] Unable to load portrait: "
                            + path
            );
        }

        CACHE.put(path, image);

        return image;
    }

    // =========================================
    // GET REFERENCE DISPLAY SIZE
    // =========================================

    private static Dimension getReferenceSize() {

        BufferedImage reference =
                loadImage(REFERENCE_PORTRAIT);

        // If Rixley's reference is unavailable,
        // use a square fallback without stretching.
        if (reference == null) {
            return new Dimension(110, 110);
        }

        int originalWidth = reference.getWidth();
        int originalHeight = reference.getHeight();

        // Scale Rixley's image to fit the available
        // space while preserving its original ratio.
        double scale = Math.min(
                MAX_PORTRAIT_WIDTH / (double) originalWidth,
                MAX_PORTRAIT_HEIGHT / (double) originalHeight
        );

        int width = Math.max(
                1,
                (int) Math.round(originalWidth * scale)
        );

        int height = Math.max(
                1,
                (int) Math.round(originalHeight * scale)
        );

        return new Dimension(width, height);
    }

    // =========================================
    // DRAW PORTRAIT
    // =========================================

    public static void draw(
            Graphics2D g,
            Dialogue dialogue,
            int x,
            int y) {

        if (!hasPortrait(dialogue)) {
            return;
        }

        String character =
                getFolder(dialogue.getPortraitCharacter());

        String expression =
                getExpression(
                        character,
                        dialogue.getEmotion()
                );

        String path =
                PORTRAIT_FOLDER
                        + character + "/"
                        + expression
                        + character + ".png";

        BufferedImage image = loadImage(path);

        // Fallback to Neutral when an emotion
        // does not have its own PNG.
        if (image == null &&
                !expression.equals("Neutral")) {

            path =
                    PORTRAIT_FOLDER
                            + character + "/Neutral"
                            + character + ".png";

            image = loadImage(path);
        }

        if (image == null) {
            return;
        }

        // Get the display dimensions based on Rixley.
        Dimension referenceSize =
                getReferenceSize();

        int targetWidth = referenceSize.width;
        int targetHeight = referenceSize.height;

        int originalWidth = image.getWidth();
        int originalHeight = image.getHeight();

        // =====================================
        // PRESERVE ORIGINAL ASPECT RATIO
        // =====================================

        double scale = Math.min(
                targetWidth / (double) originalWidth,
                targetHeight / (double) originalHeight
        );

        int drawWidth = Math.max(
                1,
                (int) Math.round(originalWidth * scale)
        );

        int drawHeight = Math.max(
                1,
                (int) Math.round(originalHeight * scale)
        );

        // Keep the portrait horizontally centered
        // inside Rixley's reference area.
        //
        // The top edge remains aligned so every
        // portrait starts at a consistent height.
        int drawX =
                x + (targetWidth - drawWidth) / 2;

        int drawY = y;

        // =====================================
        // PIXEL-ART RENDERING
        // =====================================

        Object previousInterpolation =
                g.getRenderingHint(
                        RenderingHints.KEY_INTERPOLATION
                );

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR
        );

        // Draw the portrait without stretching.
        // No added rectangle or border.
        g.drawImage(
                image,
                drawX,
                drawY,
                drawWidth,
                drawHeight,
                null
        );

        if (previousInterpolation != null) {

            g.setRenderingHint(
                    RenderingHints.KEY_INTERPOLATION,
                    previousInterpolation
            );
        }
    }
}