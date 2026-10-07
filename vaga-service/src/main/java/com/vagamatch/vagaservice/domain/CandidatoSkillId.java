package com.vagamatch.vagaservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class CandidatoSkillId implements Serializable {

    @Column(name = "candidato_id")
    private Long candidatoId;

    @Column(name = "skill_id")
    private Long skillId;

    protected CandidatoSkillId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof CandidatoSkillId that)) return false;
        return Objects.equals(candidatoId, that.candidatoId) && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(candidatoId, skillId);
    }
}
