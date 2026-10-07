package com.vagamatch.vagaservice.service;

import com.vagamatch.vagaservice.domain.Vaga;
import com.vagamatch.vagaservice.dto.VagaRequest;
import com.vagamatch.vagaservice.dto.VagaResponse;
import com.vagamatch.vagaservice.exception.RecursoNaoEncontradoException;
import com.vagamatch.vagaservice.repository.VagaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VagaService {

    private final VagaRepository vagaRepository;

    public VagaService(VagaRepository vagaRepository) {
        this.vagaRepository = vagaRepository;
    }

    @Transactional
    public VagaResponse criar(VagaRequest request) {
        Vaga vaga = new Vaga(request.titulo(), request.empresa(), request.localizacao(),
                request.descricao(), request.link());
        return VagaResponse.from(vagaRepository.save(vaga));
    }

    @Transactional(readOnly = true)
    public Page<VagaResponse> listar(Pageable pageable) {
        return vagaRepository.findAll(pageable).map(VagaResponse::from);
    }

    @Transactional(readOnly = true)
    public VagaResponse buscar(Long id) {
        return VagaResponse.from(buscarEntidade(id));
    }

    @Transactional
    public VagaResponse atualizar(Long id, VagaRequest request) {
        Vaga vaga = buscarEntidade(id);
        vaga.atualizar(request.titulo(), request.empresa(), request.localizacao(),
                request.descricao(), request.link());
        return VagaResponse.from(vaga);
    }

    @Transactional
    public void remover(Long id) {
        vagaRepository.delete(buscarEntidade(id));
    }

    private Vaga buscarEntidade(Long id) {
        return vagaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga", id));
    }
}
