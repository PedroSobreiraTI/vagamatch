package com.vagamatch.vagaservice.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class VagaSkillId implements Serializable {

    @Column(name = "vaga_id")
    private Long vagaId;

    @Column(name = "skill_id")
    private Long skillId;

    protected VagaSkillId() {
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof VagaSkillId that)) return false;
        return Objects.equals(vagaId, that.vagaId) && Objects.equals(skillId, that.skillId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(vagaId, skillId);
    }
}
