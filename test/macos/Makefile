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

VIDEO_CAP := video/x-raw,width=$(WIDTH),height=$(HEIGHT),framerate=$(FRAMERATE)

# VIDEO_CAP_UYVY := $(VIDEO_CAP) ! videoconvert ! video/x-raw,format=UYVY
# VIDEO_CAP_YUYV := $(VIDEO_CAP) ! videoconvert ! video/x-raw,format=YUYV
# VIDEO_CAP_NV12 := $(VIDEO_CAP) ! videoconvert ! video/x-raw,format=NV12
VIDEO_CAP_I420 := $(VIDEO_CAP) ! videoconvert ! video/x-raw,format=I420
# If the camera supports it
# VIDEO_CAP_UYVY := $(VIDEO_CAP),format=UYVY
# VIDEO_CAP_YUYV := $(VIDEO_CAP),format=YUYV
VIDEO_CAP_NV12 := $(VIDEO_CAP),format=NV12
# VIDEO_CAP_I420 := $(VIDEO_CAP),format=I420

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
# applemedia:  avfvideosrc: Video Source (AVFoundation)
# src format GLMemory: { UYVY }
# src format: { NV12, UYVY, YUY2 }
# src format: { BGRA }
VIDEO_DEVICE ?= 0
VIDEO_SRC := avfvideosrc do-stats=true do-timestamp=true device-index=$(VIDEO_DEVICE)

# ------------ Video sink ------------
# osxvideo:  osxvideosink: macOS Video sink
# sink format: { UYVY }
VIDEO_SINK := osxvideosink sync=false async=false

# ------------ Audio src ------------
# osxaudio:  osxaudiosrc: Audio Source (macOS)
# src format: { F64LE, F64BE, F32LE, F32BE, S32LE, S32BE, U32LE, U32BE, S24_32LE, S24_32BE, U24_32LE, U24_32BE, S24LE, S24BE, U24LE, U24BE, S20_32LE, S20_32BE, U20_32LE, U20_32BE, S20LE, S20BE, U20LE, U20BE, S18LE, S18BE, U18LE, U18BE, S16LE, S16BE, U16LE, U16BE, S8, U8 }
AUDIO_DEVICE ?= 0
AUDIO_SRC := osxaudiosrc device=$(AUDIO_DEVICE) do-timestamp=true

# ------------ Audio sink ------------
# osxaudio:  osxaudiosink: Audio Sink (macOS)
# sink format: { F64LE, F64BE, F32LE, F32BE, S32LE, S32BE, U32LE, U32BE, S24_32LE, S24_32BE, U24_32LE, U24_32BE, S24LE, S24BE, U24LE, U24BE, S20LE, S20BE, U20LE, U20BE, S18LE, S18BE, U18LE, U18BE, S16LE, S16BE, U16LE, U16BE, S8, U8 }
AUDIO_SINK := osxaudiosink sync=false async=false

# Video codec
###########################################

# ------------ H.264 ------------
# libav:  avdec_h264: libav H.264 / AVC / MPEG-4 AVC / MPEG-4 part 10 decoder
# sink format: video/x-h264
#               alignment: au
#           stream-format: { avc, byte-stream }
# sink format: video/x-h264
#               alignment: nal
#           stream-format: byte-stream
# src format: { I420, YUY2, RGB, BGR, Y42B, Y444, YUV9, Y41B, GRAY8, RGB8P, I420, Y42B, Y444, UYVY, NV12, NV21, ARGB, RGBA, ABGR, BGRA, GRAY16_BE, GRAY16_LE, A420, RGB16, RGB15, I420_10BE, I420_10LE, I422_10BE, I422_10LE, Y444_10BE, Y444_10LE, GBR, GBR_10BE, GBR_10LE, A420_10BE, A420_10LE, A422_10BE, A422_10LE, A444_10BE, A444_10LE, GBRA, xRGB, RGBx, xBGR, BGRx, I420_12BE, I420_12LE, I422_12BE, I422_12LE, Y444_12BE, Y444_12LE, GBR_12BE, GBR_12LE, P010_10LE, GBRA_12BE, GBRA_12LE, GBRA_10BE, GBRA_10LE, GRAY10_LE16, VUYA, P012_LE, Y212_LE,Y410, Y412_LE }

H264_RECV := $(VIDEO_UDP_SRC)H264" ! rtph264depay ! h264parse ! avdec_h264 ! videoconvert

# ------------ H.264 ------------
# applemedia:  vtenc_h264_hw: H.264 (HW only) encoder
# sink format: { AYUV64, UYVY, NV12, I420, P010_10LE, ARGB64_BE, RGBA64_LE }
# src format: video/x-h264
#          interlace-mode: { progressive, interleaved }
#           stream-format: avc
#               alignment: au

