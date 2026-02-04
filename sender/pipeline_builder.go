package main

import (
	"fmt"

	"github.com/go-gst/go-gst/gst"
)

// BuildVideoPipeline builds a video pipeline from elements
func BuildVideoPipeline(config StreamConfig) (*gst.Pipeline, error) {
	pipeline, err := gst.NewPipeline("video-pipeline")
	if err != nil {
		return nil, fmt.Errorf("failed to create pipeline: %w", err)
	}

	// Create source element
	var src *gst.Element
	if config.UseVideoTestSrc {
		src, err = gst.NewElement("videotestsrc")
		if err != nil {
			return nil, fmt.Errorf("failed to create videotestsrc: %w", err)
		}
		src.SetProperty("is-live", true)
		src.SetProperty("pattern", "ball")
	} else {
		// Use device.CreateElement() for real devices
		src = config.VideoDevice.CreateElement("")
		// Set additional properties if not test source
		platform, _ := detectPlatform()
		if platform == "darwin" {
			src.SetProperty("do-stats", true)
			src.SetProperty("do-timestamp", true)
		}
	}

	// Build the rest of the pipeline
	elements := []*gst.Element{src}

	// Add conversion elements based on platform
	platform, _ := detectPlatform()
	if platform == "darwin" {
		scale, _ := gst.NewElement("videoscale")
		rate, _ := gst.NewElement("videorate")
		convert, _ := gst.NewElement("videoconvert")
		elements = append(elements, scale, rate, convert)
	}

	// Add caps filter
	capsFilter, _ := gst.NewElement("capsfilter")
	capsStr := buildVideoCaps(config, platform)
	caps := gst.NewCapsFromString(capsStr)
	capsFilter.SetProperty("caps", caps)
	elements = append(elements, capsFilter)

	// Add queue
	queue, _ := gst.NewElement("queue")
	queue.SetProperty("max-size-buffers", 1)
	queue.SetProperty("leaky", 2) // downstream
	elements = append(elements, queue)

	// Add encoder
	encoder, err := createVideoEncoder(config.Encoder)
	if err != nil {
		return nil, err
	}
	elements = append(elements, encoder)

	// Add parser and payloader
	parser, payloader, err := createVideoParserPayloader(config.Encoder)
	if err != nil {
		return nil, err
	}
	elements = append(elements, parser, payloader)

	// Add UDP sink
	udpSink, _ := gst.NewElement("udpsink")
	udpSink.SetProperty("host", config.Host)
	udpSink.SetProperty("port", config.Port)
	udpSink.SetProperty("sync", false)
	udpSink.SetProperty("async", false)
	elements = append(elements, udpSink)

	// Add all elements to pipeline
	for _, elem := range elements {
		pipeline.Add(elem)
	}

	// Link all elements
	for i := 0; i < len(elements)-1; i++ {
		if err := elements[i].Link(elements[i+1]); err != nil {
			return nil, fmt.Errorf("failed to link %s to %s: %w", elements[i].GetName(), elements[i+1].GetName(), err)
		}
	}

	return pipeline, nil
}

