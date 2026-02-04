import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.swing.GstVideoComponent;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Manages a GStreamer pipeline for receiving video and audio over UDP/RTP.
 * Uses auto elements (decodebin, autovideosink, autoaudiosink) for automatic codec detection.
 */
public class GStreamerPipeline {
    private static final Logger logger = Logger.getLogger(GStreamerPipeline.class.getName());

    private final Pipeline pipeline;
    private final Element videoUdpSrc;
    private final GstVideoComponent videoComponent;

    /**
     * Creates a new GStreamer pipeline.
     *
     * @param videoComponent The video component for display
     * @param videoCodec Video codec encoding name (e.g., "H264", "H265", "VP8", "VP9", "AV1")
     * @param videoPort Video UDP port
     * @param audioCodec Audio codec encoding name (e.g., "OPUS", "PCMU")
     * @param audioPort Audio UDP port
     */
    public GStreamerPipeline(GstVideoComponent videoComponent, String videoCodec, int videoPort,
                            String audioCodec, int audioPort) {
        logger.info(String.format("Creating pipeline: Video=%s:%d, Audio=%s:%d",
                                  videoCodec, videoPort, audioCodec, audioPort));
        this.videoComponent = videoComponent;
        this.pipeline = new Pipeline("fpv-pipeline");

        // Build video branch
        videoUdpSrc = buildVideoBranch(videoCodec, videoPort);

        // Build audio branch
        buildAudioBranch(audioCodec, audioPort);

        logger.info("Pipeline created successfully");
    }

    private Element buildVideoBranch(String videoCodec, int videoPort) {
        logger.info("Building video branch: " + videoCodec + " on port " + videoPort);

        // Video source
        Element udpSrc = ElementFactory.make("udpsrc", "video_src");
        udpSrc.set("port", videoPort);
        udpSrc.set("caps", Caps.fromString(
            "application/x-rtp, media=video, encoding-name=" + videoCodec + ", clock-rate=90000"));
        logger.fine("Created udpsrc for video");

        // RTP jitter buffer
        Element jitter = ElementFactory.make("rtpjitterbuffer", "jitter");
        jitter.set("latency", 0);

        // Queue for buffering
        Element queue = ElementFactory.make("queue", "video_queue");
        queue.set("max-size-buffers", 1);

        // RTP depayloader
        Element depay = ElementFactory.make(getDepayloader(videoCodec), "video_depay");

        // Parser (if needed)
        Element parse = createVideoParser(videoCodec);

        // Explicit decoder for low latency
        Element decoder = createVideoDecoder(videoCodec);

        // Video converter
        Element convert = ElementFactory.make("videoconvert", "video_convert");

        // Video sink
        Element sink = videoComponent.getElement();
        sink.set("sync", false);
        sink.set("async", false);

        // Add elements to pipeline
        if (parse != null) {
            pipeline.addMany(udpSrc, jitter, queue, depay, parse, decoder, convert, sink);
            Element.linkMany(udpSrc, jitter, queue, depay, parse, decoder, convert, sink);
        } else {
            pipeline.addMany(udpSrc, jitter, queue, depay, decoder, convert, sink);
            Element.linkMany(udpSrc, jitter, queue, depay, decoder, convert, sink);
        }

        logger.info("Video branch built successfully");
        return udpSrc;
    }

    private void buildAudioBranch(String audioCodec, int audioPort) {
        logger.info("Building audio branch: " + audioCodec + " on port " + audioPort);

        // Audio source
        Element udpSrc = ElementFactory.make("udpsrc", "audio_src");
        udpSrc.set("port", audioPort);
        udpSrc.set("caps", Caps.fromString(
            "application/x-rtp, media=audio, encoding-name=" + audioCodec));
        logger.fine("Created udpsrc for audio");

        // Queue for buffering
        Element queue = ElementFactory.make("queue", "audio_queue");
        queue.set("max-size-buffers", 1);

        // RTP depayloader
        Element depay = ElementFactory.make(getAudioDepayloader(audioCodec), "audio_depay");

        // Explicit audio decoder for low latency
        Element decoder = createAudioDecoder(audioCodec);

        // Audio converter
        Element convert = ElementFactory.make("audioconvert", "audio_convert");

        // Auto audio sink (automatically selects the best audio sink for the platform)
        Element sink = ElementFactory.make("autoaudiosink", "audio_sink");
        sink.set("sync", false);

        // Add elements to pipeline
        pipeline.addMany(udpSrc, queue, depay, decoder, convert, sink);

        // Link elements
        Element.linkMany(udpSrc, queue, depay, decoder, convert, sink);

        logger.info("Audio branch built successfully");
    }

    private String getDepayloader(String codec) {
        switch (codec.toUpperCase()) {
            case "H265":
                return "rtph265depay";
            case "VP8":
                return "rtpvp8depay";
            case "VP9":
                return "rtpvp9depay";
            case "AV1":
                return "rtpav1depay";
            case "H264":
            default:
                return "rtph264depay";
        }
    }

