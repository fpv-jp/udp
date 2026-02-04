package main

import (
	"fmt"

	"github.com/go-gst/go-gst/gst"
)

// StreamConfig represents the stream configuration
type StreamConfig struct {
	Encoder           EncoderType
	Resolution        Resolution
	Host              string
	Port              int
	VideoDevice       *gst.Device
	AudioDevice       *gst.Device
	AudioPort         int
	UseVideoTestSrc   bool
	UseAudioTestSrc   bool
}

// BuildVideoPipelineString builds the GStreamer video pipeline string based on encoder and resolution
func BuildVideoPipelineString(config StreamConfig) string {
	codecFamily := GetCodecFamily(config.Encoder)
	encoder := string(config.Encoder)
	width := config.Resolution.Width
	height := config.Resolution.Height
	host := config.Host
	port := config.Port

	// Get device property (or empty if using test source)
	var deviceProp string
	var devicePrefix string
	if config.UseVideoTestSrc {
		deviceProp = ""
		devicePrefix = ""
	} else {
		deviceProp = buildDeviceProperty(config.VideoDevice)
		devicePrefix = devicePropPrefix(deviceProp)
	}

	// Fixed framerate and format
	framerate := "30/1"
	format := "NV12"

	switch codecFamily {
	case CodecH264:
		return buildH264Pipeline(encoder, devicePrefix, width, height, framerate, format, host, port)
	case CodecH265:
		return buildH265Pipeline(encoder, devicePrefix, width, height, framerate, format, host, port)
	case CodecVP8:
		return buildVP8Pipeline(encoder, devicePrefix, width, height, framerate, host, port)
	case CodecVP9:
		return buildVP9Pipeline(encoder, devicePrefix, width, height, framerate, host, port)
	case CodecAV1:
		return buildAV1Pipeline(encoder, devicePrefix, width, height, framerate, host, port)
	default:
		return buildH264Pipeline(encoder, devicePrefix, width, height, framerate, format, host, port)
	}
}

func buildH264Pipeline(encoder, devicePrefix string, width, height int, framerate, format, host string, port int) string {
	sourceName := getVideoSourceName()
	platform, _ := detectPlatform()

	// Check if we're using a test source
	isTestSource := devicePrefix == ""
	if isTestSource {
		sourceName = "videotestsrc is-live=true pattern=ball"
	}

	// Build source parameters
	var sourceParams string
	if isTestSource {
		sourceParams = ""
	} else {
		sourceParams = "do-stats=true do-timestamp=true "
	}

	// For macOS, use videoscale + videoconvert to handle resolution and format conversion
	var caps string
	if platform == "darwin" {
		caps = fmt.Sprintf("videoscale ! video/x-raw,width=%d,height=%d ! videorate ! video/x-raw,framerate=%s ! videoconvert ! video/x-raw,format=%s,pixel-aspect-ratio=1/1",
			width, height, framerate, format)
	} else {
		caps = fmt.Sprintf("video/x-raw,width=%d,height=%d,framerate=%s,format=%s",
			width, height, framerate, format)
	}

	switch EncoderType(encoder) {
	case VTEncH264HW:
		return fmt.Sprintf(
			"%s %s%s! %s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"vtenc_h264_hw realtime=true ! "+
				"h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, sourceParams, devicePrefix, caps, host, port,
		)
	case AMFH264Enc:
		if platform == "darwin" {
			return fmt.Sprintf(
				"%s %s%s! %s ! "+
					"queue max-size-buffers=1 leaky=downstream ! "+
					"amfh264enc ! h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! "+
					"udpsink host=%s port=%d sync=false async=false",
				sourceName, sourceParams, devicePrefix, caps, host, port,
			)
		}
		return fmt.Sprintf(
			"%s %s%s! %s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"amfh264enc ! h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, sourceParams, devicePrefix, caps, host, port,
		)
	case NVH264Enc:
		if platform == "darwin" {
			return fmt.Sprintf(
				"%s %s%s! %s ! "+
					"queue max-size-buffers=1 leaky=downstream ! "+
					"nvh264enc ! h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! "+
					"udpsink host=%s port=%d sync=false async=false",
				sourceName, sourceParams, devicePrefix, caps, host, port,
			)
		}
		return fmt.Sprintf(
			"%s %s%s! %s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"nvh264enc ! h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, sourceParams, devicePrefix, caps, host, port,
		)
	case VAH264Enc:
		return fmt.Sprintf(
			"%s %s%s! %s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"vah264enc ! h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, sourceParams, devicePrefix, caps, host, port,
		)
	case VAH264LPEnc:
		return fmt.Sprintf(
			"%s %s%s! %s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"vah264lpenc ! h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, sourceParams, devicePrefix, caps, host, port,
		)
	case OpenH264Enc:
		return fmt.Sprintf(
			"%s %s%s! video/x-raw,width=%d,height=%d,framerate=%s ! "+
				"videoconvert ! video/x-raw,format=I420 ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"openh264enc ! h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, sourceParams, devicePrefix, width, height, framerate, host, port,
		)
	case MPPH264Enc:
		return fmt.Sprintf(
			"%s %s%s! video/x-raw,width=%d,height=%d,framerate=%s,format=YUY2 ! "+
				"videoconvert ! video/x-raw,format=NV12 ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"mpph264enc level=40 profile=100 ! "+
				"rtph264pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, sourceParams, devicePrefix, width, height, framerate, host, port,
		)
	default:
		return fmt.Sprintf(
			"%s %s%s! %s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"x264enc tune=zerolatency speed-preset=ultrafast ! "+
				"h264parse ! rtph264pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, sourceParams, devicePrefix, caps, host, port,
		)
	}
}

