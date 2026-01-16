.PHONY: macos linux windows run exec clean help \
	gst-decoders gst-decoders-win gst-sinks gst-sinks-win \
	audio-macos-opus audio-macos-pcmu audio-linux-opus audio-linux-pcmu audio-windows-opus audio-windows-pcmu \
	video-macos-h264 video-macos-h265 video-macos-vp8 video-macos-vp9 video-macos-av1 \
	video-linux-h264 video-linux-h265 video-linux-vp8 video-linux-vp9 video-linux-av1 \
	video-windows-h264 video-windows-h265 video-windows-vp9 video-windows-av1

macos:
	mvn clean -Pmacos package

linux:
	mvn clean -Plinux package

windows:
	mvn clean -Pwindows package

run:
	killall java || true
	java --enable-native-access=ALL-UNNAMED -jar target/app-jar-with-dependencies.jar

exec:
	killall java || true
	GST_DEBUG=2 mvn exec:exec -Dexec.executable=java -Dexec.args="--enable-native-access=ALL-UNNAMED -jar target/app-jar-with-dependencies.jar"

clean:
	mvn clean

# GStreamer inspection tools
gst-decoders:
	gst-inspect-1.0 | grep -Ei 'dec' | grep -Ei '264|265|av1|vp8|vp9|opus|mulaw' | grep -Eiv 'json|timestamper|alpha'

gst-decoders-win:
	gst-inspect-1.0 | findstr /i dec | findstr /i "264 265 av1 vp8 vp9 opus mulaw"

gst-sinks:
	gst-inspect-1.0 | grep -Ei 'sink' | grep -Ei 'audio|video'

gst-sinks-win:
	gst-inspect-1.0 | findstr /i sink | findstr /i "audio video"

# Audio pipelines - macOS
audio-macos-opus:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5001 caps="application/x-rtp, media=audio, encoding-name=OPUS" ! queue max-size-buffers=3 ! rtpopusdepay ! opusdec ! audioconvert ! osxaudiosink sync=false async=false

audio-macos-pcmu:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5001 caps="application/x-rtp, media=audio, encoding-name=PCMU" ! queue max-size-buffers=3 ! rtppcmudepay ! mulawdec ! audioconvert ! osxaudiosink sync=false async=false

# Audio pipelines - Linux
audio-linux-opus:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5001 caps="application/x-rtp, media=audio, encoding-name=OPUS" ! queue max-size-buffers=3 ! rtpopusdepay ! opusdec ! audioconvert ! pulsesink sync=false async=false

audio-linux-pcmu:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5001 caps="application/x-rtp, media=audio, encoding-name=PCMU" ! queue max-size-buffers=3 ! rtppcmudepay ! mulawdec ! audioconvert ! pulsesink sync=false async=false

# Audio pipelines - Windows
audio-windows-opus:
	set GST_DEBUG=2 && gst-launch-1.0 -v -e udpsrc port=5001 caps="application/x-rtp, media=audio, encoding-name=OPUS" ! queue max-size-buffers=3 ! rtpopusdepay ! opusdec ! audioconvert ! directsoundsink sync=false async=false

audio-windows-pcmu:
	set GST_DEBUG=2 && gst-launch-1.0 -v -e udpsrc port=5001 caps="application/x-rtp, media=audio, encoding-name=PCMU" ! queue max-size-buffers=3 ! rtppcmudepay ! mulawdec ! audioconvert ! directsoundsink sync=false async=false

# Video pipelines - macOS
video-macos-h264:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=H264, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtph264depay ! h264parse ! avdec_h264 ! videoconvert ! osxvideosink sync=false async=false

video-macos-h265:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=H265, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtph265depay ! h265parse ! avdec_h265 ! videoconvert ! osxvideosink sync=false async=false

video-macos-vp8:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=VP8, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtpvp8depay ! vp8dec ! videoconvert ! osxvideosink sync=false async=false

video-macos-vp9:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=VP9, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtpvp9depay ! vp9dec ! videoconvert ! osxvideosink sync=false async=false

video-macos-av1:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=AV1, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtpav1depay ! av1parse ! dav1ddec ! videoconvert ! osxvideosink sync=false async=false

# Video pipelines - Linux
video-linux-h264:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=H264, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtph264depay ! h264parse ! avdec_h264 ! videoconvert ! autovideosink sync=false async=false

