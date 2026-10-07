package com.vagamatch.analiseservice.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.vagamatch.analiseservice.config.GeminiProperties;
import com.vagamatch.analiseservice.dto.SkillExtraida;
import com.vagamatch.analiseservice.exception.AnaliseException;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * Chama a API do Gemini (generateContent) pedindo a resposta em JSON estruturado:
 * o responseSchema obriga o modelo a devolver exatamente {"skills": [...]}, sem texto solto.
 */
@Service
public class GeminiService {

    private static final List<String> CATEGORIAS = List.of(
            "LINGUAGEM", "FRAMEWORK", "BANCO_DE_DADOS", "CLOUD", "DEVOPS",
            "FERRAMENTA", "METODOLOGIA", "SOFT_SKILL", "OUTRO");

    private static final Map<String, Object> RESPONSE_SCHEMA = Map.of(
            "type", "OBJECT",
            "properties", Map.of(
                    "skills", Map.of(
                            "type", "ARRAY",
                            "items", Map.of(
                                    "type", "OBJECT",
                                    "properties", Map.of(
                                            "nome", Map.of("type", "STRING"),
                                            "categoria", Map.of("type", "STRING", "format", "enum", "enum", CATEGORIAS),
                                            "obrigatoria", Map.of("type", "BOOLEAN")),
                                    "required", List.of("nome", "categoria", "obrigatoria")))),
            "required", List.of("skills"));

    private static final String PROMPT = """
            Você é um recrutador técnico. Extraia da vaga abaixo as skills técnicas e comportamentais exigidas.
            Regras:
            - Use o nome mais comum da skill, curto e sem versão (ex: "Java", "Spring Boot", "PostgreSQL", "Docker").
            - Não repita skills e não invente skills que não estão no texto.
            - obrigatoria = true para requisitos; false para diferenciais ("desejável", "é um plus", "diferencial").
            - categoria: uma de %s.

            Título: %s

            Descrição:
            %s
            """;

    private final GeminiProperties properties;
    private final JsonMapper jsonMapper;
    private final RestClient restClient;

    public GeminiService(GeminiProperties properties, JsonMapper jsonMapper) {
        this.properties = properties;
        this.jsonMapper = jsonMapper;

        Duration timeout = properties.timeout() != null ? properties.timeout() : Duration.ofSeconds(30);
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(5));
        requestFactory.setReadTimeout(timeout);

        this.restClient = RestClient.builder()
                .baseUrl(properties.baseUrl())
                .requestFactory(requestFactory)
                .build();
    }

    public List<SkillExtraida> extrairSkills(String titulo, String descricao) {
        if (!properties.chaveConfigurada()) {
            throw new AnaliseException("GEMINI_API_KEY não configurada");
        }

        Map<String, Object> corpo = Map.of(
                "contents", List.of(Map.of("parts", List.of(Map.of(
                        "text", PROMPT.formatted(String.join(", ", CATEGORIAS), titulo, descricao))))),
                "generationConfig", Map.of(
                        "responseMimeType", "application/json",
                        "responseSchema", RESPONSE_SCHEMA,
                        "temperature", 0));

        String resposta;
        try {
            resposta = restClient.post()
                    .uri("/models/{modelo}:generateContent", properties.model())
                    .header("x-goog-api-key", properties.apiKey()) // no header, nunca na URL (não vaza em log)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(corpo)
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException e) {
            throw new AnaliseException("Erro ao chamar o Gemini: " + e.getMessage(), e);
        }

        return lerSkills(resposta);
    }

    private List<SkillExtraida> lerSkills(String resposta) {
        try {
            GeminiResponse gemini = jsonMapper.readValue(resposta, GeminiResponse.class);
            if (gemini.candidates() == null || gemini.candidates().isEmpty()
                    || gemini.candidates().getFirst().content() == null
                    || gemini.candidates().getFirst().content().parts() == null
                    || gemini.candidates().getFirst().content().parts().isEmpty()) {
                throw new AnaliseException("Gemini respondeu sem conteúdo");
            }
            // com responseMimeType=application/json o texto da resposta é o próprio JSON pedido
            String json = gemini.candidates().getFirst().content().parts().getFirst().text();
            ResultadoAnalise resultado = jsonMapper.readValue(json, ResultadoAnalise.class);
            return resultado.skills() == null ? List.of() : resultado.skills();
        } catch (JacksonException e) {
            throw new AnaliseException("Resposta do Gemini em formato inesperado", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record GeminiResponse(List<Candidate> candidates) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Candidate(Content content) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Content(List<Part> parts) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record Part(String text) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ResultadoAnalise(List<SkillExtraida> skills) {
    }
}
