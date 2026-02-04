package main

import (
	"fmt"
	"os"

	"github.com/go-gst/go-glib/glib"
	"github.com/go-gst/go-gst/examples"
	"github.com/go-gst/go-gst/gst"
)

func main() {
	if err := Execute(); err != nil {
		fmt.Fprintf(os.Stderr, "Error: %v\n", err)
		os.Exit(1)
	}
}

// RunPipeline initializes GStreamer and runs the streaming pipeline
func RunPipeline(config StreamConfig) error {
	gst.Init(nil)

	// Detect platform
	platform, err := detectPlatform()
	if err != nil {
		return fmt.Errorf("failed to detect platform: %w", err)
	}
	fmt.Printf("Platform: %s\n\n", platform)

	// Select video device interactively
	videoSelection, err := selectDeviceInteractive("Video/Source", "video/x-raw", "videotestsrc")
	if err != nil {
		return fmt.Errorf("failed to select video device: %w", err)
	}
	config.VideoDevice = videoSelection.Device
	config.UseVideoTestSrc = videoSelection.IsTest
	fmt.Printf("Selected video: %s\n", videoSelection.Name)

	// Select audio device interactively
	audioSelection, err := selectDeviceInteractive("Audio/Source", "audio/x-raw", "audiotestsrc")
	if err != nil {
		return fmt.Errorf("failed to select audio device: %w", err)
	}
	config.AudioDevice = audioSelection.Device
	config.UseAudioTestSrc = audioSelection.IsTest
	fmt.Printf("Selected audio: %s\n", audioSelection.Name)
	fmt.Println()

	// Print pipeline commands for debugging / manual testing
	fmt.Println("Pipeline commands:")
	fmt.Println()
	fmt.Println("[Video]")
	fmt.Println(BuildVideoPipelineCommand(config))
	fmt.Println()
	fmt.Println("[Audio]")
	fmt.Println(BuildAudioPipelineCommand(config))
	fmt.Println()

	// Build pipelines
	fmt.Println("Building video pipeline...")
	videoPipeline, err := BuildVideoPipeline(config)
	if err != nil {
		return fmt.Errorf("failed to create video pipeline: %w", err)
	}

	fmt.Println("Building audio pipeline...")
	audioPipeline, err := BuildAudioPipeline(config)
	if err != nil {
		return fmt.Errorf("failed to create audio pipeline: %w", err)
	}
	fmt.Println()

	// Run the main loop
	var runErr error
	examples.RunLoop(func(mainLoop *glib.MainLoop) error {
		allPipelines := []*gst.Pipeline{videoPipeline, audioPipeline}
		addPipelineWatch(videoPipeline, "video", mainLoop, allPipelines)
		addPipelineWatch(audioPipeline, "audio", mainLoop, allPipelines)

		// Start the pipelines
		fmt.Println("Starting pipelines...")
		videoPipeline.SetState(gst.StatePlaying)
		audioPipeline.SetState(gst.StatePlaying)
		fmt.Println("Streaming... Press Ctrl+C to stop.")

		// Block on the main loop
		runErr = mainLoop.RunError()
		return runErr
	})
	return runErr
}

func selectFirstDevice(className, capsStr string) (*gst.Device, error) {
	monitor := gst.NewDeviceMonitor()
	filterCaps := gst.NewCapsFromString(capsStr)
	monitor.AddFilter(className, filterCaps)
	monitor.Start()
	devices := monitor.GetDevices()
	monitor.Stop()

	if len(devices) == 0 {
		return nil, fmt.Errorf("no devices found for %s", className)
	}

	// Return the first device
	return devices[0], nil
}

func addPipelineWatch(pipeline *gst.Pipeline, label string, mainLoop *glib.MainLoop, all []*gst.Pipeline) {
	pipeline.GetPipelineBus().AddWatch(func(msg *gst.Message) bool {
		switch msg.Type() {
		case gst.MessageEOS:
			fmt.Printf("[%s] End of stream\n", label)
			for _, p := range all {
				if p != nil {
					p.BlockSetState(gst.StateNull)
				}
			}
			mainLoop.Quit()
		case gst.MessageError:
			err := msg.ParseError()
			fmt.Printf("ERROR (%s): %s\n", label, err.Error())
			if debug := err.DebugString(); debug != "" {
				fmt.Println("DEBUG:", debug)
			}
			for _, p := range all {
				if p != nil {
					p.BlockSetState(gst.StateNull)
				}
			}
			mainLoop.Quit()
		case gst.MessageStateChanged:
			// Optionally log state changes
			// fmt.Printf("[%s] State changed\n", label)
		default:
			// Ignore other messages
		}
		return true
	})
}
