package com.vagamatch.vagaservice.dto;

import java.util.List;

/** Resultado da análise vindo do analise-service em vaga.analisada. */
public record VagaAnalisadaEvent(Long vagaId, boolean sucesso, String erro, List<SkillExtraida> skills) {

    public record SkillExtraida(String nome, String categoria, boolean obrigatoria) {
    }
}
