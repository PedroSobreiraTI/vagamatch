package com.vagamatch.vagaservice.service;

import com.vagamatch.vagaservice.domain.Skill;
import com.vagamatch.vagaservice.domain.StatusVaga;
import com.vagamatch.vagaservice.domain.Vaga;
import com.vagamatch.vagaservice.domain.VagaSkill;
import com.vagamatch.vagaservice.dto.VagaAnalisadaEvent;
import com.vagamatch.vagaservice.dto.VagaAnalisadaEvent.SkillExtraida;
import com.vagamatch.vagaservice.dto.VagaCriadaEvent;
import com.vagamatch.vagaservice.dto.VagaRequest;
import com.vagamatch.vagaservice.dto.VagaResponse;
import com.vagamatch.vagaservice.exception.RecursoNaoEncontradoException;
import com.vagamatch.vagaservice.repository.VagaRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Collectors;

import static com.vagamatch.vagaservice.Fixtures.skill;
import static com.vagamatch.vagaservice.Fixtures.vaga;
import static com.vagamatch.vagaservice.Fixtures.vagaAnalisada;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VagaServiceTest {

    @Mock
    private VagaRepository vagaRepository;
    @Mock
    private SkillService skillService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    private SimpleMeterRegistry meterRegistry;
    private VagaService vagaService;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        vagaService = new VagaService(vagaRepository, skillService, eventPublisher, meterRegistry);
    }

    private double analises(String resultado) {
        var contador = meterRegistry.find("vagamatch.vagas.analisadas").tag("resultado", resultado).counter();
        return contador == null ? 0 : contador.count();
    }

    /** SkillService falso: devolve uma Skill nova com id sequencial pra cada nome. */
    private void skillServiceCriaSkills() {
        AtomicLong ids = new AtomicLong();
        when(skillService.buscarOuCriar(anyString(), any()))
                .thenAnswer(inv -> skill(ids.incrementAndGet(), inv.getArgument(0)));
    }

    private static Map<String, Boolean> obrigatoriaPorNome(Vaga vaga) {
        return vaga.getSkills().stream()
                .collect(Collectors.toMap(vs -> vs.getSkill().getNome(), VagaSkill::isObrigatoria));
    }

    @Test
    void criarSalvaEPublicaOEventoPraAnalise() {
        when(vagaRepository.save(any(Vaga.class))).thenAnswer(inv -> {
            Vaga salva = inv.getArgument(0);
            ReflectionTestUtils.setField(salva, "id", 5L);
            return salva;
        });

        VagaResponse criada = vagaService.criar(new VagaRequest("Dev Java", "Acme", "Remoto", "Java e SQL", null));

        assertThat(criada.id()).isEqualTo(5L);
        assertThat(criada.status()).isEqualTo(StatusVaga.PENDENTE);
        verify(eventPublisher).publishEvent(new VagaCriadaEvent(5L, "Dev Java", "Java e SQL"));
    }

    @Test
    void buscarVagaInexistenteDa404() {
        when(vagaRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vagaService.buscar(9L)).isInstanceOf(RecursoNaoEncontradoException.class);
    }

    @Test
    void registraAsSkillsEMarcaAnalisada() {
        Vaga vaga = vaga(1, "Dev Java");
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
        skillServiceCriaSkills();

        vagaService.registrarAnalise(new VagaAnalisadaEvent(1L, true, null, List.of(
                new SkillExtraida("Java", "LINGUAGEM", true),
                new SkillExtraida("Docker", "DEVOPS", false))));

        assertThat(vaga.getStatus()).isEqualTo(StatusVaga.ANALISADA);
        assertThat(obrigatoriaPorNome(vaga)).containsExactlyInAnyOrderEntriesOf(Map.of("Java", true, "Docker", false));
        verify(skillService).buscarOuCriar("Java", "LINGUAGEM");
        assertThat(analises("sucesso")).isEqualTo(1);
    }

    @Test
    void skillRepetidaComCaixaDiferenteViraUmaSoEObrigatoriaPrevalece() {
        Vaga vaga = vaga(1, "Dev Java");
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
        skillServiceCriaSkills();

        vagaService.registrarAnalise(new VagaAnalisadaEvent(1L, true, null, List.of(
                new SkillExtraida("Java", "LINGUAGEM", false),
                new SkillExtraida(" java ", "LINGUAGEM", true))));

        assertThat(vaga.getSkills()).hasSize(1);
        assertThat(obrigatoriaPorNome(vaga)).containsExactly(Map.entry("java", true));
    }

    @Test
    void ignoraSkillSemNomeEListaNula() {
        Vaga vaga = vaga(1, "Dev Java");
        Vaga outra = vaga(2, "Dev Go");
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));
        when(vagaRepository.findById(2L)).thenReturn(Optional.of(outra));

        vagaService.registrarAnalise(new VagaAnalisadaEvent(1L, true, null, Arrays.asList(
                new SkillExtraida("  ", "OUTRO", true), new SkillExtraida(null, "OUTRO", true))));
        vagaService.registrarAnalise(new VagaAnalisadaEvent(2L, true, null, null));

        assertThat(vaga.getSkills()).isEmpty();
        assertThat(vaga.getStatus()).isEqualTo(StatusVaga.ANALISADA);
        assertThat(outra.getStatus()).isEqualTo(StatusVaga.ANALISADA);
        verifyNoInteractions(skillService);
    }

    @Test
    void cortaNomeECategoriaMaioresQueAColuna() {
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga(1, "Dev")));
        skillServiceCriaSkills();

        vagaService.registrarAnalise(new VagaAnalisadaEvent(1L, true, null, List.of(
                new SkillExtraida("n".repeat(150), "c".repeat(80), true))));

        verify(skillService).buscarOuCriar(argThat(nome -> nome.length() == 100),
                argThat(categoria -> categoria.length() == 50));
    }

    @Test
    void falhaNaAnaliseMarcaErro() {
        Vaga vaga = vaga(1, "Dev Java");
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));

        vagaService.registrarAnalise(new VagaAnalisadaEvent(1L, false, "Gemini 401", List.of()));

        assertThat(vaga.getStatus()).isEqualTo(StatusVaga.ERRO);
        assertThat(vaga.getSkills()).isEmpty();
        assertThat(analises("erro")).isEqualTo(1);
        verifyNoInteractions(skillService);
    }

    @Test
    void mensagemRepetidaDeVagaJaAnalisadaEhIgnorada() {
        Skill java = skill(1, "Java");
        Vaga vaga = vagaAnalisada(1, Map.of(java, true));
        when(vagaRepository.findById(1L)).thenReturn(Optional.of(vaga));

        vagaService.registrarAnalise(new VagaAnalisadaEvent(1L, false, "atrasada", List.of()));

        assertThat(vaga.getStatus()).isEqualTo(StatusVaga.ANALISADA);
        assertThat(vaga.getSkills()).hasSize(1);
        assertThat(analises("erro")).isZero();
    }

    @Test
    void analiseDeVagaRemovidaEhIgnorada() {
        when(vagaRepository.findById(eq(1L))).thenReturn(Optional.empty());

        vagaService.registrarAnalise(new VagaAnalisadaEvent(1L, true, null, List.of(
                new SkillExtraida("Java", "LINGUAGEM", true))));

        verifyNoInteractions(skillService);
        assertThat(analises("sucesso")).isZero();
    }
}