func buildH265Pipeline(encoder, devicePrefix string, width, height int, framerate, format, host string, port int) string {
	sourceName := getVideoSourceName()
	platform, _ := detectPlatform()

	// If devicePrefix is empty, it means we're using a test source
	if devicePrefix == "" {
		sourceName = "videotestsrc is-live=true pattern=ball"
	}

	// For macOS, use videoscale + videoconvert to handle resolution and format conversion
	var caps string
	if platform == "darwin" {
		caps = fmt.Sprintf("videoscale ! video/x-raw,width=%d,height=%d ! videorate ! video/x-raw,framerate=%s ! videoconvert ! video/x-raw,format=%s,pixel-aspect-ratio=1/1",
			width, height, framerate, format)
	} else {
		caps = fmt.Sprintf("video/x-raw,width=%d,height=%d,framerate=%s,format=%s",
			width, height, framerate, format)
	}

	switch EncoderType(encoder) {
	case VTEncH265HW:
		return fmt.Sprintf(
			"%s do-stats=true do-timestamp=true %s! %s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"vtenc_h265_hw realtime=true allow-frame-reordering=false ! "+
				"h265parse ! rtph265pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, caps, host, port,
		)
	case AMFH265Enc:
		return fmt.Sprintf(
			"%s do-timestamp=true %s! %s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"amfh265enc ! h265parse ! rtph265pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, caps, host, port,
		)
	case NVH265Enc:
		return fmt.Sprintf(
			"%s do-timestamp=true %s! %s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"nvh265enc ! h265parse ! rtph265pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, caps, host, port,
		)
	case VAH265Enc:
		return fmt.Sprintf(
			"%s do-timestamp=true %s! %s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"vah265enc ! h265parse ! rtph265pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, caps, host, port,
		)
	case VAH265LPEnc:
		return fmt.Sprintf(
			"%s do-timestamp=true %s! %s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"vah265lpenc ! h265parse ! rtph265pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, caps, host, port,
		)
	case X265Enc:
		return fmt.Sprintf(
			"%s do-timestamp=true %s! video/x-raw,width=%d,height=%d,framerate=%s ! "+
				"videoconvert ! video/x-raw,format=I420 ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"x265enc tune=zerolatency speed-preset=ultrafast ! "+
				"h265parse ! rtph265pay config-interval=-1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, width, height, framerate, host, port,
		)
	case MPPH265Enc:
		return fmt.Sprintf(
			"%s %s! video/x-raw,width=%d,height=%d,framerate=%s,format=YUY2 ! "+
				"videoconvert ! video/x-raw,format=NV12 ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"mpph265enc ! "+
				"rtph265pay config-interval=1 aggregate-mode=zero-latency ! "+
				"udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, width, height, framerate, host, port,
		)
	default:
		return buildH265Pipeline(string(VTEncH265HW), devicePrefix, width, height, framerate, format, host, port)
	}
}

func buildVP8Pipeline(encoder, devicePrefix string, width, height int, framerate, host string, port int) string {
	sourceName := getVideoSourceName()

	// If devicePrefix is empty, it means we're using a test source
	if devicePrefix == "" {
		sourceName = "videotestsrc is-live=true pattern=ball"
	}

	switch EncoderType(encoder) {
	case VP8Enc:
		return fmt.Sprintf(
			"%s do-timestamp=true %s! video/x-raw,width=%d,height=%d,framerate=%s ! "+
				"videoconvert ! video/x-raw,format=I420 ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"vp8enc deadline=1 ! "+
				"rtpvp8pay ! udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, width, height, framerate, host, port,
		)
	case MPPVP8Enc:
		return fmt.Sprintf(
			"%s %s! video/x-raw,width=%d,height=%d,framerate=%s,format=YUY2 ! "+
				"videoconvert ! video/x-raw,format=NV12 ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"mppvp8enc ! "+
				"rtpvp8pay ! udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, width, height, framerate, host, port,
		)
	default:
		return buildVP8Pipeline(string(VP8Enc), devicePrefix, width, height, framerate, host, port)
	}
}

