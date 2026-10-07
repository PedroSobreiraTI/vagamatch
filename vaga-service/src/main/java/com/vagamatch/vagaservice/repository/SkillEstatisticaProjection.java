package com.vagamatch.vagaservice.repository;

/** Quantas vagas analisadas pedem cada skill. */
public interface SkillEstatisticaProjection {
    Long getSkillId();
    String getNome();
    String getCategoria();
    Long getTotalVagas();
    Long getComoObrigatoria();
    Long getComoDiferencial();
}
