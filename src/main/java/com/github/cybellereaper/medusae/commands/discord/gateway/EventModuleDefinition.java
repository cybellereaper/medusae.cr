package com.github.cybellereaper.medusae.commands.discord.gateway;

import java.util.List;

public record EventModuleDefinition(Object moduleInstance, List<EventHandlerDefinition> handlers) {
}
