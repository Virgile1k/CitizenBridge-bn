package com.citizenbridge.citizenbridge.service;

import com.citizenbridge.citizenbridge.dtos.GeminiCategoryResponse;
import com.citizenbridge.citizenbridge.model.Category;
import com.citizenbridge.citizenbridge.repository.CategoryRepository;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class GeminiAIService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiAIService.class);

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private RestTemplate restTemplate;

    @Value("${gemini.api.key:AIzaSyBEM0S6CIW2ZNgs0_gmhcF_Ssp-O8SSRqs}")
    private String apiKey;

    @Value("${gemini.api.url:https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent}")
    private String apiUrl;

    /**
     * Predicts the most appropriate category for a complaint using Google's Gemini AI
     */
    public GeminiCategoryResponse predictCategory(String title, String description) {
        try {
            // Get all active categories from the database
            List<Category> categories = categoryRepository.findByIsActiveTrue();

            if (categories.isEmpty()) {
                logger.warn("No active categories found in the database");
                return new GeminiCategoryResponse(null, null, null, 0.0);
            }

            // Format categories for the prompt
            String categoryOptions = categories.stream()
                    .map(cat -> String.format("ID: %s, Name: %s, Description: %s",
                            cat.getId(), cat.getName(), cat.getDescription()))
                    .collect(Collectors.joining("\n"));

            // Build the prompt for the AI model
            String prompt = String.format(
                    "Analyze the following citizen complaint and determine the most appropriate category " +
                            "from the provided options. Return your answer in JSON format only.\n\n" +
                            "Complaint Title: %s\n" +
                            "Complaint Description: %s\n\n" +
                            "Available Categories:\n%s\n\n" +
                            "Please return your response in the following JSON format only:\n" +
                            "{\n" +
                            "  \"categoryId\": \"[selected category ID]\",\n" +
                            "  \"confidence\": [confidence score between 0 and 1],\n" +
                            "  \"reasoning\": \"[brief explanation of your selection]\"\n" +
                            "}\n",
                    title, description, categoryOptions);

            // Call Gemini API with the free API key
            String responseText = callGeminiApi(prompt);

            // Extract JSON from response
            String jsonStr = extractJsonFromResponse(responseText);
            JsonObject jsonResponse = JsonParser.parseString(jsonStr).getAsJsonObject();

            String categoryId = jsonResponse.get("categoryId").getAsString();
            double confidence = jsonResponse.get("confidence").getAsDouble();
            String reasoning = jsonResponse.get("reasoning").getAsString();

            // Look up category name
            Optional<Category> category = categories.stream()
                    .filter(c -> c.getId().equals(categoryId))
                    .findFirst();

            String categoryName = category.map(Category::getName).orElse("Unknown");

            return new GeminiCategoryResponse(categoryId, categoryName, reasoning, confidence);

        } catch (Exception e) {
            logger.error("Error predicting category with Gemini AI", e);
            return new GeminiCategoryResponse(null, null,
                    "Error processing with AI: " + e.getMessage(), 0.0);
        }
    }

    /**
     * Calls the free Gemini API using the provided API key
     */
    private String callGeminiApi(String prompt) {
        // Construct URL with API key
        String fullUrl = apiUrl + "?key=" + apiKey;

        // Create headers
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        // Build request body according to Gemini API specifications
        Map<String, Object> requestBody = new HashMap<>();

        // Add generation config
        Map<String, Object> generationConfig = new HashMap<>();
        generationConfig.put("temperature", 0.2);
        generationConfig.put("maxOutputTokens", 1024);
        generationConfig.put("topK", 40);
        generationConfig.put("topP", 0.95);
        requestBody.put("generationConfig", generationConfig);

        // Add content
        List<Map<String, Object>> contents = new ArrayList<>();
        Map<String, Object> content = new HashMap<>();
        List<Map<String, String>> parts = new ArrayList<>();
        Map<String, String> textPart = new HashMap<>();
        textPart.put("text", prompt);
        parts.add(textPart);
        content.put("parts", parts);
        contents.add(content);
        requestBody.put("contents", contents);

        // Create the request entity
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(requestBody, headers);

        // Make the API call
        ResponseEntity<String> response = restTemplate.postForEntity(fullUrl, request, String.class);

        // Process the response
        String responseBody = response.getBody();
        JsonObject jsonResponse = JsonParser.parseString(responseBody).getAsJsonObject();

        // Extract the generated text from the response
        String generatedText = jsonResponse
                .getAsJsonArray("candidates")
                .get(0).getAsJsonObject()
                .getAsJsonObject("content")
                .getAsJsonArray("parts")
                .get(0).getAsJsonObject()
                .get("text").getAsString();

        return generatedText;
    }

    /**
     * Helper method to extract JSON from AI response
     */
    private String extractJsonFromResponse(String response) {
        if (response.contains("```json")) {
            int start = response.indexOf("```json") + 7;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        } else if (response.contains("```")) {
            int start = response.indexOf("```") + 3;
            int end = response.indexOf("```", start);
            return response.substring(start, end).trim();
        }

        // Direct JSON response
        return response.trim();
    }
}