package com.github.cybellereaper.commands.core.execute;

import com.github.cybellereaper.commands.core.response.CommandResponse;

public interface CommandResponder {
    void accept(CommandResponse response);
}
