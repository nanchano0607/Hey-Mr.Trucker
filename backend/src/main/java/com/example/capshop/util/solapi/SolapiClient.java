package com.example.capshop.util.solapi;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

public class SolapiClient {

    private static final URI SEND_MANY_DETAIL_URI = URI.create("https://api.solapi.com/messages/v4/send-many/detail");

    private final HttpClient httpClient;

    public SolapiClient() {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    public String sendManyDetail(String apiKey, String apiSecret, String messageJson) throws Exception {
        String authHeader = SolapiAuth.createAuthHeader(apiKey, apiSecret);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(SEND_MANY_DETAIL_URI)
                .timeout(Duration.ofSeconds(20))
                .header("Authorization", authHeader)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(messageJson))
                .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new IllegalStateException("Solapi request failed: status=" + response.statusCode() + ", body=" + response.body());
        }

        return response.body();
    }
}
