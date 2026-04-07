package com.github.cybellereaper.medusae.commands.discord.adapter.payload;

public enum DiscordOptionType {
    SUB_COMMAND(1),
    SUB_COMMAND_GROUP(2),
    STRING(3),
    INTEGER(4),
    BOOLEAN(5),
    USER(6),
    CHANNEL(7),
    ROLE(8),
    MENTIONABLE(9),
    NUMBER(10),
    ATTACHMENT(11);

    private final int code;

    DiscordOptionType(int code) {
        this.code = code;
    }

    public static DiscordOptionType fromCode(int code) {
        for (DiscordOptionType type : values()) {
            if (type.code == code) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unsupported Discord option type: " + code);
    }

    public int code() {
        return code;
    }
}
