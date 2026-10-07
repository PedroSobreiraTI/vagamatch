package com.vagamatch.vagaservice.service;

import com.vagamatch.vagaservice.domain.Candidato;
import com.vagamatch.vagaservice.domain.Skill;
import com.vagamatch.vagaservice.dto.CandidatoRequest;
import com.vagamatch.vagaservice.dto.CandidatoResponse;
import com.vagamatch.vagaservice.dto.SkillNivelRequest;
import com.vagamatch.vagaservice.exception.ConflitoException;
import com.vagamatch.vagaservice.exception.RecursoNaoEncontradoException;
import com.vagamatch.vagaservice.repository.CandidatoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class CandidatoService {

    private final CandidatoRepository candidatoRepository;
    private final SkillService skillService;

    public CandidatoService(CandidatoRepository candidatoRepository, SkillService skillService) {
        this.candidatoRepository = candidatoRepository;
        this.skillService = skillService;
    }

    @Transactional
    public CandidatoResponse criar(CandidatoRequest request) {
        if (candidatoRepository.existsByEmailIgnoreCase(request.email())) {
            throw new ConflitoException("Já existe um candidato com o email " + request.email());
        }
        Candidato candidato = new Candidato(request.nome(), request.email());
        return CandidatoResponse.from(candidatoRepository.save(candidato));
    }

    @Transactional(readOnly = true)
    public List<CandidatoResponse> listar() {
        return candidatoRepository.findAll().stream().map(CandidatoResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public CandidatoResponse buscar(Long id) {
        return CandidatoResponse.from(buscarEntidade(id));
    }

    @Transactional
    public CandidatoResponse atualizar(Long id, CandidatoRequest request) {
        if (candidatoRepository.existsByEmailIgnoreCaseAndIdNot(request.email(), id)) {
            throw new ConflitoException("Já existe um candidato com o email " + request.email());
        }
        Candidato candidato = buscarEntidade(id);
        candidato.atualizar(request.nome(), request.email());
        return CandidatoResponse.from(candidato);
    }

    @Transactional
    public void remover(Long id) {
        candidatoRepository.delete(buscarEntidade(id));
    }

    /** Adiciona as skills ao perfil; se o candidato já tiver alguma, atualiza o nível. */
    @Transactional
    public CandidatoResponse definirSkills(Long id, List<SkillNivelRequest> skills) {
        Candidato candidato = buscarEntidade(id);
        for (SkillNivelRequest item : skills) {
            Skill skill = skillService.buscarOuCriar(item.nome(), null);
            candidato.definirSkill(skill, item.nivel());
        }
        return CandidatoResponse.from(candidato);
    }

    @Transactional
    public void removerSkill(Long candidatoId, Long skillId) {
        Candidato candidato = buscarEntidade(candidatoId);
        if (!candidato.removerSkill(skillId)) {
            throw new RecursoNaoEncontradoException("Skill do candidato", skillId);
        }
    }

    private Candidato buscarEntidade(Long id) {
        return candidatoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Candidato", id));
    }
}
