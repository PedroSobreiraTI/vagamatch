package com.vagamatch.vagaservice.domain;

import jakarta.persistence.*;

/** Skill pedida por uma vaga. obrigatoria = requisito; false = diferencial. */
@Entity
@Table(name = "vaga_skill")
public class VagaSkill {

    @EmbeddedId
    private VagaSkillId id = new VagaSkillId();

    @MapsId("vagaId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vaga_id")
    private Vaga vaga;

    @MapsId("skillId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id")
    private Skill skill;

    @Column(nullable = false)
    private boolean obrigatoria;

    protected VagaSkill() {
    }

    public VagaSkill(Vaga vaga, Skill skill, boolean obrigatoria) {
        this.vaga = vaga;
        this.skill = skill;
        this.obrigatoria = obrigatoria;
    }

    public Skill getSkill() { return skill; }
    public boolean isObrigatoria() { return obrigatoria; }
}
