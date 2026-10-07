package com.vagamatch.analiseservice.dto;

/** Skill extraída da descrição. obrigatoria = requisito; false = diferencial. */
public record SkillExtraida(String nome, String categoria, boolean obrigatoria) {
}
