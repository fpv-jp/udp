import java.util.logging.Logger;

/**
 * Holds receiver settings (codecs, ports) and notifies listeners on change.
 * No Swing or GStreamer dependencies.
 */
public class ReceiverSettings {
    private static final Logger logger = Logger.getLogger(ReceiverSettings.class.getName());

    // Video codecs
    public static final String CODEC_H264 = "H264";
    public static final String CODEC_H265 = "H265";
    public static final String CODEC_VP8 = "VP8";
    public static final String CODEC_VP9 = "VP9";
    public static final String CODEC_AV1 = "AV1";

    // Audio codecs
    public static final String CODEC_OPUS = "OPUS";

    public static final long UDP_TIMEOUT_NANOS = 3_000_000_000L;

    private String videoCodec = CODEC_H264;
    private int videoPort = 5000;
    private String audioCodec = CODEC_OPUS;
    private int audioPort = 5001;
    private boolean timeoutEnabled = false;

    private Runnable onSettingsChanged;

    public void setOnSettingsChanged(Runnable callback) {
        this.onSettingsChanged = callback;
    }

    // --- Getters ---

    public String getVideoCodec() {
        return videoCodec;
    }

    public int getVideoPort() {
        return videoPort;
    }

    public String getAudioCodec() {
        return audioCodec;
    }

    public int getAudioPort() {
        return audioPort;
    }

    public boolean isTimeoutEnabled() {
        return timeoutEnabled;
    }

    // --- Setters ---

    public void setVideoCodec(String codec) {
        if (codec.equals(this.videoCodec)) {
            return;
        }
        logger.info("Switching video codec to: " + codec);
        this.videoCodec = codec;
        this.timeoutEnabled = false;
        fireChanged();
    }

    public void setVideoPort(int port) {
        if (port == this.videoPort) {
            return;
        }
        logger.info("Switching video port to: " + port);
        this.videoPort = port;
        this.timeoutEnabled = false;
        fireChanged();
    }

    public void setAudioCodec(String codec) {
        if (codec.equals(this.audioCodec)) {
            return;
        }
        logger.info("Switching audio codec to: " + codec);
        this.audioCodec = codec;
        fireChanged();
    }

    public void setAudioPort(int port) {
        if (port == this.audioPort) {
            return;
        }
        logger.info("Switching audio port to: " + port);
        this.audioPort = port;
        fireChanged();
    }

    public void setTimeoutEnabled(boolean enabled) {
        this.timeoutEnabled = enabled;
    }

    // --- Status ---

    public String getStatusText() {
        return "Video: " + videoCodec + "/" + videoPort +
               "  Audio: " + audioCodec + "/" + audioPort;
    }

    private void fireChanged() {
        if (onSettingsChanged != null) {
            onSettingsChanged.run();
        }
    }
}
