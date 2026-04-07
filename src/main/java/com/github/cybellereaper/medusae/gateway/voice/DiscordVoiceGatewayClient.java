package com.github.cybellereaper.medusae.gateway.voice;

import java.util.Objects;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Minimal voice transport abstraction.
 * This manages audio-frame enqueueing and lifecycle hooks for future UDP/Opus transport.
 */
public final class DiscordVoiceGatewayClient implements AutoCloseable {
    private final BlockingQueue<AudioFrame> outboundFrames = new LinkedBlockingQueue<>();
    private final AtomicBoolean connected = new AtomicBoolean(false);

    private static void requireNonBlank(String value, String fieldName) {
        Objects.requireNonNull(value, fieldName);
        if (value.isBlank()) {
            throw new IllegalArgumentException(fieldName + " must not be blank");
        }
    }

    public void connect(String guildId, String channelId, String sessionId, String token, String endpoint) {
        requireNonBlank(guildId, "guildId");
        requireNonBlank(channelId, "channelId");
        requireNonBlank(sessionId, "sessionId");
        requireNonBlank(token, "token");
        requireNonBlank(endpoint, "endpoint");
        connected.set(true);
    }

    public boolean isConnected() {
        return connected.get();
    }

    public void enqueue(AudioFrame frame) {
        Objects.requireNonNull(frame, "frame");
        if (!connected.get()) {
            throw new IllegalStateException("Voice gateway is not connected");
        }
        outboundFrames.offer(frame);
    }

    public int pendingFrameCount() {
        return outboundFrames.size();
    }

    @Override
    public void close() {
        connected.set(false);
        outboundFrames.clear();
    }
}
