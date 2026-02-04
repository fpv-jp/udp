# UDP Streaming CLI

A GStreamer-based UDP video/audio streaming tool built with Go.

## Requirements

- Go 1.25.5 or later
- GStreamer 1.0
- Required GStreamer plugins for your chosen encoder

## Building

```bash
go build -x -v ./...
```

## Usage

```bash
./udp [encoder] [resolution] [host:port]
```

### Example

Stream video using vtenc_h264_hw at 640x480 (VGA) resolution to 192.168.1.10:5000:

```bash
./udp vtenc_h264_hw VGA 192.168.1.10:5000
```

This will:
- Stream video to `192.168.1.10:5000`
- Stream audio to `192.168.1.10:5001` (video port + 1)
- Use Opus audio codec at 48000Hz, 2 channels

### List Supported Encoders and Resolutions

```bash
./udp --list
```

## Supported Encoders

### H.264
- `vtenc_h264_hw` - Apple VideoToolbox (macOS)
- `amfh264enc` - AMD AMF
- `nvh264enc` - NVIDIA NVENC
- `nvv4l2h264enc` - NVIDIA V4L2
- `vah264enc` - VA-API
- `vah264lpenc` - VA-API low power
- `openh264enc` - OpenH264
- `mpph264enc` - Rockchip MPP

### H.265
- `vtenc_h265_hw` - Apple VideoToolbox (macOS)
- `amfh265enc` - AMD AMF
- `nvh265enc` - NVIDIA NVENC
- `nvv4l2h265enc` - NVIDIA V4L2
- `vah265enc` - VA-API
- `vah265lpenc` - VA-API low power
- `x265enc` - x265
- `mpph265enc` - Rockchip MPP

### VP8
- `vp8enc` - libvpx
- `nvv4l2vp8enc` - NVIDIA V4L2
- `mppvp8enc` - Rockchip MPP

### VP9
- `vp9enc` - libvpx
- `nvv4l2vp9enc` - NVIDIA V4L2

### AV1
- `svtav1enc` - SVT-AV1
- `amfav1enc` - AMD AMF
- `nvav1enc` - NVIDIA NVENC
- `vaav1enc` - VA-API

## Supported Resolutions

### 4:3 Aspect Ratio
- `QVGA` - 320x240
- `VGA` - 640x480
- `SVGA` - 800x600
- `XGA` - 1024x768
- `QuadVGA` - 1280x960
- `UXGA` - 1600x1200

### 16:9 Aspect Ratio
- `HD` - 1280x720
- `FHD` - 1920x1080
- `2K` - 1920x1080 (same as FHD)
- `UHD` - 3840x2160
- `4K` - 3840x2160 (same as UHD)

### DCI Cinema Standard (~17:9)
- `DCI2K` - 2048x1080
- `DCI4K` - 4096x2160
