package me.dailycode.tobyreactive;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.util.concurrent.CompletableFuture;

public class HttpSend {
    public static void main(String[] args) throws URISyntaxException {

        HttpClient client = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).build();

        ObjectMapper objectMapper = new ObjectMapper();

        HttpRequest request = HttpRequest.newBuilder()
                .uri(new URI("http://localhost:8080/rest?idx=1"))
                .POST(BodyPublishers.ofString("wow"))
                .setHeader("X-good", "morning")
                .setHeader("X-sweet", "home")
                .build();

        client.sendAsync(request, HttpResponse.BodyHandlers.ofString()).thenApplyAsync(stringHttpResponse -> {
                    String body = stringHttpResponse.body();
                    return body;
                })
                .thenAccept(s -> System.out.println(s));

    }
}
