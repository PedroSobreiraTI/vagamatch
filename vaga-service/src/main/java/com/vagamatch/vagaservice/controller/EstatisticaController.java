package com.vagamatch.vagaservice.controller;

import com.vagamatch.vagaservice.dto.EstatisticaSkillsResponse;
import com.vagamatch.vagaservice.service.EstatisticaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/estatisticas")
@Tag(name = "Estatísticas", description = "Visão agregada das vagas analisadas")
public class EstatisticaController {

    private final EstatisticaService estatisticaService;

    public EstatisticaController(EstatisticaService estatisticaService) {
        this.estatisticaService = estatisticaService;
    }

    @GetMapping("/skills")
    @Operation(summary = "Skills mais pedidas nas vagas analisadas")
    public EstatisticaSkillsResponse skills(@RequestParam(defaultValue = "10") @Min(1) @Max(100) int limite) {
        return estatisticaService.skillsMaisPedidas(limite);
    }
}
