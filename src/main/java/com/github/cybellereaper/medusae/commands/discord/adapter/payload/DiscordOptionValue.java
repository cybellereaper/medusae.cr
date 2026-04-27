package com.github.cybellereaper.medusae.commands.discord.adapter.payload;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

import java.io.IOException;

@JsonDeserialize(using = DiscordOptionValue.Deserializer.class)
public sealed interface DiscordOptionValue permits DiscordOptionValue.BooleanValue,
        DiscordOptionValue.NumberValue,
        DiscordOptionValue.StringValue {

    Object value();

    record StringValue(String value) implements DiscordOptionValue {
    }

    record NumberValue(Number value) implements DiscordOptionValue {
    }

    record BooleanValue(Boolean value) implements DiscordOptionValue {
    }

    final class Deserializer extends JsonDeserializer<DiscordOptionValue> {
        @Override
        public DiscordOptionValue deserialize(JsonParser parser, DeserializationContext context) throws IOException {
            JsonToken token = parser.currentToken();
            if (token == JsonToken.VALUE_NULL) return null;
            if (token == JsonToken.VALUE_TRUE || token == JsonToken.VALUE_FALSE) return new BooleanValue(parser.getBooleanValue());
            if (token == JsonToken.VALUE_NUMBER_INT) return new NumberValue(parser.getLongValue());
            if (token == JsonToken.VALUE_NUMBER_FLOAT) return new NumberValue(parser.getDoubleValue());
            return new StringValue(parser.getValueAsString());
        }
    }
}
