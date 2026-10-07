package com.vagamatch.vagaservice.dto;

import java.util.List;

/** Skills mais pedidas; percentualVagas é sobre o total de vagas analisadas. */
public record EstatisticaSkillsResponse(long vagasAnalisadas, List<SkillPedida> skills) {

    public record SkillPedida(Long skillId, String nome, String categoria, long totalVagas,
                              long comoObrigatoria, long comoDiferencial, int percentualVagas) {
    }
}