func buildVP9Pipeline(encoder, devicePrefix string, width, height int, framerate, host string, port int) string {
	sourceName := getVideoSourceName()

	// If devicePrefix is empty, it means we're using a test source
	if devicePrefix == "" {
		sourceName = "videotestsrc is-live=true pattern=ball"
	}

	return fmt.Sprintf(
		"%s do-timestamp=true %s! video/x-raw,width=%d,height=%d,framerate=%s ! "+
			"videoconvert ! video/x-raw,format=I420 ! "+
			"queue max-size-buffers=1 leaky=downstream ! "+
			"vp9enc deadline=1 cpu-used=8 threads=4 lag-in-frames=0 ! "+
			"vp9parse ! rtpvp9pay ! udpsink host=%s port=%d sync=false async=false",
		sourceName, devicePrefix, width, height, framerate, host, port,
	)
}

func buildAV1Pipeline(encoder, devicePrefix string, width, height int, framerate, host string, port int) string {
	sourceName := getVideoSourceName()

	// If devicePrefix is empty, it means we're using a test source
	if devicePrefix == "" {
		sourceName = "videotestsrc is-live=true pattern=ball"
	}

	switch EncoderType(encoder) {
	case SVTAV1Enc:
		return fmt.Sprintf(
			"%s do-timestamp=true %s! video/x-raw,width=%d,height=%d,framerate=%s ! "+
				"videoconvert ! video/x-raw,format=I420 ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"svtav1enc ! "+
				"av1parse ! rtpav1pay ! udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, width, height, framerate, host, port,
		)
	case AMFAV1Enc:
		return fmt.Sprintf(
			"%s do-timestamp=true %s! video/x-raw,width=%d,height=%d,framerate=%s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"amfav1enc ! "+
				"av1parse ! rtpav1pay ! udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, width, height, framerate, host, port,
		)
	case NVAV1Enc:
		return fmt.Sprintf(
			"%s do-timestamp=true %s! video/x-raw,width=%d,height=%d,framerate=%s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"nvav1enc ! "+
				"av1parse ! rtpav1pay ! udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, width, height, framerate, host, port,
		)
	case VAAV1Enc:
		return fmt.Sprintf(
			"%s do-timestamp=true %s! video/x-raw,width=%d,height=%d,framerate=%s ! "+
				"queue max-size-buffers=1 leaky=downstream ! "+
				"vaav1enc ! "+
				"av1parse ! rtpav1pay ! udpsink host=%s port=%d sync=false async=false",
			sourceName, devicePrefix, width, height, framerate, host, port,
		)
	default:
		return buildAV1Pipeline(string(SVTAV1Enc), devicePrefix, width, height, framerate, host, port)
	}
}

// BuildAudioPipelineString builds the GStreamer audio pipeline string
func BuildAudioPipelineString(config StreamConfig) string {
	audioSourceName := getAudioSourceName()
	deviceProp := buildDeviceProperty(config.AudioDevice)
	devicePrefix := devicePropPrefix(deviceProp)
	host := config.Host
	port := config.AudioPort

	// If we're using test source
	if config.UseAudioTestSrc {
		audioSourceName = "audiotestsrc is-live=true wave=ticks"
		devicePrefix = ""
	}

	// Fixed to Opus, 48000Hz, 2 channels
	return fmt.Sprintf(
		"%s %sdo-timestamp=true ! audio/x-raw,rate=48000,channels=2 ! "+
			"queue max-size-buffers=10 max-size-time=0 max-size-bytes=0 ! "+
			"audioconvert ! audioresample ! "+
			"queue max-size-buffers=10 max-size-time=0 max-size-bytes=0 ! "+
			"opusenc bitrate=128000 frame-size=20 ! "+
			"rtpopuspay ! udpsink host=%s port=%d sync=false async=false",
		audioSourceName, devicePrefix, host, port,
	)
}

func getVideoSourceName() string {
	platform, _ := detectPlatform()
	if platform == "linux" {
		return "v4l2src"
	}
	return "avfvideosrc"
}

func getAudioSourceName() string {
	platform, _ := detectPlatform()
	if platform == "linux" {
		return "pipewiresrc"
	}
	return "osxaudiosrc"
}
