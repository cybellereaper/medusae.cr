package com.github.cybellereaper.commands.discord.gateway;

import java.util.List;

public record EventModuleDefinition(Object moduleInstance, List<EventHandlerDefinition> handlers) {
}
