import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.Box;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.formdev.flatlaf.FlatDarculaLaf;

import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Element;
import org.freedesktop.gstreamer.ElementFactory;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.Pipeline;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.message.Message;
import org.freedesktop.gstreamer.swing.GstVideoComponent;

public class main {
    private static final long UDP_TIMEOUT_NANOS = 3_000_000_000L;
    private static final String ENCODING_H264 = "H264";
    private static final String ENCODING_H265 = "H265";
    private static final String ENCODING_VP8 = "VP8";
    private static final String ENCODING_VP9 = "VP9";
    private static final String ENCODING_AV1 = "AV1";
    private static final String ENCODING_OPUS = "OPUS";
    private static final String ENCODING_PCMU = "PCMU";

    private static Element videoUdpSrc;
    private static volatile boolean timeoutEnabled = false;
    private static volatile String currentEncoding = ENCODING_H264;
    private static volatile int currentPort = 5000;
    private static volatile String currentAudioEncoding = ENCODING_OPUS;
    private static volatile int currentAudioPort = 5001;

    public static void main(String[] args) {
        configureGstPaths();
        installLookAndFeel();
        Gst.init("UDP Receiver", args);

        SwingUtilities.invokeLater(() -> {
            GstVideoComponent videoComponent = new GstVideoComponent();
            videoComponent.setKeepAspect(true);
            Pipeline[] pipelineRef = new Pipeline[1];
            pipelineRef[0] = buildPipeline(videoComponent, currentEncoding, currentPort);
            Pipeline[] audioPipelineRef = new Pipeline[1];
            audioPipelineRef[0] = buildAudioPipeline(currentAudioEncoding, currentAudioPort);
            JLabel statusLabel = buildStatusLabel();
            updateStatusLabel(statusLabel);

            JFrame frame = new JFrame("FPV Japan UDP Receiver");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.setJMenuBar(buildMenuBar(frame, videoComponent, pipelineRef, audioPipelineRef, statusLabel));
            frame.add(videoComponent, BorderLayout.CENTER);
            frame.setSize(960, 540);
            frame.setLocationRelativeTo(null);
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    Pipeline pipeline = pipelineRef[0];
                    if (pipeline != null) {
                        pipeline.stop();
                        pipeline.dispose();
                    }
                    Pipeline audioPipeline = audioPipelineRef[0];
                    if (audioPipeline != null) {
                        audioPipeline.stop();
                        audioPipeline.dispose();
                    }
                }
            });
            frame.setVisible(true);

