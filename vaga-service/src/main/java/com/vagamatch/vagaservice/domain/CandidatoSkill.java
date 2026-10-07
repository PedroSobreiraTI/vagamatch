package com.vagamatch.vagaservice.domain;

import jakarta.persistence.*;

/** Skill do candidato com nível de proficiência de 1 a 5. */
@Entity
@Table(name = "candidato_skill")
public class CandidatoSkill {

    @EmbeddedId
    private CandidatoSkillId id = new CandidatoSkillId();

    @MapsId("candidatoId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "candidato_id")
    private Candidato candidato;

    @MapsId("skillId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(nullable = false)
    private short nivel;

    protected CandidatoSkill() {
    }

    public CandidatoSkill(Candidato candidato, Skill skill, int nivel) {
        this.candidato = candidato;
        this.skill = skill;
        setNivel(nivel);
    }

    public void setNivel(int nivel) {
        this.nivel = (short) nivel;
    }

    public Skill getSkill() { return skill; }
    public int getNivel() { return nivel; }
}
