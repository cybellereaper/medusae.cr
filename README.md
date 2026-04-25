# Medusae (Crystal Edition)

Medusae is now a **Crystal-first** Discord interaction toolkit focused on building payloads and routing interaction events with a strongly typed API.

## Features

- Gateway intent bitmask helpers (`Medusae::Gateway::GatewayIntent`)
- Discord client configuration model (`Medusae::Client::DiscordClientConfig`)
- Interaction router (`Medusae::Client::SlashCommandRouter`)
- Rich payload builders:
  - messages and embeds
  - buttons and action rows
  - string/entity/channel select menus
  - modals and text inputs

## Install

Add to your shard:

```yaml
dependencies:
  medusae:
    github: cybellereaper/medusae.cr
```

## Development

```bash
shards install
crystal spec
```

## Example: build a component-rich message

```crystal
require "medusae"

button_row = Medusae::Client::DiscordActionRow.of([
  Medusae::Client::DiscordButton.primary("confirm", "Confirm"),
  Medusae::Client::DiscordButton.link("https://discord.com/developers/docs", "Docs"),
])

select_row = Medusae::Client::DiscordActionRow.of([
  Medusae::Client::DiscordStringSelectMenu.of("theme", [
    Medusae::Client::DiscordSelectOption.of("Dark", "dark").as_default,
    Medusae::Client::DiscordSelectOption.of("Light", "light"),
  ]).with_placeholder("Choose a theme").with_selection_range(1, 1),
])

payload = Medusae::Client::DiscordMessage.of_content("Choose your settings")
  .with_components([button_row, select_row])
  .as_ephemeral
  .to_payload

puts payload.to_json
```

## Example: macro-based command bot

```crystal
require "medusae"

class DemoBot
  include Medusae::Client::CommandBot

  slash_command "ping" do |interaction|
    respond_with_message(interaction, "Pong from macro bot")
  end

  component "confirm" do |interaction|
    puts "button clicked: #{interaction}"
  end

  global_component do |interaction|
    puts "fallback component: #{interaction}"
  end
end

bot = DemoBot.new(
  ->(response : Medusae::Client::InteractionResponse) {
    puts "Responding id=#{response.id} token=#{response.token} type=#{response.type.value} data=#{response.data}"
  }
)

bot.handle_interaction(Medusae::Client::Interaction.slash_command("ping", id: "1", token: "abc"))
bot.handle_interaction(Medusae::Client::Interaction.component("confirm", id: "2", token: "def"))
```

See runnable files in [`examples/`](examples).
