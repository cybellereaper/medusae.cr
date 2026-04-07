package com.github.cybellereaper.examples.events;

import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.client.DiscordMessage;
import com.github.cybellereaper.commands.core.annotation.EventModule;
import com.github.cybellereaper.commands.core.annotation.OnGatewayEvent;
import com.github.cybellereaper.gateway.events.MessageCreateEvent;
import com.github.cybellereaper.gateway.events.ReadyEvent;

@EventModule
public final class ModerationEvents {
    @OnGatewayEvent(value = "READY", payload = ReadyEvent.class)
    public void onReady(ReadyEvent event) {
        System.out.println("Gateway session established: " + event.sessionId());
    }

    @OnGatewayEvent(value = "MESSAGE_CREATE", payload = MessageCreateEvent.class)
    public void onMessage(MessageCreateEvent event, DiscordClient client) {
        if ("!ping".equals(event.content())) {
            client.api().sendMessage(event.channelId(), DiscordMessage.ofContent("pong"));
        }
    }
}