video-linux-h265:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=H265, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtph265depay ! h265parse ! avdec_h265 ! videoconvert ! autovideosink sync=false async=false

video-linux-vp8:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=VP8, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtpvp8depay ! vp8dec ! videoconvert ! autovideosink sync=false async=false

video-linux-vp9:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=VP9, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtpvp9depay ! vp9dec ! videoconvert ! autovideosink sync=false async=false

video-linux-av1:
	GST_DEBUG=2 gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=AV1, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtpav1depay ! av1parse ! dav1ddec ! videoconvert ! autovideosink sync=false async=false

# Video pipelines - Windows
video-windows-h264:
	set GST_DEBUG=2 && gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=H264, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtph264depay ! h264parse ! d3d12h264dec ! d3d12videosink sync=false async=false

video-windows-h265:
	set GST_DEBUG=2 && gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=H265, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtph265depay ! h265parse ! d3d12h265dec ! d3d12videosink sync=false async=false

video-windows-vp9:
	set GST_DEBUG=2 && gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=VP9, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtpvp9depay ! d3d12vp9dec ! d3d12videosink sync=false async=false

video-windows-av1:
	set GST_DEBUG=2 && gst-launch-1.0 -v -e udpsrc port=5000 caps="application/x-rtp, media=video, encoding-name=AV1, clock-rate=90000" ! rtpjitterbuffer latency=0 ! queue max-size-buffers=1 ! rtpav1depay ! av1parse ! d3d12av1dec ! d3d12videosink sync=false async=false

# Help
help:
	@echo "Build targets:"
	@echo "  macos               Build with macos profile"
	@echo "  linux               Build with linux profile"
	@echo "  windows             Build with windows profile"
	@echo ""
	@echo "Run targets:"
	@echo "  run                 Run jar directly with java -jar"
	@echo "  exec                Run via maven exec (GST_DEBUG=2)"
	@echo "  clean               mvn clean"
	@echo ""
	@echo "GStreamer inspection:"
	@echo "  gst-decoders        Filter gst-inspect for decoders (macOS/Linux)"
	@echo "  gst-decoders-win    Filter gst-inspect for decoders (Windows)"
	@echo "  gst-sinks           Filter gst-inspect for audio/video sinks (macOS/Linux)"
	@echo "  gst-sinks-win       Filter gst-inspect for audio/video sinks (Windows)"
	@echo ""
	@echo "Audio pipelines (macOS):"
	@echo "  audio-macos-opus    RTP OPUS audio (port 5001)"
	@echo "  audio-macos-pcmu    RTP PCMU audio (port 5001)"
	@echo ""
	@echo "Audio pipelines (Linux):"
	@echo "  audio-linux-opus    RTP OPUS audio (port 5001)"
	@echo "  audio-linux-pcmu    RTP PCMU audio (port 5001)"
	@echo ""
	@echo "Audio pipelines (Windows):"
	@echo "  audio-windows-opus  RTP OPUS audio (port 5001)"
	@echo "  audio-windows-pcmu  RTP PCMU audio (port 5001)"
	@echo ""
	@echo "Video pipelines (macOS):"
	@echo "  video-macos-h264    RTP H.264 video (port 5000)"
	@echo "  video-macos-h265    RTP H.265 video (port 5000)"
	@echo "  video-macos-vp8     RTP VP8 video (port 5000)"
	@echo "  video-macos-vp9     RTP VP9 video (port 5000)"
	@echo "  video-macos-av1     RTP AV1 video (port 5000)"
	@echo ""
	@echo "Video pipelines (Linux):"
	@echo "  video-linux-h264    RTP H.264 video (port 5000)"
	@echo "  video-linux-h265    RTP H.265 video (port 5000)"
	@echo "  video-linux-vp8     RTP VP8 video (port 5000)"
	@echo "  video-linux-vp9     RTP VP9 video (port 5000)"
	@echo "  video-linux-av1     RTP AV1 video (port 5000)"
	@echo ""
	@echo "Video pipelines (Windows):"
	@echo "  video-windows-h264  RTP H.264 video (port 5000)"
	@echo "  video-windows-h265  RTP H.265 video (port 5000)"
	@echo "  video-windows-vp9   RTP VP9 video (port 5000)"
	@echo "  video-windows-av1   RTP AV1 video (port 5000)"
