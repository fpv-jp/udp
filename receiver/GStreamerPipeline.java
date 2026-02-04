import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.elements.AppSink;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Manages a GStreamer pipeline for receiving video and audio over UDP/RTP.
 * Builds the pipeline element-by-element to avoid parseLaunch + fakesink swap issues.
 */
public class GStreamerPipeline {
    private static final Logger logger = Logger.getLogger(GStreamerPipeline.class.getName());

    private final Pipeline pipeline;
    private final Element videoUdpSrc;
    private final AppSink displaySink;

    public GStreamerPipeline(AppSink displaySink, String videoCodec, int videoPort,
                            String audioCodec, int audioPort) {
        logger.info(String.format("Creating pipeline: Video=%s:%d, Audio=%s:%d",
                                  videoCodec, videoPort, audioCodec, audioPort));
        this.displaySink = displaySink;

        // Log equivalent gst-launch description for debugging
        logPipelineDescription(videoCodec, videoPort, audioCodec, audioPort);

        // Build pipeline element-by-element
        this.pipeline = buildPipeline(videoCodec, videoPort, audioCodec, audioPort);
        this.videoUdpSrc = pipeline.getElementByName("video_src");

        logger.info("Pipeline created successfully");
    }

    // --- Pipeline construction ---

    private Pipeline buildPipeline(String videoCodec, int videoPort,
                                   String audioCodec, int audioPort) {
        Pipeline p = new Pipeline();

        buildVideoChain(p, videoCodec, videoPort);
        buildAudioChain(p, audioCodec, audioPort);

        return p;
    }

    private void buildVideoChain(Pipeline p, String videoCodec, int videoPort) {
        // Source
        Element src = ElementFactory.make("udpsrc", "video_src");
        src.set("port", videoPort);
        src.set("caps", new Caps(
            "application/x-rtp, media=video, encoding-name=" + videoCodec));

        // Queue
        Element queue = ElementFactory.make("queue", null);
        queue.set("max-size-buffers", 3);

        // Depayloader
        Element depay = ElementFactory.make(getDepayloaderName(videoCodec), null);

        // Parser (optional)
        String parserName = getParserName(videoCodec);
        Element parser = parserName != null ? ElementFactory.make(parserName, null) : null;

        // Decoder
        Element decoder = ElementFactory.make(getVideoDecoderName(videoCodec), null);

        // Converter + sink
        Element convert = ElementFactory.make("videoconvert", "video_convert");
        displaySink.set("sync", false);
        displaySink.set("async", false);

        // Assemble chain
        List<Element> chain = new ArrayList<>();
        chain.add(src);
        chain.add(queue);
        chain.add(depay);
        if (parser != null) chain.add(parser);
        chain.add(decoder);
        chain.add(convert);
        chain.add(displaySink);

        for (Element e : chain) {
            p.add(e);
        }
        linkChain(chain);
    }

    private void buildAudioChain(Pipeline p, String audioCodec, int audioPort) {
        Element src = ElementFactory.make("udpsrc", "audio_src");
        src.set("port", audioPort);
        src.set("caps", new Caps(
            "application/x-rtp, media=audio, encoding-name=" + audioCodec));

        Element queue = ElementFactory.make("queue", null);
        queue.set("max-size-buffers", 1);

        Element depay    = ElementFactory.make("rtpopusdepay", null);
        Element dec      = ElementFactory.make("opusdec", null);
        Element convert  = ElementFactory.make("audioconvert", null);
        Element sink     = ElementFactory.make("autoaudiosink", null);
        sink.set("sync", false);

        List<Element> chain = List.of(src, queue, depay, dec, convert, sink);
        for (Element e : chain) {
            p.add(e);
        }
        linkChain(chain);
    }

    private void linkChain(List<Element> chain) {
        for (int i = 0; i < chain.size() - 1; i++) {
            Element a = chain.get(i);
            Element b = chain.get(i + 1);
            boolean ok = a.link(b);
            if (!ok) {
                logger.warning("Failed to link: " + a.getName() + " -> " + b.getName());
            }
        }
    }

