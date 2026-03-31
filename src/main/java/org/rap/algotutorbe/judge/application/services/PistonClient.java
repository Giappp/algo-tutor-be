package org.rap.algotutorbe.judge.application.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.rap.algotutorbe.judge.application.dto.PistonFile;
import org.rap.algotutorbe.judge.application.dto.PistonRequest;
import org.rap.algotutorbe.judge.application.dto.PistonResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class PistonClient {

    private final RestTemplate restTemplate;

    @Value("${piston.api.url}")
    private String pistonApiUrl;

    public PistonResponse executeCode(String language, String sourceCode, String stdin) {
        String fileName = language.equalsIgnoreCase("java") ? "Main.java" : "main." + language.toLowerCase();

        PistonRequest request = new PistonRequest(
                language.toLowerCase(),
                "*",
                List.of(new PistonFile(fileName, sourceCode)),
                stdin != null ? stdin : "",
                10000,
                3000
        );

        try {
            return restTemplate.postForObject(pistonApiUrl, request, PistonResponse.class);
        } catch (Exception e) {
            log.error("Error calling Piston API: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to execute code on Piston", e);
        }
    }
}