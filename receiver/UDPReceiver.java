import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import javax.swing.Box;
import javax.swing.ButtonGroup;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.formdev.flatlaf.FlatDarculaLaf;

import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.message.Message;
import org.freedesktop.gstreamer.swing.GstVideoComponent;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * FPV UDP Receiver application with Swing UI.
 * Receives video and audio streams over UDP/RTP using GStreamer.
 */
public class UDPReceiver {
    private static final Logger logger = Logger.getLogger(UDPReceiver.class.getName());
    private static final long UDP_TIMEOUT_NANOS = 3_000_000_000L;

    // Video codecs
    private static final String CODEC_H264 = "H264";
    private static final String CODEC_H265 = "H265";
    private static final String CODEC_VP8 = "VP8";
    private static final String CODEC_VP9 = "VP9";
    private static final String CODEC_AV1 = "AV1";

    // Audio codecs
    private static final String CODEC_OPUS = "OPUS";

    // Current settings
    private static String currentVideoCodec = CODEC_H264;
    private static int currentVideoPort = 5000;
    private static String currentAudioCodec = CODEC_OPUS;
    private static int currentAudioPort = 5001;
    private static boolean timeoutEnabled = false;

    public static void main(String[] args) {
        configureLogging();
        configureGstPaths();
        installLookAndFeel();
        Gst.init("FPV UDP Receiver", args);

        logger.info("FPV UDP Receiver starting...");

        SwingUtilities.invokeLater(() -> {
            // Create video component
            GstVideoComponent videoComponent = new GstVideoComponent();
            videoComponent.setKeepAspect(true);

            // Create pipeline
            GStreamerPipeline[] pipelineRef = new GStreamerPipeline[1];
            pipelineRef[0] = new GStreamerPipeline(videoComponent, currentVideoCodec, currentVideoPort,
                                                   currentAudioCodec, currentAudioPort);

            // Create status label
            JLabel statusLabel = createStatusLabel();
            updateStatusLabel(statusLabel);

            // Create main frame
            JFrame frame = new JFrame("FPV Japan UDP Receiver");
            frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
            frame.setLayout(new BorderLayout());
            frame.setJMenuBar(createMenuBar(frame, videoComponent, pipelineRef, statusLabel));
            frame.add(videoComponent, BorderLayout.CENTER);
            frame.setSize(960, 540);
            frame.setLocationRelativeTo(null);

            // Cleanup on close
            frame.addWindowListener(new WindowAdapter() {
                @Override
                public void windowClosed(WindowEvent e) {
                    if (pipelineRef[0] != null) {
                        pipelineRef[0].stop();
                        pipelineRef[0].dispose();
                    }
                }
            });

            frame.setVisible(true);

            // Start pipeline
            pipelineRef[0].play();
            startAutoResize(frame, videoComponent, pipelineRef[0]);
            attachStopOnTimeout(pipelineRef[0], frame);
        });
    }

