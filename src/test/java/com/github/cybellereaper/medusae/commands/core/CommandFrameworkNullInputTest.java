package com.github.cybellereaper.medusae.commands.core;

import com.github.cybellereaper.medusae.commands.core.execute.CommandFramework;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandFrameworkNullInputTest {

    @Test
    void executeRejectsNullArguments() {
        CommandFramework framework = new CommandFramework();
        assertThrows(NullPointerException.class, () -> framework.execute(null, response -> {}));
        assertThrows(NullPointerException.class, () -> framework.executeAutocomplete(null, response -> {}));
    }
}
