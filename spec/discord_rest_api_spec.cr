require "spec"
require "http/client"
require "../src/medusae"

describe Medusae::Client::DiscordRestApi do
  it "posts interaction callback payload without bot authorization header" do
    config = Medusae::Client::DiscordClientConfig.new("token", api_base_url: "https://discord.example/api", api_version: 10)

    captured_uri = URI.parse("https://invalid")
    captured_request = HTTP::Request.new("GET", "/")

    api = Medusae::Client::DiscordRestApi.new(config, ->(uri : URI, request : HTTP::Request) {
      captured_uri = uri
      captured_request = request
      HTTP::Client::Response.new(200, "OK", HTTP::Headers.new, "{}")
    })

    api.send_interaction_response(Medusae::Client::InteractionResponse.new(
      id: "123",
      token: "abc",
      type: Medusae::Client::ResponseType::Pong,
      data: nil,
    ))

    captured_uri.to_s.should eq("https://discord.example/api/v10/interactions/123/abc/callback")
    captured_request.method.should eq("POST")
    captured_request.headers.has_key?("Authorization").should be_false
    captured_request.headers["Content-Type"].should eq("application/json")
    request_body(captured_request).should contain("\"type\":\"pong\"")
  end

  it "posts authenticated rest requests" do
    config = Medusae::Client::DiscordClientConfig.new("token", api_base_url: "https://discord.example/api", api_version: 10)

    captured_uri = URI.parse("https://invalid")
    captured_request = HTTP::Request.new("GET", "/")

    api = Medusae::Client::DiscordRestApi.new(config, ->(uri : URI, request : HTTP::Request) {
      captured_uri = uri
      captured_request = request
      HTTP::Client::Response.new(200, "OK", HTTP::Headers.new, "{}")
    })

    api.post("/channels/42/messages", "{\"content\":\"hello\"}")

    captured_uri.to_s.should eq("https://discord.example/api/v10/channels/42/messages")
    captured_request.headers["Authorization"].should eq("Bot token")
    request_body(captured_request).should eq("{\"content\":\"hello\"}")
  end

  it "raises when discord returns a non-success status code" do
    config = Medusae::Client::DiscordClientConfig.new("token")
    api = Medusae::Client::DiscordRestApi.new(config, ->(_uri : URI, _request : HTTP::Request) {
      HTTP::Client::Response.new(400, "Bad Request", HTTP::Headers.new, "{\"message\":\"invalid\"}")
    })

    expect_raises(ArgumentError, /status 400/) do
      api.post("/channels/42/messages", "{}")
    end
  end

  it "provides router-compatible interaction responder" do
    config = Medusae::Client::DiscordClientConfig.new("token")
    called = false

    api = Medusae::Client::DiscordRestApi.new(config, ->(_uri : URI, _request : HTTP::Request) {
      called = true
      HTTP::Client::Response.new(200, "OK", HTTP::Headers.new, "{}")
    })

    responder = api.interaction_responder
    responder.call(Medusae::Client::InteractionResponse.new(id: "1", token: "abc", type: Medusae::Client::ResponseType::Pong))

    called.should be_true
  end
end

private def request_body(request : HTTP::Request) : String
  body = request.body
  return "" if body.nil?

  case body
  when String
    body
  else
    body.gets_to_end
  end
end
