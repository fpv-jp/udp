package main

import (
	"fmt"
	"runtime"
	"strconv"

	"github.com/go-gst/go-gst/gst"
)

// detectPlatform detects the current platform
func detectPlatform() (string, error) {
	switch runtime.GOOS {
	case "darwin":
		return "darwin", nil
	case "linux":
		return "linux", nil
	default:
		return "", fmt.Errorf("unsupported OS: %s", runtime.GOOS)
	}
}

// buildDeviceProperty builds the device property string from a GStreamer device
// NOTE: This function is no longer used with the new element-based pipeline builder
func buildDeviceProperty(device *gst.Device) string {
	if device == nil {
		return ""
	}

	props := device.GetProperties()
	if props == nil {
		return ""
	}

	values := props.Values()

	// macOS AVFoundation video devices (avfvideosrc)
	if s := stringProp(values, "avf.unique_id"); s != "" {
		return fmt.Sprintf("device-index=%s", s)
	}

	// macOS audio devices (osxaudiosrc)
	if s := stringProp(values, "unique-id"); s != "" {
		return fmt.Sprintf("device=%s", s)
	}

	// Linux V4L2 devices
	if s := stringProp(values, "device"); s != "" {
		return fmt.Sprintf("device=%s", strconv.Quote(s))
	}
	if s := stringProp(values, "path"); s != "" {
		return fmt.Sprintf("device=%s", strconv.Quote(s))
	}

	// PipeWire devices
	if s := stringProp(values, "target-object"); s != "" {
		return fmt.Sprintf("target-object=%s", strconv.Quote(s))
	}
	if s := stringProp(values, "node.id"); s != "" {
		return fmt.Sprintf("target-object=%s", strconv.Quote(s))
	}

	// Generic device-index
	if i := intProp(values, "device-index"); i >= 0 {
		return fmt.Sprintf("device-index=%d", i)
	}

	return ""
}

// devicePropPrefix adds a space after the device property if it's not empty
func devicePropPrefix(deviceProp string) string {
	if deviceProp == "" {
		return ""
	}
	return deviceProp + " "
}

// stringProp extracts a string property from a map
func stringProp(values map[string]any, key string) string {
	if v, ok := values[key]; ok {
		if s, ok := v.(string); ok && s != "" {
			return s
		}
	}
	return ""
}

// intProp extracts an int property from a map
func intProp(values map[string]any, key string) int {
	if v, ok := values[key]; ok {
		if i, ok := toInt(v); ok {
			return i
		}
	}
	return -1
}

// toInt converts various numeric types to int
func toInt(val any) (int, bool) {
	switch v := val.(type) {
	case int:
		return v, true
	case int32:
		return int(v), true
	case int64:
		return int(v), true
	case uint:
		return int(v), true
	case uint32:
		return int(v), true
	case uint64:
		return int(v), true
	default:
		return 0, false
	}
}
