package com.github.cybellereaper.medusae.client;

import java.util.Objects;

/**
 * Utility for Discord permission bitsets.
 */
public final class DiscordPermissions {
    public static final long CREATE_INSTANT_INVITE = 1L << 0;
    public static final long KICK_MEMBERS = 1L << 1;
    public static final long BAN_MEMBERS = 1L << 2;
    public static final long ADMINISTRATOR = 1L << 3;
    public static final long MANAGE_CHANNELS = 1L << 4;
    public static final long MANAGE_GUILD = 1L << 5;
    public static final long ADD_REACTIONS = 1L << 6;
    public static final long VIEW_AUDIT_LOG = 1L << 7;
    public static final long VIEW_CHANNEL = 1L << 10;
    public static final long SEND_MESSAGES = 1L << 11;
    public static final long MANAGE_MESSAGES = 1L << 13;
    public static final long EMBED_LINKS = 1L << 14;
    public static final long ATTACH_FILES = 1L << 15;
    public static final long READ_MESSAGE_HISTORY = 1L << 16;
    public static final long USE_EXTERNAL_EMOJIS = 1L << 18;
    public static final long CONNECT = 1L << 20;
    public static final long SPEAK = 1L << 21;
    public static final long MUTE_MEMBERS = 1L << 22;
    public static final long DEAFEN_MEMBERS = 1L << 23;
    public static final long MOVE_MEMBERS = 1L << 24;
    public static final long MANAGE_ROLES = 1L << 28;
    public static final long MANAGE_WEBHOOKS = 1L << 29;
    public static final long USE_APPLICATION_COMMANDS = 1L << 31;

    private DiscordPermissions() {
    }

    public static long of(long... permissions) {
        Objects.requireNonNull(permissions, "permissions");

        long value = 0L;
        for (long permission : permissions) {
            if (permission < 0) {
                throw new IllegalArgumentException("permission values must be non-negative");
            }
            value |= permission;
        }

        return value;
    }

    public static String asString(long permissions) {
        if (permissions < 0) {
            throw new IllegalArgumentException("permissions must be non-negative");
        }
        return Long.toUnsignedString(permissions);
    }
}
