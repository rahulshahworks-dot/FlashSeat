package com.rahul.flashseat.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
@RequiredArgsConstructor
public class TrafficMonitorService {

    private final StringRedisTemplate redisTemplate;

    private static final String TRAFFIC_KEY = "traffic:requests:count";
    private static final int TRAFFIC_LIMIT = 10; // 5 second window mein 10 requests se zyada = HIGH TRAFFIC

    public boolean isHighTraffic() {
        Long currentCount = redisTemplate.opsForValue().increment(TRAFFIC_KEY);

        if (currentCount != null && currentCount == 1) {
            // First hit in this window -> 5 second expiry set karo
            redisTemplate.expire(TRAFFIC_KEY, Duration.ofSeconds(5));
        }

        return currentCount != null && currentCount > TRAFFIC_LIMIT;
    }
}