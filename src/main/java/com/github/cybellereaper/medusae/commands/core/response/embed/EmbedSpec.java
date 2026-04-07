package com.github.cybellereaper.medusae.commands.core.response.embed;

public record EmbedSpec(String title, String description, Integer color, String url, String imageUrl,
                        String thumbnailUrl) {
    public static Builder create() {
        return new Builder();
    }

    public static EmbedSpec success(String title, String description) {
        return create().title(title).description(description).color(0x57F287).build();
    }

    public static EmbedSpec error(String title, String description) {
        return create().title(title).description(description).color(0xED4245).build();
    }

    public static EmbedSpec warning(String title, String description) {
        return create().title(title).description(description).color(0xFEE75C).build();
    }

    public static EmbedSpec info(String title, String description) {
        return create().title(title).description(description).color(0x5865F2).build();
    }

    public static final class Builder {
        private String title;
        private String description;
        private Integer color;
        private String url;
        private String imageUrl;
        private String thumbnailUrl;

        public Builder title(String value) {
            this.title = value;
            return this;
        }

        public Builder description(String value) {
            this.description = value;
            return this;
        }

        public Builder color(Integer value) {
            this.color = value;
            return this;
        }

        public Builder url(String value) {
            this.url = value;
            return this;
        }

        public Builder imageUrl(String value) {
            this.imageUrl = value;
            return this;
        }

        public Builder thumbnailUrl(String value) {
            this.thumbnailUrl = value;
            return this;
        }

        public EmbedSpec build() {
            if ((title == null || title.isBlank()) && (description == null || description.isBlank())) {
                throw new IllegalArgumentException("Embed requires a title or description");
            }
            return new EmbedSpec(title, description, color, url, imageUrl, thumbnailUrl);
        }
    }
}
