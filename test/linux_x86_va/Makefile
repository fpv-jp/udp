#################################################################################################################################
# Basic common settings
#################################################################################################################################

# ------------ Launch Config ------------
GST_LAUNCH := GST_DEBUG=2 gst-launch-1.0 -v -e

SEND_QUEUE := queue max-size-buffers=1 leaky=downstream
RECV_QUEUE := queue max-size-buffers=3

# ------------ Network Config ------------
HOST ?= 127.0.0.1
VIDEO_PORT ?= 5000
AUDIO_PORT ?= 5001

VIDEO_UDP_SINK := udpsink host=$(HOST) port=$(VIDEO_PORT) sync=false async=false
AUDIO_UDP_SINK := udpsink host=$(HOST) port=$(AUDIO_PORT) sync=false async=false

VIDEO_UDP_SRC := udpsrc port=$(VIDEO_PORT) caps="application/x-rtp, media=video, encoding-name=
AUDIO_UDP_SRC := udpsrc port=$(AUDIO_PORT) caps="application/x-rtp, media=audio, encoding-name=

# ------------ Video capture resolution ------------
WIDTH     ?= 640
HEIGHT    ?= 480
FRAMERATE ?= 30/1

VIDEO_CAP := video/x-raw,format=YUY2,width=$(WIDTH),height=$(HEIGHT),framerate=$(FRAMERATE)

# vapostproc: YUY2 (system memory) → NV12 (VAMemory, Intel GPU)
VIDEO_CAP_NV12 := $(VIDEO_CAP) ! vapostproc ! 'video/x-raw(memory:VAMemory),format=NV12'
# For software encoders (vp8enc, svtav1enc) that need system memory I420
VIDEO_CAP_I420 := $(VIDEO_CAP) ! vapostproc ! videoconvert ! video/x-raw,format=I420

# ------------ Audio sampling rate ------------
RATE     ?= 48000
CHANNELS ?= 2

AUDIO_CAP := audio/x-raw,rate=$(RATE),channels=$(CHANNELS)

# Basic common audio codec
###########################################

# ------------ Opus ------------
# opus:  opusdec: Opus audio decoder
# sink format: audio/x-opus
#         channel-mapping-family: 0 (gint)
# sink format: audio/x-opus
#         channel-mapping-family: [ 1, 255 ]
#                channels: [ 1, 255 ]
#            stream-count: [ 1, 255 ]
#           coupled-count: [ 0, 255 ]
# src format: { S16LE }
#                  layout: interleaved
#                    rate: { 48000, 24000, 16000, 12000, 8000 }
#                channels: [ 1, 255 ]

OPUS_SEND := $(AUDIO_CAP) ! audioconvert ! audioresample ! opusenc ! rtpopuspay ! $(AUDIO_UDP_SINK)

# opus:  opusenc: Opus audio encoder
# sink format: { S16LE }
#                  format: S16LE
#                  layout: interleaved
#                    rate: 48000 (gint)
#                channels: [ 1, 255 ]
# sink format: { S16LE }
#                  format: S16LE
#                  layout: interleaved
#                    rate: { 8000, 12000, 16000, 24000 }
#                channels: [ 1, 255 ]
# src format: audio/x-opus

OPUS_RECV := $(AUDIO_UDP_SRC)OPUS" ! rtpopusdepay ! opusdec ! audioconvert

# ------------ μ-law ------------

# mulaw:  mulawenc: Mu Law audio encoder
# sink format: { S16LE }
#                  layout: interleaved
#                    rate: [ 8000, 192000 ]
#                channels: [ 1, 2 ]
# src format: audio/x-mulaw
#                    rate: [ 8000, 192000 ]
#                channels: [ 1, 2 ]

MULAW_SEND := $(AUDIO_CAP) ! audioconvert ! audioresample ! mulawenc ! rtppcmupay ! $(AUDIO_UDP_SINK)

# mulaw:  mulawdec: Mu Law audio decoder
# sink format: audio/x-mulaw
#                    rate: [ 8000, 192000 ]
#                channels: [ 1, 2 ]
# src format: { S16LE }
#                  layout: interleaved
#                    rate: [ 8000, 192000 ]
#                channels: [ 1, 2 ]

MULAW_RECV := $(AUDIO_UDP_SRC)PCMU" ! rtppcmudepay ! mulawdec ! audioconvert

#################################################################################################################################
# Individual settings for each device
#################################################################################################################################

# ------------ Video src ------------
VIDEO_DEVICE ?= 0
VIDEO_SRC   := v4l2src device=/dev/video$(VIDEO_DEVICE) do-timestamp=true

