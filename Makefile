.DEFAULT_GOAL := help

CODECS := 264|265|av1|vp8|vp9|opus|mulaw
CODECS_WIN := 264 265 av1 vp8 vp9 opus mulaw
CODEC_EXCLUDE := json|timestamper|alpha
AV := audio|video
AV_WIN := audio video

.PHONY: help gst-cameras gst-microphones \
	gst-decoders gst-decoders-win \
	gst-encoders gst-encoders-win \
	gst-sources gst-sources-win \
	gst-sinks gst-sinks-win

# --- Device discovery ---

gst-cameras:
	gst-device-monitor-1.0 Video/Source

gst-microphones:
	gst-device-monitor-1.0 Audio/Source

# --- Codec inspection (macOS/Linux) ---

gst-decoders:
	gst-inspect-1.0 | grep -Ei 'dec' | grep -Ei '$(CODECS)' | grep -Eiv '$(CODEC_EXCLUDE)'

gst-encoders:
	gst-inspect-1.0 | grep -Ei 'enc' | grep -Ei '$(CODECS)' | grep -Eiv '$(CODEC_EXCLUDE)'

# --- Codec inspection (Windows) ---

gst-decoders-win:
	gst-inspect-1.0 | findstr /i dec | findstr /i "$(CODECS_WIN)"

gst-encoders-win:
	gst-inspect-1.0 | findstr /i enc | findstr /i "$(CODECS_WIN)"

# --- Source/Sink inspection (macOS/Linux) ---

gst-sources:
	gst-inspect-1.0 | grep -Ei 'src' | grep -Ei '$(AV)'

gst-sinks:
	gst-inspect-1.0 | grep -Ei 'sink' | grep -Ei '$(AV)'

# --- Source/Sink inspection (Windows) ---

gst-sources-win:
	gst-inspect-1.0 | findstr /i src | findstr /i "$(AV_WIN)"

gst-sinks-win:
	gst-inspect-1.0 | findstr /i sink | findstr /i "$(AV_WIN)"

# --- Help ---

help:
	@echo "Device discovery:"
	@echo "  gst-cameras         List video capture devices"
	@echo "  gst-microphones     List audio capture devices"
	@echo ""
	@echo "GStreamer inspection (macOS/Linux):"
	@echo "  gst-decoders        Filter gst-inspect for decoders"
	@echo "  gst-encoders        Filter gst-inspect for encoders"
	@echo "  gst-sources         Filter gst-inspect for audio/video sources"
	@echo "  gst-sinks           Filter gst-inspect for audio/video sinks"
	@echo ""
	@echo "GStreamer inspection (Windows):"
	@echo "  gst-decoders-win    Filter gst-inspect for decoders"
	@echo "  gst-encoders-win    Filter gst-inspect for encoders"
	@echo "  gst-sources-win     Filter gst-inspect for audio/video sources"
	@echo "  gst-sinks-win       Filter gst-inspect for audio/video sinks"
