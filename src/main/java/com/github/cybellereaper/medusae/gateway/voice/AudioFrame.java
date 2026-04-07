package com.github.cybellereaper.medusae.gateway.voice;

import java.util.Objects;

/**
 * 20ms PCM frame for Discord voice (48kHz, stereo, 16-bit little endian).
 */
public record AudioFrame(byte[] pcm16le) {
    public static final int EXPECTED_BYTES = 3_840;

    public AudioFrame {
        Objects.requireNonNull(pcm16le, "pcm16le");
        if (pcm16le.length != EXPECTED_BYTES) {
            throw new IllegalArgumentException("pcm16le must contain exactly " + EXPECTED_BYTES + " bytes for 20ms audio");
        }
    }
}
