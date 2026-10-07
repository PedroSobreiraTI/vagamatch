package com.vagamatch.vagaservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Skill que o candidato tem, com nível de 1 (básico) a 5 (avançado). */
public record SkillNivelRequest(
        @NotBlank @Size(max = 100) String nome,
        @NotNull @Min(1) @Max(5) Integer nivel
) {
}
