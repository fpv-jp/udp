### ------------ Launch Config ------------
GST_LAUNCH := GST_DEBUG=2 gst-launch-1.0 -v -e

### ------------ Network Config ------------
HOST ?= 127.0.0.1
VIDEO_PORT ?= 5000
AUDIO_PORT ?= 5001

VIDEO_UDP_SINK := udpsink host=$(HOST) port=$(VIDEO_PORT) sync=false async=false
AUDIO_UDP_SINK := udpsink host=$(HOST) port=$(AUDIO_PORT) sync=false async=false

# queue
SEND_QUEUE1 := queue max-size-buffers=1 leaky=downstream
SEND_QUEUE3 := queue max-size-buffers=3 leaky=downstream

### ------------ Video Config ------------
VIDEO_SRC_TEST  := videotestsrc pattern=ball is-live=true
VIDEO_SRC_V4L2  := nvv4l2camerasrc
VIDEO_SRC_ARGUS := nvarguscamerasrc

WIDTH      ?= 1920
HEIGHT     ?= 1080
FRAMERATE  ?= 30
FORMAT     ?= NV12
VIDEO_CAP  := video/x-raw,width=$(WIDTH),height=$(HEIGHT),framerate=$(FRAMERATE)/1
VIDEO_CONV := nvvidconv ! video/x-raw\(memory:NVMM\),format=$(FORMAT)

VIDEO_MEMORY_NVMM_CAP := video/x-raw\(memory:NVMM\),width=$(WIDTH),height=$(HEIGHT),framerate=$(FRAMERATE)/1

# nvv4l2camerasrc: { UYVY }
#   interlace-mode: { progressive, interlaced }

# nvarguscamerasrc: { NV12, P010_10LE }

# nvvidconv
#   src:  { I420, I420_10LE, P010_10LE, UYVY, YUY2, YVYU, NV12, NV16, NV24, GRAY8, BGRx, RGBA, Y42B }
#   sink: { I420, I420_10LE, P010_10LE, I420_12LE, UYVY, YUY2, YVYU, NV12, NV16, NV24, GRAY8, BGRx, RGBA, Y42B }

# nvv4l2h264enc: { I420, NV12, P010_10LE, NV24 }
# nvv4l2h265enc: { I420, NV12, P010_10LE, NV24 }
# nvv4l2vp8enc:  { I420, NV12, P010_10LE, NV24 }
# nvv4l2vp9enc:  { I420, NV12, P010_10LE, NV24 }

### ------------ Targets ------------

send_test:
	$(GST_LAUNCH) $(VIDEO_SRC_TEST) ! $(VIDEO_CAP) ! $(VIDEO_CONV) ! $(SEND_QUEUE1) ! \
		nvv4l2h264enc preset-level=3 profile=4 bitrate=20000000 ! \
		capsfilter caps=video/x-h264,level=\(string\)4 ! $(SEND_QUEUE3) ! \
		rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

send_264:
	$(GST_LAUNCH) $(VIDEO_SRC_ARGUS) ! $(VIDEO_MEMORY_NVMM_CAP) ! $(SEND_QUEUE1) ! \
		nvv4l2h264enc preset-level=3 profile=4 bitrate=20000000 ! \
		capsfilter caps=video/x-h264,level=\(string\)4 ! $(SEND_QUEUE3) ! \
		rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

send_265:
	$(GST_LAUNCH) $(VIDEO_SRC_ARGUS) ! $(VIDEO_MEMORY_NVMM_CAP) ! $(SEND_QUEUE1) ! \
		nvv4l2h265enc preset-level=3 profile=0 bitrate=30000000 ! \
		capsfilter caps=video/x-h265,level=\(string\)4 ! $(SEND_QUEUE3) ! \
		rtph265pay config-interval=1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

send_8:
	$(GST_LAUNCH) $(VIDEO_SRC_ARGUS) ! $(VIDEO_MEMORY_NVMM_CAP) ! $(SEND_QUEUE1) ! \
		nvv4l2vp8enc bitrate=20000000 ! \
		rtpvp8pay ! $(VIDEO_UDP_SINK)

send_9:
	$(GST_LAUNCH) $(VIDEO_SRC_ARGUS) ! $(VIDEO_MEMORY_NVMM_CAP) ! $(SEND_QUEUE1) ! \
		nvv4l2vp9enc bitrate=30000000 ! \
		rtpvp9pay ! $(VIDEO_UDP_SINK)

# USB camera (UVC)
send_264v:
	$(GST_LAUNCH) $(VIDEO_SRC_V4L2) ! $(VIDEO_MEMORY_NVMM_CAP) ! $(VIDEO_CONV) ! $(SEND_QUEUE1) ! \
		nvv4l2h264enc preset-level=3 profile=4 bitrate=20000000 ! \
		capsfilter caps=video/x-h264,level=\(string\)4 ! $(SEND_QUEUE3) ! \
		rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

### ------------ Help ------------
help:
	@echo "Usage:"
	@echo "  make send_264      # Send H.264 video using nvarguscamerasrc"
	@echo "  make send_265      # Send H.265 video using nvarguscamerasrc"
	@echo "  make send_8        # Send VP8 video using nvarguscamerasrc"
	@echo "  make send_9        # Send VP9 video using nvarguscamerasrc"
	@echo "  make send_test     # Test stream using videotestsrc"
	@echo ""
	@echo "Defaults:"
	@echo "  HOST         = $(HOST)"
	@echo "  VIDEO_PORT   = $(VIDEO_PORT)"
	@echo "  WIDTH        = $(WIDTH)"
	@echo "  HEIGHT       = $(HEIGHT)"
	@echo "  FRAMERATE    = $(FRAMERATE)"
	@echo ""

.PHONY: help send_264 send_265 send_8 send_9 send_test