            pipelineRef[0].play();
            if (audioPipelineRef[0] != null) {
                audioPipelineRef[0].play();
            }
            startAutoResize(frame, videoComponent, videoComponent.getElement());
            attachStopOnTimeout(pipelineRef[0], frame);
        });
    }

    private static Pipeline buildPipeline(GstVideoComponent videoComponent, String encoding, int port) {
        videoUdpSrc = ElementFactory.make("udpsrc", "src");
        videoUdpSrc.set("port", port);
        videoUdpSrc.set("caps", Caps.fromString(
                "application/x-rtp, media=video, encoding-name=" + encoding + ", clock-rate=90000"));

        Element jitter = ElementFactory.make("rtpjitterbuffer", "jitter");
        jitter.set("latency", 0);

        Element queue = ElementFactory.make("queue", "queue");
        queue.set("max-size-buffers", 1);
        // queue.set("leaky", 2);

        Element depay = ElementFactory.make(selectDepay(encoding), "depay");
        Element parse = createParse(encoding);
        Element decoder = createDecoder(encoding);
        Element convert = ElementFactory.make("videoconvert", "convert");
        Element sink = videoComponent.getElement();
        sink.set("sync", false);
        sink.set("async", false);

        Pipeline pipeline = new Pipeline("pipeline");
        List<Element> elements = new ArrayList<>();
        elements.add(videoUdpSrc);
        elements.add(jitter);
        elements.add(queue);
        elements.add(depay);
        if (parse != null) {
            elements.add(parse);
        }
        elements.add(decoder);
        elements.add(convert);
        elements.add(sink);
        pipeline.addMany(elements.toArray(new Element[0]));
        Element.linkMany(elements.toArray(new Element[0]));
        return pipeline;
    }

    private static Pipeline buildAudioPipeline(String encoding, int port) {
        Element udpSrc = ElementFactory.make("udpsrc", "audio_src");
        udpSrc.set("port", port);
        udpSrc.set("caps", Caps.fromString(
                "application/x-rtp, media=audio, encoding-name=" + encoding));

        Element queue = ElementFactory.make("queue", "audio_queue");
        queue.set("max-size-buffers", 1);

        Element depay = tryCreateElement(selectAudioDepay(encoding), "audio_depay");
        Element decoder = createAudioDecoder(encoding);
        Element convert = ElementFactory.make("audioconvert", "audio_convert");

        String sinkFactory;
        if (isWindows()) {
            sinkFactory = "directsoundsink";
        } else if (isLinux()) {
            sinkFactory = "pulsesink";
        } else {
            sinkFactory = "osxaudiosink";
        }
        Element sink = tryCreateElement(sinkFactory, "audio_sink");
        if (sink == null) {
            sink = tryCreateElement("autoaudiosink", "audio_sink");
        }
        if (depay == null || decoder == null || sink == null) {
            return null;
        }
        sink.set("sync", false);
        sink.set("async", false);

        Pipeline pipeline = new Pipeline("audio-pipeline");
        List<Element> elements = new ArrayList<>();
        elements.add(udpSrc);
        elements.add(queue);
        elements.add(depay);
        elements.add(decoder);
        elements.add(convert);
        elements.add(sink);
        pipeline.addMany(elements.toArray(new Element[0]));
        Element.linkMany(elements.toArray(new Element[0]));
        return pipeline;
    }

    private static JMenuBar buildMenuBar(JFrame frame, GstVideoComponent videoComponent, Pipeline[] pipelineRef,
            Pipeline[] audioPipelineRef, JLabel statusLabel) {
        JMenuBar menuBar = new JMenuBar();
        JMenu codecMenu = new JMenu("Video Codec");
        JMenu portMenu = new JMenu("Video Port");
        JMenu audioCodecMenu = new JMenu("Audio Codec");
        JMenu audioPortMenu = new JMenu("Audio Port");

        ButtonGroup group = new ButtonGroup();
        JRadioButtonMenuItem h264Item = new JRadioButtonMenuItem("H.264", true);
        JRadioButtonMenuItem h265Item = new JRadioButtonMenuItem("H.265");
        JRadioButtonMenuItem vp8Item = new JRadioButtonMenuItem("VP8");
        JRadioButtonMenuItem vp9Item = new JRadioButtonMenuItem("VP9");
        JRadioButtonMenuItem av1Item = new JRadioButtonMenuItem("AV1");

        h264Item.addActionListener(e -> switchCodec(frame, videoComponent, pipelineRef, statusLabel, ENCODING_H264));
        h265Item.addActionListener(e -> switchCodec(frame, videoComponent, pipelineRef, statusLabel, ENCODING_H265));
        vp8Item.addActionListener(e -> switchCodec(frame, videoComponent, pipelineRef, statusLabel, ENCODING_VP8));
        vp9Item.addActionListener(e -> switchCodec(frame, videoComponent, pipelineRef, statusLabel, ENCODING_VP9));
        av1Item.addActionListener(e -> switchCodec(frame, videoComponent, pipelineRef, statusLabel, ENCODING_AV1));

        group.add(h264Item);
        group.add(h265Item);
        group.add(vp8Item);
        group.add(vp9Item);
        group.add(av1Item);
        codecMenu.add(h264Item);
        codecMenu.add(h265Item);
        codecMenu.add(vp8Item);
        codecMenu.add(vp9Item);
        codecMenu.add(av1Item);
        menuBar.add(codecMenu);
        menuBar.add(portMenu);
        menuBar.add(audioCodecMenu);
        menuBar.add(audioPortMenu);
        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(statusLabel);

        Map<Integer, JRadioButtonMenuItem> portItems = new HashMap<>();
        ButtonGroup portGroup = new ButtonGroup();
        addPortItem(portMenu, portGroup, portItems, 5000, frame, videoComponent, pipelineRef, statusLabel);
        addPortItem(portMenu, portGroup, portItems, 6000, frame, videoComponent, pipelineRef, statusLabel);

        portMenu.addSeparator();
        JMenuItem customPort = new JMenuItem("Custom...");
        customPort.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(frame, "UDP port", String.valueOf(currentPort));
            if (input == null || input.isBlank()) {
                return;
            }
            try {
                int port = Integer.parseInt(input.trim());
                if (port <= 0 || port > 65535) {
                    return;
                }
                switchPort(frame, videoComponent, pipelineRef, statusLabel, port);
                JRadioButtonMenuItem item = portItems.get(port);
                if (item != null) {
                    item.setSelected(true);
                }
            } catch (NumberFormatException ignored) {
                // Ignore invalid custom port.
            }
        });
        portMenu.add(customPort);

        ButtonGroup audioCodecGroup = new ButtonGroup();
        JRadioButtonMenuItem opusItem = new JRadioButtonMenuItem("OPUS", ENCODING_OPUS.equals(currentAudioEncoding));
        JRadioButtonMenuItem pcmuItem = new JRadioButtonMenuItem("PCMU", ENCODING_PCMU.equals(currentAudioEncoding));
        opusItem.addActionListener(e -> switchAudioCodec(audioPipelineRef, statusLabel, ENCODING_OPUS));
        pcmuItem.addActionListener(e -> switchAudioCodec(audioPipelineRef, statusLabel, ENCODING_PCMU));
        audioCodecGroup.add(opusItem);
        audioCodecGroup.add(pcmuItem);
        audioCodecMenu.add(opusItem);
        audioCodecMenu.add(pcmuItem);

        Map<Integer, JRadioButtonMenuItem> audioPortItems = new HashMap<>();
        ButtonGroup audioPortGroup = new ButtonGroup();
        addAudioPortItem(audioPortMenu, audioPortGroup, audioPortItems, 5001, audioPipelineRef, statusLabel);
        addAudioPortItem(audioPortMenu, audioPortGroup, audioPortItems, 6001, audioPipelineRef, statusLabel);
        audioPortMenu.addSeparator();
        JMenuItem customAudioPort = new JMenuItem("Custom...");
        customAudioPort.addActionListener(e -> {
            String input = JOptionPane.showInputDialog(frame, "Audio UDP port", String.valueOf(currentAudioPort));
            if (input == null || input.isBlank()) {
                return;
            }
            try {
                int port = Integer.parseInt(input.trim());
                if (port <= 0 || port > 65535) {
                    return;
                }
                switchAudioPort(audioPipelineRef, statusLabel, port);
                JRadioButtonMenuItem item = audioPortItems.get(port);
                if (item != null) {
                    item.setSelected(true);
                }
            } catch (NumberFormatException ignored) {
                // Ignore invalid custom port.
            }
        });
        audioPortMenu.add(customAudioPort);
        return menuBar;
    }

    private static void switchCodec(JFrame frame, GstVideoComponent videoComponent, Pipeline[] pipelineRef,
            JLabel statusLabel, String encoding) {
        if (encoding.equals(currentEncoding)) {
            return;
        }
        currentEncoding = encoding;
        timeoutEnabled = false;

        restartPipeline(frame, videoComponent, pipelineRef);
        updateStatusLabel(statusLabel);
    }

    private static void switchAudioCodec(Pipeline[] audioPipelineRef, JLabel statusLabel, String encoding) {
        if (encoding.equals(currentAudioEncoding)) {
            return;
        }
        currentAudioEncoding = encoding;
        restartAudioPipeline(audioPipelineRef);
        updateStatusLabel(statusLabel);
    }

    private static void switchPort(JFrame frame, GstVideoComponent videoComponent, Pipeline[] pipelineRef,
            JLabel statusLabel, int port) {
        if (port == currentPort) {
            return;
        }
        currentPort = port;
        timeoutEnabled = false;
        restartPipeline(frame, videoComponent, pipelineRef);
        updateStatusLabel(statusLabel);
    }

    private static void switchAudioPort(Pipeline[] audioPipelineRef, JLabel statusLabel, int port) {
        if (port == currentAudioPort) {
            return;
        }
        currentAudioPort = port;
        restartAudioPipeline(audioPipelineRef);
        updateStatusLabel(statusLabel);
    }

    private static void restartPipeline(JFrame frame, GstVideoComponent videoComponent, Pipeline[] pipelineRef) {
        Pipeline oldPipeline = pipelineRef[0];
        if (oldPipeline != null) {
            oldPipeline.stop();
            oldPipeline.dispose();
        }

        Pipeline newPipeline = buildPipeline(videoComponent, currentEncoding, currentPort);
        pipelineRef[0] = newPipeline;
        newPipeline.play();
        startAutoResize(frame, videoComponent, videoComponent.getElement());
        attachStopOnTimeout(newPipeline, frame);
    }

    private static void restartAudioPipeline(Pipeline[] audioPipelineRef) {
        Pipeline oldPipeline = audioPipelineRef[0];
        if (oldPipeline != null) {
            oldPipeline.stop();
            oldPipeline.dispose();
        }

        Pipeline newPipeline = buildAudioPipeline(currentAudioEncoding, currentAudioPort);
        audioPipelineRef[0] = newPipeline;
        if (newPipeline != null) {
            newPipeline.play();
        }
    }

    private static String selectDepay(String encoding) {
        if (ENCODING_H265.equals(encoding)) {
            return "rtph265depay";
        }
        if (ENCODING_VP8.equals(encoding)) {
            return "rtpvp8depay";
        }
        if (ENCODING_VP9.equals(encoding)) {
            return "rtpvp9depay";
        }
        if (ENCODING_AV1.equals(encoding)) {
            return "rtpav1depay";
        }
        return "rtph264depay";
    }

    private static String selectAudioDepay(String encoding) {
        if (ENCODING_PCMU.equals(encoding)) {
            return "rtppcmudepay";
        }
        return "rtpopusdepay";
    }

    private static Element createParse(String encoding) {
        if (ENCODING_H265.equals(encoding)) {
            return ElementFactory.make("h265parse", "parse");
        }
        if (ENCODING_VP9.equals(encoding)) {
            return ElementFactory.make("vp9parse", "parse");
        }
        if (ENCODING_AV1.equals(encoding)) {
            return ElementFactory.make("av1parse", "parse");
        }
        if (ENCODING_H264.equals(encoding)) {
            return ElementFactory.make("h264parse", "parse");
        }
        return null;
    }

    private static Element createAudioDecoder(String encoding) {
        if (ENCODING_PCMU.equals(encoding)) {
            return tryCreateElement("mulawdec", "audio_decoder");
        }
        return tryCreateElement("opusdec", "audio_decoder");
    }

    private static Element createDecoder(String encoding) {
        List<String> candidates = new ArrayList<>();
        if (isWindows()) {
            if (ENCODING_H264.equals(encoding)) {
                candidates.add("d3d12h264dec");
            } else if (ENCODING_H265.equals(encoding)) {
                candidates.add("d3d12h265dec");
            } else if (ENCODING_VP9.equals(encoding)) {
                candidates.add("d3d12vp9dec");
            } else if (ENCODING_AV1.equals(encoding)) {
                candidates.add("d3d12av1dec");
            }
        } else if (isLinux()) {
            if (ENCODING_H264.equals(encoding)) {
                candidates.add("vah264dec");
            } else if (ENCODING_H265.equals(encoding)) {
                candidates.add("vah265dec");
            } else if (ENCODING_VP8.equals(encoding)) {
                candidates.add("vavp8dec");
            } else if (ENCODING_VP9.equals(encoding)) {
                candidates.add("vavp9dec");
            } else if (ENCODING_AV1.equals(encoding)) {
                candidates.add("vaav1dec");
            }
        }

        if (ENCODING_H264.equals(encoding)) {
            candidates.add("avdec_h264");
        } else if (ENCODING_H265.equals(encoding)) {
            candidates.add("avdec_h265");
        } else if (ENCODING_VP8.equals(encoding)) {
            candidates.add("vp8dec");
        } else if (ENCODING_VP9.equals(encoding)) {
            candidates.add("vp9dec");
        } else if (ENCODING_AV1.equals(encoding)) {
            candidates.add("dav1ddec");
        }

        for (String name : candidates) {
            Element decoder = tryCreateElement(name, "decoder");
            if (decoder != null) {
                if ("vp9dec".equals(name)) {
                    setIfSupported(decoder, "threads", 4);
                }
                return decoder;
            }
        }
        return null;
    }

    private static void addPortItem(JMenu portMenu, ButtonGroup portGroup, Map<Integer, JRadioButtonMenuItem> portItems,
            int port, JFrame frame, GstVideoComponent videoComponent, Pipeline[] pipelineRef, JLabel statusLabel) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(String.valueOf(port), port == currentPort);
        item.addActionListener(e -> switchPort(frame, videoComponent, pipelineRef, statusLabel, port));
        portGroup.add(item);
        portMenu.add(item);
        portItems.put(port, item);
    }

    private static void addAudioPortItem(JMenu portMenu, ButtonGroup portGroup,
            Map<Integer, JRadioButtonMenuItem> portItems, int port, Pipeline[] audioPipelineRef, JLabel statusLabel) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(String.valueOf(port), port == currentAudioPort);
        item.addActionListener(e -> switchAudioPort(audioPipelineRef, statusLabel, port));
        portGroup.add(item);
        portMenu.add(item);
        portItems.put(port, item);
    }

    private static void setIfSupported(Element element, String property, Object value) {
        try {
            element.set(property, value);
        } catch (IllegalArgumentException ignored) {
            // Property not supported by this GStreamer build.
        }
    }

    private static Element tryCreateElement(String factory, String name) {
        try {
            return ElementFactory.make(factory, name);
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    private static boolean isLinux() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase().contains("linux");
    }

    private static boolean isWindows() {
        String osName = System.getProperty("os.name", "");
        return osName.toLowerCase().contains("windows");
    }

    private static JLabel buildStatusLabel() {
        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBackground(new Color(190, 255, 120));
        label.setForeground(Color.BLACK);
        label.setHorizontalAlignment(JLabel.RIGHT);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return label;
    }

    private static void updateStatusLabel(JLabel label) {
        label.setText("Video: " + currentEncoding + "/" + currentPort
                + "  Audio: " + currentAudioEncoding + "/" + currentAudioPort);
    }

    private static void attachStopOnTimeout(Pipeline pipeline, JFrame frame) {
        pipeline.getBus().connect((Bus.EOS) source -> stopAndExit(pipeline, frame));
        pipeline.getBus().connect((Bus.ERROR) (source, code, message) -> stopAndExit(pipeline, frame));
        pipeline.getBus().connect("element", new Bus.MESSAGE() {
            @Override
            public void busMessage(Bus bus, Message message) {
                Structure structure = message.getStructure();
                if (structure != null && "GstUDPSrcTimeout".equals(structure.getName())) {
                    stopAndExit(pipeline, frame);
                }
            }
        });
    }

    private static void stopAndExit(Pipeline pipeline, JFrame frame) {
        SwingUtilities.invokeLater(() -> {
            pipeline.stop();
            pipeline.dispose();
            frame.dispose();
            System.exit(0);
        });
    }

    private static void startAutoResize(JFrame frame, GstVideoComponent videoComponent, Element sink) {
        Timer timer = new Timer(200, e -> {
            Dimension size = getVideoSize(sink);
            if (size != null) {
                videoComponent.setPreferredSize(size);
                frame.pack();
                frame.setLocationRelativeTo(null);
                enableTimeout();
                ((Timer) e.getSource()).stop();
            }
        });
        timer.setRepeats(true);
        timer.start();
    }

    private static Dimension getVideoSize(Element sink) {
        Pad pad = sink.getStaticPad("sink");
        if (pad == null || !pad.hasCurrentCaps()) {
            return null;
        }
        Caps caps = pad.getCurrentCaps();
        if (caps == null || caps.size() == 0) {
            return null;
        }
        Structure structure = caps.getStructure(0);
        if (structure == null || !structure.hasIntField("width") || !structure.hasIntField("height")) {
            return null;
        }
        int width = structure.getInteger("width");
        int height = structure.getInteger("height");
        if (width <= 0 || height <= 0) {
            return null;
        }
        return new Dimension(width, height);
    }

    private static void enableTimeout() {
        if (timeoutEnabled || videoUdpSrc == null) {
            return;
        }
        videoUdpSrc.set("timeout", UDP_TIMEOUT_NANOS);
        timeoutEnabled = true;
    }

    private static void configureGstPaths() {
        String envPath = System.getenv("JNA_LIBRARY_PATH");
        if (envPath != null && !envPath.isBlank()) {
            System.setProperty("jna.library.path", envPath);
            return;
        }

        Path homebrewArm = Paths.get("/opt/homebrew/lib/libgstreamer-1.0.dylib");
        Path homebrewIntel = Paths.get("/usr/local/lib/libgstreamer-1.0.dylib");

        if (Files.exists(homebrewArm)) {
            System.setProperty("jna.library.path", "/opt/homebrew/lib");
        } else if (Files.exists(homebrewIntel)) {
            System.setProperty("jna.library.path", "/usr/local/lib");
        }
    }

    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException ignored) {
            // Fall back to default L&F.
        }
    }
}