// BuildAudioPipeline builds an audio pipeline from elements
func BuildAudioPipeline(config StreamConfig) (*gst.Pipeline, error) {
	pipeline, err := gst.NewPipeline("audio-pipeline")
	if err != nil {
		return nil, fmt.Errorf("failed to create pipeline: %w", err)
	}

	// Create source element
	var src *gst.Element
	if config.UseAudioTestSrc {
		src, err = gst.NewElement("audiotestsrc")
		if err != nil {
			return nil, fmt.Errorf("failed to create audiotestsrc: %w", err)
		}
		src.SetProperty("is-live", true)
		src.SetProperty("wave", "ticks")
	} else {
		// Use device.CreateElement() for real devices
		src = config.AudioDevice.CreateElement("")
	}
	src.SetProperty("do-timestamp", true)

	// Caps filter for audio format
	capsFilter, _ := gst.NewElement("capsfilter")
	caps := gst.NewCapsFromString("audio/x-raw,rate=48000,channels=2")
	capsFilter.SetProperty("caps", caps)

	// Queues
	queue1, _ := gst.NewElement("queue")
	queue1.SetProperty("max-size-buffers", 10)
	queue1.SetProperty("max-size-time", uint64(0))
	queue1.SetProperty("max-size-bytes", 0)

	// Audio processing
	audioConvert, _ := gst.NewElement("audioconvert")
	audioResample, _ := gst.NewElement("audioresample")

	queue2, _ := gst.NewElement("queue")
	queue2.SetProperty("max-size-buffers", 10)
	queue2.SetProperty("max-size-time", uint64(0))
	queue2.SetProperty("max-size-bytes", 0)

	// Opus encoder
	opusEnc, _ := gst.NewElement("opusenc")
	opusEnc.SetProperty("bitrate", 128000)
	opusEnc.SetProperty("frame-size", 20)

	// RTP payloader
	rtpOpusPay, _ := gst.NewElement("rtpopuspay")

	// UDP sink
	udpSink, _ := gst.NewElement("udpsink")
	udpSink.SetProperty("host", config.Host)
	udpSink.SetProperty("port", config.AudioPort)
	udpSink.SetProperty("sync", false)
	udpSink.SetProperty("async", false)

	// Add all elements to pipeline
	elements := []*gst.Element{
		src, capsFilter, queue1, audioConvert, audioResample,
		queue2, opusEnc, rtpOpusPay, udpSink,
	}
	for _, elem := range elements {
		pipeline.Add(elem)
	}

	// Link all elements
	for i := 0; i < len(elements)-1; i++ {
		if err := elements[i].Link(elements[i+1]); err != nil {
			return nil, fmt.Errorf("failed to link %s to %s: %w", elements[i].GetName(), elements[i+1].GetName(), err)
		}
	}

	return pipeline, nil
}

func buildVideoCaps(config StreamConfig, platform string) string {
	if platform == "darwin" {
		return fmt.Sprintf("video/x-raw,width=%d,height=%d,framerate=30/1,format=NV12,pixel-aspect-ratio=1/1",
			config.Resolution.Width, config.Resolution.Height)
	}
	return fmt.Sprintf("video/x-raw,width=%d,height=%d,framerate=30/1,format=NV12",
		config.Resolution.Width, config.Resolution.Height)
}

func createVideoEncoder(encoderType EncoderType) (*gst.Element, error) {
	encoderName := string(encoderType)
	encoder, err := gst.NewElement(encoderName)
	if err != nil {
		return nil, fmt.Errorf("failed to create encoder %s: %w", encoderName, err)
	}

	// Set encoder-specific properties
	switch encoderType {
	case VTEncH264HW, VTEncH265HW:
		encoder.SetProperty("realtime", true)
		if encoderType == VTEncH265HW {
			encoder.SetProperty("allow-frame-reordering", false)
		}
	}

	return encoder, nil
}

func createVideoParserPayloader(encoderType EncoderType) (*gst.Element, *gst.Element, error) {
	codecFamily := GetCodecFamily(encoderType)

	var parserName, payloaderName string
	switch codecFamily {
	case CodecH264:
		parserName = "h264parse"
		payloaderName = "rtph264pay"
	case CodecH265:
		parserName = "h265parse"
		payloaderName = "rtph265pay"
	case CodecVP8:
		parserName = ""
		payloaderName = "rtpvp8pay"
	case CodecVP9:
		parserName = "vp9parse"
		payloaderName = "rtpvp9pay"
	case CodecAV1:
		parserName = "av1parse"
		payloaderName = "rtpav1pay"
	default:
		return nil, nil, fmt.Errorf("unsupported codec family: %s", codecFamily)
	}

	var parser *gst.Element
	var err error
	if parserName != "" {
		parser, err = gst.NewElement(parserName)
		if err != nil {
			return nil, nil, fmt.Errorf("failed to create parser %s: %w", parserName, err)
		}
	}

	payloader, err := gst.NewElement(payloaderName)
	if err != nil {
		return nil, nil, fmt.Errorf("failed to create payloader %s: %w", payloaderName, err)
	}

	// Set payloader properties
	if codecFamily == CodecH264 || codecFamily == CodecH265 {
		payloader.SetProperty("config-interval", -1)
		payloader.SetProperty("aggregate-mode", 0) // zero-latency
	}

	return parser, payloader, nil
}
