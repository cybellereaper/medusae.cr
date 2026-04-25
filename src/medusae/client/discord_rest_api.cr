require "http/client"
require "uri"

module Medusae
  module Client
    class DiscordRestApi
      alias RequestExecutor = Proc(URI, HTTP::Request, HTTP::Client::Response)

      def initialize(@config : DiscordClientConfig, @request_executor : RequestExecutor? = nil)
      end

      def interaction_responder : SlashCommandRouter::InteractionResponder
        ->(response : InteractionResponse) { send_interaction_response(response) }
      end

      def send_interaction_response(response : InteractionResponse) : Nil
        endpoint = @config.api_uri("/interactions/#{response.id}/#{response.token}/callback")
        post_json(endpoint, response.to_json, authenticated: false)
      end

      def post(path : String, body : String? = nil) : HTTP::Client::Response
        endpoint = @config.api_uri(path)
        post_json(endpoint, body, authenticated: true)
      end

      private def post_json(endpoint : String, body : String?, authenticated : Bool) : HTTP::Client::Response
        uri = URI.parse(endpoint)
        headers = default_headers(authenticated)
        response = execute(uri, HTTP::Request.new("POST", uri.request_target, headers, body))

        return response if response.success?

        raise ArgumentError.new("Discord REST request failed (status #{response.status_code}): #{response.body}")
      end

      private def default_headers(authenticated : Bool) : HTTP::Headers
        headers = HTTP::Headers{
          "Content-Type" => "application/json",
          "User-Agent"   => "Medusae (https://github.com/cybellereaper/medusae.cr)",
        }

        headers["Authorization"] = "Bot #{@config.bot_token}" if authenticated
        headers
      end

      private def execute(uri : URI, request : HTTP::Request) : HTTP::Client::Response
        executor = @request_executor
        return executor.call(uri, request) if executor

        HTTP::Client.new(uri) do |client|
          client.connect_timeout = @config.request_timeout
          client.read_timeout = @config.request_timeout
          return client.exec(request)
        end
      end
    end
  end
end
