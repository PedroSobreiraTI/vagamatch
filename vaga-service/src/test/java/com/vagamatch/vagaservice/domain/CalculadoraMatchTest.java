package com.vagamatch.vagaservice.domain;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.vagamatch.vagaservice.Fixtures.skill;
import static com.vagamatch.vagaservice.Fixtures.vagaAnalisada;
import static org.assertj.core.api.Assertions.assertThat;

class CalculadoraMatchTest {

    private final Skill java = skill(1, "Java");
    private final Skill sql = skill(2, "SQL");
    private final Skill docker = skill(3, "Docker");
    private final Skill kafka = skill(4, "kafka");

    /** Java e SQL obrigatórias (peso 2 cada), Docker e Kafka diferenciais (peso 1 cada): total 6. */
    private List<VagaSkill> skillsDaVaga() {
        Map<Skill, Boolean> skills = new LinkedHashMap<>();
        skills.put(java, true);
        skills.put(sql, true);
        skills.put(docker, false);
        skills.put(kafka, false);
        return List.copyOf(vagaAnalisada(10, skills).getSkills());
    }

    @Test
    void obrigatoriaPesaODobroDoDiferencial() {
        // tem Java (2) e Docker (1) de 6
        CalculadoraMatch.Resultado r = CalculadoraMatch.calcular(Set.of(1L, 3L), skillsDaVaga());

        assertThat(r.percentual()).isEqualTo(50);
        assertThat(r.skillsAtendidas()).containsExactly("Docker", "Java");
        assertThat(r.obrigatoriasFaltando()).containsExactly("SQL");
        assertThat(r.diferenciaisFaltando()).containsExactly("kafka");
        assertThat(r.atendeTodasObrigatorias()).isFalse();
    }

    @Test
    void soAsObrigatoriasJaAtendeOsRequisitos() {
        CalculadoraMatch.Resultado r = CalculadoraMatch.calcular(Set.of(1L, 2L), skillsDaVaga());

        assertThat(r.percentual()).isEqualTo(67); // 4 de 6
        assertThat(r.atendeTodasObrigatorias()).isTrue();
        assertThat(r.diferenciaisFaltando()).containsExactly("Docker", "kafka"); // ordem ignora maiúscula
    }

    @Test
    void candidatoComTudoTem100() {
        CalculadoraMatch.Resultado r = CalculadoraMatch.calcular(Set.of(1L, 2L, 3L, 4L, 99L), skillsDaVaga());

        assertThat(r.percentual()).isEqualTo(100);
        assertThat(r.obrigatoriasFaltando()).isEmpty();
        assertThat(r.diferenciaisFaltando()).isEmpty();
    }

    @Test
    void candidatoSemSkillsTem0() {
        CalculadoraMatch.Resultado r = CalculadoraMatch.calcular(Set.of(), skillsDaVaga());

        assertThat(r.percentual()).isZero();
        assertThat(r.skillsAtendidas()).isEmpty();
        assertThat(r.obrigatoriasFaltando()).containsExactly("Java", "SQL");
    }

    @Test
    void vagaSemSkillsDa0SemDividirPorZero() {
        CalculadoraMatch.Resultado r = CalculadoraMatch.calcular(Set.of(1L), List.of());

        assertThat(r.percentual()).isZero();
        assertThat(r.atendeTodasObrigatorias()).isTrue();
    }

    @ParameterizedTest
    @CsvSource({"1, 3, 33", "2, 3, 67", "1, 2, 50", "0, 5, 0", "5, 5, 100", "3, 0, 0"})
    void percentualArredondaProInteiroMaisProximo(long atendido, long total, int esperado) {
        assertThat(CalculadoraMatch.percentual(atendido, total)).isEqualTo(esperado);
    }

    @Test
    void pesos() {
        assertThat(CalculadoraMatch.peso(true)).isEqualTo(2);
        assertThat(CalculadoraMatch.peso(false)).isEqualTo(1);
    }
}
