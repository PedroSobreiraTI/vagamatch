package com.vagamatch.vagaservice.dto;

/** Publicado em vaga.criada para o analise-service extrair as skills. */
public record VagaCriadaEvent(Long vagaId, String titulo, String descricao) {
}
