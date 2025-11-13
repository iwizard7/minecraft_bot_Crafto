package com.crafto.ai.ai;

import com.crafto.ai.CraftoMod;
import com.crafto.ai.config.CraftoConfig;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Base64;

public class OllamaClient {
    private final HttpClient httpClient;
    private final String baseUrl;
    private final String model;

    public OllamaClient() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(30))  // Reduced for faster failure detection
            .version(HttpClient.Version.HTTP_2)      // HTTP/2 for better performance
            .build();
        this.baseUrl = CraftoConfig.OLLAMA_BASE_URL.get();
        this.model = CraftoConfig.OLLAMA_MODEL.get();
    }

    public String sendRequest(String systemPrompt, String userPrompt) {
        try {
            String fullPrompt = systemPrompt + "\n\n" + userPrompt;

            // Properly escape the prompt for JSON
            String escapedPrompt = fullPrompt
                .replace("\\", "\\\\")  // Escape backslashes first
                .replace("\"", "\\\"")  // Escape quotes
                .replace("\n", "\\n")   // Escape newlines
                .replace("\r", "\\r")   // Escape carriage returns
                .replace("\t", "\\t")   // Escape tabs
                .replace("\b", "\\b")   // Escape backspace
                .replace("\f", "\\f");  // Escape form feed

            // Explicitly format numbers to avoid locale issues (comma vs dot)
            String temperatureStr = String.format("%.1f", CraftoConfig.OLLAMA_TEMPERATURE.get()).replace(',', '.');
            String maxTokensStr = String.valueOf(CraftoConfig.OLLAMA_MAX_TOKENS.get());

            // Optimized for M2 MacBook - faster inference settings
            String jsonPayload = String.format(
                "{\"model\":\"%s\",\"prompt\":\"%s\",\"stream\":false,\"options\":{\"temperature\":%s,\"num_predict\":%s,\"num_ctx\":2048,\"num_thread\":8,\"repeat_penalty\":1.1,\"top_k\":40,\"top_p\":0.9}}",
                model,
                escapedPrompt,
                temperatureStr,
                maxTokensStr
            );

            CraftoMod.LOGGER.info("Sending to Ollama (JSON escaped prompt), payload length: {}", jsonPayload.length());

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(baseUrl + "/api/generate"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(jsonPayload))
                .timeout(Duration.ofSeconds(60))   // Reduced timeout for faster responses
                .build();

            CraftoMod.LOGGER.info("HTTP request created, sending...");
            long startTime = System.currentTimeMillis();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            long endTime = System.currentTimeMillis();
            CraftoMod.LOGGER.info("HTTP response received in {} ms, status: {}", (endTime - startTime), response.statusCode());

            if (response.statusCode() == 200) {
                // Ollama returns a stream of JSON objects, we need to parse the final one
                String responseBody = response.body();
                CraftoMod.LOGGER.info("Ollama response received (length: {})", responseBody.length());

                // Parse the Ollama JSON response properly
                try {
                    JsonObject ollamaResponse = JsonParser.parseString(responseBody).getAsJsonObject();
                    if (ollamaResponse.has("response")) {
                        String rawResponse = ollamaResponse.get("response").getAsString();
                        // The response is already a JSON string from Mistral, no need to unescape
                        CraftoMod.LOGGER.info("Extracted response from Ollama: {}", rawResponse);
                        return rawResponse;
                    }
                } catch (Exception e) {
                    CraftoMod.LOGGER.error("Failed to parse Ollama JSON response", e);
                }

                // If parsing fails, return the whole response for debugging
                CraftoMod.LOGGER.warn("Failed to parse Ollama response, returning raw: {}", responseBody);
                return responseBody;
            } else {
                CraftoMod.LOGGER.error("Ollama API error: {} - {}", response.statusCode(), response.body());
                return null;
            }

        } catch (IOException | InterruptedException e) {
            CraftoMod.LOGGER.error("Failed to communicate with Ollama", e);
            return null;
        }
    }
}
