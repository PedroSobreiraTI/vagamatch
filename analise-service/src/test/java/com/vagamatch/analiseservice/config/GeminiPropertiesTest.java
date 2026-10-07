package com.vagamatch.analiseservice.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class GeminiPropertiesTest {

    private static GeminiProperties comFallbacks(List<String> fallbacks) {
        return new GeminiProperties("chave", "principal", fallbacks, "url", null);
    }

    @Test
    void principalVemPrimeiroSeguidoDosFallbacks() {
        assertThat(comFallbacks(List.of("b", "c")).modelosEmOrdem()).containsExactly("principal", "b", "c");
    }

    @Test
    void ignoraVaziosRepetidosEOProprioPrincipal() {
        List<String> fallbacks = Arrays.asList(" b ", "", null, "principal", "b");
        assertThat(comFallbacks(fallbacks).modelosEmOrdem()).containsExactly("principal", "b");
    }

    @Test
    void semFallbackSoTemOPrincipal() {
        assertThat(comFallbacks(null).modelosEmOrdem()).containsExactly("principal");
    }
}
