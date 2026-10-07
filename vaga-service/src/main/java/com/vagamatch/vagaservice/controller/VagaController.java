package com.vagamatch.vagaservice.controller;

import com.vagamatch.vagaservice.dto.VagaRequest;
import com.vagamatch.vagaservice.dto.VagaResponse;
import com.vagamatch.vagaservice.service.VagaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/vagas")
@Tag(name = "Vagas", description = "Cadastro das vagas que serão analisadas")
public class VagaController {

    private final VagaService vagaService;

    public VagaController(VagaService vagaService) {
        this.vagaService = vagaService;
    }

    @PostMapping
    @Operation(summary = "Cadastra uma vaga")
    public ResponseEntity<VagaResponse> criar(@RequestBody @Valid VagaRequest request) {
        VagaResponse criada = vagaService.criar(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(criada.id()).toUri();
        return ResponseEntity.created(location).body(criada);
    }

    @GetMapping
    @Operation(summary = "Lista as vagas (paginado, mais recentes primeiro)")
    public PagedModel<VagaResponse> listar(
            @PageableDefault(size = 20, sort = "criadaEm", direction = Sort.Direction.DESC) Pageable pageable) {
        return new PagedModel<>(vagaService.listar(pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Busca uma vaga pelo id")
    public VagaResponse buscar(@PathVariable Long id) {
        return vagaService.buscar(id);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualiza os dados de uma vaga")
    public VagaResponse atualizar(@PathVariable Long id, @RequestBody @Valid VagaRequest request) {
        return vagaService.atualizar(id, request);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Remove uma vaga")
    public ResponseEntity<Void> remover(@PathVariable Long id) {
        vagaService.remover(id);
        return ResponseEntity.noContent().build();
    }
}