# ------------ Video sink ------------
VIDEO_SINK := waylandsink sync=false async=false

# ------------ Audio src ------------
AUDIO_DEVICE ?= 'hw:0,0'
AUDIO_SRC := alsasrc device=$(AUDIO_DEVICE) do-timestamp=true

# ------------ Audio sink ------------
AUDIO_SINK := pulsesink sync=false async=false

# Video codec
###########################################
# va:  vapostproc: VA-API Video Postprocessor in Intel(R) Gen Graphics
# sink format: video/x-raw(memory:VAMemory)
#                  format: { NV12, I420, YV12, YUY2, Y42B, RGBA, BGRA, RGBP, RGBx, P010_10LE, RGB, BGR10A2_LE, VUYA, Y210, Y410, P012_LE, Y212_LE, Y412_LE }
# sink format: video/x-raw(memory:DMABuf)
#                  format: DMA_DRM
#              drm-format: { NV12:0x0100000000000002, YU12, YV12, YUYV:0x0100000000000002, YU16:0x0100000000000002, AB24:0x0100000000000002, AR24:0x0100000000000002, XB24:0x0100000000000002, P010:0x0100000000000002, AR30:0x0100000000000002, AYUV:0x0100000000000002, Y210:0x0100000000000002, Y410:0x0100000000000002, P012:0x0100000000000002, Y212:0x0100000000000002, Y412:0x0100000000000002 }
# sink format: video/x-raw
#                  format: { BGRA, RGBA, BGRx, RGBx, BGR10A2_LE, RGB16, VUYA, GRAY8, NV12, NV21, YUY2, UYVY, YV12, I420, Y42B, Y444, P010_10LE, P012_LE, Y210, Y212_LE, Y410, Y412_LE, RGBP }
# src format: video/x-raw(memory:VAMemory)
#                  format: { NV12, I420, YV12, YUY2, Y42B, RGBA, BGRA, RGBP, RGBx, P010_10LE, RGB, BGR10A2_LE, VUYA, Y210, Y410, P012_LE, Y212_LE, Y412_LE }
# sink format: video/x-raw(memory:DMABuf)
#                  format: DMA_DRM
#              drm-format: { NV12:0x0100000000000002, YU12, YV12, YUYV:0x0100000000000002, YU16:0x0100000000000002, AB24:0x0100000000000002, AR24:0x0100000000000002, XB24:0x0100000000000002, P010:0x0100000000000002, AR30:0x0100000000000002, AYUV:0x0100000000000002, Y210:0x0100000000000002, Y410:0x0100000000000002, P012:0x0100000000000002, Y212:0x0100000000000002, Y412:0x0100000000000002 }
# sink format: video/x-raw
#                  format: { BGRA, RGBA, BGRx, RGBx, BGR10A2_LE, RGB16, VUYA, GRAY8, NV12, NV21, YUY2, UYVY, YV12, I420, Y42B, Y444, P010_10LE, P012_LE, Y210, Y212_LE, Y410, Y412_LE, RGBP }

# ------------ H.264 ------------

# va:  vah264lpenc: VA-API H.264 Low Power Encoder in Intel(R) Gen Graphics
# sink format: video/x-raw(memory:VAMemory)
#                  format: NV12
# sink format: video/x-raw(memory:DMABuf)
#                  format: DMA_DRM
#              drm-format: NV12:0x0100000000000002
# sink format: video/x-raw
#                  format: NV12
# src format: video/x-h264
#                 profile: { main, high, constrained-baseline }
#               alignment: au
#           stream-format: byte-stream

H264_SEND := $(VIDEO_CAP_NV12) ! vah264lpenc ! h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

# va:  vah264dec: VA-API H.264 Decoder in Intel(R) Gen Graphics
# sink format: video/x-h264
#                 profile: { main, extended, baseline, high, progressive-high, constrained-high, constrained-baseline }
#               alignment: au
#           stream-format: { avc, avc3, byte-stream }
# src format: video/x-raw(memory:VAMemory)
#                  format: NV12
# src format: video/x-raw(memory:DMABuf)
#                  format: DMA_DRM
#              drm-format: NV12:0x0100000000000002
# src format: video/x-raw
#                  format: NV12

H264_RECV := $(VIDEO_UDP_SRC)H264" ! rtph264depay ! h264parse ! vah264dec ! videoconvert

# ------------ H.265 ------------

