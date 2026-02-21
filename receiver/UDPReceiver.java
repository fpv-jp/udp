import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import com.formdev.flatlaf.FlatDarculaLaf;

import org.freedesktop.gstreamer.Gst;
import org.freedesktop.gstreamer.swing.GstVideoComponent;

import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * FPV UDP Receiver application entry point.
 */
public class UDPReceiver {
    private static final Logger logger = Logger.getLogger(UDPReceiver.class.getName());

    public static void main(String[] args) {
        configureLogging();
        configureGstPaths();
        installLookAndFeel();
        Gst.init("FPV UDP Receiver", args);

        logger.info("FPV UDP Receiver starting...");

        SwingUtilities.invokeLater(() -> {
            GstVideoComponent videoComponent = new GstVideoComponent();
            videoComponent.setKeepAspect(true);

            ReceiverSettings settings = new ReceiverSettings();
            ReceiverWindow window = new ReceiverWindow(videoComponent, settings);
            window.start();
        });
    }

    static void configureLogging() {
        String logLevel = System.getenv("JAVA_LOG_LEVEL");
        Level level = Level.INFO;

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

    static void configureGstPaths() {
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

    static void installLookAndFeel() {
        try {
            UIManager.setLookAndFeel(new FlatDarculaLaf());
        } catch (UnsupportedLookAndFeelException ignored) {
        }
    }
}
