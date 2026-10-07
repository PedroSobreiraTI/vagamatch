package com.vagamatch.vagaservice.dto;

import java.util.List;

public record MatchResponse(
        Long candidatoId,
        Long vagaId,
        String tituloVaga,
        int percentual,
        boolean atendeTodasObrigatorias,
        List<String> skillsAtendidas,
        List<String> obrigatoriasFaltando,
        List<String> diferenciaisFaltando
) {
}
