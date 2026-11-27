package story;

import engine.soundEngine.Sound;
import engine.soundEngine.SoundManager;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.SourceDataLine;
import java.io.File;
import java.io.IOException;

/**
 * GOG-35: Story Mode cutscene and gameplay music.
 *
 * WAVs are streamed on a daemon thread rather than loaded as huge Clips.
 * Identical consecutive cues do not restart. On EOF, each track loops
 * from its configured start time.
 *
 * Story gameplay tracks:
 *   Levels 1-9:   FirstPhase.wav
 *   Levels 11-19: SecondPhase.wav
 *   Levels 21-29: ThirdPhase.wav
 *   Levels 31-35: FourthPhase.wav
 * Levels 36-39 continue backup.wav at 0:53 from Panel 26.
 * Boss Level 40 continues finalBoss.wav from Panel 27.
 * Boss levels 10, 20 and 30 each have their own looping soundtrack.
 */
public final class StoryMusic {
    private static Playback active;

    private StoryMusic() { }

    /**
     * Choose the gameplay soundtrack without starting audio.
     * Package-independent and testable without a sound device.
     *
     * @return The WAV filename, or null if the level is unsupported.
     */
    public static String trackForLevel(int level) {
        if (level >= 1 && level <= 9) {
            return "FirstPhase.wav";
        }
        if (level == 10) {
            return "Boss10.wav";
        }
        if (level >= 11 && level <= 19) {
            return "SecondPhase.wav";
        }
        if (level == 20) {
            return "Boss20.wav";
        }
        if (level >= 21 && level <= 29) {
            return "ThirdPhase.wav";
        }
        if (level == 30) {
            return "Boss30.wav";
        }
        if (level >= 31 && level <= 35) {
            return "FourthPhase.wav";
        }
        if (level >= 36 && level <= 39) {
            return "backup.wav";
        }
        if (level == 40) {
            return "finalBoss.wav";
        }
        return null;
    }

    /** Select a cutscene soundtrack without starting audio. */
    public static String trackForPanel(int panel, int dialogueLine) {
        switch (panel) {
            case 1: case 2: case 3: case 4: case 5:
            case 12:
                return "base.wav";
            case 6:
                return null;
            case 7:
                return "discovery.wav";
            case 8: case 17: case 19: case 20:
                return "cornered.wav";
            case 9:
                return "panel9.wav";
            case 10: case 11:
                return "Blorp.wav";
            case 13:
                // panel.13.3: "Suddenly, a massive alarm began..."
                return dialogueLine >= 3 ? "alarm.wav" : "base.wav";
            case 14: case 15: case 16:
                return "escaped.wav";
            case 18:
                return "fast.wav";
            case 21: case 22: case 23: case 26:
                return "backup.wav";
            case 24: case 25:
                return "storytime.wav";
            case 27:
                // Exact filename uses capital B.
                return "finalBoss.wav";
            case 28:
                return "celebration.wav";
            case 29: case 30: case 31:
                return "lastpush.wav";
            case 32: case 33:
                return "ending.wav";
            default:
                return null;
        }
    }

    /** Shared starting offset for both gameplay and cutscene cues. */
    private static int startSecondsForTrack(String filename) {
        if ("backup.wav".equals(filename)) {
            return 53;
        }
        if ("ending.wav".equals(filename)) {
            return 71;
        }
        return 0;
    }

    /** Starting offset (seconds) for the selected gameplay cue. */
    public static int startSecondsForLevel(int level) {
        return startSecondsForTrack(trackForLevel(level));
    }

    /** Starting offset (seconds) for the selected cutscene cue. */
    public static int startSecondsForPanel(int panel, int dialogueLine) {
        return startSecondsForTrack(trackForPanel(panel, dialogueLine));
    }

    /**
     * Keep Panel 26 music across normal, successful Story Mode clears of
     * Levels 36-39 only. TitleScreen stops it if the player chooses Exit.
     */
    public static boolean shouldContinueAfterLevel(int level, boolean storyMode,
                                                   boolean clearedNormally) {
        return storyMode && clearedNormally && level >= 36 && level <= 39;
    }

    /**
     * Called when entering (or restarting) a Story Mode gameplay level.
     * This method is NOT used by Endless Mode or Daily Challenges.
     */
    public static void playForLevel(int level) {
        playCue(trackForLevel(level));
    }

    public static void playForPanel(int panel, int dialogueLine) {
        playCue(trackForPanel(panel, dialogueLine));
    }

    private static void playCue(String track) {
        if (track == null) {
            stop();
        } else {
            play(track, startSecondsForTrack(track));
        }
    }

    /** Start only if this is a new cue; do not restart identical cues. */
    private static synchronized void play(String filename, int beginningSeconds) {
        if (active != null && active.running
                && active.filename.equals(filename)
                && active.beginningSeconds == beginningSeconds) {
            return;
        }

        stop();
        SoundManager.getInstance().stopBgm();
        Playback p = new Playback(filename, beginningSeconds);
        active = p;
        Thread thread = new Thread(p, "GOG35-StoryMusic-" + filename);
        p.worker = thread;
        thread.setDaemon(true);
        thread.start();
    }

