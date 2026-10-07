package com.vagamatch.vagaservice.repository;

import com.vagamatch.vagaservice.domain.Vaga;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VagaRepository extends JpaRepository<Vaga, Long> {
}
