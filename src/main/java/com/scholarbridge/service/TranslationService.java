package com.scholarbridge.service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class TranslationService {
    public TranslationResult translate(String abstractText, String targetLanguage) {
        if (abstractText == null || abstractText.isBlank()) {
            return TranslationResult.failure("No abstract is available to translate.");
        }
        String key = System.getenv("AI_API_KEY");
        String endpoint = System.getenv("AI_ENDPOINT");
        if (key == null || key.isBlank()) {
            return TranslationResult.failure("AI translation is not configured: set AI_API_KEY.");
        }
        if (endpoint == null || endpoint.isBlank()) {
            return TranslationResult.failure("AI translation is not configured: set AI_ENDPOINT.");
        }

        String model = System.getenv().getOrDefault("AI_MODEL", "gpt-4o-mini");
        String prompt = "Translate the following scientific abstract into " + targetLanguage
                + ". Preserve scientific terminology, citations, numbers and units exactly where possible.\n\n"
                + abstractText;
        String body = "{\"model\":\"" + escape(model) + "\",\"messages\":["
                + "{\"role\":\"system\",\"content\":\"You are a precise academic translation assistant. Respond only with the translated text.\"},"
                + "{\"role\":\"user\",\"content\":\"" + escape(prompt) + "\"}],\"temperature\":0}";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return TranslationResult.failure("Translation provider returned HTTP " + response.statusCode()
                        + "; no translated content was generated.");
            }
            String text = extractChatContent(response.body());
            if (text == null || text.isBlank()) {
                return TranslationResult.failure("Translation provider returned no usable content.");
            }
            return TranslationResult.success(text);
        } catch (Exception exception) {
            return TranslationResult.failure("Translation request failed: " + exception.getClass().getSimpleName()
                    + "; no translated content was generated.");
        }
    }

    public TranslationResult simplify(String abstractText) {
        if (abstractText == null || abstractText.isBlank()) {
            return TranslationResult.failure("No abstract is available to explain.");
        }
        String key = System.getenv("AI_API_KEY");
        String endpoint = System.getenv("AI_ENDPOINT");
        if (key == null || key.isBlank()) {
            return TranslationResult.failure("AI explanation is not configured: set AI_API_KEY.");
        }
        if (endpoint == null || endpoint.isBlank()) {
            return TranslationResult.failure("AI explanation is not configured: set AI_ENDPOINT.");
        }
        String model = System.getenv().getOrDefault("AI_MODEL", "gpt-4o-mini");
        String prompt = "Explain the following scientific abstract in clear, accessible English for a non-specialist. "
                + "Preserve citations, numbers and scientific terms.\n\n" + abstractText;
        String body = "{\"model\":\"" + escape(model) + "\",\"messages\":["
                + "{\"role\":\"system\",\"content\":\"You are an academic explanation assistant. Respond only with the explanation.\"},"
                + "{\"role\":\"user\",\"content\":\"" + escape(prompt) + "\"}],\"temperature\":0}";
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(endpoint))
                    .timeout(Duration.ofSeconds(60))
                    .header("Authorization", "Bearer " + key)
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(body))
                    .build();
            HttpResponse<String> response = HttpClient.newHttpClient()
                    .send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return TranslationResult.failure("Explanation provider returned HTTP " + response.statusCode()
                        + "; no explanation was generated.");
            }
            String text = extractChatContent(response.body());
            if (text == null || text.isBlank()) {
                return TranslationResult.failure("Explanation provider returned no usable content.");
            }
            return TranslationResult.success(text);
        } catch (Exception exception) {
            return TranslationResult.failure("Explanation request failed: " + exception.getClass().getSimpleName()
                    + "; no explanation was generated.");
        }
    }

    private String extractChatContent(String json) {
        if (json == null) return null;
        int content = json.indexOf("\"content\":\"");
        if (content < 0) return null;
        int start = content + 11;
        StringBuilder value = new StringBuilder();
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) {
                if (c == 'n') value.append('\n');
                else if (c == 'r') value.append('\r');
                else if (c == 't') value.append('\t');
                else if (c == '"' || c == '\\') value.append(c);
                else value.append(c);
                escaped = false;
            } else if (c == '\\') escaped = true;
            else if (c == '"') break;
            else value.append(c);
        }
        return value.toString();
    }

    private String escape(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
    }

    public record TranslationResult(boolean success, String content) {
        public static TranslationResult success(String content) { return new TranslationResult(true, content); }
        public static TranslationResult failure(String content) { return new TranslationResult(false, content); }
    }
}