# va:  vah265lpenc: VA-API H.265 Low Power Encoder in Intel(R) Gen Graphics
# sink format: video/x-raw(memory:VAMemory)
#                  format: { NV12, P010_10LE, VUYA, Y410 }
# sink format: video/x-raw(memory:DMABuf)
#                  format: DMA_DRM
#              drm-format: { NV12:0x0100000000000002, P010:0x0100000000000002, AYUV:0x0100000000000002, Y410:0x0100000000000002 }
# sink format: video/x-raw
#                  format: { NV12, P010_10LE, VUYA, Y410 }
# src format: video/x-h265
#                 profile: { main, main-10, main-444, main-444-10, screen-extended-main, screen-extended-main-10, screen-extended-main-444, screen-extended-main-444-10 }
#               alignment: au
#           stream-format: byte-stream

H265_SEND := $(VIDEO_CAP_NV12) ! vah265lpenc ! h265parse ! rtph265pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

# va:  vah265dec: VA-API H.265 Decoder in Intel(R) Gen Graphics
# sink format: video/x-h265
#                 profile: { main, main-intra, main-still-picture, main-10, main-10-intra, main-12, main-422-10, main-422-12, main-444, main-444-10, main-444-12, screen-extended-main, screen-extended-main-10, screen-extended-main-444, screen-extended-main-444-10 }
#               alignment: au
#           stream-format: { hvc1, hev1, byte-stream }
# src format: video/x-raw(memory:VAMemory)
#                  format: { P010_10LE, YUY2, Y210, Y212_LE, VUYA, Y410, P012_LE, Y412_LE, NV12 }
# src format: video/x-raw(memory:DMABuf)
#                  format: DMA_DRM
#              drm-format: { P010:0x0100000000000002, YUYV:0x0100000000000002, Y210:0x0100000000000002, Y212:0x0100000000000002, AYUV:0x0100000000000002, Y410:0x0100000000000002, P012:0x0100000000000002, Y412:0x0100000000000002, NV12:0x0100000000000002 }
# src format: video/x-raw
#                  format: { P010_10LE, YUY2, Y210, Y212_LE, VUYA, Y410, P012_LE, Y412_LE, NV12 }

H265_RECV := $(VIDEO_UDP_SRC)H265" ! rtph265depay ! h265parse ! vah265dec ! videoconvert

# ------------ VP8 ------------
# vpx:  vp8enc: On2 VP8 Encoder
# sink format: { I420 }
# src format: video/x-vp8
#                 profile: { 0, 1, 2, 3 }

VP8_SEND := $(VIDEO_CAP_I420) ! vp8enc deadline=1 ! rtpvp8pay ! $(VIDEO_UDP_SINK)

# va:  vavp8dec: VA-API VP8 Decoder in Intel(R) Gen Graphics
# sink format: video/x-vp8
# src format: video/x-raw(memory:VAMemory)
#                  format: NV12
# src format: video/x-raw(memory:DMABuf)
#                  format: DMA_DRM
#              drm-format: NV12:0x0100000000000002
# src format: video/x-raw
#                  format: NV12

VP8_RECV := $(VIDEO_UDP_SRC)VP8" ! rtpvp8depay ! vavp8dec ! videoconvert

# ------------ VP9 ------------
# vpx:  vp9enc: On2 VP9 Encoder
# sink format: { I420, YV12, Y444, I420_10LE, I420_12LE, I422_10LE, I422_12LE, Y444_10LE, Y444_12LE }
# src format: video/x-vp9
#                 profile: { 0, 1, 2, 3 }

# VP9_SEND := $(VIDEO_CAP_I420) ! vp9enc deadline=1 cpu-used=8 threads=4 lag-in-frames=0 ! vp9parse ! rtpvp9pay ! $(VIDEO_UDP_SINK)
VP9_SEND := $(VIDEO_CAP_I420) ! vp9enc deadline=1 cpu-used=4 ! vp9parse ! rtpvp9pay ! $(VIDEO_UDP_SINK)

# va:  vavp9dec: VA-API VP9 Decoder in Intel(R) Gen Graphics
# sink format: video/x-vp9
#                 profile: { 0, 1, 2, 3 }
#               alignment: frame
# src format: video/x-raw(memory:VAMemory)
#                  format: { NV12, VUYA, P010_10LE, P012_LE, Y410, Y412_LE }
# src format: video/x-raw(memory:DMABuf)
#                  format: DMA_DRM
#              drm-format: { NV12:0x0100000000000002, AYUV:0x0100000000000002, P010:0x0100000000000002, P012:0x0100000000000002, Y410:0x0100000000000002, Y412:0x0100000000000002 }
# src format: video/x-raw
#                  format: { NV12, VUYA, P010_10LE, P012_LE, Y410, Y412_LE }

VP9_RECV := $(VIDEO_UDP_SRC)VP9" ! rtpvp9depay ! vp9parse ! vavp9dec ! videoconvert

