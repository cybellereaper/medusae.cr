module Medusae
  module Client
    module CommandBot
      macro included
        @router : Medusae::Client::SlashCommandRouter

        def initialize(responder : Medusae::Client::SlashCommandRouter::InteractionResponder)
          @router = Medusae::Client::SlashCommandRouter.new(responder)
          register_macro_handlers
          after_initialize
        end

        protected def after_initialize : Nil
        end

        def handle_interaction(interaction : Medusae::Client::Interaction?) : Nil
          @router.handle_interaction(interaction)
        end

        protected def respond_with_message(interaction : Medusae::Client::Interaction, content : String) : Nil
          @router.respond_with_message(interaction, content)
        end

        protected def defer_message(interaction : Medusae::Client::Interaction) : Nil
          @router.defer_message(interaction)
        end

        private def register_macro_handlers : Nil
          __register_generated_handlers
        end

        private def __register_generated_handlers : Nil
        end
      end

      macro slash_command(name, &block)
        {% method_name = "__medusae_slash_#{block.body.line_number}".id %}

        private def {{method_name}}(interaction : Medusae::Client::Interaction) : Nil
          {% if block.args.size > 0 %}
            ->({{block.args[0]}} : Medusae::Client::Interaction) { {{block.body}} }.call(interaction)
          {% else %}
            {{block.body}}
          {% end %}
        end

        private def __register_generated_handlers : Nil
          previous_def
          @router.register_slash_handler({{name}}) do |interaction|
            {{method_name}}(interaction)
          end
        end
      end

      macro user_context_command(name, &block)
        {% method_name = "__medusae_user_context_#{block.body.line_number}".id %}

        private def {{method_name}}(interaction : Medusae::Client::Interaction) : Nil
          {% if block.args.size > 0 %}
            ->({{block.args[0]}} : Medusae::Client::Interaction) { {{block.body}} }.call(interaction)
          {% else %}
            {{block.body}}
          {% end %}
        end

        private def __register_generated_handlers : Nil
          previous_def
          @router.register_user_context_menu_handler({{name}}) do |interaction|
            {{method_name}}(interaction)
          end
        end
      end

      macro message_context_command(name, &block)
        {% method_name = "__medusae_message_context_#{block.body.line_number}".id %}

        private def {{method_name}}(interaction : Medusae::Client::Interaction) : Nil
          {% if block.args.size > 0 %}
            ->({{block.args[0]}} : Medusae::Client::Interaction) { {{block.body}} }.call(interaction)
          {% else %}
            {{block.body}}
          {% end %}
        end

        private def __register_generated_handlers : Nil
          previous_def
          @router.register_message_context_menu_handler({{name}}) do |interaction|
            {{method_name}}(interaction)
          end
        end
      end

      macro component(custom_id, &block)
        {% method_name = "__medusae_component_#{block.body.line_number}".id %}

        private def {{method_name}}(interaction : Medusae::Client::Interaction) : Nil
          {% if block.args.size > 0 %}
            ->({{block.args[0]}} : Medusae::Client::Interaction) { {{block.body}} }.call(interaction)
          {% else %}
            {{block.body}}
          {% end %}
        end

        private def __register_generated_handlers : Nil
          previous_def
          @router.register_component_handler({{custom_id}}) do |interaction|
            {{method_name}}(interaction)
          end
        end
      end

      macro modal(custom_id, &block)
        {% method_name = "__medusae_modal_#{block.body.line_number}".id %}

        private def {{method_name}}(interaction : Medusae::Client::Interaction) : Nil
          {% if block.args.size > 0 %}
            ->({{block.args[0]}} : Medusae::Client::Interaction) { {{block.body}} }.call(interaction)
          {% else %}
            {{block.body}}
          {% end %}
        end

        private def __register_generated_handlers : Nil
          previous_def
          @router.register_modal_handler({{custom_id}}) do |interaction|
            {{method_name}}(interaction)
          end
        end
      end

      macro autocomplete(name, &block)
        {% method_name = "__medusae_autocomplete_#{block.body.line_number}".id %}

        private def {{method_name}}(interaction : Medusae::Client::Interaction) : Nil
          {% if block.args.size > 0 %}
            ->({{block.args[0]}} : Medusae::Client::Interaction) { {{block.body}} }.call(interaction)
          {% else %}
            {{block.body}}
          {% end %}
        end

        private def __register_generated_handlers : Nil
          previous_def
          @router.register_autocomplete_handler({{name}}) do |interaction|
            {{method_name}}(interaction)
          end
        end
      end

      macro global_component(&block)
        {% method_name = "__medusae_global_component_#{block.body.line_number}".id %}

        private def {{method_name}}(interaction : Medusae::Client::Interaction) : Nil
          {% if block.args.size > 0 %}
            ->({{block.args[0]}} : Medusae::Client::Interaction) { {{block.body}} }.call(interaction)
          {% else %}
            {{block.body}}
          {% end %}
        end

        private def __register_generated_handlers : Nil
          previous_def
          @router.register_global_component_handler do |interaction|
            {{method_name}}(interaction)
          end
        end
      end

      macro global_modal(&block)
        {% method_name = "__medusae_global_modal_#{block.body.line_number}".id %}

        private def {{method_name}}(interaction : Medusae::Client::Interaction) : Nil
          {% if block.args.size > 0 %}
            ->({{block.args[0]}} : Medusae::Client::Interaction) { {{block.body}} }.call(interaction)
          {% else %}
            {{block.body}}
          {% end %}
        end

        private def __register_generated_handlers : Nil
          previous_def
          @router.register_global_modal_handler do |interaction|
            {{method_name}}(interaction)
          end
        end
      end
    end
  end
end
