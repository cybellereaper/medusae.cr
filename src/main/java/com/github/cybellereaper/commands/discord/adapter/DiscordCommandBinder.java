package com.github.cybellereaper.commands.discord.adapter;

import com.github.cybellereaper.client.DiscordClient;
import com.github.cybellereaper.commands.core.execute.CommandFramework;
import com.github.cybellereaper.commands.core.model.CommandDefinition;
import com.github.cybellereaper.commands.core.model.CommandType;

import java.util.List;
import java.util.Objects;

public final class DiscordCommandBinder {
    public void bind(DiscordClient client, CommandFramework framework, DiscordCommandDispatcher dispatcher) {
        Objects.requireNonNull(client, "client");
        Objects.requireNonNull(framework, "framework");
        Objects.requireNonNull(dispatcher, "dispatcher");

        for (Binding binding : planBindings(framework)) {
            switch (binding.type()) {
                case CHAT_INPUT -> {
                    client.onSlashCommandContext(binding.commandName(), context -> dispatcher.dispatch(context.raw(), context));
                    if (binding.enableAutocomplete()) {
                        client.onAutocompleteContext(binding.commandName(), context -> dispatcher.dispatchAutocomplete(context.raw(), context));
                    }
                }
                case USER_CONTEXT -> client.onUserContextMenuContext(binding.commandName(), context -> dispatcher.dispatch(context.raw(), context));
                case MESSAGE_CONTEXT -> client.onMessageContextMenuContext(binding.commandName(), context -> dispatcher.dispatch(context.raw(), context));
            }
        }
    }

    public List<Binding> planBindings(CommandFramework framework) {
        Objects.requireNonNull(framework, "framework");

        return framework.registry().all().stream()
                .map(definition -> new Binding(definition.name(), definition.type(), requiresAutocomplete(definition)))
                .toList();
    }

    static boolean requiresAutocomplete(CommandDefinition definition) {
        return !definition.autocompleteHandlers().isEmpty()
                || definition.handlers().stream().anyMatch(handler -> handler.parameters().stream().anyMatch(parameter -> parameter.autocompleteId() != null));
    }

    public record Binding(String commandName, CommandType type, boolean enableAutocomplete) {
    }
}
