
### ------------ Launch Config ------------
GST_LAUNCH := GST_DEBUG=2 gst-launch-1.0 -v -e
GST_LAUNCH_FILE := GST_DEBUG=2 gst-launch-1.0 fdsrc -v -e ! video/x-h264,stream-format=byte-stream

### ------------ Network Config ------------
HOST ?= 192.168.151.5
VIDEO_PORT ?= 5000
AUDIO_PORT ?= 5001

VIDEO_UDP_SINK := udpsink host=$(HOST) port=$(VIDEO_PORT) sync=false async=false
AUDIO_UDP_SINK := udpsink host=$(HOST) port=$(AUDIO_PORT) sync=false async=false

# queue
SEND_QUEUE1 := queue max-size-buffers=1 leaky=downstream
SEND_QUEUE3 := queue max-size-buffers=3 leaky=downstream

### ------------ Video Config ------------
VIDEO_DEVICE ?= 0
WIDTH        ?= 1920
HEIGHT       ?= 1080
FRAMERATE    ?= 30
CODEC        ?= h264

VIDEO_CAPS      := video/x-raw,width=$(WIDTH),height=$(HEIGHT),framerate=$(FRAMERATE)/1
VIDEO_CAPS_H264 := video/x-h264,width=$(WIDTH),height=$(HEIGHT),framerate=$(FRAMERATE)/1,stream-format=byte-stream

# src
VIDEO_SRC_TEST           := videotestsrc pattern=ball is-live=true
VIDEO_SRC_V4L2           := v4l2src device=/dev/video$(VIDEO_DEVICE) io-mode=dmabuf do-timestamp=true
VIDEO_SRC_V4L2_H264      := v4l2src device=/dev/video$(VIDEO_DEVICE) ! $(VIDEO_CAPS_H264)
VIDEO_SRC_LIBCAMERA      := libcamerasrc
VIDEO_SRC_LIBCAMERA_FILE := libcamera-vid --width $(WIDTH) --height $(HEIGHT) --framerate $(FRAMERATE) --codec $(CODEC) --timeout 0 --inline --output  - |

# v4l2src
#       video/x-raw(memory:DMABuf) drm-format: { RGB8, AR15, XR15_BE, AR15_BE, RG16, RG16_BE, RG24, BG24, AR24, RA24, RX24, AB24, XB24, BA24, AR30, "R8", "R4", "R10", "R12", "R16", "R16_BE", GR88, YUV9, YVU9, YUYV, UYVY, VYUY, YU11, NV12:0x0400000000000001, NV24, NV42,NV12:0x0b00000000000001, P010, NV61, NV16, NV21, NV12:0x0400000000000002, NV12, YU16, YVYU, YV12, YU12, BX24, XR24, XR15 }
#       video/x-raw(memory:DMABuf, format:Interlaced) drm-format: { RGB8, AR15, XR15_BE, AR15_BE, RG16, RG16_BE, RG24, BG24, AR24, RA24, RX24, AB24, XB24, BA24, AR30, "R8", "R4", "R10", "R12", "R16", "R16_BE", GR88, YUV9, YVU9, YUYV, UYVY, VYUY, YU11, NV12:0x0400000000000001, NV24, NV42,NV12:0x0b00000000000001, P010, NV61, NV16, NV21, NV12:0x0400000000000002, NV12, YU16, YVYU, YV12, YU12, BX24, XR24, XR15 }

# v4l2h264enc
#       video/x-raw(memory:DMABuf) drm-format: { YU12, YV12, NV12, NV21, RG16, BG24, RG24, AB24, XR24, YUYV, YVYU, UYVY, VYUY }
#       video/x-raw(memory:DMABuf, format:Interlaced) drm-format: { YU12, YV12, NV12, NV21, RG16, BG24, RG24, AB24, XR24, YUYV, YVYU, UYVY, VYUY }
#       video/x-raw format: { I420, YV12, NV12, NV21, RGB16, RGB, BGR, RGBA, BGRx, BGRA, YUY2, YVYU, UYVY }
#       video/x-raw(format:Interlaced) format: { I420, YV12, NV12, NV21, RGB16, RGB, BGR, RGBA, BGRx, BGRA, YUY2, YVYU, UYVY }

