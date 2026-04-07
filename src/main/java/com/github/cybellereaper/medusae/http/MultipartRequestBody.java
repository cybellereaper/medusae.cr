package com.github.cybellereaper.medusae.http;

import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

final class MultipartRequestBody {
    private final String boundary = "----------------" + UUID.randomUUID();
    private final List<byte[]> chunks = new ArrayList<>();

    String boundary() {
        return boundary;
    }

    MultipartRequestBody addJsonPart(String name, String json) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + name + "\"\r\n"
                + "Content-Type: application/json\r\n\r\n";
        chunks.add(header.getBytes(StandardCharsets.UTF_8));
        chunks.add(json.getBytes(StandardCharsets.UTF_8));
        chunks.add("\r\n".getBytes(StandardCharsets.UTF_8));
        return this;
    }

    MultipartRequestBody addFilePart(String fieldName, String fileName, String contentType, byte[] content) {
        String header = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"" + fieldName + "\"; filename=\"" + fileName + "\"\r\n"
                + "Content-Type: " + contentType + "\r\n\r\n";
        chunks.add(header.getBytes(StandardCharsets.UTF_8));
        chunks.add(content);
        chunks.add("\r\n".getBytes(StandardCharsets.UTF_8));
        return this;
    }

    HttpRequest.BodyPublisher toPublisher() {
        chunks.add(("--" + boundary + "--\r\n").getBytes(StandardCharsets.UTF_8));
        return HttpRequest.BodyPublishers.ofByteArrays(chunks);
    }
}
