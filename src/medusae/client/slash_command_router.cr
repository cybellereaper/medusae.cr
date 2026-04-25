module Medusae
  module Client
    class SlashCommandRouter
      alias InteractionResponder = Proc(InteractionResponse, Nil)
      alias ContextHandler = Proc(Interaction, Nil)

      @slash_handlers = {} of String => ContextHandler
      @user_context_handlers = {} of String => ContextHandler
      @message_context_handlers = {} of String => ContextHandler
      @component_handlers = {} of String => ContextHandler
      @modal_handlers = {} of String => ContextHandler
      @autocomplete_handlers = {} of String => ContextHandler
      @global_component_handlers = [] of ContextHandler
      @global_modal_handlers = [] of ContextHandler

      def initialize(@responder : InteractionResponder)
      end

      def register_slash_handler(command_name : String, &handler : ContextHandler) : Nil
        register_unique_handler(@slash_handlers, command_name, "slash command", handler)
      end

      def register_user_context_menu_handler(command_name : String, &handler : ContextHandler) : Nil
        register_unique_handler(@user_context_handlers, command_name, "user context menu", handler)
      end

      def register_message_context_menu_handler(command_name : String, &handler : ContextHandler) : Nil
        register_unique_handler(@message_context_handlers, command_name, "message context menu", handler)
      end

      def register_component_handler(custom_id : String, &handler : ContextHandler) : Nil
        register_unique_handler(@component_handlers, custom_id, "component", handler)
      end

      def register_global_component_handler(&handler : ContextHandler) : Nil
        @global_component_handlers << handler
      end

      def register_modal_handler(custom_id : String, &handler : ContextHandler) : Nil
        register_unique_handler(@modal_handlers, custom_id, "modal", handler)
      end

      def register_global_modal_handler(&handler : ContextHandler) : Nil
        @global_modal_handlers << handler
      end

      def register_autocomplete_handler(command_name : String, &handler : ContextHandler) : Nil
        register_unique_handler(@autocomplete_handlers, command_name, "autocomplete", handler)
      end

      def handle_interaction(interaction : Interaction?) : Nil
        return if interaction.nil?

        case interaction.type
        when .ping?
          respond(interaction, ResponseType::Pong, nil)
        when .application_command?
          handle_application_command(interaction)
        when .application_command_autocomplete?
          dispatch(interaction, @autocomplete_handlers, global_handlers: [] of ContextHandler) { |data| data.name }
        when .message_component?
          dispatch(interaction, @component_handlers, global_handlers: @global_component_handlers) { |data| data.custom_id }
        when .modal_submit?
          dispatch(interaction, @modal_handlers, global_handlers: @global_modal_handlers) { |data| data.custom_id }
        end
      end

      def respond_with_message(interaction : Interaction, content : String) : Nil
        respond(interaction, ResponseType::ChannelMessage, InteractionResponseData.message(content))
      end

      def defer_message(interaction : Interaction) : Nil
        respond(interaction, ResponseType::DeferredChannelMessage, nil)
      end

      private def register_unique_handler(handlers : Hash(String, ContextHandler), key : String, handler_type : String, handler : ContextHandler) : Nil
        normalized_key = validate_key(key, handler_type)
        if handlers.has_key?(normalized_key)
          raise ArgumentError.new("Interaction handler already registered for #{handler_type}: #{normalized_key}")
        end
        handlers[normalized_key] = handler
      end

      private def validate_key(key : String, key_type : String) : String
        normalized = key.strip
        raise ArgumentError.new("#{key_type} key must not be blank") if normalized.empty?
        normalized
      end

      private def handle_application_command(interaction : Interaction) : Nil
        command_type = interaction.data.try(&.type) || CommandType::ChatInput
        dispatch(interaction, handlers_for(command_type), global_handlers: [] of ContextHandler) { |data| data.name }
      end

      private def dispatch(
        interaction : Interaction,
        handlers : Hash(String, ContextHandler),
        global_handlers : Array(ContextHandler),
        &key_extractor : InteractionData -> (String | Nil)
      ) : Nil
        if handler = handler_for(interaction, handlers, &key_extractor)
          handler.call(interaction)
          return
        end

        global_handlers.each { |handler| handler.call(interaction) }
      end

      private def respond(interaction : Interaction, response_type : ResponseType, data : InteractionResponseData?) : Nil
        interaction_id = non_blank_string(interaction.id)
        interaction_token = non_blank_string(interaction.token)

        if interaction_id.nil? || interaction_token.nil?
          raise ArgumentError.new("interaction must include id and token")
        end

        @responder.call(InteractionResponse.new(
          id: interaction_id,
          token: interaction_token,
          type: response_type,
          data: data,
        ))
      end

      private def handlers_for(command_type : CommandType) : Hash(String, ContextHandler)
        case command_type
        when .user_context?    then @user_context_handlers
        when .message_context? then @message_context_handlers
        else                        @slash_handlers
        end
      end

      private def handler_for(
        interaction : Interaction,
        handlers : Hash(String, ContextHandler),
        &key_extractor : InteractionData -> (String | Nil)
      ) : ContextHandler?
        key = interaction.data.try { |data| key_extractor.call(data) }
        return nil unless key

        handlers[key.strip]?
      end

      private def non_blank_string(value : String?) : String?
        return nil unless value

        return nil if value.strip.empty?
        value
      end
    end
  end
end