# ------------ AV1 ------------
# svtav1:  svtav1enc: SvtAv1Enc
# sink format: { I420, I420_10LE }
# src format: video/x-av1
#           stream-format: obu-stream
#               alignment: tu

AV1_SEND := $(VIDEO_CAP_I420) ! svtav1enc ! av1parse ! rtpav1pay ! $(VIDEO_UDP_SINK)

# dav1d:  dav1ddec: Dav1d AV1 Decoder
# sink format: video/x-av1
#           stream-format: obu-stream
#               alignment: { frame, tu }
# src format: format: { GRAY8, GRAY16_LE, I420, Y42B, Y444, I420_10LE, I422_10LE, Y444_10LE, I420_12LE, I422_12LE, Y444_12LE }

AV1_RECV := $(VIDEO_UDP_SRC)AV1" ! rtpav1depay ! av1parse ! dav1ddec ! videoconvert

# ------------ Video ------------

### H.264 ###
send_264:
	$(GST_LAUNCH) $(VIDEO_SRC) ! $(H264_SEND)

recv_264:
	$(GST_LAUNCH) $(H264_RECV) ! $(VIDEO_SINK)

### H.265 ###
send_265:
	$(GST_LAUNCH) $(VIDEO_SRC) ! $(H265_SEND)

recv_265:
	$(GST_LAUNCH) $(H265_RECV) ! $(VIDEO_SINK)

### VP8 ###
send_8:
	$(GST_LAUNCH) $(VIDEO_SRC) ! $(VP8_SEND)

recv_8:
	$(GST_LAUNCH) $(VP8_RECV) ! $(VIDEO_SINK)

### VP9 ###
send_9:
	$(GST_LAUNCH) $(VIDEO_SRC) ! $(VP9_SEND)

recv_9:
	$(GST_LAUNCH) $(VP9_RECV) ! $(VIDEO_SINK)

### AV1 ###
send_1:
	$(GST_LAUNCH) $(VIDEO_SRC) ! $(AV1_SEND)

recv_1:
	$(GST_LAUNCH) $(AV1_RECV) ! $(VIDEO_SINK)

# ------------ Audio ------------

### Opus ###
send_o:
	$(GST_LAUNCH) $(AUDIO_SRC) ! $(OPUS_SEND)

recv_o:
	$(GST_LAUNCH) $(OPUS_RECV) ! $(AUDIO_SINK)

### PCMU (G.711 μ-law) ###
send_p:
	$(GST_LAUNCH) $(AUDIO_SRC) ! $(MULAW_SEND)

recv_p:
	$(GST_LAUNCH) $(MULAW_RECV) ! $(AUDIO_SINK)

help:
	@echo "Usage:"
	@echo "  make send_264      # Send H.264 video (via vah264lpenc, VA-API)"
	@echo "  make recv_264      # Receive H.264 video (via vah264dec, VA-API)"
	@echo "  make send_265      # Send H.265 video (via vah265lpenc, VA-API)"
	@echo "  make recv_265      # Receive H.265 video (via vah265dec, VA-API)"
	@echo "  make send_8        # Send VP8 video (via vp8enc, software)"
	@echo "  make recv_8        # Receive VP8 video (via vavp8dec, VA-API)"
	@echo "  make send_9        # Send VP9 video (via vp9enc, software)"
	@echo "  make recv_9        # Receive VP9 video (via vavp9dec, VA-API)"
	@echo "  make send_1        # Send AV1 video (via svtav1enc, software)"
	@echo "  make recv_1        # Receive AV1 video (via dav1ddec, software)"
	@echo "  make send_o        # Send audio (Opus)"
	@echo "  make recv_o        # Receive audio (Opus)"
	@echo "  make send_p        # Send audio (G.711 PCMU)"
	@echo "  make recv_p        # Receive audio (G.711 PCMU)"
	@echo ""
	@echo "Default settings:"
	@echo "  HOST=$(HOST), VIDEO_PORT=$(VIDEO_PORT), AUDIO_PORT=$(AUDIO_PORT)"
	@echo "  WIDTH=$(WIDTH), HEIGHT=$(HEIGHT), FRAMERATE=$(FRAMERATE)"
	@echo "  RATE=$(RATE), CHANNELS=$(CHANNELS)"
	@echo "  VIDEO_DEVICE=$(VIDEO_DEVICE), AUDIO_DEVICE=$(AUDIO_DEVICE)"

.PHONY: send_264 recv_264 send_265 recv_265 send_8 recv_8 send_9 recv_9 send_1 recv_1 send_o recv_o send_p recv_p help
