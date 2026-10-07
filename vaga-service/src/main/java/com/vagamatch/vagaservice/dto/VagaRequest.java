package com.vagamatch.vagaservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record VagaRequest(
        @NotBlank @Size(max = 200) String titulo,
        @Size(max = 150) String empresa,
        @Size(max = 150) String localizacao,
        @NotBlank String descricao,
        @Size(max = 500) String link
) {
}
