package com.github.cybellereaper.medusae.commands.core.model;

import java.util.Map;

public record InteractionRouteMatch(String route, Map<String, String> pathParams, String statePayload) {
}