H264_SEND := $(VIDEO_CAP_NV12) ! vtenc_h264_hw realtime=true ! h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

# ------------ H.265 ------------
# libav:  avdec_h265: libav HEVC (High Efficiency Video Coding) decoder
# sink format: video/x-h265
#               alignment: au
#           stream-format: { hvc1, hev1, byte-stream }
# src format: { I420, YUY2, RGB, BGR, Y42B, Y444, YUV9, Y41B, GRAY8, RGB8P, I420, Y42B, Y444, UYVY, NV12, NV21, ARGB, RGBA, ABGR, BGRA, GRAY16_BE, GRAY16_LE, A420, RGB16, RGB15, I420_10BE, I420_10LE, I422_10BE, I422_10LE, Y444_10BE, Y444_10LE, GBR, GBR_10BE, GBR_10LE, A420_10BE, A420_10LE, A422_10BE, A422_10LE, A444_10BE, A444_10LE, GBRA, xRGB, RGBx, xBGR, BGRx, I420_12BE, I420_12LE, I422_12BE, I422_12LE, Y444_12BE, Y444_12LE, GBR_12BE, GBR_12LE, P010_10LE, GBRA_12BE, GBRA_12LE, GBRA_10BE, GBRA_10LE, GRAY10_LE16, VUYA, P012_LE, Y212_LE,Y410, Y412_LE }

H265_RECV := $(VIDEO_UDP_SRC)H265" ! rtph265depay ! h265parse ! avdec_h265 ! videoconvert

# ------------ H.265 ------------
# applemedia:  vtenc_h265_hw: H.265/HEVC (HW only) encoder
# sink format: { AYUV64, UYVY, NV12, I420, P010_10LE, ARGB64_BE, RGBA64_LE }
# src format: video/x-h265
#          interlace-mode: { progressive, interleaved }
#           stream-format: hvc1
#               alignment: au

# ------------ H.265(alpha) ------------
# applemedia:  vtenc_h265a_hw: H.265/HEVC with alpha (HW only) encoder
#         (Same as vtenc_h265_hw)

H265_SEND := $(VIDEO_CAP_NV12) ! vtenc_h265_hw realtime=true allow-frame-reordering=false ! h265parse ! rtph265pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

# ------------ VP8 ------------
# vpx:  vp8enc: On2 VP8 Encoder
# sink format: { I420 }
# src format: video/x-vp8
#                 profile: { 0, 1, 2, 3 }

VP8_SEND := $(VIDEO_CAP_I420) ! vp8enc deadline=1 ! rtpvp8pay ! $(VIDEO_UDP_SINK)

# vpx:  vp8dec: On2 VP8 Decoder
# sink format: video/x-vp8
# src format: { I420 }

VP8_RECV := $(VIDEO_UDP_SRC)VP8" ! rtpvp8depay ! vp8dec ! videoconvert

# ------------ VP9 ------------
# vpx:  vp9enc: On2 VP9 Encoder
# sink format: { I420, YV12, Y444, I420_10LE, I420_12LE, I422_10LE, I422_12LE, Y444_10LE, Y444_12LE }
# src format: video/x-vp9
#                 profile: { 0, 1, 2, 3 }

# VP9_SEND := $(VIDEO_CAP_I420) ! vp9enc deadline=1 cpu-used=8 threads=4 lag-in-frames=0 ! vp9parse ! rtpvp9pay ! $(VIDEO_UDP_SINK)
VP9_SEND := $(VIDEO_CAP_I420) ! vp9enc deadline=1 cpu-used=4 ! vp9parse ! rtpvp9pay ! $(VIDEO_UDP_SINK)

# vpx:  vp9dec: On2 VP9 Decoder
# sink format: video/x-vp9
# src format: { I420, YV12, Y42B, Y444, GBR, I420_10LE, I420_12LE, I422_10LE, I422_12LE, Y444_10LE, Y444_12LE, GBR_10LE, GBR_12LE }

VP9_RECV := $(VIDEO_UDP_SRC)VP9" ! rtpvp9depay ! vp9parse ! vp9dec threads=4 ! videoconvert

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
	@echo "  make send_264      # Send H.264 video (via vtenc_h264_hw)"
	@echo "  make recv_264      # Receive H.264 video"
	@echo "  make send_265      # Send H.265 video (via vtenc_h265_hw)"
	@echo "  make recv_265      # Receive H.265 video"
	@echo "  make send_8        # Send VP8 video"
	@echo "  make recv_8        # Receive VP8 video"
	@echo "  make send_9        # Send VP9 video"
	@echo "  make recv_9        # Receive VP9 video"
	@echo "  make send_1        # Send AV1 video (via svtav1enc)"
	@echo "  make recv_1        # Receive AV1 video"
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
