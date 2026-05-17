package org.rap.algotutorbe;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class AlgoTutorBeApplication {
    public static void main(String[] args) {
        SpringApplication.run(AlgoTutorBeApplication.class, args);
    }
}
