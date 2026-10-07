package com.vagamatch.vagaservice.service;

import com.vagamatch.vagaservice.domain.CalculadoraMatch;
import com.vagamatch.vagaservice.domain.Candidato;
import com.vagamatch.vagaservice.domain.StatusVaga;
import com.vagamatch.vagaservice.domain.Vaga;
import com.vagamatch.vagaservice.dto.MatchResponse;
import com.vagamatch.vagaservice.dto.VagaRecomendadaResponse;
import com.vagamatch.vagaservice.exception.ConflitoException;
import com.vagamatch.vagaservice.exception.RecursoNaoEncontradoException;
import com.vagamatch.vagaservice.repository.CandidatoRepository;
import com.vagamatch.vagaservice.repository.VagaRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
public class MatchService {

    private final CandidatoRepository candidatoRepository;
    private final VagaRepository vagaRepository;
    private final Counter calculosDetalhe;
    private final Counter calculosRanking;

    public MatchService(CandidatoRepository candidatoRepository, VagaRepository vagaRepository,
                        MeterRegistry meterRegistry) {
        this.candidatoRepository = candidatoRepository;
        this.vagaRepository = vagaRepository;
        this.calculosDetalhe = contador(meterRegistry, "detalhe");
        this.calculosRanking = contador(meterRegistry, "ranking");
    }

    @Transactional(readOnly = true)
    public MatchResponse calcular(Long candidatoId, Long vagaId) {
        Candidato candidato = candidatoRepository.findById(candidatoId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Candidato", candidatoId));
        Vaga vaga = vagaRepository.findById(vagaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Vaga", vagaId));
        if (vaga.getStatus() != StatusVaga.ANALISADA) {
            throw new ConflitoException("Vaga " + vagaId + " ainda não tem skills analisadas (status "
                    + vaga.getStatus() + ")");
        }

        Set<Long> skillsDoCandidato = candidato.getSkills().stream()
                .map(cs -> cs.getSkill().getId())
                .collect(Collectors.toSet());
        CalculadoraMatch.Resultado resultado = CalculadoraMatch.calcular(skillsDoCandidato, vaga.getSkills());
        calculosDetalhe.increment();

        return new MatchResponse(candidatoId, vagaId, vaga.getTitulo(), resultado.percentual(),
                resultado.atendeTodasObrigatorias(), resultado.skillsAtendidas(),
                resultado.obrigatoriasFaltando(), resultado.diferenciaisFaltando());
    }

    /** Vagas analisadas ordenadas pelo match com o candidato (maior primeiro). */
    @Transactional(readOnly = true)
    public Page<VagaRecomendadaResponse> recomendar(Long candidatoId, Pageable pageable) {
        if (!candidatoRepository.existsById(candidatoId)) {
            throw new RecursoNaoEncontradoException("Candidato", candidatoId);
        }
        // a ordem é a do ranking; ignora o sort da URL pra não quebrar a query nativa
        Pageable semOrdenacao = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        calculosRanking.increment();
        return vagaRepository.rankingPorMatch(candidatoId, CalculadoraMatch.PESO_OBRIGATORIA,
                        CalculadoraMatch.PESO_DIFERENCIAL, semOrdenacao)
                .map(VagaRecomendadaResponse::from);
    }

    private static Counter contador(MeterRegistry registry, String tipo) {
        return Counter.builder("vagamatch.match.calculos")
                .description("Cálculos de match pedidos")
                .tag("tipo", tipo)
                .register(registry);
    }
}
