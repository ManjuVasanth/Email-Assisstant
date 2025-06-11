package com.manju.email_writer.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.manju.email_writer.model.EmailRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmailGeneratorService {

    private final WebClient webClient;

    public EmailGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    @Value("${gemini.api.url}")
    private String geminiApiUrl;
    @Value("${gemini.api.key}")
    private String geminiApiKey;

    public String generateEmailReply(EmailRequest emailRequest) {
        // craft a request
        String prompt = buildPrompt(emailRequest);
        Map<String, Object> requestBody = Map.of("contents", new Object[]{
                Map.of("parts", new Object[]{
                        Map.of("text", prompt)})
        });

        //Do request and get response
        String response = webClient.post().uri(geminiApiUrl+geminiApiKey)
                .header("Content-type","application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();

        //Extract response and return
        return extractResponseContent(response);
    }
// to take only the text from response
    private String extractResponseContent(String response) {
        try{
            ObjectMapper mapper = new ObjectMapper();
            //readTree() is used to convert JSON response to tree like structure
           JsonNode rootNode= mapper.readTree(response);
           return rootNode.path("candidates")
                   .get(0)
                   .path("content")
                   .path("parts")
                   .get(0)
                   .path("text")
                   .asText();
        } catch (Exception e) {
            return "Error processing request" +e.getMessage();
        }
    }

    //Build prompt
    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Generate a professional email for the following email content. Please don't generate a subject line");
        if (emailRequest.getTone() != null && emailRequest.getTone().isEmpty()) {
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");

        }
        prompt.append("\nOriginal email: \n").append(emailRequest.getEmailContent());

        return prompt.toString();
    }
}
