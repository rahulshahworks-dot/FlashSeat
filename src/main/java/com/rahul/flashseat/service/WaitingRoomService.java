package com.rahul.flashseat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class WaitingRoomService {

    private final StringRedisTemplate redisTemplate;

    private static final String QUEUE_KEY = "waiting_room:show_1";
    private static final String ADMISSION_TOKEN_PREFIX = "gate_token:";
    private static final int DRAIN_BATCH_SIZE = 2; // Har cycle mein 2 logon ko admission

    public void joinQueue(String userId) {
        if (!hasActiveToken(userId)) {
            // Score = current timestamp ensures strict FIFO ordering
            redisTemplate.opsForZSet().add(QUEUE_KEY, userId, System.currentTimeMillis());
        }
    }

    public Long getRank(String userId) {
        Long rank = redisTemplate.opsForZSet().rank(QUEUE_KEY, userId);
        return (rank != null) ? rank + 1 : -1L;
    }

    public boolean hasActiveToken(String userId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(ADMISSION_TOKEN_PREFIX + userId));
    }

    public String generateInstantToken(String userId) {
        String token = "PASS_" + UUID.randomUUID();
        redisTemplate.opsForValue().set(ADMISSION_TOKEN_PREFIX + userId, token, Duration.ofMinutes(5));
        return token;
    }

    // Controlled release: Har 3 sec mein batch uthao aur unko token do
    @Scheduled(fixedRate = 3000)
    public void drainQueue() {
        Set<String> admittedUsers = redisTemplate.opsForZSet().range(QUEUE_KEY, 0, DRAIN_BATCH_SIZE - 1);
        if (admittedUsers == null || admittedUsers.isEmpty()) {
            return;
        }

        for (String userId : admittedUsers) {
            redisTemplate.opsForZSet().remove(QUEUE_KEY, userId);
            generateInstantToken(userId);
        }
    }
}