    /** Stop on gameplay exit, game over, or when switching to other music. */
    public static synchronized void stop() {
        Playback previous = active;
        active = null;
        if (previous != null) {
            previous.running = false;
            SourceDataLine line = previous.line;
            if (line != null) {
                try {
                    line.stop();
                    line.flush();
                    line.close();
                } catch (RuntimeException ignored) {
                    // A simultaneous close from the audio thread is harmless.
                }
            }
            if (previous.worker != null) {
                previous.worker.interrupt();
            }
        }
    }

    private static final class Playback implements Runnable {
        final String filename;
        final int beginningSeconds;
        volatile boolean running = true;
        volatile SourceDataLine line;
        Thread worker;

        Playback(String filename, int beginningSeconds) {
            this.filename = filename;
            this.beginningSeconds = beginningSeconds;
        }

        @Override
        public void run() {
            File file = new File("res/sounds", filename);
            if (!file.isFile()) {
                System.err.println("[GOG-35 AUDIO] Missing " + file.getAbsolutePath());
                running = false;
                return;
            }

            SourceDataLine playbackLine = null;
            try {
                // Reopen a fresh stream at the same offset on each loop.
                while (running) {
                    try (AudioInputStream source = AudioSystem.getAudioInputStream(file)) {
                        AudioFormat sourceFormat = source.getFormat();
                        AudioFormat playbackFormat = choosePlaybackFormat(sourceFormat);

                        try (AudioInputStream pcm = playbackFormat.matches(sourceFormat)
                                ? source
                                : AudioSystem.getAudioInputStream(playbackFormat, source)) {
                            if (playbackLine == null) {
                                DataLine.Info info = new DataLine.Info(
                                        SourceDataLine.class, playbackFormat);
                                playbackLine = (SourceDataLine) AudioSystem.getLine(info);
                                playbackLine.open(playbackFormat);
                                line = playbackLine;
                                System.out.println("[GOG-35 AUDIO] Playing " + filename
                                        + " from " + beginningSeconds + "s (" + playbackFormat + ")");
                            }

                            skipSeconds(pcm, playbackFormat, beginningSeconds);
                            byte[] buffer = new byte[16384];
                            int bytes;
                            while (running && (bytes = pcm.read(buffer, 0, buffer.length)) != -1) {
                                int offset = 0;
                                while (running && offset < bytes) {
                                    SoundManager settings = SoundManager.getInstance();
                                    if (settings.isMuted()) {
                                        playbackLine.stop();
                                        Thread.sleep(40);
                                        continue;
                                    }
                                    updateVolume(playbackLine, settings.getVolume(Sound.Category.BGM));
                                    if (!playbackLine.isRunning()) {
                                        playbackLine.start();
                                    }
                                    int written = playbackLine.write(buffer, offset, bytes - offset);
                                    if (written <= 0) {
                                        break;
                                    }
                                    offset += written;
                                }
                            }
                        }
                    }
                    // EOF: repeat using the same start offset.
                }
            } catch (Exception exception) {
                if (running) {
                    System.err.println("[GOG-35 AUDIO] Failed to play " + filename
                            + ": " + exception.getClass().getSimpleName()
                            + ": " + exception.getMessage());
                    exception.printStackTrace(System.err);
                }
            } finally {
                running = false;
                if (playbackLine != null) {
                    try {
                        playbackLine.stop();
                        playbackLine.flush();
                        playbackLine.close();
                    } catch (RuntimeException ignored) { }
                }
            }
        }

        private static AudioFormat choosePlaybackFormat(AudioFormat source) {
            AudioFormat desired = new AudioFormat(
                    AudioFormat.Encoding.PCM_SIGNED,
                    source.getSampleRate(),
                    16,
                    source.getChannels(),
                    source.getChannels() * 2,
                    source.getSampleRate(),
                    false);
            if (AudioSystem.isConversionSupported(desired, source)) {
                return desired;
            }
            if (AudioSystem.isLineSupported(
                    new DataLine.Info(SourceDataLine.class, source))) {
                return source;
            }
            throw new IllegalArgumentException(
                    "Unsupported WAV audio format: " + source
                            + ". Convert it to PCM signed 16-bit WAV in an audio editor.");
        }

        private static void skipSeconds(AudioInputStream stream,
                                        AudioFormat format, int seconds) throws IOException {
            if (seconds <= 0) return;
            long remaining = Math.round((double) format.getFrameRate() * seconds)
                    * format.getFrameSize();
            byte[] discard = new byte[8192];
            int frameBytes = Math.max(1, format.getFrameSize());
            while (remaining > 0) {
                long skipped = stream.skip(remaining);
                if (skipped > 0) {
                    remaining -= skipped;
                    continue;
                }
                int count = (int) Math.min(remaining, discard.length);
                count -= count % frameBytes;
                if (count <= 0) {
                    throw new IOException("Invalid WAV seek alignment");
                }
                int read = stream.read(discard, 0, count);
                if (read <= 0) {
                    throw new IOException("WAV is shorter than " + seconds + " seconds");
                }
                remaining -= read;
            }
        }

        private static void updateVolume(SourceDataLine line, float volume) {
            if (!line.isControlSupported(FloatControl.Type.MASTER_GAIN)) return;
            FloatControl gain = (FloatControl) line.getControl(
                    FloatControl.Type.MASTER_GAIN);
            float clamped = Math.max(0.0f, Math.min(1.0f, volume));
            float db = clamped == 0f ? gain.getMinimum()
                    : (float) (20.0 * Math.log10(clamped));
            gain.setValue(Math.max(gain.getMinimum(),
                    Math.min(gain.getMaximum(), db)));
        }
    }
}
