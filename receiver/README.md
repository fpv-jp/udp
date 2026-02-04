# UDP FPV Receiver

Java/Swing GUI application that receives and displays video/audio streams over UDP/RTP using GStreamer.

## Features

- Video display with auto-resize to match source resolution
- Runtime codec switching via menu bar (H.264, H.265, VP8, VP9, AV1)
- Runtime port switching via menu bar (preset or custom)
- Platform-aware hardware decoder selection (macOS/Linux/Windows)
- Opus audio decoding with autoaudiosink
- UDP timeout detection (3s) with automatic shutdown
- FlatLaf dark theme (Darcula)
- jpackage support for native installers (dmg/deb/msi)

## Requirements

- Java 25+
- Maven
- GStreamer 1.0 with decoder plugins for your chosen codecs

## Building

```bash
# macOS
mvn clean -Pmacos package

# Linux
mvn clean -Plinux package

# Windows
mvn clean -Pwindows package
```

This produces:
- `target/udp-jar-with-dependencies.jar` - Runnable fat JAR
- `target/dist/` - Native installer (dmg/deb/msi)

## Usage

```bash
java --enable-native-access=ALL-UNNAMED -jar target/udp-jar-with-dependencies.jar
```

### Menu Bar

| Menu         | Options                            | Default |
|--------------|------------------------------------|---------|
| Video Codec  | H.264, H.265, VP8, VP9, AV1       | H.264   |
| Video Port   | 5000, 6000, Custom...              | 5000    |
| Audio Codec  | OPUS                               | OPUS    |
| Audio Port   | 5001, 6001, Custom...              | 5001    |

Changing any setting restarts the GStreamer pipeline automatically.

### Environment Variables

| Variable          | Description                                     | Default |
|-------------------|-------------------------------------------------|---------|
| `JAVA_LOG_LEVEL`  | Logging level (ALL, FINE, INFO, WARNING, SEVERE) | INFO    |
| `JNA_LIBRARY_PATH`| Custom GStreamer library path                    | Auto-detected |

### Decoder Selection

Decoders are selected automatically based on the OS, in priority order:

| Codec | macOS                | Linux                           | Windows                 | Fallback     |
|-------|---------------------|---------------------------------|-------------------------|--------------|
| H.264 | vtdec_hw            | nvh264dec, vah264dec            | d3d12h264dec            | avdec_h264   |
| H.265 | vtdec_hw            | nvh265dec, vah265dec            | d3d12h265dec            | avdec_h265   |
| VP8   | vp8dec              | nvvp8dec, vavp8dec              | d3d12vp8dec             | vp8dec       |
| VP9   | vp9dec              | nvvp9dec, vavp9dec              | d3d12vp9dec             | vp9dec       |
| AV1   | dav1ddec            | nvav1dec, vaav1dec              | d3d12av1dec             | dav1ddec     |

## Dependencies

- [gst1-java-core](https://github.com/gstreamer-java/gst1-java-core) 1.4.0 - GStreamer Java bindings
- [gst1-java-swing](https://github.com/gstreamer-java/gst1-java-swing) 0.9.0 - Swing video component
- [FlatLaf](https://www.formdev.com/flatlaf/) 3.7 - Swing look and feel
