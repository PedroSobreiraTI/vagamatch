package com.vagamatch.vagaservice.service;

import com.vagamatch.vagaservice.domain.Skill;
import com.vagamatch.vagaservice.dto.SkillResponse;
import com.vagamatch.vagaservice.repository.SkillRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SkillService {

    private final SkillRepository skillRepository;

    public SkillService(SkillRepository skillRepository) {
        this.skillRepository = skillRepository;
    }

    /** Reaproveita a skill se já existir (sem diferenciar maiúscula/minúscula), senão cria. */
    @Transactional
    public Skill buscarOuCriar(String nome, String categoria) {
        String nomeNormalizado = nome.trim();
        return skillRepository.findByNomeIgnoreCase(nomeNormalizado)
                .orElseGet(() -> skillRepository.save(new Skill(nomeNormalizado, categoria)));
    }

    @Transactional(readOnly = true)
    public List<SkillResponse> listar() {
        return skillRepository.findAll(Sort.by("nome")).stream()
                .map(SkillResponse::from)
                .toList();
    }
}
