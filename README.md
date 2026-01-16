# udp
Test GUI for receiving media via gstreamer UDP


## Prerequisites

### Windows

```
set PATH=%PATH%;C:\Program Files\gstreamer\1.0\msvc_x86_64\bin
```

## Build

Build for your target platform:

```bash
make macos      # Build for macOS
make linux      # Build for Linux
make windows    # Build for Windows
```

## Run

```bash
make run        # Run jar directly with java -jar
make exec       # Run via maven exec (with GST_DEBUG=2)
make clean      # Clean build artifacts
```

## GStreamer Inspection

Check available decoders and sinks:

```bash
make gst-decoders       # macOS/Linux: List available decoders
make gst-decoders-win   # Windows: List available decoders
make gst-sinks          # macOS/Linux: List audio/video sinks
make gst-sinks-win      # Windows: List audio/video sinks
```

## Audio Pipelines

### macOS

```bash
make audio-macos-opus   # RTP OPUS audio (port 5001)
make audio-macos-pcmu   # RTP PCMU audio (port 5001)
```

### Linux

```bash
make audio-linux-opus   # RTP OPUS audio (port 5001)
make audio-linux-pcmu   # RTP PCMU audio (port 5001)
```

### Windows

```bash
make audio-windows-opus # RTP OPUS audio (port 5001)
make audio-windows-pcmu # RTP PCMU audio (port 5001)
```

## Video Pipelines

### macOS

```bash
make video-macos-h264   # RTP H.264 video (port 5000)
make video-macos-h265   # RTP H.265 video (port 5000)
make video-macos-vp8    # RTP VP8 video (port 5000)
make video-macos-vp9    # RTP VP9 video (port 5000)
make video-macos-av1    # RTP AV1 video (port 5000)
```

### Linux

```bash
make video-linux-h264   # RTP H.264 video (port 5000)
make video-linux-h265   # RTP H.265 video (port 5000)
make video-linux-vp8    # RTP VP8 video (port 5000)
make video-linux-vp9    # RTP VP9 video (port 5000)
make video-linux-av1    # RTP AV1 video (port 5000)
```

### Windows

```bash
make video-windows-h264 # RTP H.264 video (port 5000)
make video-windows-h265 # RTP H.265 video (port 5000)
make video-windows-vp9  # RTP VP9 video (port 5000)
make video-windows-av1  # RTP AV1 video (port 5000)
```

## Help

View all available targets:

```bash
make help
```

## Release

Create and push a new version tag:

```bash
git tag v0.1.1
git push origin v0.1.1
```
