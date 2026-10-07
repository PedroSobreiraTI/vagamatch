package com.vagamatch.analiseservice.dto;

import java.util.List;

/** Publicado em vaga.analisada com o resultado (ou a falha) da análise. */
public record VagaAnalisadaEvent(Long vagaId, boolean sucesso, String erro, List<SkillExtraida> skills) {

    public static VagaAnalisadaEvent sucesso(Long vagaId, List<SkillExtraida> skills) {
        return new VagaAnalisadaEvent(vagaId, true, null, skills);
    }

    public static VagaAnalisadaEvent falha(Long vagaId, String erro) {
        return new VagaAnalisadaEvent(vagaId, false, erro, List.of());
    }
}
