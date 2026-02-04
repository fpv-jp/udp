package main

import (
	"fmt"
	"strings"

	"github.com/spf13/cobra"
)

var rootCmd = &cobra.Command{
	Use:   "cli [encoder] [resolution] [host:port]",
	Short: "FPV streaming tool using GStreamer",
	Long: `A GStreamer-based FPV video/audio streaming tool.

Usage:
  cli [encoder] [resolution] [host:port]
  cli --list

Example:
  cli vtenc_h264_hw VGA 192.168.1.10:5000

This will stream video using vtenc_h264_hw at 640x480 (VGA) resolution to 192.168.1.10:5000.
Audio will be streamed to 192.168.1.10:5001 (video port + 1).`,
	RunE: runStream,
}

var listFlag bool

func init() {
	rootCmd.Flags().BoolVarP(&listFlag, "list", "l", false, "List supported encoders and resolutions")
}

// Execute runs the root command
func Execute() error {
	return rootCmd.Execute()
}

func runStream(cmd *cobra.Command, args []string) error {
	if listFlag {
		fmt.Println(ListEncoders())
		fmt.Println()
		fmt.Println(ListResolutions())
		return nil
	}

	// Validate arguments
	if len(args) != 3 {
		return fmt.Errorf("requires exactly 3 arguments: [encoder] [resolution] [host:port]\nUse --help for more information or --list to see supported encoders and resolutions")
	}

	// Parse arguments
	encoderStr := args[0]
	resolutionStr := args[1]
	addressStr := args[2]

	// Validate encoder
	encoder, codecFamily, err := ValidateEncoder(encoderStr)
	if err != nil {
		return fmt.Errorf("invalid encoder: %w\n\n%s", err, ListEncoders())
	}

	// Validate resolution
	resolution, err := ValidateResolution(resolutionStr)
	if err != nil {
		return fmt.Errorf("invalid resolution: %w\n\n%s", err, ListResolutions())
	}

	// Validate encoder-resolution compatibility
	if err := validateEncoderResolution(encoder, resolution); err != nil {
		return err
	}

	// Parse host:port
	host, port, err := parseAddress(addressStr)
	if err != nil {
		return fmt.Errorf("invalid address format: %w\nExpected format: host:port (e.g., 192.168.1.10:5000)", err)
	}

	fmt.Printf("Starting stream with:\n")
	fmt.Printf("  Encoder:    %s (%s)\n", encoder, codecFamily)
	fmt.Printf("  Resolution: %s (%dx%d)\n", resolution.Name, resolution.Width, resolution.Height)
	fmt.Printf("  Video:      %s:%d\n", host, port)
	fmt.Printf("  Audio:      %s:%d (Opus, 48000Hz, 2ch)\n", host, port+1)
	fmt.Println()

	// Run the streaming pipeline
	return RunPipeline(StreamConfig{
		Encoder:    encoder,
		Resolution: resolution,
		Host:       host,
		Port:       port,
		AudioPort:  port + 1,
	})
}

func parseAddress(addr string) (string, int, error) {
	parts := strings.Split(addr, ":")
	if len(parts) != 2 {
		return "", 0, fmt.Errorf("expected format host:port, got: %s", addr)
	}

	host := parts[0]
	if host == "" {
		return "", 0, fmt.Errorf("host cannot be empty")
	}

	var port int
	_, err := fmt.Sscanf(parts[1], "%d", &port)
	if err != nil {
		return "", 0, fmt.Errorf("invalid port number: %s", parts[1])
	}

	if port < 1 || port > 65535 {
		return "", 0, fmt.Errorf("port must be between 1 and 65535, got: %d", port)
	}

	return host, port, nil
}

// validateEncoderResolution checks if the encoder supports the given resolution
func validateEncoderResolution(encoder EncoderType, resolution Resolution) error {
	// Apple VideoToolbox hardware encoders have a minimum resolution of 640x480
	if encoder == VTEncH264HW || encoder == VTEncH265HW {
		if resolution.Width < 640 || resolution.Height < 480 {
			return fmt.Errorf(
				"Apple VideoToolbox encoder '%s' requires minimum resolution of 640x480 (VGA)\n"+
					"Your resolution: %s (%dx%d)\n\n"+
					"Solutions:\n"+
					"  1. Use VGA or higher resolution: VGA, SVGA, XGA, HD, FHD, etc.\n"+
					"  2. Use a software encoder that supports lower resolutions:\n"+
					"     - x264enc (H.264)\n"+
					"     - x265enc (H.265)\n"+
					"     - openh264enc (H.264)",
				encoder, resolution.Name, resolution.Width, resolution.Height,
			)
		}
	}
	return nil
}
