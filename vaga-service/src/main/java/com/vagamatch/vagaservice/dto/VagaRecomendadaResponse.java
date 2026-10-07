package com.vagamatch.vagaservice.dto;

import com.vagamatch.vagaservice.domain.CalculadoraMatch;
import com.vagamatch.vagaservice.repository.VagaMatchProjection;

public record VagaRecomendadaResponse(
        Long vagaId,
        String titulo,
        String empresa,
        int percentual,
        boolean atendeTodasObrigatorias,
        long obrigatoriasFaltando
) {
    public static VagaRecomendadaResponse from(VagaMatchProjection linha) {
        return new VagaRecomendadaResponse(linha.getVagaId(), linha.getTitulo(), linha.getEmpresa(),
                CalculadoraMatch.percentual(linha.getPesoAtendido(), linha.getPesoTotal()),
                linha.getObrigatoriasFaltando() == 0, linha.getObrigatoriasFaltando());
    }
}
