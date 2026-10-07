package com.vagamatch.vagaservice.dto;

import com.vagamatch.vagaservice.domain.Skill;

public record SkillResponse(Long id, String nome, String categoria) {

    public static SkillResponse from(Skill skill) {
        return new SkillResponse(skill.getId(), skill.getNome(), skill.getCategoria());
    }
}
