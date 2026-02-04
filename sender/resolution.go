package main

import (
	"fmt"
	"strings"
)

// Resolution represents video resolution configuration
type Resolution struct {
	Name   string
	Width  int
	Height int
}

// Resolution presets
var resolutionPresets = map[string]Resolution{
	"QVGA":     {Name: "QVGA", Width: 320, Height: 240},
	"VGA":      {Name: "VGA", Width: 640, Height: 480},
	"SVGA":     {Name: "SVGA", Width: 800, Height: 600},
	"XGA":      {Name: "XGA", Width: 1024, Height: 768},
	"QUADVGA":  {Name: "QuadVGA", Width: 1280, Height: 960},
	"UXGA":     {Name: "UXGA", Width: 1600, Height: 1200},
	"HD":       {Name: "HD", Width: 1280, Height: 720},
	"FHD":      {Name: "FHD", Width: 1920, Height: 1080},
	"2K":       {Name: "2K", Width: 1920, Height: 1080},
	"DCI2K":    {Name: "DCI2K", Width: 2048, Height: 1080},
	"UHD":      {Name: "UHD", Width: 3840, Height: 2160},
	"4K":       {Name: "4K", Width: 3840, Height: 2160},
	"DCI4K":    {Name: "DCI4K", Width: 4096, Height: 2160},
}

// ValidateResolution checks if the resolution is supported and returns the Resolution
func ValidateResolution(res string) (Resolution, error) {
	resUpper := strings.ToUpper(res)
	resolution, ok := resolutionPresets[resUpper]
	if !ok {
		return Resolution{}, fmt.Errorf("unsupported resolution: %s\nSupported: QVGA, VGA, SVGA, XGA, QuadVGA, UXGA, HD, FHD, 2K, DCI2K, UHD, 4K, DCI4K", res)
	}
	return resolution, nil
}

// ListResolutions returns all supported resolutions
func ListResolutions() string {
	return `Supported resolutions:
  - QVGA    (320x240,   4:3)
  - VGA     (640x480,   4:3)
  - SVGA    (800x600,   4:3)
  - XGA     (1024x768,  4:3)
  - QuadVGA (1280x960,  4:3)
  - UXGA    (1600x1200, 4:3)
  - HD      (1280x720,  16:9)
  - FHD     (1920x1080, 16:9)
  - 2K      (1920x1080, 16:9)
  - DCI2K   (2048x1080, ~17:9)
  - UHD     (3840x2160, 16:9)
  - 4K      (3840x2160, 16:9)
  - DCI4K   (4096x2160, ~17:9)`
}