    /**
     * Creates the menu bar with codec and port selection menus.
     */
    private static JMenuBar createMenuBar(JFrame frame, GstVideoComponent videoComponent,
                                         GStreamerPipeline[] pipelineRef, JLabel statusLabel) {
        JMenuBar menuBar = new JMenuBar();

        // Video codec menu
        JMenu videoCodecMenu = new JMenu("Video Codec");
        ButtonGroup videoCodecGroup = new ButtonGroup();
        addVideoCodecItem(videoCodecMenu, videoCodecGroup, CODEC_H264, "H.264", true,
                         frame, videoComponent, pipelineRef, statusLabel);
        addVideoCodecItem(videoCodecMenu, videoCodecGroup, CODEC_H265, "H.265", false,
                         frame, videoComponent, pipelineRef, statusLabel);
        addVideoCodecItem(videoCodecMenu, videoCodecGroup, CODEC_VP8, "VP8", false,
                         frame, videoComponent, pipelineRef, statusLabel);
        addVideoCodecItem(videoCodecMenu, videoCodecGroup, CODEC_VP9, "VP9", false,
                         frame, videoComponent, pipelineRef, statusLabel);
        addVideoCodecItem(videoCodecMenu, videoCodecGroup, CODEC_AV1, "AV1", false,
                         frame, videoComponent, pipelineRef, statusLabel);
        menuBar.add(videoCodecMenu);

        // Video port menu
        JMenu videoPortMenu = new JMenu("Video Port");
        ButtonGroup videoPortGroup = new ButtonGroup();
        Map<Integer, JRadioButtonMenuItem> videoPortItems = new HashMap<>();
        addPortItem(videoPortMenu, videoPortGroup, videoPortItems, 5000, true,
                   frame, videoComponent, pipelineRef, statusLabel, true);
        addPortItem(videoPortMenu, videoPortGroup, videoPortItems, 6000, false,
                   frame, videoComponent, pipelineRef, statusLabel, true);
        videoPortMenu.addSeparator();
        JMenuItem customVideoPort = new JMenuItem("Custom...");
        customVideoPort.addActionListener(e -> showCustomPortDialog(frame, videoComponent, pipelineRef,
                                                                    statusLabel, videoPortItems, true));
        videoPortMenu.add(customVideoPort);
        menuBar.add(videoPortMenu);

        // Audio codec menu (OPUS only)
        JMenu audioCodecMenu = new JMenu("Audio Codec");
        ButtonGroup audioCodecGroup = new ButtonGroup();
        addAudioCodecItem(audioCodecMenu, audioCodecGroup, CODEC_OPUS, "OPUS", true,
                         frame, videoComponent, pipelineRef, statusLabel);
        menuBar.add(audioCodecMenu);

        // Audio port menu
        JMenu audioPortMenu = new JMenu("Audio Port");
        ButtonGroup audioPortGroup = new ButtonGroup();
        Map<Integer, JRadioButtonMenuItem> audioPortItems = new HashMap<>();
        addPortItem(audioPortMenu, audioPortGroup, audioPortItems, 5001, true,
                   frame, videoComponent, pipelineRef, statusLabel, false);
        addPortItem(audioPortMenu, audioPortGroup, audioPortItems, 6001, false,
                   frame, videoComponent, pipelineRef, statusLabel, false);
        audioPortMenu.addSeparator();
        JMenuItem customAudioPort = new JMenuItem("Custom...");
        customAudioPort.addActionListener(e -> showCustomPortDialog(frame, videoComponent, pipelineRef,
                                                                    statusLabel, audioPortItems, false));
        audioPortMenu.add(customAudioPort);
        menuBar.add(audioPortMenu);

        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(statusLabel);

        return menuBar;
    }

