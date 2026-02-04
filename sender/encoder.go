package main

import (
	"fmt"
	"strings"
)

// EncoderType represents the video encoder type
type EncoderType string

// Supported encoders
const (
	// H.264 encoders
	VTEncH264HW    EncoderType = "vtenc_h264_hw"
	AMFH264Enc     EncoderType = "amfh264enc"
	NVH264Enc      EncoderType = "nvh264enc"
	NVV4L2H264Enc  EncoderType = "nvv4l2h264enc"
	VAH264Enc      EncoderType = "vah264enc"
	VAH264LPEnc    EncoderType = "vah264lpenc"
	OpenH264Enc    EncoderType = "openh264enc"
	MPPH264Enc     EncoderType = "mpph264enc"

	// H.265 encoders
	VTEncH265HW   EncoderType = "vtenc_h265_hw"
	AMFH265Enc    EncoderType = "amfh265enc"
	NVH265Enc     EncoderType = "nvh265enc"
	NVV4L2H265Enc EncoderType = "nvv4l2h265enc"
	VAH265Enc     EncoderType = "vah265enc"
	VAH265LPEnc   EncoderType = "vah265lpenc"
	X265Enc       EncoderType = "x265enc"
	MPPH265Enc    EncoderType = "mpph265enc"

	// VP8 encoders
	VP8Enc        EncoderType = "vp8enc"
	NVV4L2VP8Enc  EncoderType = "nvv4l2vp8enc"
	MPPVP8Enc     EncoderType = "mppvp8enc"

	// VP9 encoders
	VP9Enc       EncoderType = "vp9enc"
	NVV4L2VP9Enc EncoderType = "nvv4l2vp9enc"

	// AV1 encoders
	SVTAV1Enc EncoderType = "svtav1enc"
	AMFAV1Enc EncoderType = "amfav1enc"
	NVAV1Enc  EncoderType = "nvav1enc"
	VAAV1Enc  EncoderType = "vaav1enc"
)

// CodecFamily represents the codec type
type CodecFamily string

const (
	CodecH264 CodecFamily = "H264"
	CodecH265 CodecFamily = "H265"
	CodecVP8  CodecFamily = "VP8"
	CodecVP9  CodecFamily = "VP9"
	CodecAV1  CodecFamily = "AV1"
)

var validEncoders = map[EncoderType]CodecFamily{
	// H.264
	VTEncH264HW:   CodecH264,
	AMFH264Enc:    CodecH264,
	NVH264Enc:     CodecH264,
	NVV4L2H264Enc: CodecH264,
	VAH264Enc:     CodecH264,
	VAH264LPEnc:   CodecH264,
	OpenH264Enc:   CodecH264,
	MPPH264Enc:    CodecH264,

	// H.265
	VTEncH265HW:   CodecH265,
	AMFH265Enc:    CodecH265,
	NVH265Enc:     CodecH265,
	NVV4L2H265Enc: CodecH265,
	VAH265Enc:     CodecH265,
	VAH265LPEnc:   CodecH265,
	X265Enc:       CodecH265,
	MPPH265Enc:    CodecH265,

	// VP8
	VP8Enc:       CodecVP8,
	NVV4L2VP8Enc: CodecVP8,
	MPPVP8Enc:    CodecVP8,

	// VP9
	VP9Enc:       CodecVP9,
	NVV4L2VP9Enc: CodecVP9,

	// AV1
	SVTAV1Enc: CodecAV1,
	AMFAV1Enc: CodecAV1,
	NVAV1Enc:  CodecAV1,
	VAAV1Enc:  CodecAV1,
}

// ValidateEncoder checks if the encoder is supported
func ValidateEncoder(encoder string) (EncoderType, CodecFamily, error) {
	encoderType := EncoderType(strings.ToLower(encoder))
	codecFamily, ok := validEncoders[encoderType]
	if !ok {
		return "", "", fmt.Errorf("unsupported encoder: %s", encoder)
	}
	return encoderType, codecFamily, nil
}

// GetCodecFamily returns the codec family for an encoder
func GetCodecFamily(encoder EncoderType) CodecFamily {
	return validEncoders[encoder]
}

// ListEncoders returns all supported encoders grouped by codec
func ListEncoders() string {
	var sb strings.Builder
	sb.WriteString("Supported encoders:\n\n")
	sb.WriteString("H.264:\n")
	sb.WriteString("  - vtenc_h264_hw (Apple VideoToolbox)\n")
	sb.WriteString("  - amfh264enc (AMD AMF)\n")
	sb.WriteString("  - nvh264enc (NVIDIA NVENC)\n")
	sb.WriteString("  - nvv4l2h264enc (NVIDIA V4L2)\n")
	sb.WriteString("  - vah264enc (VA-API)\n")
	sb.WriteString("  - vah264lpenc (VA-API low power)\n")
	sb.WriteString("  - openh264enc (OpenH264)\n")
	sb.WriteString("  - mpph264enc (Rockchip MPP)\n\n")

	sb.WriteString("H.265:\n")
	sb.WriteString("  - vtenc_h265_hw (Apple VideoToolbox)\n")
	sb.WriteString("  - amfh265enc (AMD AMF)\n")
	sb.WriteString("  - nvh265enc (NVIDIA NVENC)\n")
	sb.WriteString("  - nvv4l2h265enc (NVIDIA V4L2)\n")
	sb.WriteString("  - vah265enc (VA-API)\n")
	sb.WriteString("  - vah265lpenc (VA-API low power)\n")
	sb.WriteString("  - x265enc (x265)\n")
	sb.WriteString("  - mpph265enc (Rockchip MPP)\n\n")

	sb.WriteString("VP8:\n")
	sb.WriteString("  - vp8enc (libvpx)\n")
	sb.WriteString("  - nvv4l2vp8enc (NVIDIA V4L2)\n")
	sb.WriteString("  - mppvp8enc (Rockchip MPP)\n\n")

	sb.WriteString("VP9:\n")
	sb.WriteString("  - vp9enc (libvpx)\n")
	sb.WriteString("  - nvv4l2vp9enc (NVIDIA V4L2)\n\n")

	sb.WriteString("AV1:\n")
	sb.WriteString("  - svtav1enc (SVT-AV1)\n")
	sb.WriteString("  - amfav1enc (AMD AMF)\n")
	sb.WriteString("  - nvav1enc (NVIDIA NVENC)\n")
	sb.WriteString("  - vaav1enc (VA-API)\n")

	return sb.String()
}
