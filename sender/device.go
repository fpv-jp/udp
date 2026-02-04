package main

import (
	"bufio"
	"fmt"
	"os"
	"strconv"
	"strings"

	"github.com/go-gst/go-gst/gst"
)

// DeviceSelection represents either a real device or a test source
type DeviceSelection struct {
	Device  *gst.Device
	IsTest  bool
	Name    string
}

// selectDeviceInteractive shows a list of devices and lets the user choose
func selectDeviceInteractive(className, capsStr, testSourceName string) (*DeviceSelection, error) {
	monitor := gst.NewDeviceMonitor()
	filterCaps := gst.NewCapsFromString(capsStr)
	monitor.AddFilter(className, filterCaps)
	monitor.Start()
	devices := monitor.GetDevices()
	monitor.Stop()

	if len(devices) == 0 {
		return nil, fmt.Errorf("no devices found for %s", className)
	}

	// Display device list
	fmt.Printf("\nAvailable %s devices:\n", className)
	fmt.Printf("  0: Test Source (%s)\n", testSourceName)
	for i, device := range devices {
		fmt.Printf("  %d: %s\n", i+1, device.GetDisplayName())
	}

	// Get user selection
	reader := bufio.NewReader(os.Stdin)
	var selection int
	for {
		fmt.Printf("Select device [0-%d]: ", len(devices))
		input, err := reader.ReadString('\n')
		if err != nil {
			return nil, fmt.Errorf("failed to read input: %w", err)
		}

		input = strings.TrimSpace(input)
		selection, err = strconv.Atoi(input)
		if err != nil || selection < 0 || selection > len(devices) {
			fmt.Printf("Invalid selection. Please enter a number between 0 and %d.\n", len(devices))
			continue
		}
		break
	}

	// Return selection
	if selection == 0 {
		return &DeviceSelection{
			Device: nil,
			IsTest: true,
			Name:   testSourceName,
		}, nil
	}

	return &DeviceSelection{
		Device: devices[selection-1],
		IsTest: false,
		Name:   devices[selection-1].GetDisplayName(),
	}, nil
}
