package com.vagamatch.vagaservice.controller;

import com.vagamatch.vagaservice.dto.CandidatoRequest;
import com.vagamatch.vagaservice.dto.CandidatoResponse;
import com.vagamatch.vagaservice.dto.SkillNivelRequest;
import com.vagamatch.vagaservice.service.CandidatoService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@Validated
@RequestMapping("/candidatos")
@Tag(name = "Candidatos", description = "Perfil do candidato e suas skills")
public class CandidatoController {

    private final CandidatoService candidatoService;

    public CandidatoController(CandidatoService candidatoService) {
        this.candidatoService = candidatoService;
    }

    @PostMapping
    @Operation(summary = "Cadastra um candidato")
    public ResponseEntity<CandidatoResponse> criar(@RequestBody @Valid CandidatoRequest request) {
        CandidatoResponse criado = candidatoService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criado.id()).toUri();
        return ResponseEntity.created(location).body(criado);
    }

    @GetMapping
    @Operation(summary = "Lista os candidatos")
    public List<CandidatoResponse> listar() {
        return candidatoService.listar();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca um candidato com suas skills")
    public CandidatoResponse buscar(@PathVariable Long id) {
        return candidatoService.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza nome e email do candidato")
    public CandidatoResponse atualizar(@PathVariable Long id, @RequestBody @Valid CandidatoRequest request) {
        return candidatoService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove um candidato")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        candidatoService.remover(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/skills")
    @Operation(summary = "Adiciona skills ao perfil (ou atualiza o nível das que já existem)")
    public CandidatoResponse definirSkills(@PathVariable Long id,
                                           @RequestBody @NotEmpty List<@Valid SkillNivelRequest> skills) {
        return candidatoService.definirSkills(id, skills);
    }

    @DeleteMapping("/{id}/skills/{skillId}")
    @Operation(summary = "Remove uma skill do perfil")
    public ResponseEntity<Void> removerSkill(@PathVariable Long id, @PathVariable Long skillId) {
        candidatoService.removerSkill(id, skillId);
        return ResponseEntity.noContent().build();
    }
}
