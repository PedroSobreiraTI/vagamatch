package com.vagamatch.analiseservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/** Configuração do Gemini. A chave vem de GEMINI_API_KEY (variável de ambiente ou .env). */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String model, String baseUrl, Duration timeout) {

    public boolean chaveConfigurada() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("sua-chave-aqui");
    }
}
