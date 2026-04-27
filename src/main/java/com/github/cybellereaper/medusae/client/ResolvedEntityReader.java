package com.github.cybellereaper.medusae.client;

import com.github.cybellereaper.medusae.commands.discord.adapter.payload.DiscordInteractionPayload;

import java.util.Map;

final class ResolvedEntityReader {
    private final DiscordInteractionPayload.Resolved resolved;

    ResolvedEntityReader(DiscordInteractionPayload.Resolved resolved) {
        this.resolved = resolved;
    }

    ResolvedAttachment attachmentValue(String attachmentId) {
        return resolvedEntity(resolved == null ? null : resolved.attachments(), attachmentId);
    }

    ResolvedUser userValue(String userId) {
        return resolvedEntity(resolved == null ? null : resolved.users(), userId);
    }

    ResolvedMember memberValue(String userId) {
        return resolvedEntity(resolved == null ? null : resolved.members(), userId);
    }

    ResolvedRole roleValue(String roleId) {
        return resolvedEntity(resolved == null ? null : resolved.roles(), roleId);
    }

    ResolvedChannel channelValue(String channelId) {
        return resolvedEntity(resolved == null ? null : resolved.channels(), channelId);
    }

    private static <T> T resolvedEntity(Map<String, T> entities, String id) {
        if (entities == null || id == null || id.isBlank()) {
            return null;
        }
        return entities.get(id);
    }
}
