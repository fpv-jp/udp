import org.freedesktop.gstreamer.Bin;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.swing.GstVideoComponent;

import java.util.logging.Logger;

/**
 * Manages a GStreamer pipeline for receiving video and audio over UDP/RTP.
 * Uses Gst.parseLaunch() to build the pipeline from a description string.
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
     * @param audioCodec Audio codec encoding name (e.g., "OPUS")
     * @param audioPort Audio UDP port
     */
    public GStreamerPipeline(GstVideoComponent videoComponent, String videoCodec, int videoPort,
                            String audioCodec, int audioPort) {
        logger.info(String.format("Creating pipeline: Video=%s:%d, Audio=%s:%d",
                                  videoCodec, videoPort, audioCodec, audioPort));
        this.videoComponent = videoComponent;

        // Build pipeline description and parse
        String description = buildPipelineDescription(videoCodec, videoPort, audioCodec, audioPort);
        logger.fine("Pipeline description: " + description);
        this.pipeline = (Pipeline) Gst.parseLaunch(description);

        // Replace fakesink placeholder with the actual video component
        Element fakeSink = pipeline.getElementByName("video_sink_placeholder");
        Element videoConvert = pipeline.getElementByName("video_convert");
        pipeline.remove(fakeSink);

        Element videoSink = videoComponent.getElement();
        videoSink.set("sync", false);
        videoSink.set("async", false);
        pipeline.add(videoSink);
        videoConvert.link(videoSink);

        // Get named elements for later use
        this.videoUdpSrc = pipeline.getElementByName("video_src");

        logger.info("Pipeline created successfully");
    }

    private String buildPipelineDescription(String videoCodec, int videoPort,
                                            String audioCodec, int audioPort) {
        return buildVideoBranch(videoCodec, videoPort) + " " + buildAudioBranch(audioCodec, audioPort);
    }

    private String buildVideoBranch(String videoCodec, int videoPort) {
        String depayloader = getDepayloaderName(videoCodec);
        String parser = getParserName(videoCodec);
        String decoder = getVideoDecoderName(videoCodec);

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
            "udpsrc name=video_src port=%d caps=\"application/x-rtp, media=video, encoding-name=%s, clock-rate=90000\"",
            videoPort, videoCodec));
        sb.append(" ! rtpjitterbuffer latency=0");
        sb.append(" ! queue max-size-buffers=1");
        sb.append(" ! ").append(depayloader);
        if (parser != null) {
            sb.append(" ! ").append(parser);
        }
        sb.append(" ! ").append(decoder);
        sb.append(" ! videoconvert name=video_convert");
        sb.append(" ! fakesink name=video_sink_placeholder");

        return sb.toString();
    }

    private String buildAudioBranch(String audioCodec, int audioPort) {
        return String.format(
            "udpsrc port=%d caps=\"application/x-rtp, media=audio, encoding-name=%s\"" +
            " ! queue max-size-buffers=1" +
            " ! rtpopusdepay" +
            " ! opusdec" +
            " ! audioconvert" +
            " ! autoaudiosink sync=false",
            audioPort, audioCodec);
    }

    private String getDepayloaderName(String codec) {
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

    private String getParserName(String codec) {
        switch (codec.toUpperCase()) {
            case "H264":
                return "h264parse";
            case "H265":
                return "h265parse";
            case "VP9":
                return "vp9parse";
            case "AV1":
                return "av1parse";
            case "VP8":
            default:
                return null;
        }
    }

    /**
     * Returns the best available video decoder name for the given codec.
     * Tries platform-specific hardware decoders first, falling back to software decoders.
     */
    private String getVideoDecoderName(String codec) {
        String os = System.getProperty("os.name").toLowerCase();
        String[] decoders;

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

        for (String decoderName : decoders) {
            if (ElementFactory.find(decoderName) != null) {
                logger.info("Using video decoder: " + decoderName);
                return decoderName;
            }
            logger.fine("Decoder " + decoderName + " not available, trying next...");
        }

        throw new RuntimeException("No suitable video decoder found for " + codec);
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
