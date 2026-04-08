package com.github.cybellereaper.medusae.client;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.Objects;

final class ResolvedEntityReader {
    private static final String RESOLVED_FIELD = "resolved";
    private static final String ATTACHMENTS_FIELD = "attachments";
    private static final String USERS_FIELD = "users";
    private static final String MEMBERS_FIELD = "members";
    private static final String ROLES_FIELD = "roles";
    private static final String CHANNELS_FIELD = "channels";

    private final JsonNode resolved;

    ResolvedEntityReader(JsonNode data) {
        this.resolved = Objects.requireNonNull(data, "data").path(RESOLVED_FIELD);
    }

    JsonNode attachment(String attachmentId) {
        return resolvedEntity(ATTACHMENTS_FIELD, attachmentId);
    }

    JsonNode user(String userId) {
        return resolvedEntity(USERS_FIELD, userId);
    }

    JsonNode member(String userId) {
        return resolvedEntity(MEMBERS_FIELD, userId);
    }

    JsonNode role(String roleId) {
        return resolvedEntity(ROLES_FIELD, roleId);
    }

    JsonNode channel(String channelId) {
        return resolvedEntity(CHANNELS_FIELD, channelId);
    }

    ResolvedAttachment attachmentValue(String attachmentId) {
        return ResolvedAttachment.from(attachment(attachmentId));
    }

    ResolvedUser userValue(String userId) {
        return ResolvedUser.from(user(userId));
    }

    ResolvedMember memberValue(String userId) {
        return ResolvedMember.from(userId, member(userId));
    }

    ResolvedRole roleValue(String roleId) {
        return ResolvedRole.from(role(roleId));
    }

    ResolvedChannel channelValue(String channelId) {
        return ResolvedChannel.from(channel(channelId));
    }

    private JsonNode resolvedEntity(String entityType, String id) {
        if (id == null || id.isBlank()) {
            return null;
        }
        JsonNode entity = resolved.path(entityType).path(id);
        return entity.isMissingNode() || entity.isNull() ? null : entity;
    }
}
