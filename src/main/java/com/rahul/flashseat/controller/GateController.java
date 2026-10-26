package com.rahul.flashseat.controller;

import com.rahul.flashseat.service.TrafficMonitorService;
import com.rahul.flashseat.service.WaitingRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/gate")
@RequiredArgsConstructor
public class GateController {

    private final TrafficMonitorService trafficMonitorService;
    private final WaitingRoomService waitingRoomService;
    private final StringRedisTemplate redisTemplate;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(4);

    @GetMapping(value = "/access", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter accessGate(@RequestParam String userId) {
        SseEmitter emitter = new SseEmitter(60_000L); // 60s timeout

        // Condition 1: Traffic normal hai & pehle se queue mein nahi
        if (!trafficMonitorService.isHighTraffic() && !waitingRoomService.hasActiveToken(userId)) {
            try {
                String token = waitingRoomService.generateInstantToken(userId);
                emitter.send(SseEmitter.event()
                        .name("DIRECT_ACCESS")
                        .data(Map.of("status", "ALLOWED", "token", token)));
                emitter.complete();
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            return emitter;
        }

        // Condition 2: High traffic -> Queue mein dalo aur status stream karo
        waitingRoomService.joinQueue(userId);

        ScheduledFuture<?> task = scheduler.scheduleAtFixedRate(() -> {
            try {
                // Check if user got admitted by drain scheduler
                if (waitingRoomService.hasActiveToken(userId)) {
                    String token = redisTemplate.opsForValue().get("gate_token:" + userId);
                    emitter.send(SseEmitter.event()
                            .name("ADMITTED")
                            .data(Map.of("status", "ALLOWED", "token", token != null ? token : "")));
                    emitter.complete();
                    return;
                }

                // Push current queue rank
                Long rank = waitingRoomService.getRank(userId);
                if (rank != -1L) {
                    emitter.send(SseEmitter.event()
                            .name("QUEUE_UPDATE")
                            .data(Map.of("rank", rank, "status", "WAITING")));
                }
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
        }, 0, 1, TimeUnit.SECONDS);

        emitter.onCompletion(() -> task.cancel(true));
        emitter.onTimeout(() -> task.cancel(true));
        emitter.onError(t -> task.cancel(true));

        return emitter;
    }
}