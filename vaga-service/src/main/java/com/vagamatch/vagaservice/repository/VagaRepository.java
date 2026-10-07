package com.vagamatch.vagaservice.repository;

import com.vagamatch.vagaservice.domain.StatusVaga;
import com.vagamatch.vagaservice.domain.Vaga;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VagaRepository extends JpaRepository<Vaga, Long> {

    long countByStatus(StatusVaga status);

    /**
     * Ranking das vagas analisadas por match com o candidato, calculado no banco:
     * LEFT JOIN nas skills do candidato marca o que ele tem; o GROUP BY soma os pesos por vaga.
     * Assim a paginação é real e não precisa carregar todas as vagas na memória.
     * Os pesos vêm por parâmetro pra regra ficar só na CalculadoraMatch.
     */
    @Query(value = """
            SELECT r.vaga_id AS vagaId, r.titulo AS titulo, r.empresa AS empresa,
                   r.peso_total AS pesoTotal, r.peso_atendido AS pesoAtendido,
                   r.obrigatorias_faltando AS obrigatoriasFaltando
            FROM (
                SELECT v.id AS vaga_id, v.titulo, v.empresa,
                       SUM(CASE WHEN vs.obrigatoria THEN :pesoObrigatoria ELSE :pesoDiferencial END) AS peso_total,
                       SUM(CASE WHEN cs.skill_id IS NULL THEN 0
                                WHEN vs.obrigatoria THEN :pesoObrigatoria ELSE :pesoDiferencial END) AS peso_atendido,
                       COUNT(*) FILTER (WHERE vs.obrigatoria AND cs.skill_id IS NULL) AS obrigatorias_faltando
                FROM vaga v
                JOIN vaga_skill vs ON vs.vaga_id = v.id
                LEFT JOIN candidato_skill cs ON cs.skill_id = vs.skill_id AND cs.candidato_id = :candidatoId
                WHERE v.status = 'ANALISADA'
                GROUP BY v.id, v.titulo, v.empresa
            ) r
            ORDER BY r.peso_atendido::numeric / r.peso_total DESC, r.obrigatorias_faltando, r.vaga_id
            """,
            countQuery = """
            SELECT COUNT(*) FROM vaga v
            WHERE v.status = 'ANALISADA' AND EXISTS (SELECT 1 FROM vaga_skill vs WHERE vs.vaga_id = v.id)
            """,
            nativeQuery = true)
    Page<VagaMatchProjection> rankingPorMatch(@Param("candidatoId") Long candidatoId,
                                              @Param("pesoObrigatoria") int pesoObrigatoria,
                                              @Param("pesoDiferencial") int pesoDiferencial,
                                              Pageable pageable);
}