    // --- Decoder selection ---

    private String getDepayloaderName(String codec) {
        switch (codec.toUpperCase()) {
            case "H265": return "rtph265depay";
            case "VP8":  return "rtpvp8depay";
            case "VP9":  return "rtpvp9depay";
            case "AV1":  return "rtpav1depay";
            default:     return "rtph264depay";
        }
    }

    private String getParserName(String codec) {
        switch (codec.toUpperCase()) {
            case "H264": return "h264parse";
            case "H265": return "h265parse";
            case "VP9":  return "vp9parse";
            case "AV1":  return "av1parse";
            default:     return null;
        }
    }

    private String getVideoDecoderName(String codec) {
        String os = System.getProperty("os.name").toLowerCase();
        String[] decoders;

        switch (codec.toUpperCase()) {
            case "H264":
                if (os.contains("mac") || os.contains("darwin")) {
                    decoders = new String[]{"avdec_h264", "vtdec"};
                } else if (os.contains("win")) {
                    decoders = new String[]{"d3d12h264dec", "avdec_h264"};
                } else {
                    decoders = new String[]{"nvh264dec", "vah264dec", "avdec_h264"};
                }
                break;
            case "H265":
                if (os.contains("mac") || os.contains("darwin")) {
                    decoders = new String[]{"avdec_h265", "vtdec"};
                } else if (os.contains("win")) {
                    decoders = new String[]{"d3d12h265dec", "avdec_h265"};
                } else {
                    decoders = new String[]{"nvh265dec", "vah265dec", "avdec_h265"};
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

        for (String name : decoders) {
            if (ElementFactory.find(name) != null) {
                logger.info("Using video decoder: " + name);
                return name;
            }
            logger.info("Decoder " + name + " not available, trying next...");
        }

        throw new RuntimeException("No suitable video decoder found for " + codec);
    }

    // --- Logging ---

    private void logPipelineDescription(String videoCodec, int videoPort,
                                        String audioCodec, int audioPort) {
        String parserName = getParserName(videoCodec);
        String decoderName;
        try {
            decoderName = getVideoDecoderName(videoCodec);
        } catch (RuntimeException e) {
            decoderName = "(none found)";
        }

        StringBuilder sb = new StringBuilder();
        sb.append(String.format(
            "udpsrc name=video_src port=%d caps=\"application/x-rtp, media=video, encoding-name=%s\"",
            videoPort, videoCodec));
        sb.append(" ! queue max-size-buffers=3");
        sb.append(" ! ").append(getDepayloaderName(videoCodec));
        if (parserName != null) sb.append(" ! ").append(parserName);
        sb.append(" ! ").append(decoderName);
        sb.append(" ! videoconvert");
        sb.append(" ! [GstVideoComponent]");
        sb.append(String.format(
            " udpsrc name=audio_src port=%d caps=\"application/x-rtp, media=audio, encoding-name=%s\"",
            audioPort, audioCodec));
        sb.append(" ! queue max-size-buffers=1");
        sb.append(" ! rtpopusdepay ! opusdec ! audioconvert ! autoaudiosink sync=false");

        logger.info("Pipeline description:\n  " + sb.toString().replace(" ! ", "\n  ! "));
    }

    // --- Lifecycle ---

    public void play() {
        logger.info("Starting pipeline");
        pipeline.play();
    }

    public void stop() {
        logger.info("Stopping pipeline");
        pipeline.stop();
    }

    public void dispose() {
        logger.info("Disposing pipeline");
        pipeline.dispose();
    }

    public Pipeline getPipeline() {
        return pipeline;
    }

    public Element getVideoUdpSrc() {
        return videoUdpSrc;
    }

    public void enableTimeout(long timeoutNanos) {
        if (videoUdpSrc != null) {
            videoUdpSrc.set("timeout", timeoutNanos);
        }
    }
}
