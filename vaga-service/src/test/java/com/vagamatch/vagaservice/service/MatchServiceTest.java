package com.vagamatch.vagaservice.service;

import com.vagamatch.vagaservice.domain.Skill;
import com.vagamatch.vagaservice.domain.Vaga;
import com.vagamatch.vagaservice.dto.MatchResponse;
import com.vagamatch.vagaservice.dto.VagaRecomendadaResponse;
import com.vagamatch.vagaservice.exception.ConflitoException;
import com.vagamatch.vagaservice.exception.RecursoNaoEncontradoException;
import com.vagamatch.vagaservice.repository.CandidatoRepository;
import com.vagamatch.vagaservice.repository.VagaMatchProjection;
import com.vagamatch.vagaservice.repository.VagaRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static com.vagamatch.vagaservice.Fixtures.candidato;
import static com.vagamatch.vagaservice.Fixtures.skill;
import static com.vagamatch.vagaservice.Fixtures.vaga;
import static com.vagamatch.vagaservice.Fixtures.vagaAnalisada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MatchServiceTest {

    @Mock
    private CandidatoRepository candidatoRepository;
    @Mock
    private VagaRepository vagaRepository;
    private SimpleMeterRegistry meterRegistry;
    private MatchService matchService;

    private final Skill java = skill(1, "Java");
    private final Skill sql = skill(2, "SQL");
    private final Skill docker = skill(3, "Docker");

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        matchService = new MatchService(candidatoRepository, vagaRepository, meterRegistry);
    }

    private double calculos(String tipo) {
        return meterRegistry.get("vagamatch.match.calculos").tag("tipo", tipo).counter().count();
    }

    @Test
    void calculaOMatchComASkillsDoCandidato() {
        Map<Skill, Boolean> skills = new LinkedHashMap<>();
        skills.put(java, true);
        skills.put(sql, true);
        skills.put(docker, false);
        when(candidatoRepository.findById(1L)).thenReturn(Optional.of(candidato(1, java, docker)));
        when(vagaRepository.findById(10L)).thenReturn(Optional.of(vagaAnalisada(10, skills)));

        MatchResponse match = matchService.calcular(1L, 10L);

        assertThat(match.percentual()).isEqualTo(60); // Java 2 + Docker 1 de 5
        assertThat(match.tituloVaga()).isEqualTo("Dev Java");
        assertThat(match.atendeTodasObrigatorias()).isFalse();
        assertThat(match.skillsAtendidas()).containsExactly("Docker", "Java");
        assertThat(match.obrigatoriasFaltando()).containsExactly("SQL");
        assertThat(match.diferenciaisFaltando()).isEmpty();
        assertThat(calculos("detalhe")).isEqualTo(1);
    }

    @Test
    void candidatoInexistenteDa404() {
        when(candidatoRepository.findById(1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.calcular(1L, 10L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Candidato");
    }

    @Test
    void vagaInexistenteDa404() {
        when(candidatoRepository.findById(1L)).thenReturn(Optional.of(candidato(1)));
        when(vagaRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> matchService.calcular(1L, 10L))
                .isInstanceOf(RecursoNaoEncontradoException.class)
                .hasMessageContaining("Vaga");
    }

    @Test
    void vagaAindaPendenteDa409SemContarCalculo() {
        when(candidatoRepository.findById(1L)).thenReturn(Optional.of(candidato(1)));
        when(vagaRepository.findById(10L)).thenReturn(Optional.of(vaga(10, "Dev")));

        assertThatThrownBy(() -> matchService.calcular(1L, 10L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("PENDENTE");
        assertThat(calculos("detalhe")).isZero();
    }

    @Test
    void vagaComErroNaAnaliseDa409() {
        Vaga comErro = vaga(10, "Dev");
        comErro.marcarErroNaAnalise();
        when(candidatoRepository.findById(1L)).thenReturn(Optional.of(candidato(1)));
        when(vagaRepository.findById(10L)).thenReturn(Optional.of(comErro));

        assertThatThrownBy(() -> matchService.calcular(1L, 10L))
                .isInstanceOf(ConflitoException.class)
                .hasMessageContaining("ERRO");
    }

    @Test
    void rankingPassaOsPesosDaCalculadoraEIgnoraOSortDaUrl() {
        VagaMatchProjection linha = mock(VagaMatchProjection.class);
        when(linha.getVagaId()).thenReturn(10L);
        when(linha.getTitulo()).thenReturn("Dev Java");
        when(linha.getEmpresa()).thenReturn("Acme");
        when(linha.getPesoTotal()).thenReturn(4L);
        when(linha.getPesoAtendido()).thenReturn(3L);
        when(linha.getObrigatoriasFaltando()).thenReturn(0L);
        PageRequest semSort = PageRequest.of(1, 5);
        when(candidatoRepository.existsById(1L)).thenReturn(true);
        when(vagaRepository.rankingPorMatch(1L, 2, 1, semSort)).thenReturn(new PageImpl<>(List.of(linha), semSort, 6));

        Page<VagaRecomendadaResponse> pagina = matchService.recomendar(1L, PageRequest.of(1, 5, Sort.by("titulo")));

        assertThat(pagina.getContent()).containsExactly(
                new VagaRecomendadaResponse(10L, "Dev Java", "Acme", 75, true, 0));
        assertThat(pagina.getTotalElements()).isEqualTo(6);
        assertThat(calculos("ranking")).isEqualTo(1);
    }

    @Test
    void rankingDeCandidatoInexistenteDa404SemConsultarAsVagas() {
        when(candidatoRepository.existsById(1L)).thenReturn(false);

        assertThatThrownBy(() -> matchService.recomendar(1L, PageRequest.of(0, 10)))
                .isInstanceOf(RecursoNaoEncontradoException.class);
        verify(vagaRepository, never()).rankingPorMatch(anyLong(), anyInt(), anyInt(), any());
    }
}
