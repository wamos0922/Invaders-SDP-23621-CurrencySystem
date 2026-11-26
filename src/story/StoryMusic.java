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
 * GOG-35: Music for Story Mode cutscenes only.
 *
 * WAV files are streamed on a daemon thread, not loaded as huge Clips.
 * A cue shared by consecutive panels continues at the same playback position.
 * When a cue ends, it loops from its configured beginning (e.g. 0:53).
 * Panel 27 is the sole exception to the cutscene-only rule: its soundtrack
 * stays alive through Level 40, and stops when that battle ends.
 */
public final class StoryMusic {
    private static Playback active;

    private StoryMusic() { }

    public static void playForPanel(int panel, int dialogueLine) {
        switch (panel) {
            case 1: case 2: case 3: case 4: case 5:
            case 12:
                play("base.wav", 0);
                break;
            case 6:
                stop();
                break;
            case 7:
                play("discovery.wav", 0);
                break;
            case 8: case 17: case 19: case 20:
                play("cornered.wav", 0);
                break;
            case 9:
                play("panel9.wav", 0);
                break;
            case 10: case 11:
                play("Blorp.wav", 0);
                break;
            case 13:
                // panel.13.3: "Suddenly, a massive alarm began..."
                play(dialogueLine >= 3 ? "alarm.wav" : "base.wav", 0);
                break;
            case 14: case 15: case 16:
                play("escaped.wav", 0);
                break;
            case 18:
                play("fast.wav", 0);
                break;
            case 21: case 22: case 23: case 26:
                play("backup.wav", 53);
                break;
            case 24: case 25:
                play("storytime.wav", 0);
                break;
            case 27:
                // Exact filename in the repository uses capital B.
                play("finalBoss.wav", 0);
                break;
            case 28:
                play("celebration.wav", 0);
                break;
            case 29: case 30: case 31:
                play("lastpush.wav", 0);
                break;
            case 32: case 33:
                play("ending.wav", 71);
                break;
            default:
                stop();
        }
    }

    /** Start only if this is a new cue; do not restart across adjacent panels. */
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

    /** Called on every normal cutscene exit and on Level 40 clear/game over. */
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
                // Each repeat opens a fresh stream and skips to the same
                // configured starting second. No whole-file buffering.
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
                    // EOF: loop from the same offset. For a 53-second cue,
                    // every repeat starts again at 0:53, never at 0:00.
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
            // SourceDataLine expects PCM. WAV can also contain float or
            // other encodings; Java Sound conversion handles those when available.
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
