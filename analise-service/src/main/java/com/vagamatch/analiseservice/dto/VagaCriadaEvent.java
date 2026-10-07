package com.vagamatch.analiseservice.dto;

/** Recebido de vaga.criada (publicado pelo vaga-service). */
public record VagaCriadaEvent(Long vagaId, String titulo, String descricao) {
}
