package com.vagamatch.vagaservice.controller;

import com.vagamatch.vagaservice.dto.MatchResponse;
import com.vagamatch.vagaservice.dto.VagaRecomendadaResponse;
import com.vagamatch.vagaservice.service.MatchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/candidatos/{candidatoId}")
@Tag(name = "Match", description = "Compatibilidade do candidato com as vagas")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @GetMapping("/match/{vagaId}")
    @Operation(summary = "Match do candidato com uma vaga (obrigatória pesa 2, diferencial pesa 1)")
    public MatchResponse match(@PathVariable Long candidatoId, @PathVariable Long vagaId) {
        return matchService.calcular(candidatoId, vagaId);
    }

    @GetMapping("/vagas-recomendadas")
    @Operation(summary = "Vagas analisadas ordenadas pelo match com o candidato")
    public PagedModel<VagaRecomendadaResponse> recomendadas(@PathVariable Long candidatoId,
                                                            @PageableDefault(size = 10) Pageable pageable) {
        return new PagedModel<>(matchService.recomendar(candidatoId, pageable));
    }
}
