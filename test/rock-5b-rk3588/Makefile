### ------------ Launch Config ------------
GST_LAUNCH := GST_DEBUG=2 gst-launch-1.0 -v -e

### ------------ Network Config ------------
HOST ?= 192.168.151.1
VIDEO_PORT ?= 5000
AUDIO_PORT ?= 5001

VIDEO_UDP_SINK := udpsink host=$(HOST) port=$(VIDEO_PORT) sync=false async=false
AUDIO_UDP_SINK := udpsink host=$(HOST) port=$(AUDIO_PORT) sync=false async=false

# queue
SEND_QUEUE1 := queue max-size-buffers=1 leaky=downstream

### ------------ Video Config ------------
VIDEO_DEVICE   ?= 1
VIDEO_SRC_V4L2 := v4l2src device=/dev/video$(VIDEO_DEVICE)
VIDEO_SRC_TEST := videotestsrc pattern=ball is-live=true

WIDTH      ?= 640
HEIGHT     ?= 480
FRAMERATE  ?= 30
CAP_FORMAT ?= YUY2
FORMAT     ?= NV12

VIDEO_CAP  := video/x-raw,width=$(WIDTH),height=$(HEIGHT),framerate=$(FRAMERATE)/1,format=${CAP_FORMAT}
VIDEO_CONV := videoconvert ! video/x-raw,format=$(FORMAT)

# mpph264enc: { NV12, I420, YUY2, UYVY, BGR16, RGB16, ABGR, ARGB, BGRA, RGBA, xBGR, xRGB, BGRx, RGBx, NV12, NV21, I420, YV12, NV16, NV61, BGR16, RGB, BGR, RGBA, BGRA, RGBx, BGRx }
# mpph265enc: { NV12, I420, YUY2, UYVY, BGR16, RGB16, ABGR, ARGB, BGRA, RGBA, xBGR, xRGB, BGRx, RGBx, NV12, NV21, I420, YV12, NV16, NV61, BGR16, RGB, BGR, RGBA, BGRA, RGBx, BGRx }
# mppvp8enc:  { NV12, I420, YUY2, UYVY, BGR16, RGB16, ABGR, ARGB, BGRA, RGBA, xBGR, xRGB, BGRx, RGBx, NV12, NV21, I420, YV12, NV16, NV61, BGR16, RGB, BGR, RGBA, BGRA, RGBx, BGRx }

### ------------ Targets ------------

send_264:
	$(GST_LAUNCH) $(VIDEO_SRC_V4L2) ! $(VIDEO_CAP) ! $(VIDEO_CONV) ! $(SEND_QUEUE1) ! \
		mpph264enc level=40 profile=100 ! \
		rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

send_265:
	$(GST_LAUNCH) $(VIDEO_SRC_V4L2) ! $(VIDEO_CAP) ! $(VIDEO_CONV) ! $(SEND_QUEUE1) ! \
		mpph265enc ! \
		rtph265pay config-interval=1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

send_8:
	$(GST_LAUNCH) $(VIDEO_SRC_V4L2) ! $(VIDEO_CAP) ! $(VIDEO_CONV) ! $(SEND_QUEUE1) ! \
		mppvp8enc ! \
		rtpvp8pay ! $(VIDEO_UDP_SINK)

send_test:
	$(GST_LAUNCH) $(VIDEO_SRC_V4L2) ! $(VIDEO_CAP) ! $(VIDEO_CONV) ! $(SEND_QUEUE1) ! \
		mpph264enc ! \
		rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

### ------------ Help ------------
help:
	@echo "Usage:"
	@echo "  make send_264      # Send H.264 video using v4l2src"
	@echo "  make send_265      # Send H.265 video using v4l2src"
	@echo "  make send_8        # Send VP8 video using v4l2src"
	@echo "  make send_test     # Test stream using videotestsrc"
	@echo ""

.PHONY: help send_264 send_265 send_8 send_test
