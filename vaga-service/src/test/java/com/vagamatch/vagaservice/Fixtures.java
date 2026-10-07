package com.vagamatch.vagaservice;

import com.vagamatch.vagaservice.domain.Candidato;
import com.vagamatch.vagaservice.domain.Skill;
import com.vagamatch.vagaservice.domain.Vaga;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

/** Entidades prontas pros testes de unidade. O id é setado na mão porque quem gera é o banco. */
public final class Fixtures {

    private Fixtures() {
    }

    public static Skill skill(long id, String nome) {
        Skill skill = new Skill(nome, null);
        ReflectionTestUtils.setField(skill, "id", id);
        return skill;
    }

    public static Vaga vaga(long id, String titulo) {
        Vaga vaga = new Vaga(titulo, "Acme", "Remoto", "descrição", null);
        ReflectionTestUtils.setField(vaga, "id", id);
        return vaga;
    }

    /** Vaga ANALISADA com as skills (valor = obrigatória). */
    public static Vaga vagaAnalisada(long id, Map<Skill, Boolean> skills) {
        Vaga vaga = vaga(id, "Dev Java");
        vaga.registrarAnalise(skills);
        return vaga;
    }

    public static Candidato candidato(long id, Skill... skills) {
        Candidato candidato = new Candidato("Ana", "ana@exemplo.com");
        ReflectionTestUtils.setField(candidato, "id", id);
        for (Skill skill : skills) {
            candidato.definirSkill(skill, 3);
        }
        return candidato;
    }
}