    private String getAudioDepayloader(String codec) {
        // Only OPUS is supported
        return "rtpopusdepay";
    }

    private Element createVideoParser(String codec) {
        String parserName = null;
        switch (codec.toUpperCase()) {
            case "H264":
                parserName = "h264parse";
                break;
            case "H265":
                parserName = "h265parse";
                break;
            case "VP9":
                parserName = "vp9parse";
                break;
            case "AV1":
                parserName = "av1parse";
                break;
            case "VP8":
                // VP8 doesn't need a parser
                return null;
        }

        if (parserName != null) {
            try {
                return ElementFactory.make(parserName, "video_parse");
            } catch (Exception e) {
                logger.warning("Failed to create parser " + parserName + ": " + e.getMessage());
                return null;
            }
        }
        return null;
    }

    private Element createVideoDecoder(String codec) {
        String os = System.getProperty("os.name").toLowerCase();
        String[] decoders = null;

        switch (codec.toUpperCase()) {
            case "H264":
                if (os.contains("mac") || os.contains("darwin")) {
                    decoders = new String[]{"vtdec_hw", "avdec_h264"};
                } else if (os.contains("win")) {
                    decoders = new String[]{"d3d12h264dec", "avdec_h264"};
                } else if (os.contains("linux")) {
                    decoders = new String[]{"nvh264dec", "vah264dec", "avdec_h264"};
                } else {
                    decoders = new String[]{"avdec_h264"};
                }
                break;
            case "H265":
                if (os.contains("mac") || os.contains("darwin")) {
                    decoders = new String[]{"vtdec_hw", "avdec_h265"};
                } else if (os.contains("win")) {
                    decoders = new String[]{"d3d12h265dec", "avdec_h265"};
                } else if (os.contains("linux")) {
                    decoders = new String[]{"nvh265dec", "vah265dec", "avdec_h265"};
                } else {
                    decoders = new String[]{"avdec_h265"};
                }
                break;
            case "VP8":
                if (os.contains("win")) {
                    decoders = new String[]{"d3d12vp8dec", "vp8dec"};
                } else if (os.contains("linux")) {
                    decoders = new String[]{"nvvp8dec", "vavp8dec", "vp8dec"};
                } else {
                    decoders = new String[]{"vp8dec"};
                }
                break;
            case "VP9":
                if (os.contains("win")) {
                    decoders = new String[]{"d3d12vp9dec", "vp9dec"};
                } else if (os.contains("linux")) {
                    decoders = new String[]{"nvvp9dec", "vavp9dec", "vp9dec"};
                } else {
                    decoders = new String[]{"vp9dec"};
                }
                break;
            case "AV1":
                if (os.contains("win")) {
                    decoders = new String[]{"d3d12av1dec", "dav1ddec"};
                } else if (os.contains("linux")) {
                    decoders = new String[]{"nvav1dec", "vaav1dec", "dav1ddec"};
                } else {
                    decoders = new String[]{"dav1ddec"};
                }
                break;
            default:
                decoders = new String[]{"avdec_h264"};
        }

        // Try decoders in order
        for (String decoderName : decoders) {
            try {
                Element decoder = ElementFactory.make(decoderName, "video_decoder");
                logger.info("Using video decoder: " + decoderName);

                // Set low-latency properties
                if (decoderName.equals("vp9dec")) {
                    try {
                        decoder.set("threads", 4);
                    } catch (Exception e) {
                        // Property might not be supported
                    }
                }

                return decoder;
            } catch (Exception e) {
                logger.fine("Decoder " + decoderName + " not available, trying next...");
            }
        }

        throw new RuntimeException("No suitable video decoder found for " + codec);
    }

    private Element createAudioDecoder(String codec) {
        // Only OPUS is supported
        try {
            Element decoder = ElementFactory.make("opusdec", "audio_decoder");
            logger.info("Using audio decoder: opusdec");
            return decoder;
        } catch (Exception e) {
            throw new RuntimeException("Failed to create OPUS decoder: " + e.getMessage());
        }
    }

    /**
     * Starts playing the pipeline.
     */
    public void play() {
        logger.info("Starting pipeline");
        pipeline.play();
    }

    /**
     * Stops the pipeline.
     */
    public void stop() {
        logger.info("Stopping pipeline");
        pipeline.stop();
    }

    /**
     * Disposes the pipeline and releases resources.
     */
    public void dispose() {
        logger.info("Disposing pipeline");
        pipeline.dispose();
    }

    /**
     * Returns the underlying GStreamer Pipeline object.
     */
    public Pipeline getPipeline() {
        return pipeline;
    }

    /**
     * Returns the video UDP source element.
     */
    public Element getVideoUdpSrc() {
        return videoUdpSrc;
    }

    /**
     * Enables UDP timeout on the video source.
     *
     * @param timeoutNanos Timeout in nanoseconds
     */
    public void enableTimeout(long timeoutNanos) {
        if (videoUdpSrc != null) {
            videoUdpSrc.set("timeout", timeoutNanos);
        }
    }
}
