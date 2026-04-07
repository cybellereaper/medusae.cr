package com.github.cybellereaper.medusae.gateway;

import java.util.EnumSet;

public enum GatewayIntent {
    GUILDS(1),
    GUILD_MESSAGES(1 << 9),
    DIRECT_MESSAGES(1 << 12),
    MESSAGE_CONTENT(1 << 15);

    private final int bit;

    GatewayIntent(int bit) {
        this.bit = bit;
    }

    public static int combine(GatewayIntent... intents) {
        int value = 0;
        for (GatewayIntent intent : intents) {
            value |= intent.bit;
        }
        return value;
    }

    public static int combine(EnumSet<GatewayIntent> intents) {
        return intents.stream().mapToInt(GatewayIntent::bit).reduce(0, (a, b) -> a | b);
    }

    public int bit() {
        return bit;
    }
}