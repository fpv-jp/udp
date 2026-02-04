import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
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

import org.freedesktop.gstreamer.Bus;
import org.freedesktop.gstreamer.Caps;
import org.freedesktop.gstreamer.Pad;
import org.freedesktop.gstreamer.Structure;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.swing.GstVideoComponent;

import java.util.logging.Logger;

/**
 * Main window for the FPV UDP Receiver.
 * Manages the JFrame, menus, status label, and pipeline lifecycle.
 */
public class ReceiverWindow {
    private static final Logger logger = Logger.getLogger(ReceiverWindow.class.getName());

    private final GstVideoComponent videoComponent;
    private final AppSink displaySink;
    private final ReceiverSettings settings;
    private final JFrame frame;
    private final JLabel statusLabel;
    private GStreamerPipeline pipeline;

    public ReceiverWindow(GstVideoComponent videoComponent, AppSink displaySink, ReceiverSettings settings) {
        this.videoComponent = videoComponent;
        this.displaySink = displaySink;
        this.settings = settings;

        this.statusLabel = createStatusLabel();
        updateStatusLabel();

        this.frame = new JFrame("FPV Japan UDP Receiver");
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.setLayout(new BorderLayout());
        frame.setJMenuBar(createMenuBar());
        frame.add(videoComponent, BorderLayout.CENTER);
        frame.setSize(960, 540);
        frame.setLocationRelativeTo(null);

        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                if (pipeline != null) {
                    pipeline.stop();
                    pipeline.dispose();
                }
            }
        });

        settings.setOnSettingsChanged(this::onSettingsChanged);
    }

    public void start() {
        frame.setVisible(true);
        startPipeline();
    }

    // --- Pipeline lifecycle ---

    private void startPipeline() {
        pipeline = new GStreamerPipeline(displaySink,
                settings.getVideoCodec(), settings.getVideoPort(),
                settings.getAudioCodec(), settings.getAudioPort());
        pipeline.play();
        startAutoResize();
        attachBusHandlers();
    }

    private void restartPipeline() {
        logger.info("Restarting pipeline");
        if (pipeline != null) {
            pipeline.stop();
            pipeline.dispose();
        }
        startPipeline();
    }

    private void onSettingsChanged() {
        restartPipeline();
        updateStatusLabel();
    }

    // --- Menu ---

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();

        // Video codec menu
        JMenu videoCodecMenu = new JMenu("Video Codec");
        ButtonGroup videoCodecGroup = new ButtonGroup();
        addVideoCodecItem(videoCodecMenu, videoCodecGroup, ReceiverSettings.CODEC_H264, "H.264", true);
        addVideoCodecItem(videoCodecMenu, videoCodecGroup, ReceiverSettings.CODEC_H265, "H.265", false);
        addVideoCodecItem(videoCodecMenu, videoCodecGroup, ReceiverSettings.CODEC_VP8, "VP8", false);
        addVideoCodecItem(videoCodecMenu, videoCodecGroup, ReceiverSettings.CODEC_VP9, "VP9", false);
        addVideoCodecItem(videoCodecMenu, videoCodecGroup, ReceiverSettings.CODEC_AV1, "AV1", false);
        menuBar.add(videoCodecMenu);

        // Video port menu
        JMenu videoPortMenu = new JMenu("Video Port");
        ButtonGroup videoPortGroup = new ButtonGroup();
        Map<Integer, JRadioButtonMenuItem> videoPortItems = new HashMap<>();
        addPortItem(videoPortMenu, videoPortGroup, videoPortItems, 5000, true, true);
        addPortItem(videoPortMenu, videoPortGroup, videoPortItems, 6000, false, true);
        videoPortMenu.addSeparator();
        JMenuItem customVideoPort = new JMenuItem("Custom...");
        customVideoPort.addActionListener(e -> showCustomPortDialog(videoPortItems, true));
        videoPortMenu.add(customVideoPort);
        menuBar.add(videoPortMenu);

        // Audio codec menu
        JMenu audioCodecMenu = new JMenu("Audio Codec");
        ButtonGroup audioCodecGroup = new ButtonGroup();
        addAudioCodecItem(audioCodecMenu, audioCodecGroup, ReceiverSettings.CODEC_OPUS, "OPUS", true);
        menuBar.add(audioCodecMenu);

        // Audio port menu
        JMenu audioPortMenu = new JMenu("Audio Port");
        ButtonGroup audioPortGroup = new ButtonGroup();
        Map<Integer, JRadioButtonMenuItem> audioPortItems = new HashMap<>();
        addPortItem(audioPortMenu, audioPortGroup, audioPortItems, 5001, true, false);
        addPortItem(audioPortMenu, audioPortGroup, audioPortItems, 6001, false, false);
        audioPortMenu.addSeparator();
        JMenuItem customAudioPort = new JMenuItem("Custom...");
        customAudioPort.addActionListener(e -> showCustomPortDialog(audioPortItems, false));
        audioPortMenu.add(customAudioPort);
        menuBar.add(audioPortMenu);

        menuBar.add(Box.createHorizontalGlue());
        menuBar.add(statusLabel);

        return menuBar;
    }

    private void addVideoCodecItem(JMenu menu, ButtonGroup group, String codec, String label, boolean selected) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, selected);
        item.addActionListener(e -> settings.setVideoCodec(codec));
        group.add(item);
        menu.add(item);
    }

    private void addAudioCodecItem(JMenu menu, ButtonGroup group, String codec, String label, boolean selected) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(label, selected);
        item.addActionListener(e -> settings.setAudioCodec(codec));
        group.add(item);
        menu.add(item);
    }

    private void addPortItem(JMenu menu, ButtonGroup group, Map<Integer, JRadioButtonMenuItem> portItems,
                             int port, boolean selected, boolean isVideo) {
        JRadioButtonMenuItem item = new JRadioButtonMenuItem(String.valueOf(port), selected);
        item.addActionListener(e -> {
            if (isVideo) {
                settings.setVideoPort(port);
            } else {
                settings.setAudioPort(port);
            }
        });
        group.add(item);
        menu.add(item);
        portItems.put(port, item);
    }

    private void showCustomPortDialog(Map<Integer, JRadioButtonMenuItem> portItems, boolean isVideo) {
        int currentPort = isVideo ? settings.getVideoPort() : settings.getAudioPort();
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
                settings.setVideoPort(port);
            } else {
                settings.setAudioPort(port);
            }
            JRadioButtonMenuItem item = portItems.get(port);
            if (item != null) {
                item.setSelected(true);
            }
        } catch (NumberFormatException ignored) {
        }
    }

    // --- Status label ---

    private JLabel createStatusLabel() {
        JLabel label = new JLabel();
        label.setOpaque(true);
        label.setBackground(new Color(190, 255, 120));
        label.setForeground(Color.BLACK);
        label.setHorizontalAlignment(JLabel.RIGHT);
        label.setBorder(javax.swing.BorderFactory.createEmptyBorder(4, 8, 4, 8));
        return label;
    }

    private void updateStatusLabel() {
        statusLabel.setText(settings.getStatusText());
    }

    // --- Auto-resize ---

    private void startAutoResize() {
        Timer timer = new Timer(200, e -> {
            Dimension size = getVideoSize();
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

    private Dimension getVideoSize() {
        Pad pad = displaySink.getStaticPad("sink");
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

    private void enableTimeout() {
        if (!settings.isTimeoutEnabled()) {
            pipeline.enableTimeout(ReceiverSettings.UDP_TIMEOUT_NANOS);
            settings.setTimeoutEnabled(true);
        }
    }

    // --- Bus handlers ---

    private void attachBusHandlers() {
        Bus bus = pipeline.getPipeline().getBus();

        bus.connect((Bus.MESSAGE) (bus1, message) -> {
            logger.fine("Bus message: " + message.getType() + " from " + message.getSource().getName());
        });

        bus.connect((Bus.STATE_CHANGED) (source, old, current, pending) -> {
            if (source == pipeline.getPipeline()) {
                logger.info("Pipeline state: " + old + " -> " + current
                        + (pending.toString().equals("VOID_PENDING") ? "" : " (pending: " + pending + ")"));
            }
        });

        bus.connect((Bus.EOS) source -> {
            logger.warning("End of stream");
            stopAndExit();
        });

        bus.connect((Bus.ERROR) (source, code, message) -> {
            logger.severe("Pipeline error from [" + source.getName() + "]: " + message + " (code: " + code + ")");
            stopAndExit();
        });

        bus.connect((Bus.WARNING) (source, code, message) -> {
            logger.warning("Pipeline warning from [" + source.getName() + "]: " + message + " (code: " + code + ")");
        });

        bus.connect((Bus.INFO) (source, code, message) -> {
            logger.info("Pipeline info: " + message);
        });

        bus.connect("element", (Bus.MESSAGE) (bus1, message) -> {
            Structure structure = message.getStructure();
            if (structure != null && "GstUDPSrcTimeout".equals(structure.getName())) {
                logger.warning("UDP timeout detected");
                stopAndExit();
            }
        });
    }

    private void stopAndExit() {
        logger.info("Stopping and exiting application");
        SwingUtilities.invokeLater(() -> {
            pipeline.stop();
            pipeline.dispose();
            frame.dispose();
            System.exit(0);
        });
    }
}
