package com.github.cybellereaper.medusae.commands.core.execute;

import com.github.cybellereaper.medusae.commands.core.response.CommandResponse;

public interface CommandResponder {
    void accept(CommandResponse response);
}
