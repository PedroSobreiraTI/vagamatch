package com.vagamatch.vagaservice.dto;

import com.vagamatch.vagaservice.domain.Vaga;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

public record VagaResponse(
        Long id,
        String titulo,
        String empresa,
        String localizacao,
        String descricao,
        String link,
        LocalDateTime criadaEm,
        List<SkillDaVagaResponse> skills
) {
    public static VagaResponse from(Vaga vaga) {
        List<SkillDaVagaResponse> skills = vaga.getSkills().stream()
                .map(vs -> new SkillDaVagaResponse(vs.getSkill().getNome(), vs.getSkill().getCategoria(), vs.isObrigatoria()))
                .sorted(Comparator.comparing(SkillDaVagaResponse::nome))
                .toList();
        return new VagaResponse(vaga.getId(), vaga.getTitulo(), vaga.getEmpresa(), vaga.getLocalizacao(),
                vaga.getDescricao(), vaga.getLink(), vaga.getCriadaEm(), skills);
    }

    public record SkillDaVagaResponse(String nome, String categoria, boolean obrigatoria) {
    }
}
