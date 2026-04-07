package com.github.cybellereaper.medusae.commands.core.execute;

import com.github.cybellereaper.medusae.commands.core.exception.CooldownException;
import com.github.cybellereaper.medusae.commands.core.model.CommandInteraction;
import com.github.cybellereaper.medusae.commands.core.model.CooldownSpec;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class CooldownManager {
    private final Map<String, Instant> expiry = new ConcurrentHashMap<>();

    private static String bucketKey(String bucket, CommandInteraction interaction) {
        return switch (bucket) {
            case "guild" -> interaction.guildId() == null ? "dm" : interaction.guildId();
            case "channel" -> interaction.guildId() == null ? "dm" : interaction.guildId();
            default -> interaction.userId();
        };
    }

    void enforce(String routeId, CooldownSpec cooldown, CommandInteraction interaction) {
        if (cooldown == null) {
            return;
        }
        String key = routeId + ":" + bucketKey(cooldown.bucket(), interaction);
        Instant now = Instant.now();
        Instant expiresAt = expiry.get(key);
        if (expiresAt != null && expiresAt.isAfter(now)) {
            throw new CooldownException("Cooldown active for command " + routeId);
        }
        expiry.put(key, now.plusSeconds(cooldown.seconds()));
    }
}
