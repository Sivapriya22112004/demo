package com.example.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Component
public class StartupRunner implements ApplicationRunner {

    private final RestTemplate restTemplate;

    @Value("${bfhl.name}")
    private String name;

    @Value("${bfhl.regNo}")
    private String regNo;

    @Value("${bfhl.email}")
    private String email;

    @Value("${bfhl.finalQuery}")
    private String finalQuery;

    public StartupRunner(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            // 1) Generate Webhook
            String generateUrl = "https://bfhldevapigw.healthrx.co.in/hiring/generateWebhook/JAVA";

            Map<String, String> payload = new HashMap<>();
            payload.put("name", name);
            payload.put("regNo", regNo);
            payload.put("email", email);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> request = new HttpEntity<>(payload, headers);

            System.out.println("-> Sending generateWebhook request...");
            ResponseEntity<Map> response = restTemplate.postForEntity(generateUrl, request, Map.class);

            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                System.out.println("generateWebhook failed: " + response.getStatusCode());
                return;
            }

            Map body = response.getBody();
            String webhookUrl = body.get("webhook") != null ? body.get("webhook").toString() : null;
            String accessToken = body.get("accessToken") != null ? body.get("accessToken").toString() : null;

            System.out.println("Received webhook: " + webhookUrl);
            System.out.println("Received accessToken: " + (accessToken == null ? "null" : "[REDACTED]"));

            // 2) Submit finalQuery
            if (finalQuery == null || finalQuery.trim().isEmpty()) {
                System.out.println("bfhl.finalQuery is empty in application.properties. Fill it and restart.");
                return;
            }

            if (webhookUrl == null || webhookUrl.isBlank()) {
                System.out.println("Webhook URL missing in response. Using fallback.");
                webhookUrl = "https://bfhldevapigw.healthrx.co.in/hiring/testWebhook/JAVA";
            }

            Map<String, String> submitBody = new HashMap<>();
            submitBody.put("finalQuery", finalQuery.trim());

            HttpHeaders submitHeaders = new HttpHeaders();
            submitHeaders.setContentType(MediaType.APPLICATION_JSON);
            if (accessToken != null) submitHeaders.set("Authorization", accessToken);

            HttpEntity<Map<String, String>> submitEntity = new HttpEntity<>(submitBody, submitHeaders);

            System.out.println("-> Submitting finalQuery...");
            ResponseEntity<String> submitResp = restTemplate.postForEntity(webhookUrl, submitEntity, String.class);

            System.out.println("Submit status: " + submitResp.getStatusCode());
            System.out.println("Submit response: " + submitResp.getBody());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
