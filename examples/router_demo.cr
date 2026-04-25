require "../src/medusae"

class DemoBot
  include Medusae::Client::CommandBot

  slash_command "ping" do |interaction|
    puts "slash command received: #{interaction}"
    respond_with_message(interaction, "Pong from macro bot")
  end

  component "confirm" do |interaction|
    puts "button clicked: #{interaction}"
  end

  global_component do |interaction|
    puts "fallback component handler: #{interaction}"
  end
end

bot = DemoBot.new(
  ->(response : Medusae::Client::InteractionResponse) {
    puts "Responding id=#{response.id} token=#{response.token} type=#{response.type.value} data=#{response.data}"
  }
)

bot.handle_interaction(Medusae::Client::Interaction.slash_command("ping", id: "1", token: "abc"))
bot.handle_interaction(Medusae::Client::Interaction.component("confirm", id: "2", token: "def"))
bot.handle_interaction(Medusae::Client::Interaction.component("unknown", id: "3", token: "ghi"))