### ------------ Audio Config ------------
AUDIO_DEVICE ?= 3
RATE ?= 48000
CHANNELS ?= 1
AUDIO_CAP := audio/x-raw,rate=$(RATE),channels=$(CHANNELS)

# src
AUDIO_SRC_ALSA := alsasrc device=hw:$(AUDIO_DEVICE) do-timestamp=true

### ------------ Targets ------------

## Test video stream with videotestsrc + v4l2h264enc
send_test:
	$(GST_LAUNCH) $(VIDEO_SRC_TEST) ! $(VIDEO_CAPS) ! $(SEND_QUEUE1) ! \
		v4l2h264enc output-io-mode=dmabuf capture-io-mode=dmabuf \
			extra-controls="encode,h264_profile=4,h264_level=12,video_bitrate=20000000" ! \
		capsfilter caps=video/x-h264,level=\(string\)4 ! $(SEND_QUEUE3) ! \
		rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

send_264:
	$(GST_LAUNCH) $(VIDEO_SRC_V4L2_H264) ! $(SEND_QUEUE1) ! \
	h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

## Send H.264 video using v4l2src (legacy camera support) 
send_264_v4l2:
	$(GST_LAUNCH) $(VIDEO_SRC_V4L2) ! $(VIDEO_CAPS),format=NV12 ! $(SEND_QUEUE1) ! \
	v4l2h264enc capture-io-mode=dmabuf output-io-mode=dmabuf ! \
	capsfilter caps=video/x-h264,level=\(string\)4.1 ! \
	$(SEND_QUEUE3) ! \
	rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

## (Deprecated) Send H.264 from libcamera-vid pipe
send_264_libcam_filepipe:
	$(VIDEO_SRC_LIBCAMERA_FILE) $(GST_LAUNCH_FILE) ! $(SEND_QUEUE1) ! \
	h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

## Send H.264 using libcamerasrc (default)
send_264_libcam:
	$(GST_LAUNCH) $(VIDEO_SRC_LIBCAMERA) ! $(VIDEO_CAPS),format=YUY2,interlace-mode=progressive ! \
	v4l2h264enc extra-controls="encode,h264_profile=4,h264_level=12,video_bitrate=20000000" ! \
	capsfilter caps=video/x-h264,level=\(string\)4.1 ! \
	$(SEND_QUEUE3) ! \
	rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

## Send Opus audio
send_o:
	$(GST_LAUNCH) $(AUDIO_SRC_ALSA) ! $(AUDIO_CAP) ! $(SEND_QUEUE1) ! \
	audioconvert ! audioresample ! opusenc ! rtpopuspay ! $(AUDIO_UDP_SINK)

## Send G.711 (PCMU) audio
send_p:
	$(GST_LAUNCH) $(AUDIO_SRC_ALSA) ! $(AUDIO_CAP) ! $(SEND_QUEUE1) ! \
	audioconvert ! audioresample ! mulawenc ! rtppcmupay ! $(AUDIO_UDP_SINK)

### ------------ Help ------------
help:
	@echo ""
	@echo "Usage:"
	@echo "  make send_264_v4l2          # Send H.264 video using v4l2src"
	@echo "  make send_264_libcam        # Send H.264 video using libcamerasrc"
	@echo "  make send_264_libcam_filepipe # (Deprecated) Send H.264 using libcamera-vid | fdsrc"
	@echo "  make send_test              # Test stream using videotestsrc"
	@echo "  make send_o                 # Send Opus audio"
	@echo "  make send_p                 # Send PCMU audio (G.711)"
	@echo ""
	@echo "Defaults:"
	@echo "  HOST         = $(HOST)"
	@echo "  VIDEO_PORT   = $(VIDEO_PORT)"
	@echo "  AUDIO_PORT   = $(AUDIO_PORT)"
	@echo "  WIDTH        = $(WIDTH)"
	@echo "  HEIGHT       = $(HEIGHT)"
	@echo "  FRAMERATE    = $(FRAMERATE)"
	@echo "  CODEC        = $(CODEC)"
	@echo "  AUDIO_DEVICE = $(AUDIO_DEVICE)"
	@echo ""

.PHONY: help send_264 send_o send_p send_test send_264_v4l2 send_264_libcam send_264_libcam_filepipe
