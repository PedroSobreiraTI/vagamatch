package com.vagamatch.vagaservice.config;

import com.vagamatch.vagaservice.domain.StatusVaga;
import com.vagamatch.vagaservice.repository.VagaRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Métricas que não pertencem a um service específico. Ver /actuator/metrics. */
@Configuration
public class MetricasConfig {

    /** Backlog da análise: vagas esperando o analise-service. Consulta o banco a cada leitura. */
    @Bean
    Gauge vagasPendentesGauge(MeterRegistry registry, VagaRepository vagaRepository) {
        return Gauge.builder("vagamatch.vagas.pendentes", vagaRepository,
                        repo -> repo.countByStatus(StatusVaga.PENDENTE))
                .description("Vagas aguardando análise do Gemini")
                .register(registry);
    }
}
