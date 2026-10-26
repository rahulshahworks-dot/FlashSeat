package com.rahul.flashseat;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class GateLoadSimulator {

    private static final String BASE_URL = "http://localhost:8080/api/gate/access?userId=user_";
    private static final int TOTAL_USERS = 25;

    public static void main(String[] args) throws Exception {
        System.out.println("=== STARTING GATE LOAD SIMULATION WITH JAVA 21 VIRTUAL THREADS ===");

        HttpClient client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(3))
                .build();

        AtomicInteger directAccessCount = new AtomicInteger();
        AtomicInteger waitingRoomCount = new AtomicInteger();

        // Java 21 Virtual Threads - 25 threads ek saath burst honge
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            for (int i = 1; i <= TOTAL_USERS; i++) {
                final int userId = i;
                executor.submit(() -> {
                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(BASE_URL + userId))
                                .GET()
                                .build();

                        HttpResponse<java.io.InputStream> response = client.send(
                                request,
                                HttpResponse.BodyHandlers.ofInputStream()
                        );

                        // SSE stream ki pehli line padho
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.body()))) {
                            String firstLine = reader.readLine();
                            if (firstLine != null && firstLine.contains("DIRECT_ACCESS")) {
                                directAccessCount.incrementAndGet();
                                System.out.printf("[User %02d] -> DIRECT_ACCESS (Gate Cleared)%n", userId);
                            } else if (firstLine != null && firstLine.contains("QUEUE_UPDATE")) {
                                waitingRoomCount.incrementAndGet();
                                System.out.printf("[User %02d] -> QUEUE_UPDATE (Pushed to Waiting Room)%n", userId);
                            } else {
                                System.out.printf("[User %02d] -> %s%n", userId, firstLine);
                            }
                        }
                    } catch (Exception e) {
                        System.out.printf("[User %02d] -> Failed: %s%n", userId, e.getMessage());
                    }
                });
            }
        } // Executor close hone par saare virtual threads complete hone ka wait karega

        System.out.println("\n=== SIMULATION SUMMARY ===");
        System.out.println("Direct Access Granted : " + directAccessCount.get());
        System.out.println("Waiting Room Enqueued : " + waitingRoomCount.get());
    }
}