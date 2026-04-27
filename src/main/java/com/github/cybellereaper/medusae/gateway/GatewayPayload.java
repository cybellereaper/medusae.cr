package com.github.cybellereaper.medusae.gateway;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
record GatewayPayload(int op, Object d, Long s, String t) {
}
