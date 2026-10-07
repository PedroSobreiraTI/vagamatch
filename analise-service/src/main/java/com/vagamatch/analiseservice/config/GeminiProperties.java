package com.vagamatch.analiseservice.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Configuração do Gemini. A chave vem de GEMINI_API_KEY (variável de ambiente ou .env).
 * fallbackModels: modelos tentados, em ordem, quando o principal responde 503 (sobrecarregado).
 */
@ConfigurationProperties(prefix = "gemini")
public record GeminiProperties(String apiKey, String model, List<String> fallbackModels,
                               String baseUrl, Duration timeout) {

    public boolean chaveConfigurada() {
        return apiKey != null && !apiKey.isBlank() && !apiKey.equals("sua-chave-aqui");
    }

    /** Modelo principal seguido dos fallbacks, sem vazios nem repetidos. */
    public List<String> modelosEmOrdem() {
        List<String> modelos = new ArrayList<>();
        modelos.add(model);
        if (fallbackModels != null) {
            fallbackModels.stream()
                    .filter(m -> m != null && !m.isBlank())
                    .map(String::trim)
                    .filter(m -> !modelos.contains(m))
                    .forEach(modelos::add);
        }
        return modelos;
    }
}
