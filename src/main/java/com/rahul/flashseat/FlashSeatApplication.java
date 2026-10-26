package com.rahul.flashseat;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class FlashSeatApplication {

    public static void main(String[] args) {
        SpringApplication.run(FlashSeatApplication.class, args);
    }

}