    private static void addVideoCodecItem(JMenu menu, ButtonGroup group, String codec, String label,
                                         boolean selected, JFrame frame, GstVideoComponent videoComponent,
                                         GStreamerPipeline[] pipelineRef, JLabel statusLabel) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, selected);
        item.addActionListener(e -> switchVideoCodec(codec, frame, videoComponent, pipelineRef, statusLabel));
        group.add(item);
        menu.add(item);
    }

    private static void addAudioCodecItem(JMenu menu, ButtonGroup group, String codec, String label,
                                         boolean selected, JFrame frame, GstVideoComponent videoComponent,
                                         GStreamerPipeline[] pipelineRef, JLabel statusLabel) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, selected);
        item.addActionListener(e -> switchAudioCodec(codec, frame, videoComponent, pipelineRef, statusLabel));
        group.add(item);
        menu.add(item);
    }

    private static void addPortItem(JMenu menu, ButtonGroup group, Map<Integer, JRadioButtonMenuItem> portItems,
                                   int port, boolean selected, JFrame frame, GstVideoComponent videoComponent,
                                   GStreamerPipeline[] pipelineRef, JLabel statusLabel, boolean isVideo) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(String.valueOf(port), selected);
        item.addActionListener(e -> {
            if (isVideo) {
                switchVideoPort(port, frame, videoComponent, pipelineRef, statusLabel);
            } else {
                switchAudioPort(port, frame, videoComponent, pipelineRef, statusLabel);
            }
        });
        group.add(item);
        menu.add(item);
        portItems.put(port, item);
    }

    private static void showCustomPortDialog(JFrame frame, GstVideoComponent videoComponent,
                                            GStreamerPipeline[] pipelineRef, JLabel statusLabel,
                                            Map<Integer, JRadioButtonMenuItem> portItems, boolean isVideo) {
        int currentPort = isVideo ? currentVideoPort : currentAudioPort;
        String input = JOptionPane.showInputDialog(frame,
                                                   (isVideo ? "Video" : "Audio") + " UDP port",
                                                   String.valueOf(currentPort));
        if (input == null || input.isBlank()) {
            return;
        }
        try {
            int port = Integer.parseInt(input.trim());
            if (port <= 0 || port > 65535) {
                return;
            }
            if (isVideo) {
                switchVideoPort(port, frame, videoComponent, pipelineRef, statusLabel);
            } else {
                switchAudioPort(port, frame, videoComponent, pipelineRef, statusLabel);
            }
            JRadioButtonMenuItem item = portItems.get(port);
            if (item != null) {
                item.setSelected(true);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    private static void switchVideoCodec(String codec, JFrame frame, GstVideoComponent videoComponent,
                                        GStreamerPipeline[] pipelineRef, JLabel statusLabel) {
        if (codec.equals(currentVideoCodec)) {
            return;
        }
        logger.info("Switching video codec to: " + codec);
        currentVideoCodec = codec;
        timeoutEnabled = false;
        restartPipeline(frame, videoComponent, pipelineRef);
        updateStatusLabel(statusLabel);
    }

    private static void switchAudioCodec(String codec, JFrame frame, GstVideoComponent videoComponent,
                                        GStreamerPipeline[] pipelineRef, JLabel statusLabel) {
        if (codec.equals(currentAudioCodec)) {
            return;
        }
        logger.info("Switching audio codec to: " + codec);
        currentAudioCodec = codec;
        restartPipeline(frame, videoComponent, pipelineRef);
        updateStatusLabel(statusLabel);
    }

    private static void switchVideoPort(int port, JFrame frame, GstVideoComponent videoComponent,
                                       GStreamerPipeline[] pipelineRef, JLabel statusLabel) {
        if (port == currentVideoPort) {
            return;
        }
        logger.info("Switching video port to: " + port);
        currentVideoPort = port;
        timeoutEnabled = false;
        restartPipeline(frame, videoComponent, pipelineRef);
        updateStatusLabel(statusLabel);
    }

    private static void switchAudioPort(int port, JFrame frame, GstVideoComponent videoComponent,
                                       GStreamerPipeline[] pipelineRef, JLabel statusLabel) {
        if (port == currentAudioPort) {
            return;
        }
        logger.info("Switching audio port to: " + port);
        currentAudioPort = port;
        restartPipeline(frame, videoComponent, pipelineRef);
        updateStatusLabel(statusLabel);
    }

    private static void restartPipeline(JFrame frame, GstVideoComponent videoComponent,
                                       GStreamerPipeline[] pipelineRef) {
        logger.info("Restarting pipeline");

        // Stop and dispose old pipeline
        if (pipelineRef[0] != null) {
            pipelineRef[0].stop();
            pipelineRef[0].dispose();
        }

        // Create and start new pipeline
        pipelineRef[0] = new GStreamerPipeline(videoComponent, currentVideoCodec, currentVideoPort,
                                               currentAudioCodec, currentAudioPort);
        pipelineRef[0].play();
        startAutoResize(frame, videoComponent, pipelineRef[0]);
        attachStopOnTimeout(pipelineRef[0], frame);
    }

    private static JLabel createStatusLabel() {
        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBackground(new Color(190, 255, 120));
        label.setForeground(Color.BLACK);
        label.setHorizontalAlignment(JLabel.RIGHT);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return label;
    }

    private static void updateStatusLabel(JLabel label) {
        label.setText("Video: " + currentVideoCodec + "/" + currentVideoPort +
                     "  Audio: " + currentAudioCodec + "/" + currentAudioPort);
    }

    /**
     * Automatically resizes the window when video dimensions are detected.
     */
    private static void startAutoResize(JFrame frame, GstVideoComponent videoComponent,
                                       GStreamerPipeline pipeline) {
        Timer timer = new Timer(200, e -> {
            Dimension size = getVideoSize(videoComponent);
            if (size != null) {
                videoComponent.setPreferredSize(size);
                frame.pack();
                frame.setLocationRelativeTo(null);
                enableTimeout(pipeline);
                ((Timer) e.getSource()).stop();
            }
        });
        timer.setRepeats(true);
        timer.start();
    }

    private static Dimension getVideoSize(GstVideoComponent videoComponent) {
        Pad pad = videoComponent.getElement().getStaticPad("sink");
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

    private static void enableTimeout(GStreamerPipeline pipeline) {
        if (!timeoutEnabled) {
            pipeline.enableTimeout(UDP_TIMEOUT_NANOS);
            timeoutEnabled = true;
        }
    }

    /**
     * Attaches handlers to stop and exit when pipeline errors occur or timeout.
     */
    private static void attachStopOnTimeout(GStreamerPipeline pipeline, JFrame frame) {
        Bus bus = pipeline.getPipeline().getBus();

        // Log all bus messages
        bus.connect((Bus.MESSAGE) (bus1, message) -> {
            logger.fine("Bus message: " + message.getType() + " from " + message.getSource().getName());
        });

        bus.connect((Bus.EOS) source -> {
            logger.warning("End of stream");
            stopAndExit(pipeline, frame);
        });

        bus.connect((Bus.ERROR) (source, code, message) -> {
            logger.severe("Pipeline error: " + message + " (code: " + code + ")");
            stopAndExit(pipeline, frame);
        });

        bus.connect((Bus.WARNING) (source, code, message) -> {
            logger.warning("Pipeline warning: " + message + " (code: " + code + ")");
        });

        bus.connect((Bus.INFO) (source, code, message) -> {
            logger.info("Pipeline info: " + message);
        });

        bus.connect("element", (Bus.MESSAGE) (bus1, message) -> {
            Structure structure = message.getStructure();
            if (structure != null && "GstUDPSrcTimeout".equals(structure.getName())) {
                logger.warning("UDP timeout detected");
                stopAndExit(pipeline, frame);
            }
        });
    }

    private static void stopAndExit(GStreamerPipeline pipeline, JFrame frame) {
        logger.info("Stopping and exiting application");
        SwingUtilities.invokeLater(() -> {
            pipeline.stop();
            pipeline.dispose();
            frame.dispose();
            System.exit(0);
        });
    }

    /**
     * Configures logging level based on JAVA_LOG_LEVEL environment variable.
     * Valid values: ALL, FINEST, FINER, FINE, CONFIG, INFO, WARNING, SEVERE, OFF
     * Default: INFO
     */
    private static void configureLogging() {
        String logLevel = System.getenv("JAVA_LOG_LEVEL");
        Level level = Level.INFO; // Default

        if (logLevel != null && !logLevel.isBlank()) {
            try {
                level = Level.parse(logLevel.toUpperCase());
            } catch (IllegalArgumentException e) {
                System.err.println("Invalid JAVA_LOG_LEVEL: " + logLevel + ", using INFO");
            }
        }

        Logger rootLogger = Logger.getLogger("");
        rootLogger.setLevel(level);
        for (var handler : rootLogger.getHandlers()) {
            handler.setLevel(level);
        }

        logger.info("Logging level set to: " + level);
    }

    /**
     * Configures GStreamer library paths for macOS Homebrew installations.
     */
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

    /**
     * Installs FlatLaf dark theme.
     */
    private static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException ignored) {
        }
    }
}
