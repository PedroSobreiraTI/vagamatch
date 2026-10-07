package com.vagamatch.vagaservice.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI vagaMatchOpenApi() {
        return new OpenAPI().info(new Info()
                .title("VagaMatch - vaga-service")
                .description("Cadastro de vagas e candidatos, match de skills e estatísticas")
                .version("v1"));
    }
}
