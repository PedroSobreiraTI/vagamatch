package com.vagamatch.analiseservice.service;

import com.vagamatch.analiseservice.config.GeminiProperties;
import com.vagamatch.analiseservice.dto.SkillExtraida;
import com.vagamatch.analiseservice.exception.AnaliseException;
import com.vagamatch.analiseservice.exception.GeminiPermanenteException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withException;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/** Testa a chamada HTTP de verdade (corpo, URL, status) com um servidor falso do Spring. */
class GeminiServiceTest {

    private static final String BASE_URL = "https://gemini.teste/v1beta";
    private static final String PRINCIPAL = "gemini-principal";
    private static final String FALLBACK = "gemini-fallback";

    private static final String RESPOSTA_OK = """
            {"candidates": [{"content": {"parts": [{"text": "{\\"skills\\": [\
            {\\"nome\\": \\"Java\\", \\"categoria\\": \\"LINGUAGEM\\", \\"obrigatoria\\": true}, \
            {\\"nome\\": \\"Docker\\", \\"categoria\\": \\"DEVOPS\\", \\"obrigatoria\\": false}]}"}]}}]}
            """;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();
    private MockRestServiceServer server;

    private GeminiService service(String apiKey, List<String> fallbacks) {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        GeminiProperties properties = new GeminiProperties(apiKey, PRINCIPAL, fallbacks, BASE_URL, Duration.ofSeconds(5));
        return new GeminiService(properties, jsonMapper, builder);
    }

    private GeminiService service() {
        return service("chave-teste", List.of(FALLBACK));
    }

    private static String url(String modelo) {
        return BASE_URL + "/models/" + modelo + ":generateContent";
    }

    @Test
    void extraiSkillsDaRespostaDoModeloPrincipal() {
        GeminiService service = service();
        server.expect(requestTo(url(PRINCIPAL)))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("x-goog-api-key", "chave-teste"))
                .andRespond(withSuccess(RESPOSTA_OK, MediaType.APPLICATION_JSON));

        List<SkillExtraida> skills = service.extrairSkills("Dev Java", "Java obrigatório, Docker é diferencial");

        assertThat(skills).containsExactly(
                new SkillExtraida("Java", "LINGUAGEM", true),
                new SkillExtraida("Docker", "DEVOPS", false));
        server.verify();
    }

    @Test
    void usaModeloDeFallbackQuandoOPrincipalResponde503() {
        GeminiService service = service();
        server.expect(requestTo(url(PRINCIPAL))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(url(FALLBACK))).andRespond(withSuccess(RESPOSTA_OK, MediaType.APPLICATION_JSON));

        assertThat(service.extrairSkills("Dev Java", "...")).hasSize(2);
        server.verify();
    }

    @Test
    void todosOsModelosCom503ViraErroTransitorio() {
        GeminiService service = service();
        server.expect(requestTo(url(PRINCIPAL))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));
        server.expect(requestTo(url(FALLBACK))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> service.extrairSkills("Dev Java", "..."))
                .isExactlyInstanceOf(AnaliseException.class)
                .hasMessageContaining("503");
        server.verify();
    }

    @Test
    void semFallbackConfiguradoTentaSoOPrincipal() {
        GeminiService service = service("chave-teste", null);
        server.expect(requestTo(url(PRINCIPAL))).andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> service.extrairSkills("Dev Java", "..."))
                .isExactlyInstanceOf(AnaliseException.class);
        server.verify();
    }

    @Test
    void erroDiferenteDe503NaoTentaOFallback() {
        GeminiService service = service();
        // só o principal é esperado: se o fallback fosse chamado, o servidor falso dava erro
        server.expect(requestTo(url(PRINCIPAL))).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> service.extrairSkills("Dev Java", "..."))
                .isExactlyInstanceOf(AnaliseException.class)
                .hasMessageContaining("500");
        server.verify();
    }

    @ParameterizedTest
    @ValueSource(ints = {400, 401, 403, 404})
    void erro4xxEhPermanente(int status) {
        GeminiService service = service();
        server.expect(requestTo(url(PRINCIPAL))).andRespond(withStatus(HttpStatus.valueOf(status)));

        assertThatThrownBy(() -> service.extrairSkills("Dev Java", "..."))
                .isInstanceOf(GeminiPermanenteException.class);
        server.verify();
    }

    @Test
    void rateLimit429EhTransitorio() {
        GeminiService service = service();
        server.expect(requestTo(url(PRINCIPAL))).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> service.extrairSkills("Dev Java", "..."))
                .isExactlyInstanceOf(AnaliseException.class);
        server.verify();
    }

    @Test
    void timeoutEhTransitorio() {
        GeminiService service = service();
        server.expect(requestTo(url(PRINCIPAL)))
                .andRespond(withException(new SocketTimeoutException("Read timed out")));

        assertThatThrownBy(() -> service.extrairSkills("Dev Java", "..."))
                .isExactlyInstanceOf(AnaliseException.class)
                .hasRootCauseInstanceOf(IOException.class);
    }

    @Test
    void chaveAusenteEhPermanenteENaoChamaOGemini() {
        GeminiService service = service("", List.of(FALLBACK));

        assertThatThrownBy(() -> service.extrairSkills("Dev Java", "..."))
                .isInstanceOf(GeminiPermanenteException.class)
                .hasMessageContaining("GEMINI_API_KEY");
        server.verify(); // nenhuma requisição feita
    }

    @Test
    void chaveDeExemploDoEnvContaComoAusente() {
        GeminiService service = service("sua-chave-aqui", List.of());

        assertThatThrownBy(() -> service.extrairSkills("Dev Java", "..."))
                .isInstanceOf(GeminiPermanenteException.class);
    }

    @Test
    void respostaSemCandidatosEhErro() {
        GeminiService service = service();
        server.expect(requestTo(url(PRINCIPAL)))
                .andRespond(withSuccess("{\"candidates\": []}", MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.extrairSkills("Dev Java", "..."))
                .isExactlyInstanceOf(AnaliseException.class)
                .hasMessageContaining("sem conteúdo");
    }

    @Test
    void textoQueNaoEhJsonEhErro() {
        GeminiService service = service();
        server.expect(requestTo(url(PRINCIPAL))).andRespond(withSuccess(
                "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"desculpe, não consegui\"}]}}]}",
                MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> service.extrairSkills("Dev Java", "..."))
                .isExactlyInstanceOf(AnaliseException.class)
                .hasMessageContaining("formato inesperado");
    }

    @Test
    void jsonSemSkillsDevolveListaVazia() {
        GeminiService service = service();
        server.expect(requestTo(url(PRINCIPAL))).andRespond(withSuccess(
                "{\"candidates\": [{\"content\": {\"parts\": [{\"text\": \"{}\"}]}}]}",
                MediaType.APPLICATION_JSON));

        assertThat(service.extrairSkills("Dev Java", "...")).isEmpty();
    }
}
