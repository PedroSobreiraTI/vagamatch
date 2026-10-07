package com.vagamatch.vagaservice.repository;

import com.vagamatch.vagaservice.domain.Skill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SkillRepository extends JpaRepository<Skill, Long> {

    Optional<Skill> findByNomeIgnoreCase(String nome);
}
