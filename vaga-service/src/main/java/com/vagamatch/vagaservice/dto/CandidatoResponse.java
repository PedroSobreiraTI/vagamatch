package com.vagamatch.vagaservice.dto;

import com.vagamatch.vagaservice.domain.Candidato;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record CandidatoResponse(
        Long id,
        String nome,
        String email,
        LocalDateTime criadoEm,
        List<SkillDoCandidatoResponse> skills
) {
    public static CandidatoResponse from(Candidato candidato) {
        List<SkillDoCandidatoResponse> skills = candidato.getSkills().stream()
                .map(cs -> new SkillDoCandidatoResponse(cs.getSkill().getId(), cs.getSkill().getNome(), cs.getNivel()))
                .sorted(Comparator.comparing(SkillDoCandidatoResponse::nome))
                .toList();
        return new CandidatoResponse(candidato.getId(), candidato.getNome(), candidato.getEmail(),
                candidato.getCriadoEm(), skills);
    }

    public record SkillDoCandidatoResponse(Long skillId, String nome, int nivel) {
    }
}
