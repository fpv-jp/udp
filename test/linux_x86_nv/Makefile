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
# VIDEO_CAP_NV12 := $(VIDEO_CAP),format=NV12
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

# nvvideoconvert:  nvvideoconvert: NvVidConv Plugin
# sink format: 
#       video/x-raw(memory:NVMM)
#                  format: { I420, NV12, P010_10LE, I420_12LE, BGRx, RGBA, Y444, Y444_10LE, Y444_12LE, GRAY8, GRAY16_LE, GBR, RGB, BGR, BGR10A2_LE, RGB10A2_LE, UYVP, UYVY, BGRA64_LE }
#       video/x-raw
#                  format: { I420, NV12, P010_10LE, BGRx, RGBA, Y444, GRAY8, GRAY16_LE, GBR, RGB, BGR, BGR10A2_LE, RGB10A2_LE, UYVP, UYVY, BGRA64_LE }
# src format: 
#       video/x-raw(memory:NVMM)
#                  format: { I420, NV12, P010_10LE, I420_12LE, BGRx, RGBA, Y444, Y444_10LE, Y444_12LE, GRAY8, GRAY16_LE, GBR, RGB, BGR, BGR10A2_LE, RGB10A2_LE, UYVP, UYVY, BGRA64_LE }
#       video/x-raw
#                  format: { I420, NV12, P010_10LE, BGRx, RGBA, Y444, GRAY8, GRAY16_LE, GBR, RGB, BGR, BGR10A2_LE, RGB10A2_LE, UYVP, UYVY, BGRA64_LE }

# ------------ H.264 ------------

# nvvideo4linux2:  nvv4l2h264enc: V4L2 H.264 Encoder
# sink format: video/x-raw(memory:NVMM)
#                  format: { I420, NV12, P010_10LE, Y444, Y444_10LE, NV24 }
# src format: video/x-h264
#           stream-format: byte-stream
#               alignment: { au, nal }
H264_SEND := $(VIDEO_CAP_I420) ! nvvideoconvert ! nvv4l2h264enc ! h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

# ------------ H.265 ------------
# nvvideo4linux2:  nvv4l2h265enc: V4L2 H.265 Encoder
# sink format: video/x-raw(memory:NVMM)
#                  format: { I420, NV12, P010_10LE, Y444, Y444_10LE, NV24 }
# src format: video/x-h265
#           stream-format: byte-stream
#               alignment: au
H265_SEND := $(VIDEO_CAP_I420) ! nvvideoconvert ! nvv4l2h265enc ! h265parse ! rtph265pay config-interval=-1 aggregate-mode=zero-latency ! $(VIDEO_UDP_SINK)

# ------------ VP8 ------------
# vpx:  vp8enc: On2 VP8 Encoder
# sink format: { I420 }
# src format: video/x-vp8
#                 profile: { 0, 1, 2, 3 }

VP8_SEND := $(VIDEO_CAP_I420) ! vp8enc deadline=1 ! rtpvp8pay ! $(VIDEO_UDP_SINK)

# ------------ VP9 ------------
# vpx:  vp9enc: On2 VP9 Encoder
# sink format: { I420, YV12, Y444, I420_10LE, I420_12LE, I422_10LE, I422_12LE, Y444_10LE, Y444_12LE }
# src format: video/x-vp9
#                 profile: { 0, 1, 2, 3 }

# VP9_SEND := $(VIDEO_CAP_I420) ! vp9enc deadline=1 cpu-used=8 threads=4 lag-in-frames=0 ! vp9parse ! rtpvp9pay ! $(VIDEO_UDP_SINK)
VP9_SEND := $(VIDEO_CAP_I420) ! vp9enc deadline=1 cpu-used=4 ! vp9parse ! rtpvp9pay ! $(VIDEO_UDP_SINK)

# ------------ AV1 ------------

# nvvideo4linux2:  nvv4l2av1enc: V4L2 AV1 Encoder
# sink format: video/x-raw(memory:NVMM)
#                  format: { I420, NV12, P010_10LE, Y444, Y444_10LE, NV24 }
# src format: video/x-av1
AV1_SEND := nvvideoconvert ! nvv4l2av1enc ! av1parse ! rtpav1pay ! $(VIDEO_UDP_SINK)

# nvvideo4linux2:  nvv4l2decoder: NVIDIA v4l2 video decoder
# sink format: video/x-h264
#           stream-format: { byte-stream }
#               alignment: { au }
# sink format: video/x-h265
#           stream-format: { byte-stream }
#               alignment: { au }
# sink format: video/x-av1
#           stream-format: { obu-stream }
#               alignment: { frame }
# sink format: video/x-vp8
# sink format: video/x-vp9
# src format: video/x-raw(memory:NVMM)
H264_RECV := $(VIDEO_UDP_SRC)H264" ! rtph264depay ! h264parse ! nvv4l2decoder ! videoconvert

H265_RECV := $(VIDEO_UDP_SRC)H265" ! rtph265depay ! h265parse ! nvv4l2decoder ! videoconvert

VP8_RECV := $(VIDEO_UDP_SRC)VP8" ! rtpvp8depay ! nvv4l2decoder ! videoconvert

VP9_RECV := $(VIDEO_UDP_SRC)VP9" ! rtpvp9depay ! vp9parse ! nvv4l2decoder ! videoconvert

AV1_RECV := $(VIDEO_UDP_SRC)AV1" ! rtpav1depay ! av1parse ! nvv4l2decoder ! videoconvert

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
	@echo "  make send_264      # Send H.264 video (via nvv4l2h264enc, NVIDIA HW)"
	@echo "  make recv_264      # Receive H.264 video (via nvv4l2decoder, NVIDIA HW)"
	@echo "  make send_265      # Send H.265 video (via nvv4l2h265enc, NVIDIA HW)"
	@echo "  make recv_265      # Receive H.265 video (via nvv4l2decoder, NVIDIA HW)"
	@echo "  make send_8        # Send VP8 video (via vp8enc, software)"
	@echo "  make recv_8        # Receive VP8 video (via nvv4l2decoder, NVIDIA HW)"
	@echo "  make send_9        # Send VP9 video (via vp9enc, software)"
	@echo "  make recv_9        # Receive VP9 video (via nvv4l2decoder, NVIDIA HW)"
	@echo "  make send_1        # Send AV1 video (via nvv4l2av1enc, NVIDIA HW)"
	@echo "  make recv_1        # Receive AV1 video (via nvv4l2decoder, NVIDIA HW)"
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
