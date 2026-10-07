package com.vagamatch.vagaservice.repository;

/** Linha do ranking de vagas por match, calculada direto no banco. */
public interface VagaMatchProjection {
    Long getVagaId();
    String getTitulo();
    String getEmpresa();
    Long getPesoTotal();
    Long getPesoAtendido();
    Long getObrigatoriasFaltando();
}
