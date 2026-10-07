package com.vagamatch.vagaservice.repository;

import com.vagamatch.vagaservice.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByNomeIgnoreCase(String nome);

    /** Skills mais pedidas nas vagas analisadas (usa o índice idx_vaga_skill_skill). */
    @Query(value = """
            SELECT s.id AS skillId, s.nome AS nome, s.categoria AS categoria,
                   COUNT(*) AS totalVagas,
                   COUNT(*) FILTER (WHERE vs.obrigatoria) AS comoObrigatoria,
                   COUNT(*) FILTER (WHERE NOT vs.obrigatoria) AS comoDiferencial
            FROM vaga_skill vs
            JOIN skill s ON s.id = vs.skill_id
            JOIN vaga v ON v.id = vs.vaga_id AND v.status = 'ANALISADA'
            GROUP BY s.id, s.nome, s.categoria
            ORDER BY totalVagas DESC, comoObrigatoria DESC, s.nome
            LIMIT :limite
            """, nativeQuery = true)
    List<SkillEstatisticaProjection> maisPedidas(@Param("limite") int limite);
}
