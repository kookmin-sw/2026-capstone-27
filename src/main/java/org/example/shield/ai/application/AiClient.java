package org.example.shield.ai.application;

public interface AiClient {

    String classify(String content);

    String generateBrief(String prompt);
}
