package com.vagamatch.vagaservice.service;

import com.vagamatch.vagaservice.domain.CalculadoraMatch;
import com.vagamatch.vagaservice.domain.StatusVaga;
import com.vagamatch.vagaservice.dto.EstatisticaSkillsResponse;
import com.vagamatch.vagaservice.dto.EstatisticaSkillsResponse.SkillPedida;
import com.vagamatch.vagaservice.repository.SkillRepository;
import com.vagamatch.vagaservice.repository.VagaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class EstatisticaService {

    private final SkillRepository skillRepository;
    private final VagaRepository vagaRepository;

    public EstatisticaService(SkillRepository skillRepository, VagaRepository vagaRepository) {
        this.skillRepository = skillRepository;
        this.vagaRepository = vagaRepository;
    }

    @Transactional(readOnly = true)
    public EstatisticaSkillsResponse skillsMaisPedidas(int limite) {
        long vagasAnalisadas = vagaRepository.countByStatus(StatusVaga.ANALISADA);
        List<SkillPedida> skills = skillRepository.maisPedidas(limite).stream()
                .map(s -> new SkillPedida(s.getSkillId(), s.getNome(), s.getCategoria(), s.getTotalVagas(),
                        s.getComoObrigatoria(), s.getComoDiferencial(),
                        CalculadoraMatch.percentual(s.getTotalVagas(), vagasAnalisadas)))
                .toList();
        return new EstatisticaSkillsResponse(vagasAnalisadas, skills);
    }
}
