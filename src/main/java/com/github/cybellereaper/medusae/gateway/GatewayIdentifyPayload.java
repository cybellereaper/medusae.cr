package com.github.cybellereaper.medusae.gateway;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

record GatewayIdentifyPayload(
        String token,
        int intents,
        GatewayProperties properties,
        List<Integer> shard
) {
    record GatewayProperties(String os, String browser, String device) {
    }
}